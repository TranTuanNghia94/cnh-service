package com.cnh.ies.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cnh.ies.dto.common.ApiResponse;
import com.cnh.ies.model.order.CreateOrderLineRequest;
import com.cnh.ies.model.order.OrderLineInfo;
import com.cnh.ies.service.order.OrderDetailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/order-line")
@RequiredArgsConstructor
@Slf4j
public class OrderLineController {
    private final OrderDetailService orderDetailService;


    @PostMapping("/create/{orderId}")
    //example url: /order-line/create/123
    public ApiResponse<List<OrderLineInfo>> createOrderLines(@RequestBody List<CreateOrderLineRequest> payload, @PathVariable String orderId) {
        String requestId = UUID.randomUUID().toString();
        log.info("Creating order lines for orderId: {} | payload: {}", orderId, payload);

        List<OrderLineInfo> response = orderDetailService.createOrderLines(payload, UUID.fromString(orderId), requestId);

        log.info("Creating order lines success with requestId: {}", requestId);

        return ApiResponse.success(response, "Create order lines success");
    }
}
