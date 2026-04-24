package com.cnh.ies.repository.warehouse;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cnh.ies.entity.warehouse.WarehouseInboundReceiptLineEntity;
import com.cnh.ies.repository.BaseRepo;

@Repository
public interface WarehouseInboundReceiptLineRepo extends BaseRepo<WarehouseInboundReceiptLineEntity, UUID> {

    @Query("SELECT l FROM WarehouseInboundReceiptLineEntity l "
            + "JOIN FETCH l.paymentRequestPurchaseOrderLine prpol "
            + "JOIN FETCH prpol.purchaseOrderLine pol "
            + "LEFT JOIN FETCH pol.product "
            + "WHERE l.receipt.id = :receiptId AND l.isDeleted = false")
    List<WarehouseInboundReceiptLineEntity> findByReceiptId(@Param("receiptId") UUID receiptId);

    @Query("SELECT l FROM WarehouseInboundReceiptLineEntity l WHERE l.id = :lineId AND l.receipt.id = :receiptId "
            + "AND l.isDeleted = false")
    Optional<WarehouseInboundReceiptLineEntity> findByIdAndReceiptId(
            @Param("lineId") UUID lineId, @Param("receiptId") UUID receiptId);

    @Query("SELECT COUNT(l) FROM WarehouseInboundReceiptLineEntity l "
            + "WHERE l.receipt.id = :receiptId AND l.paymentRequestPurchaseOrderLine.id = :prpolId AND l.isDeleted = false")
    long countActiveLinesByReceiptAndPrpol(@Param("receiptId") UUID receiptId, @Param("prpolId") UUID prpolId);

    @Query("SELECT COUNT(l) FROM WarehouseInboundReceiptLineEntity l WHERE l.receipt.id = :receiptId AND l.isDeleted = false")
    long countActiveLinesByReceiptId(@Param("receiptId") UUID receiptId);
}
