package com.opsguide.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StepExecutionRequest {
    
    private String requestId;
    private String stepIndex;
    private String stepName;
    private String taskId;
    private Map<String, Object> extractedEntities;
    private Map<String, Object> context;
    private Boolean skipApproval; // For auto-execution
    private String apiEndpoint; // API endpoint from metadata
    private String httpMethod; // HTTP method from metadata
    private Map<String, Object> apiParameters; // API parameters from metadata
}

