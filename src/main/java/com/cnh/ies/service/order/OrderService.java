package com.cnh.ies.service.order;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.cnh.ies.repository.order.OrderRepo;
import com.cnh.ies.repository.product.ProductRepo;
import com.cnh.ies.util.RequestContext;

import jakarta.transaction.Transactional;

import com.cnh.ies.repository.order.OrderLineRepo;
import com.cnh.ies.mapper.order.OrderMapper;
import com.cnh.ies.model.general.ListDataModel;
import com.cnh.ies.model.general.PaginationModel;
import com.cnh.ies.model.order.CreateOrderRequest;
import com.cnh.ies.model.order.CreateOrderLineRequest;
import com.cnh.ies.model.order.OrderInfo;
import com.cnh.ies.entity.customer.CustomerAddressEntity;
import com.cnh.ies.entity.customer.CustomerEntity;
import com.cnh.ies.entity.order.OrderEntity;
import com.cnh.ies.entity.order.OrderLineEntity;
import com.cnh.ies.entity.product.ProductEntity;
import com.cnh.ies.exception.ApiException;
import com.cnh.ies.mapper.order.OrderLineMapper;
import com.cnh.ies.repository.customer.CustomerRepo;
import com.cnh.ies.repository.customer.CustomerAddressRepo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    private final OrderRepo orderRepo;
    private final OrderLineRepo orderLineRepo;
    private final OrderMapper orderMapper;
    private final OrderLineMapper orderLineMapper;
    private final CustomerRepo customerRepo;
    private final ProductRepo productRepo;
    private final CustomerAddressRepo customerAddressRepo;
    private final OrderNumberService orderNumberService;

    public ListDataModel<OrderInfo> getAllOrders(String requestId, Integer page, Integer limit) {
        try {
            log.info("Getting all orders with requestId: {}", requestId);

            Page<OrderEntity> orders = orderRepo.findAllAndIsDeletedFalse(PageRequest.of(page, limit));
            List<OrderInfo> orderInfos = orders.stream().map(orderMapper::toOrderInfo).collect(Collectors.toList());

            PaginationModel pagination = PaginationModel.builder()
                    .page(page)
                    .limit(limit)
                    .total(orders.getTotalElements())
                    .totalPage(orders.getTotalPages())
                    .build();

            log.info("Getting all orders success with requestId: {} | total: {} totalPage: {}", requestId,
                    orders.getTotalElements(), orders.getTotalPages());

            return ListDataModel.<OrderInfo>builder()
                    .data(orderInfos)
                    .pagination(pagination)
                    .build();
        } catch (Exception e) {
            log.error("Error getting all orders", e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, "Error getting all orders",
                    HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }

    @Transactional
    public OrderInfo createOrder(CreateOrderRequest request, String requestId) {
        try {
            log.info("Creating order with 0/3 steps requestId: {} | request: {}", requestId, request);

            CustomerEntity customer = getCustomerOrThrow(request.getCustomerId(), requestId);
            CustomerAddressEntity customerAddress;
            if (request.getCustomerAddressId() != null) {
                customerAddress = getCustomerAddressOrThrow(request.getCustomerAddressId(),
                        requestId);
            } else {
                customerAddress = null;
            }

            BigDecimal amount = request.getOrderLines().stream().map(line -> line.getTotalAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);

            OrderEntity order = orderMapper.toOrderEntity(request, customer, customerAddress);
            order.setOrderNumber(orderNumberService.generateNextNumberOrReset());
            order.setOrderPrefix(orderNumberService.generateOrderPrefix());
            order.setFinalAmount(amount);

            OrderEntity savedOrder = orderRepo.save(order);
            log.info("Order created successfully with request 1/3: {}", requestId);

            if (request.getOrderLines() != null && !request.getOrderLines().isEmpty()) {
                log.info("Processing {} order lines for requestId: {}", request.getOrderLines().size(), requestId);

                Map<UUID, ProductEntity> productById = loadProductsById(request.getOrderLines());
                List<OrderLineEntity> orderLines = request.getOrderLines().stream()
                        .map(line -> toOrderLineEntity(line, savedOrder, productById, requestId))
                        .collect(Collectors.toList());

                orderLineRepo.saveAll(orderLines);
                log.info("Order lines created successfully with request 2/3: {}", requestId);
            }

            log.info("Order created successfully with request 3/3: {}", requestId);
            return orderMapper.toOrderInfo(savedOrder);
        } catch (Exception e) {
            log.error("Error creating order", e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, "Error creating order",
                    HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }

    public OrderInfo getOrderById(String id, String requestId) {
        try {
            log.info("Getting order by id with requestId: {} | id: {}", requestId, id);
            Optional<OrderEntity> order = orderRepo.findByIdAndIsDeletedFalse(UUID.fromString(id));
            if (order.isEmpty()) {
                log.error("Order not found with id: {} | RequestId: {}", id, requestId);
                throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "Order not found",
                        HttpStatus.NOT_FOUND.value(), requestId);
            }

            log.info("Order fetched successfully with requestId: {} | data: {}", requestId, order.get());

            return orderMapper.toOrderInfo(order.get());
        } catch (Exception e) {
            log.error("Error getting order by id", e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, "Error getting order by id",
                    HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }

    @Transactional
    public String deleteOrder(String id, String requestId) {
        try {
            log.info("Deleting order with 0/3 steps requestId: {} | id: {}", requestId, id);
            Optional<OrderEntity> order = orderRepo.findByIdAndIsDeletedFalse(UUID.fromString(id));
            if (order.isEmpty()) {
                log.error("Order not found with id: {} | RequestId: {}", id, requestId);
                throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "Order not found",
                        HttpStatus.NOT_FOUND.value(), requestId);
            }
            order.get().setIsDeleted(true);
            order.get().setUpdatedBy(RequestContext.getCurrentUsername());
            order.get().setContractNumber(order.get().getContractNumber() + "_" + "DELETED" + "_" + requestId);
            
            orderRepo.save(order.get());

            log.info("Order deleted successfully with request 1/3: {}", requestId);

            if (order.get().getOrderLines() != null) {
                List<OrderLineEntity> orderLines = order.get().getOrderLines().stream().map(orderLine -> {
                    orderLine.setIsDeleted(true);
                    orderLineRepo.save(orderLine);
                    return orderLine;
                }).collect(Collectors.toList());
                orderLineRepo.saveAll(orderLines);
                log.info("Order lines deleted successfully with request 2/3: {}", requestId);
            }

            log.info("Order deleted successfully with request 3/3: {}", requestId);

            return "Order deleted successfully";
        } catch (Exception e) {
            log.error("Error deleting order with requestId: {} | id: {}", requestId, id, e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, "Error deleting order",
                    HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }

    private CustomerEntity getCustomerOrThrow(String customerId, String requestId) {
        return customerRepo.findById(UUID.fromString(customerId)).orElseThrow(() -> {
            log.error("Customer not found with id: {} | RequestId: {}", customerId, requestId);
            return new ApiException(ApiException.ErrorCode.NOT_FOUND, "Customer not found",
                    HttpStatus.NOT_FOUND.value(), requestId);
        });
    }

    private CustomerAddressEntity getCustomerAddressOrThrow(String customerAddressId, String requestId) {
        return customerAddressRepo.findById(UUID.fromString(customerAddressId)).orElseThrow(() -> {
            log.error("Customer address not found with id: {} | RequestId: {}", customerAddressId, requestId);
            return new ApiException(ApiException.ErrorCode.NOT_FOUND, "Customer address not found",
                    HttpStatus.NOT_FOUND.value(), requestId);
        });
    }

    private Map<UUID, ProductEntity> loadProductsById(List<CreateOrderLineRequest> orderLines) {
        List<UUID> productIds = orderLines.stream()
                .map(l -> UUID.fromString(l.getProductId().get()))
                .collect(Collectors.toList());
        List<ProductEntity> products = productRepo.findByIdIn(productIds);
        return products.stream().collect(Collectors.toMap(ProductEntity::getId, Function.identity()));
    }

    private OrderLineEntity toOrderLineEntity(CreateOrderLineRequest line, OrderEntity order,
            Map<UUID, ProductEntity> productById, String requestId) {
        UUID productId = UUID.fromString(line.getProductId().get());
        ProductEntity product = Optional.ofNullable(productById.get(productId))
                .orElseThrow(() -> new ApiException(ApiException.ErrorCode.NOT_FOUND, "Product not found",
                        HttpStatus.NOT_FOUND.value(), requestId));
        OrderLineEntity entity = orderLineMapper.toOrderLineEntity(line, order);
        entity.setProduct(product);
        return entity;
    }

}
