package com.cnh.ies.model.vendors;

import java.time.Instant;

import lombok.Data;

@Data
public class VendorBanksInfo {
    private String id;
    private String bankName;
    private String bankAccountName;
    private String bankAccountNumber;
    private String bankAccountBranch;
    private String bankAccountSwift;
    private String bankAccountIban;
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;
    private Boolean isDeleted;
}
