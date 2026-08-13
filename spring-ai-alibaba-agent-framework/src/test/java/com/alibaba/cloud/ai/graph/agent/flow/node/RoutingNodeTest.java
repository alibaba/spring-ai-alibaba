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
package com.alibaba.cloud.ai.graph.agent.flow.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.MultiCommand;
import com.alibaba.cloud.ai.graph.agent.Agent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.LlmRoutingAgent;

import org.junit.jupiter.api.Test;
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
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RoutingNodeTest {

	@Test
	void routesToConfiguredFallbackAfterInvalidDecisions() throws Exception {
		AtomicInteger modelCalls = new AtomicInteger();
		ChatModel chatModel = invalidRoutingModel(modelCalls);
		Agent writerAgent = mockAgent();
		LlmRoutingAgent routingAgent = routingAgent(chatModel, writerAgent, "writer_agent");
		RoutingNode node = new RoutingNode(chatModel, routingAgent, List.of(writerAgent));
		OverAllState state = new OverAllState(Map.of(
				"input", "write a summary",
				"messages", List.<Message>of(new UserMessage("write a summary"))));

		MultiCommand command = node.apply(state, RunnableConfig.builder().build());

		assertEquals(3, modelCalls.get());
		assertEquals(List.of("writer_agent"), command.gotoNodes());
		assertEquals("write a summary", command.update().get("writer_agent_input"));
	}

	@Test
	void throwsAfterInvalidDecisionsWhenFallbackIsNotConfigured() {
		ChatModel chatModel = invalidRoutingModel(new AtomicInteger());
		Agent writerAgent = mockAgent();
		LlmRoutingAgent routingAgent = routingAgent(chatModel, writerAgent, null);
		RoutingNode node = new RoutingNode(chatModel, routingAgent, List.of(writerAgent));
		OverAllState state = new OverAllState(Map.of(
				"messages", List.<Message>of(new UserMessage("write a summary"))));

		assertThrows(IllegalStateException.class,
				() -> node.apply(state, RunnableConfig.builder().build()));
	}

	private static ChatModel invalidRoutingModel(AtomicInteger modelCalls) {
		return new ChatModel() {
			@Override
			public ChatResponse call(Prompt prompt) {
				modelCalls.incrementAndGet();
				return new ChatResponse(List.of(new Generation(new AssistantMessage(
						"{\"agents\":[{\"agent\":\"unknown_agent\",\"query\":\"ignored\"}]}"))));
			}

			@Override
			public Flux<ChatResponse> stream(Prompt prompt) {
				return Flux.just(call(prompt));
			}
		};
	}

	private static Agent mockAgent() {
		Agent writerAgent = mock(Agent.class);
		when(writerAgent.name()).thenReturn("writer_agent");
		when(writerAgent.description()).thenReturn("Writes text");
		return writerAgent;
	}

	private static LlmRoutingAgent routingAgent(ChatModel chatModel, Agent writerAgent, String fallbackAgent) {
		LlmRoutingAgent.LlmRoutingAgentBuilder builder = LlmRoutingAgent.builder()
				.name("router")
				.description("Routes requests")
				.model(chatModel)
				.subAgents(List.of(writerAgent));
		if (fallbackAgent != null) {
			builder.fallbackAgent(fallbackAgent);
		}
		return builder.build();
	}

}
