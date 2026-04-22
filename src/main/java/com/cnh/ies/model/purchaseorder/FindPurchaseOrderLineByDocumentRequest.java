package com.cnh.ies.model.purchaseorder;

import lombok.Data;

@Data
public class FindPurchaseOrderLineByDocumentRequest {
    private String paperCode;
    private String paperType;
}
