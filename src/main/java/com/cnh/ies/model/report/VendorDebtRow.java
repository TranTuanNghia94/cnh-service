package com.cnh.ies.model.report;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VendorDebtRow {
    private String vendorCode;
    private String vendorName;
    private String currency;
    private BigDecimal openingDebt;
    private BigDecimal goodsValue;
    private BigDecimal paidAmount;
    private BigDecimal closingDebt;
}
