package com.cnh.ies.entity.warehouse;

import java.math.BigDecimal;

import com.cnh.ies.entity.BaseEntity;
import com.cnh.ies.entity.payment.PaymentRequestPurchaseOrderLineEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "warehouse_inbound_receipt_lines")
@Data
@EqualsAndHashCode(callSuper = true)
public class WarehouseInboundReceiptLineEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_id", nullable = false)
    private WarehouseInboundReceiptEntity receipt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_request_purchase_order_line_id", nullable = false)
    private PaymentRequestPurchaseOrderLineEntity paymentRequestPurchaseOrderLine;

    @Column(name = "quantity_expected", precision = 15, scale = 3)
    private BigDecimal quantityExpected;

    @Column(name = "quantity_received", nullable = false, precision = 15, scale = 3)
    private BigDecimal quantityReceived;

    @Column(name = "tax_percent", precision = 5, scale = 2)
    private BigDecimal taxPercent;

    @Column(name = "line_note", columnDefinition = "TEXT")
    private String lineNote;
}
