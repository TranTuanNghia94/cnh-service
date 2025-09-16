package com.cnh.ies.model.vendors;

import java.util.Optional;

import lombok.Data;

@Data
public class CreateVendorBanksRequest {
    private String bankName;
    private String bankAccountName;
    private String bankAccountNumber;
    private String bankAccountBranch;
    private String bankAccountSwift;
    private String bankAccountIban;
    private Optional<String> vendorId;
}
