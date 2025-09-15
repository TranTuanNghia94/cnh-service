package com.cnh.ies.entity.vendors;

import com.cnh.ies.entity.BaseEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "vendor_banks")
@Data
@EqualsAndHashCode(callSuper = true)
public class VendorBanksEntity extends BaseEntity {
    
    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "bank_account_name", length = 200)
    private String bankAccountName;
    
    @Column(name = "bank_account_number", length = 100)
    private String bankAccountNumber;

    @Column(name = "bank_account_branch", length = 200)
    private String bankAccountBranch;

    @Column(name = "bank_account_swift", length = 100)
    private String bankAccountSwift;

    @Column(name = "bank_account_iban", length = 100)
    private String bankAccountIban;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id")
    private VendorsEntity vendor;
}
