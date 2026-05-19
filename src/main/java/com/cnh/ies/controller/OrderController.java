package com.cnh.ies.controller;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.cnh.ies.dto.common.ApiResponse;
import com.cnh.ies.util.RequestContext;
import com.cnh.ies.dto.response.UploadOjectResponse;
import com.cnh.ies.model.general.ListDataModel;
import com.cnh.ies.model.order.BatchOrderImportJobDetailInfo;
import com.cnh.ies.model.order.BatchOrderImportJobInfo;
import com.cnh.ies.model.order.CreateOrderRequest;
import com.cnh.ies.model.order.OrderListRequest;
import com.cnh.ies.model.order.OrderInfo;
import com.cnh.ies.model.order.UpdateOrderStatusRequest;
import com.cnh.ies.service.order.BatchOrderImportJobService;
import com.cnh.ies.service.order.OrderService;
import com.cnh.ies.service.order.UploadBatchOrderService;
import com.cnh.ies.service.security.AuthenticationUserDetails;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
@Slf4j
public class OrderController {
    private final OrderService orderService;
    private final UploadBatchOrderService uploadBatchOrderService;
    private final BatchOrderImportJobService batchOrderImportJobService;

    @PostMapping("/upload-file-batch-order")
    public ApiResponse<UploadOjectResponse> uploadFileBatchOrder(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {
        String requestId = RequestContext.getRequestIdOrGenerate();
        String createdBy = getCurrentUsername(userDetails);
        log.info("Uploading batch order file, requestId: {}, createdBy: {}", requestId, createdBy);
        UploadOjectResponse response = uploadBatchOrderService.readExcelFile(file, requestId, createdBy);
        return ApiResponse.success(response, "Upload batch order file success");
    }

    @PostMapping("/upload-file-batch-order-async")
    public ApiResponse<String> uploadFileBatchOrderAsync(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {
        String requestId = RequestContext.getRequestIdOrGenerate();
        UUID ownerUserId = getCurrentUserId(userDetails);
        String createdBy = getCurrentUsername(userDetails);
        String jobId = batchOrderImportJobService.createAndDispatch(file, ownerUserId, createdBy, requestId);
        return ApiResponse.success(jobId, "Batch order import job created");
    }

    @GetMapping("/batch-order-import-jobs")
    public ApiResponse<ListDataModel<BatchOrderImportJobInfo>> listBatchOrderImportJobs(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer limit,
            @AuthenticationPrincipal UserDetails userDetails) {
        String requestId = RequestContext.getRequestIdOrGenerate();
        UUID ownerUserId = getCurrentUserId(userDetails);
        ListDataModel<BatchOrderImportJobInfo> response =
                batchOrderImportJobService.listOwnedJobs(ownerUserId, page, limit, requestId);
        return ApiResponse.success(response, "List import jobs success");
    }

    @GetMapping("/batch-order-import-jobs/{jobId}")
    public ApiResponse<BatchOrderImportJobInfo> getBatchOrderImportJob(
            @PathVariable String jobId,
            @AuthenticationPrincipal UserDetails userDetails) {
        String requestId = RequestContext.getRequestIdOrGenerate();
        UUID ownerUserId = getCurrentUserId(userDetails);
        BatchOrderImportJobInfo response = batchOrderImportJobService.getOwnedJob(jobId, ownerUserId, requestId);
        return ApiResponse.success(response, "Get import job success");
    }

    @GetMapping("/batch-order-import-jobs/{jobId}/details")
    public ApiResponse<ListDataModel<BatchOrderImportJobDetailInfo>> getBatchOrderImportJobDetails(
            @PathVariable String jobId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer limit,
            @AuthenticationPrincipal UserDetails userDetails) {
        String requestId = RequestContext.getRequestIdOrGenerate();
        UUID ownerUserId = getCurrentUserId(userDetails);
        ListDataModel<BatchOrderImportJobDetailInfo> response =
                batchOrderImportJobService.getOwnedJobDetails(jobId, ownerUserId, page, limit, requestId);
        return ApiResponse.success(response, "Get import job details success");
    }

    @PostMapping("/create")
    public ApiResponse<OrderInfo> createOrder(@RequestBody CreateOrderRequest request) {
        String requestId = RequestContext.getRequestIdOrGenerate();
OrderInfo response = orderService.createOrder(request, requestId);
return ApiResponse.success(response, "Create order success");
    }

    @GetMapping("/{code}")
    public ApiResponse<OrderInfo> getOrderByCode(@PathVariable String code) {
        String requestId = RequestContext.getRequestIdOrGenerate();
OrderInfo response = orderService.getOrderByCode(code, requestId);
return ApiResponse.success(response, "Get order by code success");
    }

    @DeleteMapping("/delete/{id}")
    public ApiResponse<String> deleteOrder(@PathVariable String id) {
        String requestId = RequestContext.getRequestIdOrGenerate();
String response = orderService.deleteOrder(id, requestId);
return ApiResponse.success(response, "Delete order success");
    }

    @PostMapping("/update")
    public ApiResponse<OrderInfo> updateOrder(@RequestBody CreateOrderRequest request) {
        String requestId = RequestContext.getRequestIdOrGenerate();
OrderInfo response = orderService.updateOrder(request, requestId);
return ApiResponse.success(response, "Update order success");
    }


    @PostMapping("/update-status/{id}")
    public ApiResponse<OrderInfo> updateOrderStatus(@PathVariable String id, @RequestBody UpdateOrderStatusRequest request) {
        String requestId = RequestContext.getRequestIdOrGenerate();
OrderInfo response = orderService.updateOrderStatus(id, request.getStatus(), requestId);
return ApiResponse.success(response, "Update order status success");
    }

    @PostMapping("/list")
    public ApiResponse<ListDataModel<OrderInfo>> getAllOrders(@RequestBody OrderListRequest request) {
        String requestId = RequestContext.getRequestIdOrGenerate();
ListDataModel<OrderInfo> response = orderService.getAllOrders(
                requestId,
                request.getPage(),
                request.getLimit(),
                request.getCreatedBy(),
                request.getContractNumber(),
                request.getOrderNumber(),
                request.getStatus(),
                request.getCustomerName());
return ApiResponse.success(response, "Get all orders success");
    }

    private UUID getCurrentUserId(UserDetails userDetails) {
        if (userDetails instanceof AuthenticationUserDetails aud) {
            return aud.getUserId();
        }
        throw new IllegalStateException("Unexpected principal type: " + userDetails.getClass().getName());
    }

    private String getCurrentUsername(UserDetails userDetails) {
        if (userDetails instanceof AuthenticationUserDetails aud) {
            String username = aud.getUsername();
            if (username != null && !username.isBlank()) {
                return username;
            }
        }
        if (userDetails != null) {
            String username = userDetails.getUsername();
            if (username != null && !username.isBlank() && !"anonymousUser".equals(username)) {
                return username;
            }
        }
        return null;
    }
}
