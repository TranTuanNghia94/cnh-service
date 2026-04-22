package com.cnh.ies.repository.payment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.cnh.ies.entity.payment.PaymentRequestApprovalEntity;
import com.cnh.ies.repository.BaseRepo;

@Repository
public interface PaymentRequestApprovalRepo extends BaseRepo<PaymentRequestApprovalEntity, UUID> {

    @Query("SELECT a FROM PaymentRequestApprovalEntity a LEFT JOIN FETCH a.approver "
            + "WHERE a.paymentRequest.id = :paymentRequestId AND a.isDeleted = false ORDER BY a.approvalLevel ASC")
    List<PaymentRequestApprovalEntity> findByPaymentRequestId(UUID paymentRequestId);

    @Query("SELECT a FROM PaymentRequestApprovalEntity a WHERE a.paymentRequest.id = :paymentRequestId "
            + "AND a.approvalLevel = :level AND a.isDeleted = false")
    Optional<PaymentRequestApprovalEntity> findByPaymentRequestIdAndLevel(UUID paymentRequestId, Integer level);
}
