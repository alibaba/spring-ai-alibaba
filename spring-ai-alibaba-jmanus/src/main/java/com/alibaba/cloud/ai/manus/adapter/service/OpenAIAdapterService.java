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
package com.alibaba.cloud.ai.manus.adapter.service;

import com.alibaba.cloud.ai.manus.adapter.model.OpenAIRequest;
import com.alibaba.cloud.ai.manus.adapter.model.OpenAIResponse;
import com.alibaba.cloud.ai.manus.planning.PlanningFactory;
import com.alibaba.cloud.ai.manus.recorder.entity.vo.PlanExecutionRecord;
import com.alibaba.cloud.ai.manus.recorder.service.PlanExecutionRecorder;
import com.alibaba.cloud.ai.manus.recorder.service.PlanHierarchyReaderService;
import com.alibaba.cloud.ai.manus.runtime.entity.vo.ExecutionContext;
import com.alibaba.cloud.ai.manus.runtime.service.PlanIdDispatcher;
import com.alibaba.cloud.ai.manus.runtime.service.PlanningCoordinator;

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * OpenAI Adapter Service Converts OpenAI format requests to JManus execution flow and
 * formats responses back to OpenAI format
 */
@Service
public class OpenAIAdapterService {

	private static final Logger logger = LoggerFactory.getLogger(OpenAIAdapterService.class);

	// Constants
	private static final String DEFAULT_MODEL = "jmanus-1.0";

	private static final String CHAT_COMPLETION_OBJECT = "chat.completion";

	private static final String CHAT_COMPLETION_CHUNK_OBJECT = "chat.completion.chunk";

	private static final String ASSISTANT_ROLE = "assistant";

	private static final String USER_ROLE = "user";

	private static final String FINISH_REASON_STOP = "stop";

	private static final String CHATCMPL_PREFIX = "chatcmpl-";

	private static final String ERROR_PREFIX = "chatcmpl-error-";

	private static final int PLAN_EXECUTION_TIMEOUT_MINUTES = 10;

	private static final int DATABASE_PERSISTENCE_DELAY_MS = 1000;

	private static final int TOKEN_ESTIMATION_RATIO = 4; // 4 chars = 1 token

	private static final int MEMORY_ID_LENGTH = 8;

	@Autowired
	private PlanningFactory planningFactory;

	@Autowired
	private PlanExecutionRecorder planExecutionRecorder;

	@Autowired
	private PlanIdDispatcher planIdDispatcher;

	@Autowired
	private PlanningCoordinator planningCoordinator;

	@Autowired
	private PlanHierarchyReaderService planHierarchyReaderService;

	/**
	 * Process OpenAI chat completion request and return response
	 */
	public OpenAIResponse processChatCompletion(OpenAIRequest request) {
		try {
			// Extract user message
			String userMessage = extractUserMessage(request);
			if (userMessage == null || userMessage.trim().isEmpty()) {
				return createErrorResponse("No user message found in request");
			}

			// Check if this is a health check or simple greeting
			if (isHealthCheckMessage(userMessage)) {
				logger.info("Processing health check message: {}", userMessage);
				return createHealthCheckResponse(request, userMessage);
			}

			// Prepare execution context for complex tasks
			ExecutionContext context = prepareExecutionContext(request);
			if (context == null) {
				return createErrorResponse("Failed to prepare execution context");
			}

			// Execute JManus plan synchronously
			String result = executePlan(context);

			// Convert result to OpenAI response format
			return createSuccessResponse(request, result, context.getCurrentPlanId());

		}
		catch (Exception e) {
			logger.error("Error processing OpenAI chat completion request", e);
			return createErrorResponse("Internal server error: " + e.getMessage());
		}
	}

