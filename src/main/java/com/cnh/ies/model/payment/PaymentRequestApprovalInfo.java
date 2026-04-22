package com.cnh.ies.model.payment;

import lombok.Data;

@Data
public class PaymentRequestApprovalInfo {
    private String id;
    private Integer level;
    private String role;
    private String approverId;
    private String status;
    private String approvedAt;
    private String rejectionReason;
    private String note;
}
