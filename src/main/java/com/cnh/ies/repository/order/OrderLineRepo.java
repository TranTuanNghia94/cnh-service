package com.cnh.ies.repository.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

import com.cnh.ies.entity.order.OrderLineEntity;

@Repository
public interface OrderLineRepo extends JpaRepository<OrderLineEntity, UUID> {
    @Query("SELECT o FROM OrderLineEntity o WHERE o.order.id = :orderId AND o.isDeleted = false")
    List<OrderLineEntity> findByOrderId(UUID orderId);

    @Query("SELECT o FROM OrderLineEntity o WHERE o.isDeleted = false")
    Page<OrderLineEntity> findAllAndIsDeletedFalse(Pageable pageable);
}
