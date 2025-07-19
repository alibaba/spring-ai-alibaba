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

package com.alibaba.cloud.ai.example.deepresearch.node;

import com.alibaba.cloud.ai.toolcalling.searches.SearchEnum;
import com.alibaba.cloud.ai.example.deepresearch.model.dto.Plan;
import com.alibaba.cloud.ai.example.deepresearch.service.SearchFilterService;
import com.alibaba.cloud.ai.example.deepresearch.tool.SearchFilterTool;
import com.alibaba.cloud.ai.example.deepresearch.service.McpProviderFactory;
import com.alibaba.cloud.ai.example.deepresearch.util.StateUtil;
import com.alibaba.cloud.ai.example.deepresearch.util.ReflectionProcessor;
import com.alibaba.cloud.ai.example.deepresearch.util.ReflectionUtil;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.streaming.StreamingChatGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.mcp.AsyncMcpToolCallbackProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author sixiyida
 * @since 2025/6/14 11:17
 */

public class ResearcherNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(ResearcherNode.class);

	private final ChatClient researchAgent;

	private final String executorNodeId;

	private final String nodeName;

	private final SearchFilterService searchFilterService;

	private final ReflectionProcessor reflectionProcessor;

	// MCP工厂
	private final McpProviderFactory mcpFactory;

	public ResearcherNode(ChatClient researchAgent, String executorNodeId, ReflectionProcessor reflectionProcessor,
			McpProviderFactory mcpFactory, SearchFilterService searchFilterService) {
		this.researchAgent = researchAgent;
		this.executorNodeId = executorNodeId;
		this.nodeName = "researcher_" + executorNodeId;
		this.reflectionProcessor = reflectionProcessor;
		this.mcpFactory = mcpFactory;
		this.searchFilterService = searchFilterService;
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		Plan currentPlan = StateUtil.getPlan(state);
		Map<String, Object> updated = new HashMap<>();

		Plan.Step assignedStep = findAssignedStep(currentPlan);

		if (assignedStep == null) {
			logger.info("No remaining steps to be executed by {}", nodeName);
			return updated;
		}

		// Handle reflection logic
		if (reflectionProcessor != null) {
			ReflectionProcessor.ReflectionHandleResult reflectionResult = reflectionProcessor
				.handleReflection(assignedStep, nodeName, "researcher");

			if (!ReflectionUtil.shouldContinueAfterReflection(reflectionResult)) {
				logger.debug("Step {} reflection processing completed, skipping execution", assignedStep.getTitle());
				return updated;
			}
		}

		// Mark step as processing
		assignedStep.setExecutionStatus(StateUtil.EXECUTION_STATUS_PROCESSING_PREFIX + nodeName);

		// Build task messages
		List<Message> messages = new ArrayList<>();

		// Build task message with reflection history
		String taskContent = buildTaskMessageWithReflectionHistory(assignedStep);
		Message taskMessage = new UserMessage(taskContent);
		messages.add(taskMessage);

		// Add researcher-specific citation reminder
		Message citationMessage = new UserMessage(
				"IMPORTANT: DO NOT include inline citations in the text. Instead, track all sources and include a References section at the end using link reference format. Include an empty line between each citation for better readability. Use this format for each reference:\n- [Source Title](URL)\n\n- [Another Source](URL)");
		messages.add(citationMessage);

		logger.debug("{} Node messages: {}", nodeName, messages);

		// Get search tool
		SearchEnum searchEnum = state.value("search_engine", SearchEnum.class).orElse(null);

		// Call agent
		var requestSpec = researchAgent.prompt().messages(messages);

		// 使用MCP工厂创建MCP提供者
		AsyncMcpToolCallbackProvider mcpProvider = mcpFactory != null
				? mcpFactory.createProvider(state, "researchAgent") : null;
		if (mcpProvider != null) {
			requestSpec = requestSpec.toolCallbacks(mcpProvider.getToolCallbacks());
		}

		if (searchEnum != null) {
			requestSpec = requestSpec.tools(
					new SearchFilterTool(searchFilterService, searchEnum, state.value("enable_search_filter", true)));
		}
		var streamResult = requestSpec.stream().chatResponse();

		Plan.Step finalAssignedStep = assignedStep;
		logger.info("ResearcherNode {} starting streaming with key: {}", executorNodeId,
				"researcher_llm_stream_" + executorNodeId);

		var generator = StreamingChatGenerator.builder()
			.startingNode("researcher_llm_stream_" + executorNodeId)
			.startingState(state)
			.mapResult(response -> {
				// Set appropriate completion status using ReflectionUtil
				finalAssignedStep
					.setExecutionStatus(ReflectionUtil.getCompletionStatus(reflectionProcessor != null, nodeName));

				String researchContent = response.getResult().getOutput().getText();
				finalAssignedStep.setExecutionRes(Objects.requireNonNull(researchContent));
				logger.info("{} completed, content: {}", nodeName, researchContent);

				updated.put("researcher_content_" + executorNodeId, researchContent);
				return updated;
			})
			.build(streamResult);

		updated.put("researcher_content_" + executorNodeId, generator);
		return updated;
	}

	/**
	 * Find steps assigned to current node
	 */
	private Plan.Step findAssignedStep(Plan currentPlan) {
		for (Plan.Step step : currentPlan.getSteps()) {
			if (Plan.StepType.RESEARCH.equals(step.getStepType()) && ReflectionUtil.shouldProcessStep(step, nodeName)) {
				return step;
			}
		}
		return null;
	}

	/**
	 * Build task message with reflection history
	 */
	private String buildTaskMessageWithReflectionHistory(Plan.Step step) {
		StringBuilder content = new StringBuilder();

		// Basic task information
		content.append("# Current Task\n\n")
			.append("## Title\n\n")
			.append(step.getTitle())
			.append("\n\n")
			.append("## Description\n\n")
			.append(step.getDescription())
			.append("\n\n");

		// Add reflection history if available
		if (ReflectionUtil.hasReflectionHistory(step)) {
			content.append(ReflectionUtil.buildReflectionHistoryContent(step));
			content.append(
					"Please re-complete this research task based on the above previous attempt results and reflection feedback, ensuring to avoid the previously identified issues and improve upon the previous results.\n\n");
		}

		return content.toString();
	}

}
