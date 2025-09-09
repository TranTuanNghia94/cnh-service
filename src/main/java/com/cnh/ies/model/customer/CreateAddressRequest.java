package com.cnh.ies.model.customer;

import lombok.Data;

@Data
public class CreateAddressRequest {
    private String address;
    private String contactPerson;
    private String phone;
    private String email;
}
