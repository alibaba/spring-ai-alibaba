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
import com.alibaba.cloud.ai.graph.serializer.AgentInstructionMessage;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentLlmNodeTemplateRenderingTest {

	@Test
	void applyShouldRenderInstructionTemplateWithInput() throws Exception {
		PromptCapturingChatModel chatModel = new PromptCapturingChatModel();
		AgentLlmNode node = AgentLlmNode.builder()
				.agentName("main_agent")
				.chatClient(ChatClient.builder(chatModel).build())
				.instruction("用户的请求是: {input}")
				.build();

		OverAllState state = new OverAllState(Map.of("input", "写一首关于春天的诗"));
		RunnableConfig config = RunnableConfig.builder().addMetadata("_stream_", false).build();

		node.apply(state, config);

		List<Message> instructions = chatModel.lastPrompt().getInstructions();
		assertTrue(instructions.size() >= 2, "Prompt should contain instruction and user input message");
		UserMessage instruction = assertInstanceOf(UserMessage.class, instructions.get(0));
		assertEquals("用户的请求是: 写一首关于春天的诗", instruction.getText());
	}

	@Test
	void applyShouldFallbackToRawInstructionWhenTemplateRenderingFails() throws Exception {
		PromptCapturingChatModel chatModel = new PromptCapturingChatModel();
		String rawInstruction = "按 JSON 返回: {\"answer\": 1}";
		AgentLlmNode node = AgentLlmNode.builder()
				.agentName("main_agent")
				.chatClient(ChatClient.builder(chatModel).build())
				.instruction(rawInstruction)
				.build();

		OverAllState state = new OverAllState(Map.of("input", "ignored"));
		RunnableConfig config = RunnableConfig.builder().addMetadata("_stream_", false).build();

		node.apply(state, config);

		List<Message> instructions = chatModel.lastPrompt().getInstructions();
		UserMessage instruction = assertInstanceOf(UserMessage.class, instructions.get(0));
		assertEquals(rawInstruction, instruction.getText());
	}

	@Test
	void renderTemplatedUserMessageShouldConvertMessageParamsAndIgnoreListParams() {
		PromptCapturingChatModel chatModel = new PromptCapturingChatModel();
		AgentLlmNode node = AgentLlmNode.builder()
				.agentName("main_agent")
				.chatClient(ChatClient.builder(chatModel).build())
				.build();

		AgentInstructionMessage instruction = AgentInstructionMessage.builder()
				.text("input={input}, article={article}")
				.rendered(false)
				.build();
		List<Message> messages = new ArrayList<>();
		messages.add(instruction);

		node.renderTemplatedUserMessage(messages, Map.of(
				"input", "topic",
				"article", new UserMessage("from-message"),
				"ignored_list", List.of("a", "b")), java.util.Optional.empty());

		AgentInstructionMessage rendered = assertInstanceOf(AgentInstructionMessage.class, messages.get(0));
		assertTrue(rendered.isRendered(), "Templated message should be marked rendered");
		assertEquals("input=topic, article=from-message", rendered.getText());
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
