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
package com.alibaba.cloud.ai.example.manus.planning.creator;

import com.alibaba.cloud.ai.example.manus.config.ManusProperties;
import com.alibaba.cloud.ai.example.manus.dynamic.agent.entity.DynamicAgentEntity;
import com.alibaba.cloud.ai.example.manus.dynamic.prompt.model.enums.PromptEnum;
import com.alibaba.cloud.ai.example.manus.dynamic.prompt.service.PromptService;
import com.alibaba.cloud.ai.example.manus.llm.ILlmService;
import com.alibaba.cloud.ai.example.manus.llm.StreamingResponseHandler;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionContext;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.PlanInterface;
import com.alibaba.cloud.ai.example.manus.recorder.PlanExecutionRecorder;
import com.alibaba.cloud.ai.example.manus.tool.PlanningToolInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

/**
 * The class responsible for creating the execution plan
 */
public class PlanCreator {

	private static final Logger log = LoggerFactory.getLogger(PlanCreator.class);

	private final List<DynamicAgentEntity> agents;

	private final ILlmService llmService;

	private final PlanningToolInterface planningTool;

	protected final PlanExecutionRecorder recorder;

	private final PromptService promptService;

	private final ManusProperties manusProperties;

	private final StreamingResponseHandler streamingResponseHandler;

	public PlanCreator(List<DynamicAgentEntity> agents, ILlmService llmService, PlanningToolInterface planningTool,
			PlanExecutionRecorder recorder, PromptService promptService, ManusProperties manusProperties,
			StreamingResponseHandler streamingResponseHandler) {
		this.agents = agents;
		this.llmService = llmService;
		this.planningTool = planningTool;
		this.recorder = recorder;
		this.promptService = promptService;
		this.manusProperties = manusProperties;
		this.streamingResponseHandler = streamingResponseHandler;
	}

	/**
	 * Create an execution plan based on the user request
	 * @param context execution context, containing the user request and the execution
	 * process information
	 * @return plan creation result
	 */
	public void createPlan(ExecutionContext context) {
		boolean useMemory = context.isUseMemory();
		String planId = context.getCurrentPlanId();
		if (planId == null || planId.isEmpty()) {
			throw new IllegalArgumentException("Plan ID cannot be null or empty");
		}
		try {
			// Build agent information
			String agentsInfo = buildAgentsInfo(agents);
			// Generate plan prompt
			String planPrompt = generatePlanPrompt(context.getUserRequest(), agentsInfo);

			PlanInterface executionPlan = null;
			String outputText = null;

			// Retry mechanism: up to 3 attempts until a valid execution plan is obtained
			int maxRetries = 3;
			for (int attempt = 1; attempt <= maxRetries; attempt++) {
				try {
					log.info("Attempting to create plan, attempt: {}/{}", attempt, maxRetries);

					// Use LLM to generate the plan
					PromptTemplate promptTemplate = new PromptTemplate(planPrompt);
					Prompt prompt = promptTemplate.create();

					ChatClientRequestSpec requestSpec = llmService.getPlanningChatClient()
						.prompt(prompt)
						.toolCallbacks(List.of(planningTool.getFunctionToolCallback()));
					if (useMemory) {
						requestSpec.advisors(
								memoryAdvisor -> memoryAdvisor.param(CONVERSATION_ID, context.getCurrentPlanId()));
						requestSpec.advisors(MessageChatMemoryAdvisor
							.builder(llmService.getConversationMemory(manusProperties.getMaxMemory()))
							.build());
					}

					// Use streaming response handler for plan creation
					Flux<ChatResponse> responseFlux = requestSpec.stream().chatResponse();
					String planCreationText = streamingResponseHandler.processStreamingTextResponse(responseFlux,
							"Plan creation");
					outputText = planCreationText;

					executionPlan = planningTool.getCurrentPlan();

					if (executionPlan != null) {
						// Set the user input part of the plan, for later storage and use.
						executionPlan.setUserRequest(context.getUserRequest());
						log.info("Plan created successfully on attempt {}: {}", attempt, executionPlan);
						break;
					}
					else {
						log.warn("Plan creation attempt {} failed: planningTool.getCurrentPlan() returned null",
								attempt);
						if (attempt == maxRetries) {
							log.error("Failed to create plan after {} attempts", maxRetries);
						}
					}
				}
				catch (Exception e) {
					log.warn("Exception during plan creation attempt {}: {}", attempt, e.getMessage());
					e.printStackTrace();
					if (attempt == maxRetries) {
						throw e;
					}
				}
			}

			PlanInterface currentPlan;
			// 检查计划是否创建成功
			if (executionPlan != null) {
				currentPlan = planningTool.getCurrentPlan();
				currentPlan.setCurrentPlanId(planId);
				currentPlan.setRootPlanId(planId);
				currentPlan.setPlanningThinking(outputText);
			}
			else {
				throw new RuntimeException("Failed to create a valid execution plan after retries");
			}

			context.setPlan(currentPlan);

		}
		catch (Exception e) {
			log.error("Error creating plan for request: {}", context.getUserRequest(), e);
			// Handle the exception
			throw new RuntimeException("Failed to create plan", e);
		}
	}

	/**
	 * Build the agent information string
	 * @param agents agent list
	 * @return formatted agent information
	 */
	private String buildAgentsInfo(List<DynamicAgentEntity> agents) {
		StringBuilder agentsInfo = new StringBuilder("Available Agents:\n");
		for (DynamicAgentEntity agent : agents) {
			agentsInfo.append("- Agent Name: ")
				.append(agent.getAgentName())
				.append("\n  Description: ")
				.append(agent.getAgentDescription())
				.append("\n");
		}
		return agentsInfo.toString();
	}

	/**
	 * Generate the plan prompt
	 * @param request user request
	 * @param agentsInfo agent information
	 * @return formatted prompt string
	 */
	private String generatePlanPrompt(String request, String agentsInfo) {
		// Escape special characters in request to prevent StringTemplate parsing errors
		String escapedRequest = escapeForStringTemplate(request);
		Map<String, Object> variables = Map.of("agentsInfo", agentsInfo, "request", escapedRequest);
		return promptService.renderPrompt(PromptEnum.PLANNING_PLAN_CREATION.getPromptName(), variables);
	}

	/**
	 * Escape special characters for StringTemplate engine
	 * @param input input string
	 * @return escaped string
	 */
	private String escapeForStringTemplate(String input) {
		if (input == null) {
			return null;
		}
		// Escape characters that are special to StringTemplate
		// Note: Order matters - escape backslash first to avoid double-escaping
		return input.replace("\\", "\\\\")
			.replace("$", "\\$")
			.replace("<", "\\<")
			.replace(">", "\\>")
			.replace("{", "\\{")
			.replace("}", "\\}")
			.replace("[", "\\[")
			.replace("]", "\\]")
			.replace("\"", "\\\"");
	}

}
