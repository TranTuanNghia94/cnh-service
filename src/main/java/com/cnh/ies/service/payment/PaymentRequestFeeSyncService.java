package com.cnh.ies.service.payment;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.cnh.ies.entity.payment.PaymentRequestEntity;
import com.cnh.ies.entity.payment.PaymentRequestExtraFeeEntity;
import com.cnh.ies.exception.ApiException;
import com.cnh.ies.model.payment.PaymentRequestFeeRequest;
import com.cnh.ies.repository.payment.PaymentRequestExtraFeeRepo;
import com.cnh.ies.util.RequestContext;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentRequestFeeSyncService {

    private final PaymentRequestExtraFeeRepo paymentRequestExtraFeeRepo;

    public void syncFees(PaymentRequestEntity paymentRequest, List<PaymentRequestFeeRequest> feeRequests,
            String requestId) {
        List<PaymentRequestExtraFeeEntity> existing = paymentRequestExtraFeeRepo.findByPaymentRequestId(paymentRequest.getId());
        String user = RequestContext.getCurrentUsername();
        if (feeRequests == null || feeRequests.isEmpty()) {
            softDeleteAllExtraFees(existing, user);
            return;
        }

        if (feeRequests.stream().anyMatch(f -> f.getId() != null && !f.getId().isBlank())) {
            Set<UUID> incomingIds = parseFeeRequestIds(feeRequests, requestId);
            for (PaymentRequestExtraFeeEntity fee : existing) {
                if (!incomingIds.contains(fee.getId())) {
                    fee.setIsDeleted(true);
                    fee.setUpdatedBy(user);
                }
            }
            if (!existing.isEmpty()) {
                paymentRequestExtraFeeRepo.saveAll(existing);
            }
            for (PaymentRequestFeeRequest fr : feeRequests) {
                if (fr.getId() != null && !fr.getId().isBlank()) {
                    UUID feeId = UUID.fromString(fr.getId().trim());
                    PaymentRequestExtraFeeEntity fee = paymentRequestExtraFeeRepo.findById(feeId)
                            .orElseThrow(() -> new ApiException(ApiException.ErrorCode.NOT_FOUND, "Extra fee not found: " + fr.getId(),
                                    HttpStatus.NOT_FOUND.value(), requestId));
                    if (!Objects.equals(fee.getPaymentRequest().getId(), paymentRequest.getId())) {
                        throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "Extra fee does not belong to this payment request",
                                HttpStatus.BAD_REQUEST.value(), requestId);
                    }
                    if (Boolean.TRUE.equals(fee.getIsDeleted())) {
                        throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "Extra fee is deleted: " + fr.getId(),
                                HttpStatus.BAD_REQUEST.value(), requestId);
                    }
                    applyFeeRequest(fee, fr);
                    fee.setUpdatedBy(user);
                    paymentRequestExtraFeeRepo.save(fee);
                } else {
                    insertFee(paymentRequest, fr, user, requestId);
                }
            }
            return;
        }

        Set<Integer> usedIncoming = new HashSet<>();
        for (PaymentRequestExtraFeeEntity fee : existing) {
            int j = -1;
            for (int i = 0; i < feeRequests.size(); i++) {
                if (usedIncoming.contains(i)) {
                    continue;
                }
                if (sameFeeKey(fee, feeRequests.get(i))) {
                    j = i;
                    break;
                }
            }
            if (j >= 0) {
                usedIncoming.add(j);
                applyFeeRequest(fee, feeRequests.get(j));
                fee.setUpdatedBy(user);
            } else {
                fee.setIsDeleted(true);
                fee.setUpdatedBy(user);
            }
        }
        if (!existing.isEmpty()) {
            paymentRequestExtraFeeRepo.saveAll(existing);
        }
        for (int i = 0; i < feeRequests.size(); i++) {
            if (!usedIncoming.contains(i)) {
                insertFee(paymentRequest, feeRequests.get(i), user, requestId);
            }
        }
    }

    private void softDeleteAllExtraFees(List<PaymentRequestExtraFeeEntity> existing, String user) {
        if (existing.isEmpty()) {
            return;
        }
        for (PaymentRequestExtraFeeEntity fee : existing) {
            fee.setIsDeleted(true);
            fee.setUpdatedBy(user);
        }
        paymentRequestExtraFeeRepo.saveAll(existing);
    }

    private static Set<UUID> parseFeeRequestIds(List<PaymentRequestFeeRequest> feeRequests, String requestId) {
        Set<UUID> incomingIds = new HashSet<>();
        for (PaymentRequestFeeRequest fr : feeRequests) {
            if (fr.getId() == null || fr.getId().isBlank()) {
                continue;
            }
            try {
                incomingIds.add(UUID.fromString(fr.getId().trim()));
            } catch (IllegalArgumentException e) {
                throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "Invalid fee id: " + fr.getId(),
                        HttpStatus.BAD_REQUEST.value(), requestId);
            }
        }
        return incomingIds;
    }

    private static boolean sameFeeKey(PaymentRequestExtraFeeEntity e, PaymentRequestFeeRequest r) {
        return r.getFeeName() != null && Objects.equals(e.getFeeName(), r.getFeeName())
                && Objects.equals(e.getFeeType(), r.getFeeType());
    }

    private void applyFeeRequest(PaymentRequestExtraFeeEntity fee, PaymentRequestFeeRequest fr) {
        fee.setFeeName(fr.getFeeName());
        fee.setFeeType(fr.getFeeType());
        fee.setAmount(fr.getAmount() == null ? BigDecimal.ZERO : fr.getAmount());
        fee.setNote(fr.getNote());
    }

    private void insertFee(PaymentRequestEntity paymentRequest, PaymentRequestFeeRequest fr, String user, String requestId) {
        PaymentRequestExtraFeeEntity fee = new PaymentRequestExtraFeeEntity();
        fee.setPaymentRequest(paymentRequest);
        if (fr.getFeeName() == null || fr.getFeeName().isBlank()) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "feeName is required for extra fee",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }
        applyFeeRequest(fee, fr);
        fee.setCreatedBy(user);
        fee.setUpdatedBy(user);
        paymentRequestExtraFeeRepo.save(fee);
    }
}
