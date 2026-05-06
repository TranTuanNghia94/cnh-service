package com.cnh.ies.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cnh.ies.dto.common.ApiResponse;
import com.cnh.ies.model.warehouse.DeliverySlipInfo;
import com.cnh.ies.service.warehouse.DeliverySlipService;
import com.cnh.ies.util.RequestContext;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/delivery-slip")
@RequiredArgsConstructor
public class DeliverySlipController {

    private final DeliverySlipService deliverySlipService;

    @GetMapping("/warehouse-outbound/{outboundId}")
    public ApiResponse<DeliverySlipInfo> getByWarehouseOutbound(@PathVariable String outboundId) {
        DeliverySlipInfo response = deliverySlipService.getByWarehouseOutbound(
                outboundId, RequestContext.getRequestId());
        return ApiResponse.success(response, "Get delivery slip by warehouse outbound success");
    }

    @GetMapping("/outbound-number")
    public ApiResponse<DeliverySlipInfo> getByOutboundNumber(@RequestParam("outboundNumber") String outboundNumber) {
        DeliverySlipInfo response = deliverySlipService.getByOutboundNumber(
                outboundNumber, RequestContext.getRequestId());
        return ApiResponse.success(response, "Get delivery slip by outbound number success");
    }
}
