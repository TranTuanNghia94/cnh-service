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
public class UploadProductResponse {
    private String message;
    private List<String> errors;
    private int totalRows;
    private int totalErrors;
    private int totalSuccess;
}
