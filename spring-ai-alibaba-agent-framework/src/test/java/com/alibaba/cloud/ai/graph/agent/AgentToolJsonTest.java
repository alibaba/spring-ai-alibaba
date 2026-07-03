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

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.util.json.JsonParser;
import reactor.core.publisher.Flux;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentToolJsonTest {

	@Test
	void agentToolJsonShouldUseSpringAiJsonParser() throws Exception {
		String source = Files.readString(Path.of("src/main/java/com/alibaba/cloud/ai/graph/agent/AgentTool.java"));

		assertFalse(source.contains("new ObjectMapper()"));
		assertTrue(source.contains("org.springframework.ai.util.json.JsonParser")
				|| source.contains("import org.springframework.ai.util.json.JsonParser;"));
	}

	@Test
	void inputSchemaShouldBeWrappedWithSpringAiJsonParserCompatibleJson() {
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
		Map<String, Object> wrappedSchema = JsonParser.fromJson(callback.getToolDefinition().inputSchema(), Map.class);
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

	@SuppressWarnings("unchecked")
	private static Map<String, Object> asMap(Object value) {
		return assertInstanceOf(Map.class, value);
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

}
