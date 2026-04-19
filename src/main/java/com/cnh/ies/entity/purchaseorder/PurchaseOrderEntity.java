package com.cnh.ies.entity.purchaseorder;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import com.cnh.ies.entity.BaseEntity;
import com.cnh.ies.entity.order.OrderEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "purchase_orders")
@Data
@EqualsAndHashCode(callSuper = true)
public class PurchaseOrderEntity extends BaseEntity {

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "po_number", nullable = false)
    private Integer poNumber;

    @Column(name = "po_prefix", nullable = false, length = 10)
    private String poPrefix;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private OrderEntity order;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Column(name = "expected_delivery_date")
    private LocalDate expectedDeliveryDate;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "purchaseOrder", fetch = FetchType.LAZY)
    private Set<PurchaseOrderLineEntity> purchaseOrderLines = new HashSet<>();
}
