package com.cnh.ies.repository.warehouse;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cnh.ies.entity.payment.PaymentRequestPurchaseOrderLineEntity;
import com.cnh.ies.entity.warehouse.WarehouseInboundReceiptEntity;
import com.cnh.ies.repository.BaseRepo;

@Repository
public interface WarehouseInboundReceiptRepo extends BaseRepo<WarehouseInboundReceiptEntity, UUID> {

    @Query("SELECT r FROM WarehouseInboundReceiptEntity r WHERE r.isDeleted = false")
    Page<WarehouseInboundReceiptEntity> findAllAndIsDeletedFalse(Pageable pageable);

    @Query("SELECT r.receiptNumber FROM WarehouseInboundReceiptEntity r "
            + "WHERE r.receiptNumber LIKE CONCAT(:prefix, '.%') AND r.isDeleted = false")
    List<String> findReceiptNumbersByPrefix(@Param("prefix") String prefix);

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r.receiptNumber FROM WarehouseInboundReceiptEntity r "
            + "WHERE r.receiptNumber LIKE CONCAT(:prefix, '.%') AND r.isDeleted = false")
    List<String> findReceiptNumbersByPrefixForUpdate(@Param("prefix") String prefix);

    @Query("SELECT COALESCE(SUM(l.quantityReceived), 0) "
            + "FROM WarehouseInboundReceiptLineEntity l "
            + "JOIN l.receipt r "
            + "WHERE (l.purchaseOrderLine.id = :polId "
            + "    OR l.paymentRequestPurchaseOrderLine.purchaseOrderLine.id = :polId) "
            + "AND l.isDeleted = false AND r.isDeleted = false "
            + "AND r.status NOT IN ('CANCELLED', 'REJECTED')")
    BigDecimal sumReceivedQuantityByPurchaseOrderLineId(@Param("polId") UUID polId);

    @Query("SELECT r FROM WarehouseInboundReceiptEntity r "
            + "LEFT JOIN r.paymentRequest pr "
            + "LEFT JOIN PaymentRequestPurchaseOrderLineEntity prpol ON prpol.paymentRequest.id = pr.id "
            + "LEFT JOIN prpol.purchaseOrderLine pol "
            + "LEFT JOIN pol.purchaseOrder po "
            + "LEFT JOIN po.order o "
            + "LEFT JOIN o.customer c "
            + "WHERE r.isDeleted = false "
            + "AND (:createdBy = '' OR LOWER(COALESCE(r.createdBy, '')) LIKE LOWER(CONCAT('%', :createdBy, '%'))) "
            + "AND (:inboundNumber = '' OR LOWER(COALESCE(r.receiptNumber, '')) LIKE LOWER(CONCAT('%', :inboundNumber, '%'))) "
            + "AND (:contractNumber = '' OR LOWER(COALESCE(o.contractNumber, '')) LIKE LOWER(CONCAT('%', :contractNumber, '%'))) "
            + "AND (:customerName = '' OR LOWER(COALESCE(c.name, '')) LIKE LOWER(CONCAT('%', :customerName, '%'))) "
            + "AND (:orderNumber = '' OR LOWER(CONCAT(COALESCE(o.orderPrefix, ''), '.', CONCAT('', o.orderNumber))) LIKE LOWER(CONCAT('%', :orderNumber, '%'))) "
            + "AND (:status = '' OR LOWER(COALESCE(r.status, '')) LIKE LOWER(CONCAT('%', :status, '%'))) "
            + "ORDER BY r.createdAt DESC")
    Page<WarehouseInboundReceiptEntity> findAllFiltered(
            @Param("createdBy") String createdBy,
            @Param("inboundNumber") String inboundNumber,
            @Param("contractNumber") String contractNumber,
            @Param("customerName") String customerName,
            @Param("orderNumber") String orderNumber,
            @Param("status") String status,
            Pageable pageable);

    @Query("SELECT r FROM WarehouseInboundReceiptEntity r LEFT JOIN FETCH r.paymentRequest pr "
            + "WHERE pr.id = :paymentRequestId AND r.isDeleted = false ORDER BY r.createdAt DESC")
    List<WarehouseInboundReceiptEntity> findByPaymentRequestId(@Param("paymentRequestId") UUID paymentRequestId);

    @Query("SELECT r FROM WarehouseInboundReceiptEntity r LEFT JOIN FETCH r.paymentRequest pr WHERE r.id = :id AND pr.id = :paymentRequestId AND r.isDeleted = false")
    Optional<WarehouseInboundReceiptEntity> findByIdAndPaymentRequestId(
            @Param("id") UUID id, @Param("paymentRequestId") UUID paymentRequestId);

    @Query("SELECT r FROM WarehouseInboundReceiptEntity r LEFT JOIN FETCH r.paymentRequest WHERE r.id = :id AND r.isDeleted = false")
    Optional<WarehouseInboundReceiptEntity> findByIdAndIsDeletedFalse(@Param("id") UUID id);
}
