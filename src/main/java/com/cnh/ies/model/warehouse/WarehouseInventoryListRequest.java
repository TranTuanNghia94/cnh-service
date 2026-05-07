package com.cnh.ies.model.warehouse;

import com.cnh.ies.model.general.ApiRequestModel;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class WarehouseInventoryListRequest extends ApiRequestModel {
    private String productCode;
    private String productName;
    private String productCategory;
}
