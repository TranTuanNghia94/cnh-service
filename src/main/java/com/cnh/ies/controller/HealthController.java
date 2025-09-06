package com.cnh.ies.controller;

import com.cnh.ies.dto.common.ApiResponse;
import com.cnh.ies.exception.ApiException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
@Tag(name = "Health", description = "Health check APIs")
public class HealthController {
    
    @GetMapping
    @Operation(summary = "Health check", description = "Check application health status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        Map<String, Object> healthInfo = new HashMap<>();
        healthInfo.put("status", "UP");
        healthInfo.put("timestamp", LocalDateTime.now());
        healthInfo.put("service", "CNH Backend Service");
        healthInfo.put("version", "2.0.2");
        
        return ResponseEntity.ok(ApiResponse.success(healthInfo, "Service is healthy"));
    }
    
    @GetMapping("/ready")
    @Operation(summary = "Readiness check", description = "Check if the application is ready to serve requests")
    public ResponseEntity<ApiResponse<Map<String, Object>>> readiness() {
        Map<String, Object> readinessInfo = new HashMap<>();
        readinessInfo.put("status", "READY");
        readinessInfo.put("timestamp", LocalDateTime.now());
        readinessInfo.put("database", "UP");
        readinessInfo.put("redis", "UP");
        
        return ResponseEntity.ok(ApiResponse.success(readinessInfo, "Service is ready"));
    }
    
}
