/*
 * Copyright 2025 the original author or authors.
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
package com.alibaba.cloud.ai.manus.adapter.controller;

import com.alibaba.cloud.ai.manus.adapter.model.OpenAIRequest;
import com.alibaba.cloud.ai.manus.adapter.model.OpenAIResponse;
import com.alibaba.cloud.ai.manus.adapter.service.OpenAIAdapterService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

/**
 * OpenAI Compatible API Controller
 *
 * Provides OpenAI-compatible endpoints for external clients like Cherry Studio. Supports
 * both streaming and non-streaming responses.
 *
 * @author JManus Team
 * @version 1.0
 */
@RestController
@RequestMapping("/")
@CrossOrigin(origins = "*", maxAge = 3600)
public class OpenAICompatibleController {

	private static final Logger logger = LoggerFactory.getLogger(OpenAICompatibleController.class);

	private static final long STREAMING_TIMEOUT_MS = 3 * 60 * 1000; // 3 minutes

	private static final int POLLING_INTERVAL_MS = 100;

	private static final String JMANUS_MODEL_ID = "jmanus-1.0";

	private static final String JMANUS_OWNER = "jmanus";

	private static final String CHAT_COMPLETION_CHUNK = "chat.completion.chunk";

	private static final String CONTENT_TYPE_TEXT_PLAIN = "text/plain; charset=utf-8";

	private static final String CONTENT_TYPE_JSON = "application/json; charset=utf-8";

	private static final String STREAMING_DONE_MARKER = "data: [DONE]\n\n";

	private final OpenAIAdapterService adapterService;

	private final ObjectMapper objectMapper;

	@Autowired
	public OpenAICompatibleController(OpenAIAdapterService adapterService) {
		this.adapterService = adapterService;
		this.objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
	}

