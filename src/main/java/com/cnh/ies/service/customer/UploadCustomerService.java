package com.cnh.ies.service.customer;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.cnh.ies.dto.response.UploadOjectResponse;
import com.cnh.ies.entity.customer.CustomerAddressEntity;
import com.cnh.ies.entity.customer.CustomerEntity;
import com.cnh.ies.repository.customer.CustomerRepo;
import com.cnh.ies.repository.customer.CustomerAddressRepo;
import com.cnh.ies.util.ExcelUtils.TemplateType;
import com.cnh.ies.util.RequestContext;
import com.cnh.ies.exception.ApiException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import static com.cnh.ies.util.ExcelUtils.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class UploadCustomerService {
    private final CustomerRepo customerRepo;
    private final CustomerAddressRepo customerAddressRepo;

    public UploadOjectResponse readExcelFile(MultipartFile file, String requestId) {
        log.debug("Reading excel file, requestId: {}", requestId);
        List<String> errors = new ArrayList<>();
        List<PendingCustomerRow> pendingRows = new ArrayList<>();

        try (var workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            String headerError = validateHeaders(sheet, TemplateType.CUSTOMER);
            if (headerError != null) {
                throw new ApiException(ApiException.ErrorCode.INVALID_REQUEST,
                        "Invalid template: " + headerError, HttpStatus.BAD_REQUEST.value(), requestId);
            }

            Set<String> codesInFile = new HashSet<>();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                String error = validateRow(row, i, pendingRows, codesInFile);
                if (error != null) {
                    errors.add(error);
                }
            }

            int importedCount = persistRows(pendingRows, errors, requestId);

            log.info("Customer import completed: {} success, {} errors", importedCount, errors.size());
            return new UploadOjectResponse("Import completed successfully", sheet.getLastRowNum(), importedCount,
                    errors.size(), errors, Collections.emptyList());
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception e) {
            log.error("Error reading excel file: {}", e.getMessage(), e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR,
                    "Error reading excel file: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }

    private String validateRow(Row row, int rowNum, List<PendingCustomerRow> pendingRows, Set<String> codesInFile) {
        String code = getString(row, 0);
        String name = getString(row, 1);
        String misaCode = getString(row, 2);

        if (isBlank(code)) {
            return "Row " + rowNum + ": Customer code is required";
        }
        if (isBlank(name)) {
            return "Row " + rowNum + ": Customer name is required";
        }
        if (!codesInFile.add(code)) {
            return "Row " + rowNum + ": Duplicate customer code '" + code + "' in file";
        }

        pendingRows.add(new PendingCustomerRow(
                rowNum, code, name, misaCode,
                getString(row, 4), getString(row, 5), getString(row, 6), getString(row, 7)));
        return null;
    }

    private int persistRows(List<PendingCustomerRow> pendingRows, List<String> errors, String requestId) {
        if (pendingRows.isEmpty()) {
            return 0;
        }

        Set<String> codes = pendingRows.stream().map(PendingCustomerRow::code).collect(Collectors.toSet());
        Set<String> existingCodes = customerRepo.findByCodeInAndIsDeletedFalse(codes).stream()
                .map(CustomerEntity::getCode)
                .collect(Collectors.toSet());

        String username = RequestContext.getCurrentUsername();
        List<CustomerEntity> customersToSave = new ArrayList<>();
        Map<String, PendingCustomerRow> rowByCode = new HashMap<>();

        for (PendingCustomerRow row : pendingRows) {
            if (existingCodes.contains(row.code())) {
                errors.add("Row " + row.rowNum() + ": Customer with code '" + row.code() + "' already exists");
                continue;
            }
            CustomerEntity customer = new CustomerEntity();
            customer.setCode(row.code());
            customer.setName(row.name());
            customer.setMisaCode(row.misaCode());
            customer.setCreatedBy(username);
            customer.setUpdatedBy(username);
            customersToSave.add(customer);
            rowByCode.put(row.code(), row);
        }

        List<CustomerEntity> savedCustomers = customerRepo.saveAll(customersToSave);
        List<CustomerAddressEntity> addressesToSave = new ArrayList<>();

        for (CustomerEntity customer : savedCustomers) {
            PendingCustomerRow row = rowByCode.get(customer.getCode());
            if (row == null) {
                continue;
            }
            if (row.phone() != null || row.contactPerson() != null || row.address() != null) {
                CustomerAddressEntity customerAddress = new CustomerAddressEntity();
                customerAddress.setCustomer(customer);
                customerAddress.setAddress(row.address());
                customerAddress.setContactPerson(row.contactPerson());
                customerAddress.setPhone(row.phone());
                customerAddress.setEmail(row.email());
                customerAddress.setCreatedBy(username);
                customerAddress.setUpdatedBy(username);
                addressesToSave.add(customerAddress);
            }
        }

        if (!addressesToSave.isEmpty()) {
            customerAddressRepo.saveAll(addressesToSave);
        }
        return savedCustomers.size();
    }

    private record PendingCustomerRow(
            int rowNum,
            String code,
            String name,
            String misaCode,
            String phone,
            String contactPerson,
            String address,
            String email) {
    }
}
