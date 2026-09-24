package com.cnh.ies.model.report;

import java.math.BigDecimal;
import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentLedgerRow {
    private String company;
    private String vendorCode;
    private BigDecimal totalAmount;
    private Integer paymentCount;
    private String customerName;
    private String currency;
    private String paperType;
    private String paperNumber;
    private String note;
    private List<PaymentInstallment> installments;
}
