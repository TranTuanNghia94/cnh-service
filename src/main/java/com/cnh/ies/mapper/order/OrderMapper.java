package com.cnh.ies.mapper.order;

import org.springframework.stereotype.Component;

import com.cnh.ies.entity.customer.CustomerAddressEntity;
import com.cnh.ies.entity.customer.CustomerEntity;
import com.cnh.ies.entity.order.OrderEntity;
import com.cnh.ies.model.order.CreateOrderRequest;
import com.cnh.ies.model.order.OrderInfo;
import com.cnh.ies.util.RequestContext;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OrderMapper {

    public OrderInfo toOrderInfo(OrderEntity order) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(order.getId().toString());
        orderInfo.setOrderNumber(order.getOrderNumber());
        orderInfo.setOrderPrefix(order.getOrderPrefix());
        orderInfo.setCustomerName(order.getCustomer().getName());
        orderInfo.setContractNumber(order.getContractNumber());
        orderInfo.setOrderDate(order.getOrderDate());
        orderInfo.setDeliveryDate(order.getDeliveryDate());
        orderInfo.setStatus(order.getStatus());
        orderInfo.setTotalAmount(order.getTotalAmount());
        orderInfo.setDiscountAmount(order.getDiscountAmount());
        orderInfo.setTaxAmount(order.getTaxAmount());
        orderInfo.setFinalAmount(order.getFinalAmount());
        orderInfo.setNotes(order.getNotes());
        orderInfo.setCreatedAt(order.getCreatedAt().toString());
        orderInfo.setUpdatedAt(order.getUpdatedAt().toString());
        orderInfo.setCreatedBy(order.getCreatedBy());
        orderInfo.setUpdatedBy(order.getUpdatedBy());

        return orderInfo;
    }

    public OrderEntity toOrderEntity(CreateOrderRequest createOrderRequest, CustomerEntity customer, CustomerAddressEntity customerAddress) {
        OrderEntity order = new OrderEntity();
        order.setCustomer(customer);
        order.setVersion(1L);
        order.setCustomerAddress(customerAddress);
        order.setContractNumber(createOrderRequest.getContractNumber());
        order.setOrderDate(createOrderRequest.getOrderDate());
        order.setDeliveryDate(createOrderRequest.getDeliveryDate());
        order.setStatus(createOrderRequest.getStatus());
        order.setTotalAmount(createOrderRequest.getTotalAmount());
        order.setDiscountAmount(createOrderRequest.getDiscountAmount());
        order.setIsIncludedTax(false);
        order.setTaxAmount(createOrderRequest.getTaxAmount());
        order.setFinalAmount(createOrderRequest.getFinalAmount());
        order.setNotes(createOrderRequest.getNotes());
        order.setCreatedBy(RequestContext.getCurrentUsername());
        order.setUpdatedBy(RequestContext.getCurrentUsername());
        order.setIsDeleted(false);
        
        return order;
    }
    

}
