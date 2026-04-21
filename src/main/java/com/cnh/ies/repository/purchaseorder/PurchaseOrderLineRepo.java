package com.cnh.ies.repository.purchaseorder;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

import com.cnh.ies.entity.purchaseorder.PurchaseOrderLineEntity;

@Repository
public interface PurchaseOrderLineRepo extends JpaRepository<PurchaseOrderLineEntity, UUID> {

    @Query("SELECT pol FROM PurchaseOrderLineEntity pol WHERE pol.purchaseOrder.id = :purchaseOrderId AND pol.isDeleted = false")
    List<PurchaseOrderLineEntity> findByPurchaseOrderId(UUID purchaseOrderId);

    @Query("SELECT pol FROM PurchaseOrderLineEntity pol LEFT JOIN FETCH pol.product LEFT JOIN FETCH pol.vendor WHERE pol.purchaseOrder.id = :purchaseOrderId AND pol.isDeleted = false")
    List<PurchaseOrderLineEntity> findAllByPurchaseOrderId(UUID purchaseOrderId);

    @Query("SELECT pol FROM PurchaseOrderLineEntity pol LEFT JOIN FETCH pol.saleOrderLine WHERE pol.purchaseOrder.id IN :purchaseOrderIds AND pol.isDeleted = false")
    List<PurchaseOrderLineEntity> findAllByPurchaseOrderIds(List<UUID> purchaseOrderIds);

    @Query("SELECT pol FROM PurchaseOrderLineEntity pol WHERE pol.isDeleted = false")
    Page<PurchaseOrderLineEntity> findAllAndIsDeletedFalse(Pageable pageable);
}
