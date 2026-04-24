package com.cnh.ies.repository.warehouse;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cnh.ies.entity.warehouse.WarehouseInboundReceiptApprovalEntity;
import com.cnh.ies.repository.BaseRepo;

@Repository
public interface WarehouseInboundReceiptApprovalRepo extends BaseRepo<WarehouseInboundReceiptApprovalEntity, UUID> {

    @Query("SELECT a FROM WarehouseInboundReceiptApprovalEntity a LEFT JOIN FETCH a.approver "
            + "WHERE a.receipt.id = :receiptId AND a.isDeleted = false ORDER BY a.approvalLevel ASC")
    List<WarehouseInboundReceiptApprovalEntity> findByReceiptId(@Param("receiptId") UUID receiptId);

    @Query("SELECT a FROM WarehouseInboundReceiptApprovalEntity a WHERE a.receipt.id = :receiptId "
            + "AND a.approvalLevel = :level AND a.isDeleted = false")
    Optional<WarehouseInboundReceiptApprovalEntity> findByReceiptIdAndLevel(
            @Param("receiptId") UUID receiptId, @Param("level") Integer level);
}
