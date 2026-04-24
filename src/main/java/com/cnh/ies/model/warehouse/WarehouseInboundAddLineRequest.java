package com.cnh.ies.model.warehouse;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class WarehouseInboundAddLineRequest {
    private String paymentRequestPurchaseOrderLineId;
    private BigDecimal quantityReceived;
    private BigDecimal taxPercent;
    private String lineNote;
}
