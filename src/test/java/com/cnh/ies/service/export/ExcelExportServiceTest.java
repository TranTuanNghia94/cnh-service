package com.cnh.ies.service.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cnh.ies.entity.product.CategoryEntity;
import com.cnh.ies.entity.product.ProductEntity;
import com.cnh.ies.entity.warehouse.WarehouseInventoryEntity;
import com.cnh.ies.repository.customer.CustomerRepo;
import com.cnh.ies.repository.product.ProductRepo;
import com.cnh.ies.repository.vendors.VendorsRepo;
import com.cnh.ies.repository.warehouse.WarehouseInventoryRepo;

@ExtendWith(MockitoExtension.class)
class ExcelExportServiceTest {

    @Mock
    private ProductRepo productRepo;
    @Mock
    private VendorsRepo vendorsRepo;
    @Mock
    private CustomerRepo customerRepo;
    @Mock
    private WarehouseInventoryRepo warehouseInventoryRepo;

    @InjectMocks
    private ExcelExportService excelExportService;

    @Test
    void exportProducts_generatesWorkbookWithHeaderAndDataRow() throws Exception {
        CategoryEntity category = new CategoryEntity();
        category.setName("Electronics");

        ProductEntity product = new ProductEntity();
        product.setCode("P001");
        product.setName("Widget");
        product.setCategory(category);
        product.setUnit1("pcs");
        product.setPrice(new BigDecimal("10.5"));
        product.setTax(BigDecimal.ZERO);
        product.setIsActive(true);

        when(productRepo.findAllForExport()).thenReturn(List.of(product));

        ExcelExportService.ExportWorkbookResult result =
                excelExportService.export(ExportJobType.PRODUCTS, "rid");

        assertNotNull(result.content());
        assertTrue(result.fileName().startsWith("products_export_"));
        assertTrue(result.fileName().endsWith(".xlsx"));

        try (XSSFWorkbook workbook = new XSSFWorkbook(new java.io.ByteArrayInputStream(result.content()))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(0);
            assertEquals("Code", header.getCell(0).getStringCellValue());
            Row data = sheet.getRow(1);
            assertEquals("P001", data.getCell(0).getStringCellValue());
            assertEquals("Widget", data.getCell(1).getStringCellValue());
        }
    }

    @Test
    void exportWarehouseInventory_generatesWorkbookWithQuantity() throws Exception {
        ProductEntity product = new ProductEntity();
        product.setCode("WH-01");
        product.setName("Stock Item");
        product.setUnit1("box");

        WarehouseInventoryEntity inv = new WarehouseInventoryEntity();
        inv.setProduct(product);
        inv.setQuantityOnHand(new BigDecimal("42.5"));
        inv.setCreatedBy("admin");

        when(warehouseInventoryRepo.findAllForExport()).thenReturn(List.of(inv));

        ExcelExportService.ExportWorkbookResult result =
                excelExportService.export(ExportJobType.WAREHOUSE_INVENTORY, "rid");

        try (XSSFWorkbook workbook = new XSSFWorkbook(new java.io.ByteArrayInputStream(result.content()))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row data = sheet.getRow(1);
            assertEquals("WH-01", data.getCell(0).getStringCellValue());
            assertEquals("42.5", data.getCell(4).getStringCellValue());
        }
    }
}
