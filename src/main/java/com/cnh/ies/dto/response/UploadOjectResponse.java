package com.cnh.ies.dto.response;

import java.util.List;

import com.cnh.ies.model.order.BatchOrderImportResultSummary;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadOjectResponse {
    private String message;
    private int totalRows;
    private int totalSuccess;
    private int totalErrors;
    private List<String> errors;
    /**
     * Non-fatal notices (e.g. auto-created products/vendors during batch order import).
     */
    private List<String> warnings;

    /** Populated for batch order import with structured data for notifications and job UI. */
    private BatchOrderImportResultSummary importSummary;

    public UploadOjectResponse(String message, int totalRows, int totalSuccess, int totalErrors,
            List<String> errors, List<String> warnings) {
        this.message = message;
        this.totalRows = totalRows;
        this.totalSuccess = totalSuccess;
        this.totalErrors = totalErrors;
        this.errors = errors;
        this.warnings = warnings;
    }
}
