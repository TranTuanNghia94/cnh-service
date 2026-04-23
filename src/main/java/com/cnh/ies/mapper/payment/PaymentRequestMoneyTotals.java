package com.cnh.ies.mapper.payment;

import java.math.BigDecimal;

/** Normalized currency, rate, and VND totals derived from requested + fee amounts. */
public record PaymentRequestMoneyTotals(
        String normalizedCurrency,
        BigDecimal exchangeRate,
        BigDecimal requestedAmount,
        BigDecimal feeAmount,
        BigDecimal totalAmount,
        BigDecimal requestedAmountVnd,
        BigDecimal feeAmountVnd,
        BigDecimal totalAmountVnd) {
}
