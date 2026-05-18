package com.cnh.ies.service.order;

import static com.cnh.ies.util.ExcelUtils.TemplateType;
import static com.cnh.ies.util.ExcelUtils.getBoolean;
import static com.cnh.ies.util.ExcelUtils.getDate;
import static com.cnh.ies.util.ExcelUtils.getNumeric;
import static com.cnh.ies.util.ExcelUtils.getString;
import static com.cnh.ies.util.ExcelUtils.isBlank;
import static com.cnh.ies.util.ExcelUtils.validateHeaders;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Locale;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cnh.ies.constant.Constant;
import com.cnh.ies.dto.response.UploadOjectResponse;
import com.cnh.ies.model.order.BatchOrderImportEntityItem;
import com.cnh.ies.model.order.BatchOrderImportErrorItem;
import com.cnh.ies.model.order.BatchOrderImportOrderCreated;
import com.cnh.ies.model.order.BatchOrderImportResultSummary;
import com.cnh.ies.entity.customer.CustomerEntity;
import com.cnh.ies.entity.order.OrderEntity;
import com.cnh.ies.entity.order.OrderLineEntity;
import com.cnh.ies.entity.product.CategoryEntity;
import com.cnh.ies.entity.product.ProductEntity;
import com.cnh.ies.entity.vendors.VendorsEntity;
import com.cnh.ies.exception.ApiException;
import com.cnh.ies.repository.customer.CustomerRepo;
import com.cnh.ies.repository.order.OrderLineRepo;
import com.cnh.ies.repository.order.OrderRepo;
import com.cnh.ies.repository.product.CategoryRepo;
import com.cnh.ies.repository.product.ProductRepo;
import com.cnh.ies.repository.vendors.VendorsRepo;
import com.cnh.ies.util.RequestContext;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UploadBatchOrderService {

    private final CustomerRepo customerRepo;
    private final ProductRepo productRepo;
    private final VendorsRepo vendorsRepo;
    private final CategoryRepo categoryRepo;
    private final OrderRepo orderRepo;
    private final OrderLineRepo orderLineRepo;
    private final OrderNumberService orderNumberService;

    @Transactional
    public UploadOjectResponse readExcelFile(MultipartFile file, String requestId) {
        return readExcelFile(file, requestId, null);
    }

    @Transactional
    public UploadOjectResponse readExcelFile(MultipartFile file, String requestId, String createdBy) {
        try {
            return readExcelInputStream(file.getInputStream(), requestId, createdBy);
        } catch (Exception e) {
            log.error("Error opening excel file stream: {}", e.getMessage(), e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR,
                    "Error reading excel file: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    requestId);
        }
    }

    @Transactional
    public UploadOjectResponse readExcelInputStream(InputStream inputStream, String requestId) {
        return readExcelInputStream(inputStream, requestId, null);
    }

    @Transactional
    public UploadOjectResponse readExcelInputStream(InputStream inputStream, String requestId, String createdBy) {
        String actor = resolveActor(createdBy);
        log.info("Reading batch order excel file, requestId: {}, createdBy: {}", requestId, actor);
        List<String> warnings = new ArrayList<>();
        List<BatchOrderImportEntityItem> newProducts = new ArrayList<>();
        List<BatchOrderImportEntityItem> newVendors = new ArrayList<>();
        List<BatchOrderImportOrderCreated> ordersCreated = new ArrayList<>();

        try (var workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);

            String headerError = validateHeaders(sheet, TemplateType.BATCH_ORDER);
            if (headerError != null) {
                throw new ApiException(ApiException.ErrorCode.INVALID_REQUEST,
                        "Invalid template: " + headerError, HttpStatus.BAD_REQUEST.value(), requestId);
            }

            List<BatchOrderRow> rows = new ArrayList<>();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isEmptyDataRow(row)) {
                    continue;
                }
                rows.add(parseAndValidateRow(row, i, requestId));
            }

            if (rows.isEmpty()) {
                throw new ApiException(ApiException.ErrorCode.INVALID_REQUEST, "No data rows in file",
                        HttpStatus.BAD_REQUEST.value(), requestId);
            }

            Map<String, CustomerEntity> customerByCode = loadCustomersOrThrow(rows, requestId);

            Map<String, ProductEntity> productByCode = new java.util.HashMap<>();
            Map<String, VendorsEntity> vendorByCode = new java.util.HashMap<>();

            for (BatchOrderRow r : rows) {
                resolveProduct(r, productByCode, warnings, newProducts, requestId, actor);
                resolveVendor(r, vendorByCode, warnings, newVendors, actor);
            }

            Map<OrderKey, List<BatchOrderRow>> grouped = groupRows(rows);

            String orderPrefix = orderNumberService.generateOrderPrefix();
            int nextOrderNumber = orderNumberService.reserveNextOrderNumbers(orderPrefix, grouped.size());

            for (Map.Entry<OrderKey, List<BatchOrderRow>> entry : grouped.entrySet()) {
                OrderKey key = entry.getKey();
                List<BatchOrderRow> groupRows = entry.getValue();
                CustomerEntity customer = customerByCode.get(key.customerCode());

                BigDecimal orderTotal = BigDecimal.ZERO;
                for (BatchOrderRow r : groupRows) {
                    orderTotal = orderTotal.add(lineTotalAmount(r));
                }
                orderTotal = orderTotal.setScale(0, RoundingMode.HALF_UP);

                OrderEntity order = new OrderEntity();
                order.setCustomer(customer);
                order.setCustomerAddress(null);
                order.setContractNumber(key.contractNumber());
                order.setOrderDate(key.orderDate());
                order.setDeliveryDate(null);
                order.setStatus(Constant.ORDER_STATUS_DRAFT);
                order.setTotalAmount(orderTotal);
                order.setDiscountAmount(BigDecimal.ZERO);
                order.setIsIncludedTax(groupRows.get(0).isIncludedTax());
                order.setTaxRate(BigDecimal.ZERO);
                order.setTaxAmount(BigDecimal.ZERO);
                order.setFinalAmount(orderTotal);
                order.setNotes(null);
                order.setOrderPrefix(orderPrefix);
                order.setOrderNumber(nextOrderNumber++);
                order.setCreatedBy(actor);
                order.setUpdatedBy(actor);
                order.setIsDeleted(false);

                OrderEntity savedOrder = orderRepo.saveAndFlush(order);

                List<OrderLineEntity> lineEntities = new ArrayList<>();
                for (BatchOrderRow r : groupRows) {
                    ProductEntity product = productByCode.get(normalizeCodeKey(r.productCode()));
                    VendorsEntity vendor = vendorByCode.get(normalizeCodeKey(r.vendorCode()));
                    BigDecimal lineTotal = lineTotalAmount(r).setScale(0, RoundingMode.HALF_UP);

                    OrderLineEntity line = new OrderLineEntity();
                    line.setOrder(savedOrder);
                    line.setProduct(product);
                    line.setVendor(vendor);
                    line.setProductCodeSuggest(limitText(r.productCode(), 200));
                    line.setProductNameSuggest(limitText(r.productName(), 200));
                    line.setVendorCodeSuggest(limitText(r.vendorCode(), 200));
                    line.setVendorNameSuggest(limitText(r.vendorName(), 200));
                    line.setQuantity(r.quantity());
                    line.setUnitPrice(r.unitPrice().setScale(0, RoundingMode.HALF_UP));
                    line.setUom(r.normalizedUom());
                    line.setDiscountPercent(BigDecimal.ZERO);
                    line.setDiscountAmount(BigDecimal.ZERO);
                    line.setIsIncludedTax(r.isIncludedTax());
                    line.setTaxRate(BigDecimal.ZERO);
                    line.setTaxAmount(BigDecimal.ZERO);
                    line.setTotalAmount(lineTotal);
                    line.setNotes(null);
                    line.setReceiverNote(r.receiverNote());
                    line.setDeliveryNote(r.deliveryNote());
                    line.setReferenceNote(null);
                    line.setCreatedBy(actor);
                    line.setUpdatedBy(actor);
                    line.setIsDeleted(false);
                    lineEntities.add(line);
                }

                orderLineRepo.saveAll(lineEntities);

                String orderCode = orderPrefix + "." + savedOrder.getOrderNumber();
                ordersCreated.add(BatchOrderImportOrderCreated.builder()
                        .orderId(savedOrder.getId().toString())
                        .orderCode(orderCode)
                        .contractNumber(key.contractNumber())
                        .customerCode(key.customerCode())
                        .lineCount(groupRows.size())
                        .build());
            }

            BatchOrderImportResultSummary importSummary = BatchOrderImportResultSummary.builder()
                    .totalRows(rows.size())
                    .ordersCreatedCount(ordersCreated.size())
                    .newProductsCount(newProducts.size())
                    .newVendorsCount(newVendors.size())
                    .warningCount(warnings.size())
                    .errorCount(0)
                    .ordersCreated(ordersCreated)
                    .newProducts(newProducts)
                    .newVendors(newVendors)
                    .errors(Collections.emptyList())
                    .warnings(warnings)
                    .build();

            int firstOrderNum = nextOrderNumber - grouped.size();
            int lastOrderNum = nextOrderNumber - 1;
            log.info(
                    "Batch order import completed: {} rows, {} orders ({}.{}{}), {} new products, {} new vendors, {} warnings",
                    rows.size(),
                    grouped.size(),
                    orderPrefix,
                    firstOrderNum,
                    firstOrderNum == lastOrderNum ? "" : ".." + lastOrderNum,
                    newProducts.size(),
                    newVendors.size(),
                    warnings.size());

            return UploadOjectResponse.builder()
                    .message("Import completed successfully")
                    .totalRows(rows.size())
                    .totalSuccess(rows.size())
                    .totalErrors(0)
                    .errors(Collections.emptyList())
                    .warnings(warnings)
                    .importSummary(importSummary)
                    .build();

        } catch (ApiException ex) {
            throw ex;
        } catch (Exception e) {
            log.error("Error reading batch order excel file: {}", e.getMessage(), e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR,
                    "Error reading excel file: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    requestId);
        }
    }

    private boolean isEmptyDataRow(Row row) {
        return isBlank(getString(row, 0))
                && isBlank(getString(row, 1))
                && getDate(row, 2) == null
                && isBlank(getString(row, 3));
    }

    private BatchOrderRow parseAndValidateRow(Row row, int rowNum, String requestId) {
        String customerCode = getString(row, 0);
        String contractNumber = getString(row, 1);
        LocalDate orderDate = getDate(row, 2);
        String productCode = getString(row, 3);
        String productName = getString(row, 4);
        String vendorCode = getString(row, 5);
        String vendorName = getString(row, 6);
        String rawUom = Optional.ofNullable(getString(row, 7)).orElse("").trim();
        BigDecimal quantity = getNumeric(row, 8);
        BigDecimal unitPrice = getNumeric(row, 9);
        Boolean isIncludedTax = getBoolean(row, 10);
        String receiverNote = getString(row, 11);
        String deliveryNote = getString(row, 12);

        if (isBlank(customerCode)) {
            badRequest("Row " + rowNum + ": Customer code is required", requestId);
        }
        if (isBlank(contractNumber)) {
            badRequest("Row " + rowNum + ": Contract number is required", requestId);
        }
        if (orderDate == null) {
            badRequest("Row " + rowNum + ": Order date is required", requestId);
        }
        if (isBlank(productCode)) {
            badRequest("Row " + rowNum + ": Product code is required", requestId);
        }
        if (isBlank(productName)) {
            badRequest("Row " + rowNum + ": Product name is required", requestId);
        }
        if (isBlank(vendorCode)) {
            badRequest("Row " + rowNum + ": Vendor code is required", requestId);
        }
        if (isBlank(vendorName)) {
            badRequest("Row " + rowNum + ": Vendor name is required", requestId);
        }
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            badRequest("Row " + rowNum + ": Quantity must be a positive number", requestId);
        }
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            badRequest("Row " + rowNum + ": Unit price must be zero or positive", requestId);
        }
        if (isIncludedTax == null) {
            badRequest("Row " + rowNum + ": Included tax must be true/false (e.g. true, false, 1, 0)", requestId);
        }

        BigDecimal quantityScaled = Objects.requireNonNull(quantity).setScale(3, RoundingMode.HALF_UP);
        BigDecimal unitPriceValid = Objects.requireNonNull(unitPrice);

        String normalizedUom = normalizeUom(rawUom);

        return new BatchOrderRow(
                rowNum,
                customerCode.trim(),
                contractNumber.trim(),
                orderDate,
                productCode.trim(),
                productName.trim(),
                vendorCode.trim(),
                vendorName.trim(),
                rawUom,
                normalizedUom,
                quantityScaled,
                unitPriceValid,
                isIncludedTax,
                receiverNote,
                deliveryNote);
    }

    private static String normalizeUom(String rawUom) {
        if (isBlank(rawUom)) {
            return Constant.UOM_ELT;
        }
        if (Constant.UOM_UNN.equalsIgnoreCase(rawUom.trim())) {
            return Constant.UOM_UNN;
        }
        return Constant.UOM_ELT;
    }

    private Map<String, CustomerEntity> loadCustomersOrThrow(List<BatchOrderRow> rows, String requestId) {
        List<String> distinctCodes = rows.stream()
                .map(BatchOrderRow::customerCode)
                .distinct()
                .collect(Collectors.toList());

        Map<String, CustomerEntity> map = new java.util.HashMap<>();
        for (String code : distinctCodes) {
            CustomerEntity c = customerRepo.findByCode(code)
                    .orElseThrow(() -> new ApiException(ApiException.ErrorCode.INVALID_REQUEST,
                            "Customer not found for code: " + code, HttpStatus.BAD_REQUEST.value(), requestId));
            map.put(code, c);
        }
        return map;
    }

    private void resolveProduct(
            BatchOrderRow r,
            Map<String, ProductEntity> productByCode,
            List<String> warnings,
            List<BatchOrderImportEntityItem> newProducts,
            String requestId,
            String actor) {
        String code = r.productCode();
        String codeKey = normalizeCodeKey(code);
        if (productByCode.containsKey(codeKey)) {
            return;
        }
        Optional<ProductEntity> existing = productRepo.findByCodeIgnoreCase(code);
        if (existing.isPresent()) {
            productByCode.put(codeKey, existing.get());
            return;
        }

        boolean isUnn = Constant.UOM_UNN.equals(r.normalizedUom());
        CategoryEntity category = null;
        if (isUnn) {
            category = categoryRepo.findByName(Constant.CATEGORY_NAME_SACH)
                    .orElseThrow(() -> new ApiException(ApiException.ErrorCode.INVALID_REQUEST,
                            "Category '" + Constant.CATEGORY_NAME_SACH
                                    + "' not found; required for new products with UOM " + Constant.UOM_UNN,
                            HttpStatus.BAD_REQUEST.value(), requestId));
        }

        ProductEntity product = new ProductEntity();
        product.setCode(code);
        product.setName(r.productName());
        product.setCategory(category);
        product.setUnit1(isUnn ? Constant.UOM_UNN : Constant.UOM_ELT);
        product.setTax(BigDecimal.ZERO);
        product.setMisaCode("");
        product.setPrice(BigDecimal.ZERO);
        product.setCostPrice(BigDecimal.ZERO);
        product.setIsActive(true);
        product.setCreatedBy(actor);
        product.setUpdatedBy(actor);

        productRepo.save(product);
        productByCode.put(codeKey, product);
        String warning = "Auto-created product '" + code + " - " + r.productName() + "' on row " + r.rowNum();
        warnings.add(warning);
        newProducts.add(BatchOrderImportEntityItem.builder()
                .code(code)
                .name(r.productName())
                .rowNum(r.rowNum())
                .build());
    }

    private void resolveVendor(
            BatchOrderRow r,
            Map<String, VendorsEntity> vendorByCode,
            List<String> warnings,
            List<BatchOrderImportEntityItem> newVendors,
            String actor) {
        String code = r.vendorCode();
        String codeKey = normalizeCodeKey(code);
        if (vendorByCode.containsKey(codeKey)) {
            return;
        }
        Optional<VendorsEntity> existing = vendorsRepo.findByCodeIgnoreCase(code);
        if (existing.isPresent()) {
            vendorByCode.put(codeKey, existing.get());
            return;
        }

        VendorsEntity vendor = new VendorsEntity();
        vendor.setCode(code);
        vendor.setName(r.vendorName());
        vendor.setIsActive(true);
        vendor.setCreatedBy(actor);
        vendor.setUpdatedBy(actor);

        vendorsRepo.save(vendor);
        vendorByCode.put(codeKey, vendor);
        String warning = "Auto-created vendor '" + code + " - " + r.vendorName() + "' on row " + r.rowNum();
        warnings.add(warning);
        newVendors.add(BatchOrderImportEntityItem.builder()
                .code(code)
                .name(r.vendorName())
                .rowNum(r.rowNum())
                .build());
    }

    private Map<OrderKey, List<BatchOrderRow>> groupRows(List<BatchOrderRow> rows) {
        Map<OrderKey, List<BatchOrderRow>> grouped = new LinkedHashMap<>();
        for (BatchOrderRow r : rows) {
            OrderKey key = new OrderKey(r.customerCode(), r.contractNumber(), r.orderDate());
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(r);
        }
        return grouped;
    }

    private BigDecimal lineTotalAmount(BatchOrderRow r) {
        return r.quantity().multiply(r.unitPrice());
    }

    private String normalizeCodeKey(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String limitText(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private void badRequest(String message, String requestId) {
        throw new ApiException(ApiException.ErrorCode.INVALID_REQUEST, message, HttpStatus.BAD_REQUEST.value(),
                requestId);
    }

    private static String resolveActor(String createdBy) {
        if (createdBy != null && !createdBy.isBlank()) {
            return createdBy.trim();
        }
        String fromContext = RequestContext.getCurrentUsername();
        if (fromContext != null && !fromContext.isBlank()) {
            return fromContext;
        }
        return "SYSTEM";
    }

    private record BatchOrderRow(
            int rowNum,
            String customerCode,
            String contractNumber,
            LocalDate orderDate,
            String productCode,
            String productName,
            String vendorCode,
            String vendorName,
            String rawUom,
            String normalizedUom,
            BigDecimal quantity,
            BigDecimal unitPrice,
            Boolean isIncludedTax,
            String receiverNote,
            String deliveryNote) {
    }

    private record OrderKey(String customerCode, String contractNumber, LocalDate orderDate) {
    }
}
