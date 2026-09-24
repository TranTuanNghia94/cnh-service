package com.cnh.ies.service.export;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.cnh.ies.entity.export.ExportJobEntity;
import com.cnh.ies.exception.ApiException;
import com.cnh.ies.model.export.ExportJobInfo;
import com.cnh.ies.model.export.ExportNotificationMetadata;
import com.cnh.ies.model.export.ServiceReportExportParams;
import com.cnh.ies.model.general.ListDataModel;
import com.cnh.ies.model.general.PaginationModel;
import com.cnh.ies.repository.export.ExportJobRepo;
import com.cnh.ies.repository.file.FileInfoRepo;
import com.cnh.ies.service.file.FileService;
import com.cnh.ies.util.RequestContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ExportJobService {

    private final ExportJobRepo exportJobRepo;
    private final FileInfoRepo fileInfoRepo;
    private final FileService fileService;
    private final ExportJobWorkerService workerService;
    private final ObjectMapper objectMapper;

    public ExportJobService(
            ExportJobRepo exportJobRepo,
            FileInfoRepo fileInfoRepo,
            FileService fileService,
            ObjectMapper objectMapper,
            @Lazy ExportJobWorkerService workerService) {
        this.exportJobRepo = exportJobRepo;
        this.fileInfoRepo = fileInfoRepo;
        this.fileService = fileService;
        this.objectMapper = objectMapper;
        this.workerService = workerService;
    }

    @Transactional
    public String createAndDispatch(
            String typeRaw,
            LocalDate fromDate,
            LocalDate toDate,
            Map<String, String> filters,
            UUID ownerUserId,
            String createdBy,
            String requestId) {
        String type = ExportJobType.normalize(typeRaw, requestId);
        ExportJobType.validateServiceReportParams(type, fromDate, toDate, requestId);
        ExportJobType.validateOperationalReportParams(type, fromDate, toDate, filters, requestId);
        String actor = resolveActor(createdBy);

        ExportJobEntity job = new ExportJobEntity();
        job.setOwnerUserId(ownerUserId);
        job.setType(type);
        job.setStatus(ExportJobStatus.PENDING);
        job.setCreatedBy(actor);
        job.setUpdatedBy(actor);
        if (ExportJobType.isServiceReportType(type) || ExportJobType.isOperationalReportType(type)) {
            job.setReportParams(serializeReportParams(fromDate, toDate, filters, requestId));
        }
        job = exportJobRepo.save(job);

        UUID jobId = job.getId();
        dispatchAfterCommit(jobId, ownerUserId, type, actor, requestId);
        return jobId.toString();
    }

    public ServiceReportExportParams loadReportParams(ExportJobEntity job, String requestId) {
        if (job.getReportParams() == null || job.getReportParams().isBlank()) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST,
                    "Export job is missing report parameters",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }
        try {
            return objectMapper.readValue(job.getReportParams(), ServiceReportExportParams.class);
        } catch (JsonProcessingException e) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST,
                    "Invalid report parameters on export job",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }
    }

    private String serializeReportParams(
            LocalDate fromDate, LocalDate toDate, Map<String, String> filters, String requestId) {
        try {
            ServiceReportExportParams params = ServiceReportExportParams.builder()
                    .fromDate(fromDate)
                    .toDate(toDate)
                    .filters(filters)
                    .build();
            return objectMapper.writeValueAsString(params);
        } catch (JsonProcessingException e) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST,
                    "Failed to serialize report parameters",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }
    }

    public ExportJobInfo getOwnedJob(String jobId, UUID ownerUserId, String requestId) {
        return toInfo(getOwnedJobEntity(jobId, ownerUserId, requestId));
    }

    public ListDataModel<ExportJobInfo> listOwnedJobs(
            UUID ownerUserId,
            Integer page,
            Integer limit,
            String requestId) {
        int safePage = page == null || page < 0 ? 0 : page;
        int safeLimit = limit == null || limit < 1 ? 20 : limit;
        Page<ExportJobEntity> jobs = exportJobRepo.findByOwnerUserIdAndIsDeletedFalseOrderByCreatedAtDesc(
                ownerUserId, PageRequest.of(safePage, safeLimit));
        List<ExportJobInfo> data = jobs.stream().map(this::toInfo).collect(Collectors.toList());
        PaginationModel pagination = PaginationModel.builder()
                .page(safePage)
                .limit(safeLimit)
                .total(jobs.getTotalElements())
                .totalPage(jobs.getTotalPages())
                .build();
        return ListDataModel.<ExportJobInfo>builder().data(data).pagination(pagination).build();
    }

    @Transactional
    void markRunning(UUID jobId) {
        exportJobRepo.findByIdAndIsDeletedFalse(jobId).ifPresent(job -> {
            job.setStatus(ExportJobStatus.RUNNING);
            job.setStartedAt(Instant.now());
            if (job.getCreatedBy() != null) {
                job.setUpdatedBy(job.getCreatedBy());
            }
            exportJobRepo.save(job);
        });
    }

    @Transactional
    void markSuccess(UUID jobId, UUID fileInfoId, String fileName) {
        ExportJobEntity job = exportJobRepo.findByIdAndIsDeletedFalse(jobId).orElse(null);
        if (job == null) {
            return;
        }
        job.setStatus(ExportJobStatus.SUCCESS);
        job.setFileInfoId(fileInfoId);
        job.setFileName(fileName);
        job.setFinishedAt(Instant.now());
        exportJobRepo.save(job);
    }

    @Transactional
    void markFailed(UUID jobId, String message) {
        ExportJobEntity job = exportJobRepo.findByIdAndIsDeletedFalse(jobId).orElse(null);
        if (job == null) {
            return;
        }
        job.setStatus(ExportJobStatus.FAILED);
        job.setErrorMessage(message);
        job.setFinishedAt(Instant.now());
        exportJobRepo.save(job);
    }

    public String buildNotificationMetadataJson(
            UUID jobId,
            String type,
            String status,
            String fileName,
            String resultUrl,
            String downloadUrl,
            String actionUrl) {
        try {
            ExportNotificationMetadata metadata = ExportNotificationMetadata.builder()
                    .jobId(jobId.toString())
                    .type(type)
                    .status(status)
                    .fileName(fileName)
                    .resultUrl(resultUrl)
                    .downloadUrl(downloadUrl)
                    .actionUrl(actionUrl)
                    .build();
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize export notification metadata for job {}: {}", jobId, e.getMessage());
            return null;
        }
    }

    public String buildSuccessNotificationMessage(String type, String fileName) {
        return String.format("Xuất Excel %s thành công. Nhấn để lấy file: %s",
                displayTypeName(type), fileName != null ? fileName : "");
    }

    public String buildFailureNotificationMessage(String message) {
        String detail = message != null && !message.isBlank() ? ". " + message : "";
        return "Xuất Excel thất bại. Nhấn để xem kết quả" + detail;
    }

    private static String displayTypeName(String type) {
        return ExportJobType.displayTypeName(type);
    }

    private void dispatchAfterCommit(UUID jobId, UUID ownerUserId, String type, String createdBy, String requestId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    workerService.processAsync(jobId, ownerUserId, type, createdBy, requestId);
                }
            });
        } else {
            workerService.processAsync(jobId, ownerUserId, type, createdBy, requestId);
        }
    }

    private static String resolveActor(String createdBy) {
        if (createdBy != null && !createdBy.isBlank()) {
            return createdBy.trim();
        }
        String fromContext = RequestContext.getCurrentUsername();
        if (fromContext != null && !fromContext.isBlank()) {
            return fromContext;
        }
        return "SYSTEM";
    }

    private ExportJobEntity getOwnedJobEntity(String jobId, UUID ownerUserId, String requestId) {
        UUID id;
        try {
            id = UUID.fromString(jobId);
        } catch (IllegalArgumentException e) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "Invalid job id",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }
        ExportJobEntity job = exportJobRepo.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ApiException(ApiException.ErrorCode.NOT_FOUND, "Export job not found",
                        HttpStatus.NOT_FOUND.value(), requestId));
        if (!job.getOwnerUserId().equals(ownerUserId)) {
            throw new ApiException(ApiException.ErrorCode.FORBIDDEN, "Not allowed to access this export job",
                    HttpStatus.FORBIDDEN.value(), requestId);
        }
        return job;
    }

    private ExportJobInfo toInfo(ExportJobEntity job) {
        String viewUrl = null;
        if (ExportJobStatus.SUCCESS.equals(job.getStatus()) && job.getFileInfoId() != null) {
            viewUrl = fileInfoRepo.findById(job.getFileInfoId())
                    .filter(f -> !Boolean.TRUE.equals(f.getIsDeleted()))
                    .map(f -> fileService.presignGetUrlForKey(f.getFilePath()))
                    .orElse(null);
        }
        return ExportJobInfo.builder()
                .id(job.getId().toString())
                .type(job.getType())
                .status(job.getStatus())
                .fileName(job.getFileName())
                .startedAt(job.getStartedAt() != null ? job.getStartedAt().toString() : null)
                .finishedAt(job.getFinishedAt() != null ? job.getFinishedAt().toString() : null)
                .errorMessage(job.getErrorMessage())
                .createdBy(job.getCreatedBy())
                .viewUrl(viewUrl)
                .build();
    }
}
