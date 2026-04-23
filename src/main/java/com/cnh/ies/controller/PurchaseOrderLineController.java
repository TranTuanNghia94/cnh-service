package com.cnh.ies.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cnh.ies.dto.common.ApiResponse;
import com.cnh.ies.model.purchaseorder.CreatePurchaseOrderLineRequest;
import com.cnh.ies.model.purchaseorder.FindPurchaseOrderLineByDocumentRequest;
import com.cnh.ies.model.purchaseorder.POLinePaymentHistoryInfo;
import com.cnh.ies.model.purchaseorder.PurchaseOrderLineInfo;
import com.cnh.ies.model.purchaseorder.UpdatePurchaseOrderLineRequest;
import com.cnh.ies.service.purchaseorder.PurchaseOrderDetailService;
import com.cnh.ies.util.RequestContext;

import lombok.RequiredArgsConstructor;

/**
 * Purchase Order Line REST Controller.
 * 
 * Note: Request/response logging is handled automatically by RequestResponseLoggingFilter.
 */
@RestController
@RequestMapping("/purchase-order-line")
@RequiredArgsConstructor
public class PurchaseOrderLineController {

    private final PurchaseOrderDetailService purchaseOrderDetailService;

    @PostMapping("/create/{purchaseOrderId}")
    public ApiResponse<List<PurchaseOrderLineInfo>> createPurchaseOrderLines(
            @RequestBody List<CreatePurchaseOrderLineRequest> payload,
            @PathVariable String purchaseOrderId) {
        List<PurchaseOrderLineInfo> response = purchaseOrderDetailService.createPurchaseOrderLines(
                payload, UUID.fromString(purchaseOrderId), RequestContext.getRequestId());
        return ApiResponse.success(response, "Create purchase order lines success");
    }

    @PostMapping("/update/{purchaseOrderId}")
    public ApiResponse<List<PurchaseOrderLineInfo>> updatePurchaseOrderLines(
            @RequestBody List<UpdatePurchaseOrderLineRequest> payload,
            @PathVariable String purchaseOrderId) {
        List<PurchaseOrderLineInfo> response = purchaseOrderDetailService.updatePurchaseOrderLines(
                payload, UUID.fromString(purchaseOrderId), RequestContext.getRequestId());
        return ApiResponse.success(response, "Update purchase order lines success");
    }

    @PostMapping("/delete/{purchaseOrderId}")
    public ApiResponse<String> deletePurchaseOrderLines(
            @RequestBody List<String> ids,
            @PathVariable String purchaseOrderId) {
        String response = purchaseOrderDetailService.deletePurchaseOrderLines(
                ids, UUID.fromString(purchaseOrderId), RequestContext.getRequestId());
        return ApiResponse.success(response, "Delete purchase order lines success");
    }



    @PostMapping("/payment-history")
    public ApiResponse<POLinePaymentHistoryInfo> getPaymentHistory(
            @RequestBody FindPurchaseOrderLineByDocumentRequest request) {
        POLinePaymentHistoryInfo response = purchaseOrderDetailService.getPaymentHistoryByDocument(
                request, RequestContext.getRequestId());
        return ApiResponse.success(response, "Get payment history success");
    }
}
