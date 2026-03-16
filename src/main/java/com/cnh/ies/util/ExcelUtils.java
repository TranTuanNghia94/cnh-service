package com.cnh.ies.util;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

public final class ExcelUtils {

    private ExcelUtils() {}

    public enum TemplateType {
        PRODUCT(Arrays.asList("MÃ DANH MỤC", "MÃ SẢN PHẨM", "TÊN SẢN PHẨM", "ĐƠN VỊ", "THUẾ", "MÃ MISA")),
        VENDOR(Arrays.asList("MÃ VENDOR", "TÊN VENDOR", "MÃ MISA", "TIỀN TỆ", "QUỐC GIA", "SDT", "ĐỊA CHỈ", "NGÂN HÀNG", "STK", "TÊN TK", "CHI NHÁNH")),
        CUSTOMER(Arrays.asList("MÃ KH", "TÊN KH", "MÃ MISA", "EMAIL", "SDT", "NGƯỜI LIÊN HỆ", "ĐỊA CHỈ")),
        UNKNOWN(List.of());

        private final List<String> expectedHeaders;

        TemplateType(List<String> expectedHeaders) {
            this.expectedHeaders = expectedHeaders;
        }

        public List<String> getExpectedHeaders() {
            return expectedHeaders;
        }
    }

    public static TemplateType detectTemplateType(Sheet sheet) {
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) return TemplateType.UNKNOWN;

        String firstHeader = getString(headerRow, 0);
        if (isBlank(firstHeader)) return TemplateType.UNKNOWN;

        String normalized = firstHeader.toUpperCase().trim();
        if (normalized.contains("VENDOR")) return TemplateType.VENDOR;
        if (normalized.contains("DANH MỤC") || normalized.contains("SẢN PHẨM")) return TemplateType.PRODUCT;
        if (normalized.contains("KH") || normalized.contains("KHÁCH HÀNG")) return TemplateType.CUSTOMER;

        return TemplateType.UNKNOWN;
    }

    public static String validateHeaders(Sheet sheet, TemplateType expectedType) {
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) return "Header row is missing";

        List<String> expected = expectedType.getExpectedHeaders();
        for (int i = 0; i < expected.size(); i++) {
            String actual = getString(headerRow, i);
            if (isBlank(actual) || !actual.equalsIgnoreCase(expected.get(i))) {
                return "Invalid header at column " + (i + 1) + ": expected '" + expected.get(i) + "' but found '" + actual + "'";
            }
        }
        return null;
    }

    public static String getString(Row row, int index) {
        Cell cell = row.getCell(index);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue().trim();
        } else if (cell.getCellType() == CellType.NUMERIC) {
            return String.valueOf((long) cell.getNumericCellValue());
        }
        return null;
    }

    public static BigDecimal getNumeric(Row row, int index) {
        Cell cell = row.getCell(index);
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

    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
