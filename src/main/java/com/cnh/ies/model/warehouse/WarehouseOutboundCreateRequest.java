package com.cnh.ies.model.warehouse;

import java.time.LocalDate;
import java.util.List;

import lombok.Data;

@Data
public class WarehouseOutboundCreateRequest {
    private String contractNumber;
    private String outboundReason;
    private String note;
    private String currency;
    private LocalDate outboundDate;
    private Integer approvalLevels;
    private List<String> approvalRoles;
    private List<String> attachedFileIds;
    private List<WarehouseOutboundDetailRequest> details;
}
