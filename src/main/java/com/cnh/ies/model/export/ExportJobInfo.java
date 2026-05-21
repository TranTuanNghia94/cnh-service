package com.cnh.ies.model.export;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExportJobInfo {
    private String id;
    private String type;
    private String status;
    private String fileName;
    private String startedAt;
    private String finishedAt;
    private String errorMessage;
    private String createdBy;
    /** Pre-signed S3 download URL when status is SUCCESS. */
    private String viewUrl;
}
