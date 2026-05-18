package com.cnh.ies.service.order;

import java.io.InputStream;
import java.util.UUID;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cnh.ies.dto.response.UploadOjectResponse;
import com.cnh.ies.model.order.BatchOrderImportResultSummary;
import com.cnh.ies.service.file.FileService;
import com.cnh.ies.service.notification.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class BatchOrderImportWorkerService {

    private final BatchOrderImportJobService jobService;
    private final UploadBatchOrderService uploadBatchOrderService;
    private final FileService fileService;
    private final NotificationService notificationService;

    @Async
    @Transactional
    public void processAsync(UUID jobId, UUID ownerUserId, String createdBy, String requestId) {
        try {
            jobService.markRunning(jobId);
            UUID fileInfoId = jobService.getFileInfoId(jobId, requestId);

            UploadOjectResponse result;
            try (InputStream inputStream = fileService.openInputStreamByFileInfoId(fileInfoId, requestId)) {
                result = uploadBatchOrderService.readExcelInputStream(inputStream, requestId, createdBy);
            }

            BatchOrderImportResultSummary summary = result.getImportSummary();
            if (summary == null) {
                summary = BatchOrderImportResultSummary.builder()
                        .totalRows(result.getTotalRows())
                        .ordersCreatedCount(0)
                        .newProductsCount(0)
                        .newVendorsCount(0)
                        .warningCount(result.getWarnings() != null ? result.getWarnings().size() : 0)
                        .errorCount(result.getTotalErrors())
                        .warnings(result.getWarnings())
                        .build();
            }

            jobService.markSuccess(jobId, summary);

            String metadata = jobService.buildNotificationMetadataJson(jobId, summary);
            notificationService.sendNotification(
                    ownerUserId,
                    "Batch order import completed",
                    jobService.buildSuccessNotificationMessage(summary),
                    NotificationService.NotificationType.SUCCESS,
                    NotificationService.NotificationCategory.SYSTEM,
                    jobId.toString(),
                    "BATCH_ORDER_IMPORT",
                    "/imports/batch-order/" + jobId,
                    metadata);

        } catch (Exception ex) {
            log.error("Batch order import job {} failed", jobId, ex);
            BatchOrderImportResultSummary failureSummary = jobService.buildFailureSummary(ex.getMessage());
            jobService.markFailed(jobId, ex.getMessage());

            String metadata = jobService.buildNotificationMetadataJson(jobId, failureSummary);
            notificationService.sendNotification(
                    ownerUserId,
                    "Batch order import failed",
                    jobService.buildFailureNotificationMessage(ex.getMessage()),
                    NotificationService.NotificationType.ERROR,
                    NotificationService.NotificationCategory.SYSTEM,
                    jobId.toString(),
                    "BATCH_ORDER_IMPORT",
                    "/imports/batch-order/" + jobId,
                    metadata);
        }
    }
}
