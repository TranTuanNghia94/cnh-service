package com.cnh.ies.model.export;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportNotificationMetadata {

    public static final String KIND = "EXPORT_JOB";
    public static final int SCHEMA_VERSION = 1;

    @Builder.Default
    private String kind = KIND;

    @Builder.Default
    private int schemaVersion = SCHEMA_VERSION;

    private String jobId;
    private String type;
    private String fileName;
    private String downloadUrl;
}
