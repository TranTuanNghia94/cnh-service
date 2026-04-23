package com.cnh.ies.controller;

import java.util.List;

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
import com.cnh.ies.model.payment.SendToAccountantRequest;
import com.cnh.ies.service.file.FileService;
import com.cnh.ies.service.payment.PaymentRequestService;
import com.cnh.ies.util.RequestContext;

import lombok.RequiredArgsConstructor;

/**
 * Payment Request REST Controller.
 * 
 * Note: Request/response logging is handled automatically by RequestResponseLoggingFilter.
 * Use RequestContext.getRequestId() to access the request ID for correlation in services.
 */
@RestController
@RequestMapping("/payment-request")
@RequiredArgsConstructor
public class PaymentRequestController {

    private final PaymentRequestService paymentRequestService;
    private final FileService fileService;

    @PostMapping("/create-or-update")
    public ApiResponse<PaymentRequestInfo> createOrUpdate(@RequestBody CreateOrUpdatePaymentRequest request) {
        PaymentRequestInfo response = paymentRequestService.createOrUpdate(request, RequestContext.getRequestId());
        return ApiResponse.success(response, "Create/update payment request success");
    }

    @PostMapping("/list")
    public ApiResponse<ListDataModel<PaymentRequestInfo>> list(@RequestBody ApiRequestModel request) {
        ListDataModel<PaymentRequestInfo> response = paymentRequestService.getAllPaymentRequests(
            RequestContext.getRequestId(), request.getPage(), request.getLimit());
        return ApiResponse.success(response, "Get payment request list success");
    }

    @GetMapping("/{id}")
    public ApiResponse<PaymentRequestInfo> getById(@PathVariable String id) {
        PaymentRequestInfo response = paymentRequestService.getById(id, RequestContext.getRequestId());
        return ApiResponse.success(response, "Get payment request success");
    }

    @GetMapping("/{id}/uploaded-files")
    public ApiResponse<List<PaymentFileUploadInfo>> listUploadedFiles(@PathVariable String id) {
        List<PaymentFileUploadInfo> response = fileService.listUploadedFilesForPaymentRequest(id, RequestContext.getRequestId());
        return ApiResponse.success(response, "List uploaded files for payment request success");
    }

    @PostMapping("/submit/{id}")
    public ApiResponse<PaymentRequestInfo> submit(@PathVariable String id) {
        PaymentRequestInfo response = paymentRequestService.submit(id, RequestContext.getRequestId());
        return ApiResponse.success(response, "Submit payment request success");
    }

    @PostMapping("/{id}/send-to-accountant")
    public ApiResponse<PaymentRequestInfo> sendToAccountant(
            @PathVariable String id,
            @RequestBody(required = false) SendToAccountantRequest request) {
        PaymentRequestInfo response = paymentRequestService.sendToAccountant(id, request, RequestContext.getRequestId());
        return ApiResponse.success(response, "Payment request sent to accountant successfully");
    }

    @PostMapping("/cancel/{id}")
    public ApiResponse<PaymentRequestInfo> cancel(@PathVariable String id) {
        PaymentRequestInfo response = paymentRequestService.cancel(id, RequestContext.getRequestId());
        return ApiResponse.success(response, "Cancel payment request success");
    }

    @PostMapping("/approve/{id}")
    public ApiResponse<PaymentRequestInfo> approve(@PathVariable String id, @RequestBody ApprovePaymentRequest request) {
        PaymentRequestInfo response = paymentRequestService.approve(id, request, RequestContext.getRequestId());
        return ApiResponse.success(response, "Approve payment request success");
    }

    @PostMapping("/reject/{id}")
    public ApiResponse<PaymentRequestInfo> reject(@PathVariable String id, @RequestBody RejectPaymentRequest request) {
        PaymentRequestInfo response = paymentRequestService.reject(id, request, RequestContext.getRequestId());
        return ApiResponse.success(response, "Reject payment request success");
    }

    @PostMapping("/mark-paid/{id}")
    public ApiResponse<PaymentRequestInfo> markPaid(@PathVariable String id, @RequestBody MarkPaymentPaidRequest request) {
        PaymentRequestInfo response = paymentRequestService.markPaid(id, request, RequestContext.getRequestId());
        return ApiResponse.success(response, "Mark payment as paid success");
    }

    @PostMapping("/upload-file")
    public ApiResponse<PaymentFileUploadInfo> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "paymentRequestId", required = false) String paymentRequestId,
            @RequestParam(name = "attachmentType", required = false) String attachmentType) {
        PaymentFileUploadInfo response = fileService.uploadFile(file, category, paymentRequestId, attachmentType, RequestContext.getRequestId());
        return ApiResponse.success(response, "Upload payment file success");
    }
}
