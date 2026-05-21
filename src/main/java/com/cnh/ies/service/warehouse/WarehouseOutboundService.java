package com.cnh.ies.service.warehouse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.cnh.ies.constant.Constant;
import com.cnh.ies.constant.PermissionConstants;
import com.cnh.ies.entity.auth.UserEntity;
import com.cnh.ies.entity.order.OrderEntity;
import com.cnh.ies.entity.order.OrderLineEntity;
import com.cnh.ies.entity.product.ProductEntity;
import com.cnh.ies.entity.warehouse.WarehouseOutboundApprovalEntity;
import com.cnh.ies.entity.warehouse.WarehouseInventoryEntity;
import com.cnh.ies.entity.warehouse.WarehouseOutboundDetailEntity;
import com.cnh.ies.entity.warehouse.WarehouseOutboundEntity;
import com.cnh.ies.entity.warehouse.WarehouseStockTransactionEntity;
import com.cnh.ies.exception.ApiException;
import com.cnh.ies.model.general.ListDataModel;
import com.cnh.ies.model.general.PaginationModel;
import com.cnh.ies.model.payment.ApprovePaymentRequest;
import com.cnh.ies.model.payment.PaymentFileUploadInfo;
import com.cnh.ies.model.payment.PaymentRequestApprovalInfo;
import com.cnh.ies.model.payment.RejectPaymentRequest;
import com.cnh.ies.model.user.UserInfo;
import com.cnh.ies.model.warehouse.WarehouseOutboundCreateRequest;
import com.cnh.ies.model.warehouse.WarehouseOutboundActionsInfo;
import com.cnh.ies.model.warehouse.WarehouseOutboundDetailInfo;
import com.cnh.ies.model.warehouse.WarehouseOutboundDetailRequest;
import com.cnh.ies.model.warehouse.WarehouseOutboundInfo;
import com.cnh.ies.model.warehouse.WarehouseOutboundOrderLineInfo;
import com.cnh.ies.model.warehouse.WarehouseOutboundOrderSearchInfo;
import com.cnh.ies.repository.auth.UserRepo;
import com.cnh.ies.repository.order.OrderLineRepo;
import com.cnh.ies.repository.order.OrderRepo;
import com.cnh.ies.repository.warehouse.WarehouseOutboundApprovalRepo;
import com.cnh.ies.repository.warehouse.WarehouseInventoryRepo;
import com.cnh.ies.repository.warehouse.WarehouseOutboundDetailRepo;
import com.cnh.ies.repository.warehouse.WarehouseOutboundRepo;
import com.cnh.ies.repository.warehouse.WarehouseStockTransactionRepo;
import com.cnh.ies.service.file.FileService;
import com.cnh.ies.service.notification.NotificationService;
import com.cnh.ies.service.redis.RedisService;
import com.cnh.ies.util.PermissionUtils;
import com.cnh.ies.util.RequestContext;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class WarehouseOutboundService {

    private static final Logger log = LoggerFactory.getLogger(WarehouseOutboundService.class);

    private final OrderRepo orderRepo;
    private final OrderLineRepo orderLineRepo;
    private final WarehouseInventoryRepo warehouseInventoryRepo;
    private final WarehouseStockTransactionRepo warehouseStockTransactionRepo;
    private final WarehouseOutboundRepo warehouseOutboundRepo;
    private final WarehouseOutboundDetailRepo warehouseOutboundDetailRepo;
    private final WarehouseOutboundApprovalRepo warehouseOutboundApprovalRepo;
    private final WarehouseOutboundNumberService warehouseOutboundNumberService;
    private final FileService fileService;
    private final UserRepo userRepo;
    private final RedisService redisService;
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    public WarehouseOutboundOrderSearchInfo getOrderLinesByContractNumber(String contractNumber, String requestId) {
        OrderEntity order = findOrderByContract(contractNumber, requestId);
        List<OrderLineEntity> orderLines = orderLineRepo.findAllByOrderId(order.getId());
        if (orderLines.isEmpty()) {
            throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "No order lines found for contract number",
                    HttpStatus.NOT_FOUND.value(), requestId);
        }
        List<WarehouseOutboundOrderLineInfo> lines = orderLines.stream()
                .map(this::toOrderLineInfo)
                .toList();
        WarehouseOutboundOrderSearchInfo info = new WarehouseOutboundOrderSearchInfo();
        info.setOrderId(order.getId().toString());
        info.setOrderNumber(order.getOrderPrefix() + order.getOrderNumber());
        info.setContractNumber(order.getContractNumber());
        info.setOrderStatus(order.getStatus());
        if (order.getCustomer() != null) {
            info.setCustomerId(order.getCustomer().getId().toString());
            info.setCustomerCode(order.getCustomer().getCode());
            info.setCustomerName(order.getCustomer().getName());
            info.setCustomerPhone(order.getCustomer().getPhone());
            info.setCustomerTaxCode(order.getCustomer().getTaxCode());
        }
        if (order.getCustomerAddress() != null) {
            info.setCustomerAddressId(order.getCustomerAddress().getId().toString());
            info.setCustomerAddress(order.getCustomerAddress().getAddress());
            info.setCustomerContactPerson(order.getCustomerAddress().getContactPerson());
            info.setCustomerAddressPhone(order.getCustomerAddress().getPhone());
        }
        info.setOrderLines(lines);
        return info;
    }

    public ListDataModel<WarehouseOutboundInfo> list(
            String requestId,
            Integer page,
            Integer limit,
            String createdBy,
            String outboundNumber,
            String contractNumber,
            String status) {
        int safePage = page == null ? 0 : page;
        int safeLimit = limit == null ? 10 : limit;
        if (safePage < 0) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "page must be >= 0",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }
        if (safeLimit <= 0) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "limit must be > 0",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }
        String createdByFilter = normalize(createdBy) != null ? normalize(createdBy) : "";
        String outboundNumberFilter = normalize(outboundNumber) != null ? normalize(outboundNumber) : "";
        String contractNumberFilter = normalize(contractNumber) != null ? normalize(contractNumber) : "";
        String statusFilter = normalize(status) != null ? normalize(status) : "";

        Page<WarehouseOutboundEntity> outbounds = warehouseOutboundRepo.findAllFiltered(
                createdByFilter,
                outboundNumberFilter,
                contractNumberFilter,
                statusFilter,
                PageRequest.of(safePage, safeLimit));
        List<WarehouseOutboundInfo> data = outbounds.stream()
                .map(outbound -> toInfo(outbound, requestId))
                .toList();
        return ListDataModel.<WarehouseOutboundInfo>builder()
                .data(data)
                .pagination(PaginationModel.builder()
                        .page(safePage)
                        .limit(safeLimit)
                        .total(outbounds.getTotalElements())
                        .totalPage(outbounds.getTotalPages())
                        .build())
                .build();
    }

    public WarehouseOutboundInfo getOutbound(String outboundId, String requestId) {
        WarehouseOutboundEntity outbound = loadOutboundForMutation(outboundId, requestId);
        return toInfo(outbound, requestId);
    }

    public WarehouseOutboundActionsInfo getAllowedActions(String outboundId, String requestId) {
        WarehouseOutboundEntity outbound = loadOutboundForMutation(outboundId, requestId);
        WarehouseOutboundActionsInfo actions = new WarehouseOutboundActionsInfo();
        String status = outbound.getStatus();
        String currentUser = RequestContext.getCurrentUsername();
        boolean isOwner = outbound.getCreatedBy() != null && currentUser != null
                && outbound.getCreatedBy().equalsIgnoreCase(currentUser);
        actions.setCanSubmit(Constant.WAREHOUSE_OUTBOUND_STATUS_DRAFT.equals(status) && isOwner);
        actions.setCanCancel(Constant.WAREHOUSE_OUTBOUND_STATUS_DRAFT.equals(status) && isOwner);
        actions.setCanResubmit(Constant.WAREHOUSE_OUTBOUND_STATUS_REJECTED.equals(status)
                || Constant.WAREHOUSE_OUTBOUND_STATUS_CANCELLED.equals(status));
        boolean canApproveOrReject = false;
        if (Constant.WAREHOUSE_OUTBOUND_STATUS_SUBMITTED.equals(status)) {
            int nextLevel = outbound.getCurrentApprovalLevel() + 1;
            WarehouseOutboundApprovalEntity approval = warehouseOutboundApprovalRepo
                    .findByOutboundIdAndLevel(outbound.getId(), nextLevel)
                    .orElse(null);
            if (approval != null && Constant.PAYMENT_APPROVAL_STATUS_PENDING.equals(approval.getStatus())) {
                canApproveOrReject = canCurrentUserApprove(outbound, requestId);
            }
        }
        actions.setCanApprove(canApproveOrReject);
        actions.setCanReject(canApproveOrReject);
        return actions;
    }

    @Transactional
    public WarehouseOutboundInfo createOutboundByContract(WarehouseOutboundCreateRequest request, String requestId) {
        if (request == null) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "Request body is required",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }
        String contractNumber = normalize(request.getContractNumber());
        if (contractNumber == null) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "contractNumber is required",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }
        String outboundReason = normalize(request.getOutboundReason());
        if (outboundReason == null) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "outboundReason is required",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }
        int approvalLevels = request.getApprovalLevels() == null ? 1 : request.getApprovalLevels();
        if (approvalLevels < 1 || approvalLevels > 10) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST,
                    "approvalLevels must be between 1 and 10",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }
        if (request.getApprovalRoles() != null && !request.getApprovalRoles().isEmpty()
                && request.getApprovalRoles().size() != approvalLevels) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST,
                    "approvalRoles size must match approvalLevels when provided",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }

        OrderEntity order = findOrderByContract(contractNumber, requestId);
        List<OrderLineEntity> orderLines = orderLineRepo.findAllByOrderId(order.getId());
        if (orderLines.isEmpty()) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "Order has no detail lines",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }

        Map<UUID, OrderLineEntity> orderLineById = new HashMap<>();
        for (OrderLineEntity orderLine : orderLines) {
            orderLineById.put(orderLine.getId(), orderLine);
        }

        List<WarehouseOutboundDetailRequest> requestedDetails = request.getDetails();
        List<WarehouseOutboundDetailRequest> effectiveDetails = (requestedDetails == null || requestedDetails.isEmpty())
                ? buildDetailsFromOrder(orderLines, request.getCurrency())
                : requestedDetails;

        if (effectiveDetails.isEmpty()) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "At least one outbound detail is required",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }

        Map<UUID, BigDecimal> qtyByProductId = new HashMap<>();
        for (WarehouseOutboundDetailRequest detail : effectiveDetails) {
            UUID orderLineId = parseUuid(detail.getOrderLineId(), "details.orderLineId", requestId);
            OrderLineEntity orderLine = orderLineById.get(orderLineId);
            if (orderLine == null) {
                throw new ApiException(ApiException.ErrorCode.BAD_REQUEST,
                        "Order line does not belong to contract: " + detail.getOrderLineId(),
                        HttpStatus.BAD_REQUEST.value(), requestId);
            }
            if (orderLine.getProduct() == null || Boolean.TRUE.equals(orderLine.getProduct().getIsDeleted())) {
                throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "Order line has invalid product",
                        HttpStatus.BAD_REQUEST.value(), requestId);
            }
            BigDecimal quantity = detail.getQuantity();
            if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "Detail quantity must be greater than 0",
                        HttpStatus.BAD_REQUEST.value(), requestId);
            }
            BigDecimal maxQty = orderLine.getQuantity() == null ? BigDecimal.ZERO : orderLine.getQuantity();
            if (quantity.compareTo(maxQty) > 0) {
                throw new ApiException(ApiException.ErrorCode.BAD_REQUEST,
                        "Outbound quantity cannot exceed order line quantity",
                        HttpStatus.BAD_REQUEST.value(), requestId);
            }
            UUID productId = orderLine.getProduct().getId();
            qtyByProductId.merge(productId, quantity, BigDecimal::add);
        }

        for (Map.Entry<UUID, BigDecimal> entry : qtyByProductId.entrySet()) {
            WarehouseInventoryEntity inventory = warehouseInventoryRepo.findByProductId(entry.getKey())
                    .orElseThrow(() -> new ApiException(ApiException.ErrorCode.BAD_REQUEST,
                            "No inventory record for product", HttpStatus.BAD_REQUEST.value(), requestId));
            BigDecimal onHand = inventory.getQuantityOnHand() == null ? BigDecimal.ZERO : inventory.getQuantityOnHand();
            if (onHand.compareTo(entry.getValue()) < 0) {
                throw new ApiException(ApiException.ErrorCode.BAD_REQUEST,
                        "Insufficient quantity on hand for product " + inventory.getProduct().getCode(),
                        HttpStatus.BAD_REQUEST.value(), requestId);
            }
        }

        String username = RequestContext.getCurrentUsername();
        WarehouseOutboundEntity outbound = new WarehouseOutboundEntity();
        outbound.setOutboundNumber(warehouseOutboundNumberService.generateOutboundNumber());
        outbound.setOrder(order);
        outbound.setContractNumber(order.getContractNumber());
        outbound.setOutboundReason(outboundReason);
        outbound.setCurrency(resolveCurrency(request.getCurrency(), orderLines));
        outbound.setOutboundDate(request.getOutboundDate() == null ? LocalDate.now() : request.getOutboundDate());
        outbound.setStatus(Constant.WAREHOUSE_OUTBOUND_STATUS_DRAFT);
        outbound.setApprovalLevels(approvalLevels);
        outbound.setCurrentApprovalLevel(0);
        outbound.setNote(request.getNote());
        outbound.setCreatedBy(username);
        outbound.setUpdatedBy(username);
        outbound.setIsDeleted(false);
        warehouseOutboundRepo.save(outbound);
        seedApprovals(outbound, approvalLevels, request.getApprovalRoles(), requestId);

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalTaxAmount = BigDecimal.ZERO;
        for (WarehouseOutboundDetailRequest detail : effectiveDetails) {
            UUID orderLineId = parseUuid(detail.getOrderLineId(), "details.orderLineId", requestId);
            OrderLineEntity orderLine = orderLineById.get(orderLineId);
            ProductEntity product = orderLine.getProduct();
            BigDecimal quantity = detail.getQuantity();
            BigDecimal unitPrice = orderLine.getUnitPrice() != null ? orderLine.getUnitPrice() : BigDecimal.ZERO;
            BigDecimal vat = product.getTax() != null ? product.getTax() : BigDecimal.ZERO;
            BigDecimal grossAmount = unitPrice.multiply(quantity);
            BigDecimal lineTaxAmount;
            BigDecimal linePriceWithoutTax;
            if (Boolean.TRUE.equals(orderLine.getIsIncludedTax())) {
                BigDecimal divisor = BigDecimal.ONE.add(vat.divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP));
                linePriceWithoutTax = unitPrice.divide(divisor, 2, RoundingMode.HALF_UP);
                lineTaxAmount = grossAmount.subtract(linePriceWithoutTax.multiply(quantity));
            } else {
                linePriceWithoutTax = unitPrice;
                lineTaxAmount = grossAmount.multiply(vat).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            }
            BigDecimal lineTotalAmount = grossAmount;
            String lineCurrency = normalize(detail.getCurrency());
            if (lineCurrency == null) {
                lineCurrency = outbound.getCurrency();
            }

            WarehouseOutboundDetailEntity entity = new WarehouseOutboundDetailEntity();
            entity.setOutbound(outbound);
            entity.setOrderLine(orderLine);
            entity.setProduct(product);
            entity.setQuantity(quantity);
            entity.setBox(detail.getBox());
            entity.setReferenceCode(detail.getReferenceCode());
            entity.setUnitPrice(unitPrice);
            entity.setPriceWithoutTax(linePriceWithoutTax);
            entity.setVat(vat);
            entity.setCurrency(lineCurrency);
            entity.setTotalAmount(lineTotalAmount);
            entity.setTaxAmount(lineTaxAmount);
            entity.setNote(detail.getNote());
            entity.setCreatedBy(username);
            entity.setUpdatedBy(username);
            entity.setIsDeleted(false);
            warehouseOutboundDetailRepo.save(entity);

            totalAmount = totalAmount.add(lineTotalAmount);
            totalTaxAmount = totalTaxAmount.add(lineTaxAmount);
        }
        outbound.setTotalAmount(totalAmount);
        outbound.setTaxAmount(totalTaxAmount);
        outbound.setUpdatedBy(username);
        warehouseOutboundRepo.save(outbound);

        List<UUID> fileIds = parseFileIds(request.getAttachedFileIds(), requestId);
        fileService.linkFilesToWarehouseOutbound(fileIds, outbound.getId(), requestId);

        return toInfo(outbound, requestId);
    }

    @Transactional
    public WarehouseOutboundInfo submitForApproval(String outboundId, String requestId) {
        WarehouseOutboundEntity outbound = loadOutboundForMutation(outboundId, requestId);
        if (!Constant.WAREHOUSE_OUTBOUND_STATUS_DRAFT.equals(outbound.getStatus())) {
            throw new ApiException(ApiException.ErrorCode.CONFLICT, "Only DRAFT outbound can be submitted",
                    HttpStatus.CONFLICT.value(), requestId);
        }
        String currentUser = RequestContext.getCurrentUsername();
        if (outbound.getCreatedBy() == null || currentUser == null
                || !outbound.getCreatedBy().equalsIgnoreCase(currentUser)) {
            throw new ApiException(ApiException.ErrorCode.FORBIDDEN,
                    "Only the document owner can submit outbound for approval",
                    HttpStatus.FORBIDDEN.value(), requestId);
        }
        outbound.setStatus(Constant.WAREHOUSE_OUTBOUND_STATUS_SUBMITTED);
        outbound.setUpdatedBy(currentUser);
        warehouseOutboundRepo.save(outbound);
        notifyAccountantsOutboundPendingApproval(outbound);
        return toInfo(outbound, requestId);
    }

    @Transactional
    public WarehouseOutboundInfo approve(String outboundId, ApprovePaymentRequest request, String requestId) {
        WarehouseOutboundEntity outbound = loadOutboundForMutation(outboundId, requestId);
        if (!Constant.WAREHOUSE_OUTBOUND_STATUS_SUBMITTED.equals(outbound.getStatus())) {
            throw new ApiException(ApiException.ErrorCode.CONFLICT, "Outbound is not awaiting approval",
                    HttpStatus.CONFLICT.value(), requestId);
        }
        int level = request != null && request.getLevel() != null ? request.getLevel() : outbound.getCurrentApprovalLevel() + 1;
        WarehouseOutboundApprovalEntity approval = warehouseOutboundApprovalRepo.findByOutboundIdAndLevel(outbound.getId(), level)
                .orElseThrow(() -> new ApiException(ApiException.ErrorCode.NOT_FOUND, "Approval level not found",
                        HttpStatus.NOT_FOUND.value(), requestId));
        if (!Constant.PAYMENT_APPROVAL_STATUS_PENDING.equals(approval.getStatus())) {
            throw new ApiException(ApiException.ErrorCode.CONFLICT, "This level was already processed",
                    HttpStatus.CONFLICT.value(), requestId);
        }
        validateOutboundApproverRole(outbound, approval.getApprovalRole(), requestId);
        UserEntity user = findUserEntity(requestId);
        approval.setStatus(Constant.PAYMENT_APPROVAL_STATUS_APPROVED);
        approval.setApprover(user);
        approval.setApprovedAt(Instant.now());
        approval.setNote(request == null ? null : request.getNote());
        approval.setUpdatedBy(RequestContext.getCurrentUsername());
        warehouseOutboundApprovalRepo.save(approval);

        outbound.setCurrentApprovalLevel(level);
        if (level >= outbound.getApprovalLevels()) {
            outbound.setStatus(Constant.WAREHOUSE_OUTBOUND_STATUS_APPROVED);
            applyOutboundToInventory(outbound, requestId);
        }
        outbound.setUpdatedBy(RequestContext.getCurrentUsername());
        warehouseOutboundRepo.save(outbound);
        return toInfo(outbound, requestId);
    }

    @Transactional
    public WarehouseOutboundInfo reject(String outboundId, RejectPaymentRequest request, String requestId) {
        WarehouseOutboundEntity outbound = loadOutboundForMutation(outboundId, requestId);
        if (!Constant.WAREHOUSE_OUTBOUND_STATUS_SUBMITTED.equals(outbound.getStatus())) {
            throw new ApiException(ApiException.ErrorCode.CONFLICT, "Outbound is not awaiting approval",
                    HttpStatus.CONFLICT.value(), requestId);
        }
        int level = request != null && request.getLevel() != null ? request.getLevel() : outbound.getCurrentApprovalLevel() + 1;
        WarehouseOutboundApprovalEntity approval = warehouseOutboundApprovalRepo.findByOutboundIdAndLevel(outbound.getId(), level)
                .orElseThrow(() -> new ApiException(ApiException.ErrorCode.NOT_FOUND, "Approval level not found",
                        HttpStatus.NOT_FOUND.value(), requestId));
        validateOutboundApproverRole(outbound, approval.getApprovalRole(), requestId);
        UserEntity user = findUserEntity(requestId);
        approval.setStatus(Constant.PAYMENT_APPROVAL_STATUS_REJECTED);
        approval.setApprover(user);
        approval.setApprovedAt(Instant.now());
        approval.setRejectionReason(request == null ? null : request.getReason());
        approval.setNote(request == null ? null : request.getNote());
        approval.setUpdatedBy(RequestContext.getCurrentUsername());
        warehouseOutboundApprovalRepo.save(approval);

        outbound.setStatus(Constant.WAREHOUSE_OUTBOUND_STATUS_REJECTED);
        outbound.setUpdatedBy(RequestContext.getCurrentUsername());
        warehouseOutboundRepo.save(outbound);
        return toInfo(outbound, requestId);
    }

    @Transactional
    public WarehouseOutboundInfo cancel(String outboundId, String requestId) {
        WarehouseOutboundEntity outbound = loadOutboundForMutation(outboundId, requestId);
        if (!Constant.WAREHOUSE_OUTBOUND_STATUS_DRAFT.equals(outbound.getStatus())) {
            throw new ApiException(ApiException.ErrorCode.CONFLICT, "Only DRAFT outbound can be cancelled",
                    HttpStatus.CONFLICT.value(), requestId);
        }
        String currentUser = RequestContext.getCurrentUsername();
        if (outbound.getCreatedBy() == null || currentUser == null
                || !outbound.getCreatedBy().equalsIgnoreCase(currentUser)) {
            throw new ApiException(ApiException.ErrorCode.FORBIDDEN,
                    "Only the document owner can cancel this outbound",
                    HttpStatus.FORBIDDEN.value(), requestId);
        }
        outbound.setStatus(Constant.WAREHOUSE_OUTBOUND_STATUS_CANCELLED);
        outbound.setUpdatedBy(RequestContext.getCurrentUsername());
        warehouseOutboundRepo.save(outbound);
        return toInfo(outbound, requestId);
    }

    @Transactional
    public WarehouseOutboundInfo resubmit(String outboundId, String requestId) {
        WarehouseOutboundEntity outbound = loadOutboundForMutation(outboundId, requestId);
        if (!Constant.WAREHOUSE_OUTBOUND_STATUS_REJECTED.equals(outbound.getStatus())
                && !Constant.WAREHOUSE_OUTBOUND_STATUS_CANCELLED.equals(outbound.getStatus())) {
            throw new ApiException(ApiException.ErrorCode.CONFLICT, "Only REJECTED/CANCELLED outbound can be resubmitted",
                    HttpStatus.CONFLICT.value(), requestId);
        }
        resetApprovals(outbound);
        outbound.setStatus(Constant.WAREHOUSE_OUTBOUND_STATUS_SUBMITTED);
        outbound.setCurrentApprovalLevel(0);
        outbound.setUpdatedBy(RequestContext.getCurrentUsername());
        warehouseOutboundRepo.save(outbound);
        return toInfo(outbound, requestId);
    }

    private WarehouseOutboundInfo toInfo(WarehouseOutboundEntity outbound, String requestId) {
        List<WarehouseOutboundDetailEntity> details = warehouseOutboundDetailRepo.findByOutboundId(outbound.getId());
        List<PaymentFileUploadInfo> attachments = fileService.listUploadedFilesForWarehouseOutbound(
                outbound.getId().toString(), requestId);
        List<PaymentRequestApprovalInfo> approvals = warehouseOutboundApprovalRepo.findByOutboundId(outbound.getId()).stream()
                .map(this::toApprovalInfo)
                .toList();
        WarehouseOutboundInfo info = new WarehouseOutboundInfo();
        info.setId(outbound.getId().toString());
        info.setOutboundNumber(outbound.getOutboundNumber());
        info.setOrderId(outbound.getOrder().getId().toString());
        info.setOrderNumber(outbound.getOrder().getOrderPrefix() + outbound.getOrder().getOrderNumber());
        info.setContractNumber(outbound.getContractNumber());
        info.setOutboundReason(outbound.getOutboundReason());
        info.setNote(outbound.getNote());
        info.setCurrency(outbound.getCurrency());
        info.setOutboundDate(outbound.getOutboundDate() == null ? null : outbound.getOutboundDate().toString());
        info.setStatus(outbound.getStatus());
        info.setApprovalLevels(outbound.getApprovalLevels());
        info.setCurrentApprovalLevel(outbound.getCurrentApprovalLevel());
        info.setTotalAmount(outbound.getTotalAmount());
        info.setTaxAmount(outbound.getTaxAmount());
        info.setCreatedBy(outbound.getCreatedBy());
        info.setCreatedAt(outbound.getCreatedAt() == null ? null : outbound.getCreatedAt().toString());
        info.setDetails(details.stream().map(this::toDetailInfo).toList());
        info.setAttachments(attachments);
        info.setApprovals(approvals);
        return info;
    }

    private WarehouseOutboundDetailInfo toDetailInfo(WarehouseOutboundDetailEntity detail) {
        WarehouseOutboundDetailInfo info = new WarehouseOutboundDetailInfo();
        info.setId(detail.getId().toString());
        info.setOrderLineId(detail.getOrderLine().getId().toString());
        info.setProductId(detail.getProduct().getId().toString());
        info.setProductCode(detail.getProduct().getCode());
        info.setProductName(detail.getProduct().getName());
        info.setQuantity(detail.getQuantity());
        info.setBox(detail.getBox());
        info.setReferenceCode(detail.getReferenceCode());
        info.setUnitPrice(detail.getUnitPrice());
        info.setPriceWithoutTax(detail.getPriceWithoutTax());
        info.setVat(detail.getVat());
        info.setCurrency(detail.getCurrency());
        info.setTotalAmount(detail.getTotalAmount());
        info.setTaxAmount(detail.getTaxAmount());
        info.setNote(detail.getNote());
        return info;
    }

    private WarehouseOutboundOrderLineInfo toOrderLineInfo(OrderLineEntity orderLine) {
        WarehouseOutboundOrderLineInfo info = new WarehouseOutboundOrderLineInfo();
        info.setOrderLineId(orderLine.getId().toString());
        info.setProductId(orderLine.getProduct().getId().toString());
        info.setProductCode(orderLine.getProduct().getCode());
        info.setProductName(orderLine.getProduct().getName());
        info.setOrderQuantity(orderLine.getQuantity());
        WarehouseInventoryEntity inv = warehouseInventoryRepo.findByProductId(orderLine.getProduct().getId()).orElse(null);
        info.setAvailableQuantity(inv == null ? BigDecimal.ZERO : inv.getQuantityOnHand());
        info.setUnitPrice(orderLine.getUnitPrice());
        info.setVat(orderLine.getProduct().getTax());
        info.setIncludedTax(orderLine.getIsIncludedTax());
        info.setCurrency("VND");
        info.setTotalAmount(orderLine.getTotalAmount());
        info.setTaxAmount(orderLine.getTaxAmount());
        return info;
    }

    private List<WarehouseOutboundDetailRequest> buildDetailsFromOrder(List<OrderLineEntity> lines, String currency) {
        return lines.stream().map(line -> {
            WarehouseOutboundDetailRequest request = new WarehouseOutboundDetailRequest();
            request.setOrderLineId(line.getId().toString());
            request.setQuantity(line.getQuantity());
            request.setCurrency(resolveCurrency(currency, lines));
            return request;
        }).toList();
    }

    private String resolveCurrency(String requestCurrency, List<OrderLineEntity> orderLines) {
        String normalized = normalize(requestCurrency);
        if (normalized != null) {
            return normalized;
        }
        for (OrderLineEntity orderLine : orderLines) {
            if (orderLine.getVendor() != null && orderLine.getVendor().getCurrency() != null) {
                return orderLine.getVendor().getCurrency();
            }
        }
        return "VND";
    }

    private OrderEntity findOrderByContract(String contractNumber, String requestId) {
        return orderRepo.findByContractNumber(contractNumber)
                .orElseThrow(() -> new ApiException(ApiException.ErrorCode.NOT_FOUND, "Order not found by contract number",
                        HttpStatus.NOT_FOUND.value(), requestId));
    }

    private WarehouseOutboundEntity loadOutboundForMutation(String outboundId, String requestId) {
        UUID id = parseUuid(outboundId, "outboundId", requestId);
        return warehouseOutboundRepo.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ApiException(ApiException.ErrorCode.NOT_FOUND, "Outbound not found",
                        HttpStatus.NOT_FOUND.value(), requestId));
    }

    private void seedApprovals(WarehouseOutboundEntity outbound, int approvalLevels, List<String> customRoles, String requestId) {
        String username = RequestContext.getCurrentUsername();
        for (int level = 1; level <= approvalLevels; level++) {
            WarehouseOutboundApprovalEntity row = new WarehouseOutboundApprovalEntity();
            row.setOutbound(outbound);
            row.setApprovalLevel(level);
            row.setApprovalRole(resolveOutboundRole(level, approvalLevels, customRoles, requestId));
            row.setStatus(Constant.PAYMENT_APPROVAL_STATUS_PENDING);
            row.setCreatedBy(username);
            row.setUpdatedBy(username);
            row.setIsDeleted(false);
            warehouseOutboundApprovalRepo.save(row);
        }
    }

    private void resetApprovals(WarehouseOutboundEntity outbound) {
        String username = RequestContext.getCurrentUsername();
        List<WarehouseOutboundApprovalEntity> approvals = warehouseOutboundApprovalRepo.findByOutboundId(outbound.getId());
        for (WarehouseOutboundApprovalEntity approval : approvals) {
            approval.setStatus(Constant.PAYMENT_APPROVAL_STATUS_PENDING);
            approval.setApprover(null);
            approval.setApprovedAt(null);
            approval.setRejectionReason(null);
            approval.setNote(null);
            approval.setUpdatedBy(username);
            warehouseOutboundApprovalRepo.save(approval);
        }
    }

    private String resolveOutboundRole(int level, int total, List<String> customRoles, String requestId) {
        if (customRoles != null && !customRoles.isEmpty()) {
            String role = customRoles.get(level - 1);
            if (role == null || role.isBlank()) {
                throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "approvalRoles entry must not be blank",
                        HttpStatus.BAD_REQUEST.value(), requestId);
            }
            return role.trim().toUpperCase(Locale.ROOT);
        }
        if (total == 1) return Constant.ROLE_ACCOUNTANT;
        if (total == 2) return level == 1 ? Constant.ROLE_ACCOUNTANT : "HEAD_ACCOUNTANT";
        if (level == total) return "FINAL_APPROVER";
        if (level == 1) return Constant.ROLE_ACCOUNTANT;
        if (level == 2) return "HEAD_ACCOUNTANT";
        return Constant.ROLE_ACCOUNTANT_MANAGER;
    }

    private UserEntity findUserEntity(String requestId) {
        String username = RequestContext.getCurrentUsername();
        return userRepo.findOneByUsername(username)
                .orElseThrow(() -> new ApiException(ApiException.ErrorCode.NOT_FOUND, "User not found",
                        HttpStatus.NOT_FOUND.value(), requestId));
    }

    private void validateOutboundApproverRole(WarehouseOutboundEntity outbound, String approvalRole, String requestId) {
        String username = RequestContext.getCurrentUsername();
        UserInfo currentUserInfo = getCurrentUserInfoFromRedis(requestId);
        if (!PermissionUtils.canActOnWarehouseOutboundApprovalStage(currentUserInfo, approvalRole)) {
            throw new ApiException(ApiException.ErrorCode.FORBIDDEN,
                    "Current user does not have permission for this approval level",
                    HttpStatus.FORBIDDEN.value(), requestId);
        }
        if (PermissionUtils.hasPermission(currentUserInfo, PermissionConstants.WAREHOUSE_OUTBOUND_APPROVE_FINAL)
                && username != null
                && username.equalsIgnoreCase(outbound.getCreatedBy())) {
            throw new ApiException(ApiException.ErrorCode.FORBIDDEN,
                    "Final approver cannot self-approve this outbound",
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

    private boolean canCurrentUserApprove(WarehouseOutboundEntity outbound, String requestId) {
        int nextLevel = outbound.getCurrentApprovalLevel() + 1;
        return warehouseOutboundApprovalRepo.findByOutboundIdAndLevel(outbound.getId(), nextLevel)
                .map(approval -> {
                    try {
                        validateOutboundApproverRole(outbound, approval.getApprovalRole(), requestId);
                        return true;
                    } catch (ApiException e) {
                        return false;
                    }
                })
                .orElse(false);
    }

    private void notifyAccountantsOutboundPendingApproval(WarehouseOutboundEntity outbound) {
        try {
            List<UserEntity> users = userRepo.findByRoleCode(Constant.ROLE_ACCOUNTANT);
            if (users.isEmpty()) {
                log.warn("No users found with role {} to notify for warehouse outbound", Constant.ROLE_ACCOUNTANT);
                return;
            }
            String outboundNumber = outbound.getOutboundNumber() != null ? outbound.getOutboundNumber()
                    : outbound.getId().toString();
            String rId = outbound.getId().toString();
            List<UUID> userIds = users.stream().map(UserEntity::getId).toList();
            notificationService.sendNotificationToUsers(userIds,
                    "Phiếu xuất kho chờ duyệt",
                    String.format("Phiếu xuất kho %s đã được gửi và chờ duyệt.", outboundNumber),
                    NotificationService.NotificationType.APPROVAL,
                    NotificationService.NotificationCategory.WAREHOUSE_OUTBOUND,
                    rId,
                    NotificationService.ReferenceType.WAREHOUSE_OUTBOUND,
                    "/warehouse-outbound/" + rId);
            log.info("Sent warehouse outbound notification to {} accountants [outboundId={}]", userIds.size(), rId);
        } catch (Exception e) {
            log.error("Failed to send warehouse outbound notification to accountants: {}", e.getMessage());
        }
    }

    private PaymentRequestApprovalInfo toApprovalInfo(WarehouseOutboundApprovalEntity approval) {
        PaymentRequestApprovalInfo info = new PaymentRequestApprovalInfo();
        info.setId(approval.getId().toString());
        info.setLevel(approval.getApprovalLevel());
        info.setRole(approval.getApprovalRole());
        info.setApproverId(approval.getApprover() == null ? null : approval.getApprover().getId().toString());
        info.setStatus(approval.getStatus());
        info.setApprovedAt(approval.getApprovedAt() == null ? null : approval.getApprovedAt().toString());
        info.setRejectionReason(approval.getRejectionReason());
        info.setNote(approval.getNote());
        info.setUpdatedBy(approval.getUpdatedBy());
        return info;
    }

    private void applyOutboundToInventory(WarehouseOutboundEntity outbound, String requestId) {
        String username = RequestContext.getCurrentUsername();
        List<WarehouseOutboundDetailEntity> details = warehouseOutboundDetailRepo.findByOutboundId(outbound.getId());
        for (WarehouseOutboundDetailEntity detail : details) {
            ProductEntity product = detail.getProduct();
            BigDecimal quantity = detail.getQuantity() == null ? BigDecimal.ZERO : detail.getQuantity();
            if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            WarehouseInventoryEntity inventory = warehouseInventoryRepo.findByProductId(product.getId())
                    .orElseThrow(() -> new ApiException(ApiException.ErrorCode.BAD_REQUEST,
                            "No inventory record for product", HttpStatus.BAD_REQUEST.value(), requestId));
            BigDecimal onHand = inventory.getQuantityOnHand() == null ? BigDecimal.ZERO : inventory.getQuantityOnHand();
            if (onHand.compareTo(quantity) < 0) {
                throw new ApiException(ApiException.ErrorCode.BAD_REQUEST,
                        "Insufficient quantity on hand for product " + product.getCode(),
                        HttpStatus.BAD_REQUEST.value(), requestId);
            }
            inventory.setQuantityOnHand(onHand.subtract(quantity));
            inventory.setUpdatedBy(username);
            warehouseInventoryRepo.save(inventory);

            WarehouseStockTransactionEntity tx = new WarehouseStockTransactionEntity();
            tx.setProduct(product);
            tx.setDirection(Constant.WAREHOUSE_STOCK_DIRECTION_OUTBOUND);
            tx.setQuantity(quantity);
            tx.setReferenceType(Constant.WAREHOUSE_STOCK_REF_OUTBOUND_DETAIL);
            tx.setReferenceId(detail.getId());
            tx.setNote(outbound.getOutboundReason());
            tx.setCreatedBy(username);
            tx.setUpdatedBy(username);
            tx.setIsDeleted(false);
            warehouseStockTransactionRepo.save(tx);
        }
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
        if (raw == null || raw.isEmpty()) return List.of();
        return raw.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isEmpty()).map(s -> {
            try {
                return UUID.fromString(s);
            } catch (IllegalArgumentException e) {
                throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "attachedFileIds must contain valid UUID strings",
                        HttpStatus.BAD_REQUEST.value(), requestId);
            }
        }).toList();
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
