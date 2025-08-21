package com.cnh.ies.model.general;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiRequestModel {
    private Integer page;
    private Integer limit;
    private String search;
    private String sortBy;
    private String sortOrder;   
}
