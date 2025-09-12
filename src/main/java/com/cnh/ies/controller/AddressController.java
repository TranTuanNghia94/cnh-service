package com.cnh.ies.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.cnh.ies.dto.common.ApiResponse;
import com.cnh.ies.model.customer.CreateAddressRequest;
import com.cnh.ies.model.customer.CustomerAddressInfo;
import com.cnh.ies.service.customer.CustomerAddressService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/address")
@RequiredArgsConstructor
@Slf4j
public class AddressController {
    private final CustomerAddressService addressService;

    @PostMapping("/create")
    public ApiResponse<CustomerAddressInfo> createAddress(@RequestBody CreateAddressRequest request) {
        String requestId = UUID.randomUUID().toString();
        log.info("Creating address with initiated requestId: {}", requestId);

        CustomerAddressInfo response = addressService.createAddress(request, requestId);

        log.info("Creating address success with requestId: {}", requestId);

        return ApiResponse.success(response, "Create address success");
    }


    @GetMapping("/list/{customerId}")
    public ApiResponse<List<CustomerAddressInfo>> getAllAddressesByCustomerId(@PathVariable String customerId) {
        String requestId = UUID.randomUUID().toString();
        log.info("Getting all addresses by customerId: {} with initiated requestId: {}", customerId, requestId);

        List<CustomerAddressInfo> response = addressService.getAddressByCustomerId(requestId, customerId);

        log.info("Getting all addresses by customerId: {} success with requestId: {}", customerId, requestId);

        return ApiResponse.success(response, "Get all addresses by customerId success");
    }

    @DeleteMapping("/delete/{id}")
    public ApiResponse<String> deleteAddress(@PathVariable String id) {
        String requestId = UUID.randomUUID().toString();
        log.info("Deleting address with initiated requestId: {}", requestId);

        String response = addressService.deleteAddress(id, requestId);

        log.info("Deleting address success with requestId: {}", requestId);

        return ApiResponse.success(response, "Delete address success");
    }

    // @PutMapping("/update")
    // public ApiResponse<String> updateAddress(@RequestBody UpdateAddressRequest request) {
    //     String requestId = UUID.randomUUID().toString();
    //     log.info("Updating address with initiated requestId: {}", requestId);

    //     String response = addressService.updateAddress(request, requestId);
    // }

    // @GetMapping("/{id}")
    // public ApiResponse<CustomerAddressInfo> getAddressById(@PathVariable String id) {
    //     String requestId = UUID.randomUUID().toString();
    //     log.info("Getting address by id: {} initiated requestId: {}", id, requestId);

    //     CustomerAddressInfo response = addressService.getAddressById(id, requestId);
    // }
}
