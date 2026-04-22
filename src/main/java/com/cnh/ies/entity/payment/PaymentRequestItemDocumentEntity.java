package com.cnh.ies.entity.payment;

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
@Table(name = "payment_request_item_documents")
@Data
@EqualsAndHashCode(callSuper = true)
public class PaymentRequestItemDocumentEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_request_item_id", nullable = false)
    private PaymentRequestPurchaseOrderLineEntity paymentRequestItem;

    @Column(name = "document_type", nullable = false, length = 50)
    private String documentType;
}
