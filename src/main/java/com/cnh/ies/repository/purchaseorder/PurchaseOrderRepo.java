package com.cnh.ies.repository.purchaseorder;

import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

import com.cnh.ies.entity.purchaseorder.PurchaseOrderEntity;
import com.cnh.ies.repository.BaseRepo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

@Repository
public interface PurchaseOrderRepo extends BaseRepo<PurchaseOrderEntity, UUID> {

    @Query("SELECT po FROM PurchaseOrderEntity po WHERE po.isDeleted = false")
    Page<PurchaseOrderEntity> findAllAndIsDeletedFalse(Pageable pageable);

    @Query("SELECT po FROM PurchaseOrderEntity po WHERE po.id = :id AND po.isDeleted = false")
    Optional<PurchaseOrderEntity> findByIdAndIsDeletedFalse(UUID id);

    @Query("SELECT MAX(po.poNumber) FROM PurchaseOrderEntity po WHERE po.poPrefix = :poPrefix AND po.isDeleted = false")
    Integer findMaxSequenceForPrefix(String poPrefix);

    @Query("SELECT po FROM PurchaseOrderEntity po WHERE po.poPrefix = :poPrefix AND po.poNumber = :poNumber AND po.isDeleted = false")
    Optional<PurchaseOrderEntity> findByPoPrefixAndPoNumber(String poPrefix, Integer poNumber);
}
