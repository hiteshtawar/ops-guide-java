package com.opsguide.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OperationalRequest {
    
    @NotBlank
    private String requestId;
    
    @NotBlank
    private String userId;
    
    @NotBlank
    private String query;
    
    private Map<String, Object> context;
    
    private String environment = "dev";
    
    @NotNull
    private LocalDateTime timestamp = LocalDateTime.now();
}