package com.cnh.ies.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import com.cnh.ies.dto.common.ApiResponse;

import lombok.extern.slf4j.Slf4j;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Object>> handleApiException(ApiException ex, WebRequest request) {
        int status = ex.getHttpStatus();
        if (status >= 500) {
            log.error("[rid={}] Server error {} (HTTP {}): {}",
                    ex.getRequestId(), ex.getErrorCode(), status, ex.getErrorMessage(), ex);
        } else {
            log.warn("[rid={}] Client error {} (HTTP {}): {}",
                    ex.getRequestId(), ex.getErrorCode(), status, ex.getErrorMessage());
        }

        ApiResponse<Object> response = ApiResponse.<Object>builder()
                .success(false)
                .error(ex.getErrorMessage())
                .errorCode(ex.getErrorCode())
                .status(status)
                .requestId(ex.getRequestId())
                .build();

        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(Exception ex, WebRequest request) {
        log.error("Unhandled exception on {}: {}", request.getDescription(false), ex.getMessage(), ex);

        ApiResponse<Object> response = ApiResponse.<Object>builder()
                .success(false)
                .error("An unexpected error occurred")
                .errorCode(ApiException.ErrorCode.INTERNAL_ERROR)
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
