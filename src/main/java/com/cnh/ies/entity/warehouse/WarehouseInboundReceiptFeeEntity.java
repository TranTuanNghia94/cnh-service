package com.cnh.ies.entity.warehouse;

import java.math.BigDecimal;

import com.cnh.ies.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "warehouse_inbound_receipt_fees")
@Data
@EqualsAndHashCode(callSuper = true)
public class WarehouseInboundReceiptFeeEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_id", nullable = false)
    private WarehouseInboundReceiptEntity receipt;

    @Column(name = "fee_name", nullable = false, length = 100)
    private String feeName;

    @Column(name = "fee_type", length = 50)
    private String feeType;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;
}
