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
package com.alibaba.cloud.ai.manus.planning.service;

import com.alibaba.cloud.ai.manus.config.ManusProperties;
import com.alibaba.cloud.ai.manus.llm.ILlmService;
import com.alibaba.cloud.ai.manus.llm.StreamingResponseHandler;
import com.alibaba.cloud.ai.manus.memory.advisor.CustomMessageChatMemoryAdvisor;
import com.alibaba.cloud.ai.manus.prompt.model.enums.PromptEnum;
import com.alibaba.cloud.ai.manus.prompt.service.PromptService;
import com.alibaba.cloud.ai.manus.recorder.service.PlanExecutionRecorder;
import com.alibaba.cloud.ai.manus.runtime.entity.vo.ExecutionContext;
import com.alibaba.cloud.ai.manus.runtime.entity.vo.PlanExecutionResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

/**
 * Refactored PlanFinalizer with improved code organization and reduced duplication
 */
@Service
public class PlanFinalizer {

	private static final Logger log = LoggerFactory.getLogger(PlanFinalizer.class);

	private final ILlmService llmService;

	private final PlanExecutionRecorder recorder;

	private final PromptService promptService;

	private final ManusProperties manusProperties;

	private final StreamingResponseHandler streamingResponseHandler;

	public PlanFinalizer(ILlmService llmService, PlanExecutionRecorder recorder, PromptService promptService,
			ManusProperties manusProperties, StreamingResponseHandler streamingResponseHandler) {
		this.llmService = llmService;
		this.recorder = recorder;
		this.promptService = promptService;
		this.manusProperties = manusProperties;
		this.streamingResponseHandler = streamingResponseHandler;
	}

	/**
	 * Generate the execution summary of the plan using LLM
	 */
	private void generateSummary(ExecutionContext context, PlanExecutionResult result) {
		validateContextWithPlan(context, "ExecutionContext or its plan cannot be null");

		Map<String, Object> promptVariables = Map.of("executionDetail",
				context.getPlan().getPlanExecutionStateStringFormat(false), "userRequest", context.getUserRequest());

		generateWithLlm(context, result, PromptEnum.PLANNING_PLAN_FINALIZER.getPromptName(), promptVariables, "summary",
				"Generated summary: {}");
	}

	/**
	 * Generate direct LLM response for simple requests
	 */
	private void generateDirectResponse(ExecutionContext context, PlanExecutionResult result) {
		validateForGeneration(context, "ExecutionContext or user request cannot be null");

		String userRequest = context.getUserRequest();
		log.info("Generating direct response for user request: {}", userRequest);

		Map<String, Object> promptVariables = Map.of("userRequest", userRequest);

		generateWithLlm(context, result, PromptEnum.DIRECT_RESPONSE.getPromptName(), promptVariables, "direct response",
				"Generated direct response: {}");
	}

	/**
	 * Generate code-based summary when LLM is not needed
	 */
	private void generateCodeBasedSummary(ExecutionContext context, PlanExecutionResult result) {
		log.info("No need to generate summary, use code generate summary instead");
		String summary = context.getPlan().getPlanExecutionStateStringFormat(false);

		// Set result in PlanExecutionResult
		result.setFinalResult(summary);
		recordPlanCompletion(context, summary);
	}

	/**
	 * Core method for generating LLM responses with common logic
	 */
	private String generateLlmResponse(ExecutionContext context, String promptName, Map<String, Object> variables,
			String operationName) {
		Message message = promptService.createUserMessage(promptName, variables);
		Prompt prompt = new Prompt(List.of(message));

		ChatClient.ChatClientRequestSpec requestSpec = llmService.getPlanningChatClient().prompt(prompt);
		configureMemoryAdvisors(requestSpec, context);

		Flux<ChatResponse> responseFlux = requestSpec.stream().chatResponse();
		return streamingResponseHandler.processStreamingTextResponse(responseFlux, operationName,
				context.getCurrentPlanId());
	}

