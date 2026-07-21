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

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.alibaba.cloud.ai.graph.agent.tool.StateAwareToolCallback;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.ai.util.JsonHelper;
import reactor.core.publisher.Flux;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentToolJsonTest {

	@Test
	void inputSchemaShouldBeWrappedWithSpringAiJsonHelperCompatibleJson() {
		ReactAgent agent = ReactAgent.builder()
			.name("structured_child_agent")
			.description("Structured child agent")
			.model(new CapturingChatModel())
			.inputSchema("""
					{
						"type": "object",
						"properties": {
							"topic": { "type": "string" },
							"items": {
								"type": "array",
								"items": { "type": "integer" }
							}
						},
						"required": ["topic", "items"]
					}
					""")
			.build();

		ToolCallback callback = AgentTool.create(agent);
		Map<String, Object> wrappedSchema = new JsonHelper().fromJsonToMap(callback.getToolDefinition().inputSchema());
		Map<String, Object> properties = asMap(wrappedSchema.get("properties"));
		Map<String, Object> input = asMap(properties.get("input"));

		assertEquals("object", wrappedSchema.get("type"));
		assertEquals(List.of("input"), wrappedSchema.get("required"));
		assertEquals("object", input.get("type"));
		assertTrue(asMap(input.get("properties")).containsKey("topic"));
		assertTrue(asMap(input.get("properties")).containsKey("items"));
	}

	@Test
	void objectAndArrayInputShouldBeForwardedAsJsonText() {
		CapturingChatModel chatModel = new CapturingChatModel();
		ReactAgent agent = ReactAgent.builder()
			.name("json_child_agent")
			.description("JSON child agent")
			.model(chatModel)
			.build();

		AgentTool.AgentToolExecutor executor = new AgentTool.AgentToolExecutor(agent);
		executor.executeAgent("""
				{
					"input": {
						"topic": "spring",
						"items": [1, 2]
					}
				}
				""", new ToolContext(Map.of()));

		UserMessage userMessage = chatModel.lastPrompt.get().getUserMessage();
		assertEquals("{\"topic\":\"spring\",\"items\":[1,2]}", userMessage.getText());
	}

	@Test
	void callbackShouldAcceptStructuredInputWithoutMethodArgumentConversion() {
		CapturingChatModel chatModel = new CapturingChatModel();
		ReactAgent agent = ReactAgent.builder()
			.name("json_child_agent")
			.description("JSON child agent")
			.model(chatModel)
			.inputSchema("""
					{
						"type": "object",
						"properties": {
							"topic": { "type": "string" },
							"items": {
								"type": "array",
								"items": { "type": "integer" }
							}
						},
						"required": ["topic", "items"]
					}
					""")
			.build();

		ToolCallback callback = AgentTool.create(agent);
		String result = callback.call("""
				{
					"input": {
						"topic": "spring",
						"items": [1, 2]
					}
				}
				""", new ToolContext(Map.of()));

		UserMessage userMessage = chatModel.lastPrompt.get().getUserMessage();
		assertEquals("{\"topic\":\"spring\",\"items\":[1,2]}", userMessage.getText());
		assertEquals("done", result);
	}

	@Test
	void callbackShouldBeStateAwareForAgentToolNodeContextInjection() {
		ReactAgent agent = ReactAgent.builder()
			.name("state_aware_child_agent")
			.description("State aware child agent")
			.model(new CapturingChatModel())
			.build();

		ToolCallback callback = AgentTool.create(agent);

		assertInstanceOf(StateAwareToolCallback.class, callback);
	}

	@Test
	void callbackShouldWrapExecutionFailuresAsToolExecutionException() throws Exception {
		ReactAgent agent = ReactAgent.builder()
			.name("failing_child_agent")
			.description("Failing child agent")
			.model(new CapturingChatModel())
			.build();
		ToolDefinition toolDefinition = ToolDefinition.builder()
			.name("failing_child_agent")
			.description("Failing child agent")
			.inputSchema("{}")
			.build();
		ToolCallback callback = newAgentToolCallback(toolDefinition, new FailingAgentToolExecutor(agent));

		ToolExecutionException exception = assertThrows(ToolExecutionException.class,
				() -> callback.call("{\"input\":\"hello\"}", new ToolContext(Map.of())));

		assertSame(toolDefinition, exception.getToolDefinition());
		assertEquals("executor failed", exception.getCause().getMessage());
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> asMap(Object value) {
		return assertInstanceOf(Map.class, value);
	}

	private static ToolCallback newAgentToolCallback(ToolDefinition toolDefinition,
			AgentTool.AgentToolExecutor executor) throws Exception {
		Class<?> callbackClass = Class.forName("com.alibaba.cloud.ai.graph.agent.AgentTool$AgentToolCallback");
		Constructor<?> constructor = callbackClass.getDeclaredConstructor(ToolDefinition.class,
				AgentTool.AgentToolExecutor.class);
		constructor.setAccessible(true);
		return (ToolCallback) constructor.newInstance(toolDefinition, executor);
	}

	private static final class CapturingChatModel implements ChatModel {

		private final AtomicReference<Prompt> lastPrompt = new AtomicReference<>();

		@Override
		public ChatResponse call(Prompt prompt) {
			this.lastPrompt.set(prompt);
			return new ChatResponse(List.of(new Generation(new AssistantMessage("done"))));
		}

		@Override
		public Flux<ChatResponse> stream(Prompt prompt) {
			return Flux.just(call(prompt));
		}

	}

	private static final class FailingAgentToolExecutor extends AgentTool.AgentToolExecutor {

		private FailingAgentToolExecutor(ReactAgent agent) {
			super(agent);
		}

		@Override
		public AssistantMessage executeAgent(String input, ToolContext toolContext) {
			throw new IllegalStateException("executor failed");
		}

	}

}
