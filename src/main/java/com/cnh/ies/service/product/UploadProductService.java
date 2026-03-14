package com.cnh.ies.service.product;

import org.springframework.web.multipart.MultipartFile;

import com.cnh.ies.repository.product.ProductRepo;
import com.cnh.ies.exception.ApiException;
import com.cnh.ies.dto.response.UploadProductResponse;
import com.cnh.ies.entity.product.CategoryEntity;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import com.cnh.ies.entity.product.ProductEntity;
import com.cnh.ies.repository.product.CategoryRepo;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class UploadProductService {
    private final ProductRepo productRepo;
    private final CategoryRepo categoryRepo;    

    public UploadProductResponse readExcelFile(MultipartFile file, String requestId) {
        log.info("Reading excel file, requestId: {}", requestId);
        List<String> errors = new ArrayList<>();
        int importedCount = 0;

        UploadProductResponse response = new UploadProductResponse();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            response.setTotalRows(sheet.getLastRowNum());
            
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String error = processRow(row, i);
                if (error != null) {
                    errors.add(error);
                } else {
                    importedCount++;
                }
            }
        } catch (Exception e) {
            log.error("Error reading excel file: {}", e.getMessage(), e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, 
                "Error reading excel file: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }

        log.info("Import completed: {} success, {} errors", importedCount, errors.size());

        response.setErrors(errors);
        response.setMessage("Import completed successfully");
        response.setTotalSuccess(importedCount);
        response.setTotalErrors(errors.size());
        
        return response;
    }

    private String processRow(Row row, int rowNum) {
        String categoryCode = getCellStringValue(row.getCell(0));
        String code = getCellStringValue(row.getCell(1));
        String name = getCellStringValue(row.getCell(2));
        String unit = getCellStringValue(row.getCell(3));
        BigDecimal tax = getCellNumericValue(row.getCell(4));
        String misaCode = getCellStringValue(row.getCell(5));

        if (isBlank(code)) return "Row " + rowNum + ": Product code is required";
        if (isBlank(name)) return "Row " + rowNum + ": Product name is required";
        if (isBlank(unit)) return "Row " + rowNum + ": Product unit is required";

        CategoryEntity category = categoryRepo.findByCode(categoryCode).orElse(null);
        if (category == null) {
            return "Row " + rowNum + ": Category with code '" + categoryCode + "' not found";
        }

        ProductEntity product = new ProductEntity();
        product.setCode(code);
        product.setName(name);
        product.setUnit1(unit);
        product.setTax(tax != null ? tax : BigDecimal.ZERO);
        product.setMisaCode(misaCode != null ? misaCode : "");
        product.setCategory(category);
        productRepo.save(product);

        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue().trim();
        } else if (cell.getCellType() == CellType.NUMERIC) {
            return String.valueOf((long) cell.getNumericCellValue());
        }
        return null;
    }

    private BigDecimal getCellNumericValue(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue());
        } else if (cell.getCellType() == CellType.STRING) {
            try {
                return new BigDecimal(cell.getStringCellValue().trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
