package com.cnh.ies.mapper.payment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.cnh.ies.entity.payment.PaymentRequestEntity;
import com.cnh.ies.entity.payment.PaymentRequestExtraFeeEntity;
import com.cnh.ies.entity.payment.PaymentRequestPurchaseOrderLineEntity;
import com.cnh.ies.entity.purchaseorder.PurchaseOrderLineEntity;
import com.cnh.ies.exception.ApiException;
import com.cnh.ies.model.payment.CreateOrUpdatePaymentRequest;
import com.cnh.ies.model.payment.PaymentRequestInfo;

@Component
public class PaymentRequestMoneyMapper {

    public String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return "VND";
        }
        return currency.trim().toUpperCase();
    }

    public BigDecimal resolveExchangeRate(String currency, BigDecimal exchangeRate, String requestId) {
        if ("VND".equalsIgnoreCase(currency)) {
            return BigDecimal.ONE;
        }
        if (exchangeRate == null || exchangeRate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST,
                    "exchangeRate is required and must be > 0 for non-VND currency",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }
        return exchangeRate;
    }

    public void validateCurrencyAndExchangeForCreate(CreateOrUpdatePaymentRequest request, String requestId) {
        String normalizedCurrency = normalizeCurrency(request.getCurrency());
        resolveExchangeRate(normalizedCurrency, request.getExchangeRate(), requestId);
    }

    public BigDecimal toVnd(BigDecimal amount, BigDecimal exchangeRate) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        return amount.multiply(exchangeRate);
    }

    public BigDecimal sumLineRequestedAmounts(List<PaymentRequestPurchaseOrderLineEntity> items) {
        return items.stream()
                .map(item -> item.getRequestedAmount() == null ? BigDecimal.ZERO : item.getRequestedAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Line value for payment allocation: {@code quantity × unitPrice} in payment currency (header {@code amount} /
     * paid-percentage bases). When {@code totalPriceVnd} and a positive line total exist, VND is scaled from that
     * total so FX matches stored PO totals; otherwise the line {@code exchangeRate} is applied to {@code lineLocal}.
     */
    public BigDecimal purchaseOrderLinePayableInPaymentCurrency(PurchaseOrderLineEntity line, String paymentCurrency,
            BigDecimal paymentExchangeRate, String requestId) {
        String payCurrency = normalizeCurrency(paymentCurrency);
        BigDecimal qty = line.getQuantity();
        BigDecimal unit = line.getUnitPrice();
        if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST,
                    "Purchase order line " + line.getId() + " must have quantity > 0 for payment calculation",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }
        if (unit == null || unit.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST,
                    "Purchase order line " + line.getId() + " must have unitPrice > 0 for payment calculation",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }
        BigDecimal lineLocal = qty.multiply(unit);

        String lineCurrency = normalizeCurrency(line.getCurrency());
        if (lineCurrency.equals(payCurrency)) {
            return lineLocal.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal lineAmountVnd = line.getTotalPriceVnd();
        BigDecimal denominator = line.getTotalPrice();
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) <= 0) {
            denominator = line.getTotalBeforeTax();
        }
        if (lineAmountVnd != null && lineAmountVnd.compareTo(BigDecimal.ZERO) > 0
                && denominator != null && denominator.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal proportionalVnd = lineAmountVnd.multiply(lineLocal).divide(denominator, 8, RoundingMode.HALF_UP);
            if ("VND".equalsIgnoreCase(payCurrency)) {
                return proportionalVnd.setScale(2, RoundingMode.HALF_UP);
            }
            if (paymentExchangeRate == null || paymentExchangeRate.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "exchangeRate is required for currency conversion",
                        HttpStatus.BAD_REQUEST.value(), requestId);
            }
            return proportionalVnd.divide(paymentExchangeRate, 2, RoundingMode.HALF_UP);
        }

        BigDecimal lineRate = line.getExchangeRate() == null || line.getExchangeRate().compareTo(BigDecimal.ZERO) <= 0
                ? BigDecimal.ONE
                : line.getExchangeRate();
        BigDecimal derivedVnd = lineLocal.multiply(lineRate);
        if ("VND".equalsIgnoreCase(payCurrency)) {
            return derivedVnd.setScale(2, RoundingMode.HALF_UP);
        }
        if (paymentExchangeRate == null || paymentExchangeRate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "exchangeRate is required for currency conversion",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }
        return derivedVnd.divide(paymentExchangeRate, 2, RoundingMode.HALF_UP);
    }

    public BigDecimal sumPurchaseOrderLinePayables(List<PurchaseOrderLineEntity> lines, String paymentCurrency,
            BigDecimal paymentExchangeRate, String requestId) {
        if (lines == null || lines.isEmpty()) {
            return BigDecimal.ZERO;
        }
        String payCurrency = normalizeCurrency(paymentCurrency);
        BigDecimal payRate = resolveExchangeRate(payCurrency, paymentExchangeRate, requestId);
        return lines.stream()
                .map(line -> purchaseOrderLinePayableInPaymentCurrency(line, payCurrency, payRate, requestId))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Sum of each linked PO line's payable in payment currency (header {@code amount}, excludes fees). */
    public BigDecimal sumLineItemPayableAmounts(List<PaymentRequestPurchaseOrderLineEntity> items, String paymentCurrency,
            BigDecimal paymentExchangeRate, String requestId) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        String payCurrency = normalizeCurrency(paymentCurrency);
        BigDecimal payRate = resolveExchangeRate(payCurrency, paymentExchangeRate, requestId);
        return items.stream()
                .map(PaymentRequestPurchaseOrderLineEntity::getPurchaseOrderLine)
                .filter(Objects::nonNull)
                .map(line -> purchaseOrderLinePayableInPaymentCurrency(line, payCurrency, payRate, requestId))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal sumExtraFeeAmounts(List<PaymentRequestExtraFeeEntity> fees) {
        return fees.stream()
                .map(fee -> fee.getAmount() == null ? BigDecimal.ZERO : fee.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public PaymentRequestMoneyTotals computeMoneyTotals(BigDecimal requestedAmount, BigDecimal feeAmount, String currency,
            BigDecimal storedExchangeRate, String requestId) {
        String normalizedCurrency = normalizeCurrency(currency);
        BigDecimal exchangeRate = resolveExchangeRate(normalizedCurrency, storedExchangeRate, requestId);
        BigDecimal totalAmount = requestedAmount.add(feeAmount);
        BigDecimal requestedAmountVnd = toVnd(requestedAmount, exchangeRate);
        BigDecimal feeAmountVnd = toVnd(feeAmount, exchangeRate);
        // Match header upsert: VND total = sum of converted parts (not a single multiply on the sum).
        BigDecimal totalAmountVnd = requestedAmountVnd.add(feeAmountVnd);
        return new PaymentRequestMoneyTotals(normalizedCurrency, exchangeRate, requestedAmount, feeAmount, totalAmount,
                requestedAmountVnd, feeAmountVnd, totalAmountVnd);
    }

    public void applyMoneyTotals(PaymentRequestEntity entity, PaymentRequestMoneyTotals totals) {
        entity.setCurrency(totals.normalizedCurrency());
        entity.setExchangeRate(totals.exchangeRate());
        entity.setRequestedAmount(totals.requestedAmount());
        entity.setFeeAmount(totals.feeAmount());
        entity.setTotalAmount(totals.totalAmount());
        entity.setRequestedAmountVnd(totals.requestedAmountVnd());
        entity.setFeeAmountVnd(totals.feeAmountVnd());
        entity.setTotalAmountVnd(totals.totalAmountVnd());
    }

    public void applyMoneyTotals(PaymentRequestInfo info, PaymentRequestMoneyTotals totals) {
        info.setCurrency(totals.normalizedCurrency());
        info.setExchangeRate(totals.exchangeRate());
        info.setRequestedAmount(totals.requestedAmount());
        info.setFeeAmount(totals.feeAmount());
        info.setTotalAmount(totals.totalAmount());
        info.setRequestedAmountVnd(totals.requestedAmountVnd());
        info.setFeeAmountVnd(totals.feeAmountVnd());
        info.setTotalAmountVnd(totals.totalAmountVnd());
    }

    public BigDecimal calculatePaidPercentage(BigDecimal paidAmount, BigDecimal totalAmount) {
        BigDecimal safePaid = paidAmount == null ? BigDecimal.ZERO : paidAmount;
        BigDecimal safeTotal = totalAmount == null ? BigDecimal.ZERO : totalAmount;
        if (safeTotal.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal percentage = safePaid.multiply(BigDecimal.valueOf(100))
                .divide(safeTotal, 2, RoundingMode.HALF_UP);
        if (percentage.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        if (percentage.compareTo(BigDecimal.valueOf(100)) > 0) {
            return BigDecimal.valueOf(100);
        }
        return percentage;
    }
}
