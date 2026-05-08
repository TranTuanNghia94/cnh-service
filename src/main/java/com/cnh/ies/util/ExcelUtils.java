package com.cnh.ies.util;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class ExcelUtils {

    private ExcelUtils() {}

    public enum TemplateType {
        PRODUCT(Arrays.asList("MÃ DANH MỤC", "MÃ SẢN PHẨM", "TÊN SẢN PHẨM", "ĐƠN VỊ", "THUẾ", "MÃ MISA")),
        VENDOR(Arrays.asList("MÃ VENDOR", "TÊN VENDOR", "MÃ MISA", "TIỀN TỆ", "QUỐC GIA", "SDT", "ĐỊA CHỈ", "NGÂN HÀNG", "STK", "TÊN TK", "CHI NHÁNH")),
        CUSTOMER(Arrays.asList("MÃ KH", "TÊN KH", "MÃ MISA", "EMAIL", "SDT", "NGƯỜI LIÊN HỆ", "ĐỊA CHỈ")),
        BATCH_ORDER(Arrays.asList(
                "MÃ KH",
                "SỐ HỢP ĐỒNG",
                "NGÀY ĐẶT",
                "MÃ SẢN PHẨM",
                "TÊN SẢN PHẨM",
                "MÃ NCC",
                "TÊN NCC",
                "ĐVT",
                "SỐ LƯỢNG",
                "ĐƠN GIÁ",
                "BAO GỒM THUẾ",
                "GHI CHÚ NHẬN",
                "GHI CHÚ GIAO")),
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
        CellType cellType = cell.getCellType();
        if (cellType == CellType.FORMULA) {
            cellType = cell.getCachedFormulaResultType();
        }
        if (cellType == CellType.STRING) {
            return cell.getStringCellValue().trim();
        } else if (cellType == CellType.NUMERIC) {
            return String.valueOf((long) cell.getNumericCellValue());
        } else if (cellType == CellType.BOOLEAN) {
            return String.valueOf(cell.getBooleanCellValue());
        }
        return null;
    }

    public static BigDecimal getNumeric(Row row, int index) {
        Cell cell = row.getCell(index);
        if (cell == null) return null;
        CellType cellType = cell.getCellType();
        if (cellType == CellType.FORMULA) {
            cellType = cell.getCachedFormulaResultType();
        }
        if (cellType == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue());
        } else if (cellType == CellType.STRING) {
            try {
                return new BigDecimal(cell.getStringCellValue().trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DMY_SLASH = DateTimeFormatter.ofPattern("d/M/yyyy", Locale.ROOT);

    /**
     * Parses a date from Excel: numeric date cells, or strings yyyy-MM-dd / d/M/yyyy.
     */
    public static LocalDate getDate(Row row, int index) {
        Cell cell = row.getCell(index);
        if (cell == null) return null;
        CellType cellType = cell.getCellType();
        if (cellType == CellType.FORMULA) {
            cellType = cell.getCachedFormulaResultType();
        }
        if (cellType == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            java.util.Date d = cell.getDateCellValue();
            return Instant.ofEpochMilli(d.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
        }
        if (cellType == CellType.NUMERIC) {
            // Excel serial without date format
            return DateUtil.getLocalDateTime(cell.getNumericCellValue()).toLocalDate();
        }
        if (cellType == CellType.STRING) {
            String s = cell.getStringCellValue().trim();
            if (isBlank(s)) return null;
            try {
                return LocalDate.parse(s, ISO_DATE);
            } catch (DateTimeParseException ignored) {
            }
            try {
                return LocalDate.parse(s, DMY_SLASH);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    /**
     * Parses boolean from cell: true/false, 1/0, yes/no, có/không (case-insensitive).
     */
    public static Boolean getBoolean(Row row, int index) {
        Cell cell = row.getCell(index);
        if (cell == null) return null;
        CellType cellType = cell.getCellType();
        if (cellType == CellType.FORMULA) {
            cellType = cell.getCachedFormulaResultType();
        }
        if (cellType == CellType.BOOLEAN) {
            return cell.getBooleanCellValue();
        }
        if (cellType == CellType.NUMERIC) {
            double v = cell.getNumericCellValue();
            if (v == 1.0) return true;
            if (v == 0.0) return false;
            return null;
        }
        if (cellType == CellType.STRING) {
            String s = cell.getStringCellValue().trim().toLowerCase(Locale.ROOT);
            if (s.isEmpty()) return null;
            if ("true".equals(s) || "1".equals(s) || "yes".equals(s) || "có".equals(s) || "y".equals(s)) {
                return true;
            }
            if ("false".equals(s) || "0".equals(s) || "no".equals(s) || "không".equals(s) || "n".equals(s)) {
                return false;
            }
        }
        return null;
    }

    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
