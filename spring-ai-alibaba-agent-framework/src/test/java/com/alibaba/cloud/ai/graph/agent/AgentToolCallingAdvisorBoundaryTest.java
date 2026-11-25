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
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import com.alibaba.cloud.ai.graph.agent.tools.ToolContextHelper;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

	@Test
	void toolOnlyAgentShouldNotReplaceProviderOptionsWithGenericToolOptions() throws Exception {
		AtomicReference<ModelRequest> capturedRequest = new AtomicReference<>();
		ToolCallback toolCallback = ToolCallbacks.from(new BoundaryTools())[0];
		ReactAgent agent = ReactAgent.builder()
				.name("provider-options-agent")
				.model(new FixedResponseChatModel("provider response"))
				.tools(toolCallback)
				.interceptors(new CapturingModelInterceptor(capturedRequest))
				.saver(new MemorySaver())
				.build();

		AssistantMessage result = agent.call("use available tools if needed",
				RunnableConfig.builder().threadId("provider-options-thread").addMetadata("_stream_", false).build());

		assertEquals("provider response", result.getText());
		assertNull(capturedRequest.get().getOptions());
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void excludedToolCannotRunThroughTheResolver(boolean stream) throws Exception {
		var tools = new BoundaryTools();
		var callback = ToolCallbacks.from(tools)[0];
		var model = new ToolCallThenFinalChatModel();
		var agent = ReactAgent.builder()
				.name("filtered-tool-agent").model(model).tools(callback).resolver(name -> callback)
				.interceptors(new ModelInterceptor() {
					@Override
					public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
						return handler.call(ModelRequest.builder(request).tools(List.of()).build());
					}

					@Override
					public String getName() {
						return "exclude-all-tools";
					}
				})
				.saver(new MemorySaver()).build();

		var answer = agent.call("answer without tools", RunnableConfig.builder()
				.threadId("filtered-tool").addMetadata("_stream_", stream).build());

		assertEquals("final response", answer.getText());
		assertEquals(0, tools.agentToolNodeExecutions.get());
		assertEquals(0, tools.chatClientAdvisorExecutions.get());
		assertTrue(model.lastPrompt.get().getInstructions().stream().anyMatch(message ->
				message instanceof ToolResponseMessage response && response.getResponses().stream()
						.anyMatch(tool -> tool.responseData().contains("Tool not available"))));
	}

	@Test
	void configuredAgentAndChatClientToolsAreBothAvailableInTheSameRound() throws Exception {
		var tools = new BoundaryTools();
		var shadowedDefault = new BoundaryTools();
		var gateway = new GatewayTools();
		var declaredNames = new AtomicReference<List<String>>();
		var calls = new AtomicInteger();
		ChatModel model = new ChatModel() {
			@Override
			public ToolCallingChatOptions getOptions() {
				return ToolCallingChatOptions.builder().build();
			}

			@Override
			public ChatResponse call(Prompt prompt) {
				if (calls.incrementAndGet() == 1) {
					var options = (ToolCallingChatOptions) prompt.getOptions();
					declaredNames.set(options.getToolCallbacks().stream().map(tool -> tool.getToolDefinition().name()).toList());
					var localCall = new AssistantMessage.ToolCall("local", "function", "boundary_tool", "{}");
					var gatewayCall = new AssistantMessage.ToolCall("remote", "function", "gateway_lookup", "{}");
					return new ChatResponse(List.of(new Generation(
							AssistantMessage.builder().content("").toolCalls(List.of(localCall, gatewayCall)).build())));
				}
				return new ChatResponse(List.of(new Generation(new AssistantMessage("finished"))));
			}
		};
		var client = ChatClient.builder(model)
				.defaultTools(ToolCallbacks.from(gateway, shadowedDefault)).build();
		var agent = ReactAgent.builder().name("combined-tools").chatClient(client)
				.tools(ToolCallbacks.from(tools)).saver(new MemorySaver()).build();

		assertEquals("finished", agent.call("use both tools", RunnableConfig.builder()
				.threadId("combined").addMetadata("_stream_", false).build()).getText());
		assertEquals(List.of("boundary_tool", "gateway_lookup"), declaredNames.get());
		assertEquals(1, tools.agentToolNodeExecutions.get());
		assertEquals(1, gateway.executions.get());
		assertEquals(0, shadowedDefault.agentToolNodeExecutions.get());
	}

	private static final class ToolCallThenFinalChatModel implements ChatModel {

		private final AtomicInteger callCount = new AtomicInteger();

		private final AtomicReference<Prompt> lastPrompt = new AtomicReference<>();

		@Override
		public ChatResponse call(Prompt prompt) {
			lastPrompt.set(prompt);
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

	private static final class FixedResponseChatModel implements ChatModel {

		private final String response;

		private FixedResponseChatModel(String response) {
			this.response = response;
		}

		@Override
		public ChatResponse call(Prompt prompt) {
			return new ChatResponse(List.of(new Generation(new AssistantMessage(this.response))));
		}
	}

	private static final class CapturingModelInterceptor extends ModelInterceptor {

		private final AtomicReference<ModelRequest> capturedRequest;

		private CapturingModelInterceptor(AtomicReference<ModelRequest> capturedRequest) {
			this.capturedRequest = capturedRequest;
		}

		@Override
		public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
			this.capturedRequest.set(request);
			return handler.call(request);
		}

		@Override
		public String getName() {
			return "capturing-model-interceptor";
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

	private static final class GatewayTools {

		private final AtomicInteger executions = new AtomicInteger();

		@Tool(name = "gateway_lookup", description = "Gateway test tool")
		String lookup() {
			executions.incrementAndGet();
			return "remote result";
		}
	}

}
