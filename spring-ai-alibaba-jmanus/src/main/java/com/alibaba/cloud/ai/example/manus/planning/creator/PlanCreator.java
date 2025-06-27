
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
 * 负责创建执行计划的类
 */
public class PlanCreator {

	private static final Logger log = LoggerFactory.getLogger(PlanCreator.class);

	private final List<DynamicAgentEntity> agents;

	private final LlmService llmService;

	private final PlanningTool planningTool;

	protected final PlanExecutionRecorder recorder;

	private final PromptLoader promptLoader;

	public PlanCreator(List<DynamicAgentEntity> agents, LlmService llmService, PlanningTool planningTool,
			PlanExecutionRecorder recorder, PromptLoader promptLoader) {
		this.agents = agents;
		this.llmService = llmService;
		this.planningTool = planningTool;
		this.recorder = recorder;
		this.promptLoader = promptLoader;
	}

	/**
	 * 根据用户请求创建执行计划
	 * @param context 执行上下文，包含用户请求和执行的过程信息
	 * @return 计划创建结果
	 */
	public void createPlan(ExecutionContext context) {
		boolean useMemory = context.isUseMemory();
		String planId = context.getPlanId();
		if (planId == null || planId.isEmpty()) {
			throw new IllegalArgumentException("Plan ID cannot be null or empty");
		}
		try {
			// 构建代理信息
			String agentsInfo = buildAgentsInfo(agents);
			// 生成计划提示
			String planPrompt = generatePlanPrompt(context.getUserRequest(), agentsInfo);

			ExecutionPlan executionPlan = null;
			String outputText = null;

			// 重试机制：最多尝试3次直到获取到有效的执行计划
			int maxRetries = 3;
			for (int attempt = 1; attempt <= maxRetries; attempt++) {
				try {
					log.info("Attempting to create plan, attempt: {}/{}", attempt, maxRetries);

					// 使用 LLM 生成计划
					PromptTemplate promptTemplate = new PromptTemplate(planPrompt);
					Prompt prompt = promptTemplate.create();

					ChatClientRequestSpec requestSpec = llmService.getPlanningChatClient()
						.prompt(prompt)
						.toolCallbacks(List.of(PlanningTool.getFunctionToolCallback(planningTool)));
					if (useMemory) {
						requestSpec
							.advisors(memoryAdvisor -> memoryAdvisor.param(CONVERSATION_ID, context.getPlanId()));
						requestSpec
							.advisors(MessageChatMemoryAdvisor.builder(llmService.getConversationMemory()).build());
					}
					ChatClient.CallResponseSpec response = requestSpec.call();
					outputText = response.chatResponse().getResult().getOutput().getText();

					executionPlan = planningTool.getCurrentPlan();

					if (executionPlan != null) {
						// 设置计划的用户输入部分，方便后期存储和使用。
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
			// 检查计划是否创建成功
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
			// 处理异常情况
			throw new RuntimeException("Failed to create plan", e);
		}
	}

	/**
	 * 构建代理信息字符串
	 * @param agents 代理列表
	 * @return 格式化的代理信息
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
	 * 生成计划提示
	 * @param request 用户请求
	 * @param agentsInfo 代理信息
	 * @return 格式化的提示字符串
	 */
	private String generatePlanPrompt(String request, String agentsInfo) {
		Map<String, Object> variables = Map.of("agentsInfo", agentsInfo, "request", request);
		return promptLoader.renderPrompt("planning/plan-creation.txt", variables);
	}

}
