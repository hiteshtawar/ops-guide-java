package com.opsguide.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OperationalResponse {
    
    private String requestId;
    private String status;
    private LocalDateTime timestamp;
    private InputData input;
    private ClassificationData classification;
    private Map<String, Object> extractedEntities;
    private NextSteps nextSteps;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InputData {
        private String query;
        private String environment;
        private String userId;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClassificationData {
        private String useCase;
        private String taskId;
        private double confidence;
        private String service;
        private String environment;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NextSteps {
        private String description;
        private String runbook;
        private String apiSpec;
        private String[] typicalSteps;
        private StepExecutionMetadata[] stepMetadata;
        
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class StepExecutionMetadata {
            private String stepName;
            private Boolean autoExecutable;
            private Boolean requiresApproval;
            private String stepType; // VALIDATION, PERMISSION_CHECK, API_EXECUTION, VERIFICATION
            private String apiEndpoint; // API endpoint to call for this step
            private String httpMethod; // GET, POST, PATCH, DELETE
            private Map<String, Object> apiParameters; // Parameters for the API call
        }
    }
}