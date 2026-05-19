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
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Locale;
import java.util.Set;
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

import com.cnh.ies.config.Loggable;

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
    @Loggable(slowThresholdMs = 120_000)
    public UploadOjectResponse readExcelInputStream(InputStream inputStream, String requestId, String createdBy) {
        String actor = resolveActor(createdBy);
        log.debug("Reading batch order excel file, requestId: {}, createdBy: {}", requestId, actor);
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
            Map<String, ProductEntity> productByCode =
                    resolveProducts(rows, warnings, newProducts, requestId, actor);
            Map<String, VendorsEntity> vendorByCode = resolveVendors(rows, warnings, newVendors, actor);

            Map<OrderKey, List<BatchOrderRow>> grouped = groupRows(rows);

            String orderPrefix = orderNumberService.generateOrderPrefix();
            int nextOrderNumber = orderNumberService.reserveNextOrderNumbers(orderPrefix, grouped.size());

            List<OrderEntity> ordersToSave = new ArrayList<>(grouped.size());
            List<List<OrderLineEntity>> linesPerOrder = new ArrayList<>(grouped.size());
            List<OrderKey> orderKeys = new ArrayList<>(grouped.size());

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

                List<OrderLineEntity> lineEntities = new ArrayList<>(groupRows.size());
                for (BatchOrderRow r : groupRows) {
                    ProductEntity product = productByCode.get(normalizeCodeKey(r.productCode()));
                    VendorsEntity vendor = vendorByCode.get(normalizeCodeKey(r.vendorCode()));
                    BigDecimal lineTotal = lineTotalAmount(r).setScale(0, RoundingMode.HALF_UP);

                    OrderLineEntity line = new OrderLineEntity();
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

                ordersToSave.add(order);
                linesPerOrder.add(lineEntities);
                orderKeys.add(key);
            }

            List<OrderEntity> savedOrders = orderRepo.saveAll(ordersToSave);
            List<OrderLineEntity> allLines = new ArrayList<>();
            for (int i = 0; i < savedOrders.size(); i++) {
                OrderEntity savedOrder = savedOrders.get(i);
                for (OrderLineEntity line : linesPerOrder.get(i)) {
                    line.setOrder(savedOrder);
                    allLines.add(line);
                }
                OrderKey key = orderKeys.get(i);
                String orderCode = orderPrefix + "." + savedOrder.getOrderNumber();
                ordersCreated.add(BatchOrderImportOrderCreated.builder()
                        .orderId(savedOrder.getId().toString())
                        .orderCode(orderCode)
                        .contractNumber(key.contractNumber())
                        .customerCode(key.customerCode())
                        .lineCount(linesPerOrder.get(i).size())
                        .build());
            }
            orderLineRepo.saveAll(allLines);

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
                    "Tạo đơn hàng thành công: {} dòng, {} đơn hàng ({}.{}{}), {} sản phẩm mới, {} nhà cung cấp mới, {} warnings",
                    rows.size(),
                    grouped.size(),
                    orderPrefix,
                    firstOrderNum,
                    firstOrderNum == lastOrderNum ? "" : ".." + lastOrderNum,
                    newProducts.size(),
                    newVendors.size(),
                    warnings.size());

            return UploadOjectResponse.builder()
                    .message("Tạo đơn hàng thành công")
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
        Set<String> distinctCodes = rows.stream()
                .map(BatchOrderRow::customerCode)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));

        Map<String, CustomerEntity> map = new LinkedHashMap<>();
        if (!distinctCodes.isEmpty()) {
            for (CustomerEntity customer : customerRepo.findByCodeInAndIsDeletedFalse(distinctCodes)) {
                map.put(customer.getCode(), customer);
            }
        }
        for (String code : distinctCodes) {
            if (!map.containsKey(code)) {
                throw new ApiException(ApiException.ErrorCode.INVALID_REQUEST,
                        "Customer not found for code: " + code, HttpStatus.BAD_REQUEST.value(), requestId);
            }
        }
        return map;
    }

    private Map<String, ProductEntity> resolveProducts(
            List<BatchOrderRow> rows,
            List<String> warnings,
            List<BatchOrderImportEntityItem> newProducts,
            String requestId,
            String actor) {
        Map<String, BatchOrderRow> firstRowByCodeKey = new LinkedHashMap<>();
        for (BatchOrderRow row : rows) {
            firstRowByCodeKey.putIfAbsent(normalizeCodeKey(row.productCode()), row);
        }

        Map<String, ProductEntity> productByCode = new LinkedHashMap<>();
        Collection<String> codes = firstRowByCodeKey.values().stream()
                .map(BatchOrderRow::productCode)
                .map(code -> code.toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
        if (!codes.isEmpty()) {
            for (ProductEntity product : productRepo.findByCodeInIgnoreCaseAndIsDeletedFalse(codes)) {
                productByCode.put(normalizeCodeKey(product.getCode()), product);
            }
        }

        CategoryEntity categoryForUnn = null;
        List<ProductEntity> toCreate = new ArrayList<>();
        for (Map.Entry<String, BatchOrderRow> entry : firstRowByCodeKey.entrySet()) {
            if (productByCode.containsKey(entry.getKey())) {
                continue;
            }
            BatchOrderRow row = entry.getValue();
            String code = row.productCode();
            boolean isUnn = Constant.UOM_UNN.equals(row.normalizedUom());
            if (isUnn && categoryForUnn == null) {
                categoryForUnn = categoryRepo.findByName(Constant.CATEGORY_NAME_SACH)
                        .orElseThrow(() -> new ApiException(ApiException.ErrorCode.INVALID_REQUEST,
                                "Category '" + Constant.CATEGORY_NAME_SACH
                                        + "' not found; required for new products with UOM " + Constant.UOM_UNN,
                                HttpStatus.BAD_REQUEST.value(), requestId));
            }

            ProductEntity product = new ProductEntity();
            product.setCode(code);
            product.setName(row.productName());
            product.setCategory(isUnn ? categoryForUnn : null);
            product.setUnit1(isUnn ? Constant.UOM_UNN : Constant.UOM_ELT);
            product.setTax(BigDecimal.ZERO);
            product.setMisaCode("");
            product.setPrice(BigDecimal.ZERO);
            product.setCostPrice(BigDecimal.ZERO);
            product.setIsActive(true);
            product.setCreatedBy(actor);
            product.setUpdatedBy(actor);

            toCreate.add(product);
            productByCode.put(entry.getKey(), product);
            warnings.add("Auto-created product '" + code + " - " + row.productName() + "' on row " + row.rowNum());
            newProducts.add(BatchOrderImportEntityItem.builder()
                    .code(code)
                    .name(row.productName())
                    .rowNum(row.rowNum())
                    .build());
        }
        if (!toCreate.isEmpty()) {
            productRepo.saveAll(toCreate);
        }
        return productByCode;
    }

    private Map<String, VendorsEntity> resolveVendors(
            List<BatchOrderRow> rows,
            List<String> warnings,
            List<BatchOrderImportEntityItem> newVendors,
            String actor) {
        Map<String, BatchOrderRow> firstRowByCodeKey = new LinkedHashMap<>();
        for (BatchOrderRow row : rows) {
            firstRowByCodeKey.putIfAbsent(normalizeCodeKey(row.vendorCode()), row);
        }

        Map<String, VendorsEntity> vendorByCode = new LinkedHashMap<>();
        Collection<String> codes = firstRowByCodeKey.values().stream()
                .map(BatchOrderRow::vendorCode)
                .map(code -> code.toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
        if (!codes.isEmpty()) {
            for (VendorsEntity vendor : vendorsRepo.findByCodeInIgnoreCaseAndIsDeletedFalse(codes)) {
                vendorByCode.put(normalizeCodeKey(vendor.getCode()), vendor);
            }
        }

        List<VendorsEntity> toCreate = new ArrayList<>();
        for (Map.Entry<String, BatchOrderRow> entry : firstRowByCodeKey.entrySet()) {
            if (vendorByCode.containsKey(entry.getKey())) {
                continue;
            }
            BatchOrderRow row = entry.getValue();
            String code = row.vendorCode();

            VendorsEntity vendor = new VendorsEntity();
            vendor.setCode(code);
            vendor.setName(row.vendorName());
            vendor.setIsActive(true);
            vendor.setCreatedBy(actor);
            vendor.setUpdatedBy(actor);

            toCreate.add(vendor);
            vendorByCode.put(entry.getKey(), vendor);
            warnings.add("Auto-created vendor '" + code + " - " + row.vendorName() + "' on row " + row.rowNum());
            newVendors.add(BatchOrderImportEntityItem.builder()
                    .code(code)
                    .name(row.vendorName())
                    .rowNum(row.rowNum())
                    .build());
        }
        if (!toCreate.isEmpty()) {
            vendorsRepo.saveAll(toCreate);
        }
        return vendorByCode;
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
