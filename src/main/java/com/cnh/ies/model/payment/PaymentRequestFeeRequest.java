package com.cnh.ies.model.payment;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class PaymentRequestFeeRequest {
    /** Gửi từ API GET khi cập nhật để giữ cùng bản ghi, tránh tạo mới + soft-delete. */
    private String id;
    private String feeName;
    private String feeType;
    private BigDecimal amount;
    private String note;
}
