package com.cnh.ies.service.vendor;

import com.cnh.ies.dto.response.UploadOjectResponse;
import com.cnh.ies.entity.vendors.VendorBanksEntity;
import com.cnh.ies.entity.vendors.VendorsEntity;
import com.cnh.ies.exception.ApiException;
import com.cnh.ies.repository.vendors.VendorBanksRepo;
import com.cnh.ies.repository.vendors.VendorsRepo;
import com.cnh.ies.util.RequestContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.cnh.ies.util.ExcelUtils.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class UploadVendorService {
    private final VendorsRepo vendorsRepo;
    private final VendorBanksRepo vendorBanksRepo;

    public UploadOjectResponse readExcelFile(MultipartFile file, String requestId) {
        log.debug("Reading excel file, requestId: {}", requestId);
        List<String> errors = new ArrayList<>();
        List<PendingVendorRow> pendingRows = new ArrayList<>();

        try (var workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            String headerError = validateHeaders(sheet, TemplateType.VENDOR);
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

            log.info("Vendor import completed: {} success, {} errors", importedCount, errors.size());
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

    private String validateRow(Row row, int rowNum, List<PendingVendorRow> pendingRows, Set<String> codesInFile) {
        String code = getString(row, 0);
        String name = getString(row, 1);
        String currency = getString(row, 3);

        if (isBlank(code)) {
            return "Row " + rowNum + ": Vendor code is required";
        }
        if (isBlank(name)) {
            return "Row " + rowNum + ": Vendor name is required";
        }
        if (isBlank(currency)) {
            return "Row " + rowNum + ": Vendor currency is required";
        }
        if (!codesInFile.add(code)) {
            return "Row " + rowNum + ": Duplicate vendor code '" + code + "' in file";
        }

        pendingRows.add(new PendingVendorRow(
                rowNum, code, name, getString(row, 2), currency,
                getString(row, 4), getString(row, 5), getString(row, 6),
                getString(row, 7), getString(row, 8), getString(row, 9), getString(row, 10)));
        return null;
    }

    private int persistRows(List<PendingVendorRow> pendingRows, List<String> errors, String requestId) {
        if (pendingRows.isEmpty()) {
            return 0;
        }

        List<String> codesLower = pendingRows.stream()
                .map(r -> r.code().toLowerCase(Locale.ROOT))
                .toList();
        Set<String> existingCodes = vendorsRepo.findByCodeInIgnoreCaseAndIsDeletedFalse(codesLower).stream()
                .map(v -> v.getCode().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        String username = RequestContext.getCurrentUsername();
        List<VendorsEntity> vendorsToSave = new ArrayList<>();
        Map<String, PendingVendorRow> rowByCodeKey = new HashMap<>();

        for (PendingVendorRow row : pendingRows) {
            String codeKey = row.code().toLowerCase(Locale.ROOT);
            if (existingCodes.contains(codeKey)) {
                errors.add("Row " + row.rowNum() + ": Vendor with code '" + row.code() + "' already exists");
                continue;
            }
            VendorsEntity vendor = new VendorsEntity();
            vendor.setCode(row.code());
            vendor.setName(row.name());
            vendor.setMisaCode(row.misaCode());
            vendor.setCurrency(row.currency());
            vendor.setCountry(row.country());
            vendor.setPhone(row.phone());
            vendor.setAddress(row.address());
            vendor.setCreatedBy(username);
            vendor.setUpdatedBy(username);
            vendorsToSave.add(vendor);
            rowByCodeKey.put(codeKey, row);
        }

        List<VendorsEntity> savedVendors = vendorsRepo.saveAll(vendorsToSave);
        List<VendorBanksEntity> banksToSave = new ArrayList<>();

        for (VendorsEntity vendor : savedVendors) {
            PendingVendorRow row = rowByCodeKey.get(vendor.getCode().toLowerCase(Locale.ROOT));
            if (row != null && row.bankName() != null && row.bankAccountNumber() != null) {
                VendorBanksEntity vendorBank = new VendorBanksEntity();
                vendorBank.setVendor(vendor);
                vendorBank.setBankName(row.bankName());
                vendorBank.setBankAccountNumber(row.bankAccountNumber());
                vendorBank.setBankAccountName(row.bankAccountName());
                vendorBank.setBankAccountBranch(row.bankAccountBranch());
                vendorBank.setCreatedBy(username);
                vendorBank.setUpdatedBy(username);
                banksToSave.add(vendorBank);
            }
        }

        if (!banksToSave.isEmpty()) {
            vendorBanksRepo.saveAll(banksToSave);
        }
        return savedVendors.size();
    }

    private record PendingVendorRow(
            int rowNum,
            String code,
            String name,
            String misaCode,
            String currency,
            String country,
            String phone,
            String address,
            String bankName,
            String bankAccountNumber,
            String bankAccountName,
            String bankAccountBranch) {
    }
}
