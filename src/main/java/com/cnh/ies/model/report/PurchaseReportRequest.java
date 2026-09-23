package com.cnh.ies.model.report;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PurchaseReportRequest extends ServiceReportPagedRequest {

    private String purchaseOrderNumber;
    private String contractNumber;
    private String createdBy;
    private String customerName;
    private String status;
}
