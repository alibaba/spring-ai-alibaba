/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.memory.mem0.core;

import com.alibaba.cloud.ai.memory.mem0.config.Mem0ChatMemoryProperties;
import com.alibaba.cloud.ai.memory.mem0.model.Mem0ServerRequest;
import com.alibaba.cloud.ai.memory.mem0.model.Mem0ServerResp;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/**
 * Mem0 API Client Implementation
 *
 * Directly calls the Mem0 REST API interface. Reference documentation:
 * http://localhost:8888/docs
 */
public class Mem0ServiceClient {

	private static final Logger logger = LoggerFactory.getLogger(Mem0ServiceClient.class);

	private final WebClient webClient;

	private final ObjectMapper objectMapper;

	private final Mem0ChatMemoryProperties config;

	private final ResourceLoader resourceLoader;

	// Mem0 API endpoint
	private static final String CONFIGURE_ENDPOINT = "/configure";

	private static final String MEMORIES_ENDPOINT = "/memories";

	private static final String SEARCH_ENDPOINT = "/search";

	private static final String RESET_ENDPOINT = "/reset";

	/**
	 * Constructor
	 */
	public Mem0ServiceClient(Mem0ChatMemoryProperties config, ResourceLoader resourceLoader) {
		this.config = config;
		this.resourceLoader = resourceLoader;
		this.objectMapper = new ObjectMapper();
		// JSON key serialization using snake_case
		this.objectMapper.setPropertyNamingStrategy(com.fasterxml.jackson.databind.PropertyNamingStrategies.SNAKE_CASE);
		// Ignore null values and empty collections
		this.objectMapper.registerModule(new JavaTimeModule())
			.setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY);

