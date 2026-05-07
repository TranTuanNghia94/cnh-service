package com.cnh.ies.model.warehouse;

import com.cnh.ies.model.general.ApiRequestModel;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class WarehouseInboundListRequest extends ApiRequestModel {
    private String createdBy;
    private String inboundNumber;
    private String contractNumber;
    private String customerName;
    private String orderNumber;
    private String status;
}
