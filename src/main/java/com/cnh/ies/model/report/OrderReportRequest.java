package com.cnh.ies.model.report;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OrderReportRequest extends ServiceReportPagedRequest {

    private String createdBy;
    private String contractNumber;
    private String orderNumber;
    private String status;
    private String customerName;
}
