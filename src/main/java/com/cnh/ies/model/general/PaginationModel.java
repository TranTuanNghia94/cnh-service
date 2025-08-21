package com.cnh.ies.model.general;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginationModel {
    private Integer page;
    private Integer limit;
    private Long total;
    private Integer totalPage;
}
