package com.opsguide.controller;

import com.opsguide.model.*;
import com.opsguide.service.PatternClassifier;
import com.opsguide.service.RAGOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class OpsGuideController {
    
    private final PatternClassifier patternClassifier;
    private final RAGOrchestrator ragOrchestrator;
    
    @PostMapping("/request")
    public ResponseEntity<OperationalResponse> processRequest(
            @RequestBody OperationalRequest request,
            @RequestHeader("X-User-ID") String userId,
            @RequestParam(value = "mode", defaultValue = "core") String mode) {
        
        try {
            // Set user ID from header
            request.setUserId(userId);
            
            // Generate request ID if not provided
            if (request.getRequestId() == null) {
                request.setRequestId(UUID.randomUUID().toString());
            }
            
            // Manual validation
            if (request.getQuery() == null || request.getQuery().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Query is required", request.getRequestId()));
            }
            
            // Process based on mode
            if ("rag".equalsIgnoreCase(mode)) {
                return processWithRAG(request);
            } else {
                return processCore(request);
            }
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(createErrorResponse(request.getRequestId(), e.getMessage()));
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = Map.of(
            "status", "healthy",
            "service", "opsguide-java",
            "timestamp", LocalDateTime.now(),
            "version", "1.0.0",
            "components", Map.of(
                "parsing_validation", "active",
                "pattern_classification", "active",
                "entity_extraction", "active",
                "rag_orchestrator", "active"
            )
        );
        return ResponseEntity.ok(health);
    }
    
    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> getApiInfo() {
        Map<String, Object> info = Map.of(
            "service", "OpsGuide Java",
            "version", "1.0.0",
            "description", "RAG-powered operational intelligence with dual-mode architecture",
            "endpoints", Map.of(
                "POST /v1/request", "Submit operational request",
                "GET /v1/health", "Health check"
            ),
            "supported_tasks", new String[]{
                "CANCEL_ORDER: cancel order ORDER-2024-001",
                "UPDATE_ORDER_STATUS: change order status to completed",
                "CANCEL_CASE: cancel case CASE-2024-001",
                "UPDATE_CASE_STATUS: change case status to completed",
                "UPDATE_SAMPLES: update samples within case",
                "UPDATE_STAIN: update stain of a slide"
            },
            "modes", Map.of(
                "core", "Pattern matching only (zero AI costs)",
                "rag", "Full RAG pipeline with AI reasoning"
            ),
            "architecture", new String[]{
                "1. HTTP Parsing & Validation",
                "2. Pattern-based Classification",
                "3. Entity Extraction",
                "4. Structured Response (Core) OR RAG Pipeline (Premium)"
            }
        );
        return ResponseEntity.ok(info);
    }
    
    private ResponseEntity<OperationalResponse> processCore(OperationalRequest request) {
        // Core mode: Pattern matching only
        ClassificationResult classification = patternClassifier.classify(request);
        
        OperationalResponse response = buildCoreResponse(request, classification);
        return ResponseEntity.ok(response);
    }
    
    private ResponseEntity<OperationalResponse> processWithRAG(OperationalRequest request) {
        // RAG mode: Full AI pipeline
        try {
            OperationalResponse response = ragOrchestrator.processWithRAG(request).get();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Fallback to core mode if RAG fails
            ClassificationResult classification = patternClassifier.classify(request);
            OperationalResponse response = buildCoreResponse(request, classification);
            response.setStatus("processed_with_fallback");
            return ResponseEntity.ok(response);
        }
    }
    
    private OperationalResponse buildCoreResponse(OperationalRequest request, ClassificationResult classification) {
        OperationalResponse.InputData input = new OperationalResponse.InputData(
            request.getQuery(),
            request.getEnvironment(),
            request.getUserId()
        );
        
        OperationalResponse.ClassificationData classificationData = new OperationalResponse.ClassificationData(
            classification.getUseCase().getValue(),
            classification.getTaskId() != null ? classification.getTaskId().getValue() : null,
            classification.getConfidence(),
            classification.getService(),
            classification.getEnvironment()
        );
        
        OperationalResponse.NextSteps nextSteps = null;
        if (classification.getTaskId() != null) {
            nextSteps = getNextSteps(classification.getTaskId());
        }
        
        return new OperationalResponse(
            request.getRequestId(),
            "processed",
            LocalDateTime.now(),
            input,
            classificationData,
            classification.getExtractedEntities(),
            nextSteps
        );
    }
    
    private OperationalResponse.NextSteps getNextSteps(TaskId taskId) {
        switch (taskId) {
            case CANCEL_ORDER:
                return new OperationalResponse.NextSteps(
                    "Order cancellation request identified",
                    "knowledge/runbooks/cancel-order-runbook.md",
                    "knowledge/api-specs/order-management-api.md",
                    new String[]{
                        "Validate order exists and is cancellable",
                        "Check user permissions",
                        "Execute cancellation via API",
                        "Verify cancellation completed"
                    }
                );
            case UPDATE_ORDER_STATUS:
                return new OperationalResponse.NextSteps(
                    "Order status update request identified",
                    "knowledge/runbooks/update-order-status-runbook.md",
                    "knowledge/api-specs/order-management-api.md",
                    new String[]{
                        "Validate order exists",
                        "Check status transition is valid",
                        "Update order status via API",
                        "Verify status change completed"
                    }
                );
            case CANCEL_CASE:
                return new OperationalResponse.NextSteps(
                    "Case cancellation request identified",
                    "knowledge/runbooks/cancel-case-runbook.md",
                    "knowledge/api-specs/case-management-api.md",
                    new String[]{
                        "Validate case exists and is cancellable",
                        "Check user permissions",
                        "Execute cancellation via API",
                        "Verify cancellation completed"
                    }
                );
            case UPDATE_CASE_STATUS:
                return new OperationalResponse.NextSteps(
                    "Case status update request identified",
                    "knowledge/runbooks/update-case-status-runbook.md",
                    "knowledge/api-specs/case-management-api.md",
                    new String[]{
                        "Validate case exists",
                        "Check status transition is valid",
                        "Update case status via API",
                        "Verify status change completed"
                    }
                );
            case UPDATE_SAMPLES:
                return new OperationalResponse.NextSteps(
                    "Sample update request identified",
                    "knowledge/runbooks/update-samples-runbook.md",
                    "knowledge/api-specs/sample-management-api.md",
                    new String[]{
                        "Validate case and samples exist",
                        "Check sample update permissions",
                        "Execute sample update via API",
                        "Verify sample update completed"
                    }
                );
            case UPDATE_STAIN:
                return new OperationalResponse.NextSteps(
                    "Stain update request identified",
                    "knowledge/runbooks/update-stain-runbook.md",
                    "knowledge/api-specs/slide-management-api.md",
                    new String[]{
                        "Validate slide and stain exist",
                        "Check stain update permissions",
                        "Execute stain update via API",
                        "Verify stain update completed"
                    }
                );
            default:
                return new OperationalResponse.NextSteps(
                    "Generic operational request identified",
                    "knowledge/runbooks/generic-operation-runbook.md",
                    "knowledge/api-specs/generic-api.md",
                    new String[]{
                        "Analyze request requirements",
                        "Identify target system and API",
                        "Execute operation via appropriate API",
                        "Verify operation completed successfully"
                    }
                );
        }
    }
    
    private OperationalResponse createErrorResponse(String message, String requestId) {
        OperationalResponse response = new OperationalResponse();
        response.setRequestId(requestId);
        response.setStatus("error");
        response.setTimestamp(LocalDateTime.now());
        
        // Create minimal input data
        OperationalResponse.InputData inputData = new OperationalResponse.InputData();
        inputData.setQuery("");
        inputData.setEnvironment("dev");
        inputData.setUserId("");
        response.setInput(inputData);
        
        // Create error classification
        OperationalResponse.ClassificationData classification = new OperationalResponse.ClassificationData();
        classification.setUseCase("ERROR");
        classification.setTaskId("ERROR");
        classification.setConfidence(0.0);
        classification.setService("System");
        classification.setEnvironment("dev");
        response.setClassification(classification);
        
        // Create error next steps
        OperationalResponse.NextSteps nextSteps = new OperationalResponse.NextSteps();
        nextSteps.setDescription("Error: " + message);
        nextSteps.setRunbook("knowledge/runbooks/error-handling.md");
        nextSteps.setApiSpec("knowledge/api-specs/error-api.md");
        nextSteps.setTypicalSteps(new String[]{"Review error message", "Check request format", "Retry with corrected data"});
        response.setNextSteps(nextSteps);
        
        return response;
    }
}
