package com.cnh.ies.repository.warehouse;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cnh.ies.entity.warehouse.WarehouseInboundReceiptEntity;
import com.cnh.ies.repository.BaseRepo;

@Repository
public interface WarehouseInboundReceiptRepo extends BaseRepo<WarehouseInboundReceiptEntity, UUID> {

    @Query("SELECT r FROM WarehouseInboundReceiptEntity r JOIN FETCH r.paymentRequest pr "
            + "WHERE pr.id = :paymentRequestId AND r.isDeleted = false ORDER BY r.createdAt DESC")
    List<WarehouseInboundReceiptEntity> findByPaymentRequestId(@Param("paymentRequestId") UUID paymentRequestId);

    @Query("SELECT r FROM WarehouseInboundReceiptEntity r JOIN FETCH r.paymentRequest pr WHERE r.id = :id AND pr.id = :paymentRequestId AND r.isDeleted = false")
    Optional<WarehouseInboundReceiptEntity> findByIdAndPaymentRequestId(
            @Param("id") UUID id, @Param("paymentRequestId") UUID paymentRequestId);

    @Query("SELECT r FROM WarehouseInboundReceiptEntity r JOIN FETCH r.paymentRequest WHERE r.id = :id AND r.isDeleted = false")
    Optional<WarehouseInboundReceiptEntity> findByIdAndIsDeletedFalse(@Param("id") UUID id);
}
