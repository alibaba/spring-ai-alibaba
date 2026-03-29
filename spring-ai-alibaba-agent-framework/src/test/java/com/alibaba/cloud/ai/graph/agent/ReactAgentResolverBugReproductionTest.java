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

import com.alibaba.cloud.ai.graph.agent.node.AgentLlmNode;
import com.alibaba.cloud.ai.graph.agent.tools.PoetTool;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import reactor.core.publisher.Flux;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test to verify bug #3112 fix: ReactAgent resolver tool injection.
 * This test verifies that tools provided via resolver are correctly passed to llmNode.
 */
class ReactAgentResolverBugReproductionTest {

	/**
	 * Simple ToolCallbackResolver implementation that provides a tool.
	 */
	static class SimpleToolCallbackResolver implements ToolCallbackResolver {
		private final Map<String, ToolCallback> tools = new HashMap<>();

		public SimpleToolCallbackResolver(ToolCallback... toolCallbacks) {
			for (ToolCallback tool : toolCallbacks) {
				tools.put(tool.getToolDefinition().name(), tool);
			}
		}

		@Override
		public ToolCallback resolve(String toolName) {
			return tools.get(toolName);
		}
	}

	/**
	 * Mock ChatModel for testing without API calls.
	 */
	static class MockChatModel implements ChatModel {
		@Override
		public ChatResponse call(Prompt prompt) {
			return new ChatResponse(List.of(new Generation(new AssistantMessage("Mock response"))));
		}

		@Override
		public Flux<ChatResponse> stream(Prompt prompt) {
			return Flux.just(new ChatResponse(List.of(new Generation(new AssistantMessage("Mock stream response")))));
		}
	}

	/**
	 * Test case: Using .resolver() method ONLY - llmNode SHOULD have tools (BUG FIXED)
	 * 
	 * This test verifies that bug #3112 is fixed:
	 * When tools are provided ONLY via resolver, llmNode should receive tool definitions.
	 */
	@Test
	void testReactAgentWithResolverOnly_llmNodeHasTools() throws Exception {
		ToolCallback poetTool = PoetTool.createPoetToolCallback();
		SimpleToolCallbackResolver resolver = new SimpleToolCallbackResolver(poetTool);

		ReactAgent agent = ReactAgent.builder()
				.name("agent_with_resolver")
				.model(new MockChatModel())
				.resolver(resolver)  // ONLY using resolver, NOT .tools()
				.saver(new MemorySaver())
				.build();

		// Get llmNode through reflection
		AgentLlmNode llmNode = getLlmNode(agent);

		// Verify llmNode has tool definitions
		List<ToolCallback> toolCallbacks = getToolCallbacks(llmNode);
		
		// This assertion verifies the bug is fixed
		assertNotNull(toolCallbacks, "toolCallbacks should not be null");
		assertFalse(toolCallbacks.isEmpty(), 
				"BUG #3112 FIXED: llmNode should have tool definitions when using resolver. " +
				"Current size: " + toolCallbacks.size());
		assertEquals(1, toolCallbacks.size(), "Should have 1 tool");
		assertEquals("poem", toolCallbacks.get(0).getToolDefinition().name(), "Tool name should be 'poem'");
	}

	/**
	 * Helper method to get llmNode from ReactAgent using reflection.
	 */
	private AgentLlmNode getLlmNode(ReactAgent agent) throws Exception {
		Field llmNodeField = ReactAgent.class.getDeclaredField("llmNode");
		llmNodeField.setAccessible(true);
		return (AgentLlmNode) llmNodeField.get(agent);
	}

	/**
	 * Helper method to get toolCallbacks from AgentLlmNode using reflection.
	 */
	private List<ToolCallback> getToolCallbacks(AgentLlmNode llmNode) throws Exception {
		Field toolCallbacksField = AgentLlmNode.class.getDeclaredField("toolCallbacks");
		toolCallbacksField.setAccessible(true);
		@SuppressWarnings("unchecked")
		List<ToolCallback> toolCallbacks = (List<ToolCallback>) toolCallbacksField.get(llmNode);
		return toolCallbacks;
	}

}

