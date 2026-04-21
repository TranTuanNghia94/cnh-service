package com.cnh.ies.service.purchaseorder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.cnh.ies.entity.order.OrderLineEntity;
import com.cnh.ies.entity.product.ProductEntity;
import com.cnh.ies.entity.purchaseorder.PurchaseOrderEntity;
import com.cnh.ies.entity.purchaseorder.PurchaseOrderLineEntity;
import com.cnh.ies.entity.vendors.VendorsEntity;
import com.cnh.ies.exception.ApiException;
import com.cnh.ies.mapper.purchaseorder.PurchaseOrderLineMapper;
import com.cnh.ies.model.purchaseorder.CreatePurchaseOrderLineRequest;
import com.cnh.ies.model.purchaseorder.PurchaseOrderLineInfo;
import com.cnh.ies.model.purchaseorder.UpdatePurchaseOrderLineRequest;
import com.cnh.ies.repository.order.OrderLineRepo;
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
public class PurchaseOrderDetailService {

    private final PurchaseOrderLineRepo purchaseOrderLineRepo;
    private final PurchaseOrderRepo purchaseOrderRepo;
    private final PurchaseOrderLineMapper purchaseOrderLineMapper;
    private final ProductRepo productRepo;
    private final VendorsRepo vendorsRepo;
    private final OrderLineRepo orderLineRepo;

    public List<PurchaseOrderLineInfo> createPurchaseOrderLines(List<CreatePurchaseOrderLineRequest> payload,
            UUID purchaseOrderId, String requestId) {
        log.info("Creating purchase order lines for purchaseOrderId: {} | payload: {}", purchaseOrderId, payload);

        if (payload == null || payload.isEmpty()) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "Payload is required",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }

        PurchaseOrderEntity purchaseOrder = purchaseOrderRepo.findById(purchaseOrderId)
                .orElseThrow(() -> new ApiException(ApiException.ErrorCode.NOT_FOUND, "Purchase order not found",
                        HttpStatus.NOT_FOUND.value(), requestId));

        Map<UUID, ProductEntity> productById = loadProductsById(payload);

        List<PurchaseOrderLineEntity> poLines = payload.stream()
                .map(line -> setRelationsToLine(line, purchaseOrder, productById, requestId))
                .collect(Collectors.toList());

        purchaseOrderLineRepo.saveAll(poLines);

