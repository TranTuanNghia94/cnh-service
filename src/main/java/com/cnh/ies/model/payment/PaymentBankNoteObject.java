package com.cnh.ies.model.payment;

import java.util.List;

import lombok.Data;

@Data
public class PaymentBankNoteObject {
    private String transactionRef;
    private String note;
    private List<PaymentFileObject> attachments;
}
