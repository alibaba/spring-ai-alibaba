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

import com.alibaba.cloud.ai.graph.OverAllState;
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
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import reactor.core.publisher.Flux;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class
AgentToolRunnableConfigPropagationTest {

	@Test
	void subAgentShouldKeepParentMetadataAndOverrideAgentName() throws Exception {
		AtomicReference<String> capturedUserId = new AtomicReference<>();
		AtomicReference<String> capturedAgentName = new AtomicReference<>();

		BiFunction<InspectRuntimeRequest, ToolContext, String> inspectRuntime = (request, toolContext) -> {
			RunnableConfig config = ToolContextHelper.getConfig(toolContext)
				.orElseThrow(() -> new AssertionError("RunnableConfig should be available in child tool context"));
			capturedUserId.set((String) config.metadata("userId").orElse(null));
			capturedAgentName.set((String) config.metadata(RunnableConfig.AGENT_NAME_KEY).orElse(null));
			System.out.println("[AgentToolRunnableConfigPropagationTest] child tool captured userId="
					+ capturedUserId.get() + ", agentName=" + capturedAgentName.get());
			return "userId=" + capturedUserId.get() + ", agent=" + capturedAgentName.get();
		};

		ToolCallback inspectRuntimeTool = FunctionToolCallback.builder("inspect_runtime", inspectRuntime)
			.description("Inspect runtime metadata for the current agent invocation")
			.inputType(InspectRuntimeRequest.class)
			.build();

		ScriptedChatModel childModel = new ScriptedChatModel(List.of(
				toolCallResponse("child-call-1", "inspect_runtime", "{\"note\":\"check runtime\"}"),
				textResponse("child done")));

		ReactAgent childAgent = ReactAgent.builder()
			.name("metadata_reporter")
			.model(childModel)
			.description("Inspects runtime metadata")
			.instruction("Call inspect_runtime and return the inspection result.")
			.tools(List.of(inspectRuntimeTool))
			.saver(new MemorySaver())
			.build();

		ScriptedChatModel parentModel = new ScriptedChatModel(List.of(
				toolCallResponse("parent-call-1", "metadata_reporter", "{\"input\":\"inspect metadata\"}"),
				textResponse("parent done")));

		ReactAgent parentAgent = ReactAgent.builder()
			.name("supervisor_agent")
			.model(parentModel)
			.instruction("Delegate runtime inspection to metadata_reporter.")
			.tools(List.of(AgentTool.getFunctionToolCallback(childAgent)))
			.saver(new MemorySaver())
			.build();

		RunnableConfig config = RunnableConfig.builder()
			.threadId("parent-thread")
			.addMetadata("userId", "user-42")
			.build();

		Optional<OverAllState> result = parentAgent.invoke(
				List.of(new org.springframework.ai.chat.messages.UserMessage("inspect runtime metadata")), config);

		System.out.println("[AgentToolRunnableConfigPropagationTest] final captured values: userId="
				+ capturedUserId.get() + ", agentName=" + capturedAgentName.get());

		assertTrue(result.isPresent(), "Parent agent should produce a result");
		assertEquals("user-42", capturedUserId.get(), "Child agent should inherit parent metadata");
		assertEquals("metadata_reporter", capturedAgentName.get(),
				"Child agent should override _AGENT_ with its own name");
		assertEquals(2, parentModel.callCount(), "Parent model should be called twice");
		assertEquals(2, childModel.callCount(), "Child model should be called twice");

		List<?> messages = result.get().value("messages", List.class).orElseThrow();
		assertNotNull(messages, "Messages should be present in the final state");
	}

	private static ChatResponse textResponse(String text) {
		return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
	}

	private static ChatResponse toolCallResponse(String id, String name, String arguments) {
		AssistantMessage.ToolCall toolCall = new AssistantMessage.ToolCall(id, "function", name, arguments);
		AssistantMessage assistantMessage = AssistantMessage.builder()
			.content("")
			.toolCalls(List.of(toolCall))
			.build();
		return new ChatResponse(List.of(new Generation(assistantMessage)));
	}

	private record InspectRuntimeRequest(String note) {
	}

	private static final class ScriptedChatModel implements ChatModel {

		private final Queue<ChatResponse> responses;

		private final AtomicInteger callCount = new AtomicInteger();

		private ScriptedChatModel(List<ChatResponse> responses) {
			this.responses = new ArrayDeque<>(responses);
		}

		@Override
		public ChatResponse call(Prompt prompt) {
			callCount.incrementAndGet();
			ChatResponse response = responses.poll();
			if (response == null) {
				throw new AssertionError("Unexpected extra ChatModel.call invocation");
			}
			return response;
		}

		@Override
		public Flux<ChatResponse> stream(Prompt prompt) {
			return Flux.just(call(prompt));
		}

		private int callCount() {
			return callCount.get();
		}

	}

}
