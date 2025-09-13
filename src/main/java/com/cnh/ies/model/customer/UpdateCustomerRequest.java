package com.cnh.ies.model.customer;

import java.util.List;

import lombok.Data;

@Data
public class UpdateCustomerRequest {
    private String id;
    private String name;
    private String email;
    private String phone;
    private String code;
    private String taxCode;
    private String misaCode;
    private List<CreateAddressRequest> addresses;
}
