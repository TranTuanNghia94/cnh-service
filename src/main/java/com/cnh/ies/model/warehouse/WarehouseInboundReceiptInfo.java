package com.cnh.ies.model.warehouse;

import java.math.BigDecimal;
import java.util.List;

import com.cnh.ies.model.payment.PaymentFileUploadInfo;
import com.cnh.ies.model.payment.PaymentRequestApprovalInfo;

import lombok.Data;

@Data
public class WarehouseInboundReceiptInfo {
    private String id;
    private String receiptNumber;
    private String paymentRequestId;
    private String status;
    private Integer approvalLevels;
    private Integer currentApprovalLevel;
    private String currency;
    private BigDecimal exchangeRate;
    private BigDecimal feeAmount;
    private List<WarehouseInboundFeeInfo> fees;
    private BigDecimal realBillAmount;
    private BigDecimal billOnPaperAmount;
    private String note;
    private String receivedDate;
    private String inventoryPostedAt;
    private String createdAt;
    private String createdBy;
    private List<WarehouseInboundReceiptLineInfo> lines;
    private List<WarehouseInboundPurchaseOrderInfo> purchaseOrders;
    private List<WarehouseInboundOrderInfo> orders;
    private List<PaymentFileUploadInfo> attachments;
    private List<PaymentRequestApprovalInfo> approvals;
}
