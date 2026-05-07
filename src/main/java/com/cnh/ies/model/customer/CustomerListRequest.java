package com.cnh.ies.model.customer;

import com.cnh.ies.model.general.ApiRequestModel;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CustomerListRequest extends ApiRequestModel {
    private String customerCode;
    private String misaCode;
    private String customerName;
}
