package com.cnh.ies.service.report;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.cnh.ies.exception.ApiException;
import com.cnh.ies.model.export.ServiceReportExportParams;
import com.cnh.ies.model.report.OrderDetailRow;
import com.cnh.ies.model.report.OrderOverallReport;
import com.cnh.ies.model.report.PaymentRequestDetailRow;
import com.cnh.ies.model.report.PaymentRequestOverallReport;
import com.cnh.ies.model.report.PurchaseOverallReport;
import com.cnh.ies.model.report.StatusBreakdownItem;
import com.cnh.ies.model.report.WarehouseInboundDetailRow;
import com.cnh.ies.model.report.WarehouseInboundOverallReport;
import com.cnh.ies.model.report.WarehouseInventoryDetailRow;
import com.cnh.ies.model.report.WarehouseInventoryOverallReport;
import com.cnh.ies.model.report.WarehouseOutboundDetailRow;
import com.cnh.ies.model.report.WarehouseOutboundOverallReport;
import com.cnh.ies.service.export.ExcelExportService;
import com.cnh.ies.service.export.ExportJobType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceReportExcelExportService {

    private static final DateTimeFormatter INSTANT_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ServiceReportService serviceReportService;

    public ExcelExportService.ExportWorkbookResult export(String type, ServiceReportExportParams params, String requestId) {
        return switch (type) {
            case ExportJobType.SERVICE_WAREHOUSE_INBOUND_OVERALL -> exportInboundOverall(params, requestId);
            case ExportJobType.SERVICE_WAREHOUSE_INBOUND_DETAIL -> exportInboundDetail(params, requestId);
            case ExportJobType.SERVICE_WAREHOUSE_OUTBOUND_OVERALL -> exportOutboundOverall(params, requestId);
            case ExportJobType.SERVICE_WAREHOUSE_OUTBOUND_DETAIL -> exportOutboundDetail(params, requestId);
            case ExportJobType.SERVICE_WAREHOUSE_INVENTORY_OVERALL -> exportInventoryOverall(params, requestId);
            case ExportJobType.SERVICE_WAREHOUSE_INVENTORY_DETAIL -> exportInventoryDetail(params, requestId);
            case ExportJobType.SERVICE_ORDER_OVERALL -> exportOrderOverall(params, requestId);
            case ExportJobType.SERVICE_ORDER_DETAIL -> exportOrderDetail(params, requestId);
            case ExportJobType.SERVICE_PURCHASE_OVERALL -> exportPurchaseOverall(params, requestId);
            case ExportJobType.SERVICE_PAYMENT_REQUEST_OVERALL -> exportPaymentOverall(params, requestId);
            case ExportJobType.SERVICE_PAYMENT_REQUEST_DETAIL -> exportPaymentDetail(params, requestId);
            default -> throw new ApiException(ApiException.ErrorCode.BAD_REQUEST,
                    "Unsupported service report export type: " + type,
                    HttpStatus.BAD_REQUEST.value(), requestId);
        };
    }

    private ExcelExportService.ExportWorkbookResult exportInboundOverall(
            ServiceReportExportParams params, String requestId) {
        var req = ServiceReportExportRequestFactory.inbound(params);
        WarehouseInboundOverallReport report = serviceReportService.warehouseInboundOverall(req, requestId);
        byte[] bytes = buildWorkbook("Inbound Overall", new String[] { "Metric", "Value" }, (sheet, header) -> {
            int row = 1;
            row = summaryRow(sheet, row, "From Date", formatDate(report.getDateRange().getFromDate()));
            row = summaryRow(sheet, row, "To Date", formatDate(report.getDateRange().getToDate()));
            row = summaryRow(sheet, row, "Receipt Count", String.valueOf(report.getReceiptCount()));
            row = summaryRow(sheet, row, "Total Fee Amount", decimal(report.getTotalFeeAmount()));
            row = summaryRow(sheet, row, "Total Real Bill Amount", decimal(report.getTotalRealBillAmount()));
            row = summaryRow(sheet, row, "Total Bill On Paper Amount", decimal(report.getTotalBillOnPaperAmount()));
            row = summaryRow(sheet, row, "Total Expected Qty", decimal(report.getTotalExpectedQuantity()));
            row = summaryRow(sheet, row, "Total Received Qty", decimal(report.getTotalReceivedQuantity()));
            writeStatusBreakdown(sheet, row + 1, report.getStatusBreakdown());
        }, requestId);
        return new ExcelExportService.ExportWorkbookResult(bytes, fileName(ExportJobType.SERVICE_WAREHOUSE_INBOUND_OVERALL));
    }

    private ExcelExportService.ExportWorkbookResult exportInboundDetail(
            ServiceReportExportParams params, String requestId) {
        var req = ServiceReportExportRequestFactory.inbound(params);
        List<WarehouseInboundDetailRow> rows = serviceReportService.warehouseInboundDetailForExport(req);
        String[] headers = {
                "Receipt Number", "Received Date", "Status", "Payment Request", "Vendor Code", "Vendor Name",
                "Product Code", "Product Name", "Qty Expected", "Qty Received", "Fee", "Real Bill", "Bill On Paper",
                "Created By"
        };
        byte[] bytes = buildWorkbook("Inbound Detail", headers, (sheet, header) -> {
            int rowIdx = 1;
            for (WarehouseInboundDetailRow r : rows) {
                Row row = sheet.createRow(rowIdx++);
                int c = 0;
                setCell(row, c++, r.getReceiptNumber());
                setCell(row, c++, formatDate(r.getReceivedDate()));
                setCell(row, c++, r.getStatus());
                setCell(row, c++, r.getPaymentRequestNumber());
                setCell(row, c++, r.getVendorCode());
                setCell(row, c++, r.getVendorName());
                setCell(row, c++, r.getProductCode());
                setCell(row, c++, r.getProductName());
                setCell(row, c++, decimal(r.getQuantityExpected()));
                setCell(row, c++, decimal(r.getQuantityReceived()));
                setCell(row, c++, decimal(r.getFeeAmount()));
                setCell(row, c++, decimal(r.getRealBillAmount()));
                setCell(row, c++, decimal(r.getBillOnPaperAmount()));
                setCell(row, c, r.getCreatedBy());
            }
        }, requestId);
        return new ExcelExportService.ExportWorkbookResult(bytes, fileName(ExportJobType.SERVICE_WAREHOUSE_INBOUND_DETAIL));
    }

    private ExcelExportService.ExportWorkbookResult exportOutboundOverall(
            ServiceReportExportParams params, String requestId) {
        var req = ServiceReportExportRequestFactory.outbound(params);
        WarehouseOutboundOverallReport report = serviceReportService.warehouseOutboundOverall(req, requestId);
        byte[] bytes = buildWorkbook("Outbound Overall", new String[] { "Metric", "Value" }, (sheet, header) -> {
            int row = 1;
            row = summaryRow(sheet, row, "From Date", formatDate(report.getDateRange().getFromDate()));
            row = summaryRow(sheet, row, "To Date", formatDate(report.getDateRange().getToDate()));
            row = summaryRow(sheet, row, "Outbound Count", String.valueOf(report.getOutboundCount()));
            row = summaryRow(sheet, row, "Total Amount", decimal(report.getTotalAmount()));
            row = summaryRow(sheet, row, "Total Tax Amount", decimal(report.getTotalTaxAmount()));
            row = summaryRow(sheet, row, "Total Quantity", decimal(report.getTotalQuantity()));
            writeStatusBreakdown(sheet, row + 1, report.getStatusBreakdown());
        }, requestId);
        return new ExcelExportService.ExportWorkbookResult(bytes, fileName(ExportJobType.SERVICE_WAREHOUSE_OUTBOUND_OVERALL));
    }

    private ExcelExportService.ExportWorkbookResult exportOutboundDetail(
            ServiceReportExportParams params, String requestId) {
        var req = ServiceReportExportRequestFactory.outbound(params);
        List<WarehouseOutboundDetailRow> rows = serviceReportService.warehouseOutboundDetailForExport(req);
        String[] headers = {
                "Outbound Number", "Outbound Date", "Status", "Contract", "Order Number",
                "Product Code", "Product Name", "Quantity", "Unit Price", "Total Amount", "Tax Amount", "Created By"
        };
        byte[] bytes = buildWorkbook("Outbound Detail", headers, (sheet, header) -> {
            int rowIdx = 1;
            for (WarehouseOutboundDetailRow r : rows) {
                Row row = sheet.createRow(rowIdx++);
                int c = 0;
                setCell(row, c++, r.getOutboundNumber());
                setCell(row, c++, formatDate(r.getOutboundDate()));
                setCell(row, c++, r.getStatus());
                setCell(row, c++, r.getContractNumber());
                setCell(row, c++, r.getOrderNumber());
                setCell(row, c++, r.getProductCode());
                setCell(row, c++, r.getProductName());
                setCell(row, c++, decimal(r.getQuantity()));
                setCell(row, c++, decimal(r.getUnitPrice()));
                setCell(row, c++, decimal(r.getTotalAmount()));
                setCell(row, c++, decimal(r.getTaxAmount()));
                setCell(row, c, r.getCreatedBy());
            }
        }, requestId);
        return new ExcelExportService.ExportWorkbookResult(bytes, fileName(ExportJobType.SERVICE_WAREHOUSE_OUTBOUND_DETAIL));
    }

    private ExcelExportService.ExportWorkbookResult exportInventoryOverall(
            ServiceReportExportParams params, String requestId) {
        var req = ServiceReportExportRequestFactory.inventory(params);
        WarehouseInventoryOverallReport report = serviceReportService.warehouseInventoryOverall(req, requestId);
        byte[] bytes = buildWorkbook("Inventory Overall", new String[] { "Metric", "Value" }, (sheet, header) -> {
            int row = 1;
            row = summaryRow(sheet, row, "From Date", formatDate(report.getDateRange().getFromDate()));
            row = summaryRow(sheet, row, "To Date", formatDate(report.getDateRange().getToDate()));
            row = summaryRow(sheet, row, "Product Count", String.valueOf(report.getProductCount()));
            row = summaryRow(sheet, row, "Total Qty On Hand", decimal(report.getTotalQuantityOnHand()));
            row = summaryRow(sheet, row, "Inbound Movement Qty", decimal(report.getInboundMovementQuantity()));
            row = summaryRow(sheet, row, "Outbound Movement Qty", decimal(report.getOutboundMovementQuantity()));
            row = summaryRow(sheet, row, "Net Movement Qty", decimal(report.getNetMovementQuantity()));
            summaryRow(sheet, row, "Movement Count", String.valueOf(report.getMovementCount()));
        }, requestId);
        return new ExcelExportService.ExportWorkbookResult(bytes, fileName(ExportJobType.SERVICE_WAREHOUSE_INVENTORY_OVERALL));
    }

    private ExcelExportService.ExportWorkbookResult exportInventoryDetail(
            ServiceReportExportParams params, String requestId) {
        var req = ServiceReportExportRequestFactory.inventory(params);
        List<WarehouseInventoryDetailRow> rows = serviceReportService.warehouseInventoryDetailForExport(req);
        String[] headers = {
                "Product Code", "Product Name", "Direction", "Quantity", "Reference Type",
                "Reference Id", "Created At", "Note", "Created By"
        };
        byte[] bytes = buildWorkbook("Inventory Detail", headers, (sheet, header) -> {
            int rowIdx = 1;
            for (WarehouseInventoryDetailRow r : rows) {
                Row row = sheet.createRow(rowIdx++);
                int c = 0;
                setCell(row, c++, r.getProductCode());
                setCell(row, c++, r.getProductName());
                setCell(row, c++, r.getDirection());
                setCell(row, c++, decimal(r.getQuantity()));
                setCell(row, c++, r.getReferenceType());
                setCell(row, c++, r.getReferenceId());
                setCell(row, c++, formatInstant(r.getCreatedAt()));
                setCell(row, c++, r.getNote());
                setCell(row, c, r.getCreatedBy());
            }
        }, requestId);
        return new ExcelExportService.ExportWorkbookResult(bytes, fileName(ExportJobType.SERVICE_WAREHOUSE_INVENTORY_DETAIL));
    }

    private ExcelExportService.ExportWorkbookResult exportOrderOverall(
            ServiceReportExportParams params, String requestId) {
        var req = ServiceReportExportRequestFactory.order(params);
        OrderOverallReport report = serviceReportService.orderOverall(req, requestId);
        byte[] bytes = buildWorkbook("Order Overall", new String[] { "Metric", "Value" }, (sheet, header) -> {
            int row = 1;
            row = summaryRow(sheet, row, "From Date", formatDate(report.getDateRange().getFromDate()));
            row = summaryRow(sheet, row, "To Date", formatDate(report.getDateRange().getToDate()));
            row = summaryRow(sheet, row, "Order Count", String.valueOf(report.getOrderCount()));
            row = summaryRow(sheet, row, "Total Amount", decimal(report.getTotalAmount()));
            row = summaryRow(sheet, row, "Total Discount", decimal(report.getTotalDiscountAmount()));
            row = summaryRow(sheet, row, "Total Tax", decimal(report.getTotalTaxAmount()));
            row = summaryRow(sheet, row, "Total Final Amount", decimal(report.getTotalFinalAmount()));
            row = summaryRow(sheet, row, "Total Ordered Qty", decimal(report.getTotalOrderedQuantity()));
            writeStatusBreakdown(sheet, row + 1, report.getStatusBreakdown());
        }, requestId);
        return new ExcelExportService.ExportWorkbookResult(bytes, fileName(ExportJobType.SERVICE_ORDER_OVERALL));
    }

    private ExcelExportService.ExportWorkbookResult exportOrderDetail(
            ServiceReportExportParams params, String requestId) {
        var req = ServiceReportExportRequestFactory.order(params);
        List<OrderDetailRow> rows = serviceReportService.orderDetailForExport(req);
        String[] headers = {
                "Order Number", "Order Date", "Status", "Customer", "Contract",
                "Product Code", "Product Name", "Vendor Code", "Quantity", "Unit Price",
                "Discount", "Tax", "Total Amount", "Created By"
        };
        byte[] bytes = buildWorkbook("Order Detail", headers, (sheet, header) -> {
            int rowIdx = 1;
            for (OrderDetailRow r : rows) {
                Row row = sheet.createRow(rowIdx++);
                int c = 0;
                setCell(row, c++, r.getOrderNumber());
                setCell(row, c++, formatDate(r.getOrderDate()));
                setCell(row, c++, r.getStatus());
                setCell(row, c++, r.getCustomerName());
                setCell(row, c++, r.getContractNumber());
                setCell(row, c++, r.getProductCode());
                setCell(row, c++, r.getProductName());
                setCell(row, c++, r.getVendorCode());
                setCell(row, c++, decimal(r.getQuantity()));
                setCell(row, c++, decimal(r.getUnitPrice()));
                setCell(row, c++, decimal(r.getDiscountAmount()));
                setCell(row, c++, decimal(r.getTaxAmount()));
                setCell(row, c++, decimal(r.getTotalAmount()));
                setCell(row, c, r.getCreatedBy());
            }
        }, requestId);
        return new ExcelExportService.ExportWorkbookResult(bytes, fileName(ExportJobType.SERVICE_ORDER_DETAIL));
    }

    private ExcelExportService.ExportWorkbookResult exportPurchaseOverall(
            ServiceReportExportParams params, String requestId) {
        var req = ServiceReportExportRequestFactory.purchase(params);
        PurchaseOverallReport report = serviceReportService.purchaseOverall(req, requestId);
        byte[] bytes = buildWorkbook("Purchase Overall", new String[] { "Metric", "Value" }, (sheet, header) -> {
            int row = 1;
            row = summaryRow(sheet, row, "From Date", formatDate(report.getDateRange().getFromDate()));
            row = summaryRow(sheet, row, "To Date", formatDate(report.getDateRange().getToDate()));
            row = summaryRow(sheet, row, "PO Count", String.valueOf(report.getPurchaseOrderCount()));
            row = summaryRow(sheet, row, "Total Purchased Qty", decimal(report.getTotalPurchasedQuantity()));
            row = summaryRow(sheet, row, "Total Before Tax", decimal(report.getTotalBeforeTax()));
            row = summaryRow(sheet, row, "Total Price", decimal(report.getTotalPrice()));
            row = summaryRow(sheet, row, "Total Price VND", decimal(report.getTotalPriceVnd()));
            row = summaryRow(sheet, row, "Distinct Vendors", String.valueOf(report.getDistinctVendorCount()));
            row = summaryRow(sheet, row, "Distinct Products", String.valueOf(report.getDistinctProductCount()));
            writeStatusBreakdown(sheet, row + 1, report.getStatusBreakdown());
        }, requestId);
        return new ExcelExportService.ExportWorkbookResult(bytes, fileName(ExportJobType.SERVICE_PURCHASE_OVERALL));
    }

    private ExcelExportService.ExportWorkbookResult exportPaymentOverall(
            ServiceReportExportParams params, String requestId) {
        var req = ServiceReportExportRequestFactory.payment(params);
        PaymentRequestOverallReport report = serviceReportService.paymentRequestOverall(req, requestId);
        byte[] bytes = buildWorkbook("Payment Overall", new String[] { "Metric", "Value" }, (sheet, header) -> {
            int row = 1;
            row = summaryRow(sheet, row, "From Date", formatDate(report.getDateRange().getFromDate()));
            row = summaryRow(sheet, row, "To Date", formatDate(report.getDateRange().getToDate()));
            row = summaryRow(sheet, row, "Request Count", String.valueOf(report.getRequestCount()));
            row = summaryRow(sheet, row, "Total Requested", decimal(report.getTotalRequestedAmount()));
            row = summaryRow(sheet, row, "Total Requested VND", decimal(report.getTotalRequestedAmountVnd()));
            row = summaryRow(sheet, row, "Total Fee", decimal(report.getTotalFeeAmount()));
            row = summaryRow(sheet, row, "Total Fee VND", decimal(report.getTotalFeeAmountVnd()));
            row = summaryRow(sheet, row, "Total Amount", decimal(report.getTotalAmount()));
            row = summaryRow(sheet, row, "Total Amount VND", decimal(report.getTotalAmountVnd()));
            row = summaryRow(sheet, row, "Total Paid", decimal(report.getTotalPaidAmount()));
            row = summaryRow(sheet, row, "Total Paid VND", decimal(report.getTotalPaidAmountVnd()));
            row = summaryRow(sheet, row, "Total Unpaid", decimal(report.getTotalUnpaidAmount()));
            row = summaryRow(sheet, row, "Total Unpaid VND", decimal(report.getTotalUnpaidAmountVnd()));
            writeStatusBreakdown(sheet, row + 1, report.getStatusBreakdown());
        }, requestId);
        return new ExcelExportService.ExportWorkbookResult(bytes, fileName(ExportJobType.SERVICE_PAYMENT_REQUEST_OVERALL));
    }

    private ExcelExportService.ExportWorkbookResult exportPaymentDetail(
            ServiceReportExportParams params, String requestId) {
        var req = ServiceReportExportRequestFactory.payment(params);
        List<PaymentRequestDetailRow> rows = serviceReportService.paymentRequestDetailForExport(req);
        String[] headers = {
                "Request Number", "Request Date", "Status", "Vendor Code", "Vendor Name",
                "Product Code", "PO Number", "Requested Amount", "Paid Amount", "Fee Amount",
                "Total Amount", "Approval Level", "Approval Levels", "Created By"
        };
        byte[] bytes = buildWorkbook("Payment Detail", headers, (sheet, header) -> {
            int rowIdx = 1;
            for (PaymentRequestDetailRow r : rows) {
                Row row = sheet.createRow(rowIdx++);
                int c = 0;
                setCell(row, c++, r.getRequestNumber());
                setCell(row, c++, formatInstant(r.getRequestDate()));
                setCell(row, c++, r.getStatus());
                setCell(row, c++, r.getVendorCode());
                setCell(row, c++, r.getVendorName());
                setCell(row, c++, r.getProductCode());
                setCell(row, c++, r.getPurchaseOrderNumber());
                setCell(row, c++, decimal(r.getRequestedAmount()));
                setCell(row, c++, decimal(r.getPaidAmount()));
                setCell(row, c++, decimal(r.getFeeAmount()));
                setCell(row, c++, decimal(r.getTotalAmount()));
                setCell(row, c++, r.getCurrentApprovalLevel() != null ? r.getCurrentApprovalLevel().toString() : "");
                setCell(row, c++, r.getApprovalLevels() != null ? r.getApprovalLevels().toString() : "");
                setCell(row, c, r.getCreatedBy());
            }
        }, requestId);
        return new ExcelExportService.ExportWorkbookResult(bytes, fileName(ExportJobType.SERVICE_PAYMENT_REQUEST_DETAIL));
    }

    private void writeStatusBreakdown(Sheet sheet, int startRow, List<StatusBreakdownItem> breakdown) {
        if (breakdown == null || breakdown.isEmpty()) {
            return;
        }
        Row title = sheet.createRow(startRow);
        setCell(title, 0, "Status Breakdown");
        setCell(title, 1, "Count");
        int row = startRow + 1;
        for (StatusBreakdownItem item : breakdown) {
            Row r = sheet.createRow(row++);
            setCell(r, 0, item.getStatus());
            setCell(r, 1, item.getCount() != null ? item.getCount().toString() : "");
        }
    }

    private int summaryRow(Sheet sheet, int rowIdx, String label, String value) {
        Row row = sheet.createRow(rowIdx);
        setCell(row, 0, label);
        setCell(row, 1, value);
        return rowIdx + 1;
    }

    private byte[] buildWorkbook(String sheetName, String[] headers, SheetWriter writer, String requestId) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(sheetName);
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }
            writer.write(sheet, headerRow);
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            log.error("Failed to build service report workbook: {}", e.getMessage(), e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR,
                    "Failed to generate Excel file: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }

    private static String fileName(String type) {
        String prefix = ExportJobType.fileNamePrefix(type);
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                .withZone(ZoneId.systemDefault())
                .format(Instant.now());
        return prefix + "_export_" + timestamp + ".xlsx";
    }

    private static void setCell(Row row, int col, String value) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value != null ? value : "");
    }

    private static String decimal(BigDecimal value) {
        return value != null ? value.toPlainString() : "";
    }

    private static String formatDate(LocalDate date) {
        return date != null ? DATE_FORMATTER.format(date) : "";
    }

    private static String formatInstant(Instant instant) {
        return instant != null ? INSTANT_FORMATTER.format(instant) : "";
    }

    @FunctionalInterface
    private interface SheetWriter {
        void write(Sheet sheet, Row headerRow);
    }
}
