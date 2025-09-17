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
import com.alibaba.cloud.ai.manus.runtime.entity.vo.PlanInterface;

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
 * The class responsible for generating the execution summary of the plan
 */
@Service
public class PlanFinalizer {

	private final ILlmService llmService;

	private static final Logger log = LoggerFactory.getLogger(PlanFinalizer.class);

	protected final PlanExecutionRecorder recorder;

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
	 * Generate the execution summary of the plan
	 * @param context execution context, containing the user request and the execution
	 * process information
	 */
	public void generateSummary(ExecutionContext context) {
		if (context == null || context.getPlan() == null) {
			throw new IllegalArgumentException("ExecutionContext or its plan cannot be null");
		}
		if (!context.isNeedSummary()) {
			log.info("No need to generate summary, use code generate summary instead");
			String summary = context.getPlan().getPlanExecutionStateStringFormat(false);
			context.setResultSummary(summary);
			recordPlanCompletion(context, summary);
			return;

		}
		PlanInterface plan = context.getPlan();
		String executionDetail = plan.getPlanExecutionStateStringFormat(false);
		try {
			String userRequest = context.getUserRequest();

			Message combinedMessage = promptService.createUserMessage(
					PromptEnum.PLANNING_PLAN_FINALIZER.getPromptName(),
					Map.of("executionDetail", executionDetail, "userRequest", userRequest));

			Prompt prompt = new Prompt(List.of(combinedMessage));

			ChatClient.ChatClientRequestSpec requestSpec = llmService.getPlanningChatClient().prompt(prompt);
			if (context.isUseMemory() && context.getMemoryId() != null) {
				requestSpec.advisors(memoryAdvisor -> memoryAdvisor.param(CONVERSATION_ID, context.getMemoryId()));
				requestSpec.advisors(
						CustomMessageChatMemoryAdvisor
							.builder(llmService.getConversationMemory(manusProperties.getMaxMemory()),
									context.getUserRequest(), CustomMessageChatMemoryAdvisor.AdvisorType.AFTER)
							.build());
			}

			// Use streaming response handler for summary generation
			Flux<ChatResponse> responseFlux = requestSpec.stream().chatResponse();
			String summary = streamingResponseHandler.processStreamingTextResponse(responseFlux, "Summary generation",
					context.getCurrentPlanId());
			context.setResultSummary(summary);

			recordPlanCompletion(context, summary);
			log.info("Generated summary: {}", summary);
		}
		catch (Exception e) {
			log.error("Error generating summary with LLM", e);
			throw new RuntimeException("Failed to generate summary", e);
		}
	}

	/**
	 * Record plan completion with the given context and summary
	 * @param context Execution context
	 * @param summary Plan execution summary
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
	 * Generate direct LLM response for simple requests
	 * @param context execution context containing the user request
	 */
	public void generateDirectResponse(ExecutionContext context) {
		if (context == null || context.getUserRequest() == null) {
			throw new IllegalArgumentException("ExecutionContext or user request cannot be null");
		}

		String userRequest = context.getUserRequest();
		log.info("Generating direct response for user request: {}", userRequest);

		try {
			// Create a simple prompt for direct response
			Message directMessage = promptService.createUserMessage(PromptEnum.DIRECT_RESPONSE.getPromptName(),
					Map.of("userRequest", userRequest));

			Prompt prompt = new Prompt(List.of(directMessage));
			ChatClient.ChatClientRequestSpec requestSpec = llmService.getPlanningChatClient().prompt(prompt);

			if (context.isUseMemory() && context.getMemoryId() != null) {
				requestSpec.advisors(memoryAdvisor -> memoryAdvisor.param(CONVERSATION_ID, context.getMemoryId()));
				requestSpec.advisors(
						CustomMessageChatMemoryAdvisor
							.builder(llmService.getConversationMemory(manusProperties.getMaxMemory()),
									context.getUserRequest(), CustomMessageChatMemoryAdvisor.AdvisorType.AFTER)
							.build());
			}

			// Use streaming response handler for direct response generation
			Flux<ChatResponse> responseFlux = requestSpec.stream().chatResponse();
			String directResponse = streamingResponseHandler.processStreamingTextResponse(responseFlux,
					"Direct response", context.getCurrentPlanId());
			context.setResultSummary(directResponse);

			recordPlanCompletion(context, directResponse);
			log.info("Generated direct response: {}", directResponse);

		}
		catch (Exception e) {
			log.error("Error generating direct response for request: {}", userRequest, e);
			throw new RuntimeException("Failed to generate direct response", e);
		}
	}

}
