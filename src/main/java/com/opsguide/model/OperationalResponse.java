package com.opsguide.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.Map;

public class OperationalResponse {
    
    private String requestId;
    private String status;
    private LocalDateTime timestamp;
    private InputData input;
    private ClassificationData classification;
    private Map<String, Object> extractedEntities;
    private NextSteps nextSteps;
    
    public OperationalResponse() {}
    
    public OperationalResponse(String requestId, String status, LocalDateTime timestamp,
                             InputData input, ClassificationData classification,
                             Map<String, Object> extractedEntities, NextSteps nextSteps) {
        this.requestId = requestId;
        this.status = status;
        this.timestamp = timestamp;
        this.input = input;
        this.classification = classification;
        this.extractedEntities = extractedEntities;
        this.nextSteps = nextSteps;
    }
    
    // Getters and Setters
    public String getRequestId() {
        return requestId;
    }
    
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public InputData getInput() {
        return input;
    }
    
    public void setInput(InputData input) {
        this.input = input;
    }
    
    public ClassificationData getClassification() {
        return classification;
    }
    
    public void setClassification(ClassificationData classification) {
        this.classification = classification;
    }
    
    public Map<String, Object> getExtractedEntities() {
        return extractedEntities;
    }
    
    public void setExtractedEntities(Map<String, Object> extractedEntities) {
        this.extractedEntities = extractedEntities;
    }
    
    public NextSteps getNextSteps() {
        return nextSteps;
    }
    
    public void setNextSteps(NextSteps nextSteps) {
        this.nextSteps = nextSteps;
    }
    
    // Inner classes
    public static class InputData {
        private String query;
        private String environment;
        private String userId;
        
        public InputData() {}
        
        public InputData(String query, String environment, String userId) {
            this.query = query;
            this.environment = environment;
            this.userId = userId;
        }
        
        // Getters and Setters
        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }
        public String getEnvironment() { return environment; }
        public void setEnvironment(String environment) { this.environment = environment; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
    }
    
    public static class ClassificationData {
        private String useCase;
        private String taskId;
        private double confidence;
        private String service;
        private String environment;
        
        public ClassificationData() {}
        
        public ClassificationData(String useCase, String taskId, double confidence, 
                                String service, String environment) {
            this.useCase = useCase;
            this.taskId = taskId;
            this.confidence = confidence;
            this.service = service;
            this.environment = environment;
        }
        
        // Getters and Setters
        public String getUseCase() { return useCase; }
        public void setUseCase(String useCase) { this.useCase = useCase; }
        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
        public String getService() { return service; }
        public void setService(String service) { this.service = service; }
        public String getEnvironment() { return environment; }
        public void setEnvironment(String environment) { this.environment = environment; }
    }
    
    public static class NextSteps {
        private String description;
        private String runbook;
        private String apiSpec;
        private String[] typicalSteps;
        
        public NextSteps() {}
        
        public NextSteps(String description, String runbook, String apiSpec, String[] typicalSteps) {
            this.description = description;
            this.runbook = runbook;
            this.apiSpec = apiSpec;
            this.typicalSteps = typicalSteps;
        }
        
        // Getters and Setters
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getRunbook() { return runbook; }
        public void setRunbook(String runbook) { this.runbook = runbook; }
        public String getApiSpec() { return apiSpec; }
        public void setApiSpec(String apiSpec) { this.apiSpec = apiSpec; }
        public String[] getTypicalSteps() { return typicalSteps; }
        public void setTypicalSteps(String[] typicalSteps) { this.typicalSteps = typicalSteps; }
    }
}
