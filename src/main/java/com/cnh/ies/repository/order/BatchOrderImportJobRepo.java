package com.cnh.ies.repository.order;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cnh.ies.entity.order.BatchOrderImportJobEntity;
import com.cnh.ies.repository.BaseRepo;

@Repository
public interface BatchOrderImportJobRepo extends BaseRepo<BatchOrderImportJobEntity, UUID> {

    @Query("SELECT j FROM BatchOrderImportJobEntity j WHERE j.id = :id AND j.isDeleted = false")
    Optional<BatchOrderImportJobEntity> findByIdAndIsDeletedFalse(@Param("id") UUID id);

    @Query("SELECT j FROM BatchOrderImportJobEntity j WHERE j.ownerUserId = :ownerUserId AND j.isDeleted = false ORDER BY j.createdAt DESC")
    Page<BatchOrderImportJobEntity> findByOwnerUserIdAndIsDeletedFalseOrderByCreatedAtDesc(
            @Param("ownerUserId") UUID ownerUserId,
            Pageable pageable);
}
