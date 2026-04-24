package com.cnh.ies.model.warehouse;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class WarehouseInboundReceiptLineInfo {
    private String id;
    private String productId;
    private String productName;
    private String paymentRequestPurchaseOrderLineId;
    private BigDecimal quantityExpected;
    private BigDecimal quantityReceived;
    private BigDecimal taxPercent;
    private String lineNote;
}
