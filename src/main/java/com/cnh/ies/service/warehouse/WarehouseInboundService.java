package com.cnh.ies.service.warehouse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.cnh.ies.constant.Constant;
import com.cnh.ies.entity.auth.UserEntity;
import com.cnh.ies.entity.payment.PaymentRequestEntity;
import com.cnh.ies.entity.payment.PaymentRequestPurchaseOrderLineEntity;
import com.cnh.ies.entity.purchaseorder.PurchaseOrderLineEntity;
import com.cnh.ies.entity.warehouse.WarehouseInboundReceiptApprovalEntity;
import com.cnh.ies.entity.warehouse.WarehouseInboundReceiptEntity;
import com.cnh.ies.entity.warehouse.WarehouseInboundReceiptLineEntity;
import com.cnh.ies.exception.ApiException;
import com.cnh.ies.model.payment.ApprovePaymentRequest;
import com.cnh.ies.model.payment.PaymentFileUploadInfo;
import com.cnh.ies.model.payment.PaymentRequestApprovalInfo;
import com.cnh.ies.model.payment.PaymentRequestInfo;
import com.cnh.ies.model.payment.RejectPaymentRequest;
import com.cnh.ies.model.user.RoleInfo;
import com.cnh.ies.model.user.UserInfo;
import com.cnh.ies.model.warehouse.WarehouseInboundAddLineRequest;
import com.cnh.ies.model.warehouse.WarehouseInboundConfirmLineRequest;
import com.cnh.ies.model.warehouse.WarehouseInboundConfirmRequest;
import com.cnh.ies.model.warehouse.WarehouseInboundLinePatchRequest;
import com.cnh.ies.model.warehouse.WarehouseInboundReceiptInfo;
import com.cnh.ies.model.warehouse.WarehouseInboundReceiptLineInfo;
import com.cnh.ies.model.warehouse.WarehouseInboundSearchHit;
import com.cnh.ies.model.warehouse.WarehouseInboundSearchResponse;
import com.cnh.ies.repository.auth.UserRepo;
import com.cnh.ies.repository.payment.PaymentRequestPurchaseOrderLineRepo;
import com.cnh.ies.repository.payment.PaymentRequestRepo;
import com.cnh.ies.repository.purchaseorder.PurchaseOrderLineRepo;
import com.cnh.ies.repository.warehouse.WarehouseInboundReceiptApprovalRepo;
import com.cnh.ies.repository.warehouse.WarehouseInboundReceiptLineRepo;
import com.cnh.ies.repository.warehouse.WarehouseInboundReceiptRepo;
import com.cnh.ies.service.file.FileService;
import com.cnh.ies.service.payment.PaymentRequestService;
import com.cnh.ies.service.redis.RedisService;
import com.cnh.ies.util.RequestContext;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class WarehouseInboundService {

    private static final int MIN_APPROVAL_LEVELS = 1;
    private static final int MAX_APPROVAL_LEVELS = 10;

    private final PaymentRequestRepo paymentRequestRepo;
    private final PaymentRequestPurchaseOrderLineRepo paymentRequestPurchaseOrderLineRepo;
    private final PurchaseOrderLineRepo purchaseOrderLineRepo;
    private final PaymentRequestService paymentRequestService;
    private final WarehouseInboundReceiptRepo warehouseInboundReceiptRepo;
    private final WarehouseInboundReceiptLineRepo warehouseInboundReceiptLineRepo;
    private final WarehouseInboundReceiptApprovalRepo warehouseInboundReceiptApprovalRepo;
    private final WarehouseInventoryService warehouseInventoryService;
    private final FileService fileService;
    private final UserRepo userRepo;
    private final RedisService redisService;
    private final ObjectMapper objectMapper;


    public WarehouseInboundSearchResponse search(String notesContains, String paperType, String paperCode, String requestId) {
        String notesQ = normalize(notesContains);
        String code = normalize(paperCode);
        String pType = normalize(paperType);

        if (notesQ == null && code == null) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST,
                    "Provide notesContains and/or paperCode to search",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }

        LinkedHashSet<UUID> orderedIds = new LinkedHashSet<>();
        if (notesQ != null) {
            for (PaymentRequestEntity pr : paymentRequestRepo.findByNotesContainingIgnoreCase(notesQ)) {
                orderedIds.add(pr.getId());
            }
            orderedIds.addAll(paymentRequestPurchaseOrderLineRepo.findPaymentRequestIdsByPaymentRequestLineNoteContaining(notesQ));
        }
        if (code != null) {
            List<UUID> fromPaper;
            if (pType != null) {
                List<PurchaseOrderLineEntity> pol = findPurchaseOrderLinesByPaperType(pType, code, requestId);
                List<UUID> polIds = pol.stream().map(PurchaseOrderLineEntity::getId).toList();
                fromPaper = polIds.isEmpty()
                        ? List.of()
                        : paymentRequestPurchaseOrderLineRepo.findPaymentRequestIdsByPurchaseOrderLineIds(polIds);
            } else {
                fromPaper = paymentRequestPurchaseOrderLineRepo.findPaymentRequestIdsByAnyLinkedPaperCode(code);
            }
            orderedIds.addAll(fromPaper);
        }

        if (orderedIds.isEmpty()) {
            WarehouseInboundSearchResponse empty = new WarehouseInboundSearchResponse();
            empty.setHits(List.of());
            return empty;
        }

        List<PaymentRequestEntity> prs = paymentRequestRepo.findByIdInWithVendor(orderedIds);
        prs.sort(Comparator.comparing(PaymentRequestEntity::getRequestDate, Comparator.nullsLast(Comparator.reverseOrder())));

        List<WarehouseInboundSearchHit> hits = prs.stream().map(this::toSearchHit).toList();
        WarehouseInboundSearchResponse response = new WarehouseInboundSearchResponse();
        response.setHits(hits);
        return response;
    }

    /**
     * Full payment request payload for inbound: PO lines include product, vendor, and sales order context (same shape as finance detail).
     */
    public PaymentRequestInfo getPaymentRequestDetail(String paymentRequestId, String requestId) {
        return paymentRequestService.getById(paymentRequestId, requestId);
    }

    @Transactional
    public WarehouseInboundReceiptInfo confirmInbound(WarehouseInboundConfirmRequest request, String requestId) {
        if (request == null || request.getPaymentRequestId() == null || request.getPaymentRequestId().isBlank()) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "paymentRequestId is required",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }
        if (request.getLines() == null || request.getLines().isEmpty()) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "At least one line is required",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }

        int approvalLevels = request.getApprovalLevels() == null ? 2 : request.getApprovalLevels();
        if (approvalLevels < MIN_APPROVAL_LEVELS || approvalLevels > MAX_APPROVAL_LEVELS) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST,
                    "approvalLevels must be between " + MIN_APPROVAL_LEVELS + " and " + MAX_APPROVAL_LEVELS,
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }
        List<String> customRoles = request.getApprovalRoles();
        if (customRoles != null && !customRoles.isEmpty() && customRoles.size() != approvalLevels) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST,
                    "approvalRoles size must match approvalLevels when provided",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }

        UUID prId = parseUuid(request.getPaymentRequestId(), "paymentRequestId", requestId);
        PaymentRequestEntity pr = paymentRequestRepo.findByIdAndIsDeletedFalse(prId)
                .orElseThrow(() -> new ApiException(ApiException.ErrorCode.NOT_FOUND, "Payment request not found",
                        HttpStatus.NOT_FOUND.value(), requestId));

        List<PaymentRequestPurchaseOrderLineEntity> dbLines = paymentRequestPurchaseOrderLineRepo.findByPaymentRequestId(prId);
        if (dbLines.isEmpty()) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "Payment request has no purchase order lines",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }

        Map<UUID, PaymentRequestPurchaseOrderLineEntity> prpolById = dbLines.stream()
                .collect(Collectors.toMap(PaymentRequestPurchaseOrderLineEntity::getId, x -> x));
        Map<UUID, WarehouseInboundConfirmLineRequest> byId = new HashMap<>();
        for (WarehouseInboundConfirmLineRequest line : request.getLines()) {
            if (line.getPaymentRequestPurchaseOrderLineId() == null || line.getPaymentRequestPurchaseOrderLineId().isBlank()) {
                throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "Each line requires paymentRequestPurchaseOrderLineId",
                        HttpStatus.BAD_REQUEST.value(), requestId);
            }
            UUID lid = parseUuid(line.getPaymentRequestPurchaseOrderLineId(), "paymentRequestPurchaseOrderLineId", requestId);
            if (byId.containsKey(lid)) {
                throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "Duplicate line id: " + lid,
                        HttpStatus.BAD_REQUEST.value(), requestId);
            }
            if (!prpolById.containsKey(lid)) {
                throw new ApiException(ApiException.ErrorCode.BAD_REQUEST,
                        "paymentRequestPurchaseOrderLineId is not on this payment request: " + lid,
                        HttpStatus.BAD_REQUEST.value(), requestId);
            }
            byId.put(lid, line);
        }

        for (WarehouseInboundConfirmLineRequest line : request.getLines()) {
            if (line.getQuantityReceived() == null) {
                throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "quantityReceived is required on each line",
                        HttpStatus.BAD_REQUEST.value(), requestId);
            }
            if (line.getQuantityReceived().compareTo(BigDecimal.ZERO) < 0) {
                throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "quantityReceived cannot be negative",
                        HttpStatus.BAD_REQUEST.value(), requestId);
            }
        }

        BigDecimal exchangeRate = request.getExchangeRate() != null ? request.getExchangeRate() : pr.getExchangeRate();
        if (exchangeRate == null || exchangeRate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "exchangeRate must be positive",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }
        BigDecimal fee = request.getFeeAmount() != null ? request.getFeeAmount() : BigDecimal.ZERO;
        if (fee.compareTo(BigDecimal.ZERO) < 0) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "feeAmount cannot be negative",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }

        WarehouseInboundReceiptEntity receipt = new WarehouseInboundReceiptEntity();
        receipt.setPaymentRequest(pr);
        receipt.setCurrency(pr.getCurrency());
        receipt.setExchangeRate(exchangeRate);
        receipt.setFeeAmount(fee);
        receipt.setRealBillAmount(request.getRealBillAmount());
        receipt.setBillOnPaperAmount(request.getBillOnPaperAmount());
        receipt.setNote(request.getNote());
        receipt.setStatus(Constant.WAREHOUSE_INBOUND_STATUS_DRAFT);
        receipt.setApprovalLevels(approvalLevels);
        receipt.setCurrentApprovalLevel(0);
        receipt.setCreatedBy(RequestContext.getCurrentUsername());
        receipt.setUpdatedBy(RequestContext.getCurrentUsername());
        receipt.setIsDeleted(false);
        warehouseInboundReceiptRepo.save(receipt);

        seedApprovals(receipt, approvalLevels, customRoles, requestId);

        String username = RequestContext.getCurrentUsername();
        for (Map.Entry<UUID, WarehouseInboundConfirmLineRequest> e : byId.entrySet()) {
            PaymentRequestPurchaseOrderLineEntity prpol = prpolById.get(e.getKey());
            WarehouseInboundConfirmLineRequest lr = e.getValue();
            PurchaseOrderLineEntity pol = prpol.getPurchaseOrderLine();
            BigDecimal qtyExpected = pol != null && pol.getQuantity() != null ? pol.getQuantity() : BigDecimal.ZERO;
            BigDecimal tax = lr.getTaxPercent() != null ? lr.getTaxPercent()
                    : (pol != null && pol.getTax() != null ? pol.getTax() : BigDecimal.ZERO);

            WarehouseInboundReceiptLineEntity lineEntity = new WarehouseInboundReceiptLineEntity();
            lineEntity.setReceipt(receipt);
            lineEntity.setPaymentRequestPurchaseOrderLine(prpol);
            lineEntity.setQuantityExpected(qtyExpected);
            lineEntity.setQuantityReceived(lr.getQuantityReceived());
            lineEntity.setTaxPercent(tax);
            lineEntity.setLineNote(lr.getLineNote());
            lineEntity.setCreatedBy(username);
            lineEntity.setUpdatedBy(username);
            lineEntity.setIsDeleted(false);
            warehouseInboundReceiptLineRepo.save(lineEntity);
        }

        List<UUID> fileIds = parseFileIds(request.getAttachedFileIds(), requestId);
        fileService.linkFilesToWarehouseInboundReceipt(fileIds, prId, receipt.getId(), requestId);

        log.info("Warehouse inbound receipt created [receiptId={}, paymentRequestId={}, approvalLevels={}, rid={}]",
                receipt.getId(), prId, approvalLevels, requestId);
        return toReceiptInfo(receipt.getId(), requestId);
    }

    @Transactional
    public WarehouseInboundReceiptInfo submitForApproval(String receiptId, String requestId) {
        WarehouseInboundReceiptEntity receipt = loadReceiptForMutation(receiptId, requestId);
        if (!Constant.WAREHOUSE_INBOUND_STATUS_DRAFT.equals(receipt.getStatus())) {
            throw new ApiException(ApiException.ErrorCode.CONFLICT, "Only DRAFT receipts can be submitted for approval",
                    HttpStatus.CONFLICT.value(), requestId);
        }
        if (receipt.getApprovalLevels() == null || receipt.getApprovalLevels() < MIN_APPROVAL_LEVELS) {
            throw new ApiException(ApiException.ErrorCode.CONFLICT, "Receipt has no approval workflow configured",
                    HttpStatus.CONFLICT.value(), requestId);
        }
        long activeLines = warehouseInboundReceiptLineRepo.countActiveLinesByReceiptId(receipt.getId());
        if (activeLines < 1) {
            throw new ApiException(ApiException.ErrorCode.CONFLICT,
                    "Add at least one inbound line before submitting (lines may have been removed)",
                    HttpStatus.CONFLICT.value(), requestId);
        }
        receipt.setStatus(Constant.WAREHOUSE_INBOUND_STATUS_SUBMITTED);
        receipt.setUpdatedBy(RequestContext.getCurrentUsername());
        warehouseInboundReceiptRepo.save(receipt);
        log.info("Warehouse inbound submitted [receiptId={}, rid={}]", receipt.getId(), requestId);
        return toReceiptInfo(receipt.getId(), requestId);
    }

    @Transactional
    public WarehouseInboundReceiptInfo approve(String receiptId, ApprovePaymentRequest request, String requestId) {
        WarehouseInboundReceiptEntity receipt = loadReceiptForMutation(receiptId, requestId);
        ensureApprovingStatus(receipt, requestId);

        int level = resolveApprovalLevel(request == null ? null : request.getLevel(), receipt);
        if (level <= 0 || level > receipt.getApprovalLevels()) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "Invalid approval level",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }
        WarehouseInboundReceiptApprovalEntity approval = approvalAtLevelOrThrow(receipt.getId(), level, requestId);
        if (!Constant.PAYMENT_APPROVAL_STATUS_PENDING.equals(approval.getStatus())) {
            throw new ApiException(ApiException.ErrorCode.CONFLICT, "This level was already processed",
                    HttpStatus.CONFLICT.value(), requestId);
        }
        if (level != receipt.getCurrentApprovalLevel() + 1) {
            throw new ApiException(ApiException.ErrorCode.CONFLICT,
                    "Approvals must be processed in order; next level is " + (receipt.getCurrentApprovalLevel() + 1),
                    HttpStatus.CONFLICT.value(), requestId);
        }

        UserEntity currentUser = findUserEntity(requestId);
        validateInboundApproverRole(approval.getApprovalRole(), requestId);
        approval.setStatus(Constant.PAYMENT_APPROVAL_STATUS_APPROVED);
        approval.setApprover(currentUser);
        approval.setApprovedAt(Instant.now());
        approval.setNote(request != null ? request.getNote() : null);
        approval.setUpdatedBy(RequestContext.getCurrentUsername());
        warehouseInboundReceiptApprovalRepo.save(approval);

        receipt.setCurrentApprovalLevel(level);
        if (level >= receipt.getApprovalLevels()) {
            receipt.setStatus(Constant.WAREHOUSE_INBOUND_STATUS_APPROVED);
        }
        receipt.setUpdatedBy(RequestContext.getCurrentUsername());
        warehouseInboundReceiptRepo.save(receipt);

        if (Constant.WAREHOUSE_INBOUND_STATUS_APPROVED.equals(receipt.getStatus())) {
            warehouseInventoryService.applyInboundAfterApprove(receipt.getId(), requestId);
        }

        log.info("Warehouse inbound approved [receiptId={}, level={}, rid={}]", receipt.getId(), level, requestId);
        return toReceiptInfo(receipt.getId(), requestId);
    }

    @Transactional
    public WarehouseInboundReceiptInfo deleteInboundLine(String receiptId, String lineId, String requestId) {
        WarehouseInboundReceiptEntity receipt = loadReceiptForMutation(receiptId, requestId);
        if (!Constant.WAREHOUSE_INBOUND_STATUS_DRAFT.equals(receipt.getStatus())) {
            throw new ApiException(ApiException.ErrorCode.CONFLICT, "Lines can only be changed while receipt is DRAFT",
                    HttpStatus.CONFLICT.value(), requestId);
        }
        WarehouseInboundReceiptLineEntity line = loadReceiptLine(receiptId, lineId, requestId);
        line.setIsDeleted(true);
        line.setUpdatedBy(RequestContext.getCurrentUsername());
        warehouseInboundReceiptLineRepo.save(line);
        return toReceiptInfo(receipt.getId(), requestId);
    }

    @Transactional
    public WarehouseInboundReceiptInfo addInboundLine(String receiptId, WarehouseInboundAddLineRequest body, String requestId) {
        if (body == null || body.getPaymentRequestPurchaseOrderLineId() == null
                || body.getPaymentRequestPurchaseOrderLineId().isBlank()) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "paymentRequestPurchaseOrderLineId is required",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }
        if (body.getQuantityReceived() == null || body.getQuantityReceived().compareTo(BigDecimal.ZERO) < 0) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "quantityReceived is required and cannot be negative",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }
        WarehouseInboundReceiptEntity receipt = loadReceiptForMutation(receiptId, requestId);
        if (!Constant.WAREHOUSE_INBOUND_STATUS_DRAFT.equals(receipt.getStatus())) {
            throw new ApiException(ApiException.ErrorCode.CONFLICT, "Lines can only be changed while receipt is DRAFT",
                    HttpStatus.CONFLICT.value(), requestId);
        }
        UUID prpolUuid = parseUuid(body.getPaymentRequestPurchaseOrderLineId(), "paymentRequestPurchaseOrderLineId", requestId);
        PaymentRequestPurchaseOrderLineEntity prpol = paymentRequestPurchaseOrderLineRepo
                .findByIdAndPaymentRequestId(prpolUuid, receipt.getPaymentRequest().getId())
                .orElseThrow(() -> new ApiException(ApiException.ErrorCode.BAD_REQUEST,
                        "Line is not on this payment request", HttpStatus.BAD_REQUEST.value(), requestId));
        if (warehouseInboundReceiptLineRepo.countActiveLinesByReceiptAndPrpol(receipt.getId(), prpolUuid) > 0) {
            throw new ApiException(ApiException.ErrorCode.CONFLICT, "This payment-request line is already on the receipt",
                    HttpStatus.CONFLICT.value(), requestId);
        }
        PurchaseOrderLineEntity pol = prpol.getPurchaseOrderLine();
        BigDecimal qtyExpected = pol != null && pol.getQuantity() != null ? pol.getQuantity() : BigDecimal.ZERO;
        BigDecimal tax = body.getTaxPercent() != null ? body.getTaxPercent()
                : (pol != null && pol.getTax() != null ? pol.getTax() : BigDecimal.ZERO);
        String username = RequestContext.getCurrentUsername();
        WarehouseInboundReceiptLineEntity lineEntity = new WarehouseInboundReceiptLineEntity();
        lineEntity.setReceipt(receipt);
        lineEntity.setPaymentRequestPurchaseOrderLine(prpol);
        lineEntity.setQuantityExpected(qtyExpected);
        lineEntity.setQuantityReceived(body.getQuantityReceived());
        lineEntity.setTaxPercent(tax);
        lineEntity.setLineNote(body.getLineNote());
        lineEntity.setCreatedBy(username);
        lineEntity.setUpdatedBy(username);
        lineEntity.setIsDeleted(false);
        warehouseInboundReceiptLineRepo.save(lineEntity);
        return toReceiptInfo(receipt.getId(), requestId);
    }

    @Transactional
    public WarehouseInboundReceiptInfo patchInboundLine(String receiptId, String lineId, WarehouseInboundLinePatchRequest body,
            String requestId) {
        if (body == null) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "Body is required",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }
        WarehouseInboundReceiptEntity receipt = loadReceiptForMutation(receiptId, requestId);
        if (!Constant.WAREHOUSE_INBOUND_STATUS_DRAFT.equals(receipt.getStatus())) {
            throw new ApiException(ApiException.ErrorCode.CONFLICT, "Lines can only be changed while receipt is DRAFT",
                    HttpStatus.CONFLICT.value(), requestId);
        }
        WarehouseInboundReceiptLineEntity line = loadReceiptLine(receiptId, lineId, requestId);
        if (body.getQuantityReceived() != null) {
            if (body.getQuantityReceived().compareTo(BigDecimal.ZERO) < 0) {
                throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "quantityReceived cannot be negative",
                        HttpStatus.BAD_REQUEST.value(), requestId);
            }
            line.setQuantityReceived(body.getQuantityReceived());
        }
        if (body.getTaxPercent() != null) {
            line.setTaxPercent(body.getTaxPercent());
        }
        if (body.getLineNote() != null) {
            line.setLineNote(body.getLineNote());
        }
        line.setUpdatedBy(RequestContext.getCurrentUsername());
        warehouseInboundReceiptLineRepo.save(line);
        return toReceiptInfo(receipt.getId(), requestId);
    }

    @Transactional
    public WarehouseInboundReceiptInfo reject(String receiptId, RejectPaymentRequest request, String requestId) {
        WarehouseInboundReceiptEntity receipt = loadReceiptForMutation(receiptId, requestId);
        ensureApprovingStatus(receipt, requestId);

        int level = resolveApprovalLevel(request == null ? null : request.getLevel(), receipt);
        WarehouseInboundReceiptApprovalEntity approval = approvalAtLevelOrThrow(receipt.getId(), level, requestId);
        if (!Constant.PAYMENT_APPROVAL_STATUS_PENDING.equals(approval.getStatus())) {
            throw new ApiException(ApiException.ErrorCode.CONFLICT, "This level was already processed",
                    HttpStatus.CONFLICT.value(), requestId);
        }

        UserEntity currentUser = findUserEntity(requestId);
        validateInboundApproverRole(approval.getApprovalRole(), requestId);
        approval.setStatus(Constant.PAYMENT_APPROVAL_STATUS_REJECTED);
        approval.setApprover(currentUser);
        approval.setApprovedAt(Instant.now());
        approval.setRejectionReason(request != null ? request.getReason() : null);
        approval.setNote(request != null ? request.getNote() : null);
        approval.setUpdatedBy(RequestContext.getCurrentUsername());
        warehouseInboundReceiptApprovalRepo.save(approval);

        receipt.setStatus(Constant.WAREHOUSE_INBOUND_STATUS_REJECTED);
        receipt.setUpdatedBy(RequestContext.getCurrentUsername());
        warehouseInboundReceiptRepo.save(receipt);

        log.info("Warehouse inbound rejected [receiptId={}, level={}, rid={}]", receipt.getId(), level, requestId);
        return toReceiptInfo(receipt.getId(), requestId);
    }

    @Transactional
    public WarehouseInboundReceiptInfo cancel(String receiptId, String requestId) {
        WarehouseInboundReceiptEntity receipt = loadReceiptForMutation(receiptId, requestId);
        if (!Constant.WAREHOUSE_INBOUND_STATUS_DRAFT.equals(receipt.getStatus())) {
            throw new ApiException(ApiException.ErrorCode.CONFLICT, "Only DRAFT receipts can be cancelled",
                    HttpStatus.CONFLICT.value(), requestId);
        }
        receipt.setStatus(Constant.WAREHOUSE_INBOUND_STATUS_CANCELLED);
        receipt.setUpdatedBy(RequestContext.getCurrentUsername());
        warehouseInboundReceiptRepo.save(receipt);
        log.info("Warehouse inbound cancelled [receiptId={}, rid={}]", receipt.getId(), requestId);
        return toReceiptInfo(receipt.getId(), requestId);
    }

    public List<WarehouseInboundReceiptInfo> listReceiptsForPaymentRequest(String paymentRequestId, String requestId) {
        UUID prId = parseUuid(paymentRequestId, "paymentRequestId", requestId);
        paymentRequestRepo.findByIdAndIsDeletedFalse(prId)
                .orElseThrow(() -> new ApiException(ApiException.ErrorCode.NOT_FOUND, "Payment request not found",
                        HttpStatus.NOT_FOUND.value(), requestId));
        return warehouseInboundReceiptRepo.findByPaymentRequestId(prId).stream()
                .map(r -> toReceiptInfo(r.getId(), requestId))
                .toList();
    }

    public WarehouseInboundReceiptInfo getReceipt(String receiptId, String requestId) {
        UUID rid = parseUuid(receiptId, "receiptId", requestId);
        warehouseInboundReceiptRepo.findByIdAndIsDeletedFalse(rid)
                .orElseThrow(() -> new ApiException(ApiException.ErrorCode.NOT_FOUND, "Warehouse inbound receipt not found",
                        HttpStatus.NOT_FOUND.value(), requestId));
        return toReceiptInfo(rid, requestId);
    }

    private WarehouseInboundReceiptEntity loadReceiptForMutation(String receiptId, String requestId) {
        UUID rid = parseUuid(receiptId, "receiptId", requestId);
        return warehouseInboundReceiptRepo.findByIdAndIsDeletedFalse(rid)
                .orElseThrow(() -> new ApiException(ApiException.ErrorCode.NOT_FOUND, "Warehouse inbound receipt not found",
                        HttpStatus.NOT_FOUND.value(), requestId));
    }

    private WarehouseInboundReceiptLineEntity loadReceiptLine(String receiptId, String lineId, String requestId) {
        UUID rid = parseUuid(receiptId, "receiptId", requestId);
        UUID lid = parseUuid(lineId, "lineId", requestId);
        return warehouseInboundReceiptLineRepo.findByIdAndReceiptId(lid, rid)
                .orElseThrow(() -> new ApiException(ApiException.ErrorCode.NOT_FOUND, "Inbound line not found",
                        HttpStatus.NOT_FOUND.value(), requestId));
    }

    private void seedApprovals(WarehouseInboundReceiptEntity receipt, int approvalLevels, List<String> customRoles,
            String requestId) {
        String username = RequestContext.getCurrentUsername();
        for (int level = 1; level <= approvalLevels; level++) {
            WarehouseInboundReceiptApprovalEntity row = new WarehouseInboundReceiptApprovalEntity();
            row.setReceipt(receipt);
            row.setApprovalLevel(level);
            row.setApprovalRole(resolveInboundRole(level, approvalLevels, customRoles, requestId));
            row.setStatus(Constant.PAYMENT_APPROVAL_STATUS_PENDING);
            row.setCreatedBy(username);
            row.setUpdatedBy(username);
            row.setIsDeleted(false);
            warehouseInboundReceiptApprovalRepo.save(row);
        }
    }

    /**
     * Default role ladder mirrors payment request (accountant chain + final approver). Override per level with {@code approvalRoles}.
     */
    private String resolveInboundRole(int level, int total, List<String> customRoles, String requestId) {
        if (customRoles != null && !customRoles.isEmpty()) {
            String r = customRoles.get(level - 1);
            if (r == null || r.isBlank()) {
                throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "approvalRoles entry must not be blank",
                        HttpStatus.BAD_REQUEST.value(), requestId);
            }
            return r.trim().toUpperCase(Locale.ROOT);
        }
        if (total == 1) {
            return "ADMIN";
        }
        if (total == 2) {
            return level == 1 ? "ACCOUNTANT" : "HEAD_ACCOUNTANT";
        }
        if (level == total) {
            return "FINAL_APPROVER";
        }
        if (level == 1) {
            return "ACCOUNTANT";
        }
        if (level == 2) {
            return "HEAD_ACCOUNTANT";
        }
        return "ACCOUNTANT_MANAGER";
    }

    private static void ensureApprovingStatus(WarehouseInboundReceiptEntity receipt, String requestId) {
        if (!Constant.WAREHOUSE_INBOUND_STATUS_SUBMITTED.equals(receipt.getStatus())) {
            throw new ApiException(ApiException.ErrorCode.CONFLICT, "Receipt is not awaiting approval",
                    HttpStatus.CONFLICT.value(), requestId);
        }
    }

    private static int resolveApprovalLevel(Integer explicitLevel, WarehouseInboundReceiptEntity receipt) {
        return explicitLevel == null ? receipt.getCurrentApprovalLevel() + 1 : explicitLevel;
    }

    private WarehouseInboundReceiptApprovalEntity approvalAtLevelOrThrow(UUID receiptId, int level, String requestId) {
        return warehouseInboundReceiptApprovalRepo.findByReceiptIdAndLevel(receiptId, level)
                .orElseThrow(() -> new ApiException(ApiException.ErrorCode.NOT_FOUND, "Approval level not found",
                        HttpStatus.NOT_FOUND.value(), requestId));
    }

    private UserEntity findUserEntity(String requestId) {
        String username = RequestContext.getCurrentUsername();
        return userRepo.findOneByUsername(username)
                .orElseThrow(() -> new ApiException(ApiException.ErrorCode.NOT_FOUND, "User not found",
                        HttpStatus.NOT_FOUND.value(), requestId));
    }

    private void validateInboundApproverRole(String approvalRole, String requestId) {
        UserInfo currentUserInfo = getCurrentUserInfoFromRedis(requestId);
        if (currentUserInfo.getRoles() == null || currentUserInfo.getRoles().isEmpty()) {
            throw new ApiException(ApiException.ErrorCode.FORBIDDEN,
                    "Current user does not have role for this approval level",
                    HttpStatus.FORBIDDEN.value(), requestId);
        }
        Set<String> roleCodes = currentUserInfo.getRoles().stream()
                .map(RoleInfo::getCode)
                .filter(Objects::nonNull)
                .map(s -> s.trim().toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());
        String role = approvalRole.trim().toUpperCase(Locale.ROOT);
        if (roleCodes.contains(role)) {
            return;
        }
        if ("HEAD_ACCOUNTANT".equals(role)
                && (roleCodes.contains("ACCOUNTANT_MANAGER") || roleCodes.contains("HEAD_ACCOUNTANT"))) {
            return;
        }
        if ("FINAL_APPROVER".equals(role) && (roleCodes.contains("ADMIN") || roleCodes.contains("ACCOUNTANT_MANAGER"))) {
            return;
        }
        if ("ACCOUNTANT".equals(role) && roleCodes.contains("ACCOUNTANT")) {
            return;
        }
        throw new ApiException(ApiException.ErrorCode.FORBIDDEN,
                "Current user does not have role for this approval level",
                HttpStatus.FORBIDDEN.value(), requestId);
    }

    private UserInfo getCurrentUserInfoFromRedis(String requestId) {
        String username = RequestContext.getCurrentUsername();
        Object raw = redisService.get(username);
        if (raw == null) {
            throw new ApiException(ApiException.ErrorCode.UNAUTHORIZED, "User info not found in session",
                    HttpStatus.UNAUTHORIZED.value(), requestId);
        }
        return objectMapper.convertValue(raw, UserInfo.class);
    }

    private WarehouseInboundReceiptInfo toReceiptInfo(UUID receiptId, String requestId) {
        WarehouseInboundReceiptEntity receipt = warehouseInboundReceiptRepo.findByIdAndIsDeletedFalse(receiptId)
                .orElseThrow(() -> new ApiException(ApiException.ErrorCode.NOT_FOUND, "Warehouse inbound receipt not found",
                        HttpStatus.NOT_FOUND.value(), requestId));

        List<WarehouseInboundReceiptLineEntity> lineEntities = warehouseInboundReceiptLineRepo.findByReceiptId(receiptId);
        List<PaymentFileUploadInfo> attachments = fileService.listUploadedFilesForWarehouseInboundReceipt(receiptId.toString(), requestId);
        List<PaymentRequestApprovalInfo> approvals = List.of();
        if (receipt.getApprovalLevels() != null && receipt.getApprovalLevels() > 0) {
            approvals = warehouseInboundReceiptApprovalRepo.findByReceiptId(receiptId).stream()
                    .map(WarehouseInboundService::toApprovalInfo)
                    .toList();
        }

        WarehouseInboundReceiptInfo info = new WarehouseInboundReceiptInfo();
        info.setId(receipt.getId().toString());
        info.setPaymentRequestId(receipt.getPaymentRequest().getId().toString());
        info.setStatus(receipt.getStatus());
        info.setApprovalLevels(receipt.getApprovalLevels());
        info.setCurrentApprovalLevel(receipt.getCurrentApprovalLevel());
        info.setCurrency(receipt.getCurrency());
        info.setExchangeRate(receipt.getExchangeRate());
        info.setFeeAmount(receipt.getFeeAmount());
        info.setRealBillAmount(receipt.getRealBillAmount());
        info.setBillOnPaperAmount(receipt.getBillOnPaperAmount());
        info.setNote(receipt.getNote());
        info.setInventoryPostedAt(receipt.getInventoryPostedAt() == null ? null : receipt.getInventoryPostedAt().toString());
        info.setCreatedAt(receipt.getCreatedAt() != null ? receipt.getCreatedAt().toString() : null);
        info.setLines(lineEntities.stream().map(this::toReceiptLineInfo).toList());
        info.setAttachments(attachments);
        info.setApprovals(approvals);
        return info;
    }

    private static PaymentRequestApprovalInfo toApprovalInfo(WarehouseInboundReceiptApprovalEntity approval) {
        PaymentRequestApprovalInfo approvalInfo = new PaymentRequestApprovalInfo();
        approvalInfo.setId(approval.getId().toString());
        approvalInfo.setLevel(approval.getApprovalLevel());
        approvalInfo.setRole(approval.getApprovalRole());
        approvalInfo.setApproverId(approval.getApprover() == null ? null : approval.getApprover().getId().toString());
        approvalInfo.setStatus(approval.getStatus());
        approvalInfo.setApprovedAt(approval.getApprovedAt() == null ? null : approval.getApprovedAt().toString());
        approvalInfo.setRejectionReason(approval.getRejectionReason());
        approvalInfo.setNote(approval.getNote());
        approvalInfo.setUpdatedBy(approval.getUpdatedBy());
        return approvalInfo;
    }

    private WarehouseInboundReceiptLineInfo toReceiptLineInfo(WarehouseInboundReceiptLineEntity e) {
        WarehouseInboundReceiptLineInfo i = new WarehouseInboundReceiptLineInfo();
        i.setId(e.getId().toString());
        i.setPaymentRequestPurchaseOrderLineId(e.getPaymentRequestPurchaseOrderLine().getId().toString());
        var pol = e.getPaymentRequestPurchaseOrderLine().getPurchaseOrderLine();
        if (pol != null && pol.getProduct() != null) {
            i.setProductId(pol.getProduct().getId().toString());
            i.setProductName(pol.getProduct().getName());
        }
        i.setQuantityExpected(e.getQuantityExpected());
        i.setQuantityReceived(e.getQuantityReceived());
        i.setTaxPercent(e.getTaxPercent());
        i.setLineNote(e.getLineNote());
        return i;
    }

    private WarehouseInboundSearchHit toSearchHit(PaymentRequestEntity pr) {
        WarehouseInboundSearchHit h = new WarehouseInboundSearchHit();
        h.setPaymentRequestId(pr.getId().toString());
        h.setRequestNumber(pr.getRequestNumber());
        h.setStatus(pr.getStatus());
        h.setNotes(pr.getNotes());
        if (pr.getVendor() != null) {
            h.setVendorCode(pr.getVendor().getCode());
            h.setVendorName(pr.getVendor().getName());
        }
        return h;
    }

    private List<PurchaseOrderLineEntity> findPurchaseOrderLinesByPaperType(String paperType, String paperCode, String requestId) {
        String pt = paperType.trim().toUpperCase();
        return switch (pt) {
            case "QUOTE" -> purchaseOrderLineRepo.findByQuotePaperCode(paperCode);
            case "INVOICE" -> purchaseOrderLineRepo.findByInvoicePaperCode(paperCode);
            case "TRACK_ID", "TRACKID", "TRACE_ID", "TRACEID" -> purchaseOrderLineRepo.findByTrackIdPaperCode(paperCode);
            case "RECEIPT_WAREHOUSE", "RECEIPTWAREHOUSE" -> purchaseOrderLineRepo.findByReceiptWarehousePaperCode(paperCode);
            case "BILL_OF_LADDING", "BILLOFLADDING" -> purchaseOrderLineRepo.findByBillOfLaddingPaperCode(paperCode);
            default -> throw new ApiException(ApiException.ErrorCode.BAD_REQUEST,
                    "Invalid paperType. Supported: QUOTE, INVOICE, TRACK_ID, RECEIPT_WAREHOUSE, BILL_OF_LADDING",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        };
    }

    private static String normalize(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static UUID parseUuid(String raw, String field, String requestId) {
        try {
            return UUID.fromString(raw.trim());
        } catch (Exception e) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, field + " must be a valid UUID",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }
    }

    private static List<UUID> parseFileIds(List<String> raw, String requestId) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        List<UUID> out = new ArrayList<>(raw.size());
        for (String s : raw) {
            if (s == null || s.isBlank()) {
                continue;
            }
            try {
                out.add(UUID.fromString(s.trim()));
            } catch (IllegalArgumentException e) {
                throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "attachedFileIds must contain valid UUID strings",
                        HttpStatus.BAD_REQUEST.value(), requestId);
            }
        }
        return out;
    }
}
