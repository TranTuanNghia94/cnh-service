package com.cnh.ies.service.order;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.cnh.ies.repository.order.OrderRepo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderNumberService {

    private final OrderRepo orderRepo;

    private static final String ORDER_PREFIX = "OD_";
    private static final String DATE_FORMAT = "yyyy_MM";

  /**
   * Next sequence for the current month prefix (single-order create).
   */
    @Transactional(propagation = Propagation.MANDATORY)
    public int allocateNextOrderNumber(String orderPrefix) {
        return reserveNextOrderNumbers(orderPrefix, 1);
    }

    /**
     * Reserves {@code count} consecutive {@code order_number} values for {@code orderPrefix}
     * under one lock. Caller must use {@code start}, {@code start + 1}, … {@code start + count - 1}
     * in the same transaction before commit.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public int reserveNextOrderNumbers(String orderPrefix, int count) {
        if (count < 1) {
            throw new IllegalArgumentException("count must be >= 1");
        }
        orderRepo.acquireOrderPrefixAllocationLock(orderPrefix);
        orderRepo.findFirstByOrderPrefixAndIsDeletedFalseOrderByOrderNumberDesc(orderPrefix);
        Integer maxSequence = orderRepo.findMaxSequenceForYearMonth(orderPrefix);
        int start = (maxSequence == null || maxSequence < 1) ? 1 : maxSequence + 1;
        log.debug("Reserved order numbers {}..{} for prefix {}", start, start + count - 1, orderPrefix);
        return start;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Integer generateNextNumberOrReset() {
        return allocateNextOrderNumber(generateOrderPrefix());
    }

    public String generateOrderPrefix() {
        LocalDate currentDate = LocalDate.now();
        String yearMonth = currentDate.format(DateTimeFormatter.ofPattern(DATE_FORMAT));
        return ORDER_PREFIX + yearMonth;
    }
}
