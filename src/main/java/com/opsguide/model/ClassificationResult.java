package com.opsguide.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClassificationResult {
    
    private UseCase useCase;
    private TaskId taskId;
    private double confidence;
    private Map<String, Object> extractedEntities;
    private String environment;
    private String service;
}