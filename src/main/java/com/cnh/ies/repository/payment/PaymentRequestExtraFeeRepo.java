package com.cnh.ies.repository.payment;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.cnh.ies.entity.payment.PaymentRequestExtraFeeEntity;
import com.cnh.ies.repository.BaseRepo;

@Repository
public interface PaymentRequestExtraFeeRepo extends BaseRepo<PaymentRequestExtraFeeEntity, UUID> {

    @Query("SELECT f FROM PaymentRequestExtraFeeEntity f WHERE f.paymentRequest.id = :paymentRequestId AND f.isDeleted = false")
    List<PaymentRequestExtraFeeEntity> findByPaymentRequestId(UUID paymentRequestId);
}
