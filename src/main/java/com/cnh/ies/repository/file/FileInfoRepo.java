package com.cnh.ies.repository.file;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.cnh.ies.entity.file.FileInfoEntity;
import com.cnh.ies.repository.BaseRepo;

@Repository
public interface FileInfoRepo extends BaseRepo<FileInfoEntity, UUID> {

    List<FileInfoEntity> findByPaymentRequestIdAndIsDeletedFalseOrderByCreatedAtAsc(UUID paymentRequestId);

    List<FileInfoEntity> findByWarehouseInboundReceiptIdAndIsDeletedFalseOrderByCreatedAtAsc(UUID warehouseInboundReceiptId);
}
