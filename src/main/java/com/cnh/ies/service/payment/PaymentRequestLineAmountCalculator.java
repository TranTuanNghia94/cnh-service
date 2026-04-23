package com.cnh.ies.service.payment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.cnh.ies.entity.purchaseorder.PurchaseOrderLineEntity;
import com.cnh.ies.exception.ApiException;
import com.cnh.ies.model.payment.CreateOrUpdatePaymentRequest;
import com.cnh.ies.model.payment.PaymentRequestItemRequest;
import com.cnh.ies.mapper.payment.PaymentRequestMoneyMapper;

import lombok.RequiredArgsConstructor;

/**
 * Derives each payment line {@code requestedAmount} from {@code quantity × unitPrice} per PO line (in payment
 * currency) and {@code paidPercentage} (share of that line value to request). Client-supplied amounts are ignored.
 */
@Component
@RequiredArgsConstructor
public class PaymentRequestLineAmountCalculator {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal ONE_CENT = new BigDecimal("0.01");

    private final PaymentRequestMoneyMapper moneyMapper;

    public void applyCalculatedRequestedAmounts(CreateOrUpdatePaymentRequest request, List<PaymentRequestItemRequest> items,
            List<PurchaseOrderLineEntity> poLines, String requestId) {
        if (items == null || items.isEmpty()) {
            return;
        }
        String payCurrency = moneyMapper.normalizeCurrency(request.getCurrency());
        BigDecimal payRate = moneyMapper.resolveExchangeRate(payCurrency, request.getExchangeRate(), requestId);
        BigDecimal paidPct = request.getPaidPercentage() == null ? HUNDRED : request.getPaidPercentage();
        BigDecimal factor = paidPct.divide(HUNDRED, 8, RoundingMode.HALF_UP);

        Map<UUID, PurchaseOrderLineEntity> byId = new HashMap<>();
        for (PurchaseOrderLineEntity line : poLines) {
            byId.put(line.getId(), line);
        }

        List<BigDecimal> lineBases = new ArrayList<>(items.size());
        for (PaymentRequestItemRequest item : items) {
            UUID poLineId = UUID.fromString(item.getPurchaseOrderLineId());
            PurchaseOrderLineEntity line = byId.get(poLineId);
            if (line == null) {
                throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "Purchase order line not in selection: " + poLineId,
                        HttpStatus.BAD_REQUEST.value(), requestId);
            }
            lineBases.add(moneyMapper.purchaseOrderLinePayableInPaymentCurrency(line, payCurrency, payRate, requestId));
        }

        BigDecimal sumBases = lineBases.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sumBases.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "Selected purchase order lines have no payable total",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }

        BigDecimal exactRequested = sumBases.multiply(factor);
        BigDecimal totalRequested = exactRequested.setScale(2, RoundingMode.HALF_UP);
        if (totalRequested.compareTo(ONE_CENT) < 0) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "Calculated requested amount is too small",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }

        List<BigDecimal> floors = new ArrayList<>(items.size());
        BigDecimal sumFloors = BigDecimal.ZERO;
        for (BigDecimal base : lineBases) {
            BigDecimal floor = base.multiply(factor).setScale(2, RoundingMode.DOWN);
            floors.add(floor);
            sumFloors = sumFloors.add(floor);
        }

        BigDecimal diff = totalRequested.subtract(sumFloors);
        long extraCents = diff.movePointRight(2).setScale(0, RoundingMode.DOWN).longValue();
        if (extraCents < 0) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "Amount allocation rounding error",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }

        for (int i = 0; i < items.size(); i++) {
            items.get(i).setRequestedAmount(floors.get(i));
        }
        for (long k = 0; k < extraCents; k++) {
            int i = (int) (k % items.size());
            PaymentRequestItemRequest item = items.get(i);
            item.setRequestedAmount(item.getRequestedAmount().add(ONE_CENT));
        }
    }

}
