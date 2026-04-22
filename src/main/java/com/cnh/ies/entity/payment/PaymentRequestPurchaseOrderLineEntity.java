package com.cnh.ies.entity.payment;

import java.math.BigDecimal;

import com.cnh.ies.entity.BaseEntity;
import com.cnh.ies.entity.purchaseorder.PurchaseOrderLineEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "payment_request_purchase_order_lines")
@Data
@EqualsAndHashCode(callSuper = true)
public class PaymentRequestPurchaseOrderLineEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_request_id", nullable = false)
    private PaymentRequestEntity paymentRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_line_id", nullable = false)
    private PurchaseOrderLineEntity purchaseOrderLine;

    @Column(name = "selected_documents", nullable = false, columnDefinition = "TEXT")
    private String selectedDocuments;

    @Column(name = "requested_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal requestedAmount = BigDecimal.ZERO;

    @Column(name = "paid_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;
}
