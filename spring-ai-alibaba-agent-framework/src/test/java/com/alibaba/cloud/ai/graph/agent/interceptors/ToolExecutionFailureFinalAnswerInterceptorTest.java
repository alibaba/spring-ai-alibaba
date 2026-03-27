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
package com.alibaba.cloud.ai.graph.agent.interceptors;

import com.alibaba.cloud.ai.graph.agent.hook.toolexecutionfailure.ToolExecutionFailureFinalAnswerInterceptor;
import com.alibaba.cloud.ai.graph.agent.hook.toolexecutionfailure.ToolExecutionFailureGuardConstants;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import com.alibaba.cloud.ai.graph.serializer.AgentInstructionMessage;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolExecutionFailureFinalAnswerInterceptorTest {

	private final ToolExecutionFailureFinalAnswerInterceptor interceptor =
			new ToolExecutionFailureFinalAnswerInterceptor();

	@Test
	void shouldDisableToolExposureForExecutionFailureFinalAnswerTurn() {
		ToolCallback toolCallback = createEchoTool();
		ToolCallingChatOptions options = ToolCallingChatOptions.builder()
			.toolCallbacks(toolCallback)
			.internalToolExecutionEnabled(true)
			.build();
		ModelRequest request = ModelRequest.builder()
			.messages(List.of(new UserMessage("hello"), finalAnswerInstruction()))
			.options(options)
			.tools(List.of("echo"))
			.dynamicToolCallbacks(List.of(toolCallback))
			.toolDescriptions(Map.of("echo", "Echo tool"))
			.context(Map.of("traceId", "test"))
			.build();

		AtomicReference<ModelRequest> actualRequest = new AtomicReference<>();
		ModelResponse response = interceptor.interceptModel(request, captured -> {
			actualRequest.set(captured);
			return ModelResponse.of(new AssistantMessage("ok"));
		});

		assertEquals("ok", ((AssistantMessage) response.getMessage()).getText());
		assertNotSame(request, actualRequest.get());
		assertTrue(actualRequest.get().getTools().isEmpty());
		assertTrue(actualRequest.get().getDynamicToolCallbacks().isEmpty());
		assertTrue(actualRequest.get().getToolDescriptions().isEmpty());
		assertNotSame(options, actualRequest.get().getOptions());
		assertTrue(actualRequest.get().getOptions().getToolCallbacks().isEmpty());
		assertEquals(Boolean.FALSE, actualRequest.get().getOptions().getInternalToolExecutionEnabled());
		assertEquals(1, options.getToolCallbacks().size());
		assertEquals(Boolean.TRUE, options.getInternalToolExecutionEnabled());
	}

	@Test
	void shouldPassThroughWhenLastMessageIsNotExecutionFailureFinalAnswerInstruction() {
		ToolCallback toolCallback = createEchoTool();
		ToolCallingChatOptions options = ToolCallingChatOptions.builder()
			.toolCallbacks(toolCallback)
			.internalToolExecutionEnabled(true)
			.build();
		ModelRequest request = ModelRequest.builder()
			.messages(List.of(new UserMessage("hello"), AgentInstructionMessage.builder().text("normal instruction").build()))
			.options(options)
			.tools(List.of("echo"))
			.dynamicToolCallbacks(List.of(toolCallback))
			.toolDescriptions(Map.of("echo", "Echo tool"))
			.build();

		AtomicReference<ModelRequest> actualRequest = new AtomicReference<>();
		interceptor.interceptModel(request, captured -> {
			actualRequest.set(captured);
			return ModelResponse.of(new AssistantMessage("ok"));
		});

		assertSame(request, actualRequest.get());
	}

	private static Message finalAnswerInstruction() {
		return AgentInstructionMessage.builder()
			.text("Answer directly")
			.metadata(Map.of(ToolExecutionFailureGuardConstants.FINAL_ANSWER_INSTRUCTION_METADATA_KEY, true))
			.build();
	}

	private static ToolCallback createEchoTool() {
		return new ToolCallback() {
			@Override
			@NonNull
			public ToolDefinition getToolDefinition() {
				return ToolDefinition.builder().name("echo").description("Test tool echo").inputSchema("{}").build();
			}

			@Override
			@NonNull
			public String call(String toolInput, ToolContext toolContext) {
				return toolInput;
			}

			@Override
			@NonNull
			public String call(String toolInput) {
				return call(toolInput, new ToolContext(Map.of()));
			}
		};
	}

}


