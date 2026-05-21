package com.cnh.ies.service.export;

import java.util.Map;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.cnh.ies.config.Loggable;
import com.cnh.ies.config.LoggingInterceptor;
import com.cnh.ies.model.payment.PaymentFileUploadInfo;
import com.cnh.ies.service.file.FileService;
import com.cnh.ies.service.notification.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExportJobWorkerService {

    private static final String REFERENCE_TYPE = "EXPORT_JOB";

    private final ExportJobService exportJobService;
    private final ExcelExportService excelExportService;
    private final FileService fileService;
    private final NotificationService notificationService;

    @Async
    @Loggable(slowThresholdMs = 120_000)
    public void processAsync(UUID jobId, UUID ownerUserId, String type, String createdBy, String requestId) {
        Map<String, String> previousMdc = MDC.getCopyOfContextMap();
        try {
            if (requestId != null && !requestId.isBlank()) {
                MDC.put(LoggingInterceptor.REQUEST_ID_MDC_KEY, requestId);
                MDC.put(LoggingInterceptor.CORRELATION_ID_MDC_KEY, requestId);
            }
            if (createdBy != null && !createdBy.isBlank()) {
                MDC.put(LoggingInterceptor.USER_ID_MDC_KEY, createdBy);
            }
            log.info("Starting export job {} type={}", jobId, type);

            exportJobService.markRunning(jobId);

            ExcelExportService.ExportWorkbookResult workbook = excelExportService.export(type, requestId);
            String s3Category = ExportJobType.fileNamePrefix(type);
            PaymentFileUploadInfo uploaded = fileService.uploadGeneratedFile(
                    workbook.content(),
                    workbook.fileName(),
                    FileService.EXPORT_S3_PREFIX,
                    s3Category,
                    requestId);

            exportJobService.markSuccess(jobId, uploaded.getId(), workbook.fileName());

            String downloadUrl = fileService.presignGetUrlForKey(uploaded.getFilePath());
            String metadata = exportJobService.buildNotificationMetadataJson(
                    jobId, type, workbook.fileName(), downloadUrl);
            notificationService.sendNotification(
                    ownerUserId,
                    "Xuất Excel thành công",
                    exportJobService.buildSuccessNotificationMessage(type, workbook.fileName()),
                    NotificationService.NotificationType.SUCCESS,
                    NotificationService.NotificationCategory.SYSTEM,
                    jobId.toString(),
                    REFERENCE_TYPE,
                    "/exports/" + jobId,
                    metadata);

        } catch (Exception ex) {
            log.error("Export job {} failed", jobId, ex);
            exportJobService.markFailed(jobId, ex.getMessage());

            String metadata = exportJobService.buildNotificationMetadataJson(
                    jobId, type, null, null);
            notificationService.sendNotification(
                    ownerUserId,
                    "Xuất Excel thất bại",
                    exportJobService.buildFailureNotificationMessage(ex.getMessage()),
                    NotificationService.NotificationType.ERROR,
                    NotificationService.NotificationCategory.SYSTEM,
                    jobId.toString(),
                    REFERENCE_TYPE,
                    "/exports/" + jobId,
                    metadata);
        } finally {
            if (previousMdc != null) {
                MDC.setContextMap(previousMdc);
            } else {
                MDC.clear();
            }
        }
    }
}
