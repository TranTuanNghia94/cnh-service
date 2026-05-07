package com.cnh.ies.model.vendors;

import com.cnh.ies.model.general.ApiRequestModel;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class VendorListRequest extends ApiRequestModel {
    private String vendorCode;
    private String vendorName;
    private String misaCode;
    private String currency;
    private String nation;
}
