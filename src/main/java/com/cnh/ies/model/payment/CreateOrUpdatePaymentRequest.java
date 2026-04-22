package com.cnh.ies.model.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import lombok.Data;

@Data
public class CreateOrUpdatePaymentRequest {
    private String id;
    private String requestorId;
    private String currency;
    private Instant requestDate;
    private BigDecimal paidPercentage;
    private BigDecimal exchangeRate;
    private String purpose;
    private String notes;
    private List<PaymentFileObject> papers;
    private PaymentBankInfoObject bankInfo;
    private Integer approvalLevels;
    private List<PaymentRequestItemRequest> items;
    private List<PaymentRequestFeeRequest> fees;
}
