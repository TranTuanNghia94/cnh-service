package com.cnh.ies.entity.payment;

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
@Table(name = "payment_request_extra_fees")
@Data
@EqualsAndHashCode(callSuper = true)
public class PaymentRequestExtraFeeEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_request_id", nullable = false)
    private PaymentRequestEntity paymentRequest;

    @Column(name = "fee_name", nullable = false, length = 100)
    private String feeName;

    @Column(name = "fee_type", length = 50)
    private String feeType;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;
}
