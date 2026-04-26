package com.cnh.ies.model.warehouse;

import lombok.Data;

@Data
public class WarehouseInboundOrderInfo {
    private String orderId;
    private String orderNumber;
    private String contractNumber;
    private String customerName;
    private String status;
}
