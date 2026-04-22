package com.cnh.ies.service.payment;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;

import com.cnh.ies.repository.payment.PaymentRequestRepo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentRequestNumberService {

    private static final String PAYMENT_REQUEST_PREFIX = "PR_";
    private static final String DATE_FORMAT = "yyyy_MM";

    private final PaymentRequestRepo paymentRequestRepo;

    public String generateRequestPrefix() {
        String yearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern(DATE_FORMAT));
        return PAYMENT_REQUEST_PREFIX + yearMonth;
    }

    public Integer generateNextNumberOrReset() {
        String prefix = generateRequestPrefix();
        List<String> requestNumbers = paymentRequestRepo.findRequestNumbersByPrefix(prefix);
        int max = 0;
        for (String requestNumber : requestNumbers) {
            if (requestNumber == null || requestNumber.isBlank()) {
                continue;
            }
            int splitIndex = requestNumber.lastIndexOf('.');
            if (splitIndex < 0 || splitIndex >= requestNumber.length() - 1) {
                continue;
            }
            try {
                int sequence = Integer.parseInt(requestNumber.substring(splitIndex + 1));
                if (sequence > max) {
                    max = sequence;
                }
            } catch (NumberFormatException ignore) {
                // Ignore legacy/non-standard request_number formats.
            }
        }
        return max + 1;
    }

    public String generateRequestNumber() {
        return generateRequestPrefix() + "." + generateNextNumberOrReset();
    }
}
