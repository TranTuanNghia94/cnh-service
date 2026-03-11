package com.cnh.ies.model.order;

import java.util.Optional;

import com.cnh.ies.model.general.OrderLineRequestModel;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CreateOrderLineRequest extends OrderLineRequestModel {
    private Optional<String> id;
    private Optional<String> orderId;
    private Optional<String> productId;
    private Optional<String> vendorId;
}
