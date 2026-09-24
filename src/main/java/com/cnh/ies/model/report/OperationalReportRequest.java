package com.cnh.ies.model.report;

import java.time.LocalDate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class OperationalReportRequest {

    @Min(1)
    @Max(12)
    private Integer month;

    @Min(2000)
    @Max(2100)
    private Integer year;

    private LocalDate fromDate;

    private LocalDate toDate;

    private String productName;

    private String productCode;

    private String contractNumber;

    private String userName;

    private String vendor;

    private String customer;

    private String documentNumber;

    @Min(0)
    private Integer page = 0;

    @Min(1)
    @Max(500)
    private Integer limit = 20;
}
