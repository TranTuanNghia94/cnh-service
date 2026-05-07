package com.cnh.ies.model.purchaseorder;

import com.cnh.ies.model.general.ApiRequestModel;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PurchaseOrderListRequest extends ApiRequestModel {
    private String purchaseOrderNumber;
    private String contractNumber;
    private String createdBy;
    private String customerName;
}
