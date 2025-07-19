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

import com.alibaba.cloud.ai.example.deepresearch.model.dto.Plan;
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

public class CoderNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(CoderNode.class);

	private final ChatClient coderAgent;

	private final String executorNodeId;

	private final String nodeName;

	private final ReflectionProcessor reflectionProcessor;

	// MCP工厂
	private final McpProviderFactory mcpFactory;

	public CoderNode(ChatClient coderAgent, String executorNodeId, ReflectionProcessor reflectionProcessor,
			McpProviderFactory mcpFactory) {
		this.coderAgent = coderAgent;
		this.executorNodeId = executorNodeId;
		this.nodeName = "coder_" + executorNodeId;
		this.reflectionProcessor = reflectionProcessor;
		this.mcpFactory = mcpFactory;
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logger.info("coder node {} is running for thread: {}", executorNodeId, state.value("thread_id", "__default__"));

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
				.handleReflection(assignedStep, nodeName, "coder");

			if (!ReflectionUtil.shouldContinueAfterReflection(reflectionResult)) {
				logger.debug("Step {} reflection processing completed, skipping execution", assignedStep.getTitle());
				return updated;
			}
		}

		// Mark step as processing
		assignedStep.setExecutionStatus(StateUtil.EXECUTION_STATUS_PROCESSING_PREFIX + nodeName);

		List<Message> messages = new ArrayList<>();
		// Build task message with reflection history
		String taskContent = buildTaskMessageWithReflectionHistory(assignedStep, state.value("locale", "en-US"));
		Message taskMessage = new UserMessage(taskContent);
		messages.add(taskMessage);
		logger.debug("{} Node message: {}", nodeName, messages);

		// 调用agent
		var requestSpec = coderAgent.prompt().messages(messages);

		// 使用MCP工厂创建MCP客户端
		AsyncMcpToolCallbackProvider mcpProvider = mcpFactory != null ? mcpFactory.createProvider(state, "coderAgent")
				: null;
		if (mcpProvider != null) {
			requestSpec = requestSpec.toolCallbacks(mcpProvider.getToolCallbacks());
		}

		var streamResult = requestSpec.stream().chatResponse();
		Plan.Step finalAssignedStep = assignedStep;
		logger.info("CoderNode {} starting streaming with key: {}", executorNodeId,
				"coder_llm_stream_" + executorNodeId);

		var generator = StreamingChatGenerator.builder()
			.startingNode("coder_llm_stream_" + executorNodeId)
			.startingState(state)
			.mapResult(response -> {
				// Set appropriate completion status using ReflectionUtil
				finalAssignedStep
					.setExecutionStatus(ReflectionUtil.getCompletionStatus(reflectionProcessor != null, nodeName));

				String coderContent = response.getResult().getOutput().getText();
				finalAssignedStep.setExecutionRes(Objects.requireNonNull(coderContent));

				logger.info("{} completed, content: {}", nodeName, coderContent);

				updated.put("coder_content_" + executorNodeId, coderContent);
				return updated;
			})
			.build(streamResult);

		updated.put("coder_content_" + executorNodeId, generator);
		return updated;
	}

	/**
	 * Find steps assigned to current node
	 */
	private Plan.Step findAssignedStep(Plan currentPlan) {
		for (Plan.Step step : currentPlan.getSteps()) {
			if (Plan.StepType.PROCESSING.equals(step.getStepType())
					&& ReflectionUtil.shouldProcessStep(step, nodeName)) {
				return step;
			}
		}
		return null;
	}

	/**
	 * Build task message with reflection history
	 */
	private String buildTaskMessageWithReflectionHistory(Plan.Step step, String locale) {
		StringBuilder content = new StringBuilder();

		// Basic task information
		content.append("# Task\n\n")
			.append("## Title\n\n")
			.append(step.getTitle())
			.append("\n\n")
			.append("## Description\n\n")
			.append(step.getDescription())
			.append("\n\n")
			.append("## Locale\n\n")
			.append(locale)
			.append("\n\n");

		// Add reflection history if available
		if (ReflectionUtil.hasReflectionHistory(step)) {
			content.append(ReflectionUtil.buildReflectionHistoryContent(step));
			content.append(
					"Please re-complete this coding task based on the above previous attempt results and reflection feedback, ensuring to avoid the previously identified code issues and deficiencies, and improve upon the previous code.\n\n");
		}

		return content.toString();
	}

}
