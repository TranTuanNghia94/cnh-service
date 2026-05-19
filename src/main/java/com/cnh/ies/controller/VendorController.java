package com.cnh.ies.controller;

import java.util.UUID;

import com.cnh.ies.model.general.ListDataModel;
import com.cnh.ies.dto.common.ApiResponse;
import com.cnh.ies.util.RequestContext;
import com.cnh.ies.dto.response.UploadOjectResponse;
import com.cnh.ies.service.vendor.UploadVendorService;
import com.cnh.ies.service.vendor.VendorService;
import com.cnh.ies.model.vendors.VendorInfo;
import com.cnh.ies.model.vendors.CreateVendorRequest;
import com.cnh.ies.model.vendors.VendorListRequest;
import com.cnh.ies.model.vendors.UpdateVendorRequest;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/vendor")
public class VendorController {
    private final VendorService vendorService;
    private final UploadVendorService uploadVendorService;
    
    @PostMapping("/list")
    public ApiResponse<ListDataModel<VendorInfo>> getAllVendors(@RequestBody VendorListRequest request) {
        String requestId = RequestContext.getRequestIdOrGenerate();
ListDataModel<VendorInfo> response = vendorService.getAllVendors(
                requestId,
                request.getPage(),
                request.getLimit(),
                request.getVendorCode(),
                request.getVendorName(),
                request.getMisaCode(),
                request.getCurrency(),
                request.getNation());
return ApiResponse.success(response, "Get all vendors success");
    }

    @PostMapping("/create")
    public ApiResponse<VendorInfo> createVendor(@RequestBody CreateVendorRequest request) {
        String requestId = RequestContext.getRequestIdOrGenerate();
VendorInfo response = vendorService.createVendor(request, requestId);
return ApiResponse.success(response, "Create vendor success");
    }

    @PutMapping("/update")
    public ApiResponse<String> updateVendor(@RequestBody UpdateVendorRequest request) {
        String requestId = RequestContext.getRequestIdOrGenerate();
String response = vendorService.updateVendor(request, requestId);
return ApiResponse.success(response, "Update vendor success");
    }

    @DeleteMapping("/delete/{id}")
    public ApiResponse<String> deleteVendor(@PathVariable String id) {
        String requestId = RequestContext.getRequestIdOrGenerate();
String response = vendorService.deleteVendor(id, requestId);
return ApiResponse.success(response, "Delete vendor success");
    }

    @GetMapping("/{id}")
    public ApiResponse<VendorInfo> getVendorById(@PathVariable String id) {
        String requestId = RequestContext.getRequestIdOrGenerate();
VendorInfo response = vendorService.getVendorById(id, requestId);

        log.info("Getting vendor by id: {} success requestId: {}", id, requestId);

        return ApiResponse.success(response, "Get vendor by id success");
    }

    @PostMapping("/upload-file-vendor")
    public ApiResponse<UploadOjectResponse> uploadFileVendor(@RequestParam("file") MultipartFile file) {
        String requestId = RequestContext.getRequestIdOrGenerate();
UploadOjectResponse response = uploadVendorService.readExcelFile(file, requestId);
return ApiResponse.success(response, "Upload file vendor success");
    }
}
