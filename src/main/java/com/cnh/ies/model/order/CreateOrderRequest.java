package com.cnh.ies.model.order;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.List;

import lombok.Data;

@Data
public class CreateOrderRequest {
    private Optional<String> id;
    private String customerId;
    private String customerAddressId;
    private String contractNumber;
    private LocalDate orderDate;
    private LocalDate deliveryDate;
    private String status;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal finalAmount;
    private String notes;
    private List<CreateOrderLineRequest> orderLines;
}
