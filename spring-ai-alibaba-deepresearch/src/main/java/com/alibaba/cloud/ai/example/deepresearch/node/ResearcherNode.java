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
import com.alibaba.cloud.ai.example.deepresearch.util.StateUtil;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.streaming.StreamingChatGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ResearcherNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(ResearcherNode.class);

	private final ChatClient researchAgent;

	private final String executorNodeId;

	public ResearcherNode(ChatClient researchAgent, String executorNodeId) {
		this.researchAgent = researchAgent;
		this.executorNodeId = executorNodeId;
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logger.info("researcher node {} is running.", executorNodeId);
		Plan currentPlan = StateUtil.getPlan(state);
		List<String> observations = StateUtil.getMessagesByType(state, "observations");
		Map<String, Object> updated = new HashMap<>();
		String executorNodeName = "researcher_" + executorNodeId;
		Plan.Step assignedStep = null;
		long stepId = 0;
		for (Plan.Step step : currentPlan.getSteps()) {
			if (Plan.StepType.RESEARCH.equals(step.getStepType()) && !StringUtils.hasText(step.getExecutionRes())
					&& StringUtils.hasText(step.getExecutionStatus()) && step.getExecutionStatus()
						.equals(StateUtil.EXECUTION_STATUS_ASSIGNED_PREFIX + executorNodeName)) {
				assignedStep = step;
				break;
			}
			stepId++;
		}

		// 如果没有找到分配的步骤，直接返回
		if (assignedStep == null) {
			logger.info("No remaining steps to be executed by {}", executorNodeName);
			return updated;
		}

		final long assignedStepId = stepId;

		// 标记步骤为正在执行
		assignedStep.setExecutionStatus(StateUtil.EXECUTION_STATUS_PROCESSING_PREFIX + executorNodeName);

		// 添加任务消息
		List<Message> messages = new ArrayList<>();
		Message taskMessage = new UserMessage(String.format("# Current Task\n\n##title\n\n%s\n\n##description\n\n%s",
				assignedStep.getTitle(), assignedStep.getDescription()));
		messages.add(taskMessage);

		// 添加研究者特有的引用提醒
		Message citationMessage = new UserMessage(
				"IMPORTANT: DO NOT include inline citations in the text. Instead, track all sources and include a References section at the end using link reference format. Include an empty line between each citation for better readability. Use this format for each reference:\n- [Source Title](URL)\n\n- [Another Source](URL)");
		messages.add(citationMessage);

		logger.debug("researcher Node messages: {}", messages);
		// 调用agent
		var streamResult = researchAgent.prompt().messages(messages).stream().chatResponse();
		Plan.Step finalAssignedStep = assignedStep;
		logger.info("ResearcherNode {} starting streaming with key: {}", executorNodeId,
				"researcher_llm_stream_" + executorNodeId);
		var generator = StreamingChatGenerator.builder()
			.startingNode("researcher_llm_stream_" + executorNodeId)
			.startingState(state)
			.mapResult(response -> {
				finalAssignedStep.setExecutionStatus(StateUtil.EXECUTION_STATUS_COMPLETED_PREFIX + executorNodeId);
				finalAssignedStep.setExecutionRes(Objects.requireNonNull(response.getResult().getOutput().getText()));
				return Map.of("researcher_content_" + executorNodeId,
						Objects.requireNonNull(response.getResult().getOutput().getText()));
			})
			.build(streamResult);
		return Map.of("researcher_content_" + executorNodeId, generator);
	}

}
