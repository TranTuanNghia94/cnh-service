package com.cnh.ies.model.export;

import java.time.LocalDate;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateExportJobRequest {

    /** PRODUCTS, VENDORS, CUSTOMERS, WAREHOUSE_INVENTORY, or SERVICE_* report types */
    @NotBlank
    private String type;

    /** Required for SERVICE_* report export types. */
    private LocalDate fromDate;

    /** Required for SERVICE_* report export types. */
    private LocalDate toDate;

    /** Optional report filters (status, createdBy, document numbers, etc.). */
    private Map<String, String> filters;
}
