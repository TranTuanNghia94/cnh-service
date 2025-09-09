package com.cnh.ies.repository.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

import com.cnh.ies.entity.product.ProductEntity;
import com.cnh.ies.repository.BaseRepo;

@Repository
public interface ProductRepo extends BaseRepo<ProductEntity, UUID> {
    Optional<ProductEntity> findByCode(String code);

    // add pagination in the query
    @Query("SELECT p FROM ProductEntity p WHERE p.isDeleted = false ORDER BY p.createdAt DESC")
    Page<ProductEntity> findAllAndIsDeletedFalse(Pageable pageable);
}
