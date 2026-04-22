package com.cnh.ies.model.payment;

import lombok.Data;

@Data
public class PaymentBankInfoObject {
    private String bankName;
    private String accountName;
    private String accountNumber;
    private String swiftCode;
    private String branch;
    private String beneficiaryAddress;
    private String note;
}
