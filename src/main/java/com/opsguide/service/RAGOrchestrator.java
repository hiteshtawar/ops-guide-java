package com.opsguide.service;

import com.opsguide.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class RAGOrchestrator {
    
    private final EmbeddingsService embeddingsService;
    private final VectorSearchService vectorSearchService;
    private final LLMService llmService;
    private final PatternClassifier patternClassifier;
    
    @Async
    public CompletableFuture<OperationalResponse> processWithRAG(OperationalRequest request) {
        try {
            // Step 1: Generate embedding for the query
            CompletableFuture<List<Float>> embeddingFuture = 
                embeddingsService.generateEmbeddingAsync(request.getQuery());
            
            // Step 2: Vector search (depends on embedding)
            CompletableFuture<List<VectorSearchService.KnowledgeChunk>> searchFuture = 
                embeddingFuture.thenCompose(embedding -> 
                    vectorSearchService.searchAsync(embedding, 5));
            
            // Step 3: Pattern classification (parallel to search)
            CompletableFuture<ClassificationResult> classificationFuture = 
                CompletableFuture.supplyAsync(() -> patternClassifier.classify(request));
            
            // Step 4: LLM reasoning (depends on search results)
            CompletableFuture<String> llmResponseFuture = 
                searchFuture.thenCompose(knowledgeChunks -> 
                    llmService.generateResponseAsync(
                        buildPromptWithContext(request.getQuery(), knowledgeChunks)));
            
            // Step 5: Wait for all components to complete
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                classificationFuture, llmResponseFuture);
            
            // Step 6: Build final response
            return allFutures.thenApply(v -> {
                try {
                    ClassificationResult classification = classificationFuture.get();
                    String llmResponse = llmResponseFuture.get();
                    List<VectorSearchService.KnowledgeChunk> knowledgeChunks = searchFuture.get();
                    
                    return buildRAGResponse(request, classification, llmResponse, knowledgeChunks);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to build RAG response", e);
                }
            });
            
        } catch (Exception e) {
            // Fallback to core mode if RAG fails
            ClassificationResult classification = patternClassifier.classify(request);
            return CompletableFuture.completedFuture(buildCoreResponse(request, classification));
        }
    }
    
    private String buildPromptWithContext(String query, List<VectorSearchService.KnowledgeChunk> knowledgeChunks) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an operational intelligence assistant. Use the following knowledge base context to provide accurate, citation-backed responses.\n\n");
        prompt.append("Knowledge Base Context:\n");
        
        for (VectorSearchService.KnowledgeChunk chunk : knowledgeChunks) {
            prompt.append("Source: ").append(chunk.getSource()).append("\n");
            prompt.append("Content: ").append(chunk.getContent()).append("\n");
            prompt.append("Relevance Score: ").append(chunk.getScore()).append("\n\n");
        }
        
        prompt.append("User Query: ").append(query).append("\n\n");
        prompt.append("Provide a detailed response with specific API endpoints, procedures, and safety considerations. Include citations to the knowledge base sources.");
        
        return prompt.toString();
    }
    
    private OperationalResponse buildRAGResponse(OperationalRequest request, 
                                               ClassificationResult classification,
                                               String llmResponse,
                                               List<VectorSearchService.KnowledgeChunk> knowledgeChunks) {
        
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
        
        // Enhanced next steps with RAG insights
        OperationalResponse.NextSteps nextSteps = buildRAGNextSteps(classification.getTaskId(), llmResponse, knowledgeChunks);
        
        // Add RAG-specific metadata
        Map<String, Object> extractedEntities = classification.getExtractedEntities();
        extractedEntities.put("rag_response", llmResponse);
        extractedEntities.put("knowledge_sources", knowledgeChunks.stream()
            .map(chunk -> Map.of(
                "source", chunk.getSource(),
                "score", chunk.getScore()
            )).toList());
        
        return new OperationalResponse(
            request.getRequestId(),
            "processed_with_rag",
            java.time.LocalDateTime.now(),
            input,
            classificationData,
            extractedEntities,
            nextSteps
        );
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
        
        OperationalResponse.NextSteps nextSteps = getNextSteps(classification.getTaskId());
        
        return new OperationalResponse(
            request.getRequestId(),
            "processed_core",
            java.time.LocalDateTime.now(),
            input,
            classificationData,
            classification.getExtractedEntities(),
            nextSteps
        );
    }
    
    private OperationalResponse.NextSteps buildRAGNextSteps(TaskId taskId, String llmResponse, 
                                                          List<VectorSearchService.KnowledgeChunk> knowledgeChunks) {
        if (taskId == null) {
            return null;
        }
        
        String description = "AI-enhanced " + taskId.getValue().toLowerCase().replace("_", " ") + " request";
        String runbook = "knowledge/runbooks/" + taskId.getValue().toLowerCase().replace("_", "-") + "-runbook.md";
        String apiSpec = "knowledge/api-specs/case-management-api.md";
        
        // Extract steps from LLM response or use default
        String[] typicalSteps = extractStepsFromLLMResponse(llmResponse, taskId);
        
        // Create metadata for each step
        OperationalResponse.NextSteps.StepExecutionMetadata[] metadata = 
            new OperationalResponse.NextSteps.StepExecutionMetadata[typicalSteps.length];
        
        for (int i = 0; i < typicalSteps.length; i++) {
            String stepName = typicalSteps[i];
            boolean autoExecutable = isAutoExecutable(stepName);
            boolean requiresApproval = requiresApproval(stepName);
            String stepType = determineStepType(stepName);
            
            String apiEndpoint = getApiEndpointForStep(stepName, stepType);
            String httpMethod = getHttpMethodForStep(stepName, stepType);
            Map<String, Object> apiParameters = getApiParametersForStep(stepName, stepType);
            
            metadata[i] = new OperationalResponse.NextSteps.StepExecutionMetadata(
                stepName, autoExecutable, requiresApproval, stepType, apiEndpoint, httpMethod, apiParameters
            );
        }
        
        return new OperationalResponse.NextSteps(description, runbook, apiSpec, typicalSteps, metadata);
    }
    
    private boolean isAutoExecutable(String stepName) {
        String lower = stepName.toLowerCase();
        return lower.contains("validate") || 
               (lower.contains("check") && (lower.contains("permission") || lower.contains("exist"))) ||
               lower.contains("verify");
    }
    
    private boolean requiresApproval(String stepName) {
        String lower = stepName.toLowerCase();
        return lower.contains("execute") || 
               lower.contains("run") ||
               (lower.contains("cancel") && lower.contains("via")) ||
               (lower.contains("update") && lower.contains("via"));
    }
    
    private String determineStepType(String stepName) {
        String lower = stepName.toLowerCase();
        if (lower.contains("validate") || (lower.contains("check") && lower.contains("exist"))) {
            return "VALIDATION";
        } else if (lower.contains("permission")) {
            return "PERMISSION_CHECK";
        } else if (lower.contains("execute") || lower.contains("via")) {
            return "API_EXECUTION";
        } else if (lower.contains("verify") || lower.contains("confirm")) {
            return "VERIFICATION";
        }
        return "VALIDATION";
    }
    
    private OperationalResponse.NextSteps getNextSteps(TaskId taskId) {
        if (taskId == null) {
            return null;
        }
        
        switch (taskId) {
            case CANCEL_ORDER:
                return createNextStepsWithMetadata(
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
                return createNextStepsWithMetadata(
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
                return createNextStepsWithMetadata(
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
                return createNextStepsWithMetadata(
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
                return createNextStepsWithMetadata(
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
                return createNextStepsWithMetadata(
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
                return createNextStepsWithMetadata(
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
    
    private OperationalResponse.NextSteps createNextStepsWithMetadata(
            String description, String runbook, String apiSpec, String[] typicalSteps) {
        
        OperationalResponse.NextSteps.StepExecutionMetadata[] metadata = 
            new OperationalResponse.NextSteps.StepExecutionMetadata[typicalSteps.length];
        
        for (int i = 0; i < typicalSteps.length; i++) {
            String stepName = typicalSteps[i];
            boolean autoExecutable = isAutoExecutable(stepName);
            boolean requiresApproval = requiresApproval(stepName);
            String stepType = determineStepType(stepName);
            String apiEndpoint = getApiEndpointForStep(stepName, stepType);
            String httpMethod = getHttpMethodForStep(stepName, stepType);
            Map<String, Object> apiParameters = getApiParametersForStep(stepName, stepType);
            
            metadata[i] = new OperationalResponse.NextSteps.StepExecutionMetadata(
                stepName, autoExecutable, requiresApproval, stepType, apiEndpoint, httpMethod, apiParameters
            );
        }
        
        return new OperationalResponse.NextSteps(description, runbook, apiSpec, typicalSteps, metadata);
    }
    
    private String getApiEndpointForStep(String stepName, String stepType) {
        String lower = stepName.toLowerCase();
        if (stepType.equals("VALIDATION")) {
            if (lower.contains("case")) {
                return "/api/v2/cases/{case_id}/status";
            } else if (lower.contains("order")) {
                return "/api/v2/orders/{order_id}/status";
            }
        } else if (stepType.equals("PERMISSION_CHECK")) {
            return "/api/v2/users/{user_id}/roles";
        } else if (stepType.equals("API_EXECUTION")) {
            if (lower.contains("cancel") && lower.contains("case")) {
                return "/api/v2/cases/{case_id}/cancel";
            } else if (lower.contains("update") && lower.contains("case")) {
                return "/api/v2/cases/{case_id}/status";
            } else if (lower.contains("cancel") && lower.contains("order")) {
                return "/api/v2/orders/{order_id}/cancel";
            }
        } else if (stepType.equals("VERIFICATION")) {
            if (lower.contains("case")) {
                return "/api/v2/cases/{case_id}/status";
            } else if (lower.contains("order")) {
                return "/api/v2/orders/{order_id}/status";
            }
        }
        return null;
    }
    
    private String getHttpMethodForStep(String stepName, String stepType) {
        if (stepType.equals("API_EXECUTION")) {
            String lower = stepName.toLowerCase();
            if (lower.contains("cancel")) {
                return "POST";
            } else if (lower.contains("update")) {
                return "PATCH";
            }
        }
        return "GET";
    }
    
    private Map<String, Object> getApiParametersForStep(String stepName, String stepType) {
        Map<String, Object> params = new java.util.HashMap<>();
        String lower = stepName.toLowerCase();
        
        if (stepType.equals("API_EXECUTION")) {
            if (lower.contains("cancel")) {
                params.put("reason", "operational_request");
                params.put("notify_stakeholders", true);
            } else if (lower.contains("update") && lower.contains("status")) {
                params.put("action", "update_status");
            }
        }
        
        return params;
    }
    
    private String[] extractStepsFromLLMResponse(String llmResponse, TaskId taskId) {
        // Simple extraction of numbered steps from LLM response
        String[] lines = llmResponse.split("\n");
        java.util.List<String> steps = new java.util.ArrayList<>();
        
        for (String line : lines) {
            if (line.matches("\\d+\\.\\s+.*")) {
                steps.add(line.trim());
            }
        }
        
        if (steps.isEmpty()) {
            // Fallback to default steps
            return getNextSteps(taskId).getTypicalSteps();
        }
        
        return steps.toArray(new String[0]);
    }
}
