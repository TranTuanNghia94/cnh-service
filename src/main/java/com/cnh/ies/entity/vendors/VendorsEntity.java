package com.cnh.ies.entity.vendors;

import java.util.HashSet;
import java.util.Set;

import com.cnh.ies.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "vendors")
public class VendorsEntity extends BaseEntity {

    @Column(name = "code", unique = true, nullable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "currency", length = 100)
    private String currency;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "misa_code", length = 100)
    private String misaCode;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "tax_code", length = 50)
    private String taxCode;

    @Column(name = "contact_person", length = 100)
    private String contactPerson;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @OneToMany(mappedBy = "vendor", fetch = FetchType.LAZY)
    private Set<VendorBanksEntity> banks = new HashSet<>();
    
    
}
