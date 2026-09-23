package com.cnh.ies.model.report;

import com.cnh.ies.model.general.ApiRequestModel;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ServiceReportPagedRequest extends ApiRequestModel {

    @NotNull
    private java.time.LocalDate fromDate;

    @NotNull
    private java.time.LocalDate toDate;
}
