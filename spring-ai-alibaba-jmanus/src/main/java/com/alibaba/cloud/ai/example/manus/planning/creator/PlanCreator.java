
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

import com.alibaba.cloud.ai.example.manus.dynamic.agent.entity.DynamicAgentEntity;
import com.alibaba.cloud.ai.example.manus.llm.LlmService;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionContext;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionPlan;
import com.alibaba.cloud.ai.example.manus.recorder.PlanExecutionRecorder;
import com.alibaba.cloud.ai.example.manus.tool.PlanningTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;

import java.util.List;

/**
 * 负责创建执行计划的类
 */
public class PlanCreator {

	private static final Logger log = LoggerFactory.getLogger(PlanCreator.class);

	private final List<DynamicAgentEntity> agents;

	private final LlmService llmService;

	private final PlanningTool planningTool;

	protected final PlanExecutionRecorder recorder;

	public PlanCreator(List<DynamicAgentEntity> agents, LlmService llmService, PlanningTool planningTool,
			PlanExecutionRecorder recorder) {
		this.agents = agents;
		this.llmService = llmService;
		this.planningTool = planningTool;
		this.recorder = recorder;
	}

	/**
	 * 根据用户请求创建执行计划
	 * @param userRequest 用户请求
	 * @return 计划创建结果
	 */
	public void createPlan(ExecutionContext context) {

		String planId = context.getPlanId();
		if (planId == null || planId.isEmpty()) {
			throw new IllegalArgumentException("Plan ID cannot be null or empty");
		}
		try {
			// 构建代理信息
			String agentsInfo = buildAgentsInfo(agents);
			ExecutionPlan currentPlan = null;
			// 生成计划提示
			String planPrompt = generatePlanPrompt(context.getUserRequest(), agentsInfo, planId);

			// 使用 LLM 生成计划
			PromptTemplate promptTemplate = new PromptTemplate(planPrompt);
			Prompt prompt = promptTemplate.create();

			ChatClient.CallResponseSpec response = llmService.getPlanningChatClient()
				.prompt(prompt)
				.tools(List.of(planningTool.getFunctionToolCallback()))
				.advisors(memoryAdvisor -> memoryAdvisor.param("chat_memory_conversation_id", planId)
					.param("chat_memory_retrieve_size", 100))
				.call();
			String outputText = response.chatResponse().getResult().getOutput().getText();
			// 检查计划是否创建成功
			if (planId.equals(planningTool.getCurrentPlanId())) {
				currentPlan = planningTool.getCurrentPlan();
				log.info("Plan created successfully: {}", currentPlan);
				currentPlan.setPlanningThinking(outputText);
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
	 * @param planId 计划ID
	 * @return 格式化的提示字符串
	 */
	private String generatePlanPrompt(String request, String agentsInfo, String planId) {
		return """
				## 介绍
				我是一个AI助手，旨在帮助用户完成各种任务。我的设计目标是提供帮助、信息和多方面的支持。

				## 目标
				我的主要目标是通过提供信息、执行任务和提供指导来帮助用户实现他们的目标。我致力于成为问题解决和任务完成的可靠伙伴。

				## 我的任务处理方法
				当面对任务时，我通常会：
				1. 分析请求以理解需求
				2. 将复杂问题分解为可管理的步骤
				3. 为每个步骤使用适当的AGENT
				4. 以有帮助和有组织的方式交付结果

				## 当前主要目标：
				创建一个合理的计划，包含清晰的步骤来完成任务。

				## 可用代理信息：
				%s

				# 需要完成的任务：
				%s

				你可以使用规划工具来帮助创建计划，使用 %s 作为计划ID。

				重要提示：计划中的每个步骤都必须以[AGENT]开头，代理名称必须是上述列出的可用代理之一。
				例如："[BROWSER_AGENT] 搜索相关信息" 或 "[DEFAULT_AGENT] 处理搜索结果"
				""".formatted(agentsInfo, request, planId);
	}

}
