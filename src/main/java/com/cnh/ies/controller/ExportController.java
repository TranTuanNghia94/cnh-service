package com.cnh.ies.controller;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cnh.ies.dto.common.ApiResponse;
import com.cnh.ies.model.export.CreateExportJobRequest;
import com.cnh.ies.model.export.ExportJobInfo;
import com.cnh.ies.model.general.ListDataModel;
import com.cnh.ies.service.export.ExportJobService;
import com.cnh.ies.service.security.AuthenticationUserDetails;
import com.cnh.ies.util.RequestContext;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/export-jobs")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Export", description = "Async Excel export job APIs")
public class ExportController {

    private final ExportJobService exportJobService;

    @PostMapping
    public ApiResponse<String> createExportJob(
            @Valid @RequestBody CreateExportJobRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        String requestId = RequestContext.getRequestIdOrGenerate();
        UUID ownerUserId = getCurrentUserId(userDetails);
        String createdBy = getCurrentUsername(userDetails);
        String jobId = exportJobService.createAndDispatch(
                request.getType(), ownerUserId, createdBy, requestId);
        return ApiResponse.success(jobId, "Export job created");
    }

    @GetMapping
    public ApiResponse<ListDataModel<ExportJobInfo>> listExportJobs(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer limit,
            @AuthenticationPrincipal UserDetails userDetails) {
        String requestId = RequestContext.getRequestIdOrGenerate();
        UUID ownerUserId = getCurrentUserId(userDetails);
        ListDataModel<ExportJobInfo> response =
                exportJobService.listOwnedJobs(ownerUserId, page, limit, requestId);
        return ApiResponse.success(response, "List export jobs success");
    }

    @GetMapping("/{jobId}")
    public ApiResponse<ExportJobInfo> getExportJob(
            @PathVariable String jobId,
            @AuthenticationPrincipal UserDetails userDetails) {
        String requestId = RequestContext.getRequestIdOrGenerate();
        UUID ownerUserId = getCurrentUserId(userDetails);
        ExportJobInfo response = exportJobService.getOwnedJob(jobId, ownerUserId, requestId);
        return ApiResponse.success(response, "Get export job success");
    }

    private UUID getCurrentUserId(UserDetails userDetails) {
        if (userDetails instanceof AuthenticationUserDetails aud) {
            return aud.getUserId();
        }
        throw new IllegalStateException("Unexpected principal type: " + userDetails.getClass().getName());
    }

    private String getCurrentUsername(UserDetails userDetails) {
        if (userDetails instanceof AuthenticationUserDetails aud) {
            String username = aud.getUsername();
            if (username != null && !username.isBlank()) {
                return username;
            }
        }
        if (userDetails != null) {
            String username = userDetails.getUsername();
            if (username != null && !username.isBlank() && !"anonymousUser".equals(username)) {
                return username;
            }
        }
        return null;
    }
}
