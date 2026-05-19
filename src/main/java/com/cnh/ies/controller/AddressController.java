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
import com.cnh.ies.util.RequestContext;
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
        String requestId = RequestContext.getRequestIdOrGenerate();
CustomerAddressInfo response = addressService.createAddress(request, requestId);
return ApiResponse.success(response, "Create address success");
    }


    @GetMapping("/list/{customerId}")
    public ApiResponse<List<CustomerAddressInfo>> getAllAddressesByCustomerId(@PathVariable String customerId) {
        String requestId = RequestContext.getRequestIdOrGenerate();
List<CustomerAddressInfo> response = addressService.getAddressByCustomerId(requestId, customerId);
return ApiResponse.success(response, "Get all addresses by customerId success");
    }

    @DeleteMapping("/delete/{id}")
    public ApiResponse<String> deleteAddress(@PathVariable String id) {
        String requestId = RequestContext.getRequestIdOrGenerate();
String response = addressService.deleteAddress(id, requestId);
return ApiResponse.success(response, "Delete address success");
    }

    // @PutMapping("/update")
    // public ApiResponse<String> updateAddress(@RequestBody UpdateAddressRequest request) {
    //     String requestId = RequestContext.getRequestIdOrGenerate();
    //     log.info("Updating address with initiated requestId: {}", requestId);

    //     String response = addressService.updateAddress(request, requestId);
    // }

    // @GetMapping("/{id}")
    // public ApiResponse<CustomerAddressInfo> getAddressById(@PathVariable String id) {
    //     String requestId = RequestContext.getRequestIdOrGenerate();
    //     log.info("Getting address by id: {} initiated requestId: {}", id, requestId);

    //     CustomerAddressInfo response = addressService.getAddressById(id, requestId);
    // }
}
