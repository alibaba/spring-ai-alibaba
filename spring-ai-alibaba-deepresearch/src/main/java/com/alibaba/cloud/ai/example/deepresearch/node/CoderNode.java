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

/**
 * @author sixiyida
 * @since 2025/6/14 11:17
 */

public class CoderNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(CoderNode.class);

	private final ChatClient coderAgent;

	private final String executorNodeId;

	private final String nodeName;

	public CoderNode(ChatClient coderAgent) {
		this(coderAgent, "0");
	}

	public CoderNode(ChatClient coderAgent, String executorNodeId) {
		this.coderAgent = coderAgent;
		this.executorNodeId = executorNodeId;
		this.nodeName = "coder_" + executorNodeId;
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logger.info("coder node {} is running.", executorNodeId);
		Plan currentPlan = StateUtil.getPlan(state);
		Map<String, Object> updated = new HashMap<>();

		Plan.Step assignedStep = null;
		for (Plan.Step step : currentPlan.getSteps()) {
			if (step.getStepType().equals(Plan.StepType.PROCESSING) && !StringUtils.hasText(step.getExecutionRes())
					&& StringUtils.hasText(step.getExecutionStatus())
					&& step.getExecutionStatus().equals(StateUtil.EXECUTION_STATUS_ASSIGNED_PREFIX + nodeName)) {
				assignedStep = step;
				break;
			}
		}

		if (assignedStep == null) {
			logger.info("No remaining steps to be executed by {}", nodeName);
			return updated;
		}

		// 标记步骤为正在执行
		assignedStep.setExecutionStatus(StateUtil.EXECUTION_STATUS_PROCESSING_PREFIX + nodeName);

		List<Message> messages = new ArrayList<>();
		// 添加任务消息
		Message taskMessage = new UserMessage(
				String.format("#Task\n\n##title\n\n%s\n\n##description\n\n%s\n\n##locale\n\n%s",
						assignedStep.getTitle(), assignedStep.getDescription(), state.value("locale", "en-US")));
		messages.add(taskMessage);
		logger.debug("{} Node message: {}", nodeName, messages);

		// 调用agent
		var streamResult = coderAgent.prompt().messages(messages).stream().chatResponse();

		Plan.Step finalAssignedStep = assignedStep;
		logger.info("CoderNode {} starting streaming with key: {}", executorNodeId,
				"coder_llm_stream_" + executorNodeId);
		var generator = StreamingChatGenerator.builder()
			.startingNode("coder_llm_stream_" + executorNodeId)
			.startingState(state)
			.mapResult(response -> {
				finalAssignedStep.setExecutionStatus(StateUtil.EXECUTION_STATUS_COMPLETED_PREFIX + executorNodeId);
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

}
