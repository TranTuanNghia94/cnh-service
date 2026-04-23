package com.cnh.ies.model.payment;

/**
 * How an uploaded file is used on a payment request.
 * <ul>
 *   <li>{@link #PAPER} — evidence listed in {@code payment_requests.papers} (create/update flow).</li>
 *   <li>{@link #BANK_NOTE} — proof attached to {@code payment_requests.bank_note} (e.g. mark-paid flow).</li>
 * </ul>
 */
public enum PaymentFileAttachmentType {
    PAPER,
    BANK_NOTE
}
