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
import org.springframework.ai.model.tool.ToolCallingChatOptions;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

/**
 * @author yingzi
 * @since 2025/5/18 17:07
 */

public class CoderNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(CoderNode.class);

	private final ChatClient coderAgent;

	public CoderNode(ChatClient coderAgent) {
		this.coderAgent = coderAgent;
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logger.info("coder node is running.");
		Plan currentPlan = StateUtil.getPlan(state);
		Map<String, Object> updated = new HashMap<>();

		Plan.Step unexecutedStep = null;
		for (Plan.Step step : currentPlan.getSteps()) {
			if (step.getStepType().equals(Plan.StepType.PROCESSING) && step.getExecutionRes() == null) {
				unexecutedStep = step;
				break;
			}
		}

		if (unexecutedStep == null) {
			logger.info("all coder node is finished.");
			return updated;
		}

		List<Message> messages = new ArrayList<>();
		// 添加任务消息
		Message taskMessage = new UserMessage(
				String.format("#Task\n\n##title\n\n%s\n\n##description\n\n%s\n\n##locale\n\n%s",
						unexecutedStep.getTitle(), unexecutedStep.getDescription(), state.value("locale", "en-US")));
		messages.add(taskMessage);
		logger.debug("coder Node message: {}", messages);

		// 调用agent
		var streamResult = coderAgent.prompt()
			.options(ToolCallingChatOptions.builder().build())
			.messages(messages)
			.stream()
			.chatResponse();

		var generator = StreamingChatGenerator.builder()
			.startingNode("coder_llm_stream")
			.startingState(state)
			.mapResult(response -> Map.of("coder_content",
					Objects.requireNonNull(response.getResult().getOutput().getText())))
			.build(streamResult);

		return Map.of("coder_content", generator);
	}

}
