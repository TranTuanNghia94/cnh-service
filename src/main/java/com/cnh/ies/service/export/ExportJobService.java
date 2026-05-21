package com.cnh.ies.service.export;

import java.time.Instant;
import java.util.List;
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
    public String createAndDispatch(String typeRaw, UUID ownerUserId, String createdBy, String requestId) {
        String type = ExportJobType.normalize(typeRaw, requestId);
        String actor = resolveActor(createdBy);

        ExportJobEntity job = new ExportJobEntity();
        job.setOwnerUserId(ownerUserId);
        job.setType(type);
        job.setStatus(ExportJobStatus.PENDING);
        job.setCreatedBy(actor);
        job.setUpdatedBy(actor);
        job = exportJobRepo.save(job);

        UUID jobId = job.getId();
        dispatchAfterCommit(jobId, ownerUserId, type, actor, requestId);
        return jobId.toString();
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
            String fileName,
            String downloadUrl) {
        try {
            ExportNotificationMetadata metadata = ExportNotificationMetadata.builder()
                    .jobId(jobId.toString())
                    .type(type)
                    .fileName(fileName)
                    .downloadUrl(downloadUrl)
                    .build();
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize export notification metadata for job {}: {}", jobId, e.getMessage());
            return null;
        }
    }

    public String buildSuccessNotificationMessage(String type, String fileName) {
        return String.format("Xuất Excel %s thành công. File: %s",
                displayTypeName(type), fileName != null ? fileName : "");
    }

    public String buildFailureNotificationMessage(String message) {
        return message != null && !message.isBlank() ? message : "Xuất Excel thất bại";
    }

    private static String displayTypeName(String type) {
        return switch (type) {
            case ExportJobType.PRODUCTS -> "sản phẩm";
            case ExportJobType.VENDORS -> "nhà cung cấp";
            case ExportJobType.CUSTOMERS -> "khách hàng";
            case ExportJobType.WAREHOUSE_INVENTORY -> "tồn kho";
            default -> type;
        };
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
