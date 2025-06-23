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
import com.alibaba.cloud.ai.graph.streaming.StreamingChatGenerator;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class LlmNode implements NodeAction {

	public static final String LLM_RESPONSE_KEY = "llm_response";

	private String systemPrompt;

	private String userPrompt;

	private Map<String, Object> params = new HashMap<>();

	private List<Message> messages = new ArrayList<>();

	private List<Advisor> advisors = new ArrayList<>();

	private List<ToolCallback> toolCallbacks = new ArrayList<>();

	private String systemPromptKey;

	private String userPromptKey;

	private String paramsKey;

	private String messagesKey;

	private String outputKey;

	private ChatClient chatClient;

	private Boolean stream = Boolean.FALSE;

	public LlmNode() {
	}

	public LlmNode(String systemPrompt, String prompt, Map<String, Object> params, List<Message> messages,
			List<Advisor> advisors, List<ToolCallback> toolCallbacks, ChatClient chatClient, boolean stream) {
		this.systemPrompt = systemPrompt;
		this.userPrompt = prompt;
		this.params = params;
		this.messages = messages;
		this.advisors = advisors;
		this.toolCallbacks = toolCallbacks;
		this.chatClient = chatClient;
		this.stream = stream;
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		initNodeWithState(state);

		// add streaming support
		if (Boolean.TRUE.equals(stream)) {
			Flux<ChatResponse> chatResponseFlux = stream();
			var generator = StreamingChatGenerator.builder()
				.startingNode("llmNode")
				.startingState(state)
				.mapResult(response -> Map.of(StringUtils.hasLength(this.outputKey) ? this.outputKey : "messages",
						Objects.requireNonNull(response.getResult().getOutput().getText())))
				.build(chatResponseFlux);
			return Map.of(StringUtils.hasLength(this.outputKey) ? this.outputKey : "messages", generator);
		}
		else {
			ChatResponse response = call();

			Map<String, Object> updatedState = new HashMap<>();
			updatedState.put("messages", response.getResult().getOutput());
			if (StringUtils.hasLength(this.outputKey)) {
				updatedState.put(this.outputKey, response.getResult().getOutput());
			}
			return updatedState;
		}
	}

	private void initNodeWithState(OverAllState state) {
		if (StringUtils.hasLength(userPromptKey)) {
			this.userPrompt = (String) state.value(userPromptKey).orElse(this.userPrompt);
		}
		if (StringUtils.hasLength(systemPromptKey)) {
			this.systemPrompt = (String) state.value(systemPromptKey).orElse(this.systemPrompt);
		}
		if (StringUtils.hasLength(paramsKey)) {
			this.params = (Map<String, Object>) state.value(paramsKey).orElse(this.params);
		}
		if (StringUtils.hasLength(messagesKey)) {
			this.messages = (List<Message>) state.value(messagesKey).orElse(this.messages);
		}
		if (StringUtils.hasLength(userPrompt) && !params.isEmpty()) {
			this.userPrompt = renderPromptTemplate(userPrompt, params);
		}
	}

	private String renderPromptTemplate(String prompt, Map<String, Object> params) {
		PromptTemplate promptTemplate = new PromptTemplate(prompt);
		return promptTemplate.render(params);
	}

	public Flux<ChatResponse> stream() {
		if (StringUtils.hasLength(systemPrompt) && StringUtils.hasLength(userPrompt)) {
			return chatClient.prompt()
				.system(systemPrompt)
				.user(userPrompt)
				.messages(messages)
				.advisors(advisors)
				.toolCallbacks(toolCallbacks)
				.stream()
				.chatResponse();
		}
		else {
			if (StringUtils.hasLength(systemPrompt)) {
				return chatClient.prompt()
					.system(systemPrompt)
					.messages(messages)
					.advisors(advisors)
					.toolCallbacks(toolCallbacks)
					.stream()
					.chatResponse();
			}
			else if (StringUtils.hasLength(userPrompt)) {
				return chatClient.prompt()
					.user(userPrompt)
					.messages(messages)
					.advisors(advisors)
					.toolCallbacks(toolCallbacks)
					.stream()
					.chatResponse();
			}
			else {
				return chatClient.prompt()
					.messages(messages)
					.advisors(advisors)
					.toolCallbacks(toolCallbacks)
					.stream()
					.chatResponse();
			}
		}
	}

	public ChatResponse call() {

		if (StringUtils.hasLength(systemPrompt) && StringUtils.hasLength(userPrompt)) {
			return chatClient.prompt()
				.system(systemPrompt)
				.user(userPrompt)
				.messages(messages)
				.advisors(advisors)
				.toolCallbacks(toolCallbacks)
				.call()
				.chatResponse();
		}
		else {
			if (StringUtils.hasLength(systemPrompt)) {
				return chatClient.prompt()
					.system(systemPrompt)
					.messages(messages)
					.advisors(advisors)
					.toolCallbacks(toolCallbacks)
					.call()
					.chatResponse();
			}
			else if (StringUtils.hasLength(userPrompt)) {
				return chatClient.prompt()
					.user(userPrompt)
					.messages(messages)
					.advisors(advisors)
					.toolCallbacks(toolCallbacks)
					.call()
					.chatResponse();
			}
			else {
				return chatClient.prompt()
					.messages(messages)
					.advisors(advisors)
					.toolCallbacks(toolCallbacks)
					.call()
					.chatResponse();
			}
		}
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private String systemPromptTemplateKey;

		private String userPromptTemplateKey;

		private String paramsKey;

		private String messagesKey;

		private String outputKey;

		private ChatClient chatClient;

		private String userPromptTemplate;

		private String systemPromptTemplate;

		private Map<String, Object> params;

		private List<Message> messages;

		private List<Advisor> advisors;

		private List<ToolCallback> toolCallbacks;

		private Boolean stream;

		public Builder userPromptTemplate(String userPromptTemplate) {
			this.userPromptTemplate = userPromptTemplate;
			return this;
		}

		public Builder systemPromptTemplate(String systemPromptTemplate) {
			this.systemPromptTemplate = systemPromptTemplate;
			return this;
		}

		public Builder userPromptTemplateKey(String userPromptTemplateKey) {
			this.userPromptTemplateKey = userPromptTemplateKey;
			return this;
		}

		public Builder systemPromptTemplateKey(String systemPromptTemplateKey) {
			this.systemPromptTemplateKey = systemPromptTemplateKey;
			return this;
		}

		public Builder params(Map<String, String> params) {
			this.params = new HashMap<>(params);
			return this;
		}

		public Builder paramsKey(String paramsKey) {
			this.paramsKey = paramsKey;
			return this;
		}

		public Builder messagesKey(String messagesKey) {
			this.messagesKey = messagesKey;
			return this;
		}

		public Builder messages(List<Message> messages) {
			this.messages = messages;
			return this;
		}

		public Builder outputKey(String outputKey) {
			this.outputKey = outputKey;
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

		public LlmNode build() {
			LlmNode llmNode = new LlmNode();
			llmNode.systemPrompt = this.systemPromptTemplate;
			llmNode.userPrompt = this.userPromptTemplate;
			llmNode.systemPromptKey = this.systemPromptTemplateKey;
			llmNode.userPromptKey = this.userPromptTemplateKey;
			llmNode.paramsKey = this.paramsKey;
			llmNode.messagesKey = this.messagesKey;
			llmNode.outputKey = this.outputKey;
			llmNode.stream = this.stream;
			if (this.params != null) {
				llmNode.params = this.params;
			}
			if (this.messages != null) {
				llmNode.messages = this.messages;
			}
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
