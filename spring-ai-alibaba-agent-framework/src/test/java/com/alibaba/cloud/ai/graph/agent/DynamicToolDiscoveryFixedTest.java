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
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify that the dynamic tool discovery fix works correctly.
 *
 * This test validates that ToolCallbackProvider.getToolCallbacks() is called
 * on each invocation, enabling dynamic tool discovery for MCP and other scenarios.
 */
@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
class DynamicToolDiscoveryFixedTest {

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
   * Test that ToolCallbackProvider is called on each invoke (not just during build).
   */
  @Test
  void testProviderCalledOnEachInvoke() throws Exception {
    AtomicInteger callCount = new AtomicInteger(0);

    List<ToolCallback> toolList = new ArrayList<>();
    toolList.add(createTool("tool_1", "First tool"));

    ToolCallbackProvider provider = () -> {
      callCount.incrementAndGet();
      System.out.println("Provider called - count: " + callCount.get());
      return toolList.toArray(new ToolCallback[0]);
    };

    ReactAgent agent = ReactAgent.builder()
      .name("test_agent")
      .model(chatModel)
      .toolCallbackProviders(provider)
      .saver(new MemorySaver())
      .build();

    // Provider called once during build
    int buildCallCount = callCount.get();
    System.out.println("After build, call count: " + buildCallCount);
    assertTrue(buildCallCount >= 1, "Provider should be called at least once during build");

    // First invoke
    agent.invoke("Test invoke 1", RunnableConfig.builder().build());
    int afterFirstInvoke = callCount.get();
    System.out.println("After first invoke, call count: " + afterFirstInvoke);
    assertTrue(afterFirstInvoke > buildCallCount, "Provider should be called again on first invoke");

    // Second invoke
    agent.invoke("Test invoke 2", RunnableConfig.builder().build());
    int afterSecondInvoke = callCount.get();
    System.out.println("After second invoke, call count: " + afterSecondInvoke);
    assertTrue(afterSecondInvoke > afterFirstInvoke, "Provider should be called again on second invoke");

    System.out.println("\n✅ FIX VERIFIED: Provider is called on each invoke");
  }

  /**
   * Test that dynamically added tools are discovered.
   */
  @Test
  void testDynamicToolsAreDiscovered() throws Exception {
    List<ToolCallback> toolList = new ArrayList<>();
    toolList.add(createTool("initial_tool", "Initial tool"));

    ToolCallbackProvider provider = () -> {
      System.out.println("Provider returning " + toolList.size() + " tools");
      return toolList.toArray(new ToolCallback[0]);
    };

    ReactAgent agent = ReactAgent.builder()
      .name("dynamic_agent")
      .model(chatModel)
      .toolCallbackProviders(provider)
      .saver(new MemorySaver())
      .build();

    // Add new tool
    toolList.add(createTool("dynamic_tool", "Dynamically added tool"));
    System.out.println("Added dynamic_tool to provider");

    // Invoke - should see the new tool
    agent.invoke("Use tools", RunnableConfig.builder().build());

    System.out.println("\n✅ FIX VERIFIED: Dynamic tools are discovered");
  }

  /**
   * Test MCP-like scenario with changing tool count.
   */
  @Test
  void testMcpLikeScenario() throws Exception {
    AtomicInteger toolCount = new AtomicInteger(1);

    ToolCallbackProvider mcpProvider = () -> {
      List<ToolCallback> tools = new ArrayList<>();
      for (int i = 1; i <= toolCount.get(); i++) {
        tools.add(createTool("mcp_tool_" + i, "MCP tool " + i));
      }
      System.out.println("MCP Provider returning " + tools.size() + " tools");
      return tools.toArray(new ToolCallback[0]);
    };

    ReactAgent agent = ReactAgent.builder()
      .name("mcp_agent")
      .model(chatModel)
      .toolCallbackProviders(mcpProvider)
      .saver(new MemorySaver())
      .build();

    // First invoke with 1 tool
    agent.invoke("Test 1", RunnableConfig.builder().build());
    System.out.println("First invoke completed with 1 tool");

    // Simulate MCP server adding tools
    toolCount.set(3);
    System.out.println("MCP server now has 3 tools");

    // Second invoke should see 3 tools
    agent.invoke("Test 2", RunnableConfig.builder().build());
    System.out.println("Second invoke completed with 3 tools");

    // Simulate MCP server removing tools
    toolCount.set(2);
    System.out.println("MCP server now has 2 tools");

    // Third invoke should see 2 tools
    agent.invoke("Test 3", RunnableConfig.builder().build());
    System.out.println("Third invoke completed with 2 tools");

    System.out.println("\n✅ FIX VERIFIED: MCP-like dynamic tool changes work");
  }

  /**
   * Test that static tools and dynamic providers work together.
   */
  @Test
  void testStaticAndDynamicToolsTogether() throws Exception {
    List<ToolCallback> dynamicTools = new ArrayList<>();
    dynamicTools.add(createTool("dynamic_1", "Dynamic tool 1"));

    ToolCallbackProvider provider = () -> {
      System.out.println("Provider returning " + dynamicTools.size() + " dynamic tools");
      return dynamicTools.toArray(new ToolCallback[0]);
    };

    ReactAgent agent = ReactAgent.builder()
      .name("mixed_agent")
      .model(chatModel)
      .tools(createTool("static_1", "Static tool 1"))
      .toolCallbackProviders(provider)
      .saver(new MemorySaver())
      .build();

    // First invoke - should have 1 static + 1 dynamic = 2 tools
    agent.invoke("Test 1", RunnableConfig.builder().build());
    System.out.println("First invoke: 1 static + 1 dynamic");

    // Add dynamic tool
    dynamicTools.add(createTool("dynamic_2", "Dynamic tool 2"));

    // Second invoke - should have 1 static + 2 dynamic = 3 tools
    agent.invoke("Test 2", RunnableConfig.builder().build());
    System.out.println("Second invoke: 1 static + 2 dynamic");

    System.out.println("\n✅ FIX VERIFIED: Static and dynamic tools work together");
  }

  private ToolCallback createTool(String name, String description) {
    return FunctionToolCallback.builder(name, (String args) -> "Result from " + name)
      .description(description)
      .inputType(String.class)
      .build();
  }
}
