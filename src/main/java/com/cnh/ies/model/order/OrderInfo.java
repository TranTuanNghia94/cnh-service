package com.cnh.ies.model.order;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
    
import lombok.Data;

@Data
public class OrderInfo {
    private String id;
    private Integer orderNumber;
    private String orderPrefix;
    private String customerName;
    private String contractNumber;
    private LocalDate orderDate;
    private LocalDate deliveryDate;
    private String status;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal finalAmount;
    private String notes;
    private String createdAt;
    private String updatedAt;
    private String deletedAt;
    private String createdBy;
    private String updatedBy;
    private Set<OrderLineInfo> orderLines = new CopyOnWriteArraySet<>();
}
