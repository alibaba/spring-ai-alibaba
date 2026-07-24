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
package com.alibaba.cloud.ai.graph.agent;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.annotation.Tool;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReactAgentReturnDirectRoutingTest {

	@Test
	void returnDirectToolDoesNotShortCircuitLaterTurns() throws Exception {
		ToolCallingModel model = new ToolCallingModel("directAnswer", "second turn reached the model");
		DirectTools tools = new DirectTools();
		RunnableConfig config = RunnableConfig.builder().threadId("return-direct-thread").build();

		ReactAgent agent = ReactAgent.builder()
				.name("return_direct_agent")
				.model(model)
				.tools(ToolCallbacks.from(tools))
				.saver(new MemorySaver())
				.build();

		AssistantMessage firstResponse = agent.call("call the direct tool", config);

		assertTrue(firstResponse.getText().contains("direct response"));
		assertEquals(1, model.callCount());
		assertEquals(1, tools.callCount());

		AssistantMessage secondResponse = agent.call("start a new turn", config);

		assertEquals("second turn reached the model", secondResponse.getText());
		assertEquals(2, model.callCount());
		assertEquals(1, tools.callCount());
	}

	@Test
	void normalToolStillReturnsToModel() throws Exception {
		ToolCallingModel model = new ToolCallingModel("normalAnswer", "model processed tool response");
		NormalTools tools = new NormalTools();

		ReactAgent agent = ReactAgent.builder()
				.name("normal_tool_agent")
				.model(model)
				.tools(ToolCallbacks.from(tools))
				.saver(new MemorySaver())
				.build();

		AssistantMessage response = agent.call("call the normal tool");

		assertEquals("model processed tool response", response.getText());
		assertEquals(2, model.callCount());
		assertEquals(1, tools.callCount());
	}

	private static final class ToolCallingModel implements ChatModel {

		private final String toolName;

		private final String secondResponse;

		private final AtomicInteger callCount = new AtomicInteger();

		private ToolCallingModel(String toolName, String secondResponse) {
			this.toolName = toolName;
			this.secondResponse = secondResponse;
		}

		@Override
		public ChatResponse call(Prompt prompt) {
			int currentCall = callCount.incrementAndGet();
			AssistantMessage message = currentCall == 1 ? toolCallMessage(toolName) : new AssistantMessage(secondResponse);
			return new ChatResponse(List.of(new Generation(message)));
		}

		@Override
		public Flux<ChatResponse> stream(Prompt prompt) {
			return Flux.just(call(prompt));
		}

		private int callCount() {
			return callCount.get();
		}

		private static AssistantMessage toolCallMessage(String toolName) {
			return AssistantMessage.builder()
					.content("")
					.toolCalls(List.of(new AssistantMessage.ToolCall("call-1", "function", toolName, "{}")))
					.build();
		}
	}

	private static final class DirectTools {

		private final AtomicInteger callCount = new AtomicInteger();

		@Tool(name = "directAnswer", description = "Return a direct answer", returnDirect = true)
		public String directAnswer() {
			callCount.incrementAndGet();
			return "direct response";
		}

		private int callCount() {
			return callCount.get();
		}
	}

	private static final class NormalTools {

		private final AtomicInteger callCount = new AtomicInteger();

		@Tool(name = "normalAnswer", description = "Return a normal answer", returnDirect = false)
		public String normalAnswer() {
			callCount.incrementAndGet();
			return "normal response";
		}

		private int callCount() {
			return callCount.get();
		}
	}

}
