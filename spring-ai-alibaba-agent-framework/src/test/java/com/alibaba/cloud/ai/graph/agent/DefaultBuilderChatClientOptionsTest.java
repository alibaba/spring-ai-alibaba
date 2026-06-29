/*
 * Copyright 2026-2027 the original author or authors.
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

import java.lang.reflect.Field;

import com.alibaba.cloud.ai.graph.agent.node.AgentLlmNode;
import org.junit.jupiter.api.Test;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultBuilderChatClientOptionsTest {

	@Test
	void chatClientDefaultOptionsCustomizerIsMergedWithBackingModelOptions() throws Exception {
		ChatModel chatModel = new TestChatModel(OpenAiChatOptions.builder()
				.model("configured-model")
				.temperature(0.2)
				.build());
		ChatClient chatClient = ChatClient.builder(chatModel)
				.defaultOptions(OpenAiChatOptions.builder().temperature(0.7))
				.build();

		ReactAgent agent = ReactAgent.builder()
				.name("test-agent")
				.chatClient(chatClient)
				.build();

		ChatOptions effectiveOptions = getChatOptions(agent);
		assertEquals("configured-model", effectiveOptions.getModel());
		assertEquals(0.7, effectiveOptions.getTemperature());
	}

	private static ChatOptions getChatOptions(ReactAgent agent) throws Exception {
		Field llmNodeField = ReactAgent.class.getDeclaredField("llmNode");
		llmNodeField.setAccessible(true);
		AgentLlmNode llmNode = (AgentLlmNode) llmNodeField.get(agent);

		Field chatOptionsField = AgentLlmNode.class.getDeclaredField("chatOptions");
		chatOptionsField.setAccessible(true);
		return (ChatOptions) chatOptionsField.get(llmNode);
	}

	private static final class TestChatModel implements ChatModel {

		private final ChatOptions options;

		private TestChatModel(ChatOptions options) {
			this.options = options;
		}

		@Override
		public ChatOptions getOptions() {
			return this.options;
		}

		@Override
		public ChatResponse call(Prompt prompt) {
			throw new UnsupportedOperationException("This test only verifies ChatClient default options.");
		}

	}

}
