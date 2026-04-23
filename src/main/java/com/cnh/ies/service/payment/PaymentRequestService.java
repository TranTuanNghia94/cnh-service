package com.cnh.ies.service.payment;

import java.math.BigDecimal;
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
import com.cnh.ies.entity.payment.PaymentRequestItemDocumentEntity;
import com.cnh.ies.entity.payment.PaymentRequestPurchaseOrderLineEntity;
import com.cnh.ies.entity.purchaseorder.PurchaseOrderLineEntity;
import com.cnh.ies.exception.ApiException;
import com.cnh.ies.model.general.ListDataModel;
import com.cnh.ies.model.general.PaginationModel;
import com.cnh.ies.model.payment.ApprovePaymentRequest;
import com.cnh.ies.model.payment.CreateOrUpdatePaymentRequest;
import com.cnh.ies.model.payment.MarkPaymentPaidRequest;
import com.cnh.ies.model.payment.PaymentRequestFeeRequest;
import com.cnh.ies.model.payment.PaymentRequestInfo;
import com.cnh.ies.model.payment.PaymentRequestItemRequest;
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
import com.cnh.ies.mapper.payment.PaymentRequestMapper;
import com.cnh.ies.mapper.payment.PaymentRequestMoneyMapper;
import com.cnh.ies.mapper.payment.PaymentRequestMoneyTotals;
import com.cnh.ies.service.redis.RedisService;
import com.cnh.ies.util.RequestContext;
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
    private static final Set<String> DRAFT_OR_REJECTED = Set.of(
            Constant.PAYMENT_REQUEST_STATUS_DRAFT,
            Constant.PAYMENT_REQUEST_STATUS_REJECTED);

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
    private final PaymentRequestLineSyncService paymentRequestLineSyncService;
    private final PaymentRequestFeeSyncService paymentRequestFeeSyncService;
    private final PaymentRequestMapper paymentRequestMapper;
    private final PaymentRequestMoneyMapper paymentRequestMoneyMapper;
    private final PaymentRequestLineAmountCalculator paymentRequestLineAmountCalculator;

    public ListDataModel<PaymentRequestInfo> getAllPaymentRequests(String requestId, Integer page, Integer limit) {
        Page<PaymentRequestEntity> requests = paymentRequestRepo.findAllAndIsDeletedFalse(PageRequest.of(page, limit));
        List<PaymentRequestInfo> data = requests.stream()
                .map(e -> paymentRequestMapper.toSummaryInfoWithAlignedAmounts(e, requestId))
                .toList();
        PaginationModel pagination = PaginationModel.builder()
                .page(page)
                .limit(limit)
                .total(requests.getTotalElements())
                .totalPage(requests.getTotalPages())
                .build();
        return ListDataModel.<PaymentRequestInfo>builder().data(data).pagination(pagination).build();
    }

    @Transactional
    public PaymentRequestInfo getById(String id, String requestId) {
        PaymentRequestEntity paymentRequest = findPaymentRequest(id, requestId);
        if (DRAFT_OR_REJECTED.contains(paymentRequest.getStatus())) {
            recalculateAmountsBeforeSave(paymentRequest, requestId);
            paymentRequestRepo.saveAndFlush(paymentRequest);
        }
        return toInfo(paymentRequest, requestId);
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
        paymentRequestLineAmountCalculator.applyCalculatedRequestedAmounts(request, itemRequests, poLines, requestId);
        BigDecimal requestedAmount = itemRequests.stream()
                .map(PaymentRequestItemRequest::getRequestedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal feeAmount = calculateFeeAmount(request.getFees());
        String payCurrency = paymentRequestMoneyMapper.normalizeCurrency(request.getCurrency());
        BigDecimal payRate = paymentRequestMoneyMapper.resolveExchangeRate(payCurrency, request.getExchangeRate(), requestId);
        BigDecimal lineItemsAmount = paymentRequestMoneyMapper.sumPurchaseOrderLinePayables(poLines, payCurrency, payRate, requestId);

        PaymentRequestEntity entity = upsertPaymentRequestMain(request, poLines.get(0), requestedAmount, feeAmount, lineItemsAmount,
                requestId);
        paymentRequestLineSyncService.syncItems(entity, itemRequests, poLines, requestId);
        paymentRequestFeeSyncService.syncFees(entity, request.getFees(), requestId);
        paymentRequestRepo.flush();
        recalculateAmountsBeforeSave(entity, request, requestId);
        rebuildApprovalsIfNeeded(entity, request.getApprovalLevels());
        paymentRequestRepo.saveAndFlush(entity);

        return toInfo(entity, requestId);
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
        return toInfo(paymentRequest, requestId);
    }

    @Transactional
    public PaymentRequestInfo cancel(String id, String requestId) {
        PaymentRequestEntity paymentRequest = findPaymentRequest(id, requestId);
        if (!DRAFT_OR_REJECTED.contains(paymentRequest.getStatus())) {
            throw new ApiException(ApiException.ErrorCode.CONFLICT, "Only DRAFT/REJECTED can be cancelled",
                    HttpStatus.CONFLICT.value(), requestId);
        }
        recalculateAmountsBeforeSave(paymentRequest, requestId);
        transitionStatus(paymentRequest, Constant.PAYMENT_REQUEST_STATUS_CANCELLED, requestId);
        paymentRequest.setUpdatedBy(RequestContext.getCurrentUsername());
        paymentRequestRepo.save(paymentRequest);
        return toInfo(paymentRequest, requestId);
    }

    @Transactional
    public PaymentRequestInfo approve(String id, ApprovePaymentRequest request, String requestId) {
        PaymentRequestEntity paymentRequest = findPaymentRequest(id, requestId);
        ensureApprovingStatus(paymentRequest, requestId);
        recalculateAmountsBeforeSave(paymentRequest, requestId);

        int level = resolveApprovalLevel(request.getLevel(), paymentRequest);
        if (level <= 0 || level > paymentRequest.getApprovalLevels()) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "Invalid approval level", HttpStatus.BAD_REQUEST.value(),
                    requestId);
        }
        PaymentRequestApprovalEntity approval = approvalAtLevelOrThrow(paymentRequest, level, requestId);
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
        return toInfo(paymentRequest, requestId);
    }

    @Transactional
    public PaymentRequestInfo reject(String id, RejectPaymentRequest request, String requestId) {
        PaymentRequestEntity paymentRequest = findPaymentRequest(id, requestId);
        ensureApprovingStatus(paymentRequest, requestId);
        recalculateAmountsBeforeSave(paymentRequest, requestId);

        int level = resolveApprovalLevel(request.getLevel(), paymentRequest);
        PaymentRequestApprovalEntity approval = approvalAtLevelOrThrow(paymentRequest, level, requestId);

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
        return toInfo(paymentRequest, requestId);
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
        paymentRequest.setPaidAmountVnd(paymentRequest.getPaidAmountVnd()
                .add(paymentRequestMoneyMapper.toVnd(request.getPaidAmount(), exchangeRate)));
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
        return toInfo(paymentRequest, requestId);
    }

    private PaymentRequestEntity upsertPaymentRequestMain(CreateOrUpdatePaymentRequest request,
            PurchaseOrderLineEntity firstLine, BigDecimal requestedAmount, BigDecimal feeAmount, BigDecimal lineItemsAmount,
            String requestId) {
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
            if (!DRAFT_OR_REJECTED.contains(entity.getStatus())) {
                throw new ApiException(ApiException.ErrorCode.CONFLICT,
                        "Only DRAFT/REJECTED payment request can be updated", HttpStatus.CONFLICT.value(), requestId);
            }
            if (request.getRequestDate() != null) {
                entity.setRequestDate(request.getRequestDate());
            }
        }

        paymentRequestMapper.applyHeaderFromCreateOrUpdate(entity, request, findUser(requestId), firstLine.getVendor(),
                requestedAmount, feeAmount, lineItemsAmount, writeJson(request.getPapers(), requestId),
                writeJson(request.getBankInfo(), requestId),
                requestId);
        return paymentRequestRepo.save(entity);
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
        return switch (level) {
            case 1 -> "ACCOUNTANT";
            case 2 -> "HEAD_ACCOUNTANT";
            default -> "FINAL_APPROVER";
        };
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
        if (!DRAFT_OR_REJECTED.contains(paymentRequest.getStatus())) {
            throw new ApiException(ApiException.ErrorCode.CONFLICT, "Only DRAFT/REJECTED can be submitted",
                    HttpStatus.CONFLICT.value(), requestId);
        }
    }

    private static int resolveApprovalLevel(Integer explicitLevel, PaymentRequestEntity pr) {
        return explicitLevel == null ? pr.getCurrentApprovalLevel() + 1 : explicitLevel;
    }

    private PaymentRequestApprovalEntity approvalAtLevelOrThrow(PaymentRequestEntity pr, int level, String requestId) {
        return paymentRequestApprovalRepo.findByPaymentRequestIdAndLevel(pr.getId(), level)
                .orElseThrow(() -> new ApiException(ApiException.ErrorCode.NOT_FOUND, "Approval level not found",
                        HttpStatus.NOT_FOUND.value(), requestId));
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
        paymentRequestMoneyMapper.validateCurrencyAndExchangeForCreate(request, requestId);
        BigDecimal pct = request.getPaidPercentage();
        if (pct != null && (pct.compareTo(BigDecimal.ZERO) <= 0 || pct.compareTo(BigDecimal.valueOf(100)) > 0)) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST,
                    "paidPercentage must be between 0 (exclusive) and 100 (inclusive)", HttpStatus.BAD_REQUEST.value(), requestId);
        }
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

    private void recalculateAmountsBeforeSave(PaymentRequestEntity paymentRequest, String requestId) {
        recalculateAmountsBeforeSave(paymentRequest, null, requestId);
    }

    /**
     * Recomputes header money fields from persisted lines and extra fees. After create/update, pass
     * {@code createOrUpdateRequest} so currency, exchange rate, and (when unpaid) paid percentage match the payload.
     */
    private void recalculateAmountsBeforeSave(PaymentRequestEntity paymentRequest, CreateOrUpdatePaymentRequest createOrUpdateRequest,
            String requestId) {
        if (paymentRequest.getId() == null) {
            return;
        }
        List<PaymentRequestPurchaseOrderLineEntity> lineItems = paymentRequestPurchaseOrderLineRepo
                .findByPaymentRequestId(paymentRequest.getId());
        BigDecimal requestedAmount = paymentRequestMoneyMapper.sumLineRequestedAmounts(lineItems);
        BigDecimal feeAmount = paymentRequestMoneyMapper.sumExtraFeeAmounts(
                paymentRequestExtraFeeRepo.findByPaymentRequestId(paymentRequest.getId()));

        String currencyForTotals = createOrUpdateRequest != null
                ? createOrUpdateRequest.getCurrency()
                : paymentRequest.getCurrency();
        BigDecimal exchangeRateForTotals = createOrUpdateRequest != null
                ? createOrUpdateRequest.getExchangeRate()
                : paymentRequest.getExchangeRate();

        BigDecimal lineItemsAmount = paymentRequestMoneyMapper.sumLineItemPayableAmounts(lineItems, currencyForTotals,
                exchangeRateForTotals, requestId);

        PaymentRequestMoneyTotals totals = paymentRequestMoneyMapper.computeMoneyTotals(requestedAmount, feeAmount,
                currencyForTotals, exchangeRateForTotals, requestId);

        if (paymentRequest.getPaidAmount() != null && paymentRequest.getPaidAmount().compareTo(totals.totalAmount()) > 0) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "Paid amount exceeds recalculated total amount",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }

        paymentRequestMoneyMapper.applyMoneyTotals(paymentRequest, totals);
        BigDecimal paid = paymentRequest.getPaidAmount() == null ? BigDecimal.ZERO : paymentRequest.getPaidAmount();
        if (paid.compareTo(BigDecimal.ZERO) > 0) {
            paymentRequest.setPaidPercentage(paymentRequestMoneyMapper.calculatePaidPercentage(
                    paymentRequest.getPaidAmount(), totals.totalAmount()));
        } else if (createOrUpdateRequest != null) {
            paymentRequest.setPaidPercentage(createOrUpdateRequest.getPaidPercentage() == null ? BigDecimal.valueOf(100)
                    : createOrUpdateRequest.getPaidPercentage());
        }
        paymentRequest.setAmount(lineItemsAmount);
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

    private PaymentRequestInfo toInfo(PaymentRequestEntity entity, String requestId) {
        UUID id = entity.getId();
        // Load item documents first so payment-line entities enter the PC with purchaseOrderLine fetched
        // (avoids a second query re-loading lines without associations before mapping).
        List<PaymentRequestItemDocumentEntity> documents = paymentRequestItemDocumentRepo.findByPaymentRequestId(id);
        List<PaymentRequestPurchaseOrderLineEntity> lines = paymentRequestPurchaseOrderLineRepo.findByPaymentRequestId(id);
        return paymentRequestMapper.toDetailInfo(entity, lines,
                paymentRequestExtraFeeRepo.findByPaymentRequestId(id),
                documents,
                paymentRequestApprovalRepo.findByPaymentRequestId(id),
                requestId);
    }
}
