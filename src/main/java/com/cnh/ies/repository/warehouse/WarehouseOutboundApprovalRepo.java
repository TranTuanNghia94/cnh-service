package com.cnh.ies.repository.warehouse;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cnh.ies.entity.warehouse.WarehouseOutboundApprovalEntity;
import com.cnh.ies.repository.BaseRepo;

@Repository
public interface WarehouseOutboundApprovalRepo extends BaseRepo<WarehouseOutboundApprovalEntity, UUID> {

    @Query("SELECT a FROM WarehouseOutboundApprovalEntity a LEFT JOIN FETCH a.approver "
            + "WHERE a.outbound.id = :outboundId AND a.isDeleted = false ORDER BY a.approvalLevel ASC")
    List<WarehouseOutboundApprovalEntity> findByOutboundId(@Param("outboundId") UUID outboundId);

    @Query("SELECT a FROM WarehouseOutboundApprovalEntity a WHERE a.outbound.id = :outboundId "
            + "AND a.approvalLevel = :level AND a.isDeleted = false")
    Optional<WarehouseOutboundApprovalEntity> findByOutboundIdAndLevel(
            @Param("outboundId") UUID outboundId, @Param("level") Integer level);
}
