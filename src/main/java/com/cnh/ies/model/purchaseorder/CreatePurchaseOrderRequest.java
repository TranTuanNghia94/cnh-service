package com.cnh.ies.model.purchaseorder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.cnh.ies.constant.Constant;

import lombok.Data;

@Data
public class CreatePurchaseOrderRequest {
    private Optional<String> id;
    private String orderId;
    private LocalDate orderDate;
    private LocalDate expectedDeliveryDate;
    private String status = Constant.PO_STATUS_DRAFT;
    private String notes;
    private List<CreatePurchaseOrderLineRequest> purchaseOrderLines;
}
