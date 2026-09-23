package com.cnh.ies.model.report;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class WarehouseInboundReportRequest extends ServiceReportPagedRequest {

    private String createdBy;
    private String inboundNumber;
    private String contractNumber;
    private String customerName;
    private String orderNumber;
    private String status;
}
