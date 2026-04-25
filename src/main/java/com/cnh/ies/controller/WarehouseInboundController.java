package com.cnh.ies.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cnh.ies.constant.Constant;
import com.cnh.ies.dto.common.ApiResponse;
import com.cnh.ies.model.payment.ApprovePaymentRequest;
import com.cnh.ies.model.payment.PaymentRequestInfo;
import com.cnh.ies.model.payment.RejectPaymentRequest;
import com.cnh.ies.model.warehouse.WarehouseInboundAddLineRequest;
import com.cnh.ies.model.warehouse.WarehouseInboundConfirmRequest;
import com.cnh.ies.model.warehouse.WarehouseInboundLinePatchRequest;
import com.cnh.ies.model.warehouse.WarehouseInboundReceiptInfo;
import com.cnh.ies.model.warehouse.WarehouseInboundSearchResponse;
import com.cnh.ies.service.warehouse.WarehouseInboundService;
import com.cnh.ies.util.RequestContext;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/warehouse-inbound")
@RequiredArgsConstructor
public class WarehouseInboundController {

    private final WarehouseInboundService warehouseInboundService;

    @GetMapping("/search")
    // @PreAuthorize("hasRole('" + Constant.ROLE_WAREHOUSE_KEEPER + "')")
    public ApiResponse<WarehouseInboundSearchResponse> search(
            @RequestParam(name = "notesContains", required = false) String notesContains,
            @RequestParam(name = "paperType", required = false) String paperType,
            @RequestParam(name = "paperCode", required = false) String paperCode) {
        WarehouseInboundSearchResponse response = warehouseInboundService.search(notesContains, paperType, paperCode,
                RequestContext.getRequestId());
        return ApiResponse.success(response, "Warehouse inbound search success");
    }

    @GetMapping("/payment-request/{paymentRequestId}")
    // @PreAuthorize("hasRole('" + Constant.ROLE_WAREHOUSE_KEEPER + "')")
    public ApiResponse<PaymentRequestInfo> getPaymentRequest(@PathVariable String paymentRequestId) {
        PaymentRequestInfo response = warehouseInboundService.getPaymentRequestDetail(paymentRequestId,
                RequestContext.getRequestId());
        return ApiResponse.success(response, "Get payment request for inbound success");
    }

    @PostMapping("/confirm")
    // @PreAuthorize("hasRole('" + Constant.ROLE_WAREHOUSE_KEEPER + "')")
    public ApiResponse<WarehouseInboundReceiptInfo> confirm(@RequestBody WarehouseInboundConfirmRequest request) {
        WarehouseInboundReceiptInfo response = warehouseInboundService.confirmInbound(request, RequestContext.getRequestId());
        return ApiResponse.success(response, "Warehouse inbound saved as draft; submit for approval when ready");
    }

    @PostMapping("/receipt/{receiptId}/submit")
    // @PreAuthorize("hasRole('" + Constant.ROLE_WAREHOUSE_KEEPER + "')")
    public ApiResponse<WarehouseInboundReceiptInfo> submitForApproval(@PathVariable String receiptId) {
        WarehouseInboundReceiptInfo response = warehouseInboundService.submitForApproval(receiptId, RequestContext.getRequestId());
        return ApiResponse.success(response, "Warehouse inbound submitted for approval");
    }

    @PostMapping("/receipt/{receiptId}/approve")
    // @PreAuthorize("isAuthenticated()")
    public ApiResponse<WarehouseInboundReceiptInfo> approve(
            @PathVariable String receiptId,
            @RequestBody(required = false) ApprovePaymentRequest request) {
        WarehouseInboundReceiptInfo response = warehouseInboundService.approve(receiptId, request, RequestContext.getRequestId());
        return ApiResponse.success(response, "Warehouse inbound approval recorded");
    }

    @PostMapping("/receipt/{receiptId}/reject")
    // @PreAuthorize("isAuthenticated()")
    public ApiResponse<WarehouseInboundReceiptInfo> reject(
            @PathVariable String receiptId,
            @RequestBody RejectPaymentRequest request) {
        WarehouseInboundReceiptInfo response = warehouseInboundService.reject(receiptId, request, RequestContext.getRequestId());
        return ApiResponse.success(response, "Warehouse inbound rejected");
    }

    @PostMapping("/receipt/{receiptId}/cancel")
    // @PreAuthorize("hasRole('" + Constant.ROLE_WAREHOUSE_KEEPER + "')")
    public ApiResponse<WarehouseInboundReceiptInfo> cancel(@PathVariable String receiptId) {
        WarehouseInboundReceiptInfo response = warehouseInboundService.cancel(receiptId, RequestContext.getRequestId());
        return ApiResponse.success(response, "Warehouse inbound cancelled");
    }

    @DeleteMapping("/receipt/{receiptId}/lines/{lineId}")
    // @PreAuthorize("hasRole('" + Constant.ROLE_WAREHOUSE_KEEPER + "')")
    public ApiResponse<WarehouseInboundReceiptInfo> deleteLine(@PathVariable String receiptId, @PathVariable String lineId) {
        WarehouseInboundReceiptInfo response = warehouseInboundService.deleteInboundLine(receiptId, lineId,
                RequestContext.getRequestId());
        return ApiResponse.success(response, "Inbound line removed");
    }

    @PostMapping("/receipt/{receiptId}/lines")
    // @PreAuthorize("hasRole('" + Constant.ROLE_WAREHOUSE_KEEPER + "')")
    public ApiResponse<WarehouseInboundReceiptInfo> addLine(
            @PathVariable String receiptId,
            @RequestBody WarehouseInboundAddLineRequest body) {
        WarehouseInboundReceiptInfo response = warehouseInboundService.addInboundLine(receiptId, body,
                RequestContext.getRequestId());
        return ApiResponse.success(response, "Inbound line added");
    }

    @PatchMapping("/receipt/{receiptId}/lines/{lineId}")
    // @PreAuthorize("hasRole('" + Constant.ROLE_WAREHOUSE_KEEPER + "')")
    public ApiResponse<WarehouseInboundReceiptInfo> patchLine(
            @PathVariable String receiptId,
            @PathVariable String lineId,
            @RequestBody WarehouseInboundLinePatchRequest body) {
        WarehouseInboundReceiptInfo response = warehouseInboundService.patchInboundLine(receiptId, lineId, body,
                RequestContext.getRequestId());
        return ApiResponse.success(response, "Inbound line updated");
    }

    @GetMapping("/payment-request/{paymentRequestId}/receipts")
    // @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<WarehouseInboundReceiptInfo>> listReceipts(@PathVariable String paymentRequestId) {
        List<WarehouseInboundReceiptInfo> response = warehouseInboundService.listReceiptsForPaymentRequest(paymentRequestId,
                RequestContext.getRequestId());
        return ApiResponse.success(response, "List warehouse inbound receipts success");
    }

    @GetMapping("/receipt/{receiptId}")
    // @PreAuthorize("isAuthenticated()")
    public ApiResponse<WarehouseInboundReceiptInfo> getReceipt(@PathVariable String receiptId) {
        WarehouseInboundReceiptInfo response = warehouseInboundService.getReceipt(receiptId, RequestContext.getRequestId());
        return ApiResponse.success(response, "Get warehouse inbound receipt success");
    }
}
