package com.cnh.ies.model.customer;

import lombok.Data;

@Data
public class CustomerAddressInfo {
    private String id;
    private String address;
    private String contactPerson;
    private String phone;
    private String email;
    private String customerId;
    private String createdAt;
    private String updatedAt;
    private String deletedAt;
    private String createdBy;
    private String updatedBy;
}
