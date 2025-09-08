/*
 * Copyright 2024-2025 the original author or authors.
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
package com.alibaba.cloud.ai.graph.agent;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.function.FunctionToolCallback;

public class AgentTool implements BiFunction<String, ToolContext, String> {

	private final ReactAgent agent;

	public AgentTool(ReactAgent agent) {
		this.agent = agent;
	}

	@Override
	public String apply(
			@ToolParam(description = "The original user query that triggered this tool call") String originalUserQuery,
			ToolContext toolContext) {
		OverAllState state = (OverAllState) toolContext.getContext().get("state");
		String toolResult = "";
		try {
			Optional<OverAllState> resultState = agent.getAndCompileGraph().call(state.data());
			Optional<List> messages = resultState.flatMap(overAllState -> overAllState.value("messages", List.class));
			if (messages.isPresent()) {
				@SuppressWarnings("unchecked")
				List<Message> messageList = (List<Message>) messages.get();
				// Use messageList
				Message toolResponseMessage = messageList.get(messageList.size() - 1);
				toolResult = toolResponseMessage.getText();
			}
		}
		catch (GraphStateException e) {
			throw new RuntimeException(e);
		}
		return toolResult;
	}

	public static AgentTool create(ReactAgent agent) {
		return new AgentTool(agent);
	}

	public static ToolCallback getFunctionToolCallback(ReactAgent agent) {
		return FunctionToolCallback.builder(agent.name(), AgentTool.create(agent))
			.description(agent.description())
			.inputType(String.class)
			.build();
	}

}
