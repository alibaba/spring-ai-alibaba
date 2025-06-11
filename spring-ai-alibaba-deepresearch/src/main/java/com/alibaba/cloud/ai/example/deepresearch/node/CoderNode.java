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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import reactor.core.publisher.Flux;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

/**
 * @author yingzi
 * @since 2025/5/18 17:07
 * @author sixiyida
 * @since 2025/6/11 16:04
 */

public class CoderNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(CoderNode.class);

	private final ChatClient coderAgent;

	private final String executorNodeId;

	public CoderNode(ChatClient coderAgent, String executorNodeId) {
		this.coderAgent = coderAgent;
		this.executorNodeId = executorNodeId;

	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logger.info("coder node {} is running.", executorNodeId);
		Plan currentPlan = StateUtil.getPlan(state);
		List<String> observations = StateUtil.getMessagesByType(state, "observations");
		Map<String, Object> updated = new HashMap<>();

		Plan.Step assignedStep = null;
		for (Plan.Step step : currentPlan.getSteps()) {
			if (Plan.StepType.PROCESSING.equals(step.getStepType()) && !StringUtils.hasText(step.getExecutionRes())
					&& step.getExecutionStatus().equals(StateUtil.EXECUTION_STATUS_ASSIGNED_PREFIX + executorNodeId)) {
				assignedStep = step;
				break;
			}
		}

		// 如果没有找到分配的步骤，直接返回
		if (assignedStep == null) {
			logger.info("all coder node is finished.");
			return updated;
		}

		// 标记步骤为正在执行
		assignedStep.setExecutionStatus(StateUtil.EXECUTION_STATUS_PROCESSING_PREFIX + executorNodeId);

		List<Message> messages = new ArrayList<>();
		// 添加任务消息
		Message taskMessage = new UserMessage(
				String.format("#Task\n\n##title\n\n%s\n\n##description\n\n%s\n\n##locale\n\n%s",
						assignedStep.getTitle(), assignedStep.getDescription(), state.value("locale", "en-US")));
		messages.add(taskMessage);
		// 添加已被观测到的数据
		messages.add(new UserMessage(observations.toString()));

		logger.debug("coder Node message: {}", messages);
		// 调用agent
		Flux<String> StreamResult = coderAgent.prompt().messages(messages).stream().content();

		String result = StreamResult.reduce((acc, next) -> acc + next).block();
		assignedStep.setExecutionRes(result);
		assignedStep.setExecutionStatus(StateUtil.EXECUTION_STATUS_COMPLETED_PREFIX + executorNodeId);
		logger.info("coder Node result: {}", result);

		observations.add(result);
		updated.put("observations", observations);

		return updated;
	}

}
