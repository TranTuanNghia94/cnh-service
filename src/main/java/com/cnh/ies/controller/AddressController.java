package com.cnh.ies.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.cnh.ies.dto.common.ApiResponse;
import com.cnh.ies.service.product.AddressService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/address")
@RequiredArgsConstructor
@Slf4j
public class AddressController {
    private final AddressService addressService;

    @PostMapping("/create")
    public ApiResponse<AddressInfo> createAddress(@RequestBody CreateAddressRequest request) {
        String requestId = UUID.randomUUID().toString();
        log.info("Creating address with initiated requestId: {}", requestId);

        AddressInfo response = addressService.createAddress(request, requestId);

        log.info("Creating address success with requestId: {}", requestId);

        return ApiResponse.success(response, "Create address success");
    }
}
