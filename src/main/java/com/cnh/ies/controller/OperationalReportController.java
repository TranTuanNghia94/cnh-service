package com.cnh.ies.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cnh.ies.dto.common.ApiResponse;
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
import com.cnh.ies.constant.PermissionConstants;
import com.cnh.ies.service.report.OperationalReportExcelService;
import com.cnh.ies.service.report.OperationalReportService;
import com.cnh.ies.util.RequestContext;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('"
        + PermissionConstants.WAREHOUSE_INVENTORY_READ + "','"
        + PermissionConstants.WAREHOUSE_INBOUND_READ + "','"
        + PermissionConstants.WAREHOUSE_OUTBOUND_READ + "','"
        + PermissionConstants.PAYMENT_READ + "')")
public class OperationalReportController {

    private final OperationalReportService reportService;
    private final OperationalReportExcelService excelService;

    @PostMapping("/stock")
    public ApiResponse<OperationalReportPage<StockLedgerRow>> stock(@Valid @RequestBody OperationalReportRequest request) {
        return ApiResponse.success(reportService.stock(request), "Stock report success");
    }

    @PostMapping("/stock/export")
    public ResponseEntity<byte[]> exportStock(@Valid @RequestBody OperationalReportRequest request) {
        return excel(excelService.stock(request, RequestContext.getRequestIdOrGenerate()), "xuat-nhap-ton.xlsx");
    }

    @PostMapping("/payment")
    public ApiResponse<OperationalReportPage<PaymentLedgerRow>> payment(@Valid @RequestBody OperationalReportRequest request) {
        return ApiResponse.success(reportService.payments(request), "Payment report success");
    }

    @PostMapping("/payment/export")
    public ResponseEntity<byte[]> exportPayment(@Valid @RequestBody OperationalReportRequest request) {
        return excel(excelService.payments(request, RequestContext.getRequestIdOrGenerate()), "bao-cao-thanh-toan.xlsx");
    }

    @PostMapping("/vendor-debt")
    public ApiResponse<OperationalReportPage<VendorDebtRow>> vendorDebt(@Valid @RequestBody OperationalReportRequest request) {
        return ApiResponse.success(reportService.vendorDebt(request), "Vendor debt report success");
    }

    @PostMapping("/vendor-debt/export")
    public ResponseEntity<byte[]> exportVendorDebt(@Valid @RequestBody OperationalReportRequest request) {
        return excel(excelService.vendorDebt(request, RequestContext.getRequestIdOrGenerate()), "bao-cao-tong-hop.xlsx");
    }

    @PostMapping("/sales-detail")
    public ApiResponse<OperationalReportPage<SalesDetailReportRow>> salesDetail(@Valid @RequestBody OperationalReportRequest request) {
        return ApiResponse.success(reportService.salesDetail(request), "Sales detail report success");
    }

    @PostMapping("/sales-detail/export")
    public ResponseEntity<byte[]> exportSalesDetail(@Valid @RequestBody OperationalReportRequest request) {
        return excel(excelService.salesDetail(request, RequestContext.getRequestIdOrGenerate()), "bao-cao-chi-tiet.xlsx");
    }

    @PostMapping("/outbound")
    public ApiResponse<OperationalReportPage<OutboundSummaryReportRow>> outbound(@Valid @RequestBody OperationalReportRequest request) {
        return ApiResponse.success(reportService.outboundSummary(request), "Outbound report success");
    }

    @PostMapping("/outbound/export")
    public ResponseEntity<byte[]> exportOutbound(@Valid @RequestBody OperationalReportRequest request) {
        return excel(excelService.outboundSummary(request, RequestContext.getRequestIdOrGenerate()), "bao-cao-xuat-kho.xlsx");
    }

    @PostMapping("/outbound-detail")
    public ApiResponse<OperationalReportPage<OutboundDetailReportRow>> outboundDetail(@Valid @RequestBody OperationalReportRequest request) {
        return ApiResponse.success(reportService.outboundDetail(request), "Outbound detail report success");
    }

    @PostMapping("/outbound-detail/export")
    public ResponseEntity<byte[]> exportOutboundDetail(@Valid @RequestBody OperationalReportRequest request) {
        return excel(excelService.outboundDetail(request, RequestContext.getRequestIdOrGenerate()), "bao-cao-chi-tiet-xuat-kho.xlsx");
    }

    @PostMapping("/inbound")
    public ApiResponse<OperationalReportPage<InboundSummaryReportRow>> inbound(@Valid @RequestBody OperationalReportRequest request) {
        return ApiResponse.success(reportService.inboundSummary(request), "Inbound report success");
    }

    @PostMapping("/inbound/export")
    public ResponseEntity<byte[]> exportInbound(@Valid @RequestBody OperationalReportRequest request) {
        return excel(excelService.inboundSummary(request, RequestContext.getRequestIdOrGenerate()), "bao-cao-nhap-kho.xlsx");
    }

    @PostMapping("/inbound-detail")
    public ApiResponse<OperationalReportPage<InboundDetailReportRow>> inboundDetail(@Valid @RequestBody OperationalReportRequest request) {
        return ApiResponse.success(reportService.inboundDetail(request), "Inbound detail report success");
    }

    @PostMapping("/inbound-detail/export")
    public ResponseEntity<byte[]> exportInboundDetail(@Valid @RequestBody OperationalReportRequest request) {
        return excel(excelService.inboundDetail(request, RequestContext.getRequestIdOrGenerate()), "bao-cao-chi-tiet-nhap-kho.xlsx");
    }

    private ResponseEntity<byte[]> excel(byte[] bytes, String fileName) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }
}
