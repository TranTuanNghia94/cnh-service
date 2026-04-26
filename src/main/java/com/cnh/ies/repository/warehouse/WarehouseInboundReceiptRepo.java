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
            + "LEFT JOIN FETCH r.paymentRequest pr "
            + "LEFT JOIN FETCH pr.vendor "
            + "WHERE r.isDeleted = false "
            + "AND (:status = '' OR r.status = :status) "
            + "AND (:search = '' OR r.receiptNumber LIKE CONCAT('%', :search, '%') "
            + "    OR r.note LIKE CONCAT('%', :search, '%')) "
            + "ORDER BY r.createdAt DESC")
    Page<WarehouseInboundReceiptEntity> findAllFiltered(
            @Param("status") String status,
            @Param("search") String search,
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
