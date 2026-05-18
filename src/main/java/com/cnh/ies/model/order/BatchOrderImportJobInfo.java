package com.cnh.ies.model.order;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BatchOrderImportJobInfo {
    private String id;
    private String status;
    private String originalFileName;
    private Integer totalRows;
    private Integer successRows;
    private Integer errorRows;
    private Integer warningRows;
    private String startedAt;
    private String finishedAt;
    private String errorMessage;
    private String createdBy;
    /** Structured import outcome for tables / detail panels. */
    private BatchOrderImportResultSummary resultSummary;
}
