package com.cnh.ies.model.payment;

import java.util.UUID;

import lombok.Data;

@Data
public class PaymentFileUploadInfo {
    /** Populated after metadata is persisted to {@code file_infos}. */
    private UUID id;
    /** Optional link to {@code payment_requests}; set when the client passes {@code paymentRequestId} on upload. */
    private UUID paymentRequestId;
    /** Set when the file is linked to a warehouse inbound receipt after confirmation. */
    private UUID warehouseInboundReceiptId;
    /** {@link PaymentFileAttachmentType#name()} — {@code PAPER} or {@code BANK_NOTE}. */
    private String attachmentType;
    private String fileName;
    /** S3 object key stored in DB (e.g. {@code payment-requests/papers/2025/1/15/<uuid>_invoice.pdf}). */
    private String filePath;
    /** Permanent S3 URL (not signed). Only accessible if the bucket/object is public. */
    private String fileUrl;
    /** Pre-signed URL valid for 24 hours — use this to view or download the file. */
    private String viewUrl;
    private String contentType;
    private Long size;
    private String category;
}
