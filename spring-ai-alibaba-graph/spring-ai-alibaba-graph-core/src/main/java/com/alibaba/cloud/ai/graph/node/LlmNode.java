/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import reactor.core.publisher.Flux;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.util.StringUtils;

public class LlmNode implements NodeAction {

	public static final String LLM_RESPONSE_KEY = "llm_response";

	private String prompt;

	private Map<String, Object> params = new HashMap<>();

	private List<Message> messages = new ArrayList<>();

	private List<Advisor> advisors = new ArrayList<>();

	private List<ToolCallback> toolCallbacks = new ArrayList<>();

	private String templateKey;

	private String paramsKey;

	private String messagesKey;

	private String outputKey;

	private ChatClient chatClient;

	public LlmNode() {
	}

	public LlmNode(String prompt, Map<String, Object> params, List<Message> messages, List<Advisor> advisors,
			List<ToolCallback> toolCallbacks, ChatClient chatClient) {
		this.prompt = prompt;
		this.params = params;
		this.messages = messages;
		this.advisors = advisors;
		this.toolCallbacks = toolCallbacks;
		this.chatClient = chatClient;
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		initNodeWithState(state);

		// add streaming support later
		ChatResponse response = call();

		Map<String, Object> updatedState = new HashMap<>();
		updatedState.put("messages", response.getResult().getOutput());
		if (StringUtils.hasLength(this.outputKey)) {
			updatedState.put(this.outputKey, response.getResult().getOutput());
		}
		return updatedState;
	}

	private void initNodeWithState(OverAllState state) {
		if (StringUtils.hasLength(templateKey)) {
			this.prompt = (String) state.value(templateKey).orElse(this.prompt);
		}
		if (StringUtils.hasLength(paramsKey)) {
			this.params = (Map<String, Object>) state.value(paramsKey).orElse(this.params);
		}
		if (StringUtils.hasLength(messagesKey)) {
			this.messages = (List<Message>) state.value(messagesKey).orElse(this.messages);
		}
		if (StringUtils.hasLength(prompt) && !params.isEmpty()) {
			this.prompt = renderPromptTemplate(prompt, params);
		}
	}

	private String renderPromptTemplate(String prompt, Map<String, Object> params) {
		PromptTemplate promptTemplate = new PromptTemplate(prompt);
		return promptTemplate.render(params);
	}

	public Flux<ChatResponse> stream() {
		return chatClient.prompt()
			.user(prompt)
			.messages(messages)
			.advisors(advisors)
			.tools(toolCallbacks)
			.stream()
			.chatResponse();
	}

	public ChatResponse call() {
		return chatClient.prompt()
			.user(prompt)
			.messages(messages)
			.advisors(advisors)
			.tools(toolCallbacks)
			.call()
			.chatResponse();
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private String promptTemplateKey;

		private String paramsKey;

		private String messagesKey;

		private String outputKey;

		private ChatClient chatClient;

		private String promptTemplate;

		private Map<String, Object> params;

		private List<Message> messages;

		private List<Advisor> advisors;

		private List<ToolCallback> toolCallbacks;

		public Builder promptTemplate(String promptTemplate) {
			this.promptTemplate = promptTemplate;
			return this;
		}

		public Builder promptTemplateKey(String promptTemplateKey) {
			this.promptTemplateKey = promptTemplateKey;
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

		public LlmNode build() {
			LlmNode llmNode = new LlmNode();
			llmNode.prompt = this.promptTemplate;
			llmNode.templateKey = this.promptTemplateKey;
			llmNode.paramsKey = this.paramsKey;
			llmNode.messagesKey = this.messagesKey;
			llmNode.outputKey = this.outputKey;
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
