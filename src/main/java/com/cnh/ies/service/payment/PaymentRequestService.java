package com.cnh.ies.service.payment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.cnh.ies.constant.Constant;
import com.cnh.ies.entity.auth.UserEntity;
import com.cnh.ies.entity.payment.PaymentRequestApprovalEntity;
import com.cnh.ies.entity.payment.PaymentRequestEntity;
import com.cnh.ies.entity.payment.PaymentRequestExtraFeeEntity;
import com.cnh.ies.entity.payment.PaymentRequestItemDocumentEntity;
import com.cnh.ies.entity.payment.PaymentRequestPurchaseOrderLineEntity;
import com.cnh.ies.entity.purchaseorder.PurchaseOrderLineEntity;
import com.cnh.ies.exception.ApiException;
import com.cnh.ies.model.general.ListDataModel;
import com.cnh.ies.model.general.PaginationModel;
import com.cnh.ies.model.payment.ApprovePaymentRequest;
import com.cnh.ies.model.payment.CreateOrUpdatePaymentRequest;
import com.cnh.ies.model.payment.MarkPaymentPaidRequest;
import com.cnh.ies.model.payment.PaymentBankInfoObject;
import com.cnh.ies.model.payment.PaymentBankNoteObject;
import com.cnh.ies.model.payment.PaymentFileObject;
import com.cnh.ies.model.payment.PaymentRequestApprovalInfo;
import com.cnh.ies.model.payment.PaymentRequestFeeInfo;
import com.cnh.ies.model.payment.PaymentRequestFeeRequest;
import com.cnh.ies.model.payment.PaymentRequestInfo;
import com.cnh.ies.model.payment.PaymentRequestItemRequest;
import com.cnh.ies.model.payment.PaymentRequestLineInfo;
import com.cnh.ies.model.payment.RejectPaymentRequest;
import com.cnh.ies.model.user.RoleInfo;
import com.cnh.ies.model.user.UserInfo;
import com.cnh.ies.repository.auth.UserRepo;
import com.cnh.ies.repository.payment.PaymentRequestApprovalRepo;
import com.cnh.ies.repository.payment.PaymentRequestExtraFeeRepo;
import com.cnh.ies.repository.payment.PaymentRequestItemDocumentRepo;
import com.cnh.ies.repository.payment.PaymentRequestPurchaseOrderLineRepo;
import com.cnh.ies.repository.payment.PaymentRequestRepo;
import com.cnh.ies.repository.purchaseorder.PurchaseOrderLineRepo;
import com.cnh.ies.service.redis.RedisService;
import com.cnh.ies.util.RequestContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentRequestService {
    private static final Set<String> VALID_DOCUMENT_TYPES = Set.of(
            "invoice", "quote", "bill_of_ladding", "track_id", "receipt_warehouse");
    private static final Map<String, Set<String>> ALLOWED_STATUS_TRANSITIONS = Map.of(
            Constant.PAYMENT_REQUEST_STATUS_DRAFT, Set.of(
                    Constant.PAYMENT_REQUEST_STATUS_PENDING_ACCOUNTANT_APPROVAL,
                    Constant.PAYMENT_REQUEST_STATUS_CANCELLED),
            Constant.PAYMENT_REQUEST_STATUS_REJECTED, Set.of(
                    Constant.PAYMENT_REQUEST_STATUS_PENDING_ACCOUNTANT_APPROVAL,
                    Constant.PAYMENT_REQUEST_STATUS_CANCELLED),
            Constant.PAYMENT_REQUEST_STATUS_PENDING_ACCOUNTANT_APPROVAL, Set.of(
                    Constant.PAYMENT_REQUEST_STATUS_PENDING_HEAD_ACCOUNTANT_APPROVAL,
                    Constant.PAYMENT_REQUEST_STATUS_REJECTED),
            Constant.PAYMENT_REQUEST_STATUS_PENDING_HEAD_ACCOUNTANT_APPROVAL, Set.of(
                    Constant.PAYMENT_REQUEST_STATUS_PENDING_FINAL_APPROVAL,
                    Constant.PAYMENT_REQUEST_STATUS_APPROVED,
                    Constant.PAYMENT_REQUEST_STATUS_REJECTED),
            Constant.PAYMENT_REQUEST_STATUS_PENDING_FINAL_APPROVAL, Set.of(
                    Constant.PAYMENT_REQUEST_STATUS_APPROVED,
                    Constant.PAYMENT_REQUEST_STATUS_REJECTED),
            Constant.PAYMENT_REQUEST_STATUS_APPROVED, Set.of(
                    Constant.PAYMENT_REQUEST_STATUS_PARTIALLY_PAID,
                    Constant.PAYMENT_REQUEST_STATUS_PAID),
            Constant.PAYMENT_REQUEST_STATUS_PARTIALLY_PAID, Set.of(
                    Constant.PAYMENT_REQUEST_STATUS_PARTIALLY_PAID,
                    Constant.PAYMENT_REQUEST_STATUS_PAID));

    private final PaymentRequestRepo paymentRequestRepo;
    private final PaymentRequestPurchaseOrderLineRepo paymentRequestPurchaseOrderLineRepo;
    private final PaymentRequestExtraFeeRepo paymentRequestExtraFeeRepo;
    private final PaymentRequestItemDocumentRepo paymentRequestItemDocumentRepo;
    private final PaymentRequestApprovalRepo paymentRequestApprovalRepo;
    private final PurchaseOrderLineRepo purchaseOrderLineRepo;
    private final UserRepo userRepo;
    private final RedisService redisService;
    private final ObjectMapper objectMapper;
    private final PaymentRequestNumberService paymentRequestNumberService;

    public ListDataModel<PaymentRequestInfo> getAllPaymentRequests(String requestId, Integer page, Integer limit) {
        Page<PaymentRequestEntity> requests = paymentRequestRepo.findAllAndIsDeletedFalse(PageRequest.of(page, limit));
        List<PaymentRequestInfo> data = requests.stream().map(this::toInfoWithoutChildren).collect(Collectors.toList());
        PaginationModel pagination = PaginationModel.builder()
                .page(page)
                .limit(limit)
                .total(requests.getTotalElements())
                .totalPage(requests.getTotalPages())
                .build();
        return ListDataModel.<PaymentRequestInfo>builder().data(data).pagination(pagination).build();
    }

    public PaymentRequestInfo getById(String id, String requestId) {
        PaymentRequestEntity paymentRequest = findPaymentRequest(id, requestId);
        return toInfo(paymentRequest);
    }

    @Transactional
    public PaymentRequestInfo createOrUpdate(CreateOrUpdatePaymentRequest request, String requestId) {
        validateCreateRequest(request, requestId);

        List<PaymentRequestItemRequest> itemRequests = request.getItems();
        List<UUID> lineIds = itemRequests.stream().map(i -> UUID.fromString(i.getPurchaseOrderLineId())).toList();
        List<PurchaseOrderLineEntity> poLines = purchaseOrderLineRepo.findByIdInAndIsDeletedFalse(lineIds);
        if (poLines.size() != lineIds.size()) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "Some purchase order lines are invalid",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }

        validateLineDocumentSelection(itemRequests, poLines, requestId);
        validateSameVendor(poLines, requestId);
        BigDecimal requestedAmount = itemRequests.stream()
                .map(PaymentRequestItemRequest::getRequestedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal feeAmount = calculateFeeAmount(request.getFees());

        PaymentRequestEntity entity = upsertPaymentRequestMain(request, poLines.get(0), requestedAmount, feeAmount, requestId);
        replaceItems(entity, itemRequests, poLines);
        replaceFees(entity, request.getFees());
        recalculateAmountsBeforeSave(entity, requestId);
        rebuildApprovalsIfNeeded(entity, request.getApprovalLevels());
        paymentRequestRepo.save(entity);

        return toInfo(entity);
    }

    @Transactional
    public PaymentRequestInfo submit(String id, String requestId) {
        PaymentRequestEntity paymentRequest = findPaymentRequest(id, requestId);
        ensureDraft(paymentRequest, requestId);
        recalculateAmountsBeforeSave(paymentRequest, requestId);
        transitionStatus(paymentRequest, Constant.PAYMENT_REQUEST_STATUS_PENDING_ACCOUNTANT_APPROVAL, requestId);
        paymentRequest.setCurrentApprovalLevel(0);
        paymentRequest.setUpdatedBy(RequestContext.getCurrentUsername());
        paymentRequestRepo.save(paymentRequest);
        return toInfo(paymentRequest);
    }

    @Transactional
    public PaymentRequestInfo cancel(String id, String requestId) {
        PaymentRequestEntity paymentRequest = findPaymentRequest(id, requestId);
        if (!Constant.PAYMENT_REQUEST_STATUS_DRAFT.equals(paymentRequest.getStatus())
                && !Constant.PAYMENT_REQUEST_STATUS_REJECTED.equals(paymentRequest.getStatus())) {
            throw new ApiException(ApiException.ErrorCode.CONFLICT, "Only DRAFT/REJECTED can be cancelled",
                    HttpStatus.CONFLICT.value(), requestId);
        }
        recalculateAmountsBeforeSave(paymentRequest, requestId);
        transitionStatus(paymentRequest, Constant.PAYMENT_REQUEST_STATUS_CANCELLED, requestId);
        paymentRequest.setUpdatedBy(RequestContext.getCurrentUsername());
        paymentRequestRepo.save(paymentRequest);
        return toInfo(paymentRequest);
    }

    @Transactional
    public PaymentRequestInfo approve(String id, ApprovePaymentRequest request, String requestId) {
        PaymentRequestEntity paymentRequest = findPaymentRequest(id, requestId);
        ensureApprovingStatus(paymentRequest, requestId);
        recalculateAmountsBeforeSave(paymentRequest, requestId);

        int level = request.getLevel() == null ? paymentRequest.getCurrentApprovalLevel() + 1 : request.getLevel();
        if (level <= 0 || level > paymentRequest.getApprovalLevels()) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "Invalid approval level", HttpStatus.BAD_REQUEST.value(),
                    requestId);
        }
        PaymentRequestApprovalEntity approval = paymentRequestApprovalRepo
                .findByPaymentRequestIdAndLevel(paymentRequest.getId(), level)
                .orElseThrow(() -> new ApiException(ApiException.ErrorCode.NOT_FOUND, "Approval level not found",
                        HttpStatus.NOT_FOUND.value(), requestId));
        if (!Constant.PAYMENT_APPROVAL_STATUS_PENDING.equals(approval.getStatus())) {
            throw new ApiException(ApiException.ErrorCode.CONFLICT, "This level was already processed",
                    HttpStatus.CONFLICT.value(), requestId);
        }

        UserEntity currentUser = findUser(requestId);
        validateApproverRole(approval.getApprovalRole(), requestId);
        approval.setStatus(Constant.PAYMENT_APPROVAL_STATUS_APPROVED);
        approval.setApprover(currentUser);
        approval.setApprovedAt(Instant.now());
        approval.setNote(request.getNote());
        approval.setUpdatedBy(RequestContext.getCurrentUsername());
        paymentRequestApprovalRepo.save(approval);

        paymentRequest.setCurrentApprovalLevel(level);
        transitionStatus(paymentRequest, nextStatusAfterApproval(level, paymentRequest.getApprovalLevels()), requestId);
        paymentRequest.setUpdatedBy(RequestContext.getCurrentUsername());
        paymentRequestRepo.save(paymentRequest);
        return toInfo(paymentRequest);
    }

    @Transactional
    public PaymentRequestInfo reject(String id, RejectPaymentRequest request, String requestId) {
        PaymentRequestEntity paymentRequest = findPaymentRequest(id, requestId);
        ensureApprovingStatus(paymentRequest, requestId);
        recalculateAmountsBeforeSave(paymentRequest, requestId);

        int level = request.getLevel() == null ? paymentRequest.getCurrentApprovalLevel() + 1 : request.getLevel();
        PaymentRequestApprovalEntity approval = paymentRequestApprovalRepo
                .findByPaymentRequestIdAndLevel(paymentRequest.getId(), level)
                .orElseThrow(() -> new ApiException(ApiException.ErrorCode.NOT_FOUND, "Approval level not found",
                        HttpStatus.NOT_FOUND.value(), requestId));

        UserEntity currentUser = findUser(requestId);
        validateApproverRole(approval.getApprovalRole(), requestId);
        approval.setStatus(Constant.PAYMENT_APPROVAL_STATUS_REJECTED);
        approval.setApprover(currentUser);
        approval.setApprovedAt(Instant.now());
        approval.setRejectionReason(request.getReason());
        approval.setNote(request.getNote());
        approval.setUpdatedBy(RequestContext.getCurrentUsername());
        paymentRequestApprovalRepo.save(approval);

        transitionStatus(paymentRequest, Constant.PAYMENT_REQUEST_STATUS_REJECTED, requestId);
        paymentRequest.setUpdatedBy(RequestContext.getCurrentUsername());
        paymentRequestRepo.save(paymentRequest);
        return toInfo(paymentRequest);
    }

    @Transactional
    public PaymentRequestInfo markPaid(String id, MarkPaymentPaidRequest request, String requestId) {
        PaymentRequestEntity paymentRequest = findPaymentRequest(id, requestId);
        recalculateAmountsBeforeSave(paymentRequest, requestId);
        if (!Constant.PAYMENT_REQUEST_STATUS_APPROVED.equals(paymentRequest.getStatus())
                && !Constant.PAYMENT_REQUEST_STATUS_PARTIALLY_PAID.equals(paymentRequest.getStatus())) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "Payment request is not ready to be paid",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }

        if (request.getPaidAmount() == null || request.getPaidAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "Paid amount must be greater than 0",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }

        BigDecimal exchangeRate = "VND".equalsIgnoreCase(paymentRequest.getCurrency())
                ? BigDecimal.ONE
                : request.getExchangeRate();
        if (exchangeRate == null || exchangeRate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "Exchange rate must be greater than 0",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }

        BigDecimal newPaidAmount = paymentRequest.getPaidAmount().add(request.getPaidAmount());
        if (newPaidAmount.compareTo(paymentRequest.getTotalAmount()) > 0) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "Paid amount exceeds payment total",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }

        paymentRequest.setPaidAmount(newPaidAmount);
        paymentRequest.setPaidAmountVnd(paymentRequest.getPaidAmountVnd().add(toVnd(request.getPaidAmount(), exchangeRate)));
        paymentRequest.setExchangeRate(exchangeRate);
        paymentRequest.setBankNote(writeJson(request.getBankNote(), requestId));
        paymentRequest.setPaidBy(findUser(requestId));
        paymentRequest.setPaidAt(Instant.now());
        transitionStatus(paymentRequest,
                newPaidAmount.compareTo(paymentRequest.getTotalAmount()) == 0
                        ? Constant.PAYMENT_REQUEST_STATUS_PAID
                        : Constant.PAYMENT_REQUEST_STATUS_PARTIALLY_PAID,
                requestId);
        paymentRequest.setUpdatedBy(RequestContext.getCurrentUsername());
        recalculateAmountsBeforeSave(paymentRequest, requestId);
        paymentRequestRepo.save(paymentRequest);
        return toInfo(paymentRequest);
    }

    private PaymentRequestEntity upsertPaymentRequestMain(CreateOrUpdatePaymentRequest request,
            PurchaseOrderLineEntity firstLine, BigDecimal requestedAmount, BigDecimal feeAmount, String requestId) {
        PaymentRequestEntity entity;
        if (request.getId() == null || request.getId().isBlank()) {
            entity = new PaymentRequestEntity();
            entity.setRequestNumber(paymentRequestNumberService.generateRequestNumber());
            entity.setRequestDate(request.getRequestDate() == null ? Instant.now() : request.getRequestDate());
            entity.setStatus(Constant.PAYMENT_REQUEST_STATUS_DRAFT);
            entity.setCurrentApprovalLevel(0);
            entity.setCreatedBy(RequestContext.getCurrentUsername());
        } else {
            entity = findPaymentRequest(request.getId(), requestId);
            if (!Constant.PAYMENT_REQUEST_STATUS_DRAFT.equals(entity.getStatus())
                    && !Constant.PAYMENT_REQUEST_STATUS_REJECTED.equals(entity.getStatus())) {
                throw new ApiException(ApiException.ErrorCode.CONFLICT,
                        "Only DRAFT/REJECTED payment request can be updated", HttpStatus.CONFLICT.value(), requestId);
            }
            if (request.getRequestDate() != null) {
                entity.setRequestDate(request.getRequestDate());
            }
        }

        entity.setRequestor(findUser(requestId));
        entity.setVendor(firstLine.getVendor());
        String normalizedCurrency = normalizeCurrency(request.getCurrency());
        BigDecimal exchangeRate = resolveExchangeRate(normalizedCurrency, request.getExchangeRate(), requestId);
        entity.setCurrency(normalizedCurrency);
        entity.setExchangeRate(exchangeRate);
        BigDecimal requestedAmountVnd = toVnd(requestedAmount, exchangeRate);
        BigDecimal feeAmountVnd = toVnd(feeAmount, exchangeRate);
        entity.setPaidPercentage(request.getPaidPercentage() == null ? BigDecimal.ZERO : request.getPaidPercentage());
        entity.setPurpose(request.getPurpose());
        entity.setNotes(request.getNotes());
        entity.setPapers(writeJson(request.getPapers(), requestId));
        entity.setBankInfo(writeJson(request.getBankInfo(), requestId));
        entity.setApprovalLevels(request.getApprovalLevels());
        entity.setRequestedAmount(requestedAmount);
        entity.setRequestedAmountVnd(requestedAmountVnd);
        entity.setFeeAmount(feeAmount);
        entity.setFeeAmountVnd(feeAmountVnd);
        BigDecimal totalAmount = requestedAmount.add(feeAmount);
        BigDecimal totalAmountVnd = requestedAmountVnd.add(feeAmountVnd);
        entity.setTotalAmount(totalAmount);
        entity.setTotalAmountVnd(totalAmountVnd);
        entity.setAmount(totalAmount);
        entity.setUpdatedBy(RequestContext.getCurrentUsername());
        return paymentRequestRepo.save(entity);
    }

    private void replaceItems(PaymentRequestEntity paymentRequest, List<PaymentRequestItemRequest> itemRequests,
            List<PurchaseOrderLineEntity> poLines) {
        List<PaymentRequestPurchaseOrderLineEntity> existing = paymentRequestPurchaseOrderLineRepo
                .findByPaymentRequestId(paymentRequest.getId());
        List<UUID> existingItemIds = existing.stream().map(PaymentRequestPurchaseOrderLineEntity::getId).toList();
        if (!existingItemIds.isEmpty()) {
            List<PaymentRequestItemDocumentEntity> existingDocs = paymentRequestItemDocumentRepo
                    .findByPaymentRequestItemIds(existingItemIds);
            existingDocs.forEach(doc -> doc.setIsDeleted(true));
            paymentRequestItemDocumentRepo.saveAll(existingDocs);
            paymentRequestItemDocumentRepo.flush();
        }
        existing.forEach(item -> item.setIsDeleted(true));
        paymentRequestPurchaseOrderLineRepo.saveAll(existing);
        // Ensure old active rows are soft-deleted in DB before inserting replacement rows
        // to avoid hitting unique partial index uq_pr_po_line_active.
        paymentRequestPurchaseOrderLineRepo.flush();

        for (PaymentRequestItemRequest itemRequest : itemRequests) {
            PurchaseOrderLineEntity line = poLines.stream()
                    .filter(it -> it.getId().toString().equals(itemRequest.getPurchaseOrderLineId()))
                    .findFirst()
                    .orElseThrow();
            PaymentRequestPurchaseOrderLineEntity item = new PaymentRequestPurchaseOrderLineEntity();
            item.setPaymentRequest(paymentRequest);
            item.setPurchaseOrderLine(line);
            item.setSelectedDocuments(normalizeDocuments(itemRequest.getSelectedDocumentTypes()));
            item.setRequestedAmount(itemRequest.getRequestedAmount());
            item.setPaidAmount(BigDecimal.ZERO);
            item.setNote(itemRequest.getNote());
            item.setCreatedBy(RequestContext.getCurrentUsername());
            item.setUpdatedBy(RequestContext.getCurrentUsername());
            PaymentRequestPurchaseOrderLineEntity savedItem = paymentRequestPurchaseOrderLineRepo.save(item);
            for (String documentType : normalizeDocumentTypes(itemRequest.getSelectedDocumentTypes())) {
                PaymentRequestItemDocumentEntity itemDocument = new PaymentRequestItemDocumentEntity();
                itemDocument.setPaymentRequestItem(savedItem);
                itemDocument.setDocumentType(documentType);
                itemDocument.setCreatedBy(RequestContext.getCurrentUsername());
                itemDocument.setUpdatedBy(RequestContext.getCurrentUsername());
                paymentRequestItemDocumentRepo.save(itemDocument);
            }
        }
    }

    private void replaceFees(PaymentRequestEntity paymentRequest, List<PaymentRequestFeeRequest> feeRequests) {
        List<PaymentRequestExtraFeeEntity> existing = paymentRequestExtraFeeRepo.findByPaymentRequestId(paymentRequest.getId());
        existing.forEach(fee -> fee.setIsDeleted(true));
        paymentRequestExtraFeeRepo.saveAll(existing);
        if (feeRequests == null || feeRequests.isEmpty()) {
            return;
        }
        for (PaymentRequestFeeRequest feeRequest : feeRequests) {
            PaymentRequestExtraFeeEntity fee = new PaymentRequestExtraFeeEntity();
            fee.setPaymentRequest(paymentRequest);
            fee.setFeeName(feeRequest.getFeeName());
            fee.setFeeType(feeRequest.getFeeType());
            fee.setAmount(feeRequest.getAmount() == null ? BigDecimal.ZERO : feeRequest.getAmount());
            fee.setNote(feeRequest.getNote());
            fee.setCreatedBy(RequestContext.getCurrentUsername());
            fee.setUpdatedBy(RequestContext.getCurrentUsername());
            paymentRequestExtraFeeRepo.save(fee);
        }
    }

    private void rebuildApprovalsIfNeeded(PaymentRequestEntity paymentRequest, Integer approvalLevels) {
        List<PaymentRequestApprovalEntity> existing = paymentRequestApprovalRepo.findByPaymentRequestId(paymentRequest.getId());
        if (!existing.isEmpty() && existing.size() == approvalLevels) {
            return;
        }
        existing.forEach(approval -> approval.setIsDeleted(true));
        paymentRequestApprovalRepo.saveAll(existing);

        for (int level = 1; level <= approvalLevels; level++) {
            PaymentRequestApprovalEntity approval = new PaymentRequestApprovalEntity();
            approval.setPaymentRequest(paymentRequest);
            approval.setApprovalLevel(level);
            approval.setApprovalRole(getApprovalRoleByLevel(level, approvalLevels));
            approval.setStatus(Constant.PAYMENT_APPROVAL_STATUS_PENDING);
            approval.setCreatedBy(RequestContext.getCurrentUsername());
            approval.setUpdatedBy(RequestContext.getCurrentUsername());
            paymentRequestApprovalRepo.save(approval);
        }
    }

    private String getApprovalRoleByLevel(int level, int approvalLevels) {
        if (approvalLevels == 2) {
            return level == 1 ? "ACCOUNTANT" : "HEAD_ACCOUNTANT";
        }
        if (level == 1) {
            return "ACCOUNTANT";
        }
        if (level == 2) {
            return "HEAD_ACCOUNTANT";
        }
        return "FINAL_APPROVER";
    }

    private String nextStatusAfterApproval(int approvedLevel, int approvalLevels) {
        if (approvedLevel >= approvalLevels) {
            return Constant.PAYMENT_REQUEST_STATUS_APPROVED;
        }
        if (approvedLevel == 1) {
            return Constant.PAYMENT_REQUEST_STATUS_PENDING_HEAD_ACCOUNTANT_APPROVAL;
        }
        return Constant.PAYMENT_REQUEST_STATUS_PENDING_FINAL_APPROVAL;
    }

    private void ensureDraft(PaymentRequestEntity paymentRequest, String requestId) {
        if (!Constant.PAYMENT_REQUEST_STATUS_DRAFT.equals(paymentRequest.getStatus())
                && !Constant.PAYMENT_REQUEST_STATUS_REJECTED.equals(paymentRequest.getStatus())) {
            throw new ApiException(ApiException.ErrorCode.CONFLICT, "Only DRAFT/REJECTED can be submitted",
                    HttpStatus.CONFLICT.value(), requestId);
        }
    }

    private void ensureApprovingStatus(PaymentRequestEntity paymentRequest, String requestId) {
        Set<String> allowed = Set.of(
                Constant.PAYMENT_REQUEST_STATUS_SUBMITTED,
                Constant.PAYMENT_REQUEST_STATUS_PENDING_ACCOUNTANT_APPROVAL,
                Constant.PAYMENT_REQUEST_STATUS_PENDING_HEAD_ACCOUNTANT_APPROVAL,
                Constant.PAYMENT_REQUEST_STATUS_PENDING_FINAL_APPROVAL);
        if (!allowed.contains(paymentRequest.getStatus())) {
            throw new ApiException(ApiException.ErrorCode.CONFLICT, "Payment request is not in approving stage",
                    HttpStatus.CONFLICT.value(), requestId);
        }
    }

    private void validateCreateRequest(CreateOrUpdatePaymentRequest request, String requestId) {
        if (request.getRequestorId() == null || request.getRequestorId().isBlank()) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "requestorId is required",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "At least one purchase order line is required",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }
        if (request.getApprovalLevels() == null || request.getApprovalLevels() < 2 || request.getApprovalLevels() > 3) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "approvalLevels must be 2 or 3",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }
        List<String> lineIds = request.getItems().stream().map(PaymentRequestItemRequest::getPurchaseOrderLineId).toList();
        if (lineIds.size() != new LinkedHashSet<>(lineIds).size()) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "Duplicate purchaseOrderLineId in items",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }
        String normalizedCurrency = normalizeCurrency(request.getCurrency());
        resolveExchangeRate(normalizedCurrency, request.getExchangeRate(), requestId);
    }

    private String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return "VND";
        }
        return currency.trim().toUpperCase();
    }

    private BigDecimal resolveExchangeRate(String currency, BigDecimal exchangeRate, String requestId) {
        if ("VND".equalsIgnoreCase(currency)) {
            return BigDecimal.ONE;
        }
        if (exchangeRate == null || exchangeRate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST,
                    "exchangeRate is required and must be > 0 for non-VND currency",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }
        return exchangeRate;
    }

    private void validateLineDocumentSelection(List<PaymentRequestItemRequest> itemRequests, List<PurchaseOrderLineEntity> poLines,
            String requestId) {
        for (PaymentRequestItemRequest item : itemRequests) {
            if (item.getSelectedDocumentTypes() == null
                    || item.getSelectedDocumentTypes().isEmpty()
                    || item.getSelectedDocumentTypes().size() > 3) {
                throw new ApiException(ApiException.ErrorCode.BAD_REQUEST,
                        "Each line must select 1 to 3 documents", HttpStatus.BAD_REQUEST.value(), requestId);
            }
            if (item.getRequestedAmount() == null || item.getRequestedAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "requestedAmount must be > 0",
                        HttpStatus.BAD_REQUEST.value(), requestId);
            }
            PurchaseOrderLineEntity line = poLines.stream()
                    .filter(it -> it.getId().toString().equals(item.getPurchaseOrderLineId()))
                    .findFirst()
                    .orElseThrow(() -> new ApiException(ApiException.ErrorCode.NOT_FOUND, "Purchase order line not found",
                            HttpStatus.NOT_FOUND.value(), requestId));
            for (String doc : item.getSelectedDocumentTypes()) {
                if (!VALID_DOCUMENT_TYPES.contains(doc)) {
                    throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "Invalid document type: " + doc,
                            HttpStatus.BAD_REQUEST.value(), requestId);
                }
                validateDocumentExists(line, doc, requestId);
            }
        }
    }

    private void validateDocumentExists(PurchaseOrderLineEntity line, String documentType, String requestId) {
        String value;
        switch (documentType) {
            case "invoice" -> value = line.getInvoice();
            case "quote" -> value = line.getQuote();
            case "billOfLadding" -> value = line.getBillOfLadding();
            case "trackId" -> value = line.getTrackId();
            case "receiptWarehouse" -> value = line.getReceiptWarehouse();
            default -> value = null;
        }
        if (value == null || value.isBlank()) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST,
                    "Purchase order line missing document: " + documentType, HttpStatus.BAD_REQUEST.value(), requestId);
        }
    }

    private void validateSameVendor(List<PurchaseOrderLineEntity> poLines, String requestId) {
        Set<UUID> vendorIds = poLines.stream()
                .map(PurchaseOrderLineEntity::getVendor)
                .map(v -> v == null ? null : v.getId())
                .collect(Collectors.toSet());
        if (vendorIds.size() != 1 || vendorIds.contains(null)) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST,
                    "All selected purchase order lines must belong to same vendor", HttpStatus.BAD_REQUEST.value(), requestId);
        }
    }

    private BigDecimal calculateFeeAmount(List<PaymentRequestFeeRequest> fees) {
        if (fees == null || fees.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return fees.stream()
                .map(f -> f.getAmount() == null ? BigDecimal.ZERO : f.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String normalizeDocuments(List<String> selectedDocumentTypes) {
        Set<String> normalized = normalizeDocumentTypes(selectedDocumentTypes);
        return String.join(",", normalized);
    }

    private Set<String> normalizeDocumentTypes(List<String> selectedDocumentTypes) {
        return selectedDocumentTypes.stream()
                .filter(it -> it != null && !it.isBlank())
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private BigDecimal toVnd(BigDecimal amount, BigDecimal exchangeRate) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        return amount.multiply(exchangeRate);
    }

    private BigDecimal calculatePaidPercentage(BigDecimal paidAmount, BigDecimal totalAmount) {
        BigDecimal safePaid = paidAmount == null ? BigDecimal.ZERO : paidAmount;
        BigDecimal safeTotal = totalAmount == null ? BigDecimal.ZERO : totalAmount;
        if (safeTotal.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal percentage = safePaid.multiply(BigDecimal.valueOf(100))
                .divide(safeTotal, 2, RoundingMode.HALF_UP);
        if (percentage.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        if (percentage.compareTo(BigDecimal.valueOf(100)) > 0) {
            return BigDecimal.valueOf(100);
        }
        return percentage;
    }

    private void recalculateAmountsBeforeSave(PaymentRequestEntity paymentRequest, String requestId) {
        if (paymentRequest.getId() == null) {
            return;
        }
        BigDecimal requestedAmount = paymentRequestPurchaseOrderLineRepo.findByPaymentRequestId(paymentRequest.getId()).stream()
                .map(item -> item.getRequestedAmount() == null ? BigDecimal.ZERO : item.getRequestedAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal feeAmount = paymentRequestExtraFeeRepo.findByPaymentRequestId(paymentRequest.getId()).stream()
                .map(fee -> fee.getAmount() == null ? BigDecimal.ZERO : fee.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String normalizedCurrency = normalizeCurrency(paymentRequest.getCurrency());
        BigDecimal exchangeRate = resolveExchangeRate(normalizedCurrency, paymentRequest.getExchangeRate(), requestId);
        BigDecimal totalAmount = requestedAmount.add(feeAmount);

        if (paymentRequest.getPaidAmount() != null && paymentRequest.getPaidAmount().compareTo(totalAmount) > 0) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "Paid amount exceeds recalculated total amount",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }

        paymentRequest.setCurrency(normalizedCurrency);
        paymentRequest.setExchangeRate(exchangeRate);
        paymentRequest.setRequestedAmount(requestedAmount);
        paymentRequest.setFeeAmount(feeAmount);
        paymentRequest.setTotalAmount(totalAmount);
        paymentRequest.setRequestedAmountVnd(toVnd(requestedAmount, exchangeRate));
        paymentRequest.setFeeAmountVnd(toVnd(feeAmount, exchangeRate));
        paymentRequest.setTotalAmountVnd(toVnd(totalAmount, exchangeRate));
        paymentRequest.setPaidPercentage(calculatePaidPercentage(paymentRequest.getPaidAmount(), totalAmount));
        paymentRequest.setAmount(totalAmount);
    }

    private void transitionStatus(PaymentRequestEntity entity, String targetStatus, String requestId) {
        String currentStatus = entity.getStatus();
        if (currentStatus == null) {
            entity.setStatus(targetStatus);
            return;
        }
        Set<String> allowedTargets = ALLOWED_STATUS_TRANSITIONS.getOrDefault(currentStatus, Set.of());
        if (!allowedTargets.contains(targetStatus)) {
            throw new ApiException(ApiException.ErrorCode.CONFLICT,
                    "Invalid payment request status transition: " + currentStatus + " -> " + targetStatus,
                    HttpStatus.CONFLICT.value(), requestId);
        }
        entity.setStatus(targetStatus);
    }

    private String writeJson(Object value, String requestId) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "Invalid JSON object format",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }
    }

    private <T> T readJson(String value, TypeReference<T> typeReference) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(value, typeReference);
        } catch (Exception e) {
            return null;
        }
    }

    private UserEntity findUser(String requestId) {
        String userId = RequestContext.getCurrentUsername();
        return userRepo.findOneByUsername(userId)
                .orElseThrow(() -> new ApiException(ApiException.ErrorCode.NOT_FOUND, "User not found",
                        HttpStatus.NOT_FOUND.value(), requestId));
    }

    private void validateApproverRole(String approvalRole, String requestId) {
        UserInfo currentUserInfo = getCurrentUserInfoFromRedis(requestId);
        Set<String> roleCodes = currentUserInfo.getRoles().stream().map(RoleInfo::getCode).collect(Collectors.toSet());
        boolean allowed;
        if ("ACCOUNTANT".equalsIgnoreCase(approvalRole)) {
            allowed = roleCodes.contains("ACCOUNTANT");
        } else if ("HEAD_ACCOUNTANT".equalsIgnoreCase(approvalRole)) {
            allowed = roleCodes.contains("ACCOUNTANT_MANAGER") || roleCodes.contains("HEAD_ACCOUNTANT");
        } else {
            allowed = roleCodes.contains("ADMIN") || roleCodes.contains("ACCOUNTANT_MANAGER");
        }
        if (!allowed) {
            throw new ApiException(ApiException.ErrorCode.FORBIDDEN, "Current user does not have role for this approval level",
                    HttpStatus.FORBIDDEN.value(), requestId);
        }
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

    private PaymentRequestEntity findPaymentRequest(String id, String requestId) {
        return paymentRequestRepo.findByIdAndIsDeletedFalse(UUID.fromString(id))
                .orElseThrow(() -> new ApiException(ApiException.ErrorCode.NOT_FOUND, "Payment request not found",
                        HttpStatus.NOT_FOUND.value(), requestId));
    }

    private PaymentRequestInfo toInfoWithoutChildren(PaymentRequestEntity entity) {
        PaymentRequestInfo info = new PaymentRequestInfo();
        info.setId(entity.getId().toString());
        info.setRequestNumber(entity.getRequestNumber());
        info.setRequestDate(entity.getRequestDate() == null ? null : entity.getRequestDate().toString());
        info.setRequestorId(entity.getRequestor() == null ? null : entity.getRequestor().getId().toString());
        info.setVendorId(entity.getVendor() == null ? null : entity.getVendor().getId().toString());
        info.setStatus(entity.getStatus());
        info.setApprovalLevels(entity.getApprovalLevels());
        info.setCurrentApprovalLevel(entity.getCurrentApprovalLevel());
        info.setCurrency(entity.getCurrency());
        info.setExchangeRate(entity.getExchangeRate());
        info.setRequestedAmount(entity.getRequestedAmount());
        info.setRequestedAmountVnd(entity.getRequestedAmountVnd());
        info.setFeeAmount(entity.getFeeAmount());
        info.setFeeAmountVnd(entity.getFeeAmountVnd());
        info.setTotalAmount(entity.getTotalAmount());
        info.setTotalAmountVnd(entity.getTotalAmountVnd());
        info.setPaidAmount(entity.getPaidAmount());
        info.setPaidPercentage(entity.getPaidPercentage());
        info.setPaidAmountVnd(entity.getPaidAmountVnd());
        info.setPurpose(entity.getPurpose());
        info.setNotes(entity.getNotes());
        info.setCreatedBy(entity.getCreatedBy());
        return info;
    }

    private PaymentRequestInfo toInfo(PaymentRequestEntity entity) {
        PaymentRequestInfo info = toInfoWithoutChildren(entity);
        info.setPapers(readJson(entity.getPapers(), new TypeReference<List<PaymentFileObject>>() {
        }));
        info.setBankInfo(readJson(entity.getBankInfo(), new TypeReference<PaymentBankInfoObject>() {
        }));
        info.setBankNote(readJson(entity.getBankNote(), new TypeReference<PaymentBankNoteObject>() {
        }));
        info.setPaidBy(entity.getPaidBy() == null ? null : entity.getPaidBy().getId().toString());
        info.setPaidAt(entity.getPaidAt() == null ? null : entity.getPaidAt().toString());

        List<PaymentRequestPurchaseOrderLineEntity> itemEntities = paymentRequestPurchaseOrderLineRepo
                .findByPaymentRequestId(entity.getId());
        List<PaymentRequestItemDocumentEntity> docEntities = paymentRequestItemDocumentRepo.findByPaymentRequestId(entity.getId());
        List<PaymentRequestLineInfo> items = itemEntities.stream()
                .map(item -> {
                    PaymentRequestLineInfo lineInfo = new PaymentRequestLineInfo();
                    lineInfo.setId(item.getId().toString());
                    lineInfo.setPurchaseOrderLineId(item.getPurchaseOrderLine().getId().toString());
                    String selectedDocuments = docEntities.stream()
                            .filter(doc -> doc.getPaymentRequestItem().getId().equals(item.getId()))
                            .map(PaymentRequestItemDocumentEntity::getDocumentType)
                            .collect(Collectors.joining(","));
                    lineInfo.setSelectedDocuments(selectedDocuments.isBlank() ? item.getSelectedDocuments() : selectedDocuments);
                    lineInfo.setRequestedAmount(item.getRequestedAmount());
                    lineInfo.setPaidAmount(item.getPaidAmount());
                    lineInfo.setNote(item.getNote());
                    return lineInfo;
                }).toList();
        info.setItems(items);

        List<PaymentRequestFeeInfo> fees = paymentRequestExtraFeeRepo.findByPaymentRequestId(entity.getId()).stream().map(fee -> {
            PaymentRequestFeeInfo feeInfo = new PaymentRequestFeeInfo();
            feeInfo.setId(fee.getId().toString());
            feeInfo.setFeeName(fee.getFeeName());
            feeInfo.setFeeType(fee.getFeeType());
            feeInfo.setAmount(fee.getAmount());
            feeInfo.setNote(fee.getNote());
            return feeInfo;
        }).toList();
        info.setFees(fees);

        List<PaymentRequestApprovalInfo> approvals = paymentRequestApprovalRepo.findByPaymentRequestId(entity.getId()).stream()
                .map(approval -> {
                    PaymentRequestApprovalInfo approvalInfo = new PaymentRequestApprovalInfo();
                    approvalInfo.setId(approval.getId().toString());
                    approvalInfo.setLevel(approval.getApprovalLevel());
                    approvalInfo.setRole(approval.getApprovalRole());
                    approvalInfo.setApproverId(approval.getApprover() == null ? null : approval.getApprover().getId().toString());
                    approvalInfo.setStatus(approval.getStatus());
                    approvalInfo.setApprovedAt(approval.getApprovedAt() == null ? null : approval.getApprovedAt().toString());
                    approvalInfo.setRejectionReason(approval.getRejectionReason());
                    approvalInfo.setNote(approval.getNote());
                    return approvalInfo;
                }).toList();
        info.setApprovals(approvals);
        return info;
    }
}
