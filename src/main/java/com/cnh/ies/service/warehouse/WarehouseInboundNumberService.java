package com.cnh.ies.service.warehouse;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;

import com.cnh.ies.repository.warehouse.WarehouseInboundReceiptRepo;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WarehouseInboundNumberService {

    private static final String WAREHOUSE_INBOUND_PREFIX = "NK_";
    private static final String DATE_FORMAT = "yyyy_MM";

    private final WarehouseInboundReceiptRepo warehouseInboundReceiptRepo;

    public String generateReceiptPrefix() {
        String yearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern(DATE_FORMAT));
        return WAREHOUSE_INBOUND_PREFIX + yearMonth;
    }

    @Transactional
    public String generateReceiptNumber() {
        String prefix = generateReceiptPrefix();
        List<String> receiptNumbers = warehouseInboundReceiptRepo.findReceiptNumbersByPrefixForUpdate(prefix);
        int max = 0;
        for (String receiptNumber : receiptNumbers) {
            if (receiptNumber == null || receiptNumber.isBlank()) {
                continue;
            }
            int splitIndex = receiptNumber.lastIndexOf('.');
            if (splitIndex < 0 || splitIndex >= receiptNumber.length() - 1) {
                continue;
            }
            try {
                int sequence = Integer.parseInt(receiptNumber.substring(splitIndex + 1));
                if (sequence > max) {
                    max = sequence;
                }
            } catch (NumberFormatException ignore) {
            }
        }
        return prefix + "." + (max + 1);
    }
}
