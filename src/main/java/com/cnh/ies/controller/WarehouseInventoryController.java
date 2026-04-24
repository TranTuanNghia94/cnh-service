package com.cnh.ies.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cnh.ies.constant.Constant;
import com.cnh.ies.dto.common.ApiResponse;
import com.cnh.ies.model.warehouse.WarehouseInventoryBalanceInfo;
import com.cnh.ies.model.warehouse.WarehouseOutboundRequest;
import com.cnh.ies.model.warehouse.WarehouseStockTransactionInfo;
import com.cnh.ies.service.warehouse.WarehouseInventoryService;
import com.cnh.ies.util.RequestContext;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/warehouse-inventory")
@RequiredArgsConstructor
public class WarehouseInventoryController {

    private final WarehouseInventoryService warehouseInventoryService;

    @GetMapping("/products/{productId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<WarehouseInventoryBalanceInfo> getBalance(@PathVariable String productId) {
        WarehouseInventoryBalanceInfo response = warehouseInventoryService.getBalance(productId, RequestContext.getRequestId());
        return ApiResponse.success(response, "Warehouse balance");
    }

    @GetMapping("/products/{productId}/transactions")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<WarehouseStockTransactionInfo>> listTransactions(@PathVariable String productId) {
        List<WarehouseStockTransactionInfo> response = warehouseInventoryService.listTransactionsForProduct(productId,
                RequestContext.getRequestId());
        return ApiResponse.success(response, "Stock transactions");
    }

    @PostMapping("/outbound")
    @PreAuthorize("hasRole('" + Constant.ROLE_WAREHOUSE_KEEPER + "')")
    public ApiResponse<WarehouseStockTransactionInfo> outbound(@RequestBody WarehouseOutboundRequest request) {
        WarehouseStockTransactionInfo response = warehouseInventoryService.recordOutbound(request, RequestContext.getRequestId());
        return ApiResponse.success(response, "Outbound recorded");
    }
}
