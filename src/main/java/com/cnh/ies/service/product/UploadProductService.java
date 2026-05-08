package com.cnh.ies.service.product;

import com.cnh.ies.dto.response.UploadOjectResponse;
import com.cnh.ies.entity.product.CategoryEntity;
import com.cnh.ies.entity.product.ProductEntity;
import com.cnh.ies.exception.ApiException;
import com.cnh.ies.repository.product.CategoryRepo;
import com.cnh.ies.repository.product.ProductRepo;
import com.cnh.ies.util.RequestContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.cnh.ies.util.ExcelUtils.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class UploadProductService {
    private final ProductRepo productRepo;
    private final CategoryRepo categoryRepo;

    public UploadOjectResponse readExcelFile(MultipartFile file, String requestId) {
        log.info("Reading excel file, requestId: {}", requestId);
        List<String> errors = new ArrayList<>();
        int importedCount = 0;

        try (var workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            
            String headerError = validateHeaders(sheet, TemplateType.PRODUCT);
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
            return new UploadOjectResponse("Import completed successfully", sheet.getLastRowNum(), importedCount, errors.size(), errors, Collections.emptyList());
        } catch (Exception e) {
            log.error("Error reading excel file: {}", e.getMessage(), e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR,
                    "Error reading excel file: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }

    private String processRow(Row row, int rowNum) {
        String categoryCode = getString(row, 0);
        String code = getString(row, 1);
        String name = getString(row, 2);
        String unit = getString(row, 3);
        BigDecimal tax = getNumeric(row, 4);
        String misaCode = getString(row, 5);

        if (isBlank(code)) return "Row " + rowNum + ": Product code is required";
        if (isBlank(name)) return "Row " + rowNum + ": Product name is required";
        if (isBlank(unit)) return "Row " + rowNum + ": Product unit is required";
        if (productRepo.findByCode(code).isPresent()) return "Row " + rowNum + ": Product with code '" + code + "' already exists";

        CategoryEntity category = categoryRepo.findByCode(categoryCode).orElse(null);
        if (category == null) return "Row " + rowNum + ": Category with code '" + categoryCode + "' not found";

        String username = RequestContext.getCurrentUsername();
        
        ProductEntity product = new ProductEntity();
        product.setCode(code);
        product.setName(name);
        product.setUnit1(unit);
        product.setTax(tax != null ? tax : BigDecimal.ZERO);
        product.setMisaCode(misaCode != null ? misaCode : "");
        product.setCategory(category);
        product.setPrice(BigDecimal.ZERO);
        product.setCostPrice(BigDecimal.ZERO);
        product.setCreatedBy(username);
        product.setUpdatedBy(username);
        productRepo.save(product);

        return null;
    }
}
