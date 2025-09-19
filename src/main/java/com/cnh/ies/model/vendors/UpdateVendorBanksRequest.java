package com.cnh.ies.model.vendors;

import java.util.Optional;

import lombok.Data;

@Data
public class UpdateVendorBanksRequest {
    private String id;
    private String bankName;
    private String bankAccountName;
    private String bankAccountNumber;
    private Optional<String> bankAccountBranch;
    private Optional<String> bankAccountSwift;
    private Optional<String> bankAccountIban;
    private Optional<Boolean> isDeleted;
    private Optional<String> vendorId;
}
