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

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.tools.ToolContextHelper;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AgentToolCallingAdvisorBoundaryTest {

	@Test
	void toolCallsShouldBeExecutedByAgentToolNodeNotChatClientAutoAdvisor() throws Exception {
		ToolCallThenFinalChatModel chatModel = new ToolCallThenFinalChatModel();
		BoundaryTools tools = new BoundaryTools();
		ToolCallback toolCallback = ToolCallbacks.from(tools)[0];

		ReactAgent agent = ReactAgent.builder()
				.name("tool-boundary-agent")
				.model(chatModel)
				.tools(toolCallback)
				.saver(new MemorySaver())
				.build();

		AssistantMessage result = agent.call("call the boundary tool",
				RunnableConfig.builder().threadId("tool-boundary-thread").addMetadata("_stream_", false).build());

		assertEquals("final response", result.getText());
		assertEquals(2, chatModel.callCount.get());
		assertEquals(1, tools.agentToolNodeExecutions.get());
		assertEquals(0, tools.chatClientAdvisorExecutions.get());
	}

	private static final class ToolCallThenFinalChatModel implements ChatModel {

		private final AtomicInteger callCount = new AtomicInteger();

		@Override
		public ChatResponse call(Prompt prompt) {
			if (callCount.incrementAndGet() == 1) {
				AssistantMessage.ToolCall toolCall = new AssistantMessage.ToolCall("call-1", "function",
						"boundary_tool", "{}");
				return new ChatResponse(List.of(new Generation(
						AssistantMessage.builder().content("").toolCalls(List.of(toolCall)).build())));
			}
			return new ChatResponse(List.of(new Generation(new AssistantMessage("final response"))));
		}

		@Override
		public Flux<ChatResponse> stream(Prompt prompt) {
			return Flux.just(call(prompt));
		}
	}

	private static final class BoundaryTools {

		private final AtomicInteger agentToolNodeExecutions = new AtomicInteger();

		private final AtomicInteger chatClientAdvisorExecutions = new AtomicInteger();

		@Tool(name = "boundary_tool", description = "Boundary test tool")
		String boundaryTool(ToolContext toolContext) {
			if (ToolContextHelper.getConfig(toolContext).isPresent()) {
				agentToolNodeExecutions.incrementAndGet();
			}
			else {
				chatClientAdvisorExecutions.incrementAndGet();
			}
			return "tool result";
		}
	}

}
