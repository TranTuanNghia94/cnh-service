package com.cnh.ies.repository.export;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cnh.ies.entity.export.ExportJobEntity;
import com.cnh.ies.repository.BaseRepo;

@Repository
public interface ExportJobRepo extends BaseRepo<ExportJobEntity, UUID> {

    @Query("SELECT j FROM ExportJobEntity j WHERE j.id = :id AND j.isDeleted = false")
    Optional<ExportJobEntity> findByIdAndIsDeletedFalse(@Param("id") UUID id);

    @Query("SELECT j FROM ExportJobEntity j WHERE j.ownerUserId = :ownerUserId AND j.isDeleted = false ORDER BY j.createdAt DESC")
    Page<ExportJobEntity> findByOwnerUserIdAndIsDeletedFalseOrderByCreatedAtDesc(
            @Param("ownerUserId") UUID ownerUserId,
            Pageable pageable);
}
