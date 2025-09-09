package com.cnh.ies.model.customer;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import lombok.Data;

@Data
public class CustomerInfo {
    private String id;
    private String code;
    private String name;
    private String email;
    private String phone;
    private String taxCode;
    private String misaCode;
    private Boolean isActive;
    private String createdAt;
    private String updatedAt;
    private String deletedAt;
    private String createdBy;
    private String updatedBy;
    private Set<CustomerAddressInfo> addresses = new CopyOnWriteArraySet<>();
}