        log.info("Purchase order lines created successfully for purchaseOrderId: {} | requestId: {}", purchaseOrderId, requestId);
        return enrichLineProgressDetails(poLines);
    }

    @Transactional
    public List<PurchaseOrderLineInfo> updatePurchaseOrderLines(List<UpdatePurchaseOrderLineRequest> payload,
            UUID purchaseOrderId, String requestId) {
        log.info("Updating purchase order lines for purchaseOrderId: {} | payload: {}", purchaseOrderId, payload);

        if (payload == null || payload.isEmpty()) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "Payload is required",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }

        PurchaseOrderEntity purchaseOrder = purchaseOrderRepo.findById(purchaseOrderId)
                .orElseThrow(() -> new ApiException(ApiException.ErrorCode.NOT_FOUND, "Purchase order not found",
                        HttpStatus.NOT_FOUND.value(), requestId));

        List<PurchaseOrderLineEntity> poLines = purchaseOrderLineRepo.findByPurchaseOrderId(purchaseOrderId);

        if (poLines.isEmpty()) {
            throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "Purchase order lines not found",
                    HttpStatus.NOT_FOUND.value(), requestId);
        }

        for (UpdatePurchaseOrderLineRequest updateRequest : payload) {
            if (updateRequest.getId() == null || updateRequest.getId().isBlank()) {
                PurchaseOrderLineEntity newPoLine = createPurchaseOrderLineFromUpdateRequest(updateRequest, purchaseOrder,
                        requestId);
                poLines.add(newPoLine);
                continue;
            }

            UUID poLineId;
            try {
                poLineId = UUID.fromString(updateRequest.getId());
            } catch (IllegalArgumentException ex) {
                throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "Invalid purchase order line id",
                        HttpStatus.BAD_REQUEST.value(), requestId);
            }

            PurchaseOrderLineEntity poLine = poLines.stream()
                    .filter(l -> l.getId() != null && l.getId().equals(poLineId))
                    .findFirst()
                    .orElseThrow(() -> new ApiException(ApiException.ErrorCode.NOT_FOUND, "Purchase order line not found",
                            HttpStatus.NOT_FOUND.value(), requestId));

            purchaseOrderLineMapper.applyUpdate(updateRequest, poLine);

            if (updateRequest.getProductId() != null) {
                ProductEntity product = productRepo.findById(UUID.fromString(updateRequest.getProductId()))
                        .orElseThrow(() -> new ApiException(ApiException.ErrorCode.NOT_FOUND, "Product not found",
                                HttpStatus.NOT_FOUND.value(), requestId));
                poLine.setProduct(product);
            }

            if (updateRequest.getVendorId() != null) {
                VendorsEntity vendor = vendorsRepo.findById(UUID.fromString(updateRequest.getVendorId()))
                        .orElseThrow(() -> new ApiException(ApiException.ErrorCode.NOT_FOUND, "Vendor not found",
                                HttpStatus.NOT_FOUND.value(), requestId));
                poLine.setVendor(vendor);
            }

            if (updateRequest.getSaleOrderLineId() != null) {
                OrderLineEntity saleOrderLine = orderLineRepo.findById(UUID.fromString(updateRequest.getSaleOrderLineId()))
                        .orElseThrow(() -> new ApiException(ApiException.ErrorCode.NOT_FOUND, "Sale order line not found",
                                HttpStatus.NOT_FOUND.value(), requestId));
                validateSaleOrderLineMatchesPurchaseOrder(purchaseOrder, saleOrderLine, requestId);
                poLine.setSaleOrderLine(saleOrderLine);
            }
        }

        purchaseOrderLineRepo.saveAll(poLines);

        log.info("Purchase order lines updated successfully for purchaseOrderId: {} | requestId: {}", purchaseOrderId, requestId);
        return enrichLineProgressDetails(poLines);
    }

    public String deletePurchaseOrderLines(List<String> ids, UUID purchaseOrderId, String requestId) {
        log.info("Deleting purchase order lines for purchaseOrderId: {} | ids: {}", purchaseOrderId, ids);

        if (ids == null || ids.isEmpty()) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "Ids is required",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }

        if (purchaseOrderId == null) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "Purchase order id is required",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }

        purchaseOrderRepo.findById(purchaseOrderId)
                .orElseThrow(() -> new ApiException(ApiException.ErrorCode.NOT_FOUND, "Purchase order not found",
                        HttpStatus.NOT_FOUND.value(), requestId));

        List<PurchaseOrderLineEntity> poLines = purchaseOrderLineRepo.findByPurchaseOrderId(purchaseOrderId);

        if (poLines.isEmpty()) {
            throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "Purchase order lines not found",
                    HttpStatus.NOT_FOUND.value(), requestId);
        }

        for (String id : ids) {
            PurchaseOrderLineEntity poLine = poLines.stream()
                    .filter(l -> l.getId().equals(UUID.fromString(id)))
                    .findFirst()
                    .orElseThrow(() -> new ApiException(ApiException.ErrorCode.NOT_FOUND, "Purchase order line not found",
                            HttpStatus.NOT_FOUND.value(), requestId));
            poLine.setIsDeleted(true);
            poLine.setUpdatedBy(RequestContext.getCurrentUsername());
        }

        purchaseOrderLineRepo.saveAll(poLines);

        log.info("Purchase order lines deleted successfully for purchaseOrderId: {} | requestId: {}", purchaseOrderId, requestId);
        return "Purchase order lines deleted successfully";
    }

    private Map<UUID, ProductEntity> loadProductsById(List<CreatePurchaseOrderLineRequest> lines) {
        List<UUID> productIds = lines.stream()
                .filter(l -> l.getProductId() != null && l.getProductId().isPresent())
                .map(l -> UUID.fromString(l.getProductId().get()))
                .collect(Collectors.toList());
        List<ProductEntity> products = productRepo.findByIdIn(productIds);
        return products.stream().collect(Collectors.toMap(ProductEntity::getId, Function.identity()));
    }

    private PurchaseOrderLineEntity setRelationsToLine(CreatePurchaseOrderLineRequest line,
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
            VendorsEntity vendor = vendorsRepo.findById(UUID.fromString(line.getVendorId().get()))
                    .orElseThrow(() -> new ApiException(ApiException.ErrorCode.NOT_FOUND, "Vendor not found",
                            HttpStatus.NOT_FOUND.value(), requestId));
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

    private PurchaseOrderLineEntity createPurchaseOrderLineFromUpdateRequest(UpdatePurchaseOrderLineRequest updateRequest,
            PurchaseOrderEntity purchaseOrder, String requestId) {
        PurchaseOrderLineEntity entity = new PurchaseOrderLineEntity();
        entity.setPurchaseOrder(purchaseOrder);
        entity.setIsDeleted(false);
        entity.setVersion(1L);
        entity.setCreatedBy(RequestContext.getCurrentUsername());
        entity.setUpdatedBy(RequestContext.getCurrentUsername());
        purchaseOrderLineMapper.applyUpdate(updateRequest, entity);

        if (updateRequest.getProductId() != null) {
            ProductEntity product = productRepo.findById(UUID.fromString(updateRequest.getProductId()))
                    .orElseThrow(() -> new ApiException(ApiException.ErrorCode.NOT_FOUND, "Product not found",
                            HttpStatus.NOT_FOUND.value(), requestId));
            entity.setProduct(product);
        }

        if (updateRequest.getVendorId() != null) {
            VendorsEntity vendor = vendorsRepo.findById(UUID.fromString(updateRequest.getVendorId()))
                    .orElseThrow(() -> new ApiException(ApiException.ErrorCode.NOT_FOUND, "Vendor not found",
                            HttpStatus.NOT_FOUND.value(), requestId));
            entity.setVendor(vendor);
        }

        if (updateRequest.getSaleOrderLineId() != null) {
            OrderLineEntity saleOrderLine = orderLineRepo.findById(UUID.fromString(updateRequest.getSaleOrderLineId()))
                    .orElseThrow(() -> new ApiException(ApiException.ErrorCode.NOT_FOUND, "Sale order line not found",
                            HttpStatus.NOT_FOUND.value(), requestId));
            validateSaleOrderLineMatchesPurchaseOrder(purchaseOrder, saleOrderLine, requestId);
            entity.setSaleOrderLine(saleOrderLine);
        }

        return entity;
    }

    private List<PurchaseOrderLineInfo> enrichLineProgressDetails(List<PurchaseOrderLineEntity> poLines) {
        List<PurchaseOrderLineInfo> lineInfos = purchaseOrderLineMapper.toPurchaseOrderLineInfos(poLines);
        for (int i = 0; i < poLines.size(); i++) {
            PurchaseOrderLineEntity poLine = poLines.get(i);
            PurchaseOrderLineInfo lineInfo = lineInfos.get(i);
            BigDecimal poQuantity = Optional.ofNullable(poLine.getQuantity()).orElse(BigDecimal.ZERO);
            BigDecimal orderDetailQuantity = poLine.getSaleOrderLine() != null
                    ? Optional.ofNullable(poLine.getSaleOrderLine().getQuantity()).orElse(BigDecimal.ZERO)
                    : BigDecimal.ZERO;

            lineInfo.setPurchaseOrderQuantity(poQuantity);
            lineInfo.setOrderDetailQuantity(orderDetailQuantity);
            lineInfo.setProcessQuantityDetail(
                    poQuantity.stripTrailingZeros().toPlainString() + "/" + orderDetailQuantity.stripTrailingZeros().toPlainString());
            lineInfo.setProcessPercentage(calculateLineProcessPercentage(poQuantity, orderDetailQuantity));
        }
        return lineInfos;
    }

    private BigDecimal calculateLineProcessPercentage(BigDecimal poQuantity, BigDecimal orderDetailQuantity) {
        if (orderDetailQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal percentage = poQuantity.multiply(BigDecimal.valueOf(100))
                .divide(orderDetailQuantity, 2, RoundingMode.HALF_UP);
        if (percentage.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        if (percentage.compareTo(BigDecimal.valueOf(100)) > 0) {
            return BigDecimal.valueOf(100);
        }
        return percentage;
    }

}
