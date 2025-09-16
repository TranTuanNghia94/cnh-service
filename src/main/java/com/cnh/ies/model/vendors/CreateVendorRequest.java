package com.cnh.ies.model.vendors;

import java.util.List;

import lombok.Data;

@Data
public class CreateVendorRequest {
    private String code;
    private String name;
    private String email;
    private String country;
    private String currency;
    private String phone;
    private String misaCode;
    private String address;
    private String taxCode;
    private String contactPerson;
    private List<CreateVendorBanksRequest> banks;
}
