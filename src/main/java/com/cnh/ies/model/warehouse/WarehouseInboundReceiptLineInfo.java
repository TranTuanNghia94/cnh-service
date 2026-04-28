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
    private Boolean taxIncluded;
    private String billOnPaper;
    private String lineNote;

    private String purchaseOrderId;
    private String purchaseOrderNumber;
    private String purchaseOrderLineId;
    private String vendorId;
    private String vendorName;
    private BigDecimal unitPrice;
    private String currency;

    private String orderId;
    private String orderNumber;
    private String orderContractNumber;
    private String orderLineId;
}
