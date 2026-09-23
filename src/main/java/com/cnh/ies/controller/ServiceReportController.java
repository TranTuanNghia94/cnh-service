package com.cnh.ies.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cnh.ies.dto.common.ApiResponse;
import com.cnh.ies.model.report.OrderOverallReport;
import com.cnh.ies.model.report.OrderReportRequest;
import com.cnh.ies.model.report.PaymentRequestOverallReport;
import com.cnh.ies.model.report.PaymentRequestReportRequest;
import com.cnh.ies.model.report.PurchaseOverallReport;
import com.cnh.ies.model.report.PurchaseReportRequest;
import com.cnh.ies.model.report.ServiceReportDetailResponse;
import com.cnh.ies.model.report.WarehouseInboundOverallReport;
import com.cnh.ies.model.report.WarehouseInboundReportRequest;
import com.cnh.ies.model.report.WarehouseInventoryOverallReport;
import com.cnh.ies.model.report.WarehouseInventoryReportRequest;
import com.cnh.ies.model.report.WarehouseOutboundOverallReport;
import com.cnh.ies.model.report.WarehouseOutboundReportRequest;
import com.cnh.ies.model.report.OrderDetailRow;
import com.cnh.ies.model.report.PaymentRequestDetailRow;
import com.cnh.ies.model.report.WarehouseInboundDetailRow;
import com.cnh.ies.model.report.WarehouseInventoryDetailRow;
import com.cnh.ies.model.report.WarehouseOutboundDetailRow;
import com.cnh.ies.service.report.ServiceReportService;
import com.cnh.ies.util.RequestContext;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/service-reports")
@RequiredArgsConstructor
@Tag(name = "Service Reports", description = "Aggregate service report APIs")
public class ServiceReportController {

    private final ServiceReportService serviceReportService;

    @PostMapping("/warehouse-inbound/overall")
    public ApiResponse<WarehouseInboundOverallReport> warehouseInboundOverall(
            @Valid @RequestBody WarehouseInboundReportRequest request) {
        return ApiResponse.success(
                serviceReportService.warehouseInboundOverall(request, RequestContext.getRequestIdOrGenerate()),
                "Warehouse inbound overall report success");
    }

    @PostMapping("/warehouse-inbound/detail")
    public ApiResponse<ServiceReportDetailResponse<WarehouseInboundDetailRow, WarehouseInboundOverallReport>> warehouseInboundDetail(
            @Valid @RequestBody WarehouseInboundReportRequest request) {
        return ApiResponse.success(
                serviceReportService.warehouseInboundDetail(request, RequestContext.getRequestIdOrGenerate()),
                "Warehouse inbound detail report success");
    }

    @PostMapping("/warehouse-outbound/overall")
    public ApiResponse<WarehouseOutboundOverallReport> warehouseOutboundOverall(
            @Valid @RequestBody WarehouseOutboundReportRequest request) {
        return ApiResponse.success(
                serviceReportService.warehouseOutboundOverall(request, RequestContext.getRequestIdOrGenerate()),
                "Warehouse outbound overall report success");
    }

    @PostMapping("/warehouse-outbound/detail")
    public ApiResponse<ServiceReportDetailResponse<WarehouseOutboundDetailRow, WarehouseOutboundOverallReport>> warehouseOutboundDetail(
            @Valid @RequestBody WarehouseOutboundReportRequest request) {
        return ApiResponse.success(
                serviceReportService.warehouseOutboundDetail(request, RequestContext.getRequestIdOrGenerate()),
                "Warehouse outbound detail report success");
    }

    @PostMapping("/warehouse-inventory/overall")
    public ApiResponse<WarehouseInventoryOverallReport> warehouseInventoryOverall(
            @Valid @RequestBody WarehouseInventoryReportRequest request) {
        return ApiResponse.success(
                serviceReportService.warehouseInventoryOverall(request, RequestContext.getRequestIdOrGenerate()),
                "Warehouse inventory overall report success");
    }

    @PostMapping("/warehouse-inventory/detail")
    public ApiResponse<ServiceReportDetailResponse<WarehouseInventoryDetailRow, WarehouseInventoryOverallReport>> warehouseInventoryDetail(
            @Valid @RequestBody WarehouseInventoryReportRequest request) {
        return ApiResponse.success(
                serviceReportService.warehouseInventoryDetail(request, RequestContext.getRequestIdOrGenerate()),
                "Warehouse inventory detail report success");
    }

    @PostMapping("/order/overall")
    public ApiResponse<OrderOverallReport> orderOverall(@Valid @RequestBody OrderReportRequest request) {
        return ApiResponse.success(
                serviceReportService.orderOverall(request, RequestContext.getRequestIdOrGenerate()),
                "Order overall report success");
    }

    @PostMapping("/order/detail")
    public ApiResponse<ServiceReportDetailResponse<OrderDetailRow, OrderOverallReport>> orderDetail(
            @Valid @RequestBody OrderReportRequest request) {
        return ApiResponse.success(
                serviceReportService.orderDetail(request, RequestContext.getRequestIdOrGenerate()),
                "Order detail report success");
    }

    @PostMapping("/purchase/overall")
    public ApiResponse<PurchaseOverallReport> purchaseOverall(@Valid @RequestBody PurchaseReportRequest request) {
        return ApiResponse.success(
                serviceReportService.purchaseOverall(request, RequestContext.getRequestIdOrGenerate()),
                "Purchase overall report success");
    }

    @PostMapping("/payment-request/overall")
    public ApiResponse<PaymentRequestOverallReport> paymentRequestOverall(
            @Valid @RequestBody PaymentRequestReportRequest request) {
        return ApiResponse.success(
                serviceReportService.paymentRequestOverall(request, RequestContext.getRequestIdOrGenerate()),
                "Payment request overall report success");
    }

    @PostMapping("/payment-request/detail")
    public ApiResponse<ServiceReportDetailResponse<PaymentRequestDetailRow, PaymentRequestOverallReport>> paymentRequestDetail(
            @Valid @RequestBody PaymentRequestReportRequest request) {
        return ApiResponse.success(
                serviceReportService.paymentRequestDetail(request, RequestContext.getRequestIdOrGenerate()),
                "Payment request detail report success");
    }
}