	/**
	 * Process OpenAI streaming chat completion request
	 */
	public CompletableFuture<Void> processChatCompletionStream(OpenAIRequest request, StreamResponseHandler handler) {
		return CompletableFuture.runAsync(() -> {
			try {
				// Extract user message
				String userMessage = extractUserMessage(request);
				if (userMessage == null || userMessage.trim().isEmpty()) {
					handler.onError("No user message found in request");
					return;
				}

				// Check if this is a health check or simple greeting
				if (isHealthCheckMessage(userMessage)) {
					logger.info("Processing streaming health check message: {}", userMessage);
					handleHealthCheckStream(request, userMessage, handler);
					return;
				}

				// Prepare execution context for complex tasks
				ExecutionContext context = prepareExecutionContext(request);
				if (context == null) {
					handler.onError("Failed to prepare execution context");
					return;
				}

				// Execute plan with streaming
				executePlanWithStreaming(context, request, handler);

			}
			catch (Exception e) {
				logger.error("Error processing streaming chat completion", e);
				handler.onError("Internal server error: " + e.getMessage());
			}
		});
	}

	/**
	 * Prepare execution context from OpenAI request (common logic)
	 */
	private ExecutionContext prepareExecutionContext(OpenAIRequest request) {
		if (request == null) {
			logger.warn("Received null OpenAI request");
			return null;
		}

		// Extract user message from OpenAI request
		String userMessage = extractUserMessage(request);
		if (userMessage == null || userMessage.trim().isEmpty()) {
			logger.warn("No valid user message found in request with {} messages",
					request.getMessages() != null ? request.getMessages().size() : 0);
			return null;
		}

		// Validate message length
		if (userMessage.length() > 10000) { // Reasonable limit
			logger.warn("User message too long: {} characters, truncating", userMessage.length());
			userMessage = userMessage.substring(0, 10000) + "...";
		}

		// Generate plan ID and create execution context
		String planId = planIdDispatcher.generatePlanId();
		logger.debug("Preparing execution context - planId: {}, messageLength: {}", planId, userMessage.length());

		return createExecutionContext(userMessage, planId);
	}

	/**
	 * Extract user message from OpenAI request
	 */
	private String extractUserMessage(OpenAIRequest request) {
		if (request.getMessages() == null || request.getMessages().isEmpty()) {
			return null;
		}

		// Find the last user message
		for (int i = request.getMessages().size() - 1; i >= 0; i--) {
			OpenAIRequest.Message message = request.getMessages().get(i);
			if (USER_ROLE.equals(message.getRole()) && message.getContent() != null
					&& !message.getContent().trim().isEmpty()) {
				return message.getContent().trim();
			}
		}

		return null;
	}

	/**
	 * Create execution context for JManus
	 */
	private ExecutionContext createExecutionContext(String userMessage, String planId) {
		try {
			ExecutionContext context = new ExecutionContext();
			context.setUserRequest(userMessage);
			context.setCurrentPlanId(planId);
			context.setRootPlanId(planId);
			context.setMemoryId(RandomStringUtils.randomAlphabetic(MEMORY_ID_LENGTH));
			context.setNeedSummary(true);

			logger.debug("Created execution context for planId: {}, messageLength: {}", planId, userMessage.length());
			return context;
		}
		catch (Exception e) {
			logger.error("Failed to create execution context for planId: {}", planId, e);
			throw new RuntimeException("Failed to create execution context", e);
		}
	}

