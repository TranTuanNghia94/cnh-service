package com.cnh.ies.model.vendors;

import lombok.Data;
import java.util.List;
import java.time.Instant;

@Data
public class VendorInfo {
    private String id;
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
    private List<VendorBanksInfo> banks;
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;
    private Boolean isDeleted;
}
