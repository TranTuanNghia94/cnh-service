package com.cnh.ies.model.customer;

import java.util.List;

import lombok.Data;

@Data
public class CreateCustomerRequest {
    private String code;
    private String name;
    private String email;
    private String phone;
    private String taxCode;
    private String misaCode;
    private List<CreateAddressRequest> addresses;
}
