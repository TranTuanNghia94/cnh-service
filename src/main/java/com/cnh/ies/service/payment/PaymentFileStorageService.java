package com.cnh.ies.service.payment;

import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cnh.ies.exception.ApiException;
import com.cnh.ies.model.payment.PaymentFileUploadInfo;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@RequiredArgsConstructor
public class PaymentFileStorageService {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.region}")
    private String region;

    public PaymentFileUploadInfo uploadPaymentFile(MultipartFile file, String category, String requestId) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "File is required", HttpStatus.BAD_REQUEST.value(), requestId);
        }
        String safeCategory = category == null || category.isBlank() ? "papers" : category.trim().toLowerCase();
        String originalFileName = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String key = buildS3Key(safeCategory, originalFileName);
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
            info.setFileUrl(buildS3Url(key));
            return info;
        } catch (IOException e) {
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, "Cannot read upload file",
                    HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        } catch (Exception e) {
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, "Upload to S3 failed: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }

    private String buildS3Key(String category, String originalFileName) {
        LocalDate now = LocalDate.now();
        String normalizedName = originalFileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        return "payment-requests/" + category + "/"
                + now.getYear() + "/" + now.getMonthValue() + "/" + now.getDayOfMonth()
                + "/" + UUID.randomUUID() + "_" + normalizedName;
    }

    private String buildS3Url(String key) {
        return "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + key;
    }
}
