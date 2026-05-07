package com.cnh.ies.repository.warehouse;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cnh.ies.entity.warehouse.WarehouseInventoryEntity;
import com.cnh.ies.repository.BaseRepo;

@Repository
public interface WarehouseInventoryRepo extends BaseRepo<WarehouseInventoryEntity, UUID> {

    @Query("SELECT i FROM WarehouseInventoryEntity i WHERE i.product.id = :productId AND i.isDeleted = false")
    Optional<WarehouseInventoryEntity> findByProductId(@Param("productId") UUID productId);

    @Query("SELECT i FROM WarehouseInventoryEntity i "
            + "JOIN FETCH i.product p "
            + "LEFT JOIN p.category c "
            + "WHERE i.isDeleted = false AND p.isDeleted = false "
            + "AND (:productCode = '' OR LOWER(p.code) LIKE LOWER(CONCAT('%', :productCode, '%'))) "
            + "AND (:productName = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :productName, '%'))) "
            + "AND (:productCategory = '' OR LOWER(COALESCE(c.name, '')) LIKE LOWER(CONCAT('%', :productCategory, '%')))")
    Page<WarehouseInventoryEntity> findAllFiltered(
            @Param("productCode") String productCode,
            @Param("productName") String productName,
            @Param("productCategory") String productCategory,
            Pageable pageable);
}
