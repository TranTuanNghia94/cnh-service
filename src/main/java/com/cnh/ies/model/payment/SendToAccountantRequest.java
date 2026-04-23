package com.cnh.ies.model.payment;

import lombok.Data;

/**
 * Request body for the owner to send a DRAFT or REJECTED payment request
 * to the accountant for review.
 */
@Data
public class SendToAccountantRequest {
    /** Optional note/comment the owner wants to attach when sending for review. */
    private String note;
}
