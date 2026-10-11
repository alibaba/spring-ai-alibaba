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

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.agent.tools.ToolContextHelper;
import com.alibaba.cloud.ai.graph.agent.tools.ToolStreamingChunk;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Agent tool streaming integration tests")
class AgentToolStreamingReactAgentTest {

	@Test
	@DisplayName("stream should expose tool progress while streamMessages stays message-only")
	void streamShouldExposeToolProgressButStreamMessagesShouldNot() throws Exception {
		ReactAgent streamAgent = createAgentWithStreamingTool(new TwoStepToolCallChatModel());
		List<NodeOutput> outputs = streamAgent.stream("run the tool")
			.collectList()
			.block(Duration.ofSeconds(5));

		assertFalse(outputs.isEmpty());
		List<StreamingOutput<?>> streamingOutputs = outputs.stream()
			.filter(output -> output instanceof StreamingOutput<?>)
			.map(output -> (StreamingOutput<?>) output)
			.collect(ArrayList::new, List::add, List::addAll);

		List<StreamingOutput<?>> toolProgressOutputs = streamingOutputs.stream()
			.filter(output -> output.getOutputType() == OutputType.AGENT_TOOL_STREAMING)
			.collect(ArrayList::new, List::add, List::addAll);
		assertEquals(2, toolProgressOutputs.size());
		assertTrue(toolProgressOutputs.stream().allMatch(output -> output.message() == null));
		assertTrue(toolProgressOutputs.stream()
			.map(output -> (ToolStreamingChunk) output.getOriginData())
			.map(ToolStreamingChunk::content)
			.toList()
			.containsAll(List.of("progress-1", "progress-2")));

		StreamingOutput<?> finishedOutput = streamingOutputs.stream()
			.filter(output -> output.getOutputType() == OutputType.AGENT_TOOL_FINISHED)
			.findFirst()
			.orElseThrow();
		assertInstanceOf(ToolResponseMessage.class, finishedOutput.message());

		ReactAgent messageAgent = createAgentWithStreamingTool(new TwoStepToolCallChatModel());
		List<Message> messages = messageAgent.streamMessages("run the tool")
			.collectList()
			.block(Duration.ofSeconds(5));

		assertFalse(messages.isEmpty());
		assertTrue(messages.stream().anyMatch(ToolResponseMessage.class::isInstance));
		assertTrue(messages.stream().noneMatch(message -> message instanceof AssistantMessage assistantMessage
				&& "progress-1".equals(assistantMessage.getText())));
		assertTrue(messages.stream().noneMatch(message -> message instanceof AssistantMessage assistantMessage
				&& "progress-2".equals(assistantMessage.getText())));
	}

	private ReactAgent createAgentWithStreamingTool(ChatModel model) {
		ToolCallback tool = new ToolCallback() {
			@Override
			public ToolDefinition getToolDefinition() {
				return ToolDefinition.builder().name("progress_tool").description("Progress tool").inputSchema("{}").build();
			}

			@Override
			public String call(String toolInput, ToolContext toolContext) {
				ToolContextHelper.getToolProgressEmitter(toolContext).ifPresent(emitter -> {
					emitter.next("progress-1");
					emitter.next("progress-2");
				});
				return "tool-result";
			}

			@Override
			public String call(String toolInput) {
				return call(toolInput, new ToolContext(Map.of()));
			}
		};

		return ReactAgent.builder()
			.name("streaming_tool_agent")
			.model(model)
			.instruction("Use the progress_tool when asked, then finish with a short answer.")
			.tools(tool)
			.saver(new MemorySaver())
			.build();
	}

	private static final class TwoStepToolCallChatModel implements ChatModel {

		private final AtomicInteger invocationCount = new AtomicInteger();

		@Override
		public ChatResponse call(Prompt prompt) {
			return nextResponse();
		}

		@Override
		public Flux<ChatResponse> stream(Prompt prompt) {
			return Flux.just(nextResponse());
		}

		private ChatResponse nextResponse() {
			int invocation = invocationCount.incrementAndGet();
			if (invocation == 1) {
				AssistantMessage assistantMessage = AssistantMessage.builder()
					.content("")
					.toolCalls(List.of(new AssistantMessage.ToolCall("call-1", "function", "progress_tool", "{}")))
					.build();
				return new ChatResponse(List.of(new Generation(assistantMessage)));
			}
			return new ChatResponse(List.of(new Generation(new AssistantMessage("all done"))));
		}
	}

}
