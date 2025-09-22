package com.cnh.ies.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.cnh.ies.dto.common.ApiResponse;
import com.cnh.ies.model.general.ApiRequestModel;
import com.cnh.ies.model.general.ListDataModel;
import com.cnh.ies.model.order.CreateOrderRequest;
import com.cnh.ies.model.order.OrderInfo;
import com.cnh.ies.service.order.OrderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
@Slf4j
public class OrderController {
    private final OrderService orderService;

    @PostMapping("/create")
    public ApiResponse<OrderInfo> createOrder(@RequestBody CreateOrderRequest request) {
        String requestId = UUID.randomUUID().toString();
        log.info("Creating order with initiated requestId: {}", requestId);

        OrderInfo response = orderService.createOrder(request, requestId);

        log.info("Creating order success with requestId: {}", requestId);

        return ApiResponse.success(response, "Create order success");
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderInfo> getOrderById(@PathVariable String id) {
        String requestId = UUID.randomUUID().toString();
        log.info("Getting order by id: {} initiated requestId: {}", id, requestId);

        OrderInfo response = orderService.getOrderById(id, requestId);

        log.info("Getting order by id: {} success with requestId: {}", id, requestId);

        return ApiResponse.success(response, "Get order by id success");
    }

    @DeleteMapping("/delete/{id}")
    public ApiResponse<String> deleteOrder(@PathVariable String id) {
        String requestId = UUID.randomUUID().toString();
        log.info("Deleting order with initiated requestId: {}", id);

        String response = orderService.deleteOrder(id, requestId);

        log.info("Deleting order with initiated requestId: {} success with requestId: {}", id, requestId);

        return ApiResponse.success(response, "Delete order success");
    }

    // @PostMapping("/update")
    // public ApiResponse<OrderInfo> updateOrder(@RequestBody UpdateOrderRequest
    // request) {
    // String requestId = UUID.randomUUID().toString();
    // log.info("Updating order with initiated requestId: {}", requestId);
    // }

    @PostMapping("/list")
    public ApiResponse<ListDataModel<OrderInfo>> getAllOrders(@RequestBody ApiRequestModel request) {
        String requestId = UUID.randomUUID().toString();
        log.info("Getting all orders with initiated requestId: {}", requestId);

        ListDataModel<OrderInfo> response = orderService.getAllOrders(requestId, request.getPage(), request.getLimit());

        log.info("Getting all orders with initiated requestId: {} success with requestId: {}", requestId, requestId);

        return ApiResponse.success(response, "Get all orders success");
    }
}
