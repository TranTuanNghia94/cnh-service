package com.cnh.ies.model.general;

import java.time.LocalDateTime;

import com.cnh.ies.constant.Constant;
import com.fasterxml.jackson.annotation.JsonInclude;

import com.cnh.ies.util.RequestContext;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponseModel<T> {
    private String status;
    private String message;
    private T data;
    private String requestId;
    private String errorCode;
    private String errorMessage;
    private Object errorDetails;
    private LocalDateTime timestamp;

    public static <T> ApiResponseModel<T> success(T data) {
        return ApiResponseModel.<T>builder()
                .status(Constant.SUCCESS)
                .data(data)
                .requestId(RequestContext.getCurrentRequestId())
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponseModel<T> success(String message) {
        return ApiResponseModel.<T>builder()
                .status(Constant.SUCCESS)
                .message(message)
                .timestamp(LocalDateTime.now())
                .requestId(RequestContext.getCurrentRequestId())
                .build();
    }

    public static <T> ApiResponseModel<T> success(String requestId, T data, String message) {
        return ApiResponseModel.<T>builder()
                .status(Constant.SUCCESS)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .requestId(requestId)
                .build();
    }

    public static <T> ApiResponseModel<T> error(String errorCode, String errorMessage, Object errorDetails) {
        return ApiResponseModel.<T>builder()
                .status(Constant.ERROR)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .errorDetails(errorDetails)
                .timestamp(LocalDateTime.now())
                .requestId(RequestContext.getCurrentRequestId())
                .build();
    }

    public static <T> ApiResponseModel<T> error(String requestId, String errorCode, String errorMessage) {
        return ApiResponseModel.<T>builder()
                .status(Constant.ERROR)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .timestamp(LocalDateTime.now())
                .requestId(requestId)
                .build();
    }



    public static <T> ApiResponseModel<T> warning(String message) {
        return ApiResponseModel.<T>builder()
                .status(Constant.WARNING)
                .message(message)
                .timestamp(LocalDateTime.now())
                .requestId(RequestContext.getCurrentRequestId())
                .build();
    }


    public static <T> ApiResponseModel<T> pending(String message) {
        return ApiResponseModel.<T>builder()
                .status(Constant.PENDING)
                .message(message)
                .timestamp(LocalDateTime.now())
                .requestId(RequestContext.getCurrentRequestId())
                .build();
    }
    
    
}