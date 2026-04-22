package com.cnh.ies.entity.payment;

import java.math.BigDecimal;
import java.time.Instant;

import com.cnh.ies.entity.BaseEntity;
import com.cnh.ies.entity.auth.UserEntity;
import com.cnh.ies.entity.vendors.VendorsEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "payment_requests")
@Data
@EqualsAndHashCode(callSuper = true)
public class PaymentRequestEntity extends BaseEntity {

    @Column(name = "request_number", nullable = false, unique = true, length = 50)
    private String requestNumber;

    @Column(name = "request_date", nullable = false)
    private Instant requestDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requestor_id", nullable = false)
    private UserEntity requestor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id")
    private VendorsEntity vendor;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency = "VND";

    @Column(name = "exchange_rate", nullable = false, precision = 15, scale = 6)
    private BigDecimal exchangeRate = BigDecimal.ONE;

    @Column(name = "purpose", columnDefinition = "TEXT")
    private String purpose;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "papers", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String papers;

    @Column(name = "bank_info", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String bankInfo;

    @Column(name = "bank_note", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String bankNote;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "approval_levels", nullable = false)
    private Integer approvalLevels = 2;

    @Column(name = "current_approval_level", nullable = false)
    private Integer currentApprovalLevel = 0;

    @Column(name = "requested_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal requestedAmount = BigDecimal.ZERO;

    // Backward-compatible field for legacy schema (V3).
    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "requested_amount_vnd", nullable = false, precision = 15, scale = 2)
    private BigDecimal requestedAmountVnd = BigDecimal.ZERO;

    @Column(name = "fee_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal feeAmount = BigDecimal.ZERO;

    @Column(name = "fee_amount_vnd", nullable = false, precision = 15, scale = 2)
    private BigDecimal feeAmountVnd = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "total_amount_vnd", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmountVnd = BigDecimal.ZERO;

    @Column(name = "paid_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "paid_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal paidPercentage = BigDecimal.ZERO;

    @Column(name = "paid_amount_vnd", nullable = false, precision = 15, scale = 2)
    private BigDecimal paidAmountVnd = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paid_by")
    private UserEntity paidBy;

    @Column(name = "paid_at")
    private Instant paidAt;
}
