package com.cnh.ies.service.export;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.cnh.ies.entity.customer.CustomerAddressEntity;
import com.cnh.ies.entity.customer.CustomerEntity;
import com.cnh.ies.entity.product.ProductEntity;
import com.cnh.ies.entity.vendors.VendorBanksEntity;
import com.cnh.ies.entity.vendors.VendorsEntity;
import com.cnh.ies.entity.warehouse.WarehouseInventoryEntity;
import com.cnh.ies.exception.ApiException;
import com.cnh.ies.repository.customer.CustomerRepo;
import com.cnh.ies.repository.product.ProductRepo;
import com.cnh.ies.repository.vendors.VendorsRepo;
import com.cnh.ies.repository.warehouse.WarehouseInventoryRepo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelExportService {

    private static final DateTimeFormatter INSTANT_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private final ProductRepo productRepo;
    private final VendorsRepo vendorsRepo;
    private final CustomerRepo customerRepo;
    private final WarehouseInventoryRepo warehouseInventoryRepo;

    public ExportWorkbookResult export(String type, String requestId) {
        return switch (type) {
            case ExportJobType.PRODUCTS -> exportProducts(requestId);
            case ExportJobType.VENDORS -> exportVendors(requestId);
            case ExportJobType.CUSTOMERS -> exportCustomers(requestId);
            case ExportJobType.WAREHOUSE_INVENTORY -> exportWarehouseInventory(requestId);
            default -> throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "Unsupported export type: " + type,
                    HttpStatus.BAD_REQUEST.value(), requestId);
        };
    }

    private ExportWorkbookResult exportProducts(String requestId) {
        List<ProductEntity> products = productRepo.findAllForExport();
        String[] headers = {
                "Code", "Name", "Category", "Unit 1", "Unit 2", "Price", "Tax", "MISA Code",
                "Cost Price", "Description", "Active", "Created At", "Updated At"
        };
        byte[] bytes = buildWorkbook("Products", headers, (sheet, headerRow) -> {
            int rowIdx = 1;
            for (ProductEntity p : products) {
                Row row = sheet.createRow(rowIdx++);
                int col = 0;
                setCell(row, col++, p.getCode());
                setCell(row, col++, p.getName());
                setCell(row, col++, p.getCategory() != null ? p.getCategory().getName() : null);
                setCell(row, col++, p.getUnit1());
                setCell(row, col++, p.getUnit2());
                setCell(row, col++, decimalToString(p.getPrice()));
                setCell(row, col++, decimalToString(p.getTax()));
                setCell(row, col++, p.getMisaCode());
                setCell(row, col++, decimalToString(p.getCostPrice()));
                setCell(row, col++, p.getDescription());
                setCell(row, col++, boolToString(p.getIsActive()));
                setCell(row, col++, formatInstant(p.getCreatedAt()));
                setCell(row, col, formatInstant(p.getUpdatedAt()));
            }
        }, requestId);
        return new ExportWorkbookResult(bytes, buildFileName(ExportJobType.PRODUCTS));
    }

    private ExportWorkbookResult exportVendors(String requestId) {
        List<VendorsEntity> vendors = vendorsRepo.findAllForExport();
        String[] headers = {
                "Code", "Name", "Email", "Phone", "Country", "Currency", "MISA Code", "Tax Code",
                "Contact Person", "Address", "Active", "Bank Accounts", "Created At", "Updated At"
        };
        byte[] bytes = buildWorkbook("Vendors", headers, (sheet, headerRow) -> {
            int rowIdx = 1;
            for (VendorsEntity v : vendors) {
                Row row = sheet.createRow(rowIdx++);
                int col = 0;
                setCell(row, col++, v.getCode());
                setCell(row, col++, v.getName());
                setCell(row, col++, v.getEmail());
                setCell(row, col++, v.getPhone());
                setCell(row, col++, v.getCountry());
                setCell(row, col++, v.getCurrency());
                setCell(row, col++, v.getMisaCode());
                setCell(row, col++, v.getTaxCode());
                setCell(row, col++, v.getContactPerson());
                setCell(row, col++, v.getAddress());
                setCell(row, col++, boolToString(v.getIsActive()));
                setCell(row, col++, formatBanks(v.getBanks()));
                setCell(row, col++, formatInstant(v.getCreatedAt()));
                setCell(row, col, formatInstant(v.getUpdatedAt()));
            }
        }, requestId);
        return new ExportWorkbookResult(bytes, buildFileName(ExportJobType.VENDORS));
    }

    private ExportWorkbookResult exportCustomers(String requestId) {
        List<CustomerEntity> customers = customerRepo.findAllForExport();
        String[] headers = {
                "Code", "Name", "Email", "Phone", "Tax Code", "MISA Code", "Active",
                "Addresses", "Created At", "Updated At"
        };
        byte[] bytes = buildWorkbook("Customers", headers, (sheet, headerRow) -> {
            int rowIdx = 1;
            for (CustomerEntity c : customers) {
                Row row = sheet.createRow(rowIdx++);
                int col = 0;
                setCell(row, col++, c.getCode());
                setCell(row, col++, c.getName());
                setCell(row, col++, c.getEmail());
                setCell(row, col++, c.getPhone());
                setCell(row, col++, c.getTaxCode());
                setCell(row, col++, c.getMisaCode());
                setCell(row, col++, boolToString(c.getIsActive()));
                setCell(row, col++, formatAddresses(c.getAddresses()));
                setCell(row, col++, formatInstant(c.getCreatedAt()));
                setCell(row, col, formatInstant(c.getUpdatedAt()));
            }
        }, requestId);
        return new ExportWorkbookResult(bytes, buildFileName(ExportJobType.CUSTOMERS));
    }

    private ExportWorkbookResult exportWarehouseInventory(String requestId) {
        List<WarehouseInventoryEntity> inventory = warehouseInventoryRepo.findAllForExport();
        String[] headers = {
                "Product Code", "Product Name", "Category", "UOM", "Quantity On Hand", "Created By"
        };
        byte[] bytes = buildWorkbook("Warehouse Inventory", headers, (sheet, headerRow) -> {
            int rowIdx = 1;
            for (WarehouseInventoryEntity inv : inventory) {
                var product = inv.getProduct();
                Row row = sheet.createRow(rowIdx++);
                int col = 0;
                setCell(row, col++, product.getCode());
                setCell(row, col++, product.getName());
                setCell(row, col++, product.getCategory() != null ? product.getCategory().getName() : null);
                setCell(row, col++, product.getUnit1());
                setCell(row, col++, decimalToString(inv.getQuantityOnHand()));
                setCell(row, col, inv.getCreatedBy());
            }
        }, requestId);
        return new ExportWorkbookResult(bytes, buildFileName(ExportJobType.WAREHOUSE_INVENTORY));
    }

    private byte[] buildWorkbook(
            String sheetName,
            String[] headers,
            SheetWriter writer,
            String requestId) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(sheetName);
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }
            writer.write(sheet, headerRow);
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            log.error("Failed to build Excel workbook: {}", e.getMessage(), e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR,
                    "Failed to generate Excel file: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }

    private static String buildFileName(String type) {
        String prefix = ExportJobType.fileNamePrefix(type);
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").withZone(ZoneId.systemDefault())
                .format(Instant.now());
        return prefix + "_export_" + timestamp + ".xlsx";
    }

    private static void setCell(Row row, int col, String value) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value != null ? value : "");
    }

    private static String decimalToString(BigDecimal value) {
        return value != null ? value.toPlainString() : "";
    }

    private static String boolToString(Boolean value) {
        if (value == null) {
            return "";
        }
        return Boolean.TRUE.equals(value) ? "Yes" : "No";
    }

    private static String formatInstant(Instant instant) {
        return instant != null ? INSTANT_FORMATTER.format(instant) : "";
    }

    private static String formatBanks(java.util.Set<VendorBanksEntity> banks) {
        if (banks == null || banks.isEmpty()) {
            return "";
        }
        return banks.stream()
                .filter(b -> !Boolean.TRUE.equals(b.getIsDeleted()))
                .map(b -> String.join(" | ",
                        nullToEmpty(b.getBankName()),
                        nullToEmpty(b.getBankAccountNumber()),
                        nullToEmpty(b.getBankAccountName())))
                .collect(Collectors.joining("; "));
    }

    private static String formatAddresses(java.util.Set<CustomerAddressEntity> addresses) {
        if (addresses == null || addresses.isEmpty()) {
            return "";
        }
        return addresses.stream()
                .filter(a -> !Boolean.TRUE.equals(a.getIsDeleted()))
                .map(a -> String.join(" | ",
                        nullToEmpty(a.getAddress()),
                        nullToEmpty(a.getContactPerson()),
                        nullToEmpty(a.getPhone())))
                .collect(Collectors.joining("; "));
    }

    private static String nullToEmpty(String s) {
        return s != null ? s : "";
    }

    @FunctionalInterface
    private interface SheetWriter {
        void write(Sheet sheet, Row headerRow);
    }

    public record ExportWorkbookResult(byte[] content, String fileName) {}
}
