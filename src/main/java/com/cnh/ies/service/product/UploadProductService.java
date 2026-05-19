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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.cnh.ies.util.ExcelUtils.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class UploadProductService {
    private final ProductRepo productRepo;
    private final CategoryRepo categoryRepo;

    public UploadOjectResponse readExcelFile(MultipartFile file, String requestId) {
        log.debug("Reading excel file, requestId: {}", requestId);
        List<String> errors = new ArrayList<>();
        List<PendingProductRow> pendingRows = new ArrayList<>();

        try (var workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            String headerError = validateHeaders(sheet, TemplateType.PRODUCT);
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

            log.info("Product import completed: {} success, {} errors", importedCount, errors.size());
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

    private String validateRow(Row row, int rowNum, List<PendingProductRow> pendingRows, Set<String> codesInFile) {
        String categoryCode = getString(row, 0);
        String code = getString(row, 1);
        String name = getString(row, 2);
        String unit = getString(row, 3);
        BigDecimal tax = getNumeric(row, 4);
        String misaCode = getString(row, 5);

        if (isBlank(code)) {
            return "Row " + rowNum + ": Product code is required";
        }
        if (isBlank(name)) {
            return "Row " + rowNum + ": Product name is required";
        }
        if (isBlank(unit)) {
            return "Row " + rowNum + ": Product unit is required";
        }
        if (!codesInFile.add(code)) {
            return "Row " + rowNum + ": Duplicate product code '" + code + "' in file";
        }

        pendingRows.add(new PendingProductRow(rowNum, categoryCode, code, name, unit, tax, misaCode));
        return null;
    }

    private int persistRows(List<PendingProductRow> pendingRows, List<String> errors, String requestId) {
        if (pendingRows.isEmpty()) {
            return 0;
        }

        Set<String> productCodes = pendingRows.stream().map(PendingProductRow::code).collect(Collectors.toSet());
        Set<String> existingCodes = productRepo.findByCodeInAndIsDeletedFalse(productCodes).stream()
                .map(ProductEntity::getCode)
                .collect(Collectors.toSet());

        Set<String> categoryCodes = pendingRows.stream()
                .map(PendingProductRow::categoryCode)
                .filter(code -> code != null && !code.isBlank())
                .collect(Collectors.toSet());
        Map<String, CategoryEntity> categoryByCode = new HashMap<>();
        if (!categoryCodes.isEmpty()) {
            for (CategoryEntity category : categoryRepo.findByCodeIn(categoryCodes)) {
                categoryByCode.put(category.getCode(), category);
            }
        }

        String username = RequestContext.getCurrentUsername();
        List<ProductEntity> toSave = new ArrayList<>();

        for (PendingProductRow row : pendingRows) {
            if (existingCodes.contains(row.code())) {
                errors.add("Row " + row.rowNum() + ": Product with code '" + row.code() + "' already exists");
                continue;
            }
            CategoryEntity category = categoryByCode.get(row.categoryCode());
            if (category == null) {
                errors.add("Row " + row.rowNum() + ": Category with code '" + row.categoryCode() + "' not found");
                continue;
            }

            ProductEntity product = new ProductEntity();
            product.setCode(row.code());
            product.setName(row.name());
            product.setUnit1(row.unit());
            product.setTax(row.tax() != null ? row.tax() : BigDecimal.ZERO);
            product.setMisaCode(row.misaCode() != null ? row.misaCode() : "");
            product.setCategory(category);
            product.setPrice(BigDecimal.ZERO);
            product.setCostPrice(BigDecimal.ZERO);
            product.setCreatedBy(username);
            product.setUpdatedBy(username);
            toSave.add(product);
        }

        if (!toSave.isEmpty()) {
            productRepo.saveAll(toSave);
        }
        return toSave.size();
    }

    private record PendingProductRow(
            int rowNum,
            String categoryCode,
            String code,
            String name,
            String unit,
            BigDecimal tax,
            String misaCode) {
    }
}