	/**
	 * Configure memory advisors for the request
	 */
	private void configureMemoryAdvisors(ChatClient.ChatClientRequestSpec requestSpec, ExecutionContext context) {
		if (!context.isUseMemory() || context.getMemoryId() == null) {
			return;
		}

		requestSpec.advisors(memoryAdvisor -> memoryAdvisor.param(CONVERSATION_ID, context.getMemoryId()));
		requestSpec.advisors(
				CustomMessageChatMemoryAdvisor
					.builder(llmService.getConversationMemory(manusProperties.getMaxMemory()), context.getUserRequest(),
							CustomMessageChatMemoryAdvisor.AdvisorType.AFTER)
					.build());
	}

	/**
	 * Record plan completion with the given context and summary
	 */
	private void recordPlanCompletion(ExecutionContext context, String summary) {
		if (context == null || context.getPlan() == null) {
			log.warn("Cannot record plan completion: context or plan is null");
			return;
		}

		String currentPlanId = context.getPlan().getCurrentPlanId();
		recorder.recordPlanCompletion(currentPlanId, summary);
	}

	/**
	 * Validate execution context with plan validation
	 */
	private void validateContextWithPlan(ExecutionContext context, String errorMessage) {
		if (context == null || context.getPlan() == null) {
			throw new IllegalArgumentException(errorMessage);
		}
	}

	/**
	 * Handle post-execution processing based on context requirements
	 * @param context Execution context
	 * @param result Execution result
	 * @return The processed execution result
	 */
	public PlanExecutionResult handlePostExecution(ExecutionContext context, PlanExecutionResult result) {
		if (context == null || result == null) {
			return result;
		}

		try {
			// Check if we need to generate a summary
			if (context.isNeedSummary()) {
				log.debug("Generating LLM summary for plan: {}", context.getCurrentPlanId());
				generateSummary(context, result);
				return result;
			}

			// Check if this is a direct response plan
			if (context.getPlan() != null && context.getPlan().isDirectResponse()) {
				log.debug("Generating direct response for plan: {}", context.getCurrentPlanId());
				generateDirectResponse(context, result);
				return result;
			}

			log.debug("Post-execution processing completed for plan: {}", context.getCurrentPlanId());

		}
		catch (Exception e) {
			log.warn("Error during post-execution processing for plan: {}, but continuing", context.getCurrentPlanId(),
					e);
			// Don't fail the entire execution for post-processing errors
		}

		return result;
	}

	/**
	 * Unified method for generating LLM responses with common processing
	 */
	private void generateWithLlm(ExecutionContext context, PlanExecutionResult result, String promptName,
			Map<String, Object> variables, String operationType, String successLogTemplate) {
		try {
			String llmResult = generateLlmResponse(context, promptName, variables,
					Character.toUpperCase(operationType.charAt(0)) + operationType.substring(1) + " generation");
			processAndRecordResult(context, result, llmResult, successLogTemplate);
		}
		catch (Exception e) {
			handleLlmError(operationType, e);
		}
	}

	/**
	 * Common result processing and recording logic
	 */
	private void processAndRecordResult(ExecutionContext context, PlanExecutionResult result, String llmResult,
			String logTemplate) {
		// Set result in PlanExecutionResult
		result.setFinalResult(llmResult);
		recordPlanCompletion(context, llmResult);
		log.info(logTemplate, llmResult);
	}

	/**
	 * Unified validation for generation methods
	 */
	private void validateForGeneration(ExecutionContext context, String errorMessage) {
		if (context == null) {
			throw new IllegalArgumentException(errorMessage);
		}
		if (context.getUserRequest() == null) {
			throw new IllegalArgumentException("User request cannot be null");
		}
	}

	/**
	 * Handle LLM generation errors with consistent error handling
	 */
	private void handleLlmError(String operationType, Exception e) {
		log.error("Error generating {} with LLM", operationType, e);
		throw new RuntimeException("Failed to generate " + operationType, e);
	}

}
