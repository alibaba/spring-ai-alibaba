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
package com.alibaba.cloud.ai.graph.agent.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class AgentLlmNodeInstructionBehaviorTest {

	@Test
	void applyShouldKeepInstructionAsRawUserMessage() throws Exception {
		PromptCapturingChatModel chatModel = new PromptCapturingChatModel();
		AgentLlmNode node = AgentLlmNode.builder()
				.agentName("main-agent")
				.chatClient(ChatClient.builder(chatModel).build())
				.instruction("route by {input}")
				.build();

		node.apply(new OverAllState(Map.of("input", "hello")), RunnableConfig.builder().addMetadata("_stream_", false).build());

		List<Message> messages = chatModel.lastPrompt().getInstructions();
		UserMessage instruction = assertInstanceOf(UserMessage.class, messages.get(0));
		assertEquals("route by {input}", instruction.getText());
	}

	private static final class PromptCapturingChatModel implements ChatModel {

		private volatile Prompt lastPrompt;

		@Override
		public ChatResponse call(Prompt prompt) {
			this.lastPrompt = prompt;
			return new ChatResponse(List.of(new Generation(new AssistantMessage("ok"))));
		}

		@Override
		public Flux<ChatResponse> stream(Prompt prompt) {
			this.lastPrompt = prompt;
			return Flux.just(new ChatResponse(List.of(new Generation(new AssistantMessage("ok")))));
		}

		Prompt lastPrompt() {
			return lastPrompt;
		}

	}

}
