/*
 * Copyright 2024-2026 the original author or authors.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.utils.FinishReasonMetadata;
import reactor.core.publisher.Flux;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.part.TextPart;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.session.SessionService;
import org.springframework.ai.session.advisor.SessionAdvisors;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import static com.alibaba.cloud.ai.graph.utils.Messageutils.convertToMessages;

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

	private String sessionIdKey;

	private List<Advisor> sessionAdvisors = List.of();

	private String outputKey;

	private String outputSchema;

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

	public static Builder builder() {
		return new Builder();
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		if (sessionIdKey != null) {
			resolveSessionId(state);
		}

		// add streaming support
		if (Boolean.TRUE.equals(stream)) {
			Flux<ChatResponse> chatResponseFlux = stream(state);
			return Map.of(StringUtils.hasLength(this.outputKey) ? this.outputKey : "messages", chatResponseFlux);
		}
		else {
			AssistantMessage responseOutput;
			try {
				ChatResponse response = call(state);
				var generation = response.getResult();
				responseOutput = FinishReasonMetadata.apply(generation.getOutput(), generation.getMetadata());
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

	private Map<String, Object> resolveParams(OverAllState state) {
		@SuppressWarnings("unchecked")
		var values = StringUtils.hasLength(paramsKey)
				? (Map<String, Object>) state.value(paramsKey).orElse(params) : params;
		// The DSL uses the string "null" to indicate a parameter that comes from state.
		Map<String, Object> resolved = new HashMap<>();
		values.forEach((key, value) -> {
			if ("null".equals(value)) {
				resolved.put(key, state.value(key).orElse(value));
			}
			else {
				resolved.put(key, value);
			}
		});
		return resolved;
	}

	private List<Message> resolveMessages(OverAllState state) {
		if (StringUtils.hasLength(messagesKey)) {
			var fromState = convertToMessages(state.value(messagesKey).orElse(null));
			if (!fromState.isEmpty()) {
				return new ArrayList<>(fromState);
			}
		}
		return new ArrayList<>(messages);
	}

	public void setToolCallbacks(List<ToolCallback> toolCallbacks) {
		this.toolCallbacks = toolCallbacks;
	}

	private String renderPromptTemplate(String prompt, Map<String, Object> params) {
		PromptTemplate promptTemplate = new PromptTemplate(prompt);
		return promptTemplate.render(params);
	}

	public Flux<ChatResponse> stream(OverAllState state) {
		return buildChatClientRequestSpec(state).stream().chatResponse();
	}

	public ChatResponse call(OverAllState state) {
		return buildChatClientRequestSpec(state).call().chatResponse();
	}

	public void augmentUserMessage(String outputSchema) {
		List<Message> updated = new ArrayList<>(messages);
		appendOutputSchema(updated, outputSchema);
		this.messages = updated;
	}

	private static void appendOutputSchema(List<Message> messages, String outputSchema) {
		if (!StringUtils.hasText(outputSchema)) {
			return;
		}
		for (int i = messages.size() - 1; i >= 0; i--) {
			if (messages.get(i) instanceof UserMessage userMessage) {
				messages.set(i, userMessage.mutate().part(TextPart.of(System.lineSeparator() + outputSchema)).build());
				return;
			}
		}
		messages.add(new UserMessage(outputSchema));
	}

	private ChatClient.ChatClientRequestSpec buildChatClientRequestSpec(OverAllState state) {
		var requestParams = resolveParams(state);
		var requestMessages = resolveMessages(state);
		String system = StringUtils.hasLength(systemPromptKey)
				? state.<String>value(systemPromptKey).orElse(systemPrompt) : systemPrompt;
		String user = StringUtils.hasLength(userPromptKey)
				? state.<String>value(userPromptKey).orElse(userPrompt) : userPrompt;

		var request = chatClient.prompt().tools(toolCallbacks).advisors(advisors);
		if (StringUtils.hasLength(system)) {
			request.system(renderPromptTemplate(system, requestParams.isEmpty() ? state.data() : requestParams));
		}
		if (StringUtils.hasLength(user)) {
			String rendered = requestParams.isEmpty() ? user : renderPromptTemplate(user, requestParams);
			request.user(StringUtils.hasText(outputSchema) ? rendered + System.lineSeparator() + outputSchema : rendered);
		}
		else {
			appendOutputSchema(requestMessages, outputSchema);
		}
		request.messages(requestMessages);

		if (sessionIdKey != null) {
			request.advisors(sessionAdvisors).advisors(SessionAdvisors.session(resolveSessionId(state)));
		}
		return request;
	}

	private String resolveSessionId(OverAllState state) {
		Object value = state.value(sessionIdKey).orElse(null);
		if (value instanceof String sessionId && StringUtils.hasText(sessionId)) {
			return sessionId;
		}
		throw new IllegalArgumentException("Missing Session Memory ID in graph state: " + sessionIdKey);
	}

	public static class Builder {

		private String systemPromptTemplateKey;

		private String userPromptTemplateKey;

		private String paramsKey;

		private String messagesKey;

		private String outputKey;

		private String outputSchema;

		private ChatClient chatClient;

		private String userPromptTemplate;

		private String systemPromptTemplate;

		private Map<String, Object> params;

		private List<Message> messages;

		private List<Advisor> advisors;

		private String sessionIdKey;

		private List<Advisor> sessionAdvisors = List.of();

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

		public Builder outputSchema(String outputSchema) {
			this.outputSchema = outputSchema;
			return this;
		}

		public Builder advisors(List<Advisor> advisors) {
			this.advisors = advisors;
			return this;
		}

		/**
		 * Loads and stores conversation history through Spring AI Session Memory.
		 * The graph state key must hold a nonblank session ID. Supply the current
		 * request as node input; the configured service supplies prior turns.
		 */
		public Builder sessionMemory(SessionService sessionService, String sessionIdKey) {
			Assert.hasText(sessionIdKey, "Session ID state key must not be blank");
			this.sessionAdvisors = SessionAdvisors.builder(sessionService).build();
			this.sessionIdKey = sessionIdKey;
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
			llmNode.outputSchema = this.outputSchema;
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
			llmNode.sessionIdKey = this.sessionIdKey;
			llmNode.sessionAdvisors = this.sessionAdvisors;
			return llmNode;
		}

	}

}
