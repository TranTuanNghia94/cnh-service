package com.cnh.ies.service.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.cnh.ies.exception.ApiException;
import com.cnh.ies.model.general.PaginationModel;
import com.cnh.ies.model.report.InboundDetailReportRow;
import com.cnh.ies.model.report.InboundSummaryReportRow;
import com.cnh.ies.model.report.OperationalReportPage;
import com.cnh.ies.model.report.OperationalReportRequest;
import com.cnh.ies.model.report.OutboundDetailReportRow;
import com.cnh.ies.model.report.OutboundSummaryReportRow;
import com.cnh.ies.model.report.PaymentLedgerRow;
import com.cnh.ies.model.report.SalesDetailReportRow;
import com.cnh.ies.model.report.StockLedgerRow;
import com.cnh.ies.model.report.VendorDebtRow;
import com.cnh.ies.repository.report.OperationalReportRepository;
import com.cnh.ies.util.RequestContext;
import com.cnh.ies.service.report.OperationalReportFormulas.PaymentCandidate;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OperationalReportService {

    private static final Pattern PAPER_CATEGORY = Pattern.compile("\"category\"\\s*:\\s*\"([^\"]+)\"");

    private final OperationalReportRepository repository;

    public OperationalReportPage<StockLedgerRow> stock(OperationalReportRequest request) {
        return page(stockRows(request), request);
    }

    public List<StockLedgerRow> stockRows(OperationalReportRequest request) {
        LocalDate[] range = range(request);
        List<StockLedgerRow> rows = new ArrayList<>();
        for (Object[] raw : repository.stockRows(range[0], range[1], request)) {
            BigDecimal beginning = OperationalReportRepository.decimal(raw[5]);
            BigDecimal inbound = OperationalReportRepository.decimal(raw[6]);
            BigDecimal outbound = OperationalReportRepository.decimal(raw[7]);
            if (!OperationalReportFormulas.includeStockRow(beginning, inbound, outbound)) {
                continue;
            }
            rows.add(StockLedgerRow.builder()
                    .productId(OperationalReportRepository.text(raw[0]))
                    .categoryName(OperationalReportRepository.text(raw[1]))
                    .productCode(OperationalReportRepository.text(raw[2]))
                    .productName(OperationalReportRepository.text(raw[3]))
                    .unit(OperationalReportRepository.text(raw[4]))
                    .beginningQuantity(beginning)
                    .inboundQuantity(inbound)
                    .outboundQuantity(outbound)
                    .endingQuantity(OperationalReportFormulas.closingStock(beginning, inbound, outbound))
                    .unitPrice(OperationalReportFormulas.roundMoney(OperationalReportRepository.decimal(raw[8])))
                    .build());
        }
        rows.sort(Comparator.comparing(StockLedgerRow::getEndingQuantity).reversed());
        return rows;
    }

    public OperationalReportPage<OutboundSummaryReportRow> outboundSummary(OperationalReportRequest request) {
        LocalDate[] range = dateRange(request);
        return page(outboundSummaryRows(request), request, range[0], range[1].minusDays(1));
    }

    public List<OutboundSummaryReportRow> outboundSummaryRows(OperationalReportRequest request) {
        LocalDate[] range = dateRange(request);
        List<OutboundSummaryReportRow> rows = new ArrayList<>();
        for (Object[] raw : repository.outboundSummaryRows(range[0], range[1], request)) {
            rows.add(OutboundSummaryReportRow.builder()
                    .userName(OperationalReportRepository.text(raw[0]))
                    .outboundDate(OperationalReportFormulas.formatDate(OperationalReportRepository.date(raw[1])))
                    .outboundNumber(OperationalReportRepository.text(raw[2]))
                    .contractNumber(OperationalReportRepository.text(raw[3]))
                    .customerName(OperationalReportRepository.text(raw[4]))
                    .amountBeforeTax(OperationalReportRepository.decimal(raw[5]))
                    .vatAmount(OperationalReportRepository.decimal(raw[6]))
                    .totalAmount(OperationalReportRepository.decimal(raw[7]))
                    .build());
        }
        return rows;
    }

    public OperationalReportPage<OutboundDetailReportRow> outboundDetail(OperationalReportRequest request) {
        LocalDate[] range = dateRange(request);
        return page(outboundDetailRows(request), request, range[0], range[1].minusDays(1));
    }

    public List<OutboundDetailReportRow> outboundDetailRows(OperationalReportRequest request) {
        LocalDate[] range = dateRange(request);
        List<OutboundDetailReportRow> rows = new ArrayList<>();
        for (Object[] raw : repository.outboundDetailRows(range[0], range[1], request)) {
            LocalDate date = OperationalReportRepository.date(raw[0]);
            String number = OperationalReportRepository.text(raw[1]);
            String customerCode = OperationalReportRepository.text(raw[2]);
            BigDecimal quantity = OperationalReportRepository.decimal(raw[6]);
            BigDecimal unitPrice = OperationalReportRepository.decimal(raw[7]);
            String formatted = OperationalReportFormulas.formatDate(date);
            rows.add(OutboundDetailReportRow.builder()
                    .postingDate(formatted)
                    .documentDate(formatted)
                    .documentNumber(number)
                    .outboundNumber(number)
                    .reason(OperationalReportFormulas.outboundReason(customerCode, number, date))
                    .invoiceNumber(number)
                    .invoiceDate(formatted)
                    .customerCode(customerCode)
                    .customerName(OperationalReportRepository.text(raw[3]))
                    .narrative(OperationalReportFormulas.outboundNarrative(number, customerCode))
                    .productCode(OperationalReportRepository.text(raw[4]))
                    .productName(OperationalReportRepository.text(raw[5]))
                    .debitAccount(OperationalReportFormulas.TK_RECEIVABLE)
                    .creditAccount(OperationalReportFormulas.TK_REVENUE)
                    .quantity(quantity)
                    .unitPrice(unitPrice)
                    .amount(OperationalReportFormulas.lineAmount(quantity, unitPrice))
                    .vatRate(OperationalReportRepository.decimal(raw[8]))
                    .vatAmount(OperationalReportFormulas.roundMoney(OperationalReportRepository.decimal(raw[9])))
                    .vatAccount(OperationalReportFormulas.TK_OUTPUT_VAT)
                    .warehouseCode(OperationalReportFormulas.WAREHOUSE_CODE)
                    .cogsAccount(OperationalReportFormulas.TK_COGS)
                    .inventoryAccount(OperationalReportFormulas.TK_INVENTORY)
                    .businessUnit(OperationalReportFormulas.BUSINESS_UNIT)
                    .build());
        }
        return rows;
    }

    public OperationalReportPage<InboundSummaryReportRow> inboundSummary(OperationalReportRequest request) {
        LocalDate[] range = dateRange(request);
        return page(inboundSummaryRows(request), request, range[0], range[1].minusDays(1));
    }

    public List<InboundSummaryReportRow> inboundSummaryRows(OperationalReportRequest request) {
        LocalDate[] range = dateRange(request);
        List<InboundSummaryReportRow> rows = new ArrayList<>();
        for (Object[] raw : repository.inboundSummaryRows(range[0], range[1], request)) {
            BigDecimal foreign = OperationalReportRepository.decimal(raw[6]);
            BigDecimal vat = OperationalReportRepository.decimal(raw[7]);
            BigDecimal fee = OperationalReportRepository.decimal(raw[8]);
            BigDecimal rate = OperationalReportRepository.decimal(raw[9]);
            BigDecimal foreignTotal = foreign.add(fee);
            rows.add(InboundSummaryReportRow.builder()
                    .userName(OperationalReportRepository.text(raw[0]))
                    .receivedDate(OperationalReportFormulas.formatDate(OperationalReportRepository.date(raw[1])))
                    .receiptNumber(OperationalReportRepository.text(raw[2]))
                    .contractNumber(OperationalReportRepository.text(raw[3]))
                    .vendorName(OperationalReportRepository.text(raw[4]))
                    .currency(OperationalReportRepository.text(raw[5]))
                    .foreignValue(foreign)
                    .vatAmount(vat)
                    .feeAmount(fee)
                    .foreignTotal(foreignTotal)
                    .vndTotal(OperationalReportFormulas.converted(foreignTotal, rate))
                    .build());
        }
        return rows;
    }

    public OperationalReportPage<InboundDetailReportRow> inboundDetail(OperationalReportRequest request) {
        LocalDate[] range = dateRange(request);
        return page(inboundDetailRows(request), request, range[0], range[1].minusDays(1));
    }

    public List<InboundDetailReportRow> inboundDetailRows(OperationalReportRequest request) {
        LocalDate[] range = dateRange(request);
        List<InboundDetailReportRow> rows = new ArrayList<>();
        for (Object[] raw : repository.inboundDetailRows(range[0], range[1], request)) {
            BigDecimal quantity = OperationalReportRepository.decimal(raw[6]);
            BigDecimal unitPrice = OperationalReportRepository.decimal(raw[7]);
            BigDecimal rate = OperationalReportRepository.decimal(raw[9]);
            BigDecimal vatRate = OperationalReportRepository.decimal(raw[10]);
            BigDecimal amount = OperationalReportFormulas.nz(quantity).multiply(OperationalReportFormulas.nz(unitPrice));
            BigDecimal vat = amount.multiply(vatRate).divide(BigDecimal.valueOf(100));
            rows.add(InboundDetailReportRow.builder()
                    .documentDate(OperationalReportFormulas.formatDate(OperationalReportRepository.date(raw[0])))
                    .receiptNumber(OperationalReportRepository.text(raw[1]))
                    .invoiceNumber(OperationalReportRepository.text(raw[2]))
                    .vendorCode(OperationalReportRepository.text(raw[3]))
                    .productCode(OperationalReportRepository.text(raw[4]))
                    .productName(OperationalReportRepository.text(raw[5]))
                    .warehouseCode(OperationalReportFormulas.WAREHOUSE_CODE)
                    .inventoryAccount(OperationalReportFormulas.TK_INVENTORY)
                    .payableAccount(OperationalReportFormulas.TK_PAYABLE)
                    .quantity(quantity)
                    .unitPrice(unitPrice)
                    .currency(OperationalReportRepository.text(raw[8]))
                    .exchangeRate(rate)
                    .amount(amount)
                    .convertedAmount(OperationalReportFormulas.converted(amount, rate))
                    .vatRate(vatRate)
                    .vatAmount(vat)
                    .convertedVatAmount(OperationalReportFormulas.nz(vat).multiply(OperationalReportFormulas.nz(rate)))
                    .inputVatAccount(OperationalReportFormulas.TK_INPUT_VAT)
                    .vatAccount(OperationalReportFormulas.TK_VAT_PAYABLE)
                    .billOnPaper(OperationalReportRepository.text(raw[11]))
                    .lineNote(OperationalReportRepository.text(raw[12]))
                    .build());
        }
        return rows;
    }

    public OperationalReportPage<PaymentLedgerRow> payments(OperationalReportRequest request) {
        return page(paymentRows(request), request);
    }

    public List<PaymentLedgerRow> paymentRows(OperationalReportRequest request) {
        LocalDate[] range = range(request);
        List<PaymentCandidate> candidates = new ArrayList<>();
        for (Object[] raw : repository.paymentRows(range[0], range[1], request)) {
            candidates.add(new PaymentCandidate(
                    OperationalReportRepository.text(raw[0]),
                    OperationalReportRepository.text(raw[1]),
                    OperationalReportRepository.text(raw[2]),
                    OperationalReportRepository.decimal(raw[3]),
                    OperationalReportRepository.instant(raw[4]),
                    OperationalReportRepository.instant(raw[5]),
                    OperationalReportRepository.text(raw[6]),
                    OperationalReportRepository.text(raw[7]),
                    OperationalReportRepository.text(raw[8]),
                    OperationalReportRepository.text(raw[9]),
                    OperationalReportRepository.text(raw[10]),
                    paperCategories(OperationalReportRepository.text(raw[11]))));
        }
        String documentNumber = request.getDocumentNumber();
        candidates.removeIf(item -> !OperationalReportFormulas.containsText(
                OperationalReportFormulas.documentKey(item.notes(), item.requestNumber()), documentNumber));
        return OperationalReportFormulas.groupPayments(candidates);
    }

    public OperationalReportPage<VendorDebtRow> vendorDebt(OperationalReportRequest request) {
        return page(vendorDebtRows(request), request, null, null);
    }

    public List<VendorDebtRow> vendorDebtRows(OperationalReportRequest request) {
        Map<String, VendorAccumulator> vendors = new LinkedHashMap<>();
        absorbInbound(vendors, repository.vendorInboundSums(null, null, false, request), false);
        absorbPaid(vendors, repository.vendorPaidSums(null, null, false, request), false);
        List<VendorDebtRow> rows = new ArrayList<>();
        for (VendorAccumulator item : vendors.values()) {
            BigDecimal debt = OperationalReportFormulas.vendorDebt(item.paymentTotal, item.paidPeriod);
            if (item.inboundPeriod.signum() == 0 && item.paidPeriod.signum() == 0 && item.paymentTotal.signum() == 0) {
                continue;
            }
            rows.add(VendorDebtRow.builder()
                    .vendorCode(item.code)
                    .vendorName(item.name)
                    .currency(item.currency)
                    .openingDebt(BigDecimal.ZERO)
                    .goodsValue(item.inboundPeriod)
                    .paidAmount(item.paidPeriod)
                    .closingDebt(debt)
                    .build());
        }
        rows.sort(Comparator.comparing(VendorDebtRow::getVendorCode));
        return rows;
    }

    public OperationalReportPage<SalesDetailReportRow> salesDetail(OperationalReportRequest request) {
        return page(salesDetailRows(request), request, null, null);
    }

    public List<SalesDetailReportRow> salesDetailRows(OperationalReportRequest request) {
        List<SalesDetailReportRow> rows = new ArrayList<>();
        for (Object[] raw : repository.salesDetailRows(null, null, request)) {
            String notes = OperationalReportRepository.text(raw[15]);
            rows.add(SalesDetailReportRow.builder()
                    .csName(OperationalReportRepository.text(raw[0]))
                    .customerCode(OperationalReportRepository.text(raw[1]))
                    .contractNumber(OperationalReportRepository.text(raw[2]))
                    .contractDate(OperationalReportFormulas.formatDate(OperationalReportRepository.date(raw[3])))
                    .productCode(OperationalReportRepository.text(raw[4]))
                    .productName(OperationalReportRepository.text(raw[5]))
                    .unit(OperationalReportRepository.text(raw[6]))
                    .salesQuantity(OperationalReportRepository.decimal(raw[7]))
                    .salesUnitPrice(OperationalReportRepository.decimal(raw[8]))
                    .salesAmount(OperationalReportRepository.decimal(raw[9]))
                    .taxRate(OperationalReportFormulas.taxLabel(OperationalReportRepository.decimal(raw[10])))
                    .suggestedVendorCode(OperationalReportRepository.text(raw[11]))
                    .reference(OperationalReportRepository.text(raw[12]))
                    .teacher("")
                    .department("")
                    .note(OperationalReportRepository.text(raw[13]))
                    .confirmedVendorCode(OperationalReportRepository.text(raw[14]))
                    .paymentPaperType(paperCategories(OperationalReportRepository.text(raw[16])))
                    .paymentPaperNumber(OperationalReportFormulas.documentKey(notes, ""))
                    .purchaseQuantity(OperationalReportRepository.decimal(raw[17]))
                    .currency(OperationalReportRepository.text(raw[18]))
                    .purchaseUnitPrice(OperationalReportRepository.decimal(raw[19]))
                    .purchaseAmount(OperationalReportRepository.decimal(raw[20]))
                    .quoteNumber(OperationalReportRepository.text(raw[21]))
                    .invoiceNumber(OperationalReportRepository.text(raw[22]))
                    .billNumber(OperationalReportRepository.text(raw[23]))
                    .receiptWarehouse(OperationalReportRepository.text(raw[24]))
                    .trackingNumber(OperationalReportRepository.text(raw[25]))
                    .inboundDate(OperationalReportFormulas.formatDate(OperationalReportRepository.date(raw[26])))
                    .inboundNumber(OperationalReportRepository.text(raw[27]))
                    .inboundQuantity(OperationalReportRepository.decimal(raw[28]))
                    .inboundUnitPrice(OperationalReportRepository.decimal(raw[29]))
                    .inboundAmount(OperationalReportRepository.decimal(raw[30]))
                    .inboundInvoiceNumber(OperationalReportRepository.text(raw[22]))
                    .bookBillNumber(OperationalReportRepository.text(raw[32]))
                    .inboundNote(OperationalReportRepository.text(raw[31]))
                    .build());
        }
        return rows;
    }

    private void absorbInbound(Map<String, VendorAccumulator> vendors, List<Object[]> rows, boolean before) {
        for (Object[] raw : rows) {
            VendorAccumulator item = vendor(vendors, raw);
            BigDecimal amount = OperationalReportRepository.decimal(raw[4]);
            if (before) {
                item.inboundBefore = item.inboundBefore.add(amount);
            } else {
                item.inboundPeriod = item.inboundPeriod.add(amount);
            }
        }
    }

    private void absorbPaid(Map<String, VendorAccumulator> vendors, List<Object[]> rows, boolean before) {
        for (Object[] raw : rows) {
            VendorAccumulator item = vendor(vendors, raw);
            BigDecimal amount = OperationalReportRepository.decimal(raw[4]);
            if (before) {
                item.paidBefore = item.paidBefore.add(amount);
            } else {
                item.paidPeriod = item.paidPeriod.add(amount);
                item.paymentTotal = item.paymentTotal.add(OperationalReportRepository.decimal(raw.length > 5 ? raw[5] : null));
            }
        }
    }

    private VendorAccumulator vendor(Map<String, VendorAccumulator> vendors, Object[] raw) {
        String key = OperationalReportRepository.text(raw[0]) + "|" + OperationalReportRepository.text(raw[3]);
        return vendors.computeIfAbsent(key, ignored -> new VendorAccumulator(
                OperationalReportRepository.text(raw[1]),
                OperationalReportRepository.text(raw[2]),
                OperationalReportRepository.text(raw[3])));
    }

    private static String paperCategories(String papersJson) {
        if (papersJson == null || papersJson.isBlank()) {
            return "";
        }
        Matcher matcher = PAPER_CATEGORY.matcher(papersJson);
        List<String> categories = new ArrayList<>();
        while (matcher.find()) {
            String category = matcher.group(1);
            if (!categories.contains(category)) {
                categories.add(category);
            }
        }
        return String.join(", ", categories);
    }

    private LocalDate[] range(OperationalReportRequest request) {
        if (request.getYear() == null || request.getMonth() == null) {
            throw badRequest("Month and year are required");
        }
        return new LocalDate[] {
                OperationalReportFormulas.periodStart(request.getYear(), request.getMonth()),
                OperationalReportFormulas.periodEndExclusive(request.getYear(), request.getMonth())
        };
    }

    private LocalDate[] dateRange(OperationalReportRequest request) {
        LocalDate from = request.getFromDate();
        LocalDate to = request.getToDate();
        if (from == null || to == null) {
            throw badRequest("From date and to date are required");
        }
        if (to.isBefore(from)) {
            throw badRequest("To date must be on or after from date");
        }
        return new LocalDate[] { from, to.plusDays(1) };
    }

    private <T> OperationalReportPage<T> page(List<T> rows, OperationalReportRequest request) {
        LocalDate[] bounds = range(request);
        return page(rows, request, bounds[0], bounds[1].minusDays(1));
    }

    private <T> OperationalReportPage<T> page(
            List<T> rows, OperationalReportRequest request, LocalDate fromDate, LocalDate toDate) {
        int page = request.getPage() == null ? 0 : request.getPage();
        int limit = request.getLimit() == null ? 20 : request.getLimit();
        int from = Math.min(page * limit, rows.size());
        int to = Math.min(from + limit, rows.size());
        int totalPage = limit == 0 ? 0 : (int) Math.ceil(rows.size() / (double) limit);
        return OperationalReportPage.<T>builder()
                .fromDate(fromDate)
                .toDate(toDate)
                .data(new ArrayList<>(rows.subList(from, to)))
                .pagination(PaginationModel.builder()
                        .page(page)
                        .limit(limit)
                        .total((long) rows.size())
                        .totalPage(totalPage)
                        .build())
                .build();
    }

    private static ApiException badRequest(String message) {
        return new ApiException(ApiException.ErrorCode.BAD_REQUEST, message,
                HttpStatus.BAD_REQUEST.value(), RequestContext.getRequestIdOrGenerate());
    }

    private static final class VendorAccumulator {
        private final String code;
        private final String name;
        private final String currency;
        private BigDecimal inboundBefore = BigDecimal.ZERO;
        private BigDecimal inboundPeriod = BigDecimal.ZERO;
        private BigDecimal paidBefore = BigDecimal.ZERO;
        private BigDecimal paidPeriod = BigDecimal.ZERO;
        private BigDecimal paymentTotal = BigDecimal.ZERO;

        private VendorAccumulator(String code, String name, String currency) {
            this.code = code;
            this.name = name;
            this.currency = currency;
        }
    }
}
