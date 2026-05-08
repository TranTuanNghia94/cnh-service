package com.cnh.ies.repository.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

import com.cnh.ies.entity.product.ProductEntity;
import com.cnh.ies.repository.BaseRepo;

@Repository
public interface ProductRepo extends BaseRepo<ProductEntity, UUID> {
    Optional<ProductEntity> findByCode(String code);
    Optional<ProductEntity> findByCodeIgnoreCase(String code);

    // add pagination in the query
    @Query("SELECT p FROM ProductEntity p WHERE p.isDeleted = false ORDER BY p.createdAt DESC")
    Page<ProductEntity> findAllAndIsDeletedFalse(Pageable pageable);

    @Query("SELECT p FROM ProductEntity p LEFT JOIN p.category c "
            + "WHERE p.isDeleted = false "
            + "AND (:productCode = '' OR LOWER(p.code) LIKE LOWER(CONCAT('%', :productCode, '%'))) "
            + "AND (:productName = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :productName, '%'))) "
            + "AND (:productCategory = '' OR LOWER(COALESCE(c.name, '')) LIKE LOWER(CONCAT('%', :productCategory, '%')))")
    Page<ProductEntity> findAllFiltered(
            @Param("productCode") String productCode,
            @Param("productName") String productName,
            @Param("productCategory") String productCategory,
            Pageable pageable);

    @Query("SELECT p FROM ProductEntity p WHERE p.id IN :ids AND p.isDeleted = false")
    List<ProductEntity> findByIdIn(List<UUID> ids);
}
