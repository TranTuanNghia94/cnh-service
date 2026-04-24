package com.cnh.ies.model.general;

import com.fasterxml.jackson.annotation.JsonAlias;

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
    
    @JsonAlias("take")
    private Integer limit;
    
    private Integer skip;
    
    private String search;
    private String sortBy;
    private String sortOrder;

    /**
     * Get the page number. If page is null but skip/limit are provided,
     * calculate page from skip/limit.
     */
    public Integer getPage() {
        if (page != null) {
            return page;
        }
        if (skip != null && limit != null && limit > 0) {
            return skip / limit;
        }
        return 0;
    }

    /**
     * Get the limit (page size). Defaults to 10 if not provided.
     */
    public Integer getLimit() {
        if (limit != null) {
            return limit;
        }
        return 10;
    }
}
