package com.cnh.ies.model.order;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Structured batch import outcome for notifications ({@code metadata}) and job API ({@code resultSummary}).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchOrderImportResultSummary {

    private int totalRows;
    private int ordersCreatedCount;
    private int newProductsCount;
    private int newVendorsCount;
    private int warningCount;
    private int errorCount;

    @Builder.Default
    private List<BatchOrderImportOrderCreated> ordersCreated = new ArrayList<>();

    @Builder.Default
    private List<BatchOrderImportEntityItem> newProducts = new ArrayList<>();

    @Builder.Default
    private List<BatchOrderImportEntityItem> newVendors = new ArrayList<>();

    @Builder.Default
    private List<BatchOrderImportErrorItem> errors = new ArrayList<>();

    @Builder.Default
    private List<String> warnings = new ArrayList<>();
}
