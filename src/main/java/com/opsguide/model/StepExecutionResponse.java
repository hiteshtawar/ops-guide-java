package com.opsguide.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StepExecutionResponse {
    
    private String stepId;
    private String requestId;
    private String stepName;
    private StepExecution.StepStatus status;
    private StepExecution.StepType type;
    private Boolean requiresApproval;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private StepExecution.StepResult result;
    private String errorMessage;
    private Map<String, Object> metadata;
}

