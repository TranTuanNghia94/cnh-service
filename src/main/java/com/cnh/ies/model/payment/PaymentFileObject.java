package com.cnh.ies.model.payment;

import lombok.Data;

@Data
public class PaymentFileObject {
    private String fileName;
    private String fileUrl;
    private String contentType;
    private Long size;
    private String uploadedAt;
    private String uploadedBy;
    private String category;
}
