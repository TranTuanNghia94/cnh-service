package com.cnh.ies.model.warehouse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import lombok.Data;

@Data
public class WarehouseInboundConfirmRequest {
    private String paymentRequestId;
    private String purchaseOrderId;
    private BigDecimal exchangeRate;
    private BigDecimal feeAmount;
    private List<WarehouseInboundFeeRequest> fees;
    private BigDecimal realBillAmount;
    private BigDecimal billOnPaperAmount;
    private String note;
    private LocalDate receivedDate;
    private Integer approvalLevels;
    private List<String> approvalRoles;
    private List<WarehouseInboundConfirmLineRequest> lines;
    private List<String> attachedFileIds;
}
