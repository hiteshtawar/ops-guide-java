package com.opsguide.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StepExecution {
    
    private String stepId;
    private String requestId;
    private String stepName;
    private String stepDescription;
    private StepStatus status;
    private StepType type;
    private Boolean requiresApproval;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private StepResult result;
    private String errorMessage;
    private Map<String, Object> metadata;
    
    public enum StepStatus {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED,
        APPROVAL_REQUIRED,
        APPROVED,
        CANCELLED
    }
    
    public enum StepType {
        VALIDATION,      // Auto-execute: Validate case exists
        PERMISSION_CHECK, // Auto-execute: Check user permissions
        API_EXECUTION,    // Requires approval: Execute cancellation
        VERIFICATION      // Auto-execute: Verify cancellation completed
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StepResult {
        private Boolean success;
        private String message;
        private Map<String, Object> data;
        private Integer statusCode;
        private String apiResponse;
    }
}

