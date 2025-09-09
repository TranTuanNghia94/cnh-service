package com.cnh.ies.entity.customer;


import com.cnh.ies.entity.BaseEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Column;
import jakarta.persistence.Table;


import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "customer_addresses")
@Data
@EqualsAndHashCode(callSuper = true)
public class CustomerAddressEntity extends BaseEntity {

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "contact_person", length = 100)
    private String contactPerson;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", length = 100)
    private String email;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private CustomerEntity customer;
}
