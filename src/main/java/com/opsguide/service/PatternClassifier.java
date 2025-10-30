package com.opsguide.service;

import com.opsguide.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class PatternClassifier {
    
    @Autowired
    private EntityExtractor entityExtractor;
    
    // Task identification patterns
    private static final Map<TaskId, Pattern[]> TASK_PATTERNS = Map.of(
        TaskId.CANCEL_ORDER, new Pattern[]{
            Pattern.compile("\\bcancel\\b.*\\border\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\border\\b.*\\bcancel\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bterminate\\b.*\\border\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\babort\\b.*\\border\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bstop\\b.*\\border\\b", Pattern.CASE_INSENSITIVE)
        },
        TaskId.UPDATE_ORDER_STATUS, new Pattern[]{
            Pattern.compile("\\bchange\\b.*\\border\\b.*\\bstatus\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\border\\b.*\\bstatus\\b.*\\bchange\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bupdate\\b.*\\border\\b.*\\bstatus\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\btransition\\b.*\\border\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bmove\\b.*\\border\\b.*\\bto\\b", Pattern.CASE_INSENSITIVE)
        },
        TaskId.CANCEL_CASE, new Pattern[]{
            Pattern.compile("\\bcancel\\b.*\\bcase\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bcase\\b.*\\bcancel\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bterminate\\b.*\\bcase\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\babort\\b.*\\bcase\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bstop\\b.*\\bcase\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bclose\\b.*\\bcase\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bcase\\b.*\\bclose\\b", Pattern.CASE_INSENSITIVE)
        },
        TaskId.UPDATE_CASE_STATUS, new Pattern[]{
            Pattern.compile("\\bchange\\b.*\\bcase\\b.*\\bstatus\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bcase\\b.*\\bstatus\\b.*\\bchange\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bupdate\\b.*\\bcase\\b.*\\bstatus\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\btransition\\b.*\\bcase\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bmove\\b.*\\bcase\\b.*\\bto\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bset\\b.*\\bcase\\b.*\\bstatus\\b", Pattern.CASE_INSENSITIVE)
        },
        TaskId.UPDATE_SAMPLES, new Pattern[]{
            Pattern.compile("\\bupdate\\b.*\\bsamples?\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bchange\\b.*\\bsamples?\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bmodify\\b.*\\bsamples?\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bsamples?\\b.*\\bupdate\\b", Pattern.CASE_INSENSITIVE)
        },
        TaskId.UPDATE_STAIN, new Pattern[]{
            Pattern.compile("\\bupdate\\b.*\\bstain\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bchange\\b.*\\bstain\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bmodify\\b.*\\bstain\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bstain\\b.*\\bupdate\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bstain\\b.*\\bslide\\b", Pattern.CASE_INSENSITIVE)
        }
    );
    
    // Environment detection patterns
    private static final Map<String, Pattern[]> ENV_PATTERNS = Map.of(
        "dev", new Pattern[]{
            Pattern.compile("\\bdev\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bdevelopment\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bdev-\\w+\\b", Pattern.CASE_INSENSITIVE)
        },
        "staging", new Pattern[]{
            Pattern.compile("\\bstaging\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bstage\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bstg\\b", Pattern.CASE_INSENSITIVE)
        },
        "prod", new Pattern[]{
            Pattern.compile("\\bprod\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bproduction\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bprd\\b", Pattern.CASE_INSENSITIVE)
        }
    );
    
    // Service detection patterns
    private static final Map<String, Pattern[]> SERVICE_PATTERNS = Map.of(
        "Order", new Pattern[]{
            Pattern.compile("\\border\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\borders\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\border management\\b", Pattern.CASE_INSENSITIVE)
        },
        "Case", new Pattern[]{
            Pattern.compile("\\bcase\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bcases\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bcase management\\b", Pattern.CASE_INSENSITIVE)
        },
        "Sample", new Pattern[]{
            Pattern.compile("\\bsample\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bsamples\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bsample management\\b", Pattern.CASE_INSENSITIVE)
        },
        "Slide", new Pattern[]{
            Pattern.compile("\\bslide\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bslides\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bslide management\\b", Pattern.CASE_INSENSITIVE)
        },
        "Stain", new Pattern[]{
            Pattern.compile("\\bstain\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bstains\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bstaining\\b", Pattern.CASE_INSENSITIVE)
        }
    );
    
    public ClassificationResult classify(OperationalRequest request) {
        String queryLower = request.getQuery().toLowerCase();
        
        // For MVP, we only handle U2 (Operational Ask)
        UseCase useCase = UseCase.OPERATIONAL_ASK;
        
        // Identify specific task using pattern matching
        TaskId taskId = identifyTask(queryLower);
        
        // Extract entities from the query
        String environment = extractEnvironment(queryLower, request.getEnvironment());
        String service = extractService(queryLower);
        
        // Extract case-specific entities
        Map<String, Object> extractedEntities = entityExtractor.extractAllEntities(request.getQuery());
        extractedEntities.put("service", service);
        
        // Calculate confidence based on pattern matches
        double confidence = (taskId != null) ? 0.9 : 0.5;
        
        return new ClassificationResult(
            useCase,
            taskId,
            confidence,
            extractedEntities,
            environment,
            service
        );
    }
    
    private TaskId identifyTask(String query) {
        for (Map.Entry<TaskId, Pattern[]> entry : TASK_PATTERNS.entrySet()) {
            for (Pattern pattern : entry.getValue()) {
                if (pattern.matcher(query).find()) {
                    return entry.getKey();
                }
            }
        }
        
        // Fallback logic - generic pattern matching
        if (query.contains("cancel") || query.contains("terminate") || 
            query.contains("abort") || query.contains("stop")) {
            if (query.contains("order")) return TaskId.CANCEL_ORDER;
            if (query.contains("case")) return TaskId.CANCEL_CASE;
        }
        
        if (query.contains("status") || query.contains("change") || 
            query.contains("update") || query.contains("transition")) {
            if (query.contains("order")) return TaskId.UPDATE_ORDER_STATUS;
            if (query.contains("case")) return TaskId.UPDATE_CASE_STATUS;
        }
        
        if (query.contains("sample")) return TaskId.UPDATE_SAMPLES;
        if (query.contains("stain")) return TaskId.UPDATE_STAIN;
        
        return null;
    }
    
    private String extractEnvironment(String query, String defaultEnv) {
        for (Map.Entry<String, Pattern[]> entry : ENV_PATTERNS.entrySet()) {
            for (Pattern pattern : entry.getValue()) {
                if (pattern.matcher(query).find()) {
                    return entry.getKey();
                }
            }
        }
        return defaultEnv;
    }
    
    private String extractService(String query) {
        for (Map.Entry<String, Pattern[]> entry : SERVICE_PATTERNS.entrySet()) {
            for (Pattern pattern : entry.getValue()) {
                if (pattern.matcher(query).find()) {
                    return entry.getKey();
                }
            }
        }
        // Default to generic service
        return "Generic";
    }
}
