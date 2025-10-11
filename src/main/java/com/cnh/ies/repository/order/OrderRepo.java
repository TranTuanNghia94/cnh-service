package com.cnh.ies.repository.order;

import org.springframework.stereotype.Repository;

import java.util.UUID;

import com.cnh.ies.entity.order.OrderEntity;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.cnh.ies.repository.BaseRepo;

@Repository
public interface OrderRepo extends BaseRepo<OrderEntity, UUID> {
    @Query("SELECT o FROM OrderEntity o WHERE o.orderNumber = :orderNumber AND o.isDeleted = false")
    Optional<OrderEntity> findByOrderNumber(String orderNumber);

    @Query("SELECT o FROM OrderEntity o WHERE o.isDeleted = false")
    Page<OrderEntity> findAllAndIsDeletedFalse(Pageable pageable);

    @Query("SELECT o FROM OrderEntity o WHERE o.id = :id AND o.isDeleted = false")
    Optional<OrderEntity> findByIdAndIsDeletedFalse(UUID id);
    
    @Query(value = """
        SELECT CAST(SUBSTRING(o.order_number, 12) AS INTEGER) as sequence
        FROM orders o 
        WHERE o.order_number LIKE :yearMonthPattern 
        AND o.is_deleted = false 
        ORDER BY sequence DESC 
        LIMIT 1
        """, nativeQuery = true)
    Integer findMaxSequenceForYearMonth(String yearMonthPattern);
}
