package com.cnh.ies.model.warehouse;

import lombok.Data;

@Data
public class WarehouseInboundPurchaseOrderInfo {
    private String purchaseOrderId;
    private String purchaseOrderNumber;
    private String vendorId;
    private String vendorName;
    private String orderId;
    private String orderNumber;
    private String orderContractNumber;
}
