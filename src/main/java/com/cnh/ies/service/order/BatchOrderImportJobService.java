package com.cnh.ies.service.order;

import java.time.Instant;
import java.util.ArrayList;
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
import org.springframework.web.multipart.MultipartFile;

import com.cnh.ies.entity.order.BatchOrderImportJobDetailEntity;
import com.cnh.ies.entity.order.BatchOrderImportJobEntity;
import com.cnh.ies.exception.ApiException;
import com.cnh.ies.model.general.ListDataModel;
import com.cnh.ies.model.general.PaginationModel;
import com.cnh.ies.model.order.BatchOrderImportEntityItem;
import com.cnh.ies.model.order.BatchOrderImportErrorItem;
import com.cnh.ies.model.order.BatchOrderImportJobDetailInfo;
import com.cnh.ies.model.order.BatchOrderImportJobInfo;
import com.cnh.ies.model.order.BatchOrderImportNotificationMetadata;
import com.cnh.ies.model.order.BatchOrderImportSummaryCounts;
import com.cnh.ies.model.order.BatchOrderImportOrderCreated;
import com.cnh.ies.model.order.BatchOrderImportResultSummary;
import com.cnh.ies.model.payment.PaymentFileAttachmentType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.cnh.ies.model.payment.PaymentFileUploadInfo;
import com.cnh.ies.repository.order.BatchOrderImportJobDetailRepo;
import com.cnh.ies.repository.order.BatchOrderImportJobRepo;
import com.cnh.ies.service.file.FileService;
import com.cnh.ies.util.RequestContext;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class BatchOrderImportJobService {

    private final BatchOrderImportJobRepo jobRepo;
    private final BatchOrderImportJobDetailRepo detailRepo;
    private final FileService fileService;
    private final BatchOrderImportWorkerService workerService;
    private final ObjectMapper objectMapper;

    public BatchOrderImportJobService(
            BatchOrderImportJobRepo jobRepo,
            BatchOrderImportJobDetailRepo detailRepo,
            FileService fileService,
            ObjectMapper objectMapper,
            @Lazy BatchOrderImportWorkerService workerService) {
        this.jobRepo = jobRepo;
        this.detailRepo = detailRepo;
        this.fileService = fileService;
        this.objectMapper = objectMapper;
        this.workerService = workerService;
    }

    @Transactional
    public String createAndDispatch(MultipartFile file, UUID ownerUserId, String createdBy, String requestId) {
        try {
            String actor = resolveActor(createdBy);
            PaymentFileUploadInfo uploaded = fileService.uploadFile(
                    file,
                    FileService.BATCH_ORDER_IMPORT_CATEGORY,
                    FileService.BATCH_ORDER_IMPORT_S3_PREFIX,
                    null,
                    PaymentFileAttachmentType.PAPER,
                    requestId);

            BatchOrderImportJobEntity job = new BatchOrderImportJobEntity();
            job.setOwnerUserId(ownerUserId);
            job.setFileInfoId(uploaded.getId());
            job.setStatus(BatchOrderImportStatus.PENDING);
            job.setOriginalFileName(uploaded.getFileName());
            job.setCreatedBy(actor);
            job.setUpdatedBy(actor);
            job = jobRepo.save(job);

            UUID jobId = job.getId();
            dispatchAfterCommit(jobId, ownerUserId, actor, requestId);
            return jobId.toString();
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR,
                    "Unable to create async import job: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }

    public BatchOrderImportJobInfo getOwnedJob(String jobId, UUID ownerUserId, String requestId) {
        return toInfo(getOwnedJobEntity(jobId, ownerUserId, requestId));
    }

    public ListDataModel<BatchOrderImportJobInfo> listOwnedJobs(
            UUID ownerUserId,
            Integer page,
            Integer limit,
            String requestId) {
        int safePage = page == null || page < 0 ? 0 : page;
        int safeLimit = limit == null || limit < 1 ? 20 : limit;
        Page<BatchOrderImportJobEntity> jobs = jobRepo.findByOwnerUserIdAndIsDeletedFalseOrderByCreatedAtDesc(
                ownerUserId, PageRequest.of(safePage, safeLimit));
        List<BatchOrderImportJobInfo> data = jobs.stream().map(this::toInfo).collect(Collectors.toList());
        PaginationModel pagination = PaginationModel.builder()
                .page(safePage)
                .limit(safeLimit)
                .total(jobs.getTotalElements())
                .totalPage(jobs.getTotalPages())
                .build();
        return ListDataModel.<BatchOrderImportJobInfo>builder().data(data).pagination(pagination).build();
    }

    public ListDataModel<BatchOrderImportJobDetailInfo> getOwnedJobDetails(
            String jobId,
            UUID ownerUserId,
            Integer page,
            Integer limit,
            String requestId) {
        BatchOrderImportJobEntity job = getOwnedJobEntity(jobId, ownerUserId, requestId);
        int safePage = page == null || page < 0 ? 0 : page;
        int safeLimit = limit == null || limit < 1 ? 20 : limit;
        Page<BatchOrderImportJobDetailEntity> details = detailRepo.findByJobIdAndIsDeletedFalseOrderByCreatedAtAsc(
                job.getId(), PageRequest.of(safePage, safeLimit));
        List<BatchOrderImportJobDetailInfo> pageData = details.stream().map(this::toDetailInfo).collect(Collectors.toList());
        PaginationModel pagination = PaginationModel.builder()
                .page(safePage)
                .limit(safeLimit)
                .total(details.getTotalElements())
                .totalPage(details.getTotalPages())
                .build();
        return ListDataModel.<BatchOrderImportJobDetailInfo>builder().data(pageData).pagination(pagination).build();
    }

    UUID getFileInfoId(UUID jobId, String requestId) {
        BatchOrderImportJobEntity job = jobRepo.findByIdAndIsDeletedFalse(jobId)
                .orElseThrow(() -> new ApiException(ApiException.ErrorCode.NOT_FOUND, "Import job not found",
                        HttpStatus.NOT_FOUND.value(), requestId));
        return job.getFileInfoId();
    }

    @Transactional
    void markRunning(UUID jobId) {
        jobRepo.findByIdAndIsDeletedFalse(jobId).ifPresent(job -> {
            job.setStatus(BatchOrderImportStatus.RUNNING);
            job.setStartedAt(Instant.now());
            if (job.getCreatedBy() != null) {
                job.setUpdatedBy(job.getCreatedBy());
            }
            jobRepo.save(job);
        });
    }

    @Transactional
    void markSuccess(UUID jobId, BatchOrderImportResultSummary summary) {
        if (summary == null) {
            return;
        }
        BatchOrderImportJobEntity job = jobRepo.findByIdAndIsDeletedFalse(jobId).orElse(null);
        if (job == null) {
            return;
        }
        job.setTotalRows(summary.getTotalRows());
        job.setSuccessRows(summary.getTotalRows());
        job.setErrorRows(summary.getErrorCount());
        job.setWarningRows(summary.getWarningCount());
        job.setStatus(summary.getErrorCount() > 0 ? BatchOrderImportStatus.PARTIAL : BatchOrderImportStatus.SUCCESS);
        job.setFinishedAt(Instant.now());
        job.setResultSummaryJson(writeSummaryJson(summary));
        jobRepo.save(job);
        persistImportDetails(jobId, summary);
    }

    @Transactional
    void markFailed(UUID jobId, String message) {
        BatchOrderImportResultSummary summary = buildFailureSummary(message);
        BatchOrderImportJobEntity job = jobRepo.findByIdAndIsDeletedFalse(jobId).orElse(null);
        if (job == null) {
            return;
        }
        job.setStatus(BatchOrderImportStatus.FAILED);
        job.setErrorMessage(message);
        job.setErrorRows(1);
        job.setFinishedAt(Instant.now());
        job.setResultSummaryJson(writeSummaryJson(summary));
        jobRepo.save(job);
        persistImportDetails(jobId, summary);
    }

    public String buildNotificationMetadataJson(UUID jobId, BatchOrderImportResultSummary summary) {
        try {
            BatchOrderImportNotificationMetadata metadata = BatchOrderImportNotificationMetadata.builder()
                    .jobId(jobId.toString())
                    .summary(BatchOrderImportSummaryCounts.from(summary))
                    .build();
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize import notification metadata for job {}: {}", jobId, e.getMessage());
            return null;
        }
    }

    public String buildSuccessNotificationMessage(BatchOrderImportResultSummary summary) {
        return String.format(
                "Tạo đơn hàng thành công: %d đơn hàng, %d sản phẩm mới, %d nhà cung cấp mới. %d warning, %d error.",
                summary.getOrdersCreatedCount(),
                summary.getNewProductsCount(),
                summary.getNewVendorsCount(),
                summary.getWarningCount(),
                summary.getErrorCount());
    }

    public String buildFailureNotificationMessage(String message) {
        return message != null && !message.isBlank() ? message : "Tạo đơn hàng thất bại";
    }

    public BatchOrderImportResultSummary buildFailureSummary(String message) {
        String safeMessage = message != null ? message : "Tạo đơn hàng thất bại";
        return BatchOrderImportResultSummary.builder()
                .totalRows(0)
                .ordersCreatedCount(0)
                .newProductsCount(0)
                .newVendorsCount(0)
                .warningCount(0)
                .errorCount(1)
                .errors(List.of(BatchOrderImportErrorItem.builder().message(safeMessage).build()))
                .build();
    }

    private void dispatchAfterCommit(UUID jobId, UUID ownerUserId, String createdBy, String requestId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    workerService.processAsync(jobId, ownerUserId, createdBy, requestId);
                }
            });
        } else {
            workerService.processAsync(jobId, ownerUserId, createdBy, requestId);
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

    private void persistImportDetails(UUID jobId, BatchOrderImportResultSummary summary) {
        List<BatchOrderImportJobDetailEntity> details = new ArrayList<>();
        if (summary.getOrdersCreated() != null) {
            for (BatchOrderImportOrderCreated order : summary.getOrdersCreated()) {
                details.add(buildDetail(jobId, "INFO", "ORDER_CREATED",
                        "Order " + order.getOrderCode() + " (" + order.getContractNumber() + ")",
                        null, writePayloadJson(order)));
            }
        }
        if (summary.getNewProducts() != null) {
            for (BatchOrderImportEntityItem product : summary.getNewProducts()) {
                details.add(buildDetail(jobId, "WARNING", "NEW_PRODUCT",
                        "New product: " + product.getCode() + " - " + product.getName(),
                        product.getRowNum(), writePayloadJson(product)));
            }
        }
        if (summary.getNewVendors() != null) {
            for (BatchOrderImportEntityItem vendor : summary.getNewVendors()) {
                details.add(buildDetail(jobId, "WARNING", "NEW_VENDOR",
                        "New vendor: " + vendor.getCode() + " - " + vendor.getName(),
                        vendor.getRowNum(), writePayloadJson(vendor)));
            }
        }
        if (summary.getWarnings() != null) {
            for (String warning : summary.getWarnings()) {
                details.add(buildDetail(jobId, "WARNING", "IMPORT_WARNING", warning, null, null));
            }
        }
        if (summary.getErrors() != null) {
            for (BatchOrderImportErrorItem error : summary.getErrors()) {
                details.add(buildDetail(jobId, "ERROR", "IMPORT_ERROR",
                        error.getMessage(),
                        error.getRowNum(),
                        writePayloadJson(error)));
            }
        }
        if (!details.isEmpty()) {
            detailRepo.saveAll(details);
        }
    }

    private BatchOrderImportJobDetailEntity buildDetail(
            UUID jobId, String level, String code, String message, Integer rowNum, String payloadJson) {
        BatchOrderImportJobDetailEntity detail = new BatchOrderImportJobDetailEntity();
        detail.setJobId(jobId);
        detail.setLevel(level);
        detail.setCode(code);
        detail.setMessage(message);
        detail.setRowNum(rowNum);
        detail.setPayloadJson(payloadJson);
        return detail;
    }

    private String writeSummaryJson(BatchOrderImportResultSummary summary) {
        try {
            return objectMapper.writeValueAsString(summary);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize import summary: {}", e.getMessage());
            return null;
        }
    }

    private String writePayloadJson(Object payload) {
        if (payload == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize import detail payload: {}", e.getMessage());
            return null;
        }
    }

    private BatchOrderImportResultSummary readSummaryJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, BatchOrderImportResultSummary.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse import summary JSON: {}", e.getMessage());
            return null;
        }
    }

    private BatchOrderImportJobEntity getOwnedJobEntity(String jobId, UUID ownerUserId, String requestId) {
        UUID id;
        try {
            id = UUID.fromString(jobId);
        } catch (IllegalArgumentException e) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "Invalid job id",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }
        BatchOrderImportJobEntity job = jobRepo.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ApiException(ApiException.ErrorCode.NOT_FOUND, "Import job not found",
                        HttpStatus.NOT_FOUND.value(), requestId));
        if (!job.getOwnerUserId().equals(ownerUserId)) {
            throw new ApiException(ApiException.ErrorCode.FORBIDDEN, "Not allowed to access this import job",
                    HttpStatus.FORBIDDEN.value(), requestId);
        }
        return job;
    }

    private BatchOrderImportJobInfo toInfo(BatchOrderImportJobEntity job) {
        return BatchOrderImportJobInfo.builder()
                .id(job.getId().toString())
                .status(job.getStatus())
                .originalFileName(job.getOriginalFileName())
                .totalRows(job.getTotalRows())
                .successRows(job.getSuccessRows())
                .errorRows(job.getErrorRows())
                .warningRows(job.getWarningRows())
                .startedAt(job.getStartedAt() != null ? job.getStartedAt().toString() : null)
                .finishedAt(job.getFinishedAt() != null ? job.getFinishedAt().toString() : null)
                .errorMessage(job.getErrorMessage())
                .createdBy(job.getCreatedBy())
                .resultSummary(readSummaryJson(job.getResultSummaryJson()))
                .build();
    }

    private BatchOrderImportJobDetailInfo toDetailInfo(BatchOrderImportJobDetailEntity detail) {
        return BatchOrderImportJobDetailInfo.builder()
                .id(detail.getId().toString())
                .rowNum(detail.getRowNum())
                .level(detail.getLevel())
                .code(detail.getCode())
                .message(detail.getMessage())
                .payloadJson(detail.getPayloadJson())
                .createdAt(detail.getCreatedAt() != null ? detail.getCreatedAt().toString() : null)
                .build();
    }

    public static class BatchOrderImportStatus {
        public static final String PENDING = "PENDING";
        public static final String RUNNING = "RUNNING";
        public static final String SUCCESS = "SUCCESS";
        public static final String PARTIAL = "PARTIAL";
        public static final String FAILED = "FAILED";

        private BatchOrderImportStatus() {}
    }
}
