package com.cnh.ies.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cnh.ies.dto.common.ApiResponse;
import com.cnh.ies.model.general.ApiRequestModel;
import com.cnh.ies.model.general.ListDataModel;
import com.cnh.ies.model.purchaseorder.CreatePurchaseOrderRequest;
import com.cnh.ies.model.purchaseorder.PurchaseOrderInfo;
import com.cnh.ies.model.purchaseorder.UpdatePurchaseOrderStatusRequest;
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
        String requestId = UUID.randomUUID().toString();
        log.info("Creating purchase order with initiated requestId: {}", requestId);

        PurchaseOrderInfo response = purchaseOrderService.createPurchaseOrder(request, requestId);

        log.info("Creating purchase order success with requestId: {}", requestId);
        return ApiResponse.success(response, "Create purchase order success");
    }

    @GetMapping("/{code}")
    public ApiResponse<PurchaseOrderInfo> getPurchaseOrderByCode(@PathVariable String code) {
        String requestId = UUID.randomUUID().toString();
        log.info("Getting purchase order by code: {} initiated requestId: {}", code, requestId);

        PurchaseOrderInfo response = purchaseOrderService.getPurchaseOrderByCode(code, requestId);

        log.info("Getting purchase order by code: {} success with requestId: {}", code, requestId);
        return ApiResponse.success(response, "Get purchase order by code success");
    }

    @DeleteMapping("/delete/{id}")
    public ApiResponse<String> deletePurchaseOrder(@PathVariable String id) {
        String requestId = UUID.randomUUID().toString();
        log.info("Deleting purchase order with initiated requestId: {}", id);

        String response = purchaseOrderService.deletePurchaseOrder(id, requestId);

        log.info("Deleting purchase order success with requestId: {}", requestId);
        return ApiResponse.success(response, "Delete purchase order success");
    }

    @PostMapping("/update")
    public ApiResponse<PurchaseOrderInfo> updatePurchaseOrder(@RequestBody CreatePurchaseOrderRequest request) {
        String requestId = UUID.randomUUID().toString();
        log.info("Updating purchase order with initiated requestId: {}", requestId);

        PurchaseOrderInfo response = purchaseOrderService.updatePurchaseOrder(request, requestId);

        log.info("Updating purchase order success with requestId: {}", requestId);
        return ApiResponse.success(response, "Update purchase order success");
    }

    @PostMapping("/update-status/{id}")
    public ApiResponse<PurchaseOrderInfo> updatePurchaseOrderStatus(@PathVariable String id,
            @RequestBody UpdatePurchaseOrderStatusRequest request) {
        String requestId = UUID.randomUUID().toString();
        log.info("Updating purchase order status with initiated requestId: {} | id: {} | status: {}", requestId, id, request.getStatus());

        PurchaseOrderInfo response = purchaseOrderService.updatePurchaseOrderStatus(id, request.getStatus(), requestId);

        log.info("Updating purchase order status success with requestId: {}", requestId);
        return ApiResponse.success(response, "Update purchase order status success");
    }

    @PostMapping("/list")
    public ApiResponse<ListDataModel<PurchaseOrderInfo>> getAllPurchaseOrders(@RequestBody ApiRequestModel request) {
        String requestId = UUID.randomUUID().toString();
        log.info("Getting all purchase orders with initiated requestId: {}", requestId);

        ListDataModel<PurchaseOrderInfo> response = purchaseOrderService.getAllPurchaseOrders(requestId, request.getPage(), request.getLimit());

        log.info("Getting all purchase orders success with requestId: {}", requestId);
        return ApiResponse.success(response, "Get all purchase orders success");
    }
}
