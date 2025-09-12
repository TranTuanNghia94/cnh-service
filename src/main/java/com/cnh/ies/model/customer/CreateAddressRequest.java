package com.cnh.ies.model.customer;

import java.util.Optional;

import lombok.Data;

@Data
public class CreateAddressRequest {
    private Optional<String> customerId;
    private String address;
    private String contactPerson;
    private String phone;
    private String email;
    private Optional<Boolean> isDeleted;
    private Optional<String> id;
}
