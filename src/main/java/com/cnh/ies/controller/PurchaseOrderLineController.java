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
import com.cnh.ies.model.purchaseorder.PurchaseOrderLineInfo;
import com.cnh.ies.model.purchaseorder.UpdatePurchaseOrderLineRequest;
import com.cnh.ies.service.purchaseorder.PurchaseOrderDetailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/purchase-order-line")
@RequiredArgsConstructor
@Slf4j
public class PurchaseOrderLineController {

    private final PurchaseOrderDetailService purchaseOrderDetailService;

    @PostMapping("/create/{purchaseOrderId}")
    public ApiResponse<List<PurchaseOrderLineInfo>> createPurchaseOrderLines(
            @RequestBody List<CreatePurchaseOrderLineRequest> payload,
            @PathVariable String purchaseOrderId) {
        String requestId = UUID.randomUUID().toString();
        log.info("Creating purchase order lines for purchaseOrderId: {} | payload: {}", purchaseOrderId, payload);

        List<PurchaseOrderLineInfo> response = purchaseOrderDetailService.createPurchaseOrderLines(
                payload, UUID.fromString(purchaseOrderId), requestId);

        log.info("Creating purchase order lines success with requestId: {}", requestId);
        return ApiResponse.success(response, "Create purchase order lines success");
    }

    @PostMapping("/update/{purchaseOrderId}")
    public ApiResponse<List<PurchaseOrderLineInfo>> updatePurchaseOrderLines(
            @RequestBody List<UpdatePurchaseOrderLineRequest> payload,
            @PathVariable String purchaseOrderId) {
        String requestId = UUID.randomUUID().toString();
        log.info("Updating purchase order lines for purchaseOrderId: {} | payload: {}", purchaseOrderId, payload);

        List<PurchaseOrderLineInfo> response = purchaseOrderDetailService.updatePurchaseOrderLines(
                payload, UUID.fromString(purchaseOrderId), requestId);

        log.info("Updating purchase order lines success with requestId: {}", requestId);
        return ApiResponse.success(response, "Update purchase order lines success");
    }

    @PostMapping("/delete/{purchaseOrderId}")
    public ApiResponse<String> deletePurchaseOrderLines(
            @RequestBody List<String> ids,
            @PathVariable String purchaseOrderId) {
        String requestId = UUID.randomUUID().toString();
        log.info("Deleting purchase order lines for purchaseOrderId: {} | ids: {}", purchaseOrderId, ids);

        String response = purchaseOrderDetailService.deletePurchaseOrderLines(
                ids, UUID.fromString(purchaseOrderId), requestId);

        log.info("Deleting purchase order lines success with requestId: {}", requestId);
        return ApiResponse.success(response, "Delete purchase order lines success");
    }
}
