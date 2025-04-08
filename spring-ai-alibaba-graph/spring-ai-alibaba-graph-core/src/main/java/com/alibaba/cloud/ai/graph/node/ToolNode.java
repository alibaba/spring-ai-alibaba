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
package com.alibaba.cloud.ai.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ToolNode implements NodeAction {

	private String llmResponseKey;

	private String outputKey;

	private List<FunctionCallback> toolCallbacks = new ArrayList<>();

	private AssistantMessage assistantMessage;

	private ToolCallbackResolver toolCallbackResolver;

	public ToolNode(ToolCallbackResolver resolver) {
		this.toolCallbackResolver = resolver;
	}

	public ToolNode(List<FunctionCallback> toolCallbacks, ToolCallbackResolver resolver) {
		this.toolCallbacks = toolCallbacks;
		this.toolCallbackResolver = resolver;
	}

	void setToolCallbacks(List<FunctionCallback> toolCallbacks) {
		this.toolCallbacks = toolCallbacks;
	}

	void setToolCallbackResolver(ToolCallbackResolver toolCallbackResolver) {
		this.toolCallbackResolver = toolCallbackResolver;
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		if (!StringUtils.hasLength(llmResponseKey)) {
			this.llmResponseKey = LlmNode.LLM_RESPONSE_KEY;
		}

		this.assistantMessage = (AssistantMessage) state.value(this.llmResponseKey).orElseGet(() -> {
			// if key not set, use 'messages' as default
			List<Message> messages = (List<Message>) state.value("messages").orElseThrow();
			return messages.get(messages.size() - 1);
		});

		ToolResponseMessage toolResponseMessage = executeFunction(assistantMessage);

		Map<String, Object> updatedState = new HashMap<>();
		updatedState.put("messages", toolResponseMessage);
		if (StringUtils.hasLength(this.outputKey)) {
			updatedState.put(this.outputKey, toolResponseMessage);
		}
		return updatedState;
	}

	private ToolResponseMessage executeFunction(AssistantMessage assistantMessage) {
		// execute the tool function
		List<ToolResponseMessage.ToolResponse> toolResponses = new ArrayList<>();

		for (AssistantMessage.ToolCall toolCall : assistantMessage.getToolCalls()) {
			String toolName = toolCall.name();
			String toolArgs = toolCall.arguments();

			FunctionCallback toolCallback = this.resolve(toolName);

			String toolResult = toolCallback.call(toolArgs, new ToolContext(Map.of()));
			toolResponses.add(new ToolResponseMessage.ToolResponse(toolCall.id(), toolName, toolResult));
		}
		return new ToolResponseMessage(toolResponses, Map.of());
	}

	private FunctionCallback resolve(String toolName) {
		return toolCallbacks.stream()
			.filter(callback -> callback.getName().equals(toolName))
			.findFirst()
			.orElseGet(() -> toolCallbackResolver.resolve(toolName));

	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private String llmResponseKey;

		private String outputKey;

		private List<FunctionCallback> toolCallbacks = new ArrayList<>();

		private List<String> toolNames = new ArrayList<>();

		private ToolCallbackResolver toolCallbackResolver;

		private Builder() {
		}

		public Builder llmResponseKey(String llmResponseKey) {
			this.llmResponseKey = llmResponseKey;
			return this;
		}

		public Builder outputKey(String outputKey) {
			this.outputKey = outputKey;
			return this;
		}

		public Builder toolCallbacks(List<FunctionCallback> toolCallbacks) {
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

		public ToolNode build() {
			ToolNode toolNode = new ToolNode(toolCallbackResolver);
			toolNode.llmResponseKey = this.llmResponseKey;
			toolNode.outputKey = this.outputKey;
			toolNode.setToolCallbacks(this.toolCallbacks);
			toolNode.setToolCallbackResolver(this.toolCallbackResolver);
			return toolNode;
		}

	}

}
