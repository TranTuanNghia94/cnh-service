package com.cnh.ies.model.report;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class WarehouseInventoryReportRequest extends ServiceReportPagedRequest {

    private String productCode;
    private String productName;
    private String productCategory;
    private String direction;
}
