package com.cnh.ies.service.warehouse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.cnh.ies.constant.Constant;
import com.cnh.ies.entity.product.ProductEntity;
import com.cnh.ies.entity.purchaseorder.PurchaseOrderLineEntity;
import com.cnh.ies.entity.warehouse.WarehouseInboundReceiptEntity;
import com.cnh.ies.entity.warehouse.WarehouseInboundReceiptLineEntity;
import com.cnh.ies.entity.warehouse.WarehouseInventoryEntity;
import com.cnh.ies.entity.warehouse.WarehouseStockTransactionEntity;
import com.cnh.ies.exception.ApiException;
import com.cnh.ies.model.general.ListDataModel;
import com.cnh.ies.model.general.PaginationModel;
import com.cnh.ies.model.warehouse.WarehouseInventoryBalanceInfo;
import com.cnh.ies.model.warehouse.WarehouseOutboundRequest;
import com.cnh.ies.model.warehouse.WarehouseStockTransactionInfo;
import com.cnh.ies.repository.product.ProductRepo;
import com.cnh.ies.repository.warehouse.WarehouseInboundReceiptLineRepo;
import com.cnh.ies.repository.warehouse.WarehouseInboundReceiptRepo;
import com.cnh.ies.repository.warehouse.WarehouseInventoryRepo;
import com.cnh.ies.repository.warehouse.WarehouseStockTransactionRepo;
import com.cnh.ies.util.RequestContext;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class WarehouseInventoryService {

    private final WarehouseInboundReceiptRepo warehouseInboundReceiptRepo;
    private final WarehouseInboundReceiptLineRepo warehouseInboundReceiptLineRepo;
    private final WarehouseInventoryRepo warehouseInventoryRepo;
    private final WarehouseStockTransactionRepo warehouseStockTransactionRepo;
    private final ProductRepo productRepo;

    /**
     * After an inbound receipt is fully approved, add received quantities to
     * inventory and write INBOUND ledger rows (idempotent).
     */
    @Transactional
    public void applyInboundAfterApprove(UUID receiptId, String requestId) {
        WarehouseInboundReceiptEntity receipt = warehouseInboundReceiptRepo.findByIdAndIsDeletedFalse(receiptId)
                .orElseThrow(() -> new ApiException(ApiException.ErrorCode.NOT_FOUND, "Receipt not found",
                        HttpStatus.NOT_FOUND.value(), requestId));
        if (receipt.getInventoryPostedAt() != null) {
            return;
        }
        if (!Constant.WAREHOUSE_INBOUND_STATUS_APPROVED.equals(receipt.getStatus())) {
            return;
        }

        List<WarehouseInboundReceiptLineEntity> lines = warehouseInboundReceiptLineRepo.findByReceiptId(receiptId);
        String username = RequestContext.getCurrentUsername();
        for (WarehouseInboundReceiptLineEntity line : lines) {
            BigDecimal qty = line.getQuantityReceived();
            if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            PurchaseOrderLineEntity pol = line.getPurchaseOrderLine() != null
                    ? line.getPurchaseOrderLine()
                    : (line.getPaymentRequestPurchaseOrderLine() != null
                            ? line.getPaymentRequestPurchaseOrderLine().getPurchaseOrderLine()
                            : null);
            if (pol == null || pol.getProduct() == null) {
                log.warn("Skipping inbound line without product [lineId={}, rid={}]", line.getId(), requestId);
                continue;
            }
            ProductEntity product = pol.getProduct();
            addQuantity(product, qty, username);
            saveTransaction(product, Constant.WAREHOUSE_STOCK_DIRECTION_INBOUND, qty,
                    Constant.WAREHOUSE_STOCK_REF_INBOUND_RECEIPT_LINE, line.getId(),
                    "Inbound receipt line", username);
        }

        receipt.setInventoryPostedAt(Instant.now());
        receipt.setUpdatedBy(username);
        warehouseInboundReceiptRepo.save(receipt);
        log.info("Posted warehouse inbound to inventory [receiptId={}, rid={}]", receiptId, requestId);
    }

    public ListDataModel<WarehouseInventoryBalanceInfo> listInventory(String requestId, int page, int limit) {
        int safePage = Math.max(page, 0);
        int safeLimit = Math.max(Math.min(limit, 200), 1);
        Page<WarehouseInventoryEntity> result = warehouseInventoryRepo
                .findAll(PageRequest.of(safePage, safeLimit, Sort.by(Sort.Direction.ASC, "product.name")));
        List<WarehouseInventoryBalanceInfo> items = result.getContent().stream().map(inv -> {
            ProductEntity product = inv.getProduct();
            WarehouseInventoryBalanceInfo info = new WarehouseInventoryBalanceInfo();
            info.setProductId(product.getId().toString());
            info.setProductCode(product.getCode());
            info.setProductName(product.getName());
            info.setQuantityOnHand(inv.getQuantityOnHand());
            return info;
        }).toList();
        return ListDataModel.<WarehouseInventoryBalanceInfo>builder()
                .data(items)
                .pagination(PaginationModel.builder().page(safePage).limit(safeLimit).total(result.getTotalElements())
                        .totalPage(result.getTotalPages()).build())
                .build();
    }

    public WarehouseInventoryBalanceInfo getBalance(String productId, String requestId) {
        UUID pid = parseUuid(productId, "productId", requestId);
        ProductEntity product = productRepo.findById(pid)
                .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
                .orElseThrow(() -> new ApiException(ApiException.ErrorCode.NOT_FOUND, "Product not found",
                        HttpStatus.NOT_FOUND.value(), requestId));
        BigDecimal qty = warehouseInventoryRepo.findByProductId(pid)
                .map(WarehouseInventoryEntity::getQuantityOnHand)
                .orElse(BigDecimal.ZERO);
        WarehouseInventoryBalanceInfo info = new WarehouseInventoryBalanceInfo();
        info.setProductId(product.getId().toString());
        info.setProductCode(product.getCode());
        info.setProductName(product.getName());
        info.setQuantityOnHand(qty);
        return info;
    }

    public List<WarehouseStockTransactionInfo> listTransactionsForProduct(String productId, String requestId) {
        UUID pid = parseUuid(productId, "productId", requestId);
        productRepo.findById(pid)
                .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
                .orElseThrow(() -> new ApiException(ApiException.ErrorCode.NOT_FOUND, "Product not found",
                        HttpStatus.NOT_FOUND.value(), requestId));
        return warehouseStockTransactionRepo.findByProductIdOrderByCreatedAtDesc(pid).stream()
                .map(WarehouseInventoryService::toTxInfo)
                .collect(Collectors.toList());
    }

    @Transactional
    public WarehouseStockTransactionInfo recordOutbound(WarehouseOutboundRequest request, String requestId) {
        if (request == null || request.getProductId() == null || request.getProductId().isBlank()) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "productId is required",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }
        if (request.getQuantity() == null || request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "quantity must be greater than 0",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }
        UUID pid = parseUuid(request.getProductId(), "productId", requestId);
        ProductEntity product = productRepo.findById(pid)
                .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
                .orElseThrow(() -> new ApiException(ApiException.ErrorCode.NOT_FOUND, "Product not found",
                        HttpStatus.NOT_FOUND.value(), requestId));

        WarehouseInventoryEntity inv = warehouseInventoryRepo.findByProductId(pid)
                .orElseThrow(
                        () -> new ApiException(ApiException.ErrorCode.BAD_REQUEST, "No inventory record for product",
                                HttpStatus.BAD_REQUEST.value(), requestId));
        if (inv.getQuantityOnHand().compareTo(request.getQuantity()) < 0) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "Insufficient quantity on hand",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }

        String username = RequestContext.getCurrentUsername();
        inv.setQuantityOnHand(inv.getQuantityOnHand().subtract(request.getQuantity()));
        inv.setUpdatedBy(username);
        warehouseInventoryRepo.save(inv);

        UUID refId = null;
        if (request.getReferenceId() != null && !request.getReferenceId().isBlank()) {
            try {
                refId = UUID.fromString(request.getReferenceId().trim());
            } catch (IllegalArgumentException e) {
                throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "referenceId must be a UUID when provided",
                        HttpStatus.BAD_REQUEST.value(), requestId);
            }
        }
        String refType = request.getReferenceType() == null || request.getReferenceType().isBlank()
                ? "MANUAL_OUTBOUND"
                : request.getReferenceType().trim();
        WarehouseStockTransactionEntity saved = saveTransaction(product, Constant.WAREHOUSE_STOCK_DIRECTION_OUTBOUND,
                request.getQuantity(), refType, refId, request.getNote(), username);
        return toTxInfo(saved);
    }

    private void addQuantity(ProductEntity product, BigDecimal qty, String username) {
        WarehouseInventoryEntity inv = warehouseInventoryRepo.findByProductId(product.getId()).orElseGet(() -> {
            WarehouseInventoryEntity n = new WarehouseInventoryEntity();
            n.setProduct(product);
            n.setQuantityOnHand(BigDecimal.ZERO);
            n.setCreatedBy(username);
            n.setUpdatedBy(username);
            n.setIsDeleted(false);
            return n;
        });
        inv.setQuantityOnHand(inv.getQuantityOnHand().add(qty));
        inv.setUpdatedBy(username);
        warehouseInventoryRepo.save(inv);
    }

    private WarehouseStockTransactionEntity saveTransaction(ProductEntity product, String direction,
            BigDecimal quantity,
            String referenceType, UUID referenceId, String note, String username) {
        WarehouseStockTransactionEntity tx = new WarehouseStockTransactionEntity();
        tx.setProduct(product);
        tx.setDirection(direction);
        tx.setQuantity(quantity);
        tx.setReferenceType(referenceType);
        tx.setReferenceId(referenceId);
        tx.setNote(note);
        tx.setCreatedBy(username);
        tx.setUpdatedBy(username);
        tx.setIsDeleted(false);
        return warehouseStockTransactionRepo.save(tx);
    }

    private static WarehouseStockTransactionInfo toTxInfo(WarehouseStockTransactionEntity e) {
        WarehouseStockTransactionInfo i = new WarehouseStockTransactionInfo();
        i.setId(e.getId().toString());
        i.setProductId(e.getProduct().getId().toString());
        i.setDirection(e.getDirection());
        i.setQuantity(e.getQuantity());
        i.setReferenceType(e.getReferenceType());
        i.setReferenceId(e.getReferenceId() == null ? null : e.getReferenceId().toString());
        i.setNote(e.getNote());
        i.setCreatedAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null);
        return i;
    }

    private static UUID parseUuid(String raw, String field, String requestId) {
        try {
            return UUID.fromString(raw.trim());
        } catch (Exception e) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, field + " must be a valid UUID",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }
    }
}
