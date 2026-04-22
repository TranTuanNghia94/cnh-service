package com.cnh.ies.model.payment;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;

@Data
public class PaymentRequestItemRequest {
    private String purchaseOrderLineId;
    private List<String> selectedDocumentTypes;
    private BigDecimal requestedAmount;
    private String note;
}