	/**
	 * Execute JManus plan synchronously
	 */
	private String executePlan(ExecutionContext context) throws Exception {
		try {
			// Execute the plan using PlanningCoordinator
			CompletableFuture<com.alibaba.cloud.ai.manus.runtime.entity.vo.PlanExecutionResult> future = planningCoordinator
				.executeByUserQuery(context.getUserRequest(), context.getCurrentPlanId(), context.getCurrentPlanId(),
						context.getCurrentPlanId(), context.getMemoryId(), null);

			// Wait for completion with extended timeout for complex tasks
			logger.info("Waiting for plan execution to complete for planId: {}", context.getCurrentPlanId());
			com.alibaba.cloud.ai.manus.runtime.entity.vo.PlanExecutionResult result = future
				.get(PLAN_EXECUTION_TIMEOUT_MINUTES, TimeUnit.MINUTES);
			logger.info("Plan execution completed for planId: {}", context.getCurrentPlanId());

			// Check if execution was successful
			if (!result.isSuccess()) {
				logger.warn("Plan execution failed for planId: {} - {}", context.getCurrentPlanId(),
						result.getErrorMessage());
				return "Task execution failed: " + result.getErrorMessage();
			}

			// Get execution record for more detailed result
			PlanExecutionRecord record = planHierarchyReaderService.readPlanTreeByRootId(context.getCurrentPlanId());
			if (record != null && record.getSummary() != null) {
				logger.info("Retrieved summary for planId {}: {}", context.getCurrentPlanId(),
						record.getSummary().substring(0, Math.min(100, record.getSummary().length())) + "...");
				return record.getSummary();
			}

			// Return a summary based on execution context
			logger.warn("No summary found for planId: {}, returning default message", context.getCurrentPlanId());
			return "Task completed successfully. Plan ID: " + context.getCurrentPlanId();

		}
		catch (Exception e) {
			String planId = context.getCurrentPlanId();
			logger.error("JManus execution failed for planId {}: {}", planId, e.getMessage(), e);

			// Log additional context for debugging
			logger.debug("Execution failure context - planId: {}, userRequest: {}, memoryId: {}", planId,
					context.getUserRequest(), context.getMemoryId());

			// Return a simple fallback response when LLM is not configured
			logger.info("Generating fallback response for planId {} due to execution failure", planId);
			return createSimpleFallbackResponse();
		}
	}

	/**
	 * Create simple fallback response when LLM service is not available
	 */
	private String createSimpleFallbackResponse() {
		return "JManus AI Assistant is ready to help! However, the LLM service is not configured yet. "
				+ "Please configure your LLM service (such as Alibaba Cloud DashScope) in the JManus settings "
				+ "to enable full AI capabilities including task planning, tool calling, and intelligent conversations.";
	}

	/**
	 * Execute plan with streaming updates
	 */
	private void executePlanWithStreaming(ExecutionContext context, OpenAIRequest request,
			StreamResponseHandler handler) {
		try {
			String planId = context.getCurrentPlanId();

			// Send initial streaming response
			handler.onResponse(createStreamStartResponse(request, planId));

			// Execute plan asynchronously
			CompletableFuture.runAsync(() -> {
				try {
					logger.info("Starting plan execution for planId: {}", planId);

					// Execute the plan using PlanningCoordinator
					CompletableFuture<com.alibaba.cloud.ai.manus.runtime.entity.vo.PlanExecutionResult> future = planningCoordinator
						.executeByUserQuery(context.getUserRequest(), planId, planId, planId, context.getMemoryId(),
								null);

					// Wait for completion
					com.alibaba.cloud.ai.manus.runtime.entity.vo.PlanExecutionResult result = future.get();

					logger.info("Plan execution completed for planId: {}", planId);

					// Check if execution was successful
					if (!result.isSuccess()) {
						logger.warn("Plan execution failed for planId: {} - {}", planId, result.getErrorMessage());
						handler.onError("Execution failed: " + result.getErrorMessage());
						return;
					}

					// Wait to ensure database persistence
					Thread.sleep(DATABASE_PERSISTENCE_DELAY_MS);

					// Get execution record for final result
					PlanExecutionRecord record = planHierarchyReaderService.readPlanTreeByRootId(planId);
					String finalResult;

					if (record != null && record.getSummary() != null && !record.getSummary().trim().isEmpty()) {
						finalResult = record.getSummary();
					}
					else {
						finalResult = "Task completed successfully. Plan ID: " + planId;
					}

					logger.info("Sending final result for planId: {} - {}", planId, finalResult);

					// Send final result chunk
					OpenAIResponse finalResponse = createStreamEndResponse(request, finalResult, planId);
					handler.onResponse(finalResponse);

					// Complete the stream
					handler.onComplete();

				}
				catch (Exception e) {
					logger.error("Error executing plan for planId: " + planId, e);
					handler.onError("Execution failed: " + e.getMessage());
				}
			});

		}
		catch (Exception e) {
			logger.error("Failed to start plan execution", e);
			handler.onError("Failed to start plan execution: " + e.getMessage());
		}
	}

