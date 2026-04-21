package com.cnh.ies.service.purchaseorder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.cnh.ies.constant.Constant;
import com.cnh.ies.entity.order.OrderEntity;
import com.cnh.ies.entity.order.OrderLineEntity;
import com.cnh.ies.entity.product.ProductEntity;
import com.cnh.ies.entity.purchaseorder.PurchaseOrderEntity;
import com.cnh.ies.entity.purchaseorder.PurchaseOrderLineEntity;
import com.cnh.ies.entity.vendors.VendorsEntity;
import com.cnh.ies.exception.ApiException;
import com.cnh.ies.mapper.purchaseorder.PurchaseOrderLineMapper;
import com.cnh.ies.mapper.purchaseorder.PurchaseOrderMapper;
import com.cnh.ies.model.general.ListDataModel;
import com.cnh.ies.model.general.PaginationModel;
import com.cnh.ies.model.purchaseorder.CreatePurchaseOrderLineRequest;
import com.cnh.ies.model.purchaseorder.CreatePurchaseOrderRequest;
import com.cnh.ies.model.purchaseorder.PurchaseOrderInfo;
import com.cnh.ies.model.purchaseorder.PurchaseOrderLineInfo;
import com.cnh.ies.repository.order.OrderLineRepo;
import com.cnh.ies.repository.order.OrderRepo;
import com.cnh.ies.repository.product.ProductRepo;
import com.cnh.ies.repository.purchaseorder.PurchaseOrderLineRepo;
import com.cnh.ies.repository.purchaseorder.PurchaseOrderRepo;
import com.cnh.ies.repository.vendors.VendorsRepo;
import com.cnh.ies.util.RequestContext;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseOrderService {

    private final PurchaseOrderRepo purchaseOrderRepo;
    private final PurchaseOrderLineRepo purchaseOrderLineRepo;
    private final PurchaseOrderMapper purchaseOrderMapper;
    private final PurchaseOrderLineMapper purchaseOrderLineMapper;
    private final VendorsRepo vendorsRepo;
    private final OrderRepo orderRepo;
    private final OrderLineRepo orderLineRepo;
    private final ProductRepo productRepo;
    private final PurchaseOrderNumberService purchaseOrderNumberService;

    public ListDataModel<PurchaseOrderInfo> getAllPurchaseOrders(String requestId, Integer page, Integer limit) {
        try {
            log.info("Getting all purchase orders with requestId: {}", requestId);

            Page<PurchaseOrderEntity> purchaseOrders = purchaseOrderRepo.findAllAndIsDeletedFalse(PageRequest.of(page, limit));
            List<PurchaseOrderInfo> infos = purchaseOrders.stream()
                    .map(purchaseOrderMapper::toPurchaseOrderInfo)
                    .collect(Collectors.toList());
            infos.forEach(info -> info.setProcessPercentage(BigDecimal.ZERO));
            Map<UUID, PurchaseOrderInfo> infoByPurchaseOrderId = infos.stream()
                    .collect(Collectors.toMap(info -> UUID.fromString(info.getId()), Function.identity()));

            List<UUID> purchaseOrderIds = purchaseOrders.stream().map(PurchaseOrderEntity::getId).collect(Collectors.toList());
            if (!purchaseOrderIds.isEmpty()) {
                Map<UUID, List<PurchaseOrderLineEntity>> poLineByPurchaseOrderId = purchaseOrderLineRepo
                        .findAllByPurchaseOrderIds(purchaseOrderIds)
                        .stream()
                        .collect(Collectors.groupingBy(pol -> pol.getPurchaseOrder().getId()));
                poLineByPurchaseOrderId.forEach((purchaseOrderId, poLines) -> {
                    PurchaseOrderInfo info = infoByPurchaseOrderId.get(purchaseOrderId);
                    PurchaseOrderEntity purchaseOrder = purchaseOrders.stream()
                            .filter(po -> po.getId().equals(purchaseOrderId))
                            .findFirst()
                            .orElse(null);
                    if (info != null) {
                        info.setProcessPercentage(calculatePurchaseOrderProgressPercentage(purchaseOrder, poLines));
                    }
                });
            }

            PaginationModel pagination = PaginationModel.builder()
                    .page(page)
                    .limit(limit)
                    .total(purchaseOrders.getTotalElements())
                    .totalPage(purchaseOrders.getTotalPages())
                    .build();

            log.info("Getting all purchase orders success with requestId: {} | total: {} totalPage: {}", requestId,
                    purchaseOrders.getTotalElements(), purchaseOrders.getTotalPages());

            return ListDataModel.<PurchaseOrderInfo>builder()
                    .data(infos)
                    .pagination(pagination)
                    .build();
        } catch (Exception e) {
            log.error("Error getting all purchase orders", e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, "Error getting all purchase orders",
                    HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }

    @Transactional
    public PurchaseOrderInfo createPurchaseOrder(CreatePurchaseOrderRequest request, String requestId) {
        try {
            log.info("Creating purchase order with requestId: {} | request: {}", requestId, request);

            OrderEntity order = null;
            if (request.getOrderId() != null) {
                order = getOrderOrThrow(request.getOrderId(), requestId);
            }

            PurchaseOrderEntity po = purchaseOrderMapper.toPurchaseOrderEntity(request, order);
            po.setPoNumber(purchaseOrderNumberService.generateNextNumberOrReset());
            po.setPoPrefix(purchaseOrderNumberService.generatePoPrefix());

            PurchaseOrderEntity savedPo = purchaseOrderRepo.save(po);
            log.info("Purchase order created successfully 1/3 requestId: {}", requestId);

            if (request.getPurchaseOrderLines() != null && !request.getPurchaseOrderLines().isEmpty()) {
                log.info("Processing {} purchase order lines for requestId: {}", request.getPurchaseOrderLines().size(), requestId);

                Map<UUID, ProductEntity> productById = loadProductsById(request.getPurchaseOrderLines());

                List<PurchaseOrderLineEntity> poLines = request.getPurchaseOrderLines().stream()
                        .map(line -> toPurchaseOrderLineEntity(line, savedPo, productById, requestId))
                        .collect(Collectors.toList());

                purchaseOrderLineRepo.saveAll(poLines);
                log.info("Purchase order lines created successfully 2/3 requestId: {}", requestId);
            }

            log.info("Purchase order created successfully 3/3 requestId: {}", requestId);
            return purchaseOrderMapper.toPurchaseOrderInfo(savedPo);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error creating purchase order", e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, "Error creating purchase order",
                    HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }

    public PurchaseOrderInfo getPurchaseOrderByCode(String code, String requestId) {
        try {
            log.info("Getting purchase order by code with requestId: {} | code: {}", requestId, code);
            String[] codeParts = code.split("\\.");

            if (codeParts.length != 2) {
                throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "Invalid code format",
                        HttpStatus.BAD_REQUEST.value(), requestId);
            }

            String poPrefix = codeParts[0];
            Integer poNumber = Integer.parseInt(codeParts[1]);
            Optional<PurchaseOrderEntity> po = purchaseOrderRepo.findByPoPrefixAndPoNumber(poPrefix, poNumber);
            if (po.isEmpty()) {
                throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "Purchase order not found",
                        HttpStatus.NOT_FOUND.value(), requestId);
            }

            PurchaseOrderInfo info = purchaseOrderMapper.toPurchaseOrderInfo(po.get());

            List<PurchaseOrderLineEntity> poLines = purchaseOrderLineRepo.findAllByPurchaseOrderId(po.get().getId());
            if (!poLines.isEmpty()) {
                List<PurchaseOrderLineInfo> poLineInfos = purchaseOrderLineMapper.toPurchaseOrderLineInfos(poLines);
                for (int i = 0; i < poLines.size(); i++) {
                    PurchaseOrderLineInfo poLineInfo = poLineInfos.get(i);
                    PurchaseOrderLineEntity poLineEntity = poLines.get(i);
                    poLineInfo.setProcessPercentage(calculatePurchaseOrderLineProgressPercentage(poLineEntity));
                    poLineInfo.setPurchaseOrderQuantity(Optional.ofNullable(poLineEntity.getQuantity()).orElse(BigDecimal.ZERO));
                    poLineInfo.setOrderDetailQuantity(getOrderDetailQuantity(poLineEntity));
                    poLineInfo.setProcessQuantityDetail(buildProcessQuantityDetail(poLineEntity));
                }
                info.setPurchaseOrderLines(
                        poLineInfos.stream().collect(Collectors.toSet()));
            }
            info.setProcessPercentage(calculatePurchaseOrderProgressPercentage(po.get(), poLines));

            log.info("Purchase order fetched successfully with requestId: {}", requestId);
            return info;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting purchase order by code", e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, "Error getting purchase order by code",
                    HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }

    @Transactional
    public PurchaseOrderInfo updatePurchaseOrderStatus(String id, String status, String requestId) {
        log.info("Updating purchase order status with requestId: {} | id: {} | status: {}", requestId, id, status);
        Optional<PurchaseOrderEntity> po = purchaseOrderRepo.findById(UUID.fromString(id));
        if (po.isEmpty()) {
            throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "Purchase order not found",
                    HttpStatus.NOT_FOUND.value(), requestId);
        }

        if (po.get().getIsDeleted()) {
            throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "Purchase order already deleted",
                    HttpStatus.NOT_FOUND.value(), requestId);
        }

        String normalizedStatus = getPoStatusName(status);
        if (po.get().getStatus().equals(normalizedStatus)) {
            return purchaseOrderMapper.toPurchaseOrderInfo(po.get());
        }

        po.get().setStatus(normalizedStatus);
        po.get().setUpdatedBy(RequestContext.getCurrentUsername());

        purchaseOrderRepo.save(po.get());
        log.info("Purchase order status updated successfully with requestId: {}", requestId);
        return purchaseOrderMapper.toPurchaseOrderInfo(po.get());
    }

    @Transactional
    public PurchaseOrderInfo updatePurchaseOrder(CreatePurchaseOrderRequest request, String requestId) {
        log.info("Updating purchase order with requestId: {} | request: {}", requestId, request);

        Optional<PurchaseOrderEntity> po = purchaseOrderRepo.findById(UUID.fromString(request.getId().orElse(null)));
        if (po.isEmpty()) {
            throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "Purchase order not found",
                    HttpStatus.NOT_FOUND.value(), requestId);
        }

        if (request.getOrderId() != null) {
            OrderEntity order = getOrderOrThrow(request.getOrderId(), requestId);
            po.get().setOrder(order);
        }

        po.get().setOrderDate(request.getOrderDate());
        po.get().setExpectedDeliveryDate(request.getExpectedDeliveryDate());
        po.get().setStatus(getPoStatusName(request.getStatus()));
        po.get().setNotes(request.getNotes());
        po.get().setUpdatedBy(RequestContext.getCurrentUsername());

        PurchaseOrderEntity savedPo = purchaseOrderRepo.save(po.get());
        upsertPurchaseOrderLines(savedPo, request.getPurchaseOrderLines(), requestId);

        PurchaseOrderInfo info = purchaseOrderMapper.toPurchaseOrderInfo(savedPo);
        List<PurchaseOrderLineEntity> poLines = purchaseOrderLineRepo.findAllByPurchaseOrderId(savedPo.getId());
        if (!poLines.isEmpty()) {
            List<PurchaseOrderLineInfo> poLineInfos = purchaseOrderLineMapper.toPurchaseOrderLineInfos(poLines);
            for (int i = 0; i < poLines.size(); i++) {
                PurchaseOrderLineInfo poLineInfo = poLineInfos.get(i);
                PurchaseOrderLineEntity poLineEntity = poLines.get(i);
                poLineInfo.setProcessPercentage(calculatePurchaseOrderLineProgressPercentage(poLineEntity));
                poLineInfo.setPurchaseOrderQuantity(Optional.ofNullable(poLineEntity.getQuantity()).orElse(BigDecimal.ZERO));
                poLineInfo.setOrderDetailQuantity(getOrderDetailQuantity(poLineEntity));
                poLineInfo.setProcessQuantityDetail(buildProcessQuantityDetail(poLineEntity));
            }
            info.setPurchaseOrderLines(poLineInfos.stream().collect(Collectors.toSet()));
        }
        info.setProcessPercentage(calculatePurchaseOrderProgressPercentage(savedPo, poLines));

        log.info("Purchase order updated successfully with requestId: {}", requestId);
        return info;
    }

    @Transactional
    public String deletePurchaseOrder(String id, String requestId) {
        try {
            log.info("Deleting purchase order with requestId: {} | id: {}", requestId, id);
            Optional<PurchaseOrderEntity> po = purchaseOrderRepo.findByIdAndIsDeletedFalse(UUID.fromString(id));
            if (po.isEmpty()) {
                throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "Purchase order not found",
                        HttpStatus.NOT_FOUND.value(), requestId);
            }
            po.get().setIsDeleted(true);
            po.get().setUpdatedBy(RequestContext.getCurrentUsername());

            purchaseOrderRepo.save(po.get());
            return "Purchase order deleted successfully";
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error deleting purchase order with requestId: {} | id: {}", requestId, id, e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, "Error deleting purchase order",
                    HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }

    private VendorsEntity getVendorOrThrow(String vendorId, String requestId) {
        return vendorsRepo.findById(UUID.fromString(vendorId)).orElseThrow(() -> {
            log.error("Vendor not found with id: {} | RequestId: {}", vendorId, requestId);
            return new ApiException(ApiException.ErrorCode.NOT_FOUND, "Vendor not found",
                    HttpStatus.NOT_FOUND.value(), requestId);
        });
    }

    private OrderEntity getOrderOrThrow(String orderId, String requestId) {
        return orderRepo.findById(UUID.fromString(orderId)).orElseThrow(() -> {
            log.error("Order not found with id: {} | RequestId: {}", orderId, requestId);
            return new ApiException(ApiException.ErrorCode.NOT_FOUND, "Order not found",
                    HttpStatus.NOT_FOUND.value(), requestId);
        });
    }

    private Map<UUID, ProductEntity> loadProductsById(List<CreatePurchaseOrderLineRequest> lines) {
        List<UUID> productIds = lines.stream()
                .filter(l -> l.getProductId() != null && l.getProductId().isPresent())
                .map(l -> UUID.fromString(l.getProductId().get()))
                .collect(Collectors.toList());
        List<ProductEntity> products = productRepo.findByIdIn(productIds);
        return products.stream().collect(Collectors.toMap(ProductEntity::getId, Function.identity()));
    }

    private PurchaseOrderLineEntity toPurchaseOrderLineEntity(CreatePurchaseOrderLineRequest line,
            PurchaseOrderEntity purchaseOrder, Map<UUID, ProductEntity> productById, String requestId) {
        PurchaseOrderLineEntity entity = purchaseOrderLineMapper.toPurchaseOrderLineEntity(line, purchaseOrder);

        if (line.getProductId() != null && line.getProductId().isPresent()) {
            UUID productId = UUID.fromString(line.getProductId().get());
            ProductEntity product = Optional.ofNullable(productById.get(productId))
                    .orElseThrow(() -> new ApiException(ApiException.ErrorCode.NOT_FOUND, "Product not found",
                            HttpStatus.NOT_FOUND.value(), requestId));
            entity.setProduct(product);
        }

        if (line.getVendorId() != null && line.getVendorId().isPresent()) {
            VendorsEntity vendor = getVendorOrThrow(line.getVendorId().get(), requestId);
            entity.setVendor(vendor);
        }

        if (line.getSaleOrderLineId() != null && line.getSaleOrderLineId().isPresent()) {
            UUID saleOrderLineId = UUID.fromString(line.getSaleOrderLineId().get());
            OrderLineEntity saleOrderLine = orderLineRepo.findById(saleOrderLineId)
                    .orElseThrow(() -> new ApiException(ApiException.ErrorCode.NOT_FOUND, "Sale order line not found",
                            HttpStatus.NOT_FOUND.value(), requestId));
            validateSaleOrderLineMatchesPurchaseOrder(purchaseOrder, saleOrderLine, requestId);
            entity.setSaleOrderLine(saleOrderLine);
        }

        return entity;
    }

    private void validateSaleOrderLineMatchesPurchaseOrder(PurchaseOrderEntity purchaseOrder, OrderLineEntity saleOrderLine,
            String requestId) {
        if (purchaseOrder.getOrder() == null) {
            return;
        }
        if (saleOrderLine.getOrder() == null || !saleOrderLine.getOrder().getId().equals(purchaseOrder.getOrder().getId())) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST,
                    "Sale order line does not belong to purchase order source order",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }
    }

    private String getPoStatusName(String status) {
        return Optional.ofNullable(Constant.PO_STATUS_MAP.get(status)).orElse(Constant.PO_STATUS_DRAFT);
    }

    private void upsertPurchaseOrderLines(PurchaseOrderEntity purchaseOrder, List<CreatePurchaseOrderLineRequest> requestLines,
            String requestId) {
        if (requestLines == null || requestLines.isEmpty()) {
            return;
        }

        List<PurchaseOrderLineEntity> existingLines = purchaseOrderLineRepo.findByPurchaseOrderId(purchaseOrder.getId());
        Map<UUID, PurchaseOrderLineEntity> existingLineById = existingLines.stream()
                .filter(line -> line.getId() != null)
                .collect(Collectors.toMap(PurchaseOrderLineEntity::getId, Function.identity()));

        Map<UUID, ProductEntity> productById = loadProductsById(requestLines);

        for (CreatePurchaseOrderLineRequest requestLine : requestLines) {
            PurchaseOrderLineEntity targetLine;
            UUID lineId = getOptionalUuid(requestLine.getId(), "purchase order line id", requestId);
            if (lineId != null) {
                targetLine = Optional.ofNullable(existingLineById.get(lineId))
                        .orElseThrow(() -> new ApiException(ApiException.ErrorCode.NOT_FOUND, "Purchase order line not found",
                                HttpStatus.NOT_FOUND.value(), requestId));
            } else {
                targetLine = new PurchaseOrderLineEntity();
                targetLine.setPurchaseOrder(purchaseOrder);
                targetLine.setVersion(1L);
                targetLine.setIsDeleted(false);
                targetLine.setCreatedBy(RequestContext.getCurrentUsername());
            }

            applyCreatePurchaseOrderLineRequest(targetLine, requestLine);
            targetLine.setUpdatedBy(RequestContext.getCurrentUsername());
            applyLineRelations(targetLine, requestLine, purchaseOrder, productById, requestId);
            purchaseOrderLineRepo.save(targetLine);
        }
    }

    private void applyCreatePurchaseOrderLineRequest(PurchaseOrderLineEntity entity, CreatePurchaseOrderLineRequest requestLine) {
        entity.setLink(requestLine.getLink());
        entity.setQuantity(requestLine.getQuantity());
        entity.setUom1(requestLine.getUom1());
        entity.setUom2(requestLine.getUom2());
        entity.setUnitPrice(requestLine.getUnitPrice());
        entity.setIsTaxIncluded(requestLine.getIsTaxIncluded() != null ? requestLine.getIsTaxIncluded() : false);
        entity.setTax(requestLine.getTax());
        entity.setTotalBeforeTax(requestLine.getTotalBeforeTax());
        entity.setTotalPrice(requestLine.getTotalPrice());
        entity.setCurrency(requestLine.getCurrency());
        entity.setExchangeRate(requestLine.getExchangeRate());
        entity.setTotalPriceVnd(requestLine.getTotalPriceVnd());
        entity.setNote(requestLine.getNote());
        entity.setQuote(requestLine.getQuote());
        entity.setInvoice(requestLine.getInvoice());
        entity.setBillOfLadding(requestLine.getBillOfLadding());
        entity.setReceiptWarehouse(requestLine.getReceiptWarehouse());
        entity.setTrackId(requestLine.getTrackId());
        entity.setPurchaseContractNumber(requestLine.getPurchaseContractNumber());
    }

    private void applyLineRelations(PurchaseOrderLineEntity entity, CreatePurchaseOrderLineRequest requestLine,
            PurchaseOrderEntity purchaseOrder, Map<UUID, ProductEntity> productById, String requestId) {
        if (requestLine.getProductId() != null && requestLine.getProductId().isPresent()
                && requestLine.getProductId().get() != null && !requestLine.getProductId().get().isBlank()) {
            UUID productId = UUID.fromString(requestLine.getProductId().get());
            ProductEntity product = Optional.ofNullable(productById.get(productId))
                    .orElseThrow(() -> new ApiException(ApiException.ErrorCode.NOT_FOUND, "Product not found",
                            HttpStatus.NOT_FOUND.value(), requestId));
            entity.setProduct(product);
        } else {
            entity.setProduct(null);
        }

        if (requestLine.getVendorId() != null && requestLine.getVendorId().isPresent()
                && requestLine.getVendorId().get() != null && !requestLine.getVendorId().get().isBlank()) {
            VendorsEntity vendor = getVendorOrThrow(requestLine.getVendorId().get(), requestId);
            entity.setVendor(vendor);
        } else {
            entity.setVendor(null);
        }

        if (requestLine.getSaleOrderLineId() != null && requestLine.getSaleOrderLineId().isPresent()
                && requestLine.getSaleOrderLineId().get() != null && !requestLine.getSaleOrderLineId().get().isBlank()) {
            UUID saleOrderLineId = UUID.fromString(requestLine.getSaleOrderLineId().get());
            OrderLineEntity saleOrderLine = orderLineRepo.findById(saleOrderLineId)
                    .orElseThrow(() -> new ApiException(ApiException.ErrorCode.NOT_FOUND, "Sale order line not found",
                            HttpStatus.NOT_FOUND.value(), requestId));
            validateSaleOrderLineMatchesPurchaseOrder(purchaseOrder, saleOrderLine, requestId);
            entity.setSaleOrderLine(saleOrderLine);
        } else {
            entity.setSaleOrderLine(null);
        }
    }

    private UUID getOptionalUuid(Optional<String> value, String fieldName, String requestId) {
        if (value == null || value.isEmpty() || value.get() == null || value.get().isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value.get());
        } catch (IllegalArgumentException e) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "Invalid " + fieldName,
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }
    }

    private BigDecimal calculatePurchaseOrderLineProgressPercentage(PurchaseOrderLineEntity poLine) {
        if (poLine.getSaleOrderLine() == null || poLine.getSaleOrderLine().getQuantity() == null
                || poLine.getSaleOrderLine().getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal poQuantity = Optional.ofNullable(poLine.getQuantity()).orElse(BigDecimal.ZERO);
        BigDecimal orderLineQuantity = poLine.getSaleOrderLine().getQuantity();
        BigDecimal percentage = poQuantity.multiply(BigDecimal.valueOf(100))
                .divide(orderLineQuantity, 2, RoundingMode.HALF_UP);
        return clampPercentage(percentage);
    }

    private BigDecimal calculatePurchaseOrderProgressPercentage(PurchaseOrderEntity purchaseOrder,
            List<PurchaseOrderLineEntity> poLines) {
        if (purchaseOrder == null || purchaseOrder.getOrder() == null || purchaseOrder.getOrder().getId() == null) {
            return BigDecimal.ZERO;
        }

        List<OrderLineEntity> orderLines = orderLineRepo.findByOrderId(purchaseOrder.getOrder().getId());
        BigDecimal totalOrderLineQuantity = orderLines.stream()
                .map(OrderLineEntity::getQuantity)
                .filter(quantity -> quantity != null && quantity.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPurchaseOrderQuantity = BigDecimal.ZERO;

        for (PurchaseOrderLineEntity poLine : poLines) {
            if (poLine.getSaleOrderLine() == null || poLine.getSaleOrderLine().getQuantity() == null
                    || poLine.getSaleOrderLine().getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal poQuantity = Optional.ofNullable(poLine.getQuantity()).orElse(BigDecimal.ZERO);

            totalPurchaseOrderQuantity = totalPurchaseOrderQuantity.add(poQuantity.max(BigDecimal.ZERO));
        }

        if (totalOrderLineQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal percentage = totalPurchaseOrderQuantity.multiply(BigDecimal.valueOf(100))
                .divide(totalOrderLineQuantity, 2, RoundingMode.HALF_UP);
        return clampPercentage(percentage);
    }

    private BigDecimal clampPercentage(BigDecimal value) {
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        if (value.compareTo(BigDecimal.valueOf(100)) > 0) {
            return BigDecimal.valueOf(100);
        }
        return value;
    }

    private BigDecimal getOrderDetailQuantity(PurchaseOrderLineEntity poLine) {
        if (poLine.getSaleOrderLine() == null || poLine.getSaleOrderLine().getQuantity() == null) {
            return BigDecimal.ZERO;
        }
        return poLine.getSaleOrderLine().getQuantity();
    }

    private String buildProcessQuantityDetail(PurchaseOrderLineEntity poLine) {
        BigDecimal poQuantity = Optional.ofNullable(poLine.getQuantity()).orElse(BigDecimal.ZERO);
        BigDecimal orderDetailQuantity = getOrderDetailQuantity(poLine);
        return poQuantity.stripTrailingZeros().toPlainString() + "/" + orderDetailQuantity.stripTrailingZeros().toPlainString();
    }
}
