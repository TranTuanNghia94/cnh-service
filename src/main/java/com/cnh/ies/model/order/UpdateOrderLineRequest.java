package com.cnh.ies.model.order;

import com.cnh.ies.model.general.OrderLineRequestModel;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class UpdateOrderLineRequest extends OrderLineRequestModel {
    private String id;
    private String productId;
    private String vendorId;
}
