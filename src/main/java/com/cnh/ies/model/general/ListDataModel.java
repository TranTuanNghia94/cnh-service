package com.cnh.ies.model.general;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListDataModel<T> {
    private List<T> data;
    private PaginationModel pagination;
}
