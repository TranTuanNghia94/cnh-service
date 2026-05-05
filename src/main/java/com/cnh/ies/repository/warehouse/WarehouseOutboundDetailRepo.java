package com.cnh.ies.repository.warehouse;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cnh.ies.entity.warehouse.WarehouseOutboundDetailEntity;
import com.cnh.ies.repository.BaseRepo;

@Repository
public interface WarehouseOutboundDetailRepo extends BaseRepo<WarehouseOutboundDetailEntity, UUID> {

    @Query("SELECT d FROM WarehouseOutboundDetailEntity d "
            + "LEFT JOIN FETCH d.product p "
            + "LEFT JOIN FETCH d.orderLine ol "
            + "WHERE d.outbound.id = :outboundId AND d.isDeleted = false AND p.isDeleted = false AND ol.isDeleted = false")
    List<WarehouseOutboundDetailEntity> findByOutboundId(@Param("outboundId") UUID outboundId);
}
