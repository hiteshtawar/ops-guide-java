package com.opsguide.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;
import software.amazon.awssdk.core.SdkBytes;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Service
public class LLMService {
    
    @Value("${aws.bedrock.region:us-east-1}")
    private String region;
    
    @Value("${aws.bedrock.claude-model:anthropic.claude-3-sonnet-20240229-v1:0}")
    private String modelId;
    
    @Value("${aws.bedrock.max-tokens:4000}")
    private int maxTokens;
    
    @Value("${aws.bedrock.temperature:0.1}")
    private double temperature;
    
    private BedrockRuntimeClient bedrockClient;
    private final ObjectMapper objectMapper;
    private final Executor asyncExecutor;
    
    public LLMService() {
        this.objectMapper = new ObjectMapper();
        this.asyncExecutor = Executors.newFixedThreadPool(10);
    }
    
    private BedrockRuntimeClient getBedrockClient() {
        if (bedrockClient == null) {
            this.bedrockClient = BedrockRuntimeClient.builder()
                .region(software.amazon.awssdk.regions.Region.of(region))
                .build();
        }
        return bedrockClient;
    }
    
    public String generateResponse(String prompt) {
        try {
            // For now, return mock response for development
            return generateMockResponse(prompt);
            
            // TODO: Implement actual Bedrock Claude call
            /*
            Map<String, Object> requestBody = Map.of(
                "anthropic_version", "bedrock-2023-05-31",
                "max_tokens", maxTokens,
                "messages", List.of(
                    Map.of(
                        "role", "user",
                        "content", prompt
                    )
                ),
                "temperature", temperature,
                "top_p", 0.9
            );
            
            InvokeModelRequest request = InvokeModelRequest.builder()
                .modelId(modelId)
                .contentType("application/json")
                .accept("application/json")
                .body(SdkBytes.fromUtf8String(objectMapper.writeValueAsString(requestBody)))
                .build();
                
            InvokeModelResponse response = bedrockClient.invokeModel(request);
            String responseBody = response.body().asUtf8String();
            
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            return jsonNode.get("content").get(0).get("text").asText();
            */
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate LLM response: " + e.getMessage(), e);
        }
    }
    
    public CompletableFuture<String> generateResponseAsync(String prompt) {
        return CompletableFuture.supplyAsync(() -> generateResponse(prompt), asyncExecutor);
    }
    
    public String generateResponseWithContext(String prompt, List<VectorSearchService.KnowledgeChunk> knowledgeChunks) {
        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("Context from knowledge base:\n");
        
        for (VectorSearchService.KnowledgeChunk chunk : knowledgeChunks) {
            contextBuilder.append("Source: ").append(chunk.getSource()).append("\n");
            contextBuilder.append("Content: ").append(chunk.getContent()).append("\n\n");
        }
        
        contextBuilder.append("User Query: ").append(prompt);
        
        return generateResponse(contextBuilder.toString());
    }
    
    private String generateMockResponse(String prompt) {
        if (prompt.toLowerCase().contains("cancel") && prompt.toLowerCase().contains("case")) {
            return "Based on the knowledge base, to cancel a case:\n\n" +
                   "1. Verify the case exists and is in a cancellable state (pending, in_progress, under_review, on_hold)\n" +
                   "2. Check for active dependencies using GET /api/v2/cases/{case_id}/dependencies\n" +
                   "3. Execute cancellation via POST /api/v2/cases/{case_id}/cancel with proper authorization\n" +
                   "4. Monitor the cancellation status using GET /api/v2/cases/{case_id}/cancel/status\n\n" +
                   "Risk Level: Medium - Related orders may be affected and customer notifications will be triggered.";
        } else if (prompt.toLowerCase().contains("status") && prompt.toLowerCase().contains("case")) {
            return "Based on the knowledge base, to update case status:\n\n" +
                   "1. Verify the case exists using GET /api/v2/cases/{case_id}\n" +
                   "2. Check that the status transition is valid (accessioning → grossing → embedding → cutting → staining → microscopy → under_review → completed)\n" +
                   "3. Update status via PATCH /api/v2/cases/{case_id}/status with required artifacts\n" +
                   "4. Verify the status change was applied successfully\n\n" +
                   "Valid statuses: accessioning, grossing, embedding, cutting, staining, microscopy, under_review, on_hold, completed, cancelled, archived, closed.";
        } else {
            return "I can help you with case management operations. Please specify whether you want to cancel a case or update case status, and provide the case ID.";
        }
    }
}
