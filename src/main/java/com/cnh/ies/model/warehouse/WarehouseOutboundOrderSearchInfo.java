package com.cnh.ies.model.warehouse;

import java.util.List;

import lombok.Data;

@Data
public class WarehouseOutboundOrderSearchInfo {
    private String orderId;
    private String orderNumber;
    private String contractNumber;
    private String orderStatus;
    private String customerId;
    private String customerCode;
    private String customerName;
    private String customerPhone;
    private String customerTaxCode;
    private String customerAddressId;
    private String customerAddress;
    private String customerContactPerson;
    private String customerAddressPhone;
    private List<WarehouseOutboundOrderLineInfo> orderLines;
}
