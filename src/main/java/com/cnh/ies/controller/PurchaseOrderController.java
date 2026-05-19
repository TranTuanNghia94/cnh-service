package com.cnh.ies.controller;

import java.util.UUID;
import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cnh.ies.dto.common.ApiResponse;
import com.cnh.ies.util.RequestContext;
import com.cnh.ies.model.general.ListDataModel;
import com.cnh.ies.model.purchaseorder.CreatePurchaseOrderRequest;
import com.cnh.ies.model.purchaseorder.FindPurchaseOrderLineByDocumentRequest;
import com.cnh.ies.model.purchaseorder.PurchaseOrderInfo;
import com.cnh.ies.model.purchaseorder.PurchaseOrderListRequest;
import com.cnh.ies.model.purchaseorder.UpdatePurchaseOrderStatusRequest;
import com.cnh.ies.model.purchaseorder.PurchaseOrderLineInfo;
import com.cnh.ies.service.purchaseorder.PurchaseOrderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/purchase-order")
@RequiredArgsConstructor
@Slf4j
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    @PostMapping("/create")
    public ApiResponse<PurchaseOrderInfo> createPurchaseOrder(@RequestBody CreatePurchaseOrderRequest request) {
        String requestId = RequestContext.getRequestIdOrGenerate();
PurchaseOrderInfo response = purchaseOrderService.createPurchaseOrder(request, requestId);
return ApiResponse.success(response, "Create purchase order success");
    }

    @GetMapping("/{code}")
    public ApiResponse<PurchaseOrderInfo> getPurchaseOrderByCode(@PathVariable String code) {
        String requestId = RequestContext.getRequestIdOrGenerate();
PurchaseOrderInfo response = purchaseOrderService.getPurchaseOrderByCode(code, requestId);
return ApiResponse.success(response, "Get purchase order by code success");
    }

    @DeleteMapping("/delete/{id}")
    public ApiResponse<String> deletePurchaseOrder(@PathVariable String id) {
        String requestId = RequestContext.getRequestIdOrGenerate();
String response = purchaseOrderService.deletePurchaseOrder(id, requestId);
return ApiResponse.success(response, "Delete purchase order success");
    }

    @PostMapping("/update")
    public ApiResponse<PurchaseOrderInfo> updatePurchaseOrder(@RequestBody CreatePurchaseOrderRequest request) {
        String requestId = RequestContext.getRequestIdOrGenerate();
PurchaseOrderInfo response = purchaseOrderService.updatePurchaseOrder(request, requestId);
return ApiResponse.success(response, "Update purchase order success");
    }

    @PostMapping("/update-status/{id}")
    public ApiResponse<PurchaseOrderInfo> updatePurchaseOrderStatus(@PathVariable String id,
            @RequestBody UpdatePurchaseOrderStatusRequest request) {
        String requestId = RequestContext.getRequestIdOrGenerate();
PurchaseOrderInfo response = purchaseOrderService.updatePurchaseOrderStatus(id, request.getStatus(), requestId);
return ApiResponse.success(response, "Update purchase order status success");
    }

    @PostMapping("/list")
    public ApiResponse<ListDataModel<PurchaseOrderInfo>> getAllPurchaseOrders(@RequestBody PurchaseOrderListRequest request) {
        String requestId = RequestContext.getRequestIdOrGenerate();
ListDataModel<PurchaseOrderInfo> response = purchaseOrderService.getAllPurchaseOrders(
                requestId,
                request.getPage(),
                request.getLimit(),
                request.getPurchaseOrderNumber(),
                request.getContractNumber(),
                request.getCreatedBy(),
                request.getCustomerName());
return ApiResponse.success(response, "Get all purchase orders success");
    }

    @PostMapping("/lines/find-by-document")
    public ApiResponse<List<PurchaseOrderLineInfo>> findPurchaseOrderLinesByDocument(
            @RequestBody FindPurchaseOrderLineByDocumentRequest request) {
        String requestId = RequestContext.getRequestIdOrGenerate();
List<PurchaseOrderLineInfo> response = purchaseOrderService.findPurchaseOrderLinesByDocument(request, requestId);
return ApiResponse.success(response, "Find purchase order lines by document success");
    }
}
