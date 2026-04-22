package com.cnh.ies.model.payment;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class PaymentRequestLineInfo {
    private String id;
    private String purchaseOrderLineId;
    private String selectedDocuments;
    private BigDecimal requestedAmount;
    private BigDecimal paidAmount;
    private String note;
}
