package com.cnh.ies.model.export;

import java.time.LocalDate;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceReportExportParams {

    private LocalDate fromDate;
    private LocalDate toDate;
    private Map<String, String> filters;
}
