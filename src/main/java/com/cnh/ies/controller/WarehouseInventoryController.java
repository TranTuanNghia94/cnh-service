package com.cnh.ies.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cnh.ies.dto.common.ApiResponse;
import com.cnh.ies.model.general.ApiRequestModel;
import com.cnh.ies.model.general.ListDataModel;
import com.cnh.ies.model.warehouse.WarehouseInventoryBalanceInfo;
import com.cnh.ies.model.warehouse.WarehouseOutboundRequest;
import com.cnh.ies.model.warehouse.WarehouseStockTransactionInfo;
import com.cnh.ies.service.warehouse.WarehouseInventoryService;
import com.cnh.ies.util.RequestContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
@RestController
@RequestMapping("/warehouse-inventory")
@RequiredArgsConstructor
@Slf4j
public class WarehouseInventoryController {

    private final WarehouseInventoryService warehouseInventoryService;

    @PostMapping("/list")   
    public ApiResponse<ListDataModel<WarehouseInventoryBalanceInfo>> list(
            @RequestBody ApiRequestModel request) {
        String requestId = UUID.randomUUID().toString();
        log.info("Getting warehouse inventory list with page: {} and limit: {} initiated requestId: {}", request.getPage(), request.getLimit(), requestId);

        ListDataModel<WarehouseInventoryBalanceInfo> result = warehouseInventoryService.listInventory(requestId, request.getPage(), request.getLimit());

        log.info("Getting warehouse inventory list with page: {} and limit: {} success requestId: {}", request.getPage(), request.getLimit(), requestId);
        return ApiResponse.success(result, "Warehouse inventory list");
    }

    @GetMapping("/products/{productId}")
    public ApiResponse<WarehouseInventoryBalanceInfo> getBalance(@PathVariable String productId) {
        WarehouseInventoryBalanceInfo response = warehouseInventoryService.getBalance(productId, RequestContext.getRequestId());
        return ApiResponse.success(response, "Warehouse balance");
    }

    @GetMapping("/products/{productId}/transactions")
    public ApiResponse<List<WarehouseStockTransactionInfo>> listTransactions(@PathVariable String productId) {
        List<WarehouseStockTransactionInfo> response = warehouseInventoryService.listTransactionsForProduct(productId,
                RequestContext.getRequestId());
        return ApiResponse.success(response, "Stock transactions");
    }

    @PostMapping("/outbound")
    public ApiResponse<WarehouseStockTransactionInfo> outbound(@RequestBody WarehouseOutboundRequest request) {
        WarehouseStockTransactionInfo response = warehouseInventoryService.recordOutbound(request, RequestContext.getRequestId());
        return ApiResponse.success(response, "Outbound recorded");
    }
}
