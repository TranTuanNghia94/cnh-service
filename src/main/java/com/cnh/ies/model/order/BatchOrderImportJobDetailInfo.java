package com.cnh.ies.model.order;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BatchOrderImportJobDetailInfo {
    private String id;
    private Integer rowNum;
    private String level;
    private String code;
    private String message;
    private String payloadJson;
    private String createdAt;
}
