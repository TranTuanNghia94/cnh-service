package com.cnh.ies.model.payment;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class PaymentRequestFeeInfo {
    private String id;
    private String feeName;
    private String feeType;
    private BigDecimal amount;
    private String note;
}
