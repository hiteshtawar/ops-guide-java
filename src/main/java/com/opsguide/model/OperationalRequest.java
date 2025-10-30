package com.opsguide.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.Map;

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
    private LocalDateTime timestamp;
    
    public OperationalRequest() {
        this.timestamp = LocalDateTime.now();
    }
    
    public OperationalRequest(String requestId, String userId, String query, 
                            Map<String, Object> context, String environment) {
        this.requestId = requestId;
        this.userId = userId;
        this.query = query;
        this.context = context;
        this.environment = environment;
        this.timestamp = LocalDateTime.now();
    }
    
    // Getters and Setters
    public String getRequestId() {
        return requestId;
    }
    
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getQuery() {
        return query;
    }
    
    public void setQuery(String query) {
        this.query = query;
    }
    
    public Map<String, Object> getContext() {
        return context;
    }
    
    public void setContext(Map<String, Object> context) {
        this.context = context;
    }
    
    public String getEnvironment() {
        return environment;
    }
    
    public void setEnvironment(String environment) {
        this.environment = environment;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
