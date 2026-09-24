package com.cnh.ies.model.report;

import java.time.LocalDate;
import java.util.List;

import com.cnh.ies.model.general.PaginationModel;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OperationalReportPage<T> {
    private LocalDate fromDate;
    private LocalDate toDate;
    private List<T> data;
    private PaginationModel pagination;
}
