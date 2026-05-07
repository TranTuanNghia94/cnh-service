package com.cnh.ies.repository.order;

import org.springframework.stereotype.Repository;

import java.util.UUID;

import com.cnh.ies.entity.order.OrderEntity;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import com.cnh.ies.repository.BaseRepo;

@Repository
public interface OrderRepo extends BaseRepo<OrderEntity, UUID> {
    @Query("SELECT o FROM OrderEntity o WHERE o.orderNumber = :orderNumber AND o.isDeleted = false")
    Optional<OrderEntity> findByOrderNumber(String orderNumber);

    @Query("SELECT o FROM OrderEntity o LEFT JOIN FETCH o.customer WHERE o.isDeleted = false")
    Page<OrderEntity> findAllAndIsDeletedFalse(Pageable pageable);

    @Query("SELECT o FROM OrderEntity o LEFT JOIN o.customer c WHERE o.isDeleted = false "
            + "AND (:createdBy = '' OR LOWER(COALESCE(o.createdBy, '')) LIKE LOWER(CONCAT('%', :createdBy, '%'))) "
            + "AND (:contractNumber = '' OR LOWER(COALESCE(o.contractNumber, '')) LIKE LOWER(CONCAT('%', :contractNumber, '%'))) "
            + "AND (:orderNumber = '' OR LOWER(CONCAT(COALESCE(o.orderPrefix, ''), '.', CONCAT('', o.orderNumber))) LIKE LOWER(CONCAT('%', :orderNumber, '%'))) "
            + "AND (:status = '' OR LOWER(COALESCE(o.status, '')) LIKE LOWER(CONCAT('%', :status, '%'))) "
            + "AND (:customerName = '' OR LOWER(COALESCE(c.name, '')) LIKE LOWER(CONCAT('%', :customerName, '%')))")
    Page<OrderEntity> findAllFiltered(
            @Param("createdBy") String createdBy,
            @Param("contractNumber") String contractNumber,
            @Param("orderNumber") String orderNumber,
            @Param("status") String status,
            @Param("customerName") String customerName,
            Pageable pageable);

    @Query("SELECT o FROM OrderEntity o WHERE o.id = :id AND o.isDeleted = false")
    Optional<OrderEntity> findByIdAndIsDeletedFalse(UUID id);
    
    @Query("SELECT o.orderNumber FROM OrderEntity o WHERE o.orderPrefix = :orderPrefix AND o.isDeleted = false")
    Integer findMaxSequenceForYearMonth(String orderPrefix);

    @Query("SELECT o FROM OrderEntity o WHERE o.orderPrefix = :orderPrefix AND o.orderNumber = :orderNumber AND o.isDeleted = false")
    Optional<OrderEntity> findByOrderPrefixAndOrderNumber(String orderPrefix, Integer orderNumber);

    @Query("SELECT o FROM OrderEntity o WHERE o.contractNumber = :contractNumber AND o.isDeleted = false")
    Optional<OrderEntity> findByContractNumber(String contractNumber);
}
