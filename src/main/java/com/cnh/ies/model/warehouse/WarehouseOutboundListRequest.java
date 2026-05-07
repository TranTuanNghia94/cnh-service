package com.cnh.ies.model.warehouse;

import com.cnh.ies.model.general.ApiRequestModel;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class WarehouseOutboundListRequest extends ApiRequestModel {
    private String createdBy;
    private String outboundNumber;
    private String contractNumber;
    private String status;
}