	/**
	 * Handle chat completions - main OpenAI compatible endpoint
	 */
	@PostMapping("/v1/chat/completions")
	public ResponseEntity<?> chatCompletions(@RequestBody OpenAIRequest request) {
		String requestId = "req_" + System.currentTimeMillis();

		try {
			logger.info("[{}] OpenAI chat request: model={}, messages={}, stream={}", requestId, request.getModel(),
					request.getMessages() != null ? request.getMessages().size() : 0, request.getStream());

			if (request.getMessages() != null) {
				for (int i = 0; i < request.getMessages().size(); i++) {
					OpenAIRequest.Message msg = request.getMessages().get(i);
					logger.debug("[{}] Message[{}]: role={}, content_type={}, content_preview={}", requestId, i,
							msg.getRole(),
							msg.getRawContent() != null ? msg.getRawContent().getClass().getSimpleName() : "null",
							truncateString(msg.getContent(), 100));
				}
			}

			if (!isValidRequest(request)) {
				logger.warn("[{}] Invalid request", requestId);
				return ResponseEntity.badRequest().build();
			}

			return Boolean.TRUE.equals(request.getStream()) ? handleTrueStreamingRequest(request, requestId)
					: handleNonStreamingRequest(request, requestId);

		}
		catch (Exception e) {
			logger.error("[{}] Request failed", requestId, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	/**
	 * Handle streaming request - OpenAI compatible format for Cherry Studio
	 */
	private ResponseEntity<String> handleTrueStreamingRequest(OpenAIRequest request, String requestId) {
		logger.info("[{}] Starting OpenAI compatible streaming request", requestId);

		try {
			// Use StringBuilder to collect streaming response in correct format
			StringBuilder streamResponse = new StringBuilder();
			final boolean[] completed = { false };
			final String[] error = { null };

			// Process with JManus
			adapterService.processChatCompletionStream(request, new OpenAIAdapterService.StreamResponseHandler() {
				@Override
				public void onResponse(OpenAIResponse response) {
					String chunkJson = convertToOpenAIChunk(response);
					streamResponse.append("data: ").append(chunkJson).append("\n\n");
					logger.debug("[{}] Added OpenAI chunk", requestId);
				}

				@Override
				public void onError(String errorMessage) {
					logger.error("[{}] Streaming error: {}", requestId, errorMessage);
					error[0] = errorMessage;
					completed[0] = true;
				}

				@Override
				public void onComplete() {
					logger.info("[{}] JManus execution completed", requestId);
					streamResponse.append(STREAMING_DONE_MARKER);
					completed[0] = true;
				}
			});

			// Wait for completion (blocking to ensure complete response)
			long startTime = System.currentTimeMillis();
			while (!completed[0] && (System.currentTimeMillis() - startTime) < STREAMING_TIMEOUT_MS) {
				Thread.sleep(POLLING_INTERVAL_MS);
			}

			if (error[0] != null) {
				// Return error in OpenAI format
				String errorChunk = createOpenAIErrorChunk(error[0]);
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.header("Content-Type", CONTENT_TYPE_TEXT_PLAIN)
					.body("data: " + errorChunk + "\n\ndata: [DONE]\n\n");
			}

			String finalResponse = streamResponse.toString();
			logger.info("[{}] Returning OpenAI compatible streaming response, chunks: {}", requestId,
					finalResponse.split("data: ").length - 1);

			return ResponseEntity.ok()
				.headers(createStreamingHeaders())
				.header("Access-Control-Allow-Origin", "*")
				.body(finalResponse);

		}
		catch (Exception e) {
			logger.error("[{}] Streaming request failed", requestId, e);
			String errorChunk = createOpenAIErrorChunk("Internal server error");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.headers(createStreamingHeaders())
				.body("data: " + errorChunk + "\n\ndata: [DONE]\n\n");
		}
	}

	/**
	 * Convert OpenAIResponse to OpenAI streaming chunk format
	 */
	private String convertToOpenAIChunk(OpenAIResponse response) {
		try {
			// Convert regular OpenAIResponse to streaming chunk format
			Map<String, Object> chunk = new HashMap<>();
			chunk.put("id", response.getId());
			chunk.put("object", CHAT_COMPLETION_CHUNK);
			chunk.put("created", response.getCreated());
			chunk.put("model", response.getModel());

			// Convert choices to delta format
			List<Map<String, Object>> choices = new ArrayList<>();
			if (response.getChoices() != null && !response.getChoices().isEmpty()) {
				OpenAIResponse.Choice choice = response.getChoices().get(0);
				Map<String, Object> chunkChoice = new HashMap<>();
				chunkChoice.put("index", 0);

				// Create delta object
				Map<String, Object> delta = new HashMap<>();
				if (choice.getMessage() != null && choice.getMessage().getContent() != null) {
					delta.put("content", choice.getMessage().getContent());
				}
				if (choice.getDelta() != null && choice.getDelta().getContent() != null) {
					delta.put("content", choice.getDelta().getContent());
				}

				chunkChoice.put("delta", delta);
				chunkChoice.put("finish_reason", choice.getFinishReason());
				choices.add(chunkChoice);
			}
			chunk.put("choices", choices);

			return objectMapper.writeValueAsString(chunk);
		}
		catch (Exception e) {
			logger.error("Error converting response to OpenAI chunk format", e);
			return "{\"error\":\"Failed to serialize response\"}";
		}
	}

	/**
	 * Create error chunk in OpenAI format
	 */
	private String createOpenAIErrorChunk(String error) {
		try {
			Map<String, Object> errorChunk = Map.of("id", "error-" + System.currentTimeMillis(), "object",
					"chat.completion.chunk", "created", Instant.now().getEpochSecond(), "model", JMANUS_MODEL_ID,
					"choices", List.of(Map.of("index", 0, "delta", Map.of("content", "Error: " + error),
							"finish_reason", "stop")));
			return objectMapper.writeValueAsString(errorChunk);
		}
		catch (Exception e) {
			logger.error("Error creating OpenAI error chunk", e);
			return "{\"error\":\"Failed to create error response\"}";
		}
	}

	private ResponseEntity<String> handleNonStreamingRequest(OpenAIRequest request, String requestId) {
		try {
			OpenAIResponse response = adapterService.processChatCompletion(request);
			logger.info("[{}] Non-streaming request completed: id={}", requestId, response.getId());

			String jsonResponse = convertToJson(response);
			return ResponseEntity.ok()
				.headers(createJsonHeaders())
				.header("Access-Control-Allow-Origin", "*")
				.body(jsonResponse);
		}
		catch (Exception e) {
			logger.error("[{}] Non-streaming request failed", requestId, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	private String convertToJson(OpenAIResponse response) {
		try {
			return objectMapper.writeValueAsString(response);
		}
		catch (Exception e) {
			logger.error("JSON conversion error", e);
			return "{\"error\":\"JSON conversion failed\"}";
		}
	}

	/**
	 * List available models
	 */
	@GetMapping("/v1/models")
	public ResponseEntity<Map<String, Object>> listModels() {
		logger.info("Models list requested");
		try {
			return ResponseEntity.ok(createModelsResponse());
		}
		catch (Exception e) {
			logger.error("Error creating models response", e);
			return ResponseEntity.status(500)
				.body(Map.of("error", "Internal server error", "message",
						e.getMessage() != null ? e.getMessage() : "Unknown error"));
		}
	}

	/**
	 * Health check endpoint
	 */
	@GetMapping("/v1/health")
	public ResponseEntity<Map<String, Object>> health() {
		logger.info("Health check requested");
		return ResponseEntity.ok(Map.of("status", "healthy", "service", "JManus OpenAI Compatible API", "timestamp",
				Instant.now().getEpochSecond(), "model", JMANUS_MODEL_ID));
	}

	// ==================== Helper Methods ====================

	private boolean isValidRequest(OpenAIRequest request) {
		return request != null && request.getMessages() != null && !request.getMessages().isEmpty()
				&& request.getMessages().stream().allMatch(this::isValidMessage);
	}

	private boolean isValidMessage(OpenAIRequest.Message msg) {
		return msg.getRole() != null && msg.getContent() != null && !msg.getContent().trim().isEmpty();
	}

	private Map<String, Object> createModelsResponse() {
		long currentTime = Instant.now().getEpochSecond();

		Map<String, Object> modelData = new HashMap<>();
		modelData.put("id", JMANUS_MODEL_ID);
		modelData.put("object", "model");
		modelData.put("created", currentTime);
		modelData.put("owned_by", JMANUS_OWNER);
		modelData.put("root", JMANUS_MODEL_ID);
		modelData.put("parent", null);

		return Map.of("object", "list", "data", List.of(modelData));
	}

	/**
	 * Create headers for streaming responses
	 */
	private HttpHeaders createStreamingHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.set("Content-Type", CONTENT_TYPE_TEXT_PLAIN);
		headers.set("Cache-Control", "no-cache");
		headers.set("Connection", "keep-alive");
		return headers;
	}

	/**
	 * Create headers for JSON responses
	 */
	private HttpHeaders createJsonHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.set("Content-Type", CONTENT_TYPE_JSON);
		return headers;
	}

	/**
	 * Truncating strings for log display
	 */
	private String truncateString(String str, int maxLength) {
		if (str == null)
			return "null";
		if (str.length() <= maxLength)
			return str;
		return str.substring(0, maxLength) + "...";
	}

}
