package com.cnh.ies.service.warehouse;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;

import com.cnh.ies.repository.warehouse.WarehouseOutboundRepo;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WarehouseOutboundNumberService {

    private static final String WAREHOUSE_OUTBOUND_PREFIX = "XK_";
    private static final String DATE_FORMAT = "yyyy_MM";

    private final WarehouseOutboundRepo warehouseOutboundRepo;

    public String generateOutboundPrefix() {
        String yearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern(DATE_FORMAT));
        return WAREHOUSE_OUTBOUND_PREFIX + yearMonth;
    }

    @Transactional
    public String generateOutboundNumber() {
        String prefix = generateOutboundPrefix();
        List<String> numbers = warehouseOutboundRepo.findOutboundNumbersByPrefixForUpdate(prefix);
        int max = 0;
        for (String number : numbers) {
            if (number == null || number.isBlank()) {
                continue;
            }
            int splitIndex = number.lastIndexOf('.');
            if (splitIndex < 0 || splitIndex >= number.length() - 1) {
                continue;
            }
            try {
                int sequence = Integer.parseInt(number.substring(splitIndex + 1));
                if (sequence > max) {
                    max = sequence;
                }
            } catch (NumberFormatException ignore) {
            }
        }
        return prefix + "." + (max + 1);
    }
}
