/*
 * Copyright 2024-2025 the original author or authors.
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
package com.alibaba.cloud.ai.graph.agent.flow;

import java.util.HashMap;
import java.util.List;

import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for FlowAgent builders to verify the refactored builder pattern.
 */
class FlowAgentBuilderTest {

	@Mock
	private ChatClient chatClient;

	@Mock
	private ChatModel chatModel;

	@Mock
	private ToolCallbackResolver toolCallbackResolver;

	@Test
	void testSequentialAgentBuilderFluentInterface() throws Exception {
		MockitoAnnotations.openMocks(this);

		// Create a simple ReactAgent to use as sub-agent
		ReactAgent subAgent = ReactAgent.builder()
			.name("subAgent")
			.description("A sub agent")
			.outputKey("sub_output")
			.chatClient(chatClient)
			.state(() -> {
				HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
				keyStrategyHashMap.put("messages", new AppendStrategy());
				return keyStrategyHashMap;
			})
			.resolver(toolCallbackResolver)
			.build();

		// Test fluent interface with SequentialAgent
		SequentialAgent sequentialAgent = SequentialAgent.builder()
			.name("testSequentialAgent")
			.description("Test sequential agent")
			.outputKey("output")
			.inputKey("input")
			.subAgents(List.of(subAgent))
			.state(() -> {
				HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
				keyStrategyHashMap.put("messages", new AppendStrategy());
				return keyStrategyHashMap;
			})
			.build();

		assertNotNull(sequentialAgent);
		assertEquals("testSequentialAgent", sequentialAgent.name());
		assertEquals("Test sequential agent", sequentialAgent.description());
		assertEquals("output", sequentialAgent.outputKey());
		assertEquals("input", sequentialAgent.inputKey());
		assertEquals(1, sequentialAgent.subAgents().size());
	}

	@Test
	void testLlmRoutingAgentBuilderFluentInterface() throws Exception {
		MockitoAnnotations.openMocks(this);

		// Create a simple ReactAgent to use as sub-agent
		ReactAgent subAgent = ReactAgent.builder()
			.name("subAgent")
			.description("A sub agent")
			.outputKey("sub_output")
			.chatClient(chatClient)
			.state(() -> {
				HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
				keyStrategyHashMap.put("messages", new AppendStrategy());
				return keyStrategyHashMap;
			})
			.resolver(toolCallbackResolver)
			.build();

		// Test fluent interface with LlmRoutingAgent
		LlmRoutingAgent routingAgent = LlmRoutingAgent.builder()
			.name("testRoutingAgent")
			.description("Test routing agent")
			.outputKey("output")
			.inputKey("input")
			.subAgents(List.of(subAgent))
			.model(chatModel)
			.state(() -> {
				HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
				keyStrategyHashMap.put("messages", new AppendStrategy());
				return keyStrategyHashMap;
			})
			.build();

		assertNotNull(routingAgent);
		assertEquals("testRoutingAgent", routingAgent.name());
		assertEquals("Test routing agent", routingAgent.description());
		assertEquals("output", routingAgent.outputKey());
		assertEquals("input", routingAgent.inputKey());
		assertEquals(1, routingAgent.subAgents().size());
	}

	@Test
	void testBuilderValidation() {
		// Test validation in SequentialAgent builder
		Exception exception = assertThrows(IllegalArgumentException.class, () -> {
			SequentialAgent.builder().build();
		});
		assertEquals("Name must be provided", exception.getMessage());

		// Test validation for missing sub-agents
		exception = assertThrows(IllegalArgumentException.class, () -> {
			SequentialAgent.builder().name("testAgent").build();
		});
		assertEquals("At least one sub-agent must be provided for flow", exception.getMessage());

		// Test validation for LlmRoutingAgent missing ChatModel
		exception = assertThrows(IllegalArgumentException.class, () -> {
			LlmRoutingAgent.builder()
				.name("testAgent")
				.subAgents(List.of()) // Empty list will fail sub-agent validation first
				.build();
		});
		assertEquals("At least one sub-agent must be provided for flow", exception.getMessage());
	}

	@Test
	void testLlmRoutingAgentRequiresChatModel() throws Exception {
		MockitoAnnotations.openMocks(this);

		ReactAgent subAgent = ReactAgent.builder()
			.name("subAgent")
			.description("A sub agent")
			.outputKey("sub_output")
			.chatClient(chatClient)
			.state(() -> {
				HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
				keyStrategyHashMap.put("messages", new AppendStrategy());
				return keyStrategyHashMap;
			})
			.resolver(toolCallbackResolver)
			.build();

		// Test that LlmRoutingAgent requires ChatModel
		Exception exception = assertThrows(IllegalArgumentException.class, () -> {
			LlmRoutingAgent.builder().name("testAgent").subAgents(List.of(subAgent)).state(() -> {
				HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
				keyStrategyHashMap.put("messages", new AppendStrategy());
				return keyStrategyHashMap;
			}).build();
		});
		assertEquals("ChatModel must be provided for LLM routing agent", exception.getMessage());
	}

}
