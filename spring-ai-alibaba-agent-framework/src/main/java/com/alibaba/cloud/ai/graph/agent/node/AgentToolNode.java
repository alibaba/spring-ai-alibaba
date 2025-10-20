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
package com.alibaba.cloud.ai.graph.agent.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.NodeActionWithConfig;
import com.alibaba.cloud.ai.graph.state.RemoveByHash;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class AgentToolNode implements NodeActionWithConfig {

	private List<ToolCallback> toolCallbacks = new ArrayList<>();

	private ToolCallbackResolver toolCallbackResolver;

	public AgentToolNode(ToolCallbackResolver resolver) {
		this.toolCallbackResolver = resolver;
	}

	public AgentToolNode(List<ToolCallback> toolCallbacks, ToolCallbackResolver resolver) {
		this.toolCallbacks = toolCallbacks;
		this.toolCallbackResolver = resolver;
	}

	public void setToolCallbacks(List<ToolCallback> toolCallbacks) {
		this.toolCallbacks = toolCallbacks;
	}

	void setToolCallbackResolver(ToolCallbackResolver toolCallbackResolver) {
		this.toolCallbackResolver = toolCallbackResolver;
	}

	@Override
	public Map<String, Object> apply(OverAllState state, RunnableConfig config) throws Exception {
		List<Message> messages = (List<Message>) state.value("messages").orElseThrow();
		Message lastMessage = messages.get(messages.size() - 1);

		Map<String, Object> updatedState = new HashMap<>();
		if (lastMessage instanceof AssistantMessage assistantMessage) {
			// execute the tool function
			List<ToolResponseMessage.ToolResponse> toolResponses = new ArrayList<>();
			for (AssistantMessage.ToolCall toolCall : assistantMessage.getToolCalls()) {
				String toolName = toolCall.name();
				String toolArgs = toolCall.arguments();

				ToolCallback toolCallback = this.resolve(toolName);

				String toolResult = toolCallback.call(toolArgs, new ToolContext(Map.of("state", state, "config", config)));
				toolResponses.add(new ToolResponseMessage.ToolResponse(toolCall.id(), toolName, toolResult));
			}

			ToolResponseMessage toolResponseMessage = new ToolResponseMessage(toolResponses, Map.of());
			updatedState.put("messages", toolResponseMessage);
		} else if (lastMessage instanceof ToolResponseMessage toolResponseMessage) {
			if (messages.size() < 2) {
				throw new IllegalStateException("Cannot find AssistantMessage before ToolResponseMessage");
			}
			Message secondLastMessage = messages.get(messages.size() - 2);
			if (!(secondLastMessage instanceof AssistantMessage assistantMessage)) {
				throw new IllegalStateException("Message before ToolResponseMessage is not an AssistantMessage");
			}

			List<ToolResponseMessage.ToolResponse> existingResponses = toolResponseMessage.getResponses();
			List<ToolResponseMessage.ToolResponse> allResponses = new ArrayList<>(existingResponses);

			Set<String> executedToolNames = existingResponses.stream()
					.map(ToolResponseMessage.ToolResponse::name)
					.collect(Collectors.toSet());

			for (AssistantMessage.ToolCall toolCall : assistantMessage.getToolCalls()) {
				if (executedToolNames.contains(toolCall.name())) {
					continue;
				}

				String toolName = toolCall.name();
				String toolArgs = toolCall.arguments();
				ToolCallback toolCallback = this.resolve(toolName);
				String toolResult = toolCallback.call(toolArgs, new ToolContext(Map.of("state", state, "config", config)));
				allResponses.add(new ToolResponseMessage.ToolResponse(toolCall.id(), toolName, toolResult));
			}

			List<Object> newMessages = new ArrayList<>();
			ToolResponseMessage newToolResponseMessage = new ToolResponseMessage(allResponses, Map.of());
			newMessages.add(newToolResponseMessage);
			newMessages.add(new RemoveByHash<>(assistantMessage));
			updatedState.put("messages", newMessages);
		} else {
			throw new IllegalStateException("Last message is not an AssistantMessage or ToolResponseMessage");
		}

		return updatedState;
	}

	private ToolCallback resolve(String toolName) {
		return toolCallbacks.stream()
			.filter(callback -> callback.getToolDefinition().name().equals(toolName))
			.findFirst()
			.orElseGet(() -> toolCallbackResolver.resolve(toolName));

	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private List<ToolCallback> toolCallbacks = new ArrayList<>();

		private List<String> toolNames = new ArrayList<>();

		private ToolCallbackResolver toolCallbackResolver;

		private Builder() {
		}

		public Builder toolCallbacks(List<ToolCallback> toolCallbacks) {
			this.toolCallbacks = toolCallbacks;
			return this;
		}

		public Builder toolNames(List<String> toolNames) {
			this.toolNames = toolNames;
			return this;
		}

		public Builder toolCallbackResolver(ToolCallbackResolver toolCallbackResolver) {
			this.toolCallbackResolver = toolCallbackResolver;
			return this;
		}

		public AgentToolNode build() {
			AgentToolNode toolNode = new AgentToolNode(toolCallbackResolver);
			toolNode.setToolCallbacks(this.toolCallbacks);
			toolNode.setToolCallbackResolver(this.toolCallbackResolver);
			return toolNode;
		}

	}

}
