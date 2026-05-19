package com.cnh.ies.model.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Wrapper stored in {@code notifications.metadata} so the client can parse a stable shape.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchOrderImportNotificationMetadata {

    public static final String KIND = "BATCH_ORDER_IMPORT";
    public static final int SCHEMA_VERSION = 1;

    @Builder.Default
    private String kind = KIND;

    @Builder.Default
    private int schemaVersion = SCHEMA_VERSION;

    private String jobId;
    /** Counts only — full details via job API / import job details. */
    private BatchOrderImportSummaryCounts summary;
}
