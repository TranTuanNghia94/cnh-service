package com.cnh.ies.repository.warehouse;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cnh.ies.entity.warehouse.WarehouseStockTransactionEntity;
import com.cnh.ies.repository.BaseRepo;

@Repository
public interface WarehouseStockTransactionRepo extends BaseRepo<WarehouseStockTransactionEntity, UUID> {

    @Query("SELECT t FROM WarehouseStockTransactionEntity t JOIN FETCH t.product "
            + "WHERE t.product.id = :productId AND t.isDeleted = false ORDER BY t.createdAt DESC")
    List<WarehouseStockTransactionEntity> findByProductIdOrderByCreatedAtDesc(@Param("productId") UUID productId);
}
