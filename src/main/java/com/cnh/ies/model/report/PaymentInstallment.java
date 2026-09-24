package com.cnh.ies.model.report;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentInstallment {
    private int sequence;
    private String requestNumber;
    private BigDecimal amount;
    private String paidDate;
    private BigDecimal percentage;
    private String status;
}
