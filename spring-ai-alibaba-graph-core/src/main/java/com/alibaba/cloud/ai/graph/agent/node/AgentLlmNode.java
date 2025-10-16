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
import com.alibaba.cloud.ai.graph.action.NodeAction;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import reactor.core.publisher.Flux;

import static com.alibaba.cloud.ai.graph.utils.Messageutils.convertToMessages;

public class AgentLlmNode implements NodeAction {

	private String userPromptTemplate;

	private String systemPrompt;

	private List<Advisor> advisors = new ArrayList<>();

	private List<ToolCallback> toolCallbacks = new ArrayList<>();

	private String outputKey;

	private String outputSchema;

	private ChatClient chatClient;

	private Boolean stream = Boolean.FALSE;

	public AgentLlmNode() {
	}

	public AgentLlmNode(String userPromptTemplate, List<Advisor> advisors, List<ToolCallback> toolCallbacks, ChatClient chatClient, boolean stream) {
		this.userPromptTemplate = userPromptTemplate;
		this.advisors = advisors;
		this.toolCallbacks = toolCallbacks;
		this.chatClient = chatClient;
		this.stream = stream;
	}

	public static Builder builder() {
		return new Builder();
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		List<Message> messages = new ArrayList<>();
		if (state.value("messages").isPresent()) {
			Object messagesValue = state.value("messages").get();
			messages = (List<Message>)messagesValue;
//			messages = convertToMessages(messagesValue);
		}

		if (messages.isEmpty()) {
			throw new IllegalArgumentException("Either 'instruction' or 'includeContents' must be provided");
		}

		// add streaming support
		if (Boolean.TRUE.equals(stream)) {
			Flux<ChatResponse> chatResponseFlux = buildChatClientRequestSpec(messages, state.data()).stream().chatResponse();
			return Map.of(StringUtils.hasLength(this.outputKey) ? this.outputKey : "messages", chatResponseFlux);
		}
		else {
			AssistantMessage responseOutput;
			try {
				ChatResponse response = buildChatClientRequestSpec(messages, state.data()).call().chatResponse();
				responseOutput = response.getResult().getOutput();
			}
			catch (Exception e) {
				responseOutput = new AssistantMessage("Exception: " + e.getMessage());
			}

			Map<String, Object> updatedState = new HashMap<>();
			updatedState.put("messages", responseOutput);
			if (StringUtils.hasLength(this.outputKey)) {
				updatedState.put(this.outputKey, responseOutput);
			}
			return updatedState;
		}
	}

	public void setToolCallbacks(List<ToolCallback> toolCallbacks) {
		this.toolCallbacks = toolCallbacks;
	}

	private String renderPromptTemplate(String prompt, Map<String, Object> params) {
		PromptTemplate promptTemplate = new PromptTemplate(prompt);
		return promptTemplate.render(params);
	}

	public void augmentUserMessage(List<Message> messages, String outputSchema) {
		for (int i = messages.size() - 1; i >= 0; i--) {
			Message message = messages.get(i);
			if (message instanceof UserMessage userMessage) {
				messages.set(i, userMessage.mutate().text(userMessage.getText() + System.lineSeparator() + outputSchema).build());
				break;
			}
			if (i == 0) {
				messages.add(new UserMessage(outputSchema));
			}
		}
	}

	private ChatClient.ChatClientRequestSpec buildChatClientRequestSpec(List<Message> messages, Map<String, Object> params) {
		augmentUserMessage(messages, outputSchema);

		ChatClient.ChatClientRequestSpec chatClientRequestSpec = chatClient.prompt()
				.options(ToolCallingChatOptions.builder()
						.toolCallbacks(toolCallbacks)
						.internalToolExecutionEnabled(false)
						.build())
				.messages(messages)
				.advisors(advisors);

		if (StringUtils.hasLength(systemPrompt)) {
			chatClientRequestSpec.system(systemPrompt);
		}

		if (messages.isEmpty() && !StringUtils.hasLength(userPromptTemplate)) {
			throw new IllegalArgumentException("Either 'instruction' or 'includeContents' must be provided for agent definition.");
		}

		if (StringUtils.hasLength(userPromptTemplate)) {
			if (!params.isEmpty()) {
				messages.add(new UserMessage(renderPromptTemplate(userPromptTemplate, params)));
			} else {
				messages.add(new UserMessage(userPromptTemplate));
			}
		}

		return chatClientRequestSpec;
	}

	public static class Builder {

		private String outputKey;

		private String outputSchema;

		private ChatClient chatClient;

		private String userPromptTemplate;

		private String systemPrompt;

		private List<Advisor> advisors;

		private List<ToolCallback> toolCallbacks;

		private Boolean stream;

		public Builder userPromptTemplate(String userPromptTemplate) {
			this.userPromptTemplate = userPromptTemplate;
			return this;
		}

		public Builder systemPrompt(String systemPrompt) {
			this.systemPrompt = systemPrompt;
			return this;
		}

		public Builder outputKey(String outputKey) {
			this.outputKey = outputKey;
			return this;
		}

		public Builder outputSchema(String outputSchema) {
			this.outputSchema = outputSchema;
			return this;
		}

		public Builder advisors(List<Advisor> advisors) {
			this.advisors = advisors;
			return this;
		}

		public Builder toolCallbacks(List<ToolCallback> toolCallbacks) {
			this.toolCallbacks = toolCallbacks;
			return this;
		}

		public Builder chatClient(ChatClient chatClient) {
			this.chatClient = chatClient;
			return this;
		}

		public Builder stream(Boolean stream) {
			this.stream = stream;
			return this;
		}

		public AgentLlmNode build() {
			AgentLlmNode llmNode = new AgentLlmNode();
			llmNode.userPromptTemplate = this.userPromptTemplate;
			llmNode.systemPrompt = this.systemPrompt;
			llmNode.outputKey = this.outputKey;
			llmNode.outputSchema = this.outputSchema;
			llmNode.stream = this.stream;
			if (this.advisors != null) {
				llmNode.advisors = this.advisors;
			}
			if (this.toolCallbacks != null) {
				llmNode.toolCallbacks = this.toolCallbacks;
			}
			llmNode.chatClient = this.chatClient;
			return llmNode;
		}

	}

}
