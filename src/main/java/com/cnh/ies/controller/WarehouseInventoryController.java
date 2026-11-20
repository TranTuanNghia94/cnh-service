package com.cnh.ies.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cnh.ies.constant.PermissionConstants;
import com.cnh.ies.dto.common.ApiResponse;
import com.cnh.ies.model.general.ListDataModel;
import com.cnh.ies.model.warehouse.WarehouseInventoryBalanceInfo;
import com.cnh.ies.model.warehouse.WarehouseInventoryListRequest;
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

    @PreAuthorize("hasAuthority('" + PermissionConstants.WAREHOUSE_INVENTORY_READ + "')")
    @PostMapping("/list")
    public ApiResponse<ListDataModel<WarehouseInventoryBalanceInfo>> list(
            @RequestBody WarehouseInventoryListRequest request) {
        String requestId = RequestContext.getRequestIdOrGenerate();
        ListDataModel<WarehouseInventoryBalanceInfo> result = warehouseInventoryService.listInventory(
                requestId,
                request.getPage(),
                request.getLimit(),
                request.getProductCode(),
                request.getProductName(),
                request.getProductCategory());

        log.info("Getting warehouse inventory list with page: {} and limit: {} success requestId: {}",
                request.getPage(), request.getLimit(), requestId);
        return ApiResponse.success(result, "Warehouse inventory list");
    }

    @PreAuthorize("hasAuthority('" + PermissionConstants.WAREHOUSE_INVENTORY_READ + "')")
    @GetMapping("/products/{productId}")
    public ApiResponse<WarehouseInventoryBalanceInfo> getBalance(@PathVariable String productId) {
        WarehouseInventoryBalanceInfo response = warehouseInventoryService.getBalance(productId,
                RequestContext.getRequestId());
        return ApiResponse.success(response, "Warehouse balance");
    }

    @PreAuthorize("hasAuthority('" + PermissionConstants.WAREHOUSE_INVENTORY_READ + "')")
    @GetMapping("/products/{productId}/transactions")
    public ApiResponse<List<WarehouseStockTransactionInfo>> listTransactions(@PathVariable String productId) {
        List<WarehouseStockTransactionInfo> response = warehouseInventoryService.listTransactionsForProduct(productId,
                RequestContext.getRequestId());
        return ApiResponse.success(response, "Stock transactions");
    }

    @PreAuthorize("hasAuthority('" + PermissionConstants.WAREHOUSE_OUTBOUND_CREATE + "')")
    @PostMapping("/outbound")
    public ApiResponse<WarehouseStockTransactionInfo> outbound(@RequestBody WarehouseOutboundRequest request) {
        WarehouseStockTransactionInfo response = warehouseInventoryService.recordOutbound(request,
                RequestContext.getRequestId());
        return ApiResponse.success(response, "Outbound recorded");
    }
}
