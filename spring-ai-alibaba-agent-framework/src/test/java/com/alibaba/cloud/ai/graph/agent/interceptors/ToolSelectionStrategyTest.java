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

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import com.alibaba.cloud.ai.graph.agent.interceptor.toolselection.ToolMetadata;
import com.alibaba.cloud.ai.graph.agent.interceptor.toolselection.ToolSelectionInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.toolselection.ToolSelectionRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.toolselection.ToolSelectionStrategy;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class ToolSelectionStrategyTest {

	@Test
	void customStrategyReceivesOnlyLightweightMetadataAndFiltersTools() {
		AtomicReference<ToolSelectionRequest> capturedSelectionRequest = new AtomicReference<>();
		AtomicReference<ModelRequest> capturedModelRequest = new AtomicReference<>();
		ToolSelectionStrategy strategy = request -> {
			capturedSelectionRequest.set(request);
			return List.of("weather_tool", "hotel_tool");
		};

		ToolSelectionInterceptor interceptor = ToolSelectionInterceptor.builder()
				.selectionStrategy(strategy)
				.maxTools(2)
				.build();
		ModelRequest request = requestWithThreeTools();

		interceptor.interceptModel(request, filteredRequest -> {
			capturedModelRequest.set(filteredRequest);
			return ModelResponse.of(new AssistantMessage("done"));
		});

		ToolSelectionRequest selectionRequest = capturedSelectionRequest.get();
		assertEquals("Find weather and a hotel", selectionRequest.query());
		assertEquals(2, selectionRequest.maxTools());
		assertEquals(Map.of("tenant", "demo"), selectionRequest.context());
		assertEquals(List.of(
				new ToolMetadata("weather_tool", "Get weather"),
				new ToolMetadata("ticket_tool", "Book tickets"),
				new ToolMetadata("hotel_tool", "Find hotels")), selectionRequest.tools());
		assertEquals(List.of("weather_tool", "hotel_tool"), capturedModelRequest.get().getTools());
	}

	@Test
	void unknownAndDuplicateToolNamesAreIgnored() {
		ToolSelectionInterceptor interceptor = ToolSelectionInterceptor.builder()
				.selectionStrategy(request -> List.of("unknown_tool", "hotel_tool", "hotel_tool"))
				.maxTools(2)
				.build();
		AtomicReference<ModelRequest> capturedRequest = new AtomicReference<>();

		interceptor.interceptModel(requestWithThreeTools(), request -> {
			capturedRequest.set(request);
			return ModelResponse.of(new AssistantMessage("done"));
		});

		assertEquals(List.of("hotel_tool"), capturedRequest.get().getTools());
	}

	@Test
	void alwaysIncludedToolsTakePriorityWithinLimit() {
		ToolSelectionInterceptor interceptor = ToolSelectionInterceptor.builder()
				.selectionStrategy(request -> List.of("weather_tool", "hotel_tool"))
				.maxTools(2)
				.alwaysInclude("ticket_tool")
				.build();
		AtomicReference<ModelRequest> capturedRequest = new AtomicReference<>();

		interceptor.interceptModel(requestWithThreeTools(), request -> {
			capturedRequest.set(request);
			return ModelResponse.of(new AssistantMessage("done"));
		});

		assertEquals(List.of("weather_tool", "ticket_tool"), capturedRequest.get().getTools());
	}

	@Test
	void strategyFailureFallsBackToOriginalRequest() {
		ToolSelectionInterceptor interceptor = ToolSelectionInterceptor.builder()
				.selectionStrategy(request -> {
					throw new IllegalStateException("index unavailable");
				})
				.maxTools(1)
				.build();
		ModelRequest originalRequest = requestWithThreeTools();
		AtomicReference<ModelRequest> capturedRequest = new AtomicReference<>();

		interceptor.interceptModel(originalRequest, request -> {
			capturedRequest.set(request);
			return ModelResponse.of(new AssistantMessage("done"));
		});

		assertSame(originalRequest, capturedRequest.get());
	}

	@Test
	void selectionIsSkippedWhenToolCountIsWithinLimit() {
		AtomicBoolean strategyCalled = new AtomicBoolean();
		ToolSelectionInterceptor interceptor = ToolSelectionInterceptor.builder()
				.selectionStrategy(request -> {
					strategyCalled.set(true);
					return List.of();
				})
				.maxTools(3)
				.build();

		interceptor.interceptModel(requestWithThreeTools(),
				request -> ModelResponse.of(new AssistantMessage("done")));

		assertFalse(strategyCalled.get());
	}

	@Test
	void builderRequiresExactlyOneSelectionMechanism() {
		assertThrows(IllegalStateException.class, () -> ToolSelectionInterceptor.builder().build());
		assertThrows(IllegalStateException.class, () -> ToolSelectionInterceptor.builder()
				.selectionModel(mock(ChatModel.class))
				.selectionStrategy(request -> List.of())
				.build());
	}

	@Test
	void selectedSchemaReachesModelAndSelectedToolExecutes() throws Exception {
		AtomicReference<ToolSelectionRequest> capturedSelectionRequest = new AtomicReference<>();
		AtomicReference<List<ToolCallback>> firstModelTools = new AtomicReference<>();
		AtomicInteger weatherToolCalls = new AtomicInteger();
		AtomicInteger ticketToolCalls = new AtomicInteger();
		AtomicInteger modelCalls = new AtomicInteger();

		ToolCallback weatherTool = tool("weather_tool", "Get weather",
				"{\"type\":\"object\",\"properties\":{\"city\":{\"type\":\"string\"}}}",
				weatherToolCalls);
		ToolCallback ticketTool = tool("ticket_tool", "Book tickets",
				"{\"type\":\"object\",\"properties\":{\"destination\":{\"type\":\"string\"}}}",
				ticketToolCalls);

		ChatModel model = new ChatModel() {
			@Override
			public ChatResponse call(Prompt prompt) {
				int invocation = modelCalls.getAndIncrement();
				ToolCallingChatOptions options = (ToolCallingChatOptions) prompt.getOptions();
				if (invocation == 0) {
					firstModelTools.set(List.copyOf(options.getToolCallbacks()));
					AssistantMessage.ToolCall toolCall = new AssistantMessage.ToolCall("call-1", "function",
							"weather_tool", "{\"city\":\"Shanghai\"}");
					return response(AssistantMessage.builder().content("").toolCalls(List.of(toolCall)).build());
				}
				return response(new AssistantMessage("Sunny in Shanghai"));
			}
		};

		ToolSelectionInterceptor interceptor = ToolSelectionInterceptor.builder()
				.selectionStrategy(request -> {
					capturedSelectionRequest.compareAndSet(null, request);
					return List.of("weather_tool");
				})
				.maxTools(1)
				.build();
		ReactAgent agent = ReactAgent.builder()
				.name("tool-selection-test")
				.model(model)
				.tools(List.of(weatherTool, ticketTool))
				.interceptors(interceptor)
				.saver(new MemorySaver())
				.build();

		AssistantMessage result = agent.call("What is the weather in Shanghai?");

		assertEquals("Sunny in Shanghai", result.getText());
		assertEquals(List.of(
				new ToolMetadata("weather_tool", "Get weather"),
				new ToolMetadata("ticket_tool", "Book tickets")), capturedSelectionRequest.get().tools());
		assertEquals(1, firstModelTools.get().size());
		assertEquals("weather_tool", firstModelTools.get().get(0).getToolDefinition().name());
		assertTrue(firstModelTools.get().get(0).getToolDefinition().inputSchema().contains("city"));
		assertFalse(firstModelTools.get().get(0).getToolDefinition().inputSchema().contains("destination"));
		assertEquals(1, weatherToolCalls.get());
		assertEquals(0, ticketToolCalls.get());
		assertEquals(2, modelCalls.get());
	}

	@Test
	void emptySelectionExposesNoSchemasToModel() throws Exception {
		AtomicReference<List<ToolCallback>> modelTools = new AtomicReference<>();
		AtomicReference<ToolSelectionRequest> capturedSelectionRequest = new AtomicReference<>();
		AtomicInteger weatherToolCalls = new AtomicInteger();
		AtomicInteger ticketToolCalls = new AtomicInteger();
		AtomicInteger dynamicToolCalls = new AtomicInteger();
		ToolCallback weatherTool = tool("weather_tool", "Get weather",
				"{\"type\":\"object\",\"properties\":{\"city\":{\"type\":\"string\"}}}",
				weatherToolCalls);
		ToolCallback ticketTool = tool("ticket_tool", "Book tickets",
				"{\"type\":\"object\",\"properties\":{\"destination\":{\"type\":\"string\"}}}",
				ticketToolCalls);
		ToolCallback dynamicTool = tool("dynamic_tool", "Dynamically loaded tool",
				"{\"type\":\"object\",\"properties\":{\"dynamicInput\":{\"type\":\"string\"}}}",
				dynamicToolCalls);
		ChatModel model = prompt -> {
			ToolCallingChatOptions options = (ToolCallingChatOptions) prompt.getOptions();
			modelTools.set(List.copyOf(options.getToolCallbacks()));
			return response(new AssistantMessage("No tool needed"));
		};
		ToolSelectionInterceptor interceptor = ToolSelectionInterceptor.builder()
				.selectionStrategy(request -> {
					capturedSelectionRequest.set(request);
					return List.of();
				})
				.maxTools(1)
				.build();
		ModelInterceptor dynamicToolInjector = new ModelInterceptor() {
			@Override
			public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
				return handler.call(ModelRequest.builder(request)
						.dynamicToolCallbacks(List.of(dynamicTool))
						.build());
			}

			@Override
			public String getName() {
				return "DynamicToolInjector";
			}
		};
		ReactAgent agent = ReactAgent.builder()
				.name("empty-tool-selection-test")
				.model(model)
				.tools(List.of(weatherTool, ticketTool))
				.interceptors(dynamicToolInjector, interceptor)
				.saver(new MemorySaver())
				.build();

		AssistantMessage result = agent.call("Say hello without using a tool");

		assertEquals("No tool needed", result.getText());
		assertTrue(capturedSelectionRequest.get().tools()
				.contains(new ToolMetadata("dynamic_tool", "Dynamically loaded tool")));
		assertTrue(modelTools.get().isEmpty());
		assertEquals(0, weatherToolCalls.get());
		assertEquals(0, ticketToolCalls.get());
		assertEquals(0, dynamicToolCalls.get());
	}

	@Test
	void interruptedSelectionRestoresInterruptAndSkipsModelCall() {
		AtomicBoolean handlerCalled = new AtomicBoolean();
		ToolSelectionInterceptor interceptor = ToolSelectionInterceptor.builder()
				.selectionStrategy(request -> {
					throw new InterruptedException("cancelled");
				})
				.maxTools(1)
				.build();

		try {
			RuntimeException exception = assertThrows(RuntimeException.class,
					() -> interceptor.interceptModel(requestWithThreeTools(), request -> {
						handlerCalled.set(true);
						return ModelResponse.of(new AssistantMessage("done"));
					}));

			assertEquals("Tool selection interrupted", exception.getMessage());
			assertTrue(Thread.currentThread().isInterrupted());
			assertFalse(handlerCalled.get());
		}
		finally {
			Thread.interrupted();
		}
	}

	@Test
	void malformedLlmSelectionFallsBackToOriginalRequest() {
		ChatModel malformedSelectionModel = prompt -> response(new AssistantMessage("```json\n{not-json}\n```"));
		ToolSelectionInterceptor interceptor = ToolSelectionInterceptor.builder()
				.selectionModel(malformedSelectionModel)
				.maxTools(1)
				.build();
		ModelRequest originalRequest = requestWithThreeTools();
		AtomicReference<ModelRequest> capturedRequest = new AtomicReference<>();

		interceptor.interceptModel(originalRequest, request -> {
			capturedRequest.set(request);
			return ModelResponse.of(new AssistantMessage("done"));
		});

		assertSame(originalRequest, capturedRequest.get());
	}

	private ModelRequest requestWithThreeTools() {
		return ModelRequest.builder()
				.messages(List.of(new UserMessage("Find weather and a hotel")))
				.tools(List.of("weather_tool", "ticket_tool", "hotel_tool"))
				.toolDescriptions(Map.of(
						"weather_tool", "Get weather",
						"ticket_tool", "Book tickets",
						"hotel_tool", "Find hotels"))
				.context(Map.of("tenant", "demo"))
				.build();
	}

	private ToolCallback tool(String name, String description, String inputSchema, AtomicInteger calls) {
		return new ToolCallback() {
			@Override
			public ToolDefinition getToolDefinition() {
				return ToolDefinition.builder()
						.name(name)
						.description(description)
						.inputSchema(inputSchema)
						.build();
			}

			@Override
			public String call(String toolInput, ToolContext toolContext) {
				calls.incrementAndGet();
				return "Sunny in Shanghai";
			}

			@Override
			public String call(String toolInput) {
				return call(toolInput, new ToolContext(Map.of()));
			}
		};
	}

	private ChatResponse response(AssistantMessage message) {
		return new ChatResponse(List.of(new Generation(message)));
	}
}
