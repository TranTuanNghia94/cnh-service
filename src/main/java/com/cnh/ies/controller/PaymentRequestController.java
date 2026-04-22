package com.cnh.ies.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.cnh.ies.dto.common.ApiResponse;
import com.cnh.ies.model.general.ApiRequestModel;
import com.cnh.ies.model.general.ListDataModel;
import com.cnh.ies.model.payment.ApprovePaymentRequest;
import com.cnh.ies.model.payment.CreateOrUpdatePaymentRequest;
import com.cnh.ies.model.payment.MarkPaymentPaidRequest;
import com.cnh.ies.model.payment.PaymentFileUploadInfo;
import com.cnh.ies.model.payment.PaymentRequestInfo;
import com.cnh.ies.model.payment.RejectPaymentRequest;
import com.cnh.ies.service.payment.PaymentFileStorageService;
import com.cnh.ies.service.payment.PaymentRequestService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/payment-request")
@RequiredArgsConstructor
@Slf4j
public class PaymentRequestController {

    private final PaymentRequestService paymentRequestService;
    private final PaymentFileStorageService paymentFileStorageService;

    @PostMapping("/create-or-update")
    public ApiResponse<PaymentRequestInfo> createOrUpdate(@RequestBody CreateOrUpdatePaymentRequest request) {
        String requestId = UUID.randomUUID().toString();
        PaymentRequestInfo response = paymentRequestService.createOrUpdate(request, requestId);
        return ApiResponse.success(response, "Create/update payment request success");
    }

    @PostMapping("/list")
    public ApiResponse<ListDataModel<PaymentRequestInfo>> list(@RequestBody ApiRequestModel request) {
        String requestId = UUID.randomUUID().toString();
        ListDataModel<PaymentRequestInfo> response = paymentRequestService.getAllPaymentRequests(requestId, request.getPage(),
                request.getLimit());
        return ApiResponse.success(response, "Get payment request list success");
    }

    @GetMapping("/{id}")
    public ApiResponse<PaymentRequestInfo> getById(@PathVariable String id) {
        String requestId = UUID.randomUUID().toString();
        PaymentRequestInfo response = paymentRequestService.getById(id, requestId);
        return ApiResponse.success(response, "Get payment request success");
    }

    @PostMapping("/submit/{id}")
    public ApiResponse<PaymentRequestInfo> submit(@PathVariable String id) {
        String requestId = UUID.randomUUID().toString();
        PaymentRequestInfo response = paymentRequestService.submit(id, requestId);
        return ApiResponse.success(response, "Submit payment request success");
    }

    @PostMapping("/cancel/{id}")
    public ApiResponse<PaymentRequestInfo> cancel(@PathVariable String id) {
        String requestId = UUID.randomUUID().toString();
        PaymentRequestInfo response = paymentRequestService.cancel(id, requestId);
        return ApiResponse.success(response, "Cancel payment request success");
    }

    @PostMapping("/approve/{id}")
    public ApiResponse<PaymentRequestInfo> approve(@PathVariable String id, @RequestBody ApprovePaymentRequest request) {
        String requestId = UUID.randomUUID().toString();
        PaymentRequestInfo response = paymentRequestService.approve(id, request, requestId);
        return ApiResponse.success(response, "Approve payment request success");
    }

    @PostMapping("/reject/{id}")
    public ApiResponse<PaymentRequestInfo> reject(@PathVariable String id, @RequestBody RejectPaymentRequest request) {
        String requestId = UUID.randomUUID().toString();
        PaymentRequestInfo response = paymentRequestService.reject(id, request, requestId);
        return ApiResponse.success(response, "Reject payment request success");
    }

    @PostMapping("/mark-paid/{id}")
    public ApiResponse<PaymentRequestInfo> markPaid(@PathVariable String id, @RequestBody MarkPaymentPaidRequest request) {
        String requestId = UUID.randomUUID().toString();
        PaymentRequestInfo response = paymentRequestService.markPaid(id, request, requestId);
        return ApiResponse.success(response, "Mark payment as paid success");
    }

    @PostMapping("/upload-file")
    public ApiResponse<PaymentFileUploadInfo> uploadFile(@RequestParam("file") MultipartFile file,
            @RequestParam(name = "category", required = false) String category) {
        String requestId = UUID.randomUUID().toString();
        PaymentFileUploadInfo response = paymentFileStorageService.uploadPaymentFile(file, category, requestId);
        return ApiResponse.success(response, "Upload payment file success");
    }
}
