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

import com.alibaba.cloud.ai.graph.agent.hook.returndirect.ReturnDirectModelHook;
import com.alibaba.cloud.ai.graph.agent.node.AgentLlmNode;
import com.alibaba.cloud.ai.graph.agent.tools.PoetTool;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import reactor.core.publisher.Flux;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test to verify bug #3112 fix: ReactAgent resolver tool injection.
 * This test verifies that tools provided via resolver are correctly passed to llmNode.
 */
class ReactAgentResolverBugReproductionTest {

	private static final String DIRECT_TOOL_RESULT = "\"RAW_RESULT=42\"";

	private static final String NATURAL_LANGUAGE_RESULT = "12 加 30 的结果是 42。";

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

	@Test
	void testReactAgentReturnDirectShortCircuitsSecondModelCall() throws GraphRunnerException {
		ScriptedToolLoopChatModel chatModel = new ScriptedToolLoopChatModel();

		ReactAgent agent = ReactAgent.builder()
				.name("return-direct-default-react-agent")
				.model(chatModel)
				.methodTools(new ReturnDirectAddTool())
				.build();

		AssistantMessage assistantMessage = agent.call("请调用工具计算 12 + 30，并用一句完整中文解释结果");

		assertEquals(DIRECT_TOOL_RESULT, assistantMessage.getText());
		assertEquals(1, chatModel.callCount.get(), "returnDirect=true should stop before the second model call");
	}

	@Test
	void testReactAgentNonReturnDirectStillCallsModelAgain() throws GraphRunnerException {
		ScriptedToolLoopChatModel chatModel = new ScriptedToolLoopChatModel();

		ReactAgent agent = ReactAgent.builder()
				.name("return-direct-false-react-agent")
				.model(chatModel)
				.methodTools(new NonReturnDirectAddTool())
				.build();

		AssistantMessage assistantMessage = agent.call("请调用工具计算 12 + 30，并用一句完整中文解释结果");

		assertEquals(NATURAL_LANGUAGE_RESULT, assistantMessage.getText());
		assertEquals(2, chatModel.callCount.get(), "returnDirect=false should continue into the second model call");
		assertEquals(DIRECT_TOOL_RESULT, chatModel.lastToolResponseData);
		assertNotEquals(DIRECT_TOOL_RESULT, assistantMessage.getText());
	}

	@Test
	void testExplicitReturnDirectHookStillShortCircuitsSecondModelCall() throws GraphRunnerException {
		ScriptedToolLoopChatModel chatModel = new ScriptedToolLoopChatModel();

		ReactAgent agent = ReactAgent.builder()
				.name("return-direct-hooked-react-agent")
				.model(chatModel)
				.methodTools(new ReturnDirectAddTool())
				.hooks(List.of(new ReturnDirectModelHook()))
				.build();

		AssistantMessage assistantMessage = agent.call("请调用工具计算 12 + 30，并用一句完整中文解释结果");

		assertEquals(DIRECT_TOOL_RESULT, assistantMessage.getText());
		assertEquals(1, chatModel.callCount.get(), "ReturnDirectModelHook should stop the loop before the second model call");
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

	static class ReturnDirectAddTool {

		@Tool(name = "my_add", description = "实现两个整数相加", returnDirect = true)
		public String add(@ToolParam(required = true, description = "必须是整数") int first,
				@ToolParam(required = true, description = "必须是整数") int second) {
			return "RAW_RESULT=" + (first + second);
		}
	}

	static class NonReturnDirectAddTool {

		@Tool(name = "my_add", description = "实现两个整数相加", returnDirect = false)
		public String add(@ToolParam(required = true, description = "必须是整数") int first,
				@ToolParam(required = true, description = "必须是整数") int second) {
			return "RAW_RESULT=" + (first + second);
		}
	}

	static final class ScriptedToolLoopChatModel implements ChatModel {

		private final AtomicInteger callCount = new AtomicInteger();

		private volatile String lastToolResponseData;

		@Override
		public ChatResponse call(Prompt prompt) {
			int currentCall = callCount.incrementAndGet();
			if (currentCall == 1) {
				AssistantMessage.ToolCall toolCall = new AssistantMessage.ToolCall(
						"call-1", "function", "my_add", "{\"first\":12,\"second\":30}");
				AssistantMessage assistantMessage = AssistantMessage.builder()
						.content("")
						.toolCalls(List.of(toolCall))
						.build();
				return new ChatResponse(List.of(new Generation(assistantMessage)));
			}

			List<Message> instructions = prompt.getInstructions();
			Message lastMessage = instructions.get(instructions.size() - 1);
			ToolResponseMessage toolResponseMessage = assertInstanceOf(ToolResponseMessage.class, lastMessage);
			lastToolResponseData = toolResponseMessage.getResponses().get(0).responseData();
			System.out.println("[spring-ai-alibaba-repro] second-model-call observed tool response: " + lastToolResponseData);
			return new ChatResponse(List.of(new Generation(new AssistantMessage(NATURAL_LANGUAGE_RESULT))));
		}
	}

}
