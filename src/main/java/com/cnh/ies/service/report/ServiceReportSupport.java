package com.cnh.ies.service.report;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import com.cnh.ies.exception.ApiException;
import com.cnh.ies.model.general.ListDataModel;
import com.cnh.ies.model.general.PaginationModel;
import com.cnh.ies.model.report.ServiceReportDateRangeInfo;
import com.cnh.ies.model.report.StatusBreakdownItem;
import com.cnh.ies.repository.report.projection.StatusCountProjection;

public final class ServiceReportSupport {

    private static final ZoneId ZONE = ZoneId.systemDefault();

    private ServiceReportSupport() {}

    public static void validateDateRange(LocalDate fromDate, LocalDate toDate, String requestId) {
        if (fromDate == null || toDate == null) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST,
                    "fromDate and toDate are required", HttpStatus.BAD_REQUEST.value(), requestId);
        }
        if (fromDate.isAfter(toDate)) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST,
                    "fromDate must be on or before toDate", HttpStatus.BAD_REQUEST.value(), requestId);
        }
    }

    public static ServiceReportDateRangeInfo dateRange(LocalDate fromDate, LocalDate toDate) {
        return ServiceReportDateRangeInfo.builder().fromDate(fromDate).toDate(toDate).build();
    }

    public static Instant startOfDay(LocalDate date) {
        return date.atStartOfDay(ZONE).toInstant();
    }

    public static Instant endExclusive(LocalDate toDate) {
        return toDate.plusDays(1).atStartOfDay(ZONE).toInstant();
    }

    public static Pageable pageable(int page, int limit) {
        int safePage = Math.max(page, 0);
        int safeLimit = limit < 1 ? 10 : limit;
        return PageRequest.of(safePage, safeLimit);
    }

    public static <T> ListDataModel<T> pagedList(List<T> data, int page, int limit, long total) {
        int safeLimit = limit < 1 ? 10 : limit;
        int totalPage = safeLimit == 0 ? 0 : (int) Math.ceil((double) total / safeLimit);
        PaginationModel pagination = PaginationModel.builder()
                .page(page)
                .limit(safeLimit)
                .total(total)
                .totalPage(totalPage)
                .build();
        return ListDataModel.<T>builder().data(data).pagination(pagination).build();
    }

    public static List<StatusBreakdownItem> toStatusBreakdown(List<StatusCountProjection> rows) {
        return rows.stream()
                .map(r -> StatusBreakdownItem.builder()
                        .status(r.getStatus())
                        .count(r.getCount())
                        .build())
                .collect(Collectors.toList());
    }
}
