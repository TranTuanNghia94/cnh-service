package com.cnh.ies.model.payment;

import com.cnh.ies.model.general.ApiRequestModel;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PaymentRequestListRequest extends ApiRequestModel {
    private String createdBy;
    private String paymentRequestNumber;
    private String vendorCode;
    private String numberOfPaper;
    private String status;
}
