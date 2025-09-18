package com.cnh.ies.controller;

import java.util.UUID;

import com.cnh.ies.model.general.ListDataModel;
import com.cnh.ies.model.general.ApiRequestModel;
import com.cnh.ies.service.vendor.VendorService;
import com.cnh.ies.model.vendors.VendorInfo;
import com.cnh.ies.model.vendors.CreateVendorRequest;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/vendor")
public class VendorController {
    private final VendorService vendorService;

    @PostMapping("/list")
    public ListDataModel<VendorInfo> getAllVendors(@RequestBody ApiRequestModel request) {
        String requestId = UUID.randomUUID().toString();
        log.info("Getting all vendors with initiated requestId: {}", requestId);

        ListDataModel<VendorInfo> response = vendorService.getAllVendors(requestId, request.getPage(), request.getLimit());

        log.info("Getting all vendors success with requestId: {}", requestId);

        return response;
    }

    @PostMapping("/create")
    public VendorInfo createVendor(@RequestBody CreateVendorRequest request) {
        String requestId = UUID.randomUUID().toString();
        log.info("Creating vendor with initiated requestId: {}", requestId);

        VendorInfo response = vendorService.createVendor(request, requestId);

        log.info("Creating vendor success with requestId: {}", requestId);

        return response;
    }



    @DeleteMapping("/delete/{id}")
    public String deleteVendor(@PathVariable String id) {
        String requestId = UUID.randomUUID().toString();
        log.info("Deleting vendor with initiated requestId: {}", requestId);

        String response = vendorService.deleteVendor(id, requestId);

        log.info("Deleting vendor success with requestId: {}", requestId);

        return response;
    }

    @GetMapping("/{id}")
    public VendorInfo getVendorById(@PathVariable String id) {
        String requestId = UUID.randomUUID().toString();
        log.info("Getting vendor by id: {} initiated requestId: {}", id, requestId);

        VendorInfo response = vendorService.getVendorById(id, requestId);

        log.info("Getting vendor by id: {} success requestId: {}", id, requestId);

        return response;
    }
}