	/**
	 * Create success response in OpenAI format
	 */
	private OpenAIResponse createSuccessResponse(OpenAIRequest request, String content, String planId) {
		OpenAIResponse response = createBaseResponse(CHATCMPL_PREFIX + planId, CHAT_COMPLETION_OBJECT, request);

		// Create choice with message
		OpenAIResponse.Choice choice = createChoice(0, FINISH_REASON_STOP);
		choice.setMessage(createMessage(ASSISTANT_ROLE, content));
		response.setChoices(Arrays.asList(choice));

		// Create usage info
		response.setUsage(createUsageInfo(request, content));
		return response;
	}

	/**
	 * Create error response in OpenAI format
	 */
	private OpenAIResponse createErrorResponse(String error) {
		OpenAIResponse response = createBaseResponse(ERROR_PREFIX + System.currentTimeMillis(), CHAT_COMPLETION_OBJECT,
				null);

		OpenAIResponse.Choice choice = createChoice(0, FINISH_REASON_STOP);
		choice.setMessage(createMessage(ASSISTANT_ROLE, "Error: " + error));
		response.setChoices(Arrays.asList(choice));

		return response;
	}

	/**
	 * Create streaming start response
	 */
	private OpenAIResponse createStreamStartResponse(OpenAIRequest request, String planId) {
		OpenAIResponse response = createBaseResponse(CHATCMPL_PREFIX + planId, CHAT_COMPLETION_CHUNK_OBJECT, request);

		OpenAIResponse.Choice choice = createChoice(0, null);
		choice.setDelta(createDelta(ASSISTANT_ROLE, ""));
		response.setChoices(Arrays.asList(choice));

		return response;
	}

	/**
	 * Create streaming end response
	 */
	private OpenAIResponse createStreamEndResponse(OpenAIRequest request, String finalContent, String planId) {
		OpenAIResponse response = createBaseResponse(CHATCMPL_PREFIX + planId, CHAT_COMPLETION_CHUNK_OBJECT, request);

		OpenAIResponse.Choice choice = createChoice(0, FINISH_REASON_STOP);
		choice.setDelta(createDelta(null, finalContent));
		response.setChoices(Arrays.asList(choice));

		return response;
	}

	/**
	 * Create base OpenAI response
	 */
	private OpenAIResponse createBaseResponse(String id, String object, OpenAIRequest request) {
		OpenAIResponse response = new OpenAIResponse();
		response.setId(id);
		response.setObject(object);
		response.setCreated(Instant.now().getEpochSecond());
		response.setModel(request != null && request.getModel() != null ? request.getModel() : DEFAULT_MODEL);
		return response;
	}

	/**
	 * Create choice object
	 */
	private OpenAIResponse.Choice createChoice(int index, String finishReason) {
		OpenAIResponse.Choice choice = new OpenAIResponse.Choice();
		choice.setIndex(index);
		choice.setFinishReason(finishReason);
		return choice;
	}

	/**
	 * Create message object
	 */
	private OpenAIResponse.Message createMessage(String role, String content) {
		OpenAIResponse.Message message = new OpenAIResponse.Message();
		message.setRole(role);
		message.setContent(content);
		return message;
	}

	/**
	 * Create delta object for streaming
	 */
	private OpenAIResponse.Delta createDelta(String role, String content) {
		OpenAIResponse.Delta delta = new OpenAIResponse.Delta();
		if (role != null)
			delta.setRole(role);
		if (content != null)
			delta.setContent(content);
		return delta;
	}

