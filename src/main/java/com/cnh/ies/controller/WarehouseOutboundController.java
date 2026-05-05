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
import com.cnh.ies.model.payment.PaymentFileUploadInfo;
import com.cnh.ies.model.payment.RejectPaymentRequest;
import com.cnh.ies.model.warehouse.WarehouseOutboundCreateRequest;
import com.cnh.ies.model.warehouse.WarehouseOutboundActionsInfo;
import com.cnh.ies.model.warehouse.WarehouseOutboundInfo;
import com.cnh.ies.model.warehouse.WarehouseOutboundOrderSearchInfo;
import com.cnh.ies.service.file.FileService;
import com.cnh.ies.service.warehouse.WarehouseOutboundService;
import com.cnh.ies.util.RequestContext;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/warehouse-outbound")
@RequiredArgsConstructor
public class WarehouseOutboundController {

    private final WarehouseOutboundService warehouseOutboundService;
    private final FileService fileService;

    @GetMapping("/order-lines")
    public ApiResponse<WarehouseOutboundOrderSearchInfo> getOrderLinesByContract(
            @RequestParam("contractNumber") String contractNumber) {
        WarehouseOutboundOrderSearchInfo response = warehouseOutboundService.getOrderLinesByContractNumber(
                contractNumber, RequestContext.getRequestId());
        return ApiResponse.success(response, "Get order lines by contract number success");
    }

    @PostMapping("/create")
    public ApiResponse<WarehouseOutboundInfo> create(@RequestBody WarehouseOutboundCreateRequest request) {
        WarehouseOutboundInfo response = warehouseOutboundService.createOutboundByContract(
                request, RequestContext.getRequestId());
        return ApiResponse.success(response, "Create warehouse outbound success");
    }

    @PostMapping("/list")
    public ApiResponse<ListDataModel<WarehouseOutboundInfo>> list(
            @RequestBody ApiRequestModel request,
            @RequestParam(name = "status", required = false) String status) {
        ListDataModel<WarehouseOutboundInfo> response = warehouseOutboundService.list(
                RequestContext.getRequestId(), request.getPage(), request.getLimit(), status, request.getSearch());
        return ApiResponse.success(response, "Get warehouse outbound list success");
    }

    @GetMapping("/{outboundId}")
    public ApiResponse<WarehouseOutboundInfo> getOutbound(@PathVariable String outboundId) {
        WarehouseOutboundInfo response = warehouseOutboundService.getOutbound(outboundId, RequestContext.getRequestId());
        return ApiResponse.success(response, "Get warehouse outbound success");
    }

    @GetMapping("/{outboundId}/actions")
    public ApiResponse<WarehouseOutboundActionsInfo> getAllowedActions(@PathVariable String outboundId) {
        WarehouseOutboundActionsInfo response = warehouseOutboundService.getAllowedActions(
                outboundId, RequestContext.getRequestId());
        return ApiResponse.success(response, "Get warehouse outbound allowed actions success");
    }

    @PostMapping("/{outboundId}/submit")
    public ApiResponse<WarehouseOutboundInfo> submit(@PathVariable String outboundId) {
        WarehouseOutboundInfo response = warehouseOutboundService.submitForApproval(outboundId, RequestContext.getRequestId());
        return ApiResponse.success(response, "Submit warehouse outbound for approval success");
    }

    @PostMapping("/{outboundId}/approve")
    public ApiResponse<WarehouseOutboundInfo> approve(
            @PathVariable String outboundId,
            @RequestBody(required = false) ApprovePaymentRequest request) {
        WarehouseOutboundInfo response = warehouseOutboundService.approve(outboundId, request, RequestContext.getRequestId());
        return ApiResponse.success(response, "Approve warehouse outbound success");
    }

    @PostMapping("/{outboundId}/reject")
    public ApiResponse<WarehouseOutboundInfo> reject(
            @PathVariable String outboundId,
            @RequestBody RejectPaymentRequest request) {
        WarehouseOutboundInfo response = warehouseOutboundService.reject(outboundId, request, RequestContext.getRequestId());
        return ApiResponse.success(response, "Reject warehouse outbound success");
    }

    @PostMapping("/{outboundId}/cancel")
    public ApiResponse<WarehouseOutboundInfo> cancel(@PathVariable String outboundId) {
        WarehouseOutboundInfo response = warehouseOutboundService.cancel(outboundId, RequestContext.getRequestId());
        return ApiResponse.success(response, "Cancel warehouse outbound success");
    }

    @PostMapping("/{outboundId}/resubmit")
    public ApiResponse<WarehouseOutboundInfo> resubmit(@PathVariable String outboundId) {
        WarehouseOutboundInfo response = warehouseOutboundService.resubmit(outboundId, RequestContext.getRequestId());
        return ApiResponse.success(response, "Resubmit warehouse outbound success");
    }

    @PostMapping("/{outboundId}/upload-file")
    public ApiResponse<PaymentFileUploadInfo> uploadFile(
            @PathVariable String outboundId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "category", required = false) String category) {
        PaymentFileUploadInfo response = fileService.uploadFileForWarehouseOutbound(file, category, outboundId,
                RequestContext.getRequestId());
        return ApiResponse.success(response, "Upload warehouse outbound file success");
    }

    @GetMapping("/{outboundId}/files")
    public ApiResponse<List<PaymentFileUploadInfo>> listFiles(@PathVariable String outboundId) {
        List<PaymentFileUploadInfo> response = fileService.listUploadedFilesForWarehouseOutbound(outboundId,
                RequestContext.getRequestId());
        return ApiResponse.success(response, "List warehouse outbound files success");
    }
}
