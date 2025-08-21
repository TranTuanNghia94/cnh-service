package com.cnh.ies.dto.common;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.Data;

@Data
public class PaginationRequest {
    
    @Min(value = 1, message = "Page number must be at least 1")
    private Integer page = 1;
    
    @Min(value = 1, message = "Page size must be at least 1")
    @Max(value = 100, message = "Page size cannot exceed 100")
    private Integer size = 20;
    
    private String sortBy = "id";
    
    private String sortDirection = "DESC";
    
    private String search;
    
    public int getOffset() {
        return (page - 1) * size;
    }
}
