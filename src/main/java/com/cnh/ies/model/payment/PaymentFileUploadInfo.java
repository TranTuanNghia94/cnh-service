package com.cnh.ies.model.payment;

import lombok.Data;

@Data
public class PaymentFileUploadInfo {
    private String fileName;
    private String fileUrl;
    private String contentType;
    private Long size;
    private String category;
}
