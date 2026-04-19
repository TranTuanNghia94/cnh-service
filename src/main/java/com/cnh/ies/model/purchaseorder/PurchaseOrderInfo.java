package com.cnh.ies.model.purchaseorder;

import java.time.LocalDate;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import com.cnh.ies.model.order.OrderInfo;

import lombok.Data;

@Data
public class PurchaseOrderInfo {
    private String id;
    private Integer poNumber;
    private String poPrefix;
    private String orderId;
    private LocalDate orderDate;
    private LocalDate expectedDeliveryDate;
    private String status;
    private String notes;
    private String createdAt;
    private String updatedAt;
    private String deletedAt;
    private String createdBy;
    private String updatedBy;
    private Set<PurchaseOrderLineInfo> purchaseOrderLines = new CopyOnWriteArraySet<>();
    private OrderInfo order;
}
