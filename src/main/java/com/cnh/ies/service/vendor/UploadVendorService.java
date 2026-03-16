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
import java.util.List;

import static com.cnh.ies.util.ExcelUtils.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class UploadVendorService {
    private final VendorsRepo vendorsRepo;
    private final VendorBanksRepo vendorBanksRepo;

    public UploadOjectResponse readExcelFile(MultipartFile file, String requestId) {
        log.info("Reading excel file, requestId: {}", requestId);
        List<String> errors = new ArrayList<>();
        int importedCount = 0;

        try (var workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            
            String headerError = validateHeaders(sheet, TemplateType.VENDOR);
            if (headerError != null) {
                throw new ApiException(ApiException.ErrorCode.INVALID_REQUEST,
                        "Invalid template: " + headerError, HttpStatus.BAD_REQUEST.value(), requestId);
            }
            
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String error = processRow(row, i);
                if (error != null) errors.add(error);
                else importedCount++;
            }
            log.info("Import completed: {} success, {} errors", importedCount, errors.size());
            return new UploadOjectResponse("Import completed successfully", sheet.getLastRowNum(), importedCount, errors.size(), errors);
        } catch (Exception e) {
            log.error("Error reading excel file: {}", e.getMessage(), e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR,
                    "Error reading excel file: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }

    private String processRow(Row row, int rowNum) {
        String code = getString(row, 0);
        String name = getString(row, 1);
        String currency = getString(row, 3);

        if (isBlank(code)) return "Row " + rowNum + ": Vendor code is required";
        if (isBlank(name)) return "Row " + rowNum + ": Vendor name is required";
        if (isBlank(currency)) return "Row " + rowNum + ": Vendor currency is required";
        if (vendorsRepo.findByCode(code).isPresent()) return "Row " + rowNum + ": Vendor with code '" + code + "' already exists";

        String username = RequestContext.getCurrentUsername();
        
        VendorsEntity vendor = new VendorsEntity();
        vendor.setCode(code);
        vendor.setName(name);
        vendor.setMisaCode(getString(row, 2));
        vendor.setCurrency(currency);
        vendor.setCountry(getString(row, 4));
        vendor.setPhone(getString(row, 5));
        vendor.setAddress(getString(row, 6));
        vendor.setCreatedBy(username);
        vendor.setUpdatedBy(username);
        vendorsRepo.save(vendor);

        if (getString(row, 7) != null && getString(row, 8) != null) {
            VendorBanksEntity vendorBank = new VendorBanksEntity();
            vendorBank.setVendor(vendor);
            vendorBank.setBankName(getString(row, 7));
            vendorBank.setBankAccountNumber(getString(row, 8));
            vendorBank.setBankAccountName(getString(row, 9));
            vendorBank.setBankAccountBranch(getString(row, 10));
            vendorBank.setCreatedBy(username);
            vendorBank.setUpdatedBy(username);
            vendorBanksRepo.save(vendorBank);
        }

        return null;
    }
}
