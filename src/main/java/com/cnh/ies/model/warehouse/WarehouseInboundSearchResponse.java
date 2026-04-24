package com.cnh.ies.model.warehouse;

import java.util.List;

import lombok.Data;

@Data
public class WarehouseInboundSearchResponse {
    private List<WarehouseInboundSearchHit> hits;
}
