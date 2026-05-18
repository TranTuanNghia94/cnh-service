package com.cnh.ies.model.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchOrderImportOrderCreated {
    private String orderId;
    /** Display code e.g. {@code 202505.42} */
    private String orderCode;
    private String contractNumber;
    private String customerCode;
    private Integer lineCount;
}