		// Create WebClient to connect to Mem0 API
		this.webClient = WebClient.builder()
			.baseUrl(config.getClient().getBaseUrl())
			.defaultHeader("Content-Type", "application/json")
			.build();
	}

	/**
	 * Configures Mem0
	 */
	public void configure(Mem0ChatMemoryProperties.Server config) {
		try {
			if (Objects.nonNull(config.getProject())) {
				config.getProject().setCustomInstructions(this.loadPrompt(config.getProject().getCustomInstructions()));
				config.getProject().setCustomCategories(this.loadPrompt(config.getProject().getCustomCategories()));
			}
			if (Objects.nonNull(config.getVectorStore())) {
				config.getGraphStore().setCustomPrompt(this.loadPrompt(config.getGraphStore().getCustomPrompt()));
			}
			config.setCustomFactExtractionPrompt(this.loadPrompt(config.getCustomFactExtractionPrompt()));
			config.setCustomUpdateMemoryPrompt(this.loadPrompt(config.getCustomUpdateMemoryPrompt()));

			String requestJson = objectMapper.writeValueAsString(config);
			String response = webClient.post()
				.uri(CONFIGURE_ENDPOINT)
				.contentType(MediaType.APPLICATION_JSON)
				.body(BodyInserters.fromValue(requestJson))
				.retrieve()
				.bodyToMono(String.class)
				.timeout(Duration.ofSeconds(this.config.getClient().getTimeoutSeconds()))
				.block();
			if (StringUtils.hasText(response) && response.contains("successfully")) {
				logger.info("Mem0 configuration updated successfully");
			}
			else {
				logger.error("Failed to configure Mem0: {}", response);
				throw new RuntimeException("Failed to configure Mem0");
			}
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
		catch (Exception e) {
			logger.error("Failed to configure Mem0: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to configure Mem0", e);
		}
	}

	/**
	 * Add memory
	 */
	public void addMemory(Mem0ServerRequest.MemoryCreate memoryCreate) {
		try {
			// Add debugging information
			String requestJson = objectMapper.writeValueAsString(memoryCreate);

			String response = webClient.post()
				.uri(MEMORIES_ENDPOINT)
				.contentType(MediaType.APPLICATION_JSON)
				.body(BodyInserters.fromValue(requestJson))
				.retrieve()
				.bodyToMono(String.class)
				.timeout(Duration.ofSeconds(config.getClient().getTimeoutSeconds()))
				.retry(config.getClient().getMaxRetryAttempts())
				.block();

			if (response != null) {
				Map<String, Object> result = objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {
				});
				logger.info("Successfully added memory with {} messages", memoryCreate.getMessages().size());
			}
		}
		catch (WebClientResponseException e) {
			String errorBody = e.getResponseBodyAsString();
			logger.error("HTTP error adding memory: {} - {}", e.getStatusCode(), errorBody, e);
			throw new RuntimeException("Failed to add memory: " + errorBody, e);
		}
		catch (Exception e) {
			logger.error("UNKNOWN error adding memory: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to add memory", e);
		}

	}

	/**
	 * Get all memory
	 */
	public Mem0ServerResp getAllMemories(String userId, String runId, String agentId) {
		try {
			String response = webClient.get().uri(uriBuilder -> {
				uriBuilder.path(MEMORIES_ENDPOINT);
				if (userId != null)
					uriBuilder.queryParam("user_id", userId);
				if (runId != null)
					uriBuilder.queryParam("run_id", runId);
				if (agentId != null)
					uriBuilder.queryParam("agent_id", agentId);
				return uriBuilder.build();
			})
				.retrieve()
				.bodyToMono(String.class)
				.timeout(Duration.ofSeconds(config.getClient().getTimeoutSeconds()))
				.retry(config.getClient().getMaxRetryAttempts())
				.block();

			if (response != null) {
				// Mem0 service returns data in the format {"results":[],"relations":[]}
				return objectMapper.readValue(response, new TypeReference<Mem0ServerResp>() {
				});
			}
		}
		catch (Exception e) {
			logger.error("Failed to get memories: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to get memories", e);
		}

		return new Mem0ServerResp();
	}

	/**
	 * Get single memory
	 */
	public Mem0ServerResp getMemory(String memoryId) {
		try {
			String response = webClient.get()
				.uri(MEMORIES_ENDPOINT + "/{memoryId}", memoryId)
				.retrieve()
				.bodyToMono(String.class)
				.timeout(Duration.ofSeconds(config.getClient().getTimeoutSeconds()))
				.retry(config.getClient().getMaxRetryAttempts())
				.block();

			if (response != null) {
				Mem0ServerResp memory = objectMapper.readValue(response, Mem0ServerResp.class);
				logger.info("Retrieved memory: {}", memoryId);
				return memory;
			}
		}
		catch (Exception e) {
			logger.error("Failed to get memory {}: {}", memoryId, e.getMessage(), e);
			throw new RuntimeException("Failed to get memory " + memoryId, e);
		}

		return null;
	}

	/**
	 * Search memory
	 */
	public Mem0ServerResp searchMemories(Mem0ServerRequest.SearchRequest searchRequest) {
		try {
			// The SEARCH_ENDPOINT requires the query field to have a value, so a fallback
			// mechanism is implemented
			if (!StringUtils.hasText(searchRequest.getQuery())) {
				return getAllMemories(searchRequest.getUserId(), searchRequest.getRunId(), searchRequest.getAgentId());
			}

			// Add debug logging
			String requestJson = objectMapper.writeValueAsString(searchRequest);
			logger.info("Sending search request to Mem0: {}", requestJson);

			String response = webClient.post()
				.uri(SEARCH_ENDPOINT)
				.contentType(MediaType.APPLICATION_JSON)
				.body(BodyInserters.fromValue(requestJson))
				.retrieve()
				.bodyToMono(String.class)
				.timeout(Duration.ofSeconds(config.getClient().getTimeoutSeconds()))
				.retry(config.getClient().getMaxRetryAttempts())
				.block();

			if (response != null) {
				logger.info("Received response from Mem0: " + response);
				// The Mem0 service returns data in the format
				// {"results":[],"relations":[]}
				return objectMapper.readValue(response, new TypeReference<Mem0ServerResp>() {
				});

			}
		}
		catch (Exception e) {
			logger.error("Failed to search memories: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to search memories", e);
		}

		return new Mem0ServerResp();
	}

	/**
	 * Update memory
	 */
	public Map<String, Object> updateMemory(String memoryId, Map<String, Object> updatedMemory) {
		try {
			String response = webClient.put()
				.uri(MEMORIES_ENDPOINT + "/{memoryId}", memoryId)
				.bodyValue(updatedMemory)
				.retrieve()
				.bodyToMono(String.class)
				.timeout(Duration.ofSeconds(config.getClient().getTimeoutSeconds()))
				.retry(config.getClient().getMaxRetryAttempts())
				.block();

			if (response != null) {
				Map<String, Object> result = objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {
				});
				logger.info("Successfully updated memory: " + memoryId);
				return result;
			}
		}
		catch (Exception e) {
			logger.error("Failed to update memory {}: {}", memoryId, e.getMessage(), e);
			throw new RuntimeException("Failed to update memory", e);
		}

		return new HashMap<>();
	}

	/**
	 * Get memory history
	 */
	public List<Map<String, Object>> getMemoryHistory(String memoryId) {
		try {
			String response = webClient.get()
				.uri(MEMORIES_ENDPOINT + "/{memoryId}/history", memoryId)
				.retrieve()
				.bodyToMono(String.class)
				.timeout(Duration.ofSeconds(config.getClient().getTimeoutSeconds()))
				.block();

			if (response != null) {
				// Attempt to parse as an object and then extract the array
				Map<String, Object> responseMap = objectMapper.readValue(response,
						new TypeReference<Map<String, Object>>() {
						});

				// Check if there is a "data" field containing an array
				if (responseMap.containsKey("data")) {
					Object data = responseMap.get("data");
					if (data instanceof List) {
						List<Map<String, Object>> history = objectMapper.convertValue(data,
								new TypeReference<List<Map<String, Object>>>() {
								});
						return history;
					}
				}

				// If there is no "data" field, attempt to parse directly as an array
				try {
					List<Map<String, Object>> history = objectMapper.readValue(response,
							new TypeReference<List<Map<String, Object>>>() {
							});
					logger.info("Retrieved history for memory: {}", memoryId);
					return history;
				}
				catch (Exception e) {
					logger.error("Failed to parse history response as array, trying as object: {}", e.getMessage());
				}

				// If all attempts fail, return an empty list.
				logger.warn("Could not parse memory history from response: {}", response);
				return new ArrayList<>();
			}
		}
		catch (Exception e) {
			logger.error("Failed to get memory history {}: {}", memoryId, e.getMessage(), e);
			throw new RuntimeException("Failed to get memory history", e);
		}

		return new ArrayList<>();
	}

	/**
	 * Delete single memory
	 */
	public void deleteMemory(String memoryId) {
		try {
			webClient.delete()
				.uri(MEMORIES_ENDPOINT + "/{memoryId}", memoryId)
				.retrieve()
				.bodyToMono(String.class)
				.timeout(Duration.ofSeconds(config.getClient().getTimeoutSeconds()))
				.block();

			logger.info("Successfully deleted memory: {}", memoryId);
		}
		catch (Exception e) {
			logger.error("Failed to delete memory {}: {}", memoryId, e.getMessage(), e);
			throw new RuntimeException("Failed to delete memory", e);
		}
	}

	/**
	 * Delete all memory
	 */
	public void deleteAllMemories(String userId, String runId, String agentId) {
		try {
			webClient.delete().uri(uriBuilder -> {
				uriBuilder.path(MEMORIES_ENDPOINT);
				if (userId != null)
					uriBuilder.queryParam("user_id", userId);
				if (runId != null)
					uriBuilder.queryParam("run_id", runId);
				if (agentId != null)
					uriBuilder.queryParam("agent_id", agentId);
				return uriBuilder.build();
			})
				.retrieve()
				.bodyToMono(String.class)
				.timeout(Duration.ofSeconds(config.getClient().getTimeoutSeconds()))
				.block();

			logger.info("Successfully deleted all memories");
		}
		catch (Exception e) {
			logger.error("Failed to delete all memories: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to delete all memories", e);
		}
	}

	/**
	 * Reset all memory
	 */
	public void resetAllMemories() {
		try {
			webClient.post()
				.uri(RESET_ENDPOINT)
				.retrieve()
				.bodyToMono(String.class)
				.timeout(Duration.ofSeconds(config.getClient().getTimeoutSeconds()))
				.block();

			logger.info("Successfully reset all memories");
		}
		catch (Exception e) {
			logger.error("Failed to reset all memories: " + e.getMessage(), e);
			throw new RuntimeException("Failed to reset all memories", e);
		}
	}

	public String loadPrompt(String classPath) throws Exception {
		if (StringUtils.hasText(classPath)) {
			Resource resource = resourceLoader.getResource(classPath);
			if (!resource.exists()) {
				throw new IllegalArgumentException("Prompt resource not found: " + classPath);
			}
			// Read file content as a string
			return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		}
		return null;
	}

}
