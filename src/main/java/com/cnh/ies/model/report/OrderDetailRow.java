package com.cnh.ies.model.report;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailRow {

    private String orderId;
    private String orderNumber;
    private LocalDate orderDate;
    private String status;
    private String customerName;
    private String contractNumber;
    private String productCode;
    private String productName;
    private String vendorCode;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private String createdBy;
}
