package com.dpl.whatsapp.controller;

import com.dpl.whatsapp.service.AzureAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Health check and status endpoints
 */
@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
@Tag(name = "Health", description = "Health check endpoints")
public class HealthController {

    private final AzureAuthService authService;

    @GetMapping
    @Operation(summary = "Basic health check")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now().toString());
        health.put("service", "WhatsApp CRM Integration");
        return ResponseEntity.ok(health);
    }

    @GetMapping("/crm")
    @Operation(summary = "Check CRM/Dataverse connectivity")
    public ResponseEntity<Map<String, Object>> crmHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("timestamp", LocalDateTime.now().toString());
        
        try {
            boolean valid = authService.isTokenValid();
            health.put("status", valid ? "UP" : "DOWN");
            health.put("message", valid ? "Successfully connected to Dynamics 365" : "Failed to authenticate");
        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("message", "Error: " + e.getMessage());
        }
        
        return ResponseEntity.ok(health);
    }
}
