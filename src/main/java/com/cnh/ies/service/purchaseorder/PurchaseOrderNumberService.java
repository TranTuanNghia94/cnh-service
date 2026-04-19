package com.cnh.ies.service.purchaseorder;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;

import com.cnh.ies.repository.purchaseorder.PurchaseOrderRepo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseOrderNumberService {

    private final PurchaseOrderRepo purchaseOrderRepo;

    private static final String PO_PREFIX = "PO_";
    private static final String DATE_FORMAT = "yyyy_MM";

    public Integer generateNextNumberOrReset() {
        String prefix = generatePoPrefix();
        Integer maxSequence = purchaseOrderRepo.findMaxSequenceForPrefix(prefix);
        if (maxSequence == null) {
            return 1;
        }
        return maxSequence + 1;
    }

    public String generatePoPrefix() {
        LocalDate currentDate = LocalDate.now();
        String yearMonth = currentDate.format(DateTimeFormatter.ofPattern(DATE_FORMAT));
        return PO_PREFIX + yearMonth;
    }
}
