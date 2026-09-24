package com.cnh.ies.service.report;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.cnh.ies.model.report.PaymentInstallment;
import com.cnh.ies.model.report.PaymentLedgerRow;

public final class OperationalReportFormulas {

    public static final String TK_RECEIVABLE = "131-03";
    public static final String TK_REVENUE = "5111-06";
    public static final String TK_OUTPUT_VAT = "33311";
    public static final String WAREHOUSE_CODE = "KHOHH_OVERSEA";
    public static final String TK_COGS = "6321-06";
    public static final String TK_INVENTORY = "1561-06";
    public static final String BUSINESS_UNIT = "CS";
    public static final String TK_PAYABLE = "331-02";
    public static final String TK_INPUT_VAT = "1331";
    public static final String TK_VAT_PAYABLE = "33312";
    public static final String PURCHASE_FORM = "1";

    private static final DateTimeFormatter VI_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final ZoneId ZONE = ZoneId.systemDefault();

    private OperationalReportFormulas() {}

    public static LocalDate periodStart(int year, int month) {
        return LocalDate.of(year, month, 1);
    }

    public static LocalDate periodEndExclusive(int year, int month) {
        return periodStart(year, month).plusMonths(1);
    }

    public static BigDecimal closingStock(BigDecimal beginning, BigDecimal inbound, BigDecimal outbound) {
        return nz(beginning).add(nz(inbound)).subtract(nz(outbound));
    }

    public static boolean includeStockRow(BigDecimal beginning, BigDecimal inbound, BigDecimal outbound) {
        BigDecimal ending = closingStock(beginning, inbound, outbound);
        return nz(beginning).signum() != 0
                || nz(inbound).signum() != 0
                || nz(outbound).signum() != 0
                || ending.signum() != 0;
    }

    public static BigDecimal openingDebt(BigDecimal inboundBefore, BigDecimal paidBefore) {
        return nz(inboundBefore).subtract(nz(paidBefore));
    }

    public static BigDecimal vendorDebt(BigDecimal paymentTotal, BigDecimal paid) {
        return nz(paymentTotal).subtract(nz(paid));
    }

    public static BigDecimal closingDebt(BigDecimal opening, BigDecimal goodsIn, BigDecimal paid) {
        return nz(opening).add(nz(goodsIn)).subtract(nz(paid));
    }

    public static String taxLabel(BigDecimal taxRate) {
        if (taxRate == null || taxRate.compareTo(BigDecimal.ZERO) == 0) {
            return "KXĐ";
        }
        return taxRate.stripTrailingZeros().toPlainString();
    }

    public static String preferMisa(String misaCode, String code) {
        if (misaCode != null && !misaCode.isBlank()) {
            return misaCode.trim();
        }
        return code == null ? "" : code.trim();
    }

    public static boolean containsText(String value, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        return value != null && value.toLowerCase(java.util.Locale.ROOT).contains(query.trim().toLowerCase(java.util.Locale.ROOT));
    }

    public static String documentKey(String notes, String fallback) {
        if (notes != null) {
            String firstLine = notes.split("\\R", 2)[0].trim();
            if (!firstLine.isEmpty()) {
                return firstLine;
            }
        }
        return fallback == null ? "" : fallback;
    }

    public static String formatDate(LocalDate date) {
        return date == null ? "" : VI_DATE.format(date);
    }

    public static String formatDate(Instant instant) {
        if (instant == null) {
            return "";
        }
        return VI_DATE.format(instant.atZone(ZONE).toLocalDate());
    }

    public static BigDecimal roundMoney(BigDecimal value) {
        return nz(value).setScale(0, RoundingMode.HALF_UP);
    }

    public static BigDecimal lineAmount(BigDecimal quantity, BigDecimal unitPrice) {
        return roundMoney(nz(quantity).multiply(nz(unitPrice)));
    }

    public static BigDecimal converted(BigDecimal amount, BigDecimal exchangeRate) {
        return roundMoney(nz(amount).multiply(nz(exchangeRate)));
    }

    public static String outboundReason(String customerCode, String outboundNumber, LocalDate outboundDate) {
        return "Bán hàng " + nullToEmpty(customerCode)
                + " theo " + nullToEmpty(outboundNumber)
                + " ngày " + formatDate(outboundDate);
    }

    public static String outboundNarrative(String outboundNumber, String customerCode) {
        return "Bán hàng theo " + nullToEmpty(outboundNumber) + " " + nullToEmpty(customerCode);
    }

    public static List<PaymentLedgerRow> groupPayments(List<PaymentCandidate> sources) {
        Map<String, List<PaymentCandidate>> grouped = new LinkedHashMap<>();
        if (sources == null) {
            return List.of();
        }
        for (PaymentCandidate item : sources) {
            if (item == null || "REJECTED".equalsIgnoreCase(item.status())) {
                continue;
            }
            String key = documentKey(item.notes(), item.requestNumber());
            grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(item);
        }
        List<PaymentLedgerRow> rows = new ArrayList<>();
        for (Map.Entry<String, List<PaymentCandidate>> entry : grouped.entrySet()) {
            rows.add(toPaymentRow(entry.getKey(), entry.getValue()));
        }
        return rows;
    }

    private static PaymentLedgerRow toPaymentRow(String key, List<PaymentCandidate> items) {
        PaymentCandidate first = items.get(0);
        BigDecimal total = BigDecimal.ZERO;
        for (PaymentCandidate item : items) {
            total = total.add(nz(item.totalAmount()));
        }
        List<PaymentInstallment> installments = new ArrayList<>();
        BigDecimal assigned = BigDecimal.ZERO;
        for (int index = 0; index < items.size(); index++) {
            PaymentCandidate item = items.get(index);
            BigDecimal amount = nz(item.totalAmount());
            BigDecimal percentage = installmentShare(amount, total, index == items.size() - 1, assigned);
            if (index < items.size() - 1) {
                assigned = assigned.add(percentage);
            }
            Instant paidOn = item.paidAt() != null ? item.paidAt() : item.requestDate();
            installments.add(PaymentInstallment.builder()
                    .sequence(index + 1)
                    .requestNumber(nullToEmpty(item.requestNumber()))
                    .amount(amount)
                    .paidDate(formatDate(paidOn))
                    .percentage(percentage)
                    .status(nullToEmpty(item.status()))
                    .build());
        }
        return PaymentLedgerRow.builder()
                .company("")
                .vendorCode(preferMisa(first.vendorMisaCode(), first.vendorCode()))
                .totalAmount(total)
                .paymentCount(items.size())
                .customerName(nullToEmpty(first.customerName()))
                .currency(firstNonBlank(first.vendorCurrency(), first.currency()))
                .paperType(firstNonBlank(first.paperType(), key))
                .paperNumber(key)
                .note("")
                .installments(installments)
                .build();
    }

    private static BigDecimal installmentShare(
            BigDecimal amount, BigDecimal total, boolean last, BigDecimal assigned) {
        if (total.signum() == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (last) {
            return new BigDecimal("100").subtract(assigned).setScale(2, RoundingMode.HALF_UP);
        }
        return amount.multiply(new BigDecimal("100")).divide(total, 2, RoundingMode.HALF_UP);
    }

    public static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    public static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        return second == null ? "" : second.trim();
    }

    public record PaymentCandidate(
            String requestNumber,
            String notes,
            String status,
            BigDecimal totalAmount,
            Instant paidAt,
            Instant requestDate,
            String currency,
            String vendorCode,
            String vendorMisaCode,
            String vendorCurrency,
            String customerName,
            String paperType) {}
}
