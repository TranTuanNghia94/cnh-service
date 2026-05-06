package com.cnh.ies.model.warehouse;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;

@Data
public class DeliverySlipInfo {
    private String outboundId;
    private String outboundNumber;
    private String outboundDate;
    private String outboundReason;
    private String outboundStatus;
    private String contractNumber;
    private String note;
    private String createdAt;
    private String createdBy;

    private String orderId;
    private String orderNumber;
    private String orderDate;
    private String deliveryDate;
    private String orderStatus;

    private String customerId;
    private String customerCode;
    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private String customerTaxCode;

    private String customerAddressId;
    private String customerAddress;
    private String customerContactPerson;
    private String customerAddressPhone;
    private String customerAddressEmail;

    private String currency;
    private BigDecimal totalAmount;
    private BigDecimal taxAmount;
    private List<DeliverySlipLineInfo> lines;
}
