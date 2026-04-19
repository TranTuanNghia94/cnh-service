package com.cnh.ies.service.purchaseorder;

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
                info.setPurchaseOrderLines(
                        purchaseOrderLineMapper.toPurchaseOrderLineInfos(poLines).stream().collect(Collectors.toSet()));
            }

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

        purchaseOrderRepo.save(po.get());
        log.info("Purchase order updated successfully with requestId: {}", requestId);
        return purchaseOrderMapper.toPurchaseOrderInfo(po.get());
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
}
