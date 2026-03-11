package com.cnh.ies.service.order;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.cnh.ies.entity.product.ProductEntity;
import com.cnh.ies.entity.order.OrderEntity;
import com.cnh.ies.entity.order.OrderLineEntity;
import com.cnh.ies.exception.ApiException;
import com.cnh.ies.model.order.CreateOrderLineRequest;
import com.cnh.ies.model.order.OrderLineInfo;
import com.cnh.ies.model.order.UpdateOrderLineRequest;
import com.cnh.ies.repository.order.OrderRepo;
import com.cnh.ies.repository.order.OrderLineRepo;
import com.cnh.ies.mapper.order.OrderLineMapper;
import com.cnh.ies.repository.product.ProductRepo;
import com.cnh.ies.util.RequestContext;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderDetailService {
    private final OrderLineRepo orderLineRepo;
    private final OrderRepo orderRepo;
    private final OrderLineMapper orderLineMapper;
    private final ProductRepo productRepo;

    public List<OrderLineInfo> createOrderLines(List<CreateOrderLineRequest> payload, UUID orderId, String requestId) {
        log.info("Creating order lines for orderId: {} | payload: {}", orderId, payload);

        if (payload == null || payload.isEmpty()) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "Payload is required",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }

        OrderEntity order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ApiException(ApiException.ErrorCode.NOT_FOUND, "Order not found",
                        HttpStatus.NOT_FOUND.value(), requestId));

        Map<UUID, ProductEntity> productById = loadProductsById(payload);

        List<OrderLineEntity> orderLines = payload.stream().map(line -> setProductToOrderLine(line, order, productById))
                .collect(Collectors.toList());
        orderLineRepo.saveAll(orderLines);

        log.info("Order lines created successfully for orderId: {} | requestId: {}", orderId, requestId);

        return orderLineMapper.toOrderLineInfos(orderLines);
    }

    @Transactional
    public List<OrderLineInfo> updateOrderLines(List<UpdateOrderLineRequest> payload, UUID orderId, String requestId) {
        log.info("Updating order lines for orderId: {} | payload: {}", orderId, payload);

        if (payload == null || payload.isEmpty()) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "Payload is required",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }

        orderRepo.findById(orderId)
                .orElseThrow(() -> new ApiException(ApiException.ErrorCode.NOT_FOUND, "Order not found",
                        HttpStatus.NOT_FOUND.value(), requestId));

        List<OrderLineEntity> orderLines = orderLineRepo.findByOrderId(orderId);

        if (orderLines.isEmpty()) {
            throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "Order lines not found",
                    HttpStatus.NOT_FOUND.value(), requestId);
        }

        for (UpdateOrderLineRequest updateOrderLineRequest : payload) {
            OrderLineEntity orderLine = orderLines.stream()
                    .filter(l -> l.getId() != null && l.getId().equals(UUID.fromString(updateOrderLineRequest.getId())))
                    .findFirst()
                    .orElseThrow(() -> new ApiException(ApiException.ErrorCode.NOT_FOUND, "Order line not found",
                            HttpStatus.NOT_FOUND.value(), requestId));
            orderLineMapper.applyUpdate(updateOrderLineRequest, orderLine);
        }

        orderLineRepo.saveAll(orderLines);

        log.info("Order lines updated successfully for orderId: {} | requestId: {}", orderId, requestId);

        return orderLines.stream().map(orderLineMapper::toOrderLineInfo).collect(Collectors.toList());
    }


    public String deleteOrderLines(List<String> ids, UUID orderId, String requestId) {
        log.info("Deleting order lines for orderId: {} | ids: {}", orderId, ids);

        if (ids == null || ids.isEmpty()) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "Ids is required",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }

        if (orderId == null) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "Order id is required",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }
        orderRepo.findById(orderId)
                .orElseThrow(() -> new ApiException(ApiException.ErrorCode.NOT_FOUND, "Order not found",
                        HttpStatus.NOT_FOUND.value(), requestId));

        List<OrderLineEntity> orderLines = orderLineRepo.findByOrderId(orderId);

        if (orderLines.isEmpty()) {
            throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "Order lines not found",
                    HttpStatus.NOT_FOUND.value(), requestId);
        }

        for (String id : ids) {
            OrderLineEntity orderLine = orderLines.stream().filter(l -> l.getId().equals(UUID.fromString(id))).findFirst().orElseThrow(() -> new ApiException(ApiException.ErrorCode.NOT_FOUND, "Order line not found",
                    HttpStatus.NOT_FOUND.value(), requestId));
            orderLine.setIsDeleted(true);
            orderLine.setUpdatedBy(RequestContext.getCurrentUsername());
        }
        orderLineRepo.saveAll(orderLines);

        log.info("Order lines deleted successfully for orderId: {} | requestId: {}", orderId, requestId);

        return "Order lines deleted successfully";
    }

    private Map<UUID, ProductEntity> loadProductsById(List<CreateOrderLineRequest> orderLines) {
        List<UUID> productIds = orderLines.stream()
                .map(l -> UUID.fromString(l.getProductId().get()))
                .collect(Collectors.toList());
        List<ProductEntity> products = productRepo.findByIdIn(productIds);
        return products.stream().collect(Collectors.toMap(ProductEntity::getId, Function.identity()));
    }

    private OrderLineEntity setProductToOrderLine(CreateOrderLineRequest line, OrderEntity order,
        Map<UUID, ProductEntity> productById) {
        UUID productId = UUID.fromString(line.getProductId().get());
        ProductEntity product = productById.get(productId);
        OrderLineEntity entity = orderLineMapper.toOrderLineEntity(line, order);
        entity.setProduct(product);
        return entity;
    }
}