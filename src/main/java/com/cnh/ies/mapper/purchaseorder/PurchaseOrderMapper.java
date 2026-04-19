package com.cnh.ies.mapper.purchaseorder;

import org.springframework.stereotype.Component;

import com.cnh.ies.entity.order.OrderEntity;
import com.cnh.ies.entity.purchaseorder.PurchaseOrderEntity;
import com.cnh.ies.model.purchaseorder.CreatePurchaseOrderRequest;
import com.cnh.ies.model.purchaseorder.PurchaseOrderInfo;
import com.cnh.ies.util.RequestContext;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PurchaseOrderMapper {

    public PurchaseOrderInfo toPurchaseOrderInfo(PurchaseOrderEntity po) {
        PurchaseOrderInfo info = new PurchaseOrderInfo();
        info.setId(po.getId().toString());
        info.setPoNumber(po.getPoNumber());
        info.setPoPrefix(po.getPoPrefix());
        info.setOrderId(po.getOrder() != null ? po.getOrder().getId().toString() : null);
        info.setOrderDate(po.getOrderDate());
        info.setExpectedDeliveryDate(po.getExpectedDeliveryDate());
        info.setStatus(po.getStatus());
        info.setNotes(po.getNotes());
        info.setCreatedAt(po.getCreatedAt() != null ? po.getCreatedAt().toString() : null);
        info.setUpdatedAt(po.getUpdatedAt() != null ? po.getUpdatedAt().toString() : null);
        info.setCreatedBy(po.getCreatedBy());
        info.setUpdatedBy(po.getUpdatedBy());
        return info;
    }

    public PurchaseOrderEntity toPurchaseOrderEntity(CreatePurchaseOrderRequest request, OrderEntity order) {
        PurchaseOrderEntity entity = new PurchaseOrderEntity();
        entity.setOrder(order);
        entity.setVersion(1L);
        entity.setOrderDate(request.getOrderDate());
        entity.setExpectedDeliveryDate(request.getExpectedDeliveryDate());
        entity.setStatus(request.getStatus());
        entity.setNotes(request.getNotes());
        entity.setCreatedBy(RequestContext.getCurrentUsername());
        entity.setUpdatedBy(RequestContext.getCurrentUsername());
        entity.setIsDeleted(false);
        return entity;
    }
}
