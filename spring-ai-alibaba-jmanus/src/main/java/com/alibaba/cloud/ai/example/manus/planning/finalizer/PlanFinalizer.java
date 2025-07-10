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
package com.alibaba.cloud.ai.example.manus.planning.finalizer;

import java.util.List;
import java.util.Map;

import com.alibaba.cloud.ai.example.manus.config.ManusProperties;
import com.alibaba.cloud.ai.example.manus.dynamic.prompt.model.enums.PromptEnum;
import com.alibaba.cloud.ai.example.manus.dynamic.prompt.service.PromptService;
import com.alibaba.cloud.ai.example.manus.llm.LlmService;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionContext;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.PlanInterface;
import com.alibaba.cloud.ai.example.manus.recorder.PlanExecutionRecorder;
import com.alibaba.cloud.ai.example.manus.recorder.entity.PlanExecutionRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

/**
 * The class responsible for generating the execution summary of the plan
 */
public class PlanFinalizer {

	private final LlmService llmService;

	private static final Logger log = LoggerFactory.getLogger(PlanFinalizer.class);

	protected final PlanExecutionRecorder recorder;

	private final PromptService promptService;

	private final ManusProperties manusProperties;

	public PlanFinalizer(LlmService llmService, PlanExecutionRecorder recorder, PromptService promptService,
			ManusProperties manusProperties) {
		this.llmService = llmService;
		this.recorder = recorder;
		this.promptService = promptService;
		this.manusProperties = manusProperties;
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

			Message systemMessage = promptService.createSystemMessage(
					PromptEnum.PLANNING_PLAN_FINALIZER.getPromptName(), Map.of("executionDetail", executionDetail));

			Message userMessage = promptService.createUserMessage(PromptEnum.PLANNING_USER_REQUEST.getPromptName(),
					Map.of("userRequest", userRequest));

			Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

			ChatClient.ChatClientRequestSpec requestSpec = llmService.getPlanningChatClient().prompt(prompt);
			if (context.isUseMemory()) {
				requestSpec.advisors(memoryAdvisor -> memoryAdvisor.param(CONVERSATION_ID, context.getCurrentPlanId()));
				requestSpec.advisors(MessageChatMemoryAdvisor
					.builder(llmService.getConversationMemory(manusProperties.getMaxMemory()))
					.build());
			}
			ChatResponse response = requestSpec.call().chatResponse();

			String summary = response.getResult().getOutput().getText();
			context.setResultSummary(summary);

			recordPlanCompletion(context, summary);
			log.info("Generated summary: {}", summary);
		}
		catch (Exception e) {
			log.error("Error generating summary with LLM", e);
			throw new RuntimeException("Failed to generate summary", e);
		}
		finally {
			llmService.clearConversationMemory(plan.getCurrentPlanId());
		}
	}

	/**
	 * Record plan completion
	 * @param context The execution context
	 * @param summary The summary of the plan execution
	 */
	private void recordPlanCompletion(ExecutionContext context, String summary) {
		// Use thinkActRecordId from context to support sub-plan executions
		PlanExecutionRecord planRecord = recorder.getExecutionRecord(context.getPlan().getCurrentPlanId(),
				context.getPlan().getRootPlanId(), context.getThinkActRecordId());
		if (planRecord != null) {
			recorder.recordPlanCompletion(planRecord, summary);
		}

		log.info("Plan completed with ID: {} (thinkActRecordId: {}) and summary: {}",
				context.getPlan().getCurrentPlanId(), context.getThinkActRecordId(), summary);
	}

}
