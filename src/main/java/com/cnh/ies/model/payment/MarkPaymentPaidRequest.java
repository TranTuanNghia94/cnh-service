package com.cnh.ies.model.payment;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class MarkPaymentPaidRequest {
    private BigDecimal paidAmount;
    private BigDecimal exchangeRate;
    private PaymentBankNoteObject bankNote;
}
