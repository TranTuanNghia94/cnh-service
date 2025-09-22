package com.cnh.ies.entity.order;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import com.cnh.ies.entity.BaseEntity;
import com.cnh.ies.entity.customer.CustomerAddressEntity;
import com.cnh.ies.entity.customer.CustomerEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "orders")
@Data
@EqualsAndHashCode(callSuper = true)
public class OrderEntity extends BaseEntity {

    @Column(name = "version", nullable = false)
    private Long version;
    
    @Column(name = "order_number", unique = true, nullable = false, length = 100)
    private String orderNumber;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private CustomerEntity customer;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_address_id")
    private CustomerAddressEntity customerAddress;
    
    @Column(name = "contract_number", nullable = false, length = 200)
    private String contractNumber;
    
    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;
    
    @Column(name = "delivery_date")
    private LocalDate deliveryDate;
    
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 0)
    private BigDecimal totalAmount;
    
    @Column(name = "discount_amount", nullable = false, precision = 15, scale = 0)
    private BigDecimal discountAmount;
    
    @Column(name = "is_included_tax", nullable = false)
    private Boolean isIncludedTax;
    
    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal taxRate;
    
    @Column(name = "tax_amount", nullable = false, precision = 15, scale = 0)
    private BigDecimal taxAmount;
    
    @Column(name = "final_amount", nullable = false, precision = 15, scale = 0)
    private BigDecimal finalAmount;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;


    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
    private Set<OrderLineEntity> orderLines = new HashSet<>();
}
