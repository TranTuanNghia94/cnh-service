package com.cnh.ies.model.payment;

import lombok.Data;

@Data
public class RejectPaymentRequest {
    private Integer level;
    private String reason;
    private String note;
}
