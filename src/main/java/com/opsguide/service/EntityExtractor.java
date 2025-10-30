package com.opsguide.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class EntityExtractor {
    
    // Generic ID patterns for various entities
    private static final Pattern ORDER_ID_PATTERN = Pattern.compile(
        "ORDER[_-]?(\\d{4})[_-]?([\\w-]+)", Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern CASE_ID_PATTERN = Pattern.compile(
        "CASE[_-]?(\\d{4})[_-]?([\\w-]+)", Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern SAMPLE_ID_PATTERN = Pattern.compile(
        "SAMPLE[_-]?(\\d{4})[_-]?([\\w-]+)", Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern SLIDE_ID_PATTERN = Pattern.compile(
        "SLIDE[_-]?(\\d{4})[_-]?([\\w-]+)", Pattern.CASE_INSENSITIVE
    );
    
    // Generic ID patterns
    private static final Pattern GENERIC_ID_PATTERN = Pattern.compile(
        "\\b(\\w+)[\\s_-]?(\\d+)\\b", Pattern.CASE_INSENSITIVE
    );
    
    // Common status keywords (no validation - just extraction)
    private static final String[] STATUS_KEYWORDS = {
        "pending", "in_progress", "completed", "cancelled", "on_hold", 
        "failed", "archived", "closed", "active", "inactive", "processing",
        "ready", "waiting", "approved", "rejected", "draft", "published"
    };
    
    public String extractOrderId(String query) {
        Matcher matcher = ORDER_ID_PATTERN.matcher(query);
        if (matcher.find()) {
            return matcher.group(1) + "-" + matcher.group(2);
        }
        return null;
    }
    
    public String extractCaseId(String query) {
        Matcher matcher = CASE_ID_PATTERN.matcher(query);
        if (matcher.find()) {
            return matcher.group(1) + "-" + matcher.group(2);
        }
        return null;
    }
    
    public String extractSampleId(String query) {
        Matcher matcher = SAMPLE_ID_PATTERN.matcher(query);
        if (matcher.find()) {
            return matcher.group(1) + "-" + matcher.group(2);
        }
        return null;
    }
    
    public String extractSlideId(String query) {
        Matcher matcher = SLIDE_ID_PATTERN.matcher(query);
        if (matcher.find()) {
            return matcher.group(1) + "-" + matcher.group(2);
        }
        return null;
    }
    
    public String extractGenericId(String query) {
        Matcher matcher = GENERIC_ID_PATTERN.matcher(query);
        if (matcher.find()) {
            return matcher.group(1) + "-" + matcher.group(2);
        }
        return null;
    }
    
    public String extractTargetStatus(String query) {
        String queryLower = query.toLowerCase();
        
        for (String status : STATUS_KEYWORDS) {
            if (queryLower.contains(status)) {
                return status;
            }
        }
        
        return null;
    }
    
    public String extractEntityType(String query) {
        String queryLower = query.toLowerCase();
        
        if (queryLower.contains("order")) return "order";
        if (queryLower.contains("case")) return "case";
        if (queryLower.contains("sample")) return "sample";
        if (queryLower.contains("slide")) return "slide";
        if (queryLower.contains("stain")) return "stain";
        
        return "unknown";
    }
    
    public Map<String, Object> extractAllEntities(String query) {
        Map<String, Object> entities = new HashMap<>();
        
        // Extract various ID types
        String orderId = extractOrderId(query);
        if (orderId != null) {
            entities.put("order_id", orderId);
        }
        
        String caseId = extractCaseId(query);
        if (caseId != null) {
            entities.put("case_id", caseId);
        }
        
        String sampleId = extractSampleId(query);
        if (sampleId != null) {
            entities.put("sample_id", sampleId);
        }
        
        String slideId = extractSlideId(query);
        if (slideId != null) {
            entities.put("slide_id", slideId);
        }
        
        // Extract generic ID if no specific ID found
        if (entities.isEmpty()) {
            String genericId = extractGenericId(query);
            if (genericId != null) {
                entities.put("entity_id", genericId);
            }
        }
        
        // Extract status
        String targetStatus = extractTargetStatus(query);
        if (targetStatus != null) {
            entities.put("target_status", targetStatus);
        }
        
        // Extract entity type
        String entityType = extractEntityType(query);
        entities.put("entity_type", entityType);
        
        return entities;
    }
}
