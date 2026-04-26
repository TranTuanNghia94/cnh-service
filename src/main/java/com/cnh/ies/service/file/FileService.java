package com.cnh.ies.service.file;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cnh.ies.constant.Constant;
import com.cnh.ies.entity.file.FileInfoEntity;
import com.cnh.ies.entity.payment.PaymentRequestEntity;
import com.cnh.ies.exception.ApiException;
import com.cnh.ies.model.payment.PaymentFileAttachmentType;
import com.cnh.ies.model.payment.PaymentFileUploadInfo;
import com.cnh.ies.repository.file.FileInfoRepo;
import com.cnh.ies.repository.payment.PaymentRequestRepo;
import com.cnh.ies.repository.warehouse.WarehouseInboundReceiptRepo;
import com.cnh.ies.util.RequestContext;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileService {

    private static final String DEFAULT_S3_PATH_PREFIX = "payment-requests";
    private static final Duration PRESIGN_DURATION = Duration.ofHours(24);

    /** Statuses from which a BANK_NOTE upload can mark the request as PAID. */
    private static final Set<String> BANK_NOTE_PAYABLE_STATUSES = Set.of(
            Constant.PAYMENT_REQUEST_STATUS_APPROVED,
            Constant.PAYMENT_REQUEST_STATUS_PARTIALLY_PAID);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final FileInfoRepo fileInfoRepo;
    private final PaymentRequestRepo paymentRequestRepo;
    private final WarehouseInboundReceiptRepo warehouseInboundReceiptRepo;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.region}")
    private String region;

    /**
     * Uploads the file to S3 and persists metadata in {@code file_infos} (no payment request link; {@code PAPER} type).
     */
    @Transactional
    public PaymentFileUploadInfo uploadFile(MultipartFile file, String category, String requestId) {
        return uploadFile(file, category, null, PaymentFileAttachmentType.PAPER, requestId);
    }

    /**
     * Uploads to S3 and saves {@code file_infos}. Parses optional {@code paymentRequestId} (UUID string) and
     * {@code attachmentType} ({@code PAPER} or {@code BANK_NOTE}, default {@code PAPER}).
     */
    @Transactional
    public PaymentFileUploadInfo uploadFile(MultipartFile file, String category, String paymentRequestIdRaw,
            String attachmentTypeRaw, String requestId) {
        UUID paymentRequestId = parseOptionalPaymentRequestId(paymentRequestIdRaw, requestId);
        PaymentFileAttachmentType attachmentType = parseAttachmentType(attachmentTypeRaw, requestId);
        return uploadFile(file, category, DEFAULT_S3_PATH_PREFIX, paymentRequestId, attachmentType, requestId);
    }

    /**
     * Uploads to S3 and saves {@code file_infos} including optional {@code paymentRequestId} and {@code attachmentType}
     * ({@code PAPER} vs {@code BANK_NOTE}).
     */
    @Transactional
    public PaymentFileUploadInfo uploadFile(MultipartFile file, String category, UUID paymentRequestId,
            PaymentFileAttachmentType attachmentType, String requestId) {
        return uploadFile(file, category, DEFAULT_S3_PATH_PREFIX, paymentRequestId, attachmentType, requestId);
    }

    /**
     * Same as {@link #uploadFile(MultipartFile, String, UUID, PaymentFileAttachmentType, String)} with a custom S3 key prefix.
     */
    @Transactional
    public PaymentFileUploadInfo uploadFile(MultipartFile file, String category, String s3PathPrefix,
            UUID paymentRequestId, PaymentFileAttachmentType attachmentType, String requestId) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "File is required", HttpStatus.BAD_REQUEST.value(), requestId);
        }
        PaymentFileAttachmentType type = attachmentType == null ? PaymentFileAttachmentType.PAPER : attachmentType;
        if (paymentRequestId != null) {
            paymentRequestRepo.findByIdAndIsDeletedFalse(paymentRequestId)
                    .orElseThrow(() -> new ApiException(ApiException.ErrorCode.NOT_FOUND, "Payment request not found",
                            HttpStatus.NOT_FOUND.value(), requestId));
        }

        String prefix = s3PathPrefix == null || s3PathPrefix.isBlank() ? DEFAULT_S3_PATH_PREFIX : s3PathPrefix.trim();
        String safeCategory = resolveStorageCategory(category, type);
        String originalFileName = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String key = buildS3Key(prefix, safeCategory, originalFileName);

        log.info("Uploading file '{}' ({} bytes, contentType={}, type={}) to s3://{}/{}",
                originalFileName, file.getSize(), file.getContentType(), type, bucketName, key);

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

            PaymentFileUploadInfo info = new PaymentFileUploadInfo();
            info.setFileName(originalFileName);
            info.setContentType(file.getContentType());
            info.setSize(file.getSize());
            info.setCategory(safeCategory);
            info.setFilePath(key);
            info.setFileUrl(buildS3Url(key));
            info.setViewUrl(presignGetUrl(key));
            info.setPaymentRequestId(paymentRequestId);
            info.setAttachmentType(type.name());
            PaymentFileUploadInfo saved = saveFileInfo(info, requestId);
            log.info("File uploaded successfully: id={} path={}", saved.getId(), saved.getFilePath());

            if (type == PaymentFileAttachmentType.BANK_NOTE && paymentRequestId != null) {
                markPaymentRequestPaid(paymentRequestId, requestId);
            }

            return saved;
        } catch (IOException e) {
            log.error("Cannot read file '{}' for S3 upload", originalFileName, e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, "Cannot read upload file",
                    HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        } catch (S3Exception e) {
            if (e.statusCode() == 403) {
                log.error("S3 upload forbidden for key '{}': {}", key, e.awsErrorDetails().errorMessage());
                throw new ApiException(ApiException.ErrorCode.FORBIDDEN,
                        "S3 upload denied: grant IAM s3:PutObject (and s3:GetObject if needed) on bucket "
                                + bucketName + " for prefix " + prefix + "/. AWS: " + e.awsErrorDetails().errorMessage(),
                        HttpStatus.FORBIDDEN.value(), requestId);
            }
            log.error("S3 upload failed for key '{}': {}", key, e.getMessage(), e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, "Upload to S3 failed: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error uploading file '{}' to S3", originalFileName, e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, "Upload to S3 failed: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }

    /**
     * All non-deleted uploads linked to a payment request (papers and bank-note files), ordered by time.
     */
    @Transactional
    public List<PaymentFileUploadInfo> listUploadedFilesForPaymentRequest(String paymentRequestId, String requestId) {
        UUID id;
        try {
            id = UUID.fromString(paymentRequestId);
        } catch (IllegalArgumentException e) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "paymentRequestId must be a valid UUID",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }
        paymentRequestRepo.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ApiException(ApiException.ErrorCode.NOT_FOUND, "Payment request not found",
                        HttpStatus.NOT_FOUND.value(), requestId));
        List<PaymentFileUploadInfo> files = fileInfoRepo.findByPaymentRequestIdAndIsDeletedFalseOrderByCreatedAtAsc(id).stream()
                .map(this::toUploadInfo)
                .collect(Collectors.toList());
        log.debug("Listed {} file(s) for paymentRequestId={}", files.size(), paymentRequestId);
        return files;
    }

    /**
     * Uploads a file directly linked to a warehouse inbound receipt (works for both PR-based and PO-direct receipts).
     */
    @Transactional
    public PaymentFileUploadInfo uploadFileForWarehouseInbound(MultipartFile file, String category,
            String receiptIdRaw, String requestId) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "File is required", HttpStatus.BAD_REQUEST.value(), requestId);
        }
        UUID receiptId;
        try {
            receiptId = UUID.fromString(receiptIdRaw.trim());
        } catch (Exception e) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "receiptId must be a valid UUID",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }
        warehouseInboundReceiptRepo.findByIdAndIsDeletedFalse(receiptId)
                .orElseThrow(() -> new ApiException(ApiException.ErrorCode.NOT_FOUND, "Warehouse inbound receipt not found",
                        HttpStatus.NOT_FOUND.value(), requestId));

        String safeCategory = category != null && !category.isBlank() ? category.trim().toLowerCase(Locale.ROOT) : "warehouse-inbound";
        String originalFileName = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String key = buildS3Key("warehouse-inbound", safeCategory, originalFileName);

        log.info("Uploading warehouse inbound file '{}' ({} bytes) to s3://{}/{}",
                originalFileName, file.getSize(), bucketName, key);

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

            FileInfoEntity entity = new FileInfoEntity();
            entity.setFileName(originalFileName);
            entity.setFilePath(key);
            entity.setContentType(file.getContentType());
            entity.setFileSizeBytes(file.getSize());
            entity.setCategory(safeCategory);
            entity.setWarehouseInboundReceiptId(receiptId);
            entity.setAttachmentType(PaymentFileAttachmentType.PAPER.name());
            FileInfoEntity saved = fileInfoRepo.save(entity);

            PaymentFileUploadInfo info = toUploadInfo(saved);
            log.info("Warehouse inbound file uploaded: id={} path={}", saved.getId(), saved.getFilePath());
            return info;
        } catch (IOException e) {
            log.error("Cannot read file '{}' for S3 upload", originalFileName, e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, "Cannot read upload file",
                    HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        } catch (S3Exception e) {
            log.error("S3 upload failed for key '{}': {}", key, e.getMessage(), e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, "Upload to S3 failed: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }

    /**
     * Lists uploads linked to a warehouse inbound receipt (after confirmation).
     */
    @Transactional
    public List<PaymentFileUploadInfo> listUploadedFilesForWarehouseInboundReceipt(String receiptId, String requestId) {
        UUID id;
        try {
            id = UUID.fromString(receiptId);
        } catch (IllegalArgumentException e) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "receiptId must be a valid UUID",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }
        warehouseInboundReceiptRepo.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ApiException(ApiException.ErrorCode.NOT_FOUND, "Warehouse inbound receipt not found",
                        HttpStatus.NOT_FOUND.value(), requestId));
        return fileInfoRepo.findByWarehouseInboundReceiptIdAndIsDeletedFalseOrderByCreatedAtAsc(id).stream()
                .map(this::toUploadInfo)
                .collect(Collectors.toList());
    }

    /**
     * Associates existing {@code file_infos} rows (already tied to the payment request) with a warehouse inbound receipt.
     */
    @Transactional
    public void linkFilesToWarehouseInboundReceipt(List<UUID> fileIds, UUID paymentRequestId, UUID receiptId, String requestId) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }
        warehouseInboundReceiptRepo.findByIdAndPaymentRequestId(receiptId, paymentRequestId)
                .orElseThrow(() -> new ApiException(ApiException.ErrorCode.NOT_FOUND,
                        "Warehouse inbound receipt not found for this payment request",
                        HttpStatus.NOT_FOUND.value(), requestId));
        for (UUID fileId : fileIds) {
            FileInfoEntity f = fileInfoRepo.findById(fileId)
                    .orElseThrow(() -> new ApiException(ApiException.ErrorCode.NOT_FOUND, "File not found: " + fileId,
                            HttpStatus.NOT_FOUND.value(), requestId));
            if (Boolean.TRUE.equals(f.getIsDeleted())) {
                throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "File is deleted: " + fileId,
                        HttpStatus.BAD_REQUEST.value(), requestId);
            }
            if (f.getPaymentRequestId() == null || !f.getPaymentRequestId().equals(paymentRequestId)) {
                throw new ApiException(ApiException.ErrorCode.BAD_REQUEST,
                        "File must belong to the same payment request before linking to inbound receipt: " + fileId,
                        HttpStatus.BAD_REQUEST.value(), requestId);
            }
            f.setWarehouseInboundReceiptId(receiptId);
            f.setUpdatedBy(RequestContext.getCurrentUsername());
            fileInfoRepo.save(f);
        }
    }

    private static UUID parseOptionalPaymentRequestId(String raw, String requestId) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException e) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "paymentRequestId must be a valid UUID",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }
    }

    public static PaymentFileAttachmentType parseAttachmentType(String raw, String requestId) {
        if (raw == null || raw.isBlank()) {
            return PaymentFileAttachmentType.PAPER;
        }
        try {
            return PaymentFileAttachmentType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST,
                    "attachmentType must be PAPER or BANK_NOTE", HttpStatus.BAD_REQUEST.value(), requestId);
        }
    }

    private String resolveStorageCategory(String category, PaymentFileAttachmentType attachmentType) {
        if (category != null && !category.isBlank()) {
            return category.trim().toLowerCase(Locale.ROOT);
        }
        return attachmentType == PaymentFileAttachmentType.BANK_NOTE ? "bank-note" : "papers";
    }

    /**
     * Transitions the payment request to {@code PAID} when a bank note is uploaded.
     * Only allowed from {@code APPROVED} or {@code PARTIALLY_PAID}; throws {@code CONFLICT} otherwise.
     */
    private void markPaymentRequestPaid(UUID paymentRequestId, String requestId) {
        PaymentRequestEntity entity = paymentRequestRepo.findByIdAndIsDeletedFalse(paymentRequestId)
                .orElseThrow(() -> new ApiException(ApiException.ErrorCode.NOT_FOUND,
                        "Payment request not found", HttpStatus.NOT_FOUND.value(), requestId));

        if (!BANK_NOTE_PAYABLE_STATUSES.contains(entity.getStatus())) {
            throw new ApiException(ApiException.ErrorCode.CONFLICT,
                    "Bank note can only be attached to an APPROVED or PARTIALLY_PAID payment request (current: "
                            + entity.getStatus() + ")",
                    HttpStatus.CONFLICT.value(), requestId);
        }

        String prevStatus = entity.getStatus();
        entity.setStatus(Constant.PAYMENT_REQUEST_STATUS_PAID);
        entity.setPaidAt(Instant.now());
        entity.setUpdatedBy(RequestContext.getCurrentUsername());
        paymentRequestRepo.save(entity);

        log.info("Payment request auto-marked PAID after bank note upload [id={}, prevStatus={}, rid={}]",
                paymentRequestId, prevStatus, requestId);
    }

    private PaymentFileUploadInfo saveFileInfo(PaymentFileUploadInfo info, String requestId) {
        if (info.getFilePath() == null || info.getFilePath().isBlank()) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "File path is required to save file info",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }
        if (info.getFileName() == null || info.getFileName().isBlank()) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "File name is required to save file info",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }

        FileInfoEntity entity = new FileInfoEntity();
        entity.setFileName(info.getFileName());
        entity.setFilePath(info.getFilePath());
        entity.setContentType(info.getContentType());
        entity.setFileSizeBytes(info.getSize());
        entity.setCategory(info.getCategory());
        entity.setPaymentRequestId(info.getPaymentRequestId());
        entity.setAttachmentType(info.getAttachmentType() != null ? info.getAttachmentType() : PaymentFileAttachmentType.PAPER.name());

        FileInfoEntity saved = fileInfoRepo.save(entity);
        info.setId(saved.getId());
        return info;
    }

    private PaymentFileUploadInfo toUploadInfo(FileInfoEntity entity) {
        PaymentFileUploadInfo info = new PaymentFileUploadInfo();
        info.setId(entity.getId());
        info.setPaymentRequestId(entity.getPaymentRequestId());
        info.setWarehouseInboundReceiptId(entity.getWarehouseInboundReceiptId());
        info.setAttachmentType(entity.getAttachmentType());
        info.setFileName(entity.getFileName());
        info.setFilePath(entity.getFilePath());
        info.setFileUrl(buildS3Url(entity.getFilePath()));
        info.setViewUrl(presignGetUrl(entity.getFilePath()));
        info.setContentType(entity.getContentType());
        info.setSize(entity.getFileSizeBytes());
        info.setCategory(entity.getCategory());
        return info;
    }

    /**
     * Generates a pre-signed GET URL for the given S3 key, valid for {@value #PRESIGN_DURATION} (24 h).
     * Returns an empty string if presigning fails so the rest of the response is still usable.
     */
    private String presignGetUrl(String key) {
        if (key == null || key.isBlank()) return "";
        try {
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(PRESIGN_DURATION)
                    .getObjectRequest(GetObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .build())
                    .build();
            return s3Presigner.presignGetObject(presignRequest).url().toString();
        } catch (Exception e) {
            log.warn("Failed to generate pre-signed URL for key '{}': {}", key, e.getMessage());
            return "";
        }
    }

    private String buildS3Key(String pathPrefix, String category, String originalFileName) {
        LocalDate now = LocalDate.now();
        String normalizedName = originalFileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        return pathPrefix + "/" + category + "/"
                + now.getYear() + "/" + now.getMonthValue() + "/" + now.getDayOfMonth()
                + "/" + UUID.randomUUID() + "_" + normalizedName;
    }

    private String buildS3Url(String key) {
        return "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + key;
    }
}
