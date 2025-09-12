package com.cnh.ies.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cnh.ies.dto.common.ApiResponse;
import com.cnh.ies.model.customer.CreateCustomerRequest;
import com.cnh.ies.model.customer.CustomerInfo;
import com.cnh.ies.model.customer.UpdateCustomerRequest;
import com.cnh.ies.model.general.ApiRequestModel;
import com.cnh.ies.model.general.ListDataModel;
import com.cnh.ies.service.customer.CustomerService;

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

    @PostMapping("/list")
    public ApiResponse<ListDataModel<CustomerInfo>> getAllCustomers(@RequestBody ApiRequestModel request) {
        String requestId = UUID.randomUUID().toString();
        log.info("Getting all customers with initiated requestId: {}", requestId);

        ListDataModel<CustomerInfo> response = customerService.getAllCustomers(requestId, request.getPage(), request.getLimit());

        log.info("Getting all customers success with requestId: {}", requestId);

        return ApiResponse.success(response, "Get all customers success");
    }

    @PostMapping("/create")
    public ApiResponse<CustomerInfo> createCustomer(@RequestBody CreateCustomerRequest request) {
        String requestId = UUID.randomUUID().toString();
        log.info("Creating customer with initiated requestId: {}", requestId);

        CustomerInfo response = customerService.createCustomer(request, requestId);

        log.info("Creating customer success with requestId: {}", requestId);

        return ApiResponse.success(response, "Create customer success");
    }

    @PutMapping("/update")
    public ApiResponse<String> updateCustomer(@RequestBody UpdateCustomerRequest request) {
        String requestId = UUID.randomUUID().toString();
        log.info("Updating customer with initiated requestId: {}", requestId);

        String response = customerService.updateCustomer(request, requestId);

        log.info("Updating customer success with requestId: {}", requestId);

        return ApiResponse.success(response, "Update customer success");
    }

    @GetMapping("/{id}")
    public ApiResponse<CustomerInfo> getCustomerById(@PathVariable String id) {
        String requestId = UUID.randomUUID().toString();
        log.info("Getting customer by id: {} initiated requestId: {}", id, requestId);

        CustomerInfo response = customerService.getCustomerById(id, requestId);

        log.info("Getting customer by id: {} success requestId: {}", id, requestId);

        return ApiResponse.success(response, "Get customer by id success");
    }

    @PostMapping("/delete/{id}")
    public ApiResponse<String> deleteCustomer(@PathVariable String id) {
        String requestId = UUID.randomUUID().toString();
        log.info("Deleting customer with initiated requestId: {}", requestId);

        String response = customerService.deleteCustomer(id, requestId);

        log.info("Deleting customer success with requestId: {}", requestId);

        return ApiResponse.success(response, "Delete customer success");
    }
}
