package com.cnh.ies.model.warehouse;

import lombok.Data;

@Data
public class WarehouseOutboundActionsInfo {
    private Boolean canSubmit;
    private Boolean canApprove;
    private Boolean canReject;
    private Boolean canCancel;
    private Boolean canResubmit;
}
