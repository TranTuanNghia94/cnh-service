package com.cnh.ies.service.order;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cnh.ies.repository.order.OrderRepo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for generating auto-increment order numbers in the format: OD_YYYY_MM.XXX
 * 
 * Format breakdown:
 * - OD_: Fixed prefix
 * - YYYY: 4-digit year
 * - _: Separator
 * - MM: 2-digit month (01-12)
 * - .: Separator
 * - XXX: 3-digit sequence number (001-999)
 * 
 * Examples:
 * - OD_2025_10.001 (First order in October 2025)
 * - OD_2025_10.012 (12th order in October 2025)
 * - OD_2025_11.001 (First order in November 2025)
 * 
 * The sequence number resets to 001 each month.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderNumberService {
    
    private final OrderRepo orderRepo;
    
    private static final String ORDER_PREFIX = "OD_";
    private static final String DATE_FORMAT = "yyyy_MM";
    private static final String SEQUENCE_FORMAT = "%03d";
    
    /**
     * Generates the next order number in format: OD_YYYY_MM.XXX
     * where XXX is a 3-digit sequence number that resets each month
     * 
     * @return the next order number
     */
    @Transactional
    public String generateNextOrderNumber() {
        LocalDate currentDate = LocalDate.now();
        String yearMonth = currentDate.format(DateTimeFormatter.ofPattern(DATE_FORMAT));
        String prefix = ORDER_PREFIX + yearMonth + ".";
        
        log.info("Generating next order number with prefix: {}", prefix);
        
        // Find the highest sequence number for the current year/month
        Integer maxSequence = orderRepo.findMaxSequenceForYearMonth(yearMonth);
        
        // Increment sequence number (start from 1 if no orders exist for this month)
        int nextSequence = (maxSequence != null ? maxSequence : 0) + 1;
        
        // Format sequence number with leading zeros (3 digits)
        String sequenceStr = String.format(SEQUENCE_FORMAT, nextSequence);
        String orderNumber = prefix + sequenceStr;
        
        log.info("Generated order number: {} (sequence: {})", orderNumber, nextSequence);
        
        return orderNumber;
    }
    
    /**
     * Validates if an order number follows the expected format
     * 
     * @param orderNumber the order number to validate
     * @return true if valid, false otherwise
     */
    public boolean isValidOrderNumberFormat(String orderNumber) {
        if (orderNumber == null || orderNumber.length() != 14) {
            return false;
        }
        
        try {
            // Expected format: OD_YYYY_MM.XXX
            if (!orderNumber.startsWith(ORDER_PREFIX)) {
                return false;
            }
            
            String yearMonth = orderNumber.substring(3, 10); // YYYY_MM
            String sequence = orderNumber.substring(11); // XXX
            
            // Validate year_month format
            LocalDate.parse(yearMonth.replace("_", "-01"), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            
            // Validate sequence is 3 digits
            if (sequence.length() != 3) {
                return false;
            }
            
            Integer.parseInt(sequence);
            return true;
            
        } catch (Exception e) {
            log.warn("Invalid order number format: {}", orderNumber, e);
            return false;
        }
    }
    
    /**
     * Extracts year and month from order number
     * 
     * @param orderNumber the order number
     * @return year_month string (YYYY_MM) or null if invalid
     */
    public Optional<String> extractYearMonth(String orderNumber) {
        if (!isValidOrderNumberFormat(orderNumber)) {
            return Optional.empty();
        }
        
        String yearMonth = orderNumber.substring(3, 10); // YYYY_MM
        return Optional.of(yearMonth);
    }
}
