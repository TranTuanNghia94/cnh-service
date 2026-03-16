package com.cnh.ies.service.customer;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.List;
import com.cnh.ies.dto.response.UploadOjectResponse;
import com.cnh.ies.entity.customer.CustomerAddressEntity;
import com.cnh.ies.entity.customer.CustomerEntity;
import com.cnh.ies.repository.customer.CustomerRepo;
import com.cnh.ies.repository.customer.CustomerAddressRepo;
import com.cnh.ies.util.ExcelUtils.TemplateType;
import com.cnh.ies.util.RequestContext;
import com.cnh.ies.exception.ApiException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import static com.cnh.ies.util.ExcelUtils.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class UploadCustomerService {
    private final CustomerRepo customerRepo;
    private final CustomerAddressRepo customerAddressRepo;

    public UploadOjectResponse readExcelFile(MultipartFile file, String requestId) {
        log.info("Reading excel file, requestId: {}", requestId);
        List<String> errors = new ArrayList<>();
        int importedCount = 0;

        try (var workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            String headerError = validateHeaders(sheet, TemplateType.CUSTOMER);
            if (headerError != null) {
                throw new ApiException(ApiException.ErrorCode.INVALID_REQUEST,
                        "Invalid template: " + headerError, HttpStatus.BAD_REQUEST.value(), requestId);
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null)
                    continue;

                String error = processRow(row, i);
                if (error != null)
                    errors.add(error);
                else
                    importedCount++;
            }
            log.info("Import completed: {} success, {} errors", importedCount, errors.size());
            return new UploadOjectResponse("Import completed successfully", sheet.getLastRowNum(), importedCount,
                    errors.size(), errors);
        } catch (Exception e) {
            log.error("Error reading excel file: {}", e.getMessage(), e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR,
                    "Error reading excel file: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }

    // EMAIL, SDT, NGƯỜI LIÊN HỆ, ĐỊA CHỈ
    private String processRow(Row row, int rowNum) {
        String code = getString(row, 0);
        String name = getString(row, 1);
        String misaCode = getString(row, 2);

        if (isBlank(code))
            return "Row " + rowNum + ": Customer code is required";
        if (isBlank(name))
            return "Row " + rowNum + ": Customer name is required";
        if (customerRepo.findByCode(code).isPresent())
            return "Row " + rowNum + ": Customer with code '" + code + "' already exists";

        String username = RequestContext.getCurrentUsername();

        CustomerEntity customer = new CustomerEntity();
        customer.setCode(code);
        customer.setName(name);
        customer.setMisaCode(misaCode);
        customer.setCreatedBy(username);
        customer.setUpdatedBy(username);
        customerRepo.save(customer);

        if (getString(row, 4) != null || getString(row, 5) != null || getString(row, 6) != null) {
            CustomerAddressEntity customerAddress = new CustomerAddressEntity();
            customerAddress.setCustomer(customer);
            customerAddress.setAddress(getString(row, 6));
            customerAddress.setContactPerson(getString(row, 5));
            customerAddress.setPhone(getString(row, 4));
            customerAddress.setEmail(getString(row, 7));
            customerAddress.setCreatedBy(username);
            customerAddress.setUpdatedBy(username);
            customerAddressRepo.save(customerAddress);
        }

        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
