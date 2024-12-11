/*
 * Copyright 2024 the original author or authors.
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

import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.state.AgentState;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.Generation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author 北极星
 */
public class LLMNodeAction<State extends AgentState> extends AbstractNode implements NodeAction<State> {

	/**
	 * each llm node has their own state
	 */
	public static final String MESSAGES_KEY = "messages";

	public ChatClient chatClient;

	public LLMNodeAction(ChatClient chatClient) {
		this.chatClient = chatClient;
	}

	public static Builder builder(ChatModel chatModel) {

		return new Builder(chatModel);
	}

	/**
	 * @param state
	 * @return Map<String, Object>
	 * @throws Exception
	 */
	@Override
	public Map<String, Object> apply(State state) throws Exception {

		List<Message> messages = state.value(MESSAGES_KEY, new ArrayList<>());
		List<Generation> generations = chatClient.prompt()
			.user(s -> s.params(state.data()))
			.messages(messages)
			.call()
			.chatResponse()
			.getResults();
		List<Message> output = generations.stream().map(Generation::getOutput).collect(Collectors.toList());
		return Map.of(MESSAGES_KEY, output);
	}

	public static class Builder {

		protected ChatModel chatModel;

		protected String sysPrompt;

		protected String[] functions;

		public Builder(ChatModel chatModel) {
			this.chatModel = chatModel;
		}

		public <State extends AgentState> LLMNodeAction<AgentState> build() {
			ChatClient.Builder builder = ChatClient.builder(this.chatModel);
			String sysPr = Optional.ofNullable(sysPrompt)
				.orElse("{'role': 'system', 'content': 'You are a helpful assistant.'}");
			builder.defaultSystem(sysPr);
			if (functions != null && functions.length > 0)
				builder.defaultFunctions(functions);
			return new LLMNodeAction<>(builder.build());
		}

		public Builder withSysPrompt(String prompt) {
			this.sysPrompt = prompt;
			return this;
		}

		public Builder withFunctions(String... functionNames) {
			if (functionNames == null || functionNames.length == 0)
				return this;
			this.functions = functionNames;
			return this;
		}

	}

}
