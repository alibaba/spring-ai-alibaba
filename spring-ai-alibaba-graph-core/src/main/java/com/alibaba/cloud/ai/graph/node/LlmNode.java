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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import reactor.core.publisher.Flux;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.util.StringUtils;

public class LlmNode implements NodeAction {

	public static final String LLM_RESPONSE_KEY = "llm_response";

	private static final ObjectMapper objectMapper = new ObjectMapper();

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

	public static Builder builder() {
		return new Builder();
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		initNodeWithState(state);

		// add streaming support
		if (Boolean.TRUE.equals(stream)) {
			Flux<ChatResponse> chatResponseFlux = stream();
			return Map.of(StringUtils.hasLength(this.outputKey) ? this.outputKey : "messages", chatResponseFlux);
		}
		else {
			AssistantMessage responseOutput;
			try {
				ChatResponse response = call();
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
		// Used for adapting the dify's DSL conversion
		if (!this.params.isEmpty()) {
			Map<String, Object> rawParams = this.params;
			Map<String, Object> filledParams = new HashMap<>();
			for (Map.Entry<String, Object> entry : rawParams.entrySet()) {
				if (entry.getValue().equals("null")) {
					Optional<Object> valueFromState = state.value(entry.getKey());
					filledParams.put(entry.getKey(), valueFromState.orElse(entry.getValue()));
				}
				else {
					filledParams.put(entry.getKey(), entry.getValue());
				}
			}

			this.params = filledParams;
		}
		if (StringUtils.hasLength(messagesKey)) {
			Object messagesValue = state.value(messagesKey).orElse(null);
			if (messagesValue != null) {
				List<Message> convertedMessages = convertToMessages(messagesValue);
				this.messages = convertedMessages.isEmpty() ? this.messages : convertedMessages;
			}
		}
		if (StringUtils.hasLength(userPrompt) && !params.isEmpty()) {
			this.userPrompt = renderPromptTemplate(userPrompt, params);
		}
	}

	public void setToolCallbacks(List<ToolCallback> toolCallbacks) {
		this.toolCallbacks = toolCallbacks;
	}

	private String renderPromptTemplate(String prompt, Map<String, Object> params) {
		PromptTemplate promptTemplate = new PromptTemplate(prompt);
		return promptTemplate.render(params);
	}

	public Flux<ChatResponse> stream() {
		return buildChatClientRequestSpec().stream().chatResponse();
	}

	public ChatResponse call() {
		return buildChatClientRequestSpec().call().chatResponse();
	}

	private ChatClient.ChatClientRequestSpec buildChatClientRequestSpec() {
		ChatClient.ChatClientRequestSpec chatClientRequestSpec = chatClient.prompt()
				.toolCallbacks(toolCallbacks)
				.messages(messages)
				.advisors(advisors);

		if (StringUtils.hasLength(systemPrompt)) {
			if (!params.isEmpty()) {
				systemPrompt = renderPromptTemplate(systemPrompt, params);
			}
			chatClientRequestSpec.system(systemPrompt);
		}

		if (StringUtils.hasLength(userPrompt)) {
			if (!params.isEmpty()) {
				userPrompt = renderPromptTemplate(userPrompt, params);
			}
			chatClientRequestSpec.user(userPrompt);
		}

		return chatClientRequestSpec;
	}

	/**
	 * 通用方法，将各种类型的消息转换为 List<Message> 支持的类型： 1. String - 转换为 UserMessage 2. Map (如
	 * {"role": "user", "text": "测试"}) - 根据role创建相应的Message 3. Spring AI Message - 直接使用 4.
	 * List - 递归处理列表中的每个元素
	 */
	private List<Message> convertToMessages(Object value) {
		List<Message> result = new ArrayList<>();

		if (value == null) {
			return result;
		}

		if (value instanceof List<?>) {
			List<?> list = (List<?>) value;
			if (list.isEmpty()) {
				return result;
			}

			for (Object item : list) {
				result.addAll(convertToMessages(item));
			}
		}
		else if (value instanceof Message) {
			// 如果已经是 Spring AI Message，直接添加
			result.add((Message) value);
		}
		else if (value instanceof String) {
			// 如果是字符串，转换为 UserMessage
			String text = (String) value;
			if (StringUtils.hasLength(text)) {
				result.add(new UserMessage(text));
			}
		}
		else if (value instanceof Map) {
			// 如果是 Map，尝试解析为 Message
			Map<?, ?> map = (Map<?, ?>) value;
			Message message = convertMapToMessage(map);
			if (message != null) {
				result.add(message);
			}
		}
		else {
			// 对于其他类型，尝试通过 JSON 序列化/反序列化转换
			try {
				String json = objectMapper.writeValueAsString(value);
				JsonNode jsonNode = objectMapper.readTree(json);
				if (jsonNode.isObject()) {
					Message message = convertJsonNodeToMessage(jsonNode);
					if (message != null) {
						result.add(message);
					}
				}
			}
			catch (Exception e) {
				// 如果转换失败，将其作为字符串处理
				String text = value.toString();
				if (StringUtils.hasLength(text)) {
					result.add(new UserMessage(text));
				}
			}
		}

		return result;
	}

	/**
	 * 将 Map 转换为 Message
	 */
	private Message convertMapToMessage(Map<?, ?> map) {
		if (map == null || map.isEmpty()) {
			return null;
		}

		Object roleObj = map.get("role");
		Object textObj = map.get("text");
		Object contentObj = map.get("content");

		// 优先使用 content，然后是 text
		String content = null;
		if (contentObj != null) {
			content = contentObj.toString();
		}
		else if (textObj != null) {
			content = textObj.toString();
		}

		if (!StringUtils.hasLength(content)) {
			return null;
		}

		String role = roleObj != null ? roleObj.toString().toLowerCase() : "user";

		switch (role) {
			case "system":
				return new SystemMessage(content);
			case "assistant":
				return new AssistantMessage(content);
			case "user":
			default:
				return new UserMessage(content);
		}
	}

	/**
	 * 将 JsonNode 转换为 Message
	 */
	private Message convertJsonNodeToMessage(JsonNode jsonNode) {
		if (jsonNode == null || !jsonNode.isObject()) {
			return null;
		}

		JsonNode roleNode = jsonNode.get("role");
		JsonNode textNode = jsonNode.get("text");
		JsonNode contentNode = jsonNode.get("content");

		// 优先使用 content，然后是 text
		String content = null;
		if (contentNode != null && contentNode.isTextual()) {
			content = contentNode.asText();
		}
		else if (textNode != null && textNode.isTextual()) {
			content = textNode.asText();
		}

		if (!StringUtils.hasLength(content)) {
			return null;
		}

		String role = roleNode != null && roleNode.isTextual() ? roleNode.asText().toLowerCase() : "user";

		return switch (role) {
			case "system" -> new SystemMessage(content);
			case "assistant" -> new AssistantMessage(content);
			case "user" -> new UserMessage(content);
			default -> new UserMessage(content);
		};
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
