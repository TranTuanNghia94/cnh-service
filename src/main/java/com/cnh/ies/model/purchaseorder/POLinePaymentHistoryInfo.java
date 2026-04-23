package com.cnh.ies.model.purchaseorder;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payment history response for purchase order lines matching a document.
 * Returns a summary and list of related payment requests.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class POLinePaymentHistoryInfo {

    private int totalPOLinesFound;

    private BigDecimal totalAmount;

    private BigDecimal totalPaidAmount;

    private BigDecimal totalRemainingAmount;

    private BigDecimal paidPercentage;

    private List<PaymentRequestSummary> paymentRequests;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentRequestSummary {
        private String paymentRequestId;
        private String paymentRequestNumber;
        private String status;
        private String requestDate;
        private String vendorName;
        private String currency;
        private BigDecimal exchangeRate;
        private BigDecimal totalAmount;
        private BigDecimal totalAmountVnd;
        private BigDecimal requestedAmount;
        private BigDecimal requestedAmountVnd;
        private BigDecimal paidAmount;
        private BigDecimal paidAmountVnd;
        private BigDecimal paidPercentage;
        private String paidAt;
        private String paidBy;
        private String purpose;
        private int poLinesCount;
    }
}