	/**
	 * Create usage info
	 */
	private OpenAIResponse.Usage createUsageInfo(OpenAIRequest request, String content) {
		OpenAIResponse.Usage usage = new OpenAIResponse.Usage();
		usage.setPromptTokens(estimateTokens(extractUserMessage(request)));
		usage.setCompletionTokens(estimateTokens(content));
		usage.setTotalTokens(usage.getPromptTokens() + usage.getCompletionTokens());
		return usage;
	}

	/**
	 * Estimate token count (simple approximation)
	 */
	private Integer estimateTokens(String text) {
		if (text == null)
			return 0;
		return Math.max(1, text.length() / TOKEN_ESTIMATION_RATIO);
	}

	/**
	 * Check if message is a health check or simple greeting
	 */
	private boolean isHealthCheckMessage(String message) {
		if (message == null)
			return false;

		String lowerMessage = message.toLowerCase().trim();
		return lowerMessage.equals("hi") || lowerMessage.equals("hello") || lowerMessage.equals("health")
				|| lowerMessage.equals("ping") || lowerMessage.equals("test") || lowerMessage.equals("hello world")
				|| lowerMessage.length() <= 5;
	}

	/**
	 * Create health check response for non-streaming
	 */
	private OpenAIResponse createHealthCheckResponse(OpenAIRequest request, String userMessage) {
		String planId = "health-" + System.currentTimeMillis();
		String responseContent = generateHealthCheckContent(userMessage);

		OpenAIResponse response = createBaseResponse(CHATCMPL_PREFIX + planId, CHAT_COMPLETION_OBJECT, request);

		OpenAIResponse.Choice choice = createChoice(0, FINISH_REASON_STOP);
		choice.setMessage(createMessage(ASSISTANT_ROLE, responseContent));
		response.setChoices(Arrays.asList(choice));

		response.setUsage(createUsageInfo(request, responseContent));
		return response;
	}

	/**
	 * Handle health check for streaming
	 */
	private void handleHealthCheckStream(OpenAIRequest request, String userMessage, StreamResponseHandler handler) {
		try {
			String planId = "health-" + System.currentTimeMillis();
			String responseContent = generateHealthCheckContent(userMessage);

			// Send start response
			handler.onResponse(createStreamStartResponse(request, planId));

			// Small delay to simulate processing
			Thread.sleep(100);

			// Send content response
			OpenAIResponse contentResponse = createStreamEndResponse(request, responseContent, planId);
			handler.onResponse(contentResponse);

			// Complete the stream
			handler.onComplete();

		}
		catch (Exception e) {
			logger.error("Error in health check streaming", e);
			handler.onError("Health check failed: " + e.getMessage());
		}
	}

	/**
	 * Generate appropriate health check content
	 */
	private String generateHealthCheckContent(String userMessage) {
		String lowerMessage = userMessage.toLowerCase().trim();

		if (lowerMessage.contains("health") || lowerMessage.equals("ping")) {
			return "✅ JManus is healthy and ready to assist! All systems operational.";
		}
		else if (lowerMessage.equals("hi") || lowerMessage.equals("hello")) {
			return "👋 Hello! I'm JManus, your AI assistant. I'm ready to help you with tasks, planning, and more. How can I assist you today?";
		}
		else if (lowerMessage.equals("test")) {
			return "✅ Test successful! JManus is working properly and ready for complex tasks.";
		}
		else {
			return "👋 Hello! I'm JManus, your intelligent assistant. I can help you with various tasks including planning, analysis, and problem-solving. What would you like to work on?";
		}
	}

	/**
	 * Interface for streaming response handling
	 */
	public interface StreamResponseHandler {

		void onResponse(OpenAIResponse response);

		void onError(String error);

		void onComplete();

	}

}
