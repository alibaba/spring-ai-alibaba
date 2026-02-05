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

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.node.AgentLlmNode;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test to reproduce the dynamic tool discovery issue with ToolCallbackProvider.
 * 
 * Issue: Spring AI Alibaba Agent framework only calls ToolCallbackProvider.getToolCallbacks() 
 * once during build time, and cannot detect tool changes at runtime.
 * 
 * Expected: ToolCallbackProvider should be called on each AI request to support dynamic tool discovery (like MCP).
 * Actual: Tools are fixed at build time and never refreshed.
 */
@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
class DynamicToolDiscoveryIssueTest {

	private ChatModel chatModel;

	@BeforeEach
	void setUp() {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
			.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
			.build();
		this.chatModel = DashScopeChatModel.builder()
			.dashScopeApi(dashScopeApi)
			.build();
	}

	/**
	 * Test 1: Verify that ToolCallbackProvider is only called once during build.
	 * This test uses reflection to check the actual tool count in the agent.
	 */
	@Test
	void testToolCallbackProviderOnlyCalledOnceDuringBuild() throws Exception {
		// Track how many times getToolCallbacks() is called
		AtomicInteger callCount = new AtomicInteger(0);
		
		// Create a dynamic tool list
		List<ToolCallback> toolList = new ArrayList<>();
		toolList.add(createTool("tool_1", "First tool"));
		
		// Create provider that tracks calls
		ToolCallbackProvider provider = () -> {
			callCount.incrementAndGet();
			System.out.println("ToolCallbackProvider.getToolCallbacks() called - count: " + callCount.get());
			return toolList.toArray(new ToolCallback[0]);
		};
		
		// Build agent
		ReactAgent agent = ReactAgent.builder()
			.name("test_agent")
			.model(chatModel)
			.toolCallbackProviders(provider)
			.saver(new MemorySaver())
			.build();
		
		// Verify provider was called once during build
		assertEquals(1, callCount.get(), "Provider should be called once during build");
		
		// Get tool count via reflection
		int initialToolCount = getToolCountFromAgent(agent);
		assertEquals(1, initialToolCount, "Agent should have 1 tool initially");
		
		// Add a new tool to the provider's list
		toolList.add(createTool("tool_2", "Second tool"));
		System.out.println("Added tool_2 to provider's list");
		
		// Invoke agent - this should trigger a new call to getToolCallbacks() but it doesn't
		agent.invoke("Use available tools", RunnableConfig.builder().build());
		
		// Check if provider was called again (it won't be - this is the bug)
		System.out.println("After invoke, provider call count: " + callCount.get());
		assertEquals(1, callCount.get(), "BUG: Provider is still only called once, not refreshed on invoke");
		
		// Verify tool count hasn't changed
		int finalToolCount = getToolCountFromAgent(agent);
		assertEquals(1, finalToolCount, "BUG: Agent still has 1 tool, should have 2 if provider was called again");
		
		System.out.println("\n=== BUG CONFIRMED ===");
		System.out.println("Expected: ToolCallbackProvider.getToolCallbacks() called on each invoke");
		System.out.println("Actual: Only called once during build, tools are fixed");
	}

	/**
	 * Test 2: Simulate MCP scenario where tools change dynamically.
	 * This demonstrates the real-world impact on MCP integration.
	 */
	@Test
	void testMcpLikeDynamicToolScenario() throws Exception {
		// Simulate MCP server with dynamic tools
		AtomicInteger toolCount = new AtomicInteger(1);
		
		ToolCallbackProvider mcpLikeProvider = () -> {
			List<ToolCallback> tools = new ArrayList<>();
			for (int i = 1; i <= toolCount.get(); i++) {
				tools.add(createTool("mcp_tool_" + i, "MCP tool " + i));
			}
			System.out.println("MCP Provider returning " + tools.size() + " tools");
			return tools.toArray(new ToolCallback[0]);
		};
		
		// Build agent with initial tool set
		ReactAgent agent = ReactAgent.builder()
			.name("mcp_agent")
			.model(chatModel)
			.toolCallbackProviders(mcpLikeProvider)
			.saver(new MemorySaver())
			.build();
		
		int initialTools = getToolCountFromAgent(agent);
		System.out.println("Initial tool count: " + initialTools);
		assertEquals(1, initialTools, "Should start with 1 tool");
		
		// Simulate MCP server adding new tools
		toolCount.set(3);
		System.out.println("MCP server now has 3 tools");
		
		// Invoke agent - should see 3 tools but won't
		agent.invoke("List available tools", RunnableConfig.builder().build());
		
		int finalTools = getToolCountFromAgent(agent);
		System.out.println("Final tool count: " + finalTools);
		
		System.out.println("\n=== MCP INTEGRATION ISSUE ===");
		System.out.println("MCP server has 3 tools, but agent still sees: " + finalTools);
		System.out.println("This breaks MCP's dynamic tool registration feature");
		
		assertEquals(1, finalTools, "BUG: Agent cannot detect MCP tool changes");
	}

	/**
	 * Test 3: Compare with Spring AI's expected behavior.
	 * Documents what the correct behavior should be.
	 */
	@Test
	void testExpectedBehaviorDocumentation() {
		System.out.println("\n=== EXPECTED BEHAVIOR (Spring AI) ===");
		System.out.println("1. ToolCallbackProvider.getToolCallbacks() should be called on EACH AI request");
		System.out.println("2. This allows dynamic tool discovery (MCP, hot-reload, etc.)");
		System.out.println("3. Spring AI's ChatClient does this correctly");
		
		System.out.println("\n=== ACTUAL BEHAVIOR (Spring AI Alibaba) ===");
		System.out.println("1. ToolCallbackProvider.getToolCallbacks() called ONCE during build()");
		System.out.println("2. Tools are fixed and never refreshed");
		System.out.println("3. Agent cannot detect tool changes at runtime");
		
		System.out.println("\n=== IMPACT ===");
		System.out.println("- MCP dynamic tool registration doesn't work");
		System.out.println("- Cannot hot-reload tools without rebuilding agent");
		System.out.println("- Inconsistent with Spring AI's design");
		
		System.out.println("\n=== FIX NEEDED ===");
		System.out.println("Call ToolCallbackProvider.getToolCallbacks() in:");
		System.out.println("- AgentLlmNode.apply() before each model call");
		System.out.println("- OR AgentToolNode.resolve() when resolving tools");
	}

	// Helper methods

	private ToolCallback createTool(String name, String description) {
		return FunctionToolCallback.builder(name, (String args) -> "Result from " + name)
			.description(description)
			.inputType(String.class)
			.build();
	}

	private int getToolCountFromAgent(ReactAgent agent) throws Exception {
		Field llmNodeField = ReactAgent.class.getDeclaredField("llmNode");
		llmNodeField.setAccessible(true);
		AgentLlmNode llmNode = (AgentLlmNode) llmNodeField.get(agent);
		
		Field toolCallbacksField = AgentLlmNode.class.getDeclaredField("toolCallbacks");
		toolCallbacksField.setAccessible(true);
		@SuppressWarnings("unchecked")
		List<ToolCallback> tools = (List<ToolCallback>) toolCallbacksField.get(llmNode);
		
		return tools != null ? tools.size() : 0;
	}
}
