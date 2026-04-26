package com.cnh.ies.repository.warehouse;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cnh.ies.entity.warehouse.WarehouseInboundReceiptFeeEntity;
import com.cnh.ies.repository.BaseRepo;

@Repository
public interface WarehouseInboundReceiptFeeRepo extends BaseRepo<WarehouseInboundReceiptFeeEntity, UUID> {

    @Query("SELECT f FROM WarehouseInboundReceiptFeeEntity f WHERE f.receipt.id = :receiptId AND f.isDeleted = false ORDER BY f.createdAt ASC")
    List<WarehouseInboundReceiptFeeEntity> findByReceiptId(@Param("receiptId") UUID receiptId);
}
