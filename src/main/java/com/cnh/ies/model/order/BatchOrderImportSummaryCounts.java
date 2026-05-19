package com.cnh.ies.model.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Compact import counts for notification metadata (no order/vendor/product lists).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchOrderImportSummaryCounts {

    private int totalRows;
    private int ordersCreatedCount;
    private int newProductsCount;
    private int newVendorsCount;
    private int warningCount;
    private int errorCount;

    public static BatchOrderImportSummaryCounts from(BatchOrderImportResultSummary summary) {
        if (summary == null) {
            return null;
        }
        return BatchOrderImportSummaryCounts.builder()
                .totalRows(summary.getTotalRows())
                .ordersCreatedCount(summary.getOrdersCreatedCount())
                .newProductsCount(summary.getNewProductsCount())
                .newVendorsCount(summary.getNewVendorsCount())
                .warningCount(summary.getWarningCount())
                .errorCount(summary.getErrorCount())
                .build();
    }
}
