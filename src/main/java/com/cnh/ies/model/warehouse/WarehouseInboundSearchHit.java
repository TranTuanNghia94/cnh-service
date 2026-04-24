package com.cnh.ies.model.warehouse;

import lombok.Data;

@Data
public class WarehouseInboundSearchHit {
    private String paymentRequestId;
    private String requestNumber;
    private String status;
    private String notes;
    private String vendorCode;
    private String vendorName;
}
