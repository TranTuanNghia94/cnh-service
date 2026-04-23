package com.cnh.ies.model.payment;

import java.math.BigDecimal;

import com.cnh.ies.model.purchaseorder.PurchaseOrderLineInfo;

import lombok.Data;

@Data
public class PaymentRequestLineInfo {
    private String id;
    private String purchaseOrderId;
    private String purchaseOrderLineId;
    private PurchaseOrderLineInfo purchaseOrderLine;
    private String selectedDocuments;
    private BigDecimal requestedAmount;
    private BigDecimal paidAmount;
    private String note;
}
