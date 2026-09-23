package com.cnh.ies.model.report;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class WarehouseOutboundReportRequest extends ServiceReportPagedRequest {

    private String createdBy;
    private String outboundNumber;
    private String contractNumber;
    private String status;
}
