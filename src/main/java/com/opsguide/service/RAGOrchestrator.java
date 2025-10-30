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
        
        return new OperationalResponse.NextSteps(description, runbook, apiSpec, typicalSteps);
    }
    
    private OperationalResponse.NextSteps getNextSteps(TaskId taskId) {
        if (taskId == null) {
            return null;
        }
        
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
