package com.cnh.ies.repository.purchaseorder;

import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

import com.cnh.ies.entity.purchaseorder.PurchaseOrderEntity;
import com.cnh.ies.repository.BaseRepo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface PurchaseOrderRepo extends BaseRepo<PurchaseOrderEntity, UUID> {

    @EntityGraph(attributePaths = { "order", "order.customer" })
    @Query("SELECT po FROM PurchaseOrderEntity po WHERE po.isDeleted = false")
    Page<PurchaseOrderEntity> findAllAndIsDeletedFalse(Pageable pageable);

    @Query("SELECT po FROM PurchaseOrderEntity po "
            + "LEFT JOIN po.order o "
            + "LEFT JOIN o.customer c "
            + "WHERE po.isDeleted = false "
            + "AND (:purchaseOrderNumber = '' OR LOWER(CONCAT(COALESCE(po.poPrefix, ''), '.', CONCAT('', po.poNumber))) LIKE LOWER(CONCAT('%', :purchaseOrderNumber, '%'))) "
            + "AND (:contractNumber = '' OR LOWER(COALESCE(o.contractNumber, '')) LIKE LOWER(CONCAT('%', :contractNumber, '%'))) "
            + "AND (:createdBy = '' OR LOWER(COALESCE(po.createdBy, '')) LIKE LOWER(CONCAT('%', :createdBy, '%'))) "
            + "AND (:customerName = '' OR LOWER(COALESCE(c.name, '')) LIKE LOWER(CONCAT('%', :customerName, '%')))")
    Page<PurchaseOrderEntity> findAllFiltered(
            @Param("purchaseOrderNumber") String purchaseOrderNumber,
            @Param("contractNumber") String contractNumber,
            @Param("createdBy") String createdBy,
            @Param("customerName") String customerName,
            Pageable pageable);

    @Query("SELECT po FROM PurchaseOrderEntity po WHERE po.id = :id AND po.isDeleted = false")
    Optional<PurchaseOrderEntity> findByIdAndIsDeletedFalse(UUID id);

    @Query("SELECT MAX(po.poNumber) FROM PurchaseOrderEntity po WHERE po.poPrefix = :poPrefix AND po.isDeleted = false")
    Integer findMaxSequenceForPrefix(String poPrefix);

    @Query("SELECT po FROM PurchaseOrderEntity po WHERE po.poPrefix = :poPrefix AND po.poNumber = :poNumber AND po.isDeleted = false")
    Optional<PurchaseOrderEntity> findByPoPrefixAndPoNumber(String poPrefix, Integer poNumber);
}
