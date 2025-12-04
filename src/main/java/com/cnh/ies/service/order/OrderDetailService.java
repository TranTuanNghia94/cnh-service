package com.cnh.ies.service.order;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.cnh.ies.entity.order.OrderEntity;
import com.cnh.ies.entity.order.OrderLineEntity;
import com.cnh.ies.exception.ApiException;
import com.cnh.ies.model.order.CreateOrderLineRequest;
import com.cnh.ies.model.order.OrderLineInfo;
import com.cnh.ies.repository.order.OrderRepo;
import com.cnh.ies.repository.order.OrderLineRepo;
import com.cnh.ies.mapper.order.OrderLineMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderDetailService {
    private final OrderLineRepo orderLineRepo;
    private final OrderRepo orderRepo;
    private final OrderLineMapper orderLineMapper;

    public List<OrderLineInfo> createOrderLines(List<CreateOrderLineRequest> payload, UUID orderId, String requestId) {
        log.info("Creating order lines for orderId: {} | payload: {}", orderId, payload);

        if (payload == null || payload.isEmpty()) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "Payload is required",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }

        OrderEntity order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ApiException(ApiException.ErrorCode.NOT_FOUND, "Order not found",
                        HttpStatus.NOT_FOUND.value(), requestId));

        List<OrderLineEntity> orderLines = payload.stream().map(line -> orderLineMapper.toOrderLineEntity(line, order))
                .collect(Collectors.toList());
        orderLineRepo.saveAll(orderLines);

        log.info("Order lines created successfully for orderId: {} | requestId: {}", orderId, requestId);

        return orderLines.stream().map(orderLineMapper::toOrderLineInfo).collect(Collectors.toList());
    }

}
