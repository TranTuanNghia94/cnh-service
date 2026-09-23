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
public class WarehouseOutboundDetailRow {

    private String outboundId;
    private String outboundNumber;
    private LocalDate outboundDate;
    private String status;
    private String contractNumber;
    private String orderNumber;
    private String productCode;
    private String productName;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private BigDecimal taxAmount;
    private String createdBy;
}
