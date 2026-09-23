package com.cnh.ies.model.report;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PaymentRequestReportRequest extends ServiceReportPagedRequest {

    private String createdBy;
    private String paymentRequestNumber;
    private String vendorCode;
    private String status;
}
