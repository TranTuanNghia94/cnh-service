package com.cnh.ies.service.report;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.cnh.ies.exception.ApiException;
import com.cnh.ies.model.report.InboundDetailReportRow;
import com.cnh.ies.model.report.PaymentInstallment;
import com.cnh.ies.model.report.InboundSummaryReportRow;
import com.cnh.ies.model.report.OperationalReportRequest;
import com.cnh.ies.model.report.OutboundDetailReportRow;
import com.cnh.ies.model.report.OutboundSummaryReportRow;
import com.cnh.ies.model.report.PaymentLedgerRow;
import com.cnh.ies.model.report.SalesDetailReportRow;
import com.cnh.ies.model.report.StockLedgerRow;
import com.cnh.ies.model.report.VendorDebtRow;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OperationalReportExcelService {

    private final OperationalReportService reportService;

    public byte[] stock(OperationalReportRequest request, String requestId) {
        return workbook("xuat-nhap-ton", new String[] {
                "Mã hàng", "Nhóm hàng", "Tên hàng", "Đơn vị tính", "Tồn đầu kỳ", "Nhập kho", "Xuất kho",
                "Tồn cuối kỳ", "Đơn giá"
        }, reportService.stockRows(request).stream().map(row -> List.<Object>of(
                row.getProductCode(), row.getCategoryName(), row.getProductName(), row.getUnit(),
                row.getBeginningQuantity(), row.getInboundQuantity(), row.getOutboundQuantity(),
                row.getEndingQuantity(), row.getUnitPrice()
        )).toList(), requestId);
    }

    public byte[] payments(OperationalReportRequest request, String requestId) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("thanh-toan");
            List<PaymentLedgerRow> rows = reportService.paymentRows(request);
            int installmentSlots = 1;
            for (PaymentLedgerRow row : rows) {
                int count = row.getInstallments() == null ? 0 : row.getInstallments().size();
                installmentSlots = Math.max(installmentSlots, count);
            }
            CellStyle blue = fill(workbook, new byte[] {(byte) 0x66, (byte) 0xCC, (byte) 0xFF});
            CellStyle orange = fill(workbook, new byte[] {(byte) 0xCC, (byte) 0x99, (byte) 0xFF});
            CellStyle green = fill(workbook, new byte[] {(byte) 0x99, (byte) 0xCC, (byte) 0x66});
            String[] singles = {
                    "ĐƠN VỊ", "NHÀ CUNG CẤP", "SỐ LẦN", "TỔNG TIỀN", "KHÁCH HÀNG", "TIỀN TỆ",
                    "LOẠI CHỨNG TỪ", "SỐ CHỨNG TỪ", "NOTE"
            };
            CellStyle[] colors = {blue, blue, blue, blue, blue, blue, orange, orange, orange};
            Row top = sheet.createRow(0);
            Row second = sheet.createRow(1);
            for (int i = 0; i < singles.length; i++) {
                sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 1, i, i));
                Cell cell = top.createCell(i);
                cell.setCellValue(singles[i]);
                cell.setCellStyle(colors[i]);
            }
            String[] sub = {"Số tiền", "Ngày thanh toán", "Phần trăm", "Trạng thái"};
            for (int group = 0; group < installmentSlots; group++) {
                int start = singles.length + group * sub.length;
                sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, start, start + sub.length - 1));
                Cell title = top.createCell(start);
                title.setCellValue("Đã Thanh toán Lần " + (group + 1));
                title.setCellStyle(green);
                for (int i = 0; i < sub.length; i++) {
                    Cell cell = second.createCell(start + i);
                    cell.setCellValue(sub[i]);
                    cell.setCellStyle(green);
                }
            }
            int rowIndex = 2;
            for (PaymentLedgerRow row : rows) {
                List<Object> values = new java.util.ArrayList<>();
                values.add(row.getCompany());
                values.add(row.getVendorCode());
                values.add(row.getPaymentCount());
                values.add(row.getTotalAmount());
                values.add(row.getCustomerName());
                values.add(row.getCurrency());
                values.add(row.getPaperType());
                values.add(row.getPaperNumber());
                values.add(row.getNote());
                List<PaymentInstallment> installments = row.getInstallments() == null
                        ? List.of()
                        : row.getInstallments();
                for (int group = 0; group < installmentSlots; group++) {
                    if (group < installments.size()) {
                        PaymentInstallment installment = installments.get(group);
                        values.add(installment.getAmount());
                        values.add(installment.getPaidDate());
                        values.add(installment.getPercentage());
                        values.add(installment.getStatus());
                    } else {
                        values.add("");
                        values.add("");
                        values.add("");
                        values.add("");
                    }
                }
                write(sheet.createRow(rowIndex++), values);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException exception) {
            throw excelError(requestId, exception);
        }
    }

    public byte[] vendorDebt(OperationalReportRequest request, String requestId) {
        return workbook("tong-hop-cong-no", new String[] {
                "Mã NCC", "Tên NCC", "Ngoại tệ", "Dư nợ đầu kỳ", "Giá trị hàng nhập", "Đã thanh toán", "Dư nợ cuối kỳ"
        }, reportService.vendorDebtRows(request).stream().map(row -> List.<Object>of(
                row.getVendorCode(), row.getVendorName(), row.getCurrency(), row.getOpeningDebt(),
                row.getGoodsValue(), row.getPaidAmount(), row.getClosingDebt()
        )).toList(), requestId);
    }

    public byte[] salesDetail(OperationalReportRequest request, String requestId) {
        return workbook("bao-cao-chi-tiet", new String[] {
                "STT", "CS", "Mã KH", "Số HĐ", "Ngày HĐ", "Mã hàng", "Tên hàng", "ĐVT", "SL", "Đơn giá",
                "Thành tiền", "Thuế suất", "Vendor tham khảo", "Ref", "Giáo viên", "Phòng ban", "Ghi chú",
                "Vendor CF", "Loại chứng từ thanh toán", "Số chứng từ thanh toán", "Số lượng", "Ngoại tệ",
                "Đơn giá", "Thành tiền", "Số Quote", "Số invoice", "Số bill", "Receipt warehouse",
                "Tracking number", "Ngày nhập kho", "Số phiếu nhập", "Số lượng nhập", "Đơn giá nhập",
                "Thành tiền", "Số invoice", "Số bill sổ sách", "Ghi chú"
        }, indexed(reportService.salesDetailRows(request).stream().map(this::salesValues).toList()), requestId);
    }

    public byte[] outboundSummary(OperationalReportRequest request, String requestId) {
        return workbook("xuat-kho", new String[] {
                "User", "Ngày thực hiện", "Số phiếu", "Số hợp đồng", "Khách hàng", "Thành tiền", "Tiền VAT", "Tổng tiền"
        }, reportService.outboundSummaryRows(request).stream().map(row -> List.<Object>of(
                row.getUserName(), row.getOutboundDate(), row.getOutboundNumber(), row.getContractNumber(),
                row.getCustomerName(), row.getAmountBeforeTax(), row.getVatAmount(), row.getTotalAmount()
        )).toList(), requestId);
    }

    public byte[] outboundDetail(OperationalReportRequest request, String requestId) {
        String[] headers = {
                "Hiển thị trên sổ", "Hình thức bán hàng", "Phương thức thanh toán", "Kiêm phiếu xuất kho",
                "Lập kèm hóa đơn", "Đã lập hóa đơn", "Ngày hạch toán (*)", "Ngày chứng từ (*)", "Số chứng từ (*)",
                "Số phiếu xuất", "Lý do xuất", "Mẫu số HĐ", "Ký hiệu HĐ", "Số hóa đơn", "Ngày hóa đơn",
                "Mã khách hàng", "Tên khách hàng", "Địa chỉ", "Mã số thuế", "Diễn giải", "Nộp vào TK",
                "NV bán hàng", "Loại tiền", "Tỷ giá", "Mã hàng (*)", "Tên hàng", "Hàng khuyến mại",
                "TK Tiền/Chi phí/Nợ (*)", "TK Doanh thu/Có (*)", "ĐVT", "Số lượng", "Đơn giá sau thuế",
                "Đơn giá", "Thành tiền", "Thành tiền quy đổi", "Tỷ lệ CK (%)", "Tiền chiết khấu",
                "Tiền chiết khấu quy đổi", "TK chiết khấu", "Giá tính thuế XK", "% thuế XK", "Tiền thuế XK",
                "TK thuế XK", "% thuế GTGT", "Tỷ lệ tính thuế (Thuế suất KHAC)", "Tiền thuế GTGT",
                "Tiền thuế GTGT quy đổi", "TK thuế GTGT", "HH không TH trên tờ khai thuế GTGT", "Kho",
                "TK giá vốn", "TK Kho", "ĐƠN VỊ", "Đơn giá vốn", "Tiền vốn", "Hàng hóa giữ hộ/bán hộ"
        };
        List<List<Object>> rows = reportService.outboundDetailRows(request).stream().map(this::outboundMisa).toList();
        return coloredWorkbook("xuat-kho-chi-tiet", headers, rows, 24, requestId);
    }

    public byte[] inboundSummary(OperationalReportRequest request, String requestId) {
        return workbook("nhap-kho", new String[] {
                "User", "Ngày thực hiện", "Số phiếu", "Số hợp đồng", "Vendor", "Loại tiền",
                "Giá trị (ngoại tệ)", "VAT", "Phí", "Tổng tiền (ngoại tệ)", "Tổng tiền (VND)"
        }, reportService.inboundSummaryRows(request).stream().map(row -> List.<Object>of(
                row.getUserName(), row.getReceivedDate(), row.getReceiptNumber(), row.getContractNumber(),
                row.getVendorName(), row.getCurrency(), row.getForeignValue(), row.getVatAmount(),
                row.getFeeAmount(), row.getForeignTotal(), row.getVndTotal()
        )).toList(), requestId);
    }

    public byte[] inboundDetail(OperationalReportRequest request, String requestId) {
        String[] headers = {
                "Hiển thị trên sổ", "Hình thức mua hàng", "Phương thức thanh toán", "Nhận kèm hóa đơn",
                "Ngày hạch toán (*)", "Ngày chứng từ (*)", "Số phiếu nhập (*)", "Số chứng từ thanh toán",
                "Mẫu số HĐ", "Ký hiệu HĐ", "Số hóa đơn", "Ngày hóa đơn", "Mã nhà cung cấp", "Tên nhà cung cấp",
                "Người giao hàng", "Diễn giải", "NV mua hàng", "Loại tiền", "Tỷ giá", "Mã hàng (*)", "Tên hàng",
                "Kho", "Hàng hóa giữ hộ/bán hộ", "TK kho (*)", "TK công nợ/TK tiền (*)", "ĐVT", "Số lượng",
                "Đơn giá", "Loại tiền", "Tỷ giá", "Thành tiền", "Thành tiền quy đổi", "Tỷ lệ CK", "Tiền chiết khấu",
                "Tiền chiết khấu quy đổi", "Phí hàng về kho/Chi phí mua hàng", "% thuế GTGT",
                "Tỷ lệ tính thuế (Thuế suất KHAC)", "Tiền thuế GTGT", "Tiền thuế GTGT quy đổi", "TKĐƯ thuế GTGT",
                "TK thuế GTGT", "Nhóm HHDV mua vào", "Phí trước hải quan", "Giá tính thuế NK", "% thuế NK",
                "Tiền thuế NK", "TK thuế NK", "% thuế TTĐB", "Tiền thuế TTĐB", "TK thuế TTĐB", "Thuế suất",
                "Bill thực tế", "Bill sổ sách"
        };
        List<List<Object>> rows = reportService.inboundDetailRows(request).stream().map(this::inboundMisa).toList();
        return coloredWorkbook("nhap-kho-chi-tiet", headers, rows, 19, requestId);
    }

    private List<Object> salesValues(SalesDetailReportRow row) {
        return List.of(
                row.getCsName(), row.getCustomerCode(), row.getContractNumber(), row.getContractDate(),
                row.getProductCode(), row.getProductName(), row.getUnit(), row.getSalesQuantity(),
                row.getSalesUnitPrice(), row.getSalesAmount(), row.getTaxRate(), row.getSuggestedVendorCode(),
                row.getReference(), row.getTeacher(), row.getDepartment(), row.getNote(),
                row.getConfirmedVendorCode(), row.getPaymentPaperType(), row.getPaymentPaperNumber(),
                row.getPurchaseQuantity(), row.getCurrency(), row.getPurchaseUnitPrice(), row.getPurchaseAmount(),
                row.getQuoteNumber(), row.getInvoiceNumber(), row.getBillNumber(), row.getReceiptWarehouse(),
                row.getTrackingNumber(), row.getInboundDate(), row.getInboundNumber(), row.getInboundQuantity(),
                row.getInboundUnitPrice(), row.getInboundAmount(), row.getInboundInvoiceNumber(),
                row.getBookBillNumber(), row.getInboundNote());
    }

    private List<Object> outboundMisa(OutboundDetailReportRow row) {
        List<Object> values = new java.util.ArrayList<>();
        for (int i = 0; i < 56; i++) {
            values.add("");
        }
        values.set(6, row.getPostingDate());
        values.set(7, row.getDocumentDate());
        values.set(8, row.getDocumentNumber());
        values.set(9, row.getOutboundNumber());
        values.set(10, row.getReason());
        values.set(13, row.getInvoiceNumber());
        values.set(14, row.getInvoiceDate());
        values.set(15, row.getCustomerCode());
        values.set(16, row.getCustomerName());
        values.set(19, row.getNarrative());
        values.set(24, row.getProductCode());
        values.set(25, row.getProductName());
        values.set(27, row.getDebitAccount());
        values.set(28, row.getCreditAccount());
        values.set(30, row.getQuantity());
        values.set(32, row.getUnitPrice());
        values.set(33, row.getAmount());
        values.set(43, row.getVatRate());
        values.set(45, row.getVatAmount());
        values.set(47, row.getVatAccount());
        values.set(49, row.getWarehouseCode());
        values.set(50, row.getCogsAccount());
        values.set(51, row.getInventoryAccount());
        values.set(52, row.getBusinessUnit());
        return values;
    }

    private List<Object> inboundMisa(InboundDetailReportRow row) {
        List<Object> values = new java.util.ArrayList<>();
        for (int i = 0; i < 54; i++) {
            values.add("");
        }
        values.set(1, OperationalReportFormulas.PURCHASE_FORM);
        values.set(4, row.getDocumentDate());
        values.set(5, row.getDocumentDate());
        values.set(6, row.getReceiptNumber());
        values.set(10, row.getInvoiceNumber());
        values.set(12, row.getVendorCode());
        values.set(19, row.getProductCode());
        values.set(20, row.getProductName());
        values.set(21, row.getWarehouseCode());
        values.set(23, row.getInventoryAccount());
        values.set(24, row.getPayableAccount());
        values.set(26, row.getQuantity());
        values.set(27, row.getUnitPrice());
        values.set(28, row.getCurrency());
        values.set(29, row.getExchangeRate());
        values.set(30, row.getAmount());
        values.set(31, row.getConvertedAmount());
        values.set(36, row.getVatRate());
        values.set(38, row.getVatAmount());
        values.set(39, row.getConvertedVatAmount());
        values.set(40, row.getInputVatAccount());
        values.set(41, row.getVatAccount());
        values.set(51, row.getVatRate());
        values.set(52, row.getBillOnPaper());
        values.set(53, row.getLineNote());
        return values;
    }

    private List<List<Object>> indexed(List<List<Object>> rows) {
        List<List<Object>> indexed = new java.util.ArrayList<>();
        int index = 1;
        for (List<Object> row : rows) {
            List<Object> withIndex = new java.util.ArrayList<>();
            withIndex.add(index++);
            withIndex.addAll(row);
            indexed.add(withIndex);
        }
        return indexed;
    }

    private byte[] workbook(String name, String[] headers, List<List<Object>> rows, String requestId) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(name);
            write(sheet.createRow(0), java.util.Arrays.asList(headers));
            int rowIndex = 1;
            for (List<Object> row : rows) {
                write(sheet.createRow(rowIndex++), row);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException exception) {
            throw excelError(requestId, exception);
        }
    }

    private byte[] coloredWorkbook(String name, String[] headers, List<List<Object>> rows, int yellowFrom, String requestId) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(name);
            CellStyle purple = fill(workbook, new byte[] {(byte) 0xB1, (byte) 0xCC, (byte) 0xFF});
            CellStyle yellow = fill(workbook, new byte[] {(byte) 0xFF, (byte) 0xFF, (byte) 0x00});
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(i < yellowFrom ? purple : yellow);
            }
            int rowIndex = 1;
            for (List<Object> row : rows) {
                write(sheet.createRow(rowIndex++), row);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException exception) {
            throw excelError(requestId, exception);
        }
    }

    private static void write(Row row, List<Object> values) {
        for (int i = 0; i < values.size(); i++) {
            Object value = values.get(i);
            Cell cell = row.createCell(i);
            if (value == null) {
                cell.setBlank();
            } else if (value instanceof BigDecimal decimal) {
                cell.setCellValue(decimal.doubleValue());
            } else if (value instanceof Number number) {
                cell.setCellValue(number.doubleValue());
            } else {
                cell.setCellValue(value.toString());
            }
        }
    }

    private static CellStyle fill(XSSFWorkbook workbook, byte[] rgb) {
        XSSFCellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(new XSSFColor(rgb, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private static ApiException excelError(String requestId, IOException exception) {
        return new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, exception.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
    }
}
