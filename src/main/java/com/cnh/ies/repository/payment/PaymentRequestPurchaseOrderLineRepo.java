package com.cnh.ies.repository.payment;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.cnh.ies.entity.payment.PaymentRequestPurchaseOrderLineEntity;
import com.cnh.ies.repository.BaseRepo;

@Repository
public interface PaymentRequestPurchaseOrderLineRepo extends BaseRepo<PaymentRequestPurchaseOrderLineEntity, UUID> {

    @Query("SELECT i FROM PaymentRequestPurchaseOrderLineEntity i "
            + "LEFT JOIN FETCH i.purchaseOrderLine pol "
            + "LEFT JOIN FETCH pol.vendor "
            + "LEFT JOIN FETCH pol.purchaseOrder "
            + "LEFT JOIN FETCH pol.product "
            + "LEFT JOIN FETCH pol.saleOrderLine "
            + "WHERE i.paymentRequest.id = :paymentRequestId AND i.isDeleted = false")
    List<PaymentRequestPurchaseOrderLineEntity> findByPaymentRequestId(UUID paymentRequestId);
}
