package com.cnh.ies.repository.warehouse;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Collection;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cnh.ies.entity.warehouse.WarehouseInboundReceiptLineEntity;
import com.cnh.ies.repository.BaseRepo;

@Repository
public interface WarehouseInboundReceiptLineRepo extends BaseRepo<WarehouseInboundReceiptLineEntity, UUID> {

    @Query("SELECT l FROM WarehouseInboundReceiptLineEntity l "
            + "LEFT JOIN FETCH l.paymentRequestPurchaseOrderLine prpol "
            + "LEFT JOIN FETCH prpol.purchaseOrderLine prpol_pol "
            + "LEFT JOIN FETCH l.purchaseOrderLine pol "
            + "LEFT JOIN FETCH pol.product "
            + "LEFT JOIN FETCH pol.vendor "
            + "LEFT JOIN FETCH pol.purchaseOrder po "
            + "LEFT JOIN FETCH po.order o "
            + "LEFT JOIN FETCH o.customer "
            + "LEFT JOIN FETCH pol.saleOrderLine sol "
            + "LEFT JOIN FETCH sol.order so "
            + "LEFT JOIN FETCH so.customer "
            + "LEFT JOIN FETCH prpol_pol.product "
            + "LEFT JOIN FETCH prpol_pol.vendor "
            + "LEFT JOIN FETCH prpol_pol.purchaseOrder prpol_po "
            + "LEFT JOIN FETCH prpol_po.order prpol_o "
            + "LEFT JOIN FETCH prpol_o.customer "
            + "LEFT JOIN FETCH prpol_pol.saleOrderLine prpol_sol "
            + "LEFT JOIN FETCH prpol_sol.order prpol_so "
            + "LEFT JOIN FETCH prpol_so.customer "
            + "WHERE l.receipt.id = :receiptId AND l.isDeleted = false")
    List<WarehouseInboundReceiptLineEntity> findByReceiptId(@Param("receiptId") UUID receiptId);

    @Query("SELECT l FROM WarehouseInboundReceiptLineEntity l WHERE l.id = :lineId AND l.receipt.id = :receiptId "
            + "AND l.isDeleted = false")
    Optional<WarehouseInboundReceiptLineEntity> findByIdAndReceiptId(
            @Param("lineId") UUID lineId, @Param("receiptId") UUID receiptId);

    @Query("SELECT COUNT(l) FROM WarehouseInboundReceiptLineEntity l "
            + "WHERE l.receipt.id = :receiptId AND l.paymentRequestPurchaseOrderLine.id = :prpolId AND l.isDeleted = false")
    long countActiveLinesByReceiptAndPrpol(@Param("receiptId") UUID receiptId, @Param("prpolId") UUID prpolId);

    @Query("SELECT l.id, r.createdBy FROM WarehouseInboundReceiptLineEntity l "
            + "JOIN l.receipt r "
            + "WHERE l.id IN :lineIds AND l.isDeleted = false")
    List<Object[]> findReceiptOwnersByLineIds(@Param("lineIds") Collection<UUID> lineIds);

    @Query("SELECT COUNT(l) FROM WarehouseInboundReceiptLineEntity l WHERE l.receipt.id = :receiptId AND l.isDeleted = false")
    long countActiveLinesByReceiptId(@Param("receiptId") UUID receiptId);

    @Query("SELECT COUNT(l) FROM WarehouseInboundReceiptLineEntity l "
            + "WHERE l.receipt.id = :receiptId AND l.purchaseOrderLine.id = :polId AND l.isDeleted = false")
    long countActiveLinesByReceiptAndPol(@Param("receiptId") UUID receiptId, @Param("polId") UUID polId);
}
