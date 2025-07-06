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

import java.util.List;
import java.util.Map;

import com.alibaba.cloud.ai.example.manus.config.ManusProperties;
import com.alibaba.cloud.ai.example.manus.dynamic.agent.entity.DynamicAgentEntity;
import com.alibaba.cloud.ai.example.manus.llm.LlmService;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionContext;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionPlan;
import com.alibaba.cloud.ai.example.manus.prompt.PromptLoader;
import com.alibaba.cloud.ai.example.manus.recorder.PlanExecutionRecorder;
import com.alibaba.cloud.ai.example.manus.tool.PlanningTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

/**
 * The class responsible for creating the execution plan
 */
public class PlanCreator {

	private static final Logger log = LoggerFactory.getLogger(PlanCreator.class);

	private final List<DynamicAgentEntity> agents;

	private final LlmService llmService;

	private final PlanningTool planningTool;

	protected final PlanExecutionRecorder recorder;

	private final PromptLoader promptLoader;

	private final ManusProperties manusProperties;

	public PlanCreator(List<DynamicAgentEntity> agents, LlmService llmService, PlanningTool planningTool,
			PlanExecutionRecorder recorder, PromptLoader promptLoader, ManusProperties manusProperties) {
		this.agents = agents;
		this.llmService = llmService;
		this.planningTool = planningTool;
		this.recorder = recorder;
		this.promptLoader = promptLoader;
		this.manusProperties = manusProperties;
	}

	/**
	 * Create an execution plan based on the user request
	 * @param context execution context, containing the user request and the execution
	 * process information
	 * @return plan creation result
	 */
	public void createPlan(ExecutionContext context) {
		boolean useMemory = context.isUseMemory();
		String planId = context.getPlanId();
		if (planId == null || planId.isEmpty()) {
			throw new IllegalArgumentException("Plan ID cannot be null or empty");
		}
		try {
			// Build agent information
			String agentsInfo = buildAgentsInfo(agents);
			// Generate plan prompt
			String planPrompt = generatePlanPrompt(context.getUserRequest(), agentsInfo);

			ExecutionPlan executionPlan = null;
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
						.toolCallbacks(List.of(PlanningTool.getFunctionToolCallback(planningTool)));
					if (useMemory) {
						requestSpec
							.advisors(memoryAdvisor -> memoryAdvisor.param(CONVERSATION_ID, context.getPlanId()));
						requestSpec.advisors(MessageChatMemoryAdvisor
							.builder(llmService.getConversationMemory(manusProperties.getMaxMemory()))
							.build());
					}
					ChatClient.CallResponseSpec response = requestSpec.call();
					outputText = response.chatResponse().getResult().getOutput().getText();

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

			ExecutionPlan currentPlan;
			// Check if the plan is created successfully
			if (executionPlan != null) {
				currentPlan = planningTool.getCurrentPlan();
				currentPlan.setPlanId(planId);
				currentPlan.setPlanningThinking(outputText);
			}
			else {
				log.warn("Creating fallback plan for planId: {}", planId);
				currentPlan = new ExecutionPlan(planId, "answer question without plan");
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
		Map<String, Object> variables = Map.of("agentsInfo", agentsInfo, "request", request);
		return promptLoader.renderPrompt("planning/plan-creation.txt", variables);
	}

}