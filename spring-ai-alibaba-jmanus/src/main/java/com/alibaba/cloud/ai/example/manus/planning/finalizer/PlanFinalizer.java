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

import com.alibaba.cloud.ai.example.manus.llm.LlmService;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionContext;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionPlan;
import com.alibaba.cloud.ai.example.manus.recorder.PlanExecutionRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

/**
 * 负责生成计划执行总结的类
 */
public class PlanFinalizer {

	private final LlmService llmService;

	private static final Logger log = LoggerFactory.getLogger(PlanFinalizer.class);

	protected final PlanExecutionRecorder recorder;

	public PlanFinalizer(LlmService llmService, PlanExecutionRecorder recorder) {
		this.llmService = llmService;
		this.recorder = recorder;
	}

	/**
	 * 生成计划执行总结
	 * @param context 执行上下文，包含用户请求和执行的过程信息
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
		ExecutionPlan plan = context.getPlan();
		String executionDetail = plan.getPlanExecutionStateStringFormat(false);
		try {
			String userRequest = context.getUserRequest();

			SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate("""
					您是 jmanus，一个能够回应用户请求的AI助手，你需要根据这个分步骤的执行计划的执行结果，来回应用户的请求。

					分步骤计划的执行详情：
					{executionDetail}

					请根据执行详情里面的信息，来回应用户的请求。

					""");

			Message systemMessage = systemPromptTemplate.createMessage(Map.of("executionDetail", executionDetail));

			String userRequestTemplate = """
					当前的用户请求是:
					{userRequest}
					""";

			PromptTemplate userMessageTemplate = new PromptTemplate(userRequestTemplate);
			Message userMessage = userMessageTemplate.createMessage(Map.of("userRequest", userRequest));

			Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

			ChatClient.ChatClientRequestSpec requestSpec = llmService.getPlanningChatClient().prompt(prompt);
			if (context.isUseMemory()) {
				requestSpec.advisors(memoryAdvisor -> memoryAdvisor.param(CONVERSATION_ID, context.getPlanId()));
				requestSpec.advisors(MessageChatMemoryAdvisor.builder(llmService.getConversationMemory()).build());
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
			llmService.clearConversationMemory(plan.getPlanId());
		}
	}

	/**
	 * Record plan completion
	 * @param context The execution context
	 * @param summary The summary of the plan execution
	 */
	private void recordPlanCompletion(ExecutionContext context, String summary) {
		recorder.recordPlanCompletion(context.getPlan().getPlanId(), summary);

		log.info("Plan completed with ID: {} and summary: {}", context.getPlan().getPlanId(), summary);
	}

}
