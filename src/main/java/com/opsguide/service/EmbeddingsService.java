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
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Service
public class EmbeddingsService {
    
    @Value("${aws.bedrock.region:us-east-1}")
    private String region;
    
    @Value("${aws.bedrock.embeddings-model:amazon.titan-embed-text-v1}")
    private String modelId;
    
    private BedrockRuntimeClient bedrockClient;
    private final ObjectMapper objectMapper;
    private final Executor asyncExecutor;
    
    public EmbeddingsService() {
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
    
    public List<Float> generateEmbedding(String text) {
        try {
            // For now, return mock embedding for development
            return generateMockEmbedding(text);
            
            // TODO: Implement actual Bedrock Titan call
            /*
            String requestBody = objectMapper.writeValueAsString(Map.of(
                "inputText", text,
                "dimensions", 1536,
                "normalize", true
            ));
            
            InvokeModelRequest request = InvokeModelRequest.builder()
                .modelId(modelId)
                .contentType("application/json")
                .accept("application/json")
                .body(SdkBytes.fromUtf8String(requestBody))
                .build();
                
            InvokeModelResponse response = bedrockClient.invokeModel(request);
            String responseBody = response.body().asUtf8String();
            
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            JsonNode embeddingNode = jsonNode.get("embedding");
            
            List<Float> embedding = new ArrayList<>();
            for (JsonNode value : embeddingNode) {
                embedding.add(value.floatValue());
            }
            
            return embedding;
            */
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate embedding: " + e.getMessage(), e);
        }
    }
    
    public CompletableFuture<List<Float>> generateEmbeddingAsync(String text) {
        return CompletableFuture.supplyAsync(() -> generateEmbedding(text), asyncExecutor);
    }
    
    public List<List<Float>> generateBatchEmbeddings(List<String> texts) {
        List<List<Float>> embeddings = new ArrayList<>();
        for (String text : texts) {
            embeddings.add(generateEmbedding(text));
        }
        return embeddings;
    }
    
    private List<Float> generateMockEmbedding(String text) {
        // Generate mock 1536-dimensional embedding using text hash
        List<Float> embedding = new ArrayList<>();
        int hash = text.hashCode();
        
        for (int i = 0; i < 1536; i++) {
            // Use hash with different multipliers to generate pseudo-random values
            float value = (float) Math.sin(hash * (i + 1) * 0.001) * 0.5f;
            embedding.add(value);
        }
        
        return embedding;
    }
}
