package com.cnh.ies.entity.warehouse;

import java.math.BigDecimal;
import java.time.Instant;

import com.cnh.ies.constant.Constant;
import com.cnh.ies.entity.BaseEntity;
import com.cnh.ies.entity.payment.PaymentRequestEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "warehouse_inbound_receipts")
@Data
@EqualsAndHashCode(callSuper = true)
public class WarehouseInboundReceiptEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_request_id", nullable = false)
    private PaymentRequestEntity paymentRequest;

    @Column(name = "currency", length = 10)
    private String currency;

    @Column(name = "exchange_rate", nullable = false, precision = 15, scale = 6)
    private BigDecimal exchangeRate = BigDecimal.ONE;

    @Column(name = "fee_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal feeAmount = BigDecimal.ZERO;

    @Column(name = "real_bill_amount", precision = 15, scale = 2)
    private BigDecimal realBillAmount;

    @Column(name = "bill_on_paper_amount", precision = 15, scale = 2)
    private BigDecimal billOnPaperAmount;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "status", nullable = false, length = 50)
    private String status = Constant.WAREHOUSE_INBOUND_STATUS_DRAFT;

    @Column(name = "approval_levels", nullable = false)
    private Integer approvalLevels = 0;

    @Column(name = "current_approval_level", nullable = false)
    private Integer currentApprovalLevel = 0;

    /** When set, inbound quantities were applied to {@code warehouse_inventory} (idempotent). */
    @Column(name = "inventory_posted_at")
    private Instant inventoryPostedAt;
}
