package com.cnh.ies.repository.warehouse;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cnh.ies.entity.warehouse.WarehouseOutboundEntity;
import com.cnh.ies.repository.BaseRepo;

@Repository
public interface WarehouseOutboundRepo extends BaseRepo<WarehouseOutboundEntity, UUID> {

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o.outboundNumber FROM WarehouseOutboundEntity o "
            + "WHERE o.outboundNumber LIKE CONCAT(:prefix, '.%') AND o.isDeleted = false")
    List<String> findOutboundNumbersByPrefixForUpdate(@Param("prefix") String prefix);

    @Query("SELECT o FROM WarehouseOutboundEntity o LEFT JOIN FETCH o.order "
            + "WHERE o.id = :id AND o.isDeleted = false")
    Optional<WarehouseOutboundEntity> findByIdAndIsDeletedFalse(@Param("id") UUID id);

    @Query("SELECT o FROM WarehouseOutboundEntity o "
            + "WHERE o.isDeleted = false "
            + "AND (:status = '' OR o.status = :status) "
            + "AND (:search = '' OR o.outboundNumber LIKE CONCAT('%', :search, '%') "
            + "    OR o.contractNumber LIKE CONCAT('%', :search, '%') "
            + "    OR o.outboundReason LIKE CONCAT('%', :search, '%')) "
            + "ORDER BY o.createdAt DESC")
    Page<WarehouseOutboundEntity> findAllFiltered(
            @Param("status") String status,
            @Param("search") String search,
            Pageable pageable);
}
