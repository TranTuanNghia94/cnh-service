package com.cnh.ies.model.report;

import com.cnh.ies.model.general.ListDataModel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceReportDetailResponse<T, S> {

    private ServiceReportDateRangeInfo dateRange;
    private S summary;
    private ListDataModel<T> rows;
}
