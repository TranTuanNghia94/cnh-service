package com.cnh.ies.model.payment;

import lombok.Data;

@Data
public class ApprovePaymentRequest {
    private Integer level;
    private String note;
}
