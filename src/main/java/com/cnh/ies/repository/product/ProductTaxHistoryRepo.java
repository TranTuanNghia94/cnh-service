package com.cnh.ies.repository.product;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cnh.ies.entity.product.ProductTaxHistoryEntity;
import com.cnh.ies.repository.BaseRepo;

@Repository
public interface ProductTaxHistoryRepo extends BaseRepo<ProductTaxHistoryEntity, UUID> {

    @Query("SELECT h FROM ProductTaxHistoryEntity h WHERE h.product.id = :productId AND h.isDeleted = false ORDER BY h.createdAt DESC")
    Page<ProductTaxHistoryEntity> findByProductIdAndIsDeletedFalseOrderByCreatedAtDesc(
            @Param("productId") UUID productId,
            Pageable pageable);
}
