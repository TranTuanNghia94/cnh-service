package com.cnh.ies.entity.file;

import com.cnh.ies.entity.BaseEntity;

import java.util.UUID;

import com.cnh.ies.model.payment.PaymentFileAttachmentType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "file_infos")
@Data
@EqualsAndHashCode(callSuper = true)
public class FileInfoEntity extends BaseEntity {

    @Column(name = "file_name", nullable = false, length = 512)
    private String fileName;

    /** S3 object key (path only, e.g. {@code payment-requests/papers/2025/1/15/<uuid>_invoice.pdf}). Full URL is built at runtime. */
    @Column(name = "file_path", nullable = false, columnDefinition = "TEXT")
    private String filePath;

    @Column(name = "content_type", length = 255)
    private String contentType;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "payment_request_id")
    private UUID paymentRequestId;

    @Column(name = "warehouse_inbound_receipt_id")
    private UUID warehouseInboundReceiptId;

    @Column(name = "attachment_type", nullable = false, length = 50)
    private String attachmentType = PaymentFileAttachmentType.PAPER.name();
}
