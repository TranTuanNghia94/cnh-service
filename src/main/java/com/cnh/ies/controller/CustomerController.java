package com.cnh.ies.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.cnh.ies.dto.common.ApiResponse;
import com.cnh.ies.util.RequestContext;
import com.cnh.ies.model.customer.CreateCustomerRequest;
import com.cnh.ies.model.customer.CustomerInfo;
import com.cnh.ies.model.customer.CustomerListRequest;
import com.cnh.ies.model.customer.UpdateCustomerRequest;
import com.cnh.ies.model.general.ListDataModel;
import com.cnh.ies.service.customer.CustomerService;
import com.cnh.ies.service.customer.UploadCustomerService;
import com.cnh.ies.dto.response.UploadOjectResponse;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/customer")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Customer", description = "Customer management APIs")
public class CustomerController {
    private final CustomerService customerService;
    private final UploadCustomerService uploadCustomerService;

    @PostMapping("/list")
    public ApiResponse<ListDataModel<CustomerInfo>> getAllCustomers(@RequestBody CustomerListRequest request) {
        String requestId = RequestContext.getRequestIdOrGenerate();
ListDataModel<CustomerInfo> response = customerService.getAllCustomers(
                requestId,
                request.getPage(),
                request.getLimit(),
                request.getCustomerCode(),
                request.getMisaCode(),
                request.getCustomerName());
return ApiResponse.success(response, "Get all customers success");
    }

    @PostMapping("/create")
    public ApiResponse<CustomerInfo> createCustomer(@RequestBody CreateCustomerRequest request) {
        String requestId = RequestContext.getRequestIdOrGenerate();
CustomerInfo response = customerService.createCustomer(request, requestId);
return ApiResponse.success(response, "Create customer success");
    }

    @PutMapping("/update")
    public ApiResponse<String> updateCustomer(@RequestBody UpdateCustomerRequest request) {
        String requestId = RequestContext.getRequestIdOrGenerate();
String response = customerService.updateCustomer(request, requestId);
return ApiResponse.success(response, "Update customer success");
    }

    @GetMapping("/{id}")
    public ApiResponse<CustomerInfo> getCustomerById(@PathVariable String id) {
        String requestId = RequestContext.getRequestIdOrGenerate();
CustomerInfo response = customerService.getCustomerById(id, requestId);

        log.info("Getting customer by id: {} success requestId: {}", id, requestId);

        return ApiResponse.success(response, "Get customer by id success");
    }

    @PostMapping("/delete/{id}")
    public ApiResponse<String> deleteCustomer(@PathVariable String id) {
        String requestId = RequestContext.getRequestIdOrGenerate();
String response = customerService.deleteCustomer(id, requestId);
return ApiResponse.success(response, "Delete customer success");
    }

    @PostMapping("/upload-file-customer")
    public ApiResponse<UploadOjectResponse> uploadFileCustomer(@RequestParam("file") MultipartFile file) {
        String requestId = RequestContext.getRequestIdOrGenerate();
UploadOjectResponse response = uploadCustomerService.readExcelFile(file, requestId);
return ApiResponse.success(response, "Upload file customer success");
    }
}
