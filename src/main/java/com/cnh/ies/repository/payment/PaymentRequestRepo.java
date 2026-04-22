package com.cnh.ies.repository.payment;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.cnh.ies.entity.payment.PaymentRequestEntity;
import com.cnh.ies.repository.BaseRepo;

@Repository
public interface PaymentRequestRepo extends BaseRepo<PaymentRequestEntity, UUID> {

    @Query("SELECT pr FROM PaymentRequestEntity pr WHERE pr.isDeleted = false")
    Page<PaymentRequestEntity> findAllAndIsDeletedFalse(Pageable pageable);

    @Query("SELECT pr FROM PaymentRequestEntity pr WHERE pr.id = :id AND pr.isDeleted = false")
    Optional<PaymentRequestEntity> findByIdAndIsDeletedFalse(UUID id);

    @Query("SELECT pr.requestNumber FROM PaymentRequestEntity pr "
            + "WHERE pr.requestNumber LIKE CONCAT(:prefix, '.%') AND pr.isDeleted = false")
    List<String> findRequestNumbersByPrefix(String prefix);
}
