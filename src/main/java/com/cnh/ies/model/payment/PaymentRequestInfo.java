package com.cnh.ies.model.payment;

import java.math.BigDecimal;
import java.util.List;

import com.cnh.ies.model.purchaseorder.PurchaseOrderInfo;

import lombok.Data;

@Data
public class PaymentRequestInfo {
    private String id;
    private String requestNumber;
    private String requestDate;
    private String requestorId;
    private String vendorId;
    private String status;
    private Integer approvalLevels;
    private Integer currentApprovalLevel;
    private String currency;
    private BigDecimal exchangeRate;
    private BigDecimal amount;
    private BigDecimal requestedAmount;
    private BigDecimal requestedAmountVnd;
    private BigDecimal feeAmount;
    private BigDecimal feeAmountVnd;
    private BigDecimal totalAmount;
    private BigDecimal totalAmountVnd;
    private BigDecimal paidAmount;
    private BigDecimal paidPercentage;
    private BigDecimal paidAmountVnd;
    private String purpose;
    private String notes;
    private List<PaymentFileObject> papers;
    private PaymentBankInfoObject bankInfo;
    private PaymentBankNoteObject bankNote;
    private String paidBy;
    private String paidAt;
    private String createdBy;
    private String customerCode;
    private List<PaymentRequestLineInfo> items;
    private List<PaymentRequestFeeInfo> fees;
    private List<PaymentRequestApprovalInfo> approvals;
    private List<PurchaseOrderInfo> purchaseOrders;
}
