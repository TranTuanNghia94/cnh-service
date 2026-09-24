package com.cnh.ies.service.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.cnh.ies.model.report.PaymentLedgerRow;
import com.cnh.ies.service.report.OperationalReportFormulas.PaymentCandidate;

class OperationalReportFormulasTest {

    @Test
    void periodIsInclusiveStartAndExclusiveNextMonth() {
        assertEquals(LocalDate.of(2026, 9, 1), OperationalReportFormulas.periodStart(2026, 9));
        assertEquals(LocalDate.of(2026, 10, 1), OperationalReportFormulas.periodEndExclusive(2026, 9));
    }

    @Test
    void closingStockIsOpeningPlusInboundMinusOutbound() {
        assertEquals(new BigDecimal("12"), OperationalReportFormulas.closingStock(
                new BigDecimal("10"), new BigDecimal("5"), new BigDecimal("3")));
        assertEquals(new BigDecimal("-2"), OperationalReportFormulas.closingStock(
                BigDecimal.ZERO, BigDecimal.ONE, new BigDecimal("3")));
    }

    @Test
    void stockRowIsIncludedWhenAnyQuantityIsNonZero() {
        assertFalse(OperationalReportFormulas.includeStockRow(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        assertTrue(OperationalReportFormulas.includeStockRow(BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO));
        assertTrue(OperationalReportFormulas.includeStockRow(BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ZERO));
        assertTrue(OperationalReportFormulas.includeStockRow(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE));
    }

    @Test
    void paymentsGroupEveryInstallmentAndSplitTheTotal() {
        Instant first = Instant.parse("2026-09-01T00:00:00Z");
        Instant second = Instant.parse("2026-09-10T00:00:00Z");
        Instant third = Instant.parse("2026-09-20T00:00:00Z");
        Instant fourth = Instant.parse("2026-09-25T00:00:00Z");
        String notes = "HD-100\nextra";

        List<PaymentLedgerRow> rows = OperationalReportFormulas.groupPayments(List.of(
                candidate("PR-1", notes, "APPROVED", "100", first, null),
                candidate("PR-2", notes, "PAID", "40", null, second),
                candidate("PR-X", notes, "REJECTED", "999", second, second),
                candidate("PR-3", notes, "PARTIALLY_PAID", "10", third, null),
                candidate("PR-4", notes, "APPROVED", "7", fourth, null),
                candidate("PR-OTHER", "HD-200", "PAID", "5", first, null)));

        assertEquals(2, rows.size());
        PaymentLedgerRow grouped = rows.get(0);
        assertEquals("HD-100", grouped.getPaperNumber());
        assertEquals(4, grouped.getPaymentCount());
        assertEquals(new BigDecimal("157"), grouped.getTotalAmount());
        assertEquals(4, grouped.getInstallments().size());
        assertEquals(1, grouped.getInstallments().get(0).getSequence());
        assertEquals(4, grouped.getInstallments().get(3).getSequence());
        assertEquals("PR-4", grouped.getInstallments().get(3).getRequestNumber());
        assertEquals(new BigDecimal("7"), grouped.getInstallments().get(3).getAmount());
        assertEquals("APPROVED", grouped.getInstallments().get(3).getStatus());
        assertEquals(new BigDecimal("63.69"), grouped.getInstallments().get(0).getPercentage());
        assertEquals(new BigDecimal("25.48"), grouped.getInstallments().get(1).getPercentage());
        assertEquals(new BigDecimal("6.37"), grouped.getInstallments().get(2).getPercentage());
        assertEquals(new BigDecimal("4.46"), grouped.getInstallments().get(3).getPercentage());
        assertEquals("", grouped.getCompany());
        assertEquals("MISA-V", grouped.getVendorCode());
        assertEquals("HD-200", rows.get(1).getPaperNumber());
        assertEquals(1, rows.get(1).getPaymentCount());
        assertEquals(new BigDecimal("5"), rows.get(1).getTotalAmount());
        assertEquals(new BigDecimal("100.00"), rows.get(1).getInstallments().get(0).getPercentage());
    }

    @Test
    void paymentDocumentNumberMatchesTheGroupKey() {
        assertTrue(OperationalReportFormulas.containsText(
                OperationalReportFormulas.documentKey("HD-100\nextra", "PR-1"), "hd-100"));
        assertFalse(OperationalReportFormulas.containsText(
                OperationalReportFormulas.documentKey("HD-100\nextra", "PR-1"), "extra"));
        assertTrue(OperationalReportFormulas.containsText("HD-100", "  "));
    }

    @Test
    void vendorDebtOpeningAndClosing() {
        BigDecimal opening = OperationalReportFormulas.openingDebt(new BigDecimal("100"), new BigDecimal("40"));
        assertEquals(new BigDecimal("60"), opening);
        assertEquals(new BigDecimal("70"), OperationalReportFormulas.closingDebt(
                opening, new BigDecimal("20"), new BigDecimal("10")));
        assertEquals(new BigDecimal("65"), OperationalReportFormulas.vendorDebt(
                new BigDecimal("100").add(new BigDecimal("5")), new BigDecimal("40")));
    }

    @Test
    void zeroTaxRateRendersAsUndetermined() {
        assertEquals("KXĐ", OperationalReportFormulas.taxLabel(BigDecimal.ZERO));
        assertEquals("KXĐ", OperationalReportFormulas.taxLabel(null));
        assertEquals("8", OperationalReportFormulas.taxLabel(new BigDecimal("8.00")));
    }

    private static PaymentCandidate candidate(
            String number, String notes, String status, String amount, Instant paidAt, Instant requestDate) {
        return new PaymentCandidate(
                number, notes, status, new BigDecimal(amount), paidAt, requestDate,
                "USD", "V001", "MISA-V", "USD", "Customer A", "");
    }
}
