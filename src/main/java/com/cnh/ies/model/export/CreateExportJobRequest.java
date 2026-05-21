package com.cnh.ies.model.export;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateExportJobRequest {

    /** PRODUCTS, VENDORS, CUSTOMERS, WAREHOUSE_INVENTORY */
    @NotBlank
    private String type;
}
