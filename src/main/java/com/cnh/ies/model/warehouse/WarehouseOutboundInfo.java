package com.cnh.ies.model.warehouse;

import java.math.BigDecimal;
import java.util.List;

import com.cnh.ies.model.payment.PaymentFileUploadInfo;
import com.cnh.ies.model.payment.PaymentRequestApprovalInfo;

import lombok.Data;

@Data
public class WarehouseOutboundInfo {
    private String id;
    private String outboundNumber;
    private String orderId;
    private String orderNumber;
    private String contractNumber;
    private String outboundReason;
    private String note;
    private String currency;
    private String outboundDate;
    private String status;
    private Integer approvalLevels;
    private Integer currentApprovalLevel;
    private BigDecimal totalAmount;
    private BigDecimal taxAmount;
    private String createdBy;
    private String createdAt;
    private List<WarehouseOutboundDetailInfo> details;
    private List<PaymentFileUploadInfo> attachments;
    private List<PaymentRequestApprovalInfo> approvals;
}
