package com.cnh.ies.dto.response;

import java.util.List;

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
}
