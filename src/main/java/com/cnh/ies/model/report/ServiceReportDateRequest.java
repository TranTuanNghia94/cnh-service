package com.cnh.ies.model.report;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceReportDateRequest {

    @NotNull
    private LocalDate fromDate;

    @NotNull
    private LocalDate toDate;
}
