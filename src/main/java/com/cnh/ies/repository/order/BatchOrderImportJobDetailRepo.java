package com.cnh.ies.repository.order;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cnh.ies.entity.order.BatchOrderImportJobDetailEntity;
import com.cnh.ies.repository.BaseRepo;

@Repository
public interface BatchOrderImportJobDetailRepo extends BaseRepo<BatchOrderImportJobDetailEntity, UUID> {

    @Query("SELECT d FROM BatchOrderImportJobDetailEntity d WHERE d.jobId = :jobId AND d.isDeleted = false ORDER BY d.createdAt ASC")
    Page<BatchOrderImportJobDetailEntity> findByJobIdAndIsDeletedFalseOrderByCreatedAtAsc(
            @Param("jobId") UUID jobId,
            Pageable pageable);
}
