package com.opsguide.model;

import java.util.Map;

public class ClassificationResult {
    
    private UseCase useCase;
    private TaskId taskId;
    private double confidence;
    private Map<String, Object> extractedEntities;
    private String environment;
    private String service;
    
    public ClassificationResult() {}
    
    public ClassificationResult(UseCase useCase, TaskId taskId, double confidence, 
                              Map<String, Object> extractedEntities, String environment, String service) {
        this.useCase = useCase;
        this.taskId = taskId;
        this.confidence = confidence;
        this.extractedEntities = extractedEntities;
        this.environment = environment;
        this.service = service;
    }
    
    // Getters and Setters
    public UseCase getUseCase() {
        return useCase;
    }
    
    public void setUseCase(UseCase useCase) {
        this.useCase = useCase;
    }
    
    public TaskId getTaskId() {
        return taskId;
    }
    
    public void setTaskId(TaskId taskId) {
        this.taskId = taskId;
    }
    
    public double getConfidence() {
        return confidence;
    }
    
    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }
    
    public Map<String, Object> getExtractedEntities() {
        return extractedEntities;
    }
    
    public void setExtractedEntities(Map<String, Object> extractedEntities) {
        this.extractedEntities = extractedEntities;
    }
    
    public String getEnvironment() {
        return environment;
    }
    
    public void setEnvironment(String environment) {
        this.environment = environment;
    }
    
    public String getService() {
        return service;
    }
    
    public void setService(String service) {
        this.service = service;
    }
}
