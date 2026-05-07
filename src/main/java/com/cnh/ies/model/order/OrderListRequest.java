package com.cnh.ies.model.order;

import com.cnh.ies.model.general.ApiRequestModel;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class OrderListRequest extends ApiRequestModel {
    private String createdBy;
    private String contractNumber;
    private String orderNumber;
    private String status;
    private String customerName;
}
