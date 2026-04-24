package com.cnh.ies.model.warehouse;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;

@Data
public class WarehouseInboundConfirmRequest {
    private String paymentRequestId;
    private BigDecimal exchangeRate;
    private BigDecimal feeAmount;
    private BigDecimal realBillAmount;
    private BigDecimal billOnPaperAmount;
    private String note;
    private Integer approvalLevels;
    private List<String> approvalRoles;
    private List<WarehouseInboundConfirmLineRequest> lines;
    private List<String> attachedFileIds;
}
