package com.cnh.ies.service.purchaseorder;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional(propagation = Propagation.MANDATORY)
    public Integer generateNextNumberOrReset() {
        String prefix = generatePoPrefix();
        purchaseOrderRepo.acquirePoPrefixAllocationLock(prefix);
        Integer maxSequence = purchaseOrderRepo.findMaxSequenceForPrefix(prefix);
        int next = (maxSequence == null || maxSequence < 1) ? 1 : maxSequence + 1;
        log.debug("Allocated PO number {} for prefix {}", next, prefix);
        return next;
    }

    public String generatePoPrefix() {
        LocalDate currentDate = LocalDate.now();
        String yearMonth = currentDate.format(DateTimeFormatter.ofPattern(DATE_FORMAT));
        return PO_PREFIX + yearMonth;
    }
}
