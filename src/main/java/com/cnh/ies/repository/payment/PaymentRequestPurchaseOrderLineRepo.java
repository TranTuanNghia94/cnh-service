package com.cnh.ies.repository.payment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cnh.ies.entity.payment.PaymentRequestPurchaseOrderLineEntity;
import com.cnh.ies.repository.BaseRepo;

@Repository
public interface PaymentRequestPurchaseOrderLineRepo extends BaseRepo<PaymentRequestPurchaseOrderLineEntity, UUID> {

    @Query("SELECT DISTINCT i FROM PaymentRequestPurchaseOrderLineEntity i "
            + "LEFT JOIN FETCH i.purchaseOrderLine pol "
            + "LEFT JOIN FETCH pol.vendor "
            + "LEFT JOIN FETCH pol.purchaseOrder po "
            + "LEFT JOIN FETCH po.order poOrder "
            + "LEFT JOIN FETCH poOrder.customer "
            + "LEFT JOIN FETCH poOrder.customerAddress "
            + "LEFT JOIN FETCH pol.product "
            + "LEFT JOIN FETCH pol.saleOrderLine "
            + "WHERE i.paymentRequest.id = :paymentRequestId AND i.isDeleted = false")
    List<PaymentRequestPurchaseOrderLineEntity> findByPaymentRequestId(UUID paymentRequestId);

    /**
     * Find all payment request lines for the given purchase order line IDs.
     * Includes payment request details for payment history tracking.
     * Only returns non-deleted records with non-cancelled/rejected payment requests.
     */
    @Query("SELECT prpol FROM PaymentRequestPurchaseOrderLineEntity prpol "
            + "LEFT JOIN FETCH prpol.paymentRequest pr "
            + "LEFT JOIN FETCH pr.paidBy "
            + "WHERE prpol.purchaseOrderLine.id IN :purchaseOrderLineIds "
            + "AND prpol.isDeleted = false "
            + "AND pr.isDeleted = false "
            + "AND pr.status NOT IN ('CANCELLED', 'REJECTED') "
            + "ORDER BY pr.requestDate DESC")
    List<PaymentRequestPurchaseOrderLineEntity> findPaymentHistoryByPurchaseOrderLineIds(
            @Param("purchaseOrderLineIds") List<UUID> purchaseOrderLineIds);

    /**
     * Find all payment request lines for a single purchase order line.
     */
    @Query("SELECT prpol FROM PaymentRequestPurchaseOrderLineEntity prpol "
            + "LEFT JOIN FETCH prpol.paymentRequest pr "
            + "LEFT JOIN FETCH pr.paidBy "
            + "WHERE prpol.purchaseOrderLine.id = :purchaseOrderLineId "
            + "AND prpol.isDeleted = false "
            + "AND pr.isDeleted = false "
            + "AND pr.status NOT IN ('CANCELLED', 'REJECTED') "
            + "ORDER BY pr.requestDate DESC")
    List<PaymentRequestPurchaseOrderLineEntity> findPaymentHistoryByPurchaseOrderLineId(
            @Param("purchaseOrderLineId") UUID purchaseOrderLineId);

    @Query("SELECT DISTINCT prpol.paymentRequest.id FROM PaymentRequestPurchaseOrderLineEntity prpol "
            + "WHERE prpol.isDeleted = false AND prpol.paymentRequest.isDeleted = false "
            + "AND prpol.purchaseOrderLine.id IN :purchaseOrderLineIds")
    List<UUID> findPaymentRequestIdsByPurchaseOrderLineIds(
            @Param("purchaseOrderLineIds") List<UUID> purchaseOrderLineIds);

    @Query("SELECT DISTINCT prpol.paymentRequest.id FROM PaymentRequestPurchaseOrderLineEntity prpol "
            + "JOIN prpol.purchaseOrderLine pol "
            + "WHERE prpol.isDeleted = false AND prpol.paymentRequest.isDeleted = false AND ("
            + "LOWER(COALESCE(pol.quote, '')) LIKE LOWER(CONCAT('%', :code, '%')) OR "
            + "LOWER(COALESCE(pol.invoice, '')) LIKE LOWER(CONCAT('%', :code, '%')) OR "
            + "LOWER(COALESCE(pol.trackId, '')) LIKE LOWER(CONCAT('%', :code, '%')) OR "
            + "LOWER(COALESCE(pol.receiptWarehouse, '')) LIKE LOWER(CONCAT('%', :code, '%')) OR "
            + "LOWER(COALESCE(pol.billOfLadding, '')) LIKE LOWER(CONCAT('%', :code, '%'))"
            + ")")
    List<UUID> findPaymentRequestIdsByAnyLinkedPaperCode(@Param("code") String code);

    @Query("SELECT DISTINCT prpol.paymentRequest.id FROM PaymentRequestPurchaseOrderLineEntity prpol "
            + "WHERE prpol.isDeleted = false AND prpol.paymentRequest.isDeleted = false "
            + "AND LOWER(COALESCE(prpol.note, '')) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<UUID> findPaymentRequestIdsByPaymentRequestLineNoteContaining(@Param("q") String q);

    @Query("SELECT prpol FROM PaymentRequestPurchaseOrderLineEntity prpol "
            + "JOIN FETCH prpol.purchaseOrderLine pol LEFT JOIN FETCH pol.product "
            + "WHERE prpol.id = :id AND prpol.paymentRequest.id = :paymentRequestId AND prpol.isDeleted = false")
    Optional<PaymentRequestPurchaseOrderLineEntity> findByIdAndPaymentRequestId(
            @Param("id") UUID id, @Param("paymentRequestId") UUID paymentRequestId);
}
