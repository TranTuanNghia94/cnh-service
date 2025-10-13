package com.cnh.ies.service.order;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;

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
    
    public Integer generateNextNumberOrReset() {
        
        String prefix = generateOrderPrefix();
        Integer maxSequence = orderRepo.findMaxSequenceForYearMonth(prefix);
        if (maxSequence == null) {
            return 1;
        }
        return maxSequence + 1;
    }

    public String generateOrderPrefix() {
        LocalDate currentDate = LocalDate.now();
        String yearMonth = currentDate.format(DateTimeFormatter.ofPattern(DATE_FORMAT));
        String prefix = ORDER_PREFIX + yearMonth;

        return prefix;
    }
   
}
