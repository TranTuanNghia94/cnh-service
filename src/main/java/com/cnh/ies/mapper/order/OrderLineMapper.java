package com.cnh.ies.mapper.order;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.cnh.ies.entity.order.OrderEntity;
import com.cnh.ies.entity.order.OrderLineEntity;
import com.cnh.ies.model.order.CreateOrderLineRequest;
import com.cnh.ies.model.order.OrderLineInfo;
import com.cnh.ies.util.RequestContext;

@Component
public class OrderLineMapper {
    
    public OrderLineInfo toOrderLineInfo(OrderLineEntity orderLineEntity) {
        OrderLineInfo orderLineInfo = new OrderLineInfo();
        orderLineInfo.setId(orderLineEntity.getId().toString());
        orderLineInfo.setOrderId(orderLineEntity.getOrder().getId().toString());
        orderLineInfo.setProductId(orderLineEntity.getProduct().getId().toString());
        orderLineInfo.setProductName(orderLineEntity.getProduct().getName());
        orderLineInfo.setVendorId(orderLineEntity.getVendor().getId().toString());
        orderLineInfo.setVendorName(orderLineEntity.getVendor().getName());
        orderLineInfo.setProductCodeSuggest(orderLineEntity.getProductCodeSuggest());
        orderLineInfo.setProductNameSuggest(orderLineEntity.getProductNameSuggest());
        orderLineInfo.setVendorCodeSuggest(orderLineEntity.getVendorCodeSuggest());
        orderLineInfo.setVendorNameSuggest(orderLineEntity.getVendorNameSuggest());
        orderLineInfo.setQuantity(orderLineEntity.getQuantity());
        orderLineInfo.setUnitPrice(orderLineEntity.getUnitPrice());
        orderLineInfo.setUom(orderLineEntity.getUom());
        orderLineInfo.setDiscountPercent(orderLineEntity.getDiscountPercent());
        orderLineInfo.setDiscountAmount(orderLineEntity.getDiscountAmount());
        orderLineInfo.setIsIncludedTax(orderLineEntity.getIsIncludedTax());
        orderLineInfo.setTaxRate(orderLineEntity.getTaxRate());
        orderLineInfo.setTaxAmount(orderLineEntity.getTaxAmount());
        orderLineInfo.setTotalAmount(orderLineEntity.getTotalAmount());
        orderLineInfo.setNotes(orderLineEntity.getNotes());
        orderLineInfo.setReceiverNote(orderLineEntity.getReceiverNote());
        orderLineInfo.setDeliveryNote(orderLineEntity.getDeliveryNote());
        orderLineInfo.setReferenceNote(orderLineEntity.getReferenceNote());
        orderLineInfo.setCreatedAt(orderLineEntity.getCreatedAt().toString());
        orderLineInfo.setUpdatedAt(orderLineEntity.getUpdatedAt().toString());
        orderLineInfo.setCreatedBy(orderLineEntity.getCreatedBy());
        orderLineInfo.setUpdatedBy(orderLineEntity.getUpdatedBy());

        return orderLineInfo;
    }

    public OrderLineEntity toOrderLineEntity(CreateOrderLineRequest createOrderLineRequest, OrderEntity order) {
        OrderLineEntity orderLineEntity = new OrderLineEntity();
        orderLineEntity.setOrder(order);
        orderLineEntity.setIsDeleted(false);
        orderLineEntity.setVersion(1L);
        orderLineEntity.setProductCodeSuggest(createOrderLineRequest.getProductCodeSuggest());
        orderLineEntity.setProductNameSuggest(createOrderLineRequest.getProductNameSuggest());
        orderLineEntity.setVendorCodeSuggest(createOrderLineRequest.getVendorCodeSuggest());
        orderLineEntity.setVendorNameSuggest(createOrderLineRequest.getVendorNameSuggest());
        orderLineEntity.setQuantity(createOrderLineRequest.getQuantity());
        orderLineEntity.setUnitPrice(createOrderLineRequest.getUnitPrice());
        orderLineEntity.setUom(createOrderLineRequest.getUom());
        orderLineEntity.setDiscountPercent(createOrderLineRequest.getDiscountPercent());
        orderLineEntity.setDiscountAmount(createOrderLineRequest.getDiscountAmount());
        orderLineEntity.setIsIncludedTax(createOrderLineRequest.getIsIncludedTax());
        orderLineEntity.setTaxRate(createOrderLineRequest.getTaxRate());
        orderLineEntity.setTaxAmount(createOrderLineRequest.getTaxAmount());
        orderLineEntity.setTotalAmount(createOrderLineRequest.getTotalAmount());
        orderLineEntity.setNotes(createOrderLineRequest.getNotes());
        orderLineEntity.setReceiverNote(orNull(createOrderLineRequest.getReceiverNote()));
        orderLineEntity.setDeliveryNote(orNull(createOrderLineRequest.getDeliveryNote()));
        orderLineEntity.setReferenceNote(orNull(createOrderLineRequest.getReferenceNote()));
        String currentUser = RequestContext.getCurrentUsername();
        orderLineEntity.setCreatedBy(currentUser);
        orderLineEntity.setUpdatedBy(currentUser);

        return orderLineEntity;
    }
    
    private String orNull(Optional<String> value) {
        return value != null ? value.orElse(null) : null;
    }
    
}
