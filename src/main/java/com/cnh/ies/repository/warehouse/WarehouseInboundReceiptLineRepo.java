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

    @Query("SELECT r.receiptNumber, "
            + "COALESCE(pol.quote, alt.quote, ''), COALESCE(pol.invoice, alt.invoice, ''), "
            + "COALESCE(pol.billOfLadding, alt.billOfLadding, ''), "
            + "COALESCE(pol.receiptWarehouse, alt.receiptWarehouse, ''), "
            + "COALESCE(pol.trackId, alt.trackId, ''), "
            + "COALESCE(pol.purchaseContractNumber, alt.purchaseContractNumber, ''), "
            + "COALESCE(o.contractNumber, altOrder.contractNumber, '') "
            + "FROM WarehouseInboundReceiptLineEntity l "
            + "JOIN l.receipt r "
            + "LEFT JOIN l.purchaseOrderLine pol "
            + "LEFT JOIN pol.purchaseOrder po "
            + "LEFT JOIN po.order o "
            + "LEFT JOIN l.paymentRequestPurchaseOrderLine prpol "
            + "LEFT JOIN prpol.purchaseOrderLine alt "
            + "LEFT JOIN alt.purchaseOrder altPo "
            + "LEFT JOIN altPo.order altOrder "
            + "WHERE l.isDeleted = false AND r.isDeleted = false "
            + "AND r.status NOT IN ('CANCELLED', 'REJECTED') "
            + "AND (pol.quote IN :codes OR alt.quote IN :codes "
            + "OR pol.invoice IN :codes OR alt.invoice IN :codes "
            + "OR pol.billOfLadding IN :codes OR alt.billOfLadding IN :codes "
            + "OR pol.receiptWarehouse IN :codes OR alt.receiptWarehouse IN :codes "
            + "OR pol.trackId IN :codes OR alt.trackId IN :codes "
            + "OR pol.purchaseContractNumber IN :codes OR alt.purchaseContractNumber IN :codes "
            + "OR o.contractNumber IN :codes OR altOrder.contractNumber IN :codes)")
    List<Object[]> findReceiptNumbersByDocumentCodes(@Param("codes") Collection<String> codes);

    @Query("SELECT l.id, r.receiptNumber FROM WarehouseInboundReceiptLineEntity l "
            + "JOIN l.receipt r "
            + "WHERE l.id IN :lineIds AND l.isDeleted = false")
    List<Object[]> findReceiptNumbersByLineIds(@Param("lineIds") Collection<UUID> lineIds);

    @Query("SELECT COUNT(l) FROM WarehouseInboundReceiptLineEntity l WHERE l.receipt.id = :receiptId AND l.isDeleted = false")
    long countActiveLinesByReceiptId(@Param("receiptId") UUID receiptId);

    @Query("SELECT COUNT(l) FROM WarehouseInboundReceiptLineEntity l "
            + "WHERE l.receipt.id = :receiptId AND l.purchaseOrderLine.id = :polId AND l.isDeleted = false")
    long countActiveLinesByReceiptAndPol(@Param("receiptId") UUID receiptId, @Param("polId") UUID polId);
}
