package com.cnh.ies.model.vendors;

import java.util.List;

import lombok.Data;

@Data
public class UpdateVendorRequest {
    private String id;
    private String name;
    private String email;
    private String country;
    private String currency;
    private String phone;
    private String code;
    private String taxCode;
    private String misaCode;
    private String address;
    private String contactPerson;
    private List<CreateVendorBanksRequest> banks;
}
