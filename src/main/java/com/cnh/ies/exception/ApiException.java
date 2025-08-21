package com.cnh.ies.exception;

import lombok.Getter;

@Getter
public class ApiException extends RuntimeException {
    
    private final String errorCode;
    private final String errorMessage;
    private final int httpStatus;
    private final String requestId;

    public ApiException(String errorCode, String errorMessage, int httpStatus, String requestId) {
        super(errorMessage);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.httpStatus = httpStatus;
        this.requestId = requestId;
    }

    public static class ErrorCode {
        public static final String INVALID_REQUEST = "INVALID_REQUEST";
        public static final String UNAUTHORIZED = "UNAUTHORIZED";
        public static final String FORBIDDEN = "FORBIDDEN";
        public static final String NOT_FOUND = "NOT_FOUND";
        public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
        public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
        public static final String BUSINESS_ERROR = "BUSINESS_ERROR";
        public static final String SERVICE_UNAVAILABLE = "SERVICE_UNAVAILABLE";
        public static final String INVALID_OLD_PASSWORD = "INVALID_OLD_PASSWORD";
        public static final String BAD_REQUEST = "BAD_REQUEST";
        public static final String CONFLICT = "CONFLICT";
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ApiException{");
        sb.append("errorCode='").append(errorCode).append("', ");
        sb.append("message='").append(getMessage()).append("'");
        if (requestId != null) {
            sb.append(", requestId='").append(requestId).append("'");
        }
        sb.append(", httpStatus=").append(httpStatus);
        sb.append("}");
        return sb.toString();
    }
    
    
}
