package com.cnh.ies.repository.payment;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.cnh.ies.entity.payment.PaymentRequestItemDocumentEntity;
import com.cnh.ies.repository.BaseRepo;

@Repository
public interface PaymentRequestItemDocumentRepo extends BaseRepo<PaymentRequestItemDocumentEntity, UUID> {

    @Query("SELECT d FROM PaymentRequestItemDocumentEntity d "
            + "JOIN FETCH d.paymentRequestItem i "
            + "LEFT JOIN FETCH i.purchaseOrderLine pol "
            + "LEFT JOIN FETCH pol.vendor "
            + "LEFT JOIN FETCH pol.purchaseOrder "
            + "LEFT JOIN FETCH pol.product "
            + "LEFT JOIN FETCH pol.saleOrderLine "
            + "WHERE i.paymentRequest.id = :paymentRequestId AND d.isDeleted = false AND i.isDeleted = false")
    List<PaymentRequestItemDocumentEntity> findByPaymentRequestId(UUID paymentRequestId);

    @Query("SELECT d FROM PaymentRequestItemDocumentEntity d "
            + "WHERE d.paymentRequestItem.id IN :paymentRequestItemIds AND d.isDeleted = false")
    List<PaymentRequestItemDocumentEntity> findByPaymentRequestItemIds(List<UUID> paymentRequestItemIds);
}
