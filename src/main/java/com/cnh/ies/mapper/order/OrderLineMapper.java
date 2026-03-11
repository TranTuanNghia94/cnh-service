package com.cnh.ies.mapper.order;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.cnh.ies.entity.order.OrderEntity;
import com.cnh.ies.entity.order.OrderLineEntity;
import com.cnh.ies.mapper.product.ProductMapper;
import com.cnh.ies.mapper.vendors.VendorsMapper;
import com.cnh.ies.model.general.OrderLineRequestModel;
import com.cnh.ies.model.order.CreateOrderLineRequest;
import com.cnh.ies.model.order.OrderLineInfo;
import com.cnh.ies.model.order.UpdateOrderLineRequest;
import com.cnh.ies.util.RequestContext;

@Component
public class OrderLineMapper {
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private VendorsMapper vendorMapper;

    public OrderLineInfo toOrderLineInfo(OrderLineEntity e) {
        OrderLineInfo info = new OrderLineInfo();
        info.setId(e.getId() != null ? e.getId().toString() : null);
        info.setOrderId(e.getOrder() != null ? e.getOrder().getId().toString() : null);
        info.setProductId(e.getProduct() != null ? e.getProduct().getId().toString() : null);
        info.setProductName(e.getProduct() != null ? e.getProduct().getName() : null);
        info.setProductCodeSuggest(e.getProductCodeSuggest());
        info.setProductNameSuggest(e.getProductNameSuggest());
        info.setProduct(productMapper.toProductInfo(e.getProduct()));

        boolean hasVendor = e.getVendor() != null;
        info.setVendorId(hasVendor ? e.getVendor().getId().toString() : null);
        info.setVendorName(hasVendor ? e.getVendor().getName() : e.getVendorNameSuggest());
        info.setVendorCodeSuggest(hasVendor ? e.getVendor().getCode() : e.getVendorCodeSuggest());
        info.setVendorNameSuggest(hasVendor ? e.getVendor().getName() : e.getVendorNameSuggest());
        info.setVendor(hasVendor ? vendorMapper.toVendorInfo(e.getVendor()) : null);

        info.setQuantity(e.getQuantity());
        info.setUnitPrice(e.getUnitPrice());
        info.setUom(e.getUom());
        info.setDiscountPercent(e.getDiscountPercent());
        info.setDiscountAmount(e.getDiscountAmount());
        info.setIsIncludedTax(e.getIsIncludedTax());
        info.setTaxRate(e.getTaxRate());
        info.setTaxAmount(e.getTaxAmount());
        info.setTotalAmount(e.getTotalAmount());
        info.setNotes(e.getNotes());
        info.setReceiverNote(e.getReceiverNote());
        info.setDeliveryNote(e.getDeliveryNote());
        info.setReferenceNote(e.getReferenceNote());
        info.setCreatedAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null);
        info.setUpdatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null);
        info.setCreatedBy(e.getCreatedBy());
        info.setUpdatedBy(e.getUpdatedBy());
        return info;
    }

    public OrderLineEntity toOrderLineEntity(CreateOrderLineRequest request, OrderEntity order) {
        OrderLineEntity entity = buildCommon(request, order);
        String currentUser = RequestContext.getCurrentUsername();
        entity.setCreatedBy(currentUser);
        entity.setUpdatedBy(currentUser);
        return entity;
    }

    public OrderLineEntity toOrderLineEntity(UpdateOrderLineRequest request, OrderEntity order) {
        OrderLineEntity entity = buildCommon(request, order);

        if (request.getIsDeleted() != null) {
            entity.setIsDeleted(false);
        }

        entity.setUpdatedBy(RequestContext.getCurrentUsername());
        return entity;
    }

    public void applyUpdate(UpdateOrderLineRequest request, OrderLineEntity entity) {
        entity.setProductCodeSuggest(request.getProductCodeSuggest());
        entity.setProductNameSuggest(request.getProductNameSuggest());
        entity.setVendorCodeSuggest(request.getVendorCodeSuggest());
        entity.setVendorNameSuggest(request.getVendorNameSuggest());
        entity.setQuantity(request.getQuantity());
        entity.setUnitPrice(request.getUnitPrice());
        entity.setUom(request.getUom());
        entity.setDiscountPercent(request.getDiscountPercent());
        entity.setDiscountAmount(request.getDiscountAmount());
        entity.setIsIncludedTax(request.getIsIncludedTax());
        entity.setTaxRate(request.getTaxRate());
        entity.setTaxAmount(request.getTaxAmount());
        entity.setTotalAmount(request.getTotalAmount());
        entity.setNotes(request.getNotes());
        entity.setReceiverNote(orNull(request.getReceiverNote()));
        entity.setDeliveryNote(orNull(request.getDeliveryNote()));
        entity.setReferenceNote(orNull(request.getReferenceNote()));
        entity.setUpdatedBy(RequestContext.getCurrentUsername());
    }

    public List<OrderLineInfo> toOrderLineInfos(List<OrderLineEntity> orderLines) {
        return orderLines.stream().map(this::toOrderLineInfo).collect(Collectors.toList());
    }

    private OrderLineEntity buildCommon(OrderLineRequestModel request, OrderEntity order) {
        OrderLineEntity entity = new OrderLineEntity();
        entity.setOrder(order);
        entity.setIsDeleted(false);
        entity.setVersion(1L);
        entity.setProductCodeSuggest(request.getProductCodeSuggest());
        entity.setProductNameSuggest(request.getProductNameSuggest());
        entity.setVendorCodeSuggest(request.getVendorCodeSuggest());
        entity.setVendorNameSuggest(request.getVendorNameSuggest());
        entity.setQuantity(request.getQuantity());
        entity.setUnitPrice(request.getUnitPrice());
        entity.setUom(request.getUom());
        entity.setDiscountPercent(request.getDiscountPercent());
        entity.setDiscountAmount(request.getDiscountAmount());
        entity.setIsIncludedTax(request.getIsIncludedTax());
        entity.setTaxRate(request.getTaxRate());
        entity.setTaxAmount(request.getTaxAmount());
        entity.setTotalAmount(request.getTotalAmount());
        entity.setNotes(request.getNotes());
        entity.setReceiverNote(orNull(request.getReceiverNote()));
        entity.setDeliveryNote(orNull(request.getDeliveryNote()));
        entity.setReferenceNote(orNull(request.getReferenceNote()));
        return entity;
    }

    private String orNull(Optional<String> value) {
        return value != null ? value.orElse(null) : null;
    }
}
