package com.cnh.ies.model.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductTaxHistoryInfo {

    private String id;
    private String productId;
    private String oldTax;
    private String newTax;
    private String sourceType;
    private String sourceId;
    private String note;
    private String createdAt;
    private String createdBy;
}
