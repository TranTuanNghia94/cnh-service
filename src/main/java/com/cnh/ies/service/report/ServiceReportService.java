package com.cnh.ies.service.report;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.cnh.ies.model.general.ListDataModel;
import com.cnh.ies.model.report.OrderDetailRow;
import com.cnh.ies.model.report.OrderOverallReport;
import com.cnh.ies.model.report.OrderReportRequest;
import com.cnh.ies.model.report.PaymentRequestDetailRow;
import com.cnh.ies.model.report.PaymentRequestOverallReport;
import com.cnh.ies.model.report.PaymentRequestReportRequest;
import com.cnh.ies.model.report.PurchaseOverallReport;
import com.cnh.ies.model.report.PurchaseReportRequest;
import com.cnh.ies.model.report.ServiceReportDetailResponse;
import com.cnh.ies.model.report.WarehouseInboundDetailRow;
import com.cnh.ies.model.report.WarehouseInboundOverallReport;
import com.cnh.ies.model.report.WarehouseInboundReportRequest;
import com.cnh.ies.model.report.WarehouseInventoryDetailRow;
import com.cnh.ies.model.report.WarehouseInventoryOverallReport;
import com.cnh.ies.model.report.WarehouseInventoryReportRequest;
import com.cnh.ies.model.report.WarehouseOutboundDetailRow;
import com.cnh.ies.model.report.WarehouseOutboundOverallReport;
import com.cnh.ies.model.report.WarehouseOutboundReportRequest;
import com.cnh.ies.repository.report.ServiceReportRepository;
import com.cnh.ies.repository.report.ServiceReportRepository.StockMovementTotals;
import com.cnh.ies.repository.report.projection.OrderDetailProjection;
import com.cnh.ies.repository.report.projection.PaymentRequestDetailProjection;
import com.cnh.ies.repository.report.projection.WarehouseInboundDetailProjection;
import com.cnh.ies.repository.report.projection.WarehouseInventoryDetailProjection;
import com.cnh.ies.repository.report.projection.WarehouseOutboundDetailProjection;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ServiceReportService {

    private static final int EXPORT_MAX_ROWS = 50_000;

    private final ServiceReportRepository reportRepository;

    public WarehouseInboundOverallReport warehouseInboundOverall(
            WarehouseInboundReportRequest request, String requestId) {
        ServiceReportSupport.validateDateRange(request.getFromDate(), request.getToDate(), requestId);
        BigDecimal[] money = reportRepository.inboundHeaderMoneyTotals(
                request.getFromDate(), request.getToDate(),
                request.getCreatedBy(), request.getInboundNumber(), request.getContractNumber(),
                request.getCustomerName(), request.getOrderNumber(), request.getStatus());
        BigDecimal[] qty = reportRepository.inboundLineQuantities(
                request.getFromDate(), request.getToDate(),
                request.getCreatedBy(), request.getInboundNumber(), request.getContractNumber(),
                request.getCustomerName(), request.getOrderNumber(), request.getStatus());
        return WarehouseInboundOverallReport.builder()
                .dateRange(ServiceReportSupport.dateRange(request.getFromDate(), request.getToDate()))
                .receiptCount(reportRepository.countInboundReceipts(
                        request.getFromDate(), request.getToDate(),
                        request.getCreatedBy(), request.getInboundNumber(), request.getContractNumber(),
                        request.getCustomerName(), request.getOrderNumber(), request.getStatus()))
                .statusBreakdown(ServiceReportSupport.toStatusBreakdown(
                        reportRepository.inboundStatusBreakdown(
                                request.getFromDate(), request.getToDate(),
                                request.getCreatedBy(), request.getInboundNumber(), request.getContractNumber(),
                                request.getCustomerName(), request.getOrderNumber(), request.getStatus())))
                .totalFeeAmount(money[0])
                .totalRealBillAmount(money[1])
                .totalBillOnPaperAmount(money[2])
                .totalExpectedQuantity(qty[0])
                .totalReceivedQuantity(qty[1])
                .build();
    }

    public ServiceReportDetailResponse<WarehouseInboundDetailRow, WarehouseInboundOverallReport> warehouseInboundDetail(
            WarehouseInboundReportRequest request, String requestId) {
        WarehouseInboundOverallReport summary = warehouseInboundOverall(request, requestId);
        Pageable pageable = ServiceReportSupport.pageable(request.getPage(), request.getLimit());
        long total = reportRepository.countInboundDetail(
                request.getFromDate(), request.getToDate(),
                request.getCreatedBy(), request.getInboundNumber(), request.getContractNumber(),
                request.getCustomerName(), request.getOrderNumber(), request.getStatus());
        List<WarehouseInboundDetailRow> rows = reportRepository.findInboundDetail(
                        request.getFromDate(), request.getToDate(),
                        request.getCreatedBy(), request.getInboundNumber(), request.getContractNumber(),
                        request.getCustomerName(), request.getOrderNumber(), request.getStatus(), pageable)
                .stream()
                .map(this::toInboundDetailRow)
                .collect(Collectors.toList());
        return ServiceReportDetailResponse.<WarehouseInboundDetailRow, WarehouseInboundOverallReport>builder()
                .dateRange(summary.getDateRange())
                .summary(summary)
                .rows(ServiceReportSupport.pagedList(rows, request.getPage(), request.getLimit(), total))
                .build();
    }

    public List<WarehouseInboundDetailRow> warehouseInboundDetailForExport(WarehouseInboundReportRequest request) {
        Pageable pageable = ServiceReportSupport.pageable(0, EXPORT_MAX_ROWS);
        return reportRepository.findInboundDetail(
                        request.getFromDate(), request.getToDate(),
                        request.getCreatedBy(), request.getInboundNumber(), request.getContractNumber(),
                        request.getCustomerName(), request.getOrderNumber(), request.getStatus(), pageable)
                .stream()
                .map(this::toInboundDetailRow)
                .collect(Collectors.toList());
    }

    public WarehouseOutboundOverallReport warehouseOutboundOverall(
            WarehouseOutboundReportRequest request, String requestId) {
        ServiceReportSupport.validateDateRange(request.getFromDate(), request.getToDate(), requestId);
        BigDecimal[] header = reportRepository.outboundHeaderTotals(
                request.getFromDate(), request.getToDate(),
                request.getCreatedBy(), request.getOutboundNumber(), request.getContractNumber(), request.getStatus());
        return WarehouseOutboundOverallReport.builder()
                .dateRange(ServiceReportSupport.dateRange(request.getFromDate(), request.getToDate()))
                .outboundCount(reportRepository.countOutbound(
                        request.getFromDate(), request.getToDate(),
                        request.getCreatedBy(), request.getOutboundNumber(), request.getContractNumber(), request.getStatus()))
                .statusBreakdown(ServiceReportSupport.toStatusBreakdown(
                        reportRepository.outboundStatusBreakdown(
                                request.getFromDate(), request.getToDate(),
                                request.getCreatedBy(), request.getOutboundNumber(), request.getContractNumber(),
                                request.getStatus())))
                .totalAmount(header[0])
                .totalTaxAmount(header[1])
                .totalQuantity(reportRepository.outboundDetailQuantityTotal(
                        request.getFromDate(), request.getToDate(),
                        request.getCreatedBy(), request.getOutboundNumber(), request.getContractNumber(), request.getStatus()))
                .build();
    }

    public ServiceReportDetailResponse<WarehouseOutboundDetailRow, WarehouseOutboundOverallReport> warehouseOutboundDetail(
            WarehouseOutboundReportRequest request, String requestId) {
        WarehouseOutboundOverallReport summary = warehouseOutboundOverall(request, requestId);
        Pageable pageable = ServiceReportSupport.pageable(request.getPage(), request.getLimit());
        long total = reportRepository.countOutboundDetail(
                request.getFromDate(), request.getToDate(),
                request.getCreatedBy(), request.getOutboundNumber(), request.getContractNumber(), request.getStatus());
        List<WarehouseOutboundDetailRow> rows = reportRepository.findOutboundDetail(
                        request.getFromDate(), request.getToDate(),
                        request.getCreatedBy(), request.getOutboundNumber(), request.getContractNumber(),
                        request.getStatus(), pageable)
                .stream()
                .map(this::toOutboundDetailRow)
                .collect(Collectors.toList());
        return ServiceReportDetailResponse.<WarehouseOutboundDetailRow, WarehouseOutboundOverallReport>builder()
                .dateRange(summary.getDateRange())
                .summary(summary)
                .rows(ServiceReportSupport.pagedList(rows, request.getPage(), request.getLimit(), total))
                .build();
    }

    public List<WarehouseOutboundDetailRow> warehouseOutboundDetailForExport(WarehouseOutboundReportRequest request) {
        Pageable pageable = ServiceReportSupport.pageable(0, EXPORT_MAX_ROWS);
        return reportRepository.findOutboundDetail(
                        request.getFromDate(), request.getToDate(),
                        request.getCreatedBy(), request.getOutboundNumber(), request.getContractNumber(),
                        request.getStatus(), pageable)
                .stream()
                .map(this::toOutboundDetailRow)
                .collect(Collectors.toList());
    }

    public WarehouseInventoryOverallReport warehouseInventoryOverall(
            WarehouseInventoryReportRequest request, String requestId) {
        ServiceReportSupport.validateDateRange(request.getFromDate(), request.getToDate(), requestId);
        StockMovementTotals movement = reportRepository.stockMovementTotals(
                ServiceReportSupport.startOfDay(request.getFromDate()),
                ServiceReportSupport.endExclusive(request.getToDate()),
                request.getProductCode(), request.getProductName(), request.getProductCategory(), request.getDirection());
        BigDecimal inbound = movement.inboundQty();
        BigDecimal outbound = movement.outboundQty();
        return WarehouseInventoryOverallReport.builder()
                .dateRange(ServiceReportSupport.dateRange(request.getFromDate(), request.getToDate()))
                .productCount(reportRepository.countInventoryProducts(
                        request.getProductCode(), request.getProductName(), request.getProductCategory()))
                .totalQuantityOnHand(reportRepository.sumQuantityOnHand(
                        request.getProductCode(), request.getProductName(), request.getProductCategory()))
                .inboundMovementQuantity(inbound)
                .outboundMovementQuantity(outbound)
                .netMovementQuantity(inbound.subtract(outbound))
                .movementCount(movement.movementCount())
                .build();
    }

    public ServiceReportDetailResponse<WarehouseInventoryDetailRow, WarehouseInventoryOverallReport> warehouseInventoryDetail(
            WarehouseInventoryReportRequest request, String requestId) {
        WarehouseInventoryOverallReport summary = warehouseInventoryOverall(request, requestId);
        Pageable pageable = ServiceReportSupport.pageable(request.getPage(), request.getLimit());
        var from = ServiceReportSupport.startOfDay(request.getFromDate());
        var toEx = ServiceReportSupport.endExclusive(request.getToDate());
        long total = reportRepository.countInventoryDetail(from, toEx,
                request.getProductCode(), request.getProductName(), request.getProductCategory(), request.getDirection());
        List<WarehouseInventoryDetailRow> rows = reportRepository.findInventoryDetail(
                        from, toEx, request.getProductCode(), request.getProductName(),
                        request.getProductCategory(), request.getDirection(), pageable)
                .stream()
                .map(this::toInventoryDetailRow)
                .collect(Collectors.toList());
        return ServiceReportDetailResponse.<WarehouseInventoryDetailRow, WarehouseInventoryOverallReport>builder()
                .dateRange(summary.getDateRange())
                .summary(summary)
                .rows(ServiceReportSupport.pagedList(rows, request.getPage(), request.getLimit(), total))
                .build();
    }

    public List<WarehouseInventoryDetailRow> warehouseInventoryDetailForExport(WarehouseInventoryReportRequest request) {
        Pageable pageable = ServiceReportSupport.pageable(0, EXPORT_MAX_ROWS);
        var from = ServiceReportSupport.startOfDay(request.getFromDate());
        var toEx = ServiceReportSupport.endExclusive(request.getToDate());
        return reportRepository.findInventoryDetail(
                        from, toEx, request.getProductCode(), request.getProductName(),
                        request.getProductCategory(), request.getDirection(), pageable)
                .stream()
                .map(this::toInventoryDetailRow)
                .collect(Collectors.toList());
    }

    public OrderOverallReport orderOverall(OrderReportRequest request, String requestId) {
        ServiceReportSupport.validateDateRange(request.getFromDate(), request.getToDate(), requestId);
        var fromInstant = ServiceReportSupport.startOfDay(request.getFromDate());
        var toExclusive = ServiceReportSupport.endExclusive(request.getToDate());
        BigDecimal[] header = reportRepository.orderHeaderTotals(
                request.getFromDate(), request.getToDate(), fromInstant, toExclusive,
                request.getCreatedBy(), request.getContractNumber(), request.getOrderNumber(),
                request.getStatus(), request.getCustomerName());
        return OrderOverallReport.builder()
                .dateRange(ServiceReportSupport.dateRange(request.getFromDate(), request.getToDate()))
                .orderCount(reportRepository.countOrders(
                        request.getFromDate(), request.getToDate(), fromInstant, toExclusive,
                        request.getCreatedBy(), request.getContractNumber(), request.getOrderNumber(),
                        request.getStatus(), request.getCustomerName()))
                .statusBreakdown(ServiceReportSupport.toStatusBreakdown(
                        reportRepository.orderStatusBreakdown(
                                request.getFromDate(), request.getToDate(), fromInstant, toExclusive,
                                request.getCreatedBy(), request.getContractNumber(), request.getOrderNumber(),
                                request.getStatus(), request.getCustomerName())))
                .totalAmount(header[0])
                .totalDiscountAmount(header[1])
                .totalTaxAmount(header[2])
                .totalFinalAmount(header[3])
                .totalOrderedQuantity(reportRepository.orderLineQuantityTotal(
                        request.getFromDate(), request.getToDate(), fromInstant, toExclusive,
                        request.getCreatedBy(), request.getContractNumber(), request.getOrderNumber(),
                        request.getStatus(), request.getCustomerName()))
                .build();
    }

    public ServiceReportDetailResponse<OrderDetailRow, OrderOverallReport> orderDetail(
            OrderReportRequest request, String requestId) {
        OrderOverallReport summary = orderOverall(request, requestId);
        Pageable pageable = ServiceReportSupport.pageable(request.getPage(), request.getLimit());
        var fromInstant = ServiceReportSupport.startOfDay(request.getFromDate());
        var toExclusive = ServiceReportSupport.endExclusive(request.getToDate());
        long total = reportRepository.countOrderDetail(
                request.getFromDate(), request.getToDate(), fromInstant, toExclusive,
                request.getCreatedBy(), request.getContractNumber(), request.getOrderNumber(),
                request.getStatus(), request.getCustomerName());
        List<OrderDetailRow> rows = reportRepository.findOrderDetail(
                        request.getFromDate(), request.getToDate(), fromInstant, toExclusive,
                        request.getCreatedBy(), request.getContractNumber(), request.getOrderNumber(),
                        request.getStatus(), request.getCustomerName(), pageable)
                .stream()
                .map(this::toOrderDetailRow)
                .collect(Collectors.toList());
        return ServiceReportDetailResponse.<OrderDetailRow, OrderOverallReport>builder()
                .dateRange(summary.getDateRange())
                .summary(summary)
                .rows(ServiceReportSupport.pagedList(rows, request.getPage(), request.getLimit(), total))
                .build();
    }

    public List<OrderDetailRow> orderDetailForExport(OrderReportRequest request) {
        Pageable pageable = ServiceReportSupport.pageable(0, EXPORT_MAX_ROWS);
        var fromInstant = ServiceReportSupport.startOfDay(request.getFromDate());
        var toExclusive = ServiceReportSupport.endExclusive(request.getToDate());
        return reportRepository.findOrderDetail(
                        request.getFromDate(), request.getToDate(), fromInstant, toExclusive,
                        request.getCreatedBy(), request.getContractNumber(), request.getOrderNumber(),
                        request.getStatus(), request.getCustomerName(), pageable)
                .stream()
                .map(this::toOrderDetailRow)
                .collect(Collectors.toList());
    }

    public PurchaseOverallReport purchaseOverall(PurchaseReportRequest request, String requestId) {
        ServiceReportSupport.validateDateRange(request.getFromDate(), request.getToDate(), requestId);
        BigDecimal[] line = reportRepository.purchaseLineTotals(
                request.getFromDate(), request.getToDate(),
                request.getPurchaseOrderNumber(), request.getContractNumber(),
                request.getCreatedBy(), request.getCustomerName(), request.getStatus());
        return PurchaseOverallReport.builder()
                .dateRange(ServiceReportSupport.dateRange(request.getFromDate(), request.getToDate()))
                .purchaseOrderCount(reportRepository.countPurchaseOrders(
                        request.getFromDate(), request.getToDate(),
                        request.getPurchaseOrderNumber(), request.getContractNumber(),
                        request.getCreatedBy(), request.getCustomerName(), request.getStatus()))
                .statusBreakdown(ServiceReportSupport.toStatusBreakdown(
                        reportRepository.purchaseStatusBreakdown(
                                request.getFromDate(), request.getToDate(),
                                request.getPurchaseOrderNumber(), request.getContractNumber(),
                                request.getCreatedBy(), request.getCustomerName(), request.getStatus())))
                .totalPurchasedQuantity(line[0])
                .totalBeforeTax(line[1])
                .totalPrice(line[2])
                .totalPriceVnd(line[3])
                .distinctVendorCount(line[4].longValue())
                .distinctProductCount(line[5].longValue())
                .build();
    }

    public PaymentRequestOverallReport paymentRequestOverall(
            PaymentRequestReportRequest request, String requestId) {
        ServiceReportSupport.validateDateRange(request.getFromDate(), request.getToDate(), requestId);
        var from = ServiceReportSupport.startOfDay(request.getFromDate());
        var toEx = ServiceReportSupport.endExclusive(request.getToDate());
        BigDecimal[] totals = reportRepository.paymentHeaderTotals(from, toEx,
                request.getCreatedBy(), request.getPaymentRequestNumber(), request.getVendorCode(), request.getStatus());
        return PaymentRequestOverallReport.builder()
                .dateRange(ServiceReportSupport.dateRange(request.getFromDate(), request.getToDate()))
                .requestCount(reportRepository.countPaymentRequests(from, toEx,
                        request.getCreatedBy(), request.getPaymentRequestNumber(), request.getVendorCode(), request.getStatus()))
                .statusBreakdown(ServiceReportSupport.toStatusBreakdown(
                        reportRepository.paymentStatusBreakdown(from, toEx,
                                request.getCreatedBy(), request.getPaymentRequestNumber(), request.getVendorCode(),
                                request.getStatus())))
                .totalRequestedAmount(totals[0])
                .totalRequestedAmountVnd(totals[1])
                .totalFeeAmount(totals[2])
                .totalFeeAmountVnd(totals[3])
                .totalAmount(totals[4])
                .totalAmountVnd(totals[5])
                .totalPaidAmount(totals[6])
                .totalPaidAmountVnd(totals[7])
                .totalUnpaidAmount(totals[4].subtract(totals[6]))
                .totalUnpaidAmountVnd(totals[5].subtract(totals[7]))
                .build();
    }

    public ServiceReportDetailResponse<PaymentRequestDetailRow, PaymentRequestOverallReport> paymentRequestDetail(
            PaymentRequestReportRequest request, String requestId) {
        PaymentRequestOverallReport summary = paymentRequestOverall(request, requestId);
        Pageable pageable = ServiceReportSupport.pageable(request.getPage(), request.getLimit());
        var from = ServiceReportSupport.startOfDay(request.getFromDate());
        var toEx = ServiceReportSupport.endExclusive(request.getToDate());
        long total = reportRepository.countPaymentDetail(from, toEx,
                request.getCreatedBy(), request.getPaymentRequestNumber(), request.getVendorCode(), request.getStatus());
        List<PaymentRequestDetailRow> rows = reportRepository.findPaymentDetail(
                        from, toEx, request.getCreatedBy(), request.getPaymentRequestNumber(),
                        request.getVendorCode(), request.getStatus(), pageable)
                .stream()
                .map(this::toPaymentDetailRow)
                .collect(Collectors.toList());
        return ServiceReportDetailResponse.<PaymentRequestDetailRow, PaymentRequestOverallReport>builder()
                .dateRange(summary.getDateRange())
                .summary(summary)
                .rows(ServiceReportSupport.pagedList(rows, request.getPage(), request.getLimit(), total))
                .build();
    }

    public List<PaymentRequestDetailRow> paymentRequestDetailForExport(PaymentRequestReportRequest request) {
        Pageable pageable = ServiceReportSupport.pageable(0, EXPORT_MAX_ROWS);
        var from = ServiceReportSupport.startOfDay(request.getFromDate());
        var toEx = ServiceReportSupport.endExclusive(request.getToDate());
        return reportRepository.findPaymentDetail(
                        from, toEx, request.getCreatedBy(), request.getPaymentRequestNumber(),
                        request.getVendorCode(), request.getStatus(), pageable)
                .stream()
                .map(this::toPaymentDetailRow)
                .collect(Collectors.toList());
    }

    private WarehouseInboundDetailRow toInboundDetailRow(WarehouseInboundDetailProjection p) {
        return WarehouseInboundDetailRow.builder()
                .receiptId(p.getReceiptId() != null ? p.getReceiptId().toString() : null)
                .receiptNumber(p.getReceiptNumber())
                .receivedDate(p.getReceivedDate())
                .status(p.getStatus())
                .paymentRequestNumber(p.getPaymentRequestNumber())
                .vendorCode(p.getVendorCode())
                .vendorName(p.getVendorName())
                .productCode(p.getProductCode())
                .productName(p.getProductName())
                .quantityExpected(p.getQuantityExpected())
                .quantityReceived(p.getQuantityReceived())
                .feeAmount(p.getFeeAmount())
                .realBillAmount(p.getRealBillAmount())
                .billOnPaperAmount(p.getBillOnPaperAmount())
                .createdBy(p.getCreatedBy())
                .build();
    }

    private WarehouseOutboundDetailRow toOutboundDetailRow(WarehouseOutboundDetailProjection p) {
        return WarehouseOutboundDetailRow.builder()
                .outboundId(p.getOutboundId() != null ? p.getOutboundId().toString() : null)
                .outboundNumber(p.getOutboundNumber())
                .outboundDate(p.getOutboundDate())
                .status(p.getStatus())
                .contractNumber(p.getContractNumber())
                .orderNumber(p.getOrderNumber())
                .productCode(p.getProductCode())
                .productName(p.getProductName())
                .quantity(p.getQuantity())
                .unitPrice(p.getUnitPrice())
                .totalAmount(p.getTotalAmount())
                .taxAmount(p.getTaxAmount())
                .createdBy(p.getCreatedBy())
                .build();
    }

    private WarehouseInventoryDetailRow toInventoryDetailRow(WarehouseInventoryDetailProjection p) {
        return WarehouseInventoryDetailRow.builder()
                .transactionId(p.getTransactionId() != null ? p.getTransactionId().toString() : null)
                .productCode(p.getProductCode())
                .productName(p.getProductName())
                .direction(p.getDirection())
                .quantity(p.getQuantity())
                .referenceType(p.getReferenceType())
                .referenceId(p.getReferenceId() != null ? p.getReferenceId().toString() : null)
                .createdAt(p.getCreatedAt())
                .note(p.getNote())
                .createdBy(p.getCreatedBy())
                .build();
    }

    private OrderDetailRow toOrderDetailRow(OrderDetailProjection p) {
        return OrderDetailRow.builder()
                .orderId(p.getOrderId() != null ? p.getOrderId().toString() : null)
                .orderNumber(p.getOrderNumber())
                .orderDate(p.getOrderDate())
                .status(p.getStatus())
                .customerName(p.getCustomerName())
                .contractNumber(p.getContractNumber())
                .productCode(p.getProductCode())
                .productName(p.getProductName())
                .vendorCode(p.getVendorCode())
                .quantity(p.getQuantity())
                .unitPrice(p.getUnitPrice())
                .discountAmount(p.getDiscountAmount())
                .taxAmount(p.getTaxAmount())
                .totalAmount(p.getTotalAmount())
                .createdBy(p.getCreatedBy())
                .build();
    }

    private PaymentRequestDetailRow toPaymentDetailRow(PaymentRequestDetailProjection p) {
        return PaymentRequestDetailRow.builder()
                .paymentRequestId(p.getPaymentRequestId() != null ? p.getPaymentRequestId().toString() : null)
                .requestNumber(p.getRequestNumber())
                .requestDate(p.getRequestDate())
                .status(p.getStatus())
                .vendorCode(p.getVendorCode())
                .vendorName(p.getVendorName())
                .productCode(p.getProductCode())
                .purchaseOrderNumber(p.getPurchaseOrderNumber())
                .requestedAmount(p.getRequestedAmount())
                .paidAmount(p.getPaidAmount())
                .feeAmount(p.getFeeAmount())
                .totalAmount(p.getTotalAmount())
                .currentApprovalLevel(p.getCurrentApprovalLevel())
                .approvalLevels(p.getApprovalLevels())
                .createdBy(p.getCreatedBy())
                .build();
    }
}
