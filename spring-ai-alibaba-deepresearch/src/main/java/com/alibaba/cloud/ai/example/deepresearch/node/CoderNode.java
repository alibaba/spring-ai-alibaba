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

import com.alibaba.cloud.ai.example.deepresearch.model.Plan;
import com.alibaba.cloud.ai.example.deepresearch.tool.PythonReplTool;
import com.alibaba.cloud.ai.example.deepresearch.util.TemplateUtil;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.model.tool.ToolCallingChatOptions;

import java.util.*;

/**
 * @author yingzi
 * @since 2025/5/18 17:07
 */

public class CoderNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(CoderNode.class);

	private final ChatClient coderAgent;

	private final PythonReplTool pythonReplTool;

	public CoderNode(ChatClient coderAgent, PythonReplTool pythonReplTool) {
		this.coderAgent = coderAgent;
		this.pythonReplTool = pythonReplTool;
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logger.info("Coder Node is running.");
		List<Message> messages = TemplateUtil.applyPromptTemplate("coder", state);
		Plan currentPlan = state.value("current_plan", Plan.class).get();
		List<String> observations = state.value("observations", List.class)
			.map(list -> (List<String>) list)
			.orElse(Collections.emptyList());

		Plan.Step unexecutedStep = null;
		for (Plan.Step step : currentPlan.getSteps()) {
			if (step.getStepType().equals(Plan.StepType.PROCESSING) && step.getExecutionRes() == null) {
				unexecutedStep = step;
				break;
			}
		}

		// 添加任务消息
		Message taskMessage = new UserMessage(
				String.format("#Task\n\n##title\n\n%s\n\n##description\n\n%s\n\n##locale\n\n%s",
						unexecutedStep.getTitle(), unexecutedStep.getDescription(), state.value("locale", "en-US")));
		messages.add(taskMessage);

		logger.debug("Coder Node message: {}", messages);
		// 调用agent
		String content = coderAgent.prompt()
			.options(ToolCallingChatOptions.builder().build())
			.messages(messages)
			.tools(pythonReplTool)
			.call()
			.content();
		unexecutedStep.setExecutionRes(content);

		logger.info("Coder Node result: {}", content);
		if (content == null) {
			content = "";
		}
		Map<String, Object> updated = new HashMap<>();
		updated.put("messages", List.of(new AssistantMessage(content)));
		observations.add(content);
		updated.put("observations", observations);

		return updated;
	}

}
