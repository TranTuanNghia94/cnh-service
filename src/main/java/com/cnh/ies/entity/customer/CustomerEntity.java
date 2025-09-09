package com.cnh.ies.entity.customer;

import com.cnh.ies.entity.BaseEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;

import java.util.Set;
import java.util.HashSet;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "customers")
@Data
@EqualsAndHashCode(callSuper = true)
public class CustomerEntity extends BaseEntity {
    
    @Column(name = "code", unique = true, nullable = false, length = 100)
    private String code;

    @Column(name = "name", nullable = false, length = 300)
    private String name;
    
    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "tax_code", length = 50)
    private String taxCode;

    @Column(name = "misa_code", length = 50)
    private String misaCode;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @OneToMany(mappedBy = "customer", fetch = FetchType.LAZY)
    private Set<CustomerAddressEntity> addresses = new HashSet<>();
}
