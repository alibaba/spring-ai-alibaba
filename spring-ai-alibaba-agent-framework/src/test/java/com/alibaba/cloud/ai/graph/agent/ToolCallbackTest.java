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
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.method.MethodToolCallback;
import org.springframework.ai.tool.resolution.StaticToolCallbackResolver;
import org.springframework.ai.tool.support.ToolDefinitions;
import org.springframework.util.ReflectionUtils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import reactor.core.publisher.Flux;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test cases demonstrating different ways to provide tools to ReactAgent:
 * 1. MethodTools - Using @Tool annotation on methods
 * 2. ToolCallbackProvider - Using ToolCallbackProvider interface
 * 3. ToolNames - Using tool names with ToolCallbackResolver
 * 4. ToolCallbackResolver - Custom tool resolution
 */
@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
class ToolCallbackTest {

	private ChatModel chatModel;

	// ==================== Tool Classes for Testing ====================

	/**
	 * Tool class using @Tool annotation for method-based tools
	 */
	public static class CalculatorTools {
		public static int callCount = 0;

		@Tool(description = "Add two numbers together")
		public String add(
				@ToolParam(description = "First number") int a,
				@ToolParam(description = "Second number") int b) {
			callCount++;
			return String.valueOf(a + b);
		}

		@Tool(description = "Multiply two numbers together")
		public String multiply(
				@ToolParam(description = "First number") int a,
				@ToolParam(description = "Second number") int b) {
			callCount++;
			return String.valueOf(a * b);
		}

	}

	/**
	 * Tool class with no-parameter method using @Tool annotation
	 */
	public static class SystemInfoTools {
		public static int callCount = 0;

		@Tool(description = "Get current system time in milliseconds")
		public String getCurrentTime() {
			callCount++;
			return "Current time: " + System.currentTimeMillis() + " ms";
		}

		@Tool(description = "Get system information")
		public String getSystemInfo() {
			callCount++;
			return String.format("System: %s, Java Version: %s, Available Processors: %d",
					System.getProperty("os.name"),
					System.getProperty("java.version"),
					Runtime.getRuntime().availableProcessors());
		}
	}

	/**
	 * Tool class using Function interface
	 */
	public static class SearchTool implements BiFunction<String, ToolContext, String> {
		@Override
		public String apply(String query, ToolContext toolContext) {
			return "Search results for: " + query;
		}
	}

	/**
	 * ToolCallbackProvider implementation
	 */
	public static class CustomToolCallbackProvider implements ToolCallbackProvider {
		private final List<ToolCallback> toolCallbacks;

		public CustomToolCallbackProvider(List<ToolCallback> toolCallbacks) {
			this.toolCallbacks = toolCallbacks;
		}

		@Override
		public ToolCallback[] getToolCallbacks() {
			return toolCallbacks.toArray(new ToolCallback[0]);
		}
	}

	/**
	 * Request class for search tool with composite type
	 */
	public static class SearchRequest {
		@JsonProperty(required = true)
		@JsonPropertyDescription("The search query string")
		public String query;

		public SearchRequest() {
		}

		public SearchRequest(String query) {
			this.query = query;
		}
	}

	/**
	 * Search function using composite type
	 */
	public static class SearchFunction implements BiFunction<SearchRequest, ToolContext, String> {
		@Override
		public String apply(SearchRequest request, ToolContext toolContext) {
			return "Search results for: " + request.query;
		}
	}

	/**
	 * Request class for calculator tool with composite type
	 */
	public static class CalculatorRequest {
		@JsonProperty(required = true)
		@JsonPropertyDescription("First number for the calculation")
		public int a;

		@JsonProperty(required = true)
		@JsonPropertyDescription("Second number for the calculation")
		public int b;

		public CalculatorRequest() {
		}

		public CalculatorRequest(int a, int b) {
			this.a = a;
			this.b = b;
		}
	}

	/**
	 * Calculator function using composite type
	 */
	public static class CalculatorFunction implements BiFunction<CalculatorRequest, ToolContext, String> {
		@Override
		public String apply(CalculatorRequest request, ToolContext toolContext) {
			return String.valueOf(request.a + request.b);
		}
	}

	@BeforeEach
	void setUp() {
		// Create DashScopeApi instance using the API key from environment variable
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		// Create DashScope ChatModel instance
		this.chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();
	}

	/**
	 * Test 1: Using methodTools with @Tool annotation
	 * Demonstrates how to use methods annotated with @Tool as tools
	 */
	@Test
	public void testMethodTools() throws Exception {
		CalculatorTools calculatorTools = new CalculatorTools();

		ReactAgent agent = ReactAgent.builder()
				.name("calculator_agent")
				.model(chatModel)
				.description("An agent that can perform calculations")
				.instruction("You are a helpful calculator assistant. Use the available tools to perform calculations.")
				.methodTools(calculatorTools)  // Pass the object with @Tool annotated methods
				.saver(new MemorySaver())
				.enableLogging(true)
				.build();

		try {
			Optional<OverAllState> result = agent.invoke("What is 15 + 27?");

			assertTrue(result.isPresent(), "Result should be present");
			OverAllState state = result.get();
			assertTrue(state.value("messages").isPresent(), "Messages should be present in state");

			System.out.println("=== MethodTools Test ===");
			System.out.println(result.get());
		}
		catch (java.util.concurrent.CompletionException e) {
			e.printStackTrace();
			fail("MethodTools execution failed: " + e.getMessage());
		}
	}

	/**
	 * Test 2: Using ToolCallbackProvider
	 * Demonstrates how to use ToolCallbackProvider to provide tools dynamically
	 */
	@Test
	public void testToolCallbackProvider() throws Exception {
		// Create tools
		ToolCallback searchTool = FunctionToolCallback.builder("search", new SearchTool())
				.description("Search for information")
				.inputType(String.class)
				.build();

		// Create ToolCallbackProvider
		ToolCallbackProvider toolProvider = new CustomToolCallbackProvider(List.of(searchTool));

		ReactAgent agent = ReactAgent.builder()
				.name("search_agent")
				.model(chatModel)
				.description("An agent that can search for information")
				.instruction("You are a helpful assistant with search capabilities.")
				.toolCallbackProviders(toolProvider)  // Use ToolCallbackProvider
				.saver(new MemorySaver())
				.enableLogging(true)
				.build();

		try {
			Optional<OverAllState> result = agent.invoke("Search for information about Spring AI");

			assertTrue(result.isPresent(), "Result should be present");
			OverAllState state = result.get();
			assertTrue(state.value("messages").isPresent(), "Messages should be present in state");

			System.out.println("=== ToolCallbackProvider Test ===");
			System.out.println(result.get());
		}
		catch (java.util.concurrent.CompletionException e) {
			e.printStackTrace();
			fail("ToolCallbackProvider execution failed: " + e.getMessage());
		}
	}

	/**
	 * Test 3: Using toolNames with ToolCallbackResolver
	 * Demonstrates how to use tool names with a resolver to dynamically resolve tools
	 */
	@Test
	public void testToolNamesWithResolver() throws Exception {
		// Create tools with composite input types
		ToolCallback searchTool = FunctionToolCallback.builder("search", new SearchFunction())
				.description("Search for information")
				.inputType(SearchRequest.class)
				.build();

		ToolCallback calculatorTool = FunctionToolCallback.builder("calculator", new CalculatorFunction())
				.description("Perform arithmetic calculations")
				.inputType(CalculatorRequest.class)
				.build();

		// Create StaticToolCallbackResolver with the tools
		StaticToolCallbackResolver resolver = new StaticToolCallbackResolver(
				List.of(calculatorTool, searchTool));

		ReactAgent agent = ReactAgent.builder()
				.name("multi_tool_agent")
				.model(chatModel)
				.description("An agent with multiple tools")
				.instruction("You are a helpful assistant with access to calculator and search tools.")
				.toolNames("calculator", "search")  // Use tool names instead of ToolCallback instances
				.resolver(resolver)  // Provide the resolver to resolve tool names
				.saver(new MemorySaver())
				.enableLogging(true)
				.build();

		try {
			Optional<OverAllState> result = agent.invoke("Calculate 25 + 4 and then search for information about the result");

			assertTrue(result.isPresent(), "Result should be present");
			OverAllState state = result.get();
			assertTrue(state.value("messages").isPresent(), "Messages should be present in state");

			System.out.println("=== ToolNames with Resolver Test ===");
			System.out.println(result.get());
		}
		catch (java.util.concurrent.CompletionException e) {
			e.printStackTrace();
			fail("ToolNames with Resolver execution failed: " + e.getMessage());
		}
	}

	/**
	 * Test 4: Using ToolCallbackResolver directly
	 * Demonstrates how to use a custom ToolCallbackResolver for tool resolution
	 * Uses MethodToolCallback instead of FunctionToolCallback to support JSON format input
	 */
	@Test
	public void testToolCallbackResolver() throws Exception {
		// Create a calculator tool class with a method for calculations
		class CalculatorTool {
			public String calculate(String expression, ToolContext toolContext) {
				// Simple calculation parsing (for demo purposes)
				if (expression.contains("/")) {
					String[] parts = expression.split("/");
					double result = Double.parseDouble(parts[0].trim()) / Double.parseDouble(parts[1].trim());
					return String.valueOf(result);
				}
				return "Calculation result for: " + expression;
			}
		}

		// Create MethodToolCallback using reflection
		CalculatorTool calculatorToolInstance = new CalculatorTool();
		java.lang.reflect.Method method = ReflectionUtils.findMethod(CalculatorTool.class, 
				"calculate", String.class, ToolContext.class);
		
		if (method == null) {
			fail("Could not find calculate method in CalculatorTool class");
		}

		// Create ToolDefinition using ToolDefinitions.builder() with the method
		ToolDefinition toolDefinition = ToolDefinitions.builder(method)
				.name("calculator")
				.description("Perform arithmetic calculations")
				.build();

		// Build MethodToolCallback
		ToolCallback calculatorTool = MethodToolCallback.builder()
				.toolDefinition(toolDefinition)
				.toolMethod(method)
				.toolObject(calculatorToolInstance)
				.build();

		// Create a custom resolver
		StaticToolCallbackResolver resolver = new StaticToolCallbackResolver(
				List.of(calculatorTool));

		ReactAgent agent = ReactAgent.builder()
				.name("resolver_agent")
				.model(chatModel)
				.description("An agent using ToolCallbackResolver")
				.instruction("You are a helpful calculator assistant.")
				.tools(calculatorTool)  // Direct tool assignment
				.resolver(resolver)  // Also set resolver for tool node
				.saver(new MemorySaver())
				.enableLogging(true)
				.build();

		try {
			Optional<OverAllState> result = agent.invoke("What is 100 divided by 4?");

			assertTrue(result.isPresent(), "Result should be present");
			OverAllState state = result.get();
			assertTrue(state.value("messages").isPresent(), "Messages should be present in state");

			System.out.println("=== ToolCallbackResolver Test ===");
			System.out.println(result.get());
		}
		catch (java.util.concurrent.CompletionException e) {
			e.printStackTrace();
			fail("ToolCallbackResolver execution failed: " + e.getMessage());
		}
	}

	/**
	 * Test 5: Combining multiple tool provision methods
	 * Demonstrates combining methodTools, ToolCallbackProvider, and direct tools
	 */
	@Test
	public void testCombinedToolProvision() throws Exception {
		// Method tools
		CalculatorTools calculatorTools = new CalculatorTools();

		// Direct tool
		ToolCallback searchTool = FunctionToolCallback.builder("search", new SearchTool())
				.description("Search for information")
				.inputType(String.class)
				.build();

		// ToolCallbackProvider
		ToolCallbackProvider toolProvider = new CustomToolCallbackProvider(List.of(searchTool));

		ReactAgent agent = ReactAgent.builder()
				.name("combined_tool_agent")
				.model(chatModel)
				.description("An agent with multiple tool provision methods")
				.instruction("You are a helpful assistant with calculator and search capabilities.")
				.methodTools(calculatorTools)  // Method-based tools
				.toolCallbackProviders(toolProvider)  // Provider-based tools
				.tools(searchTool)  // Direct tools
				.saver(new MemorySaver())
				.enableLogging(true)
				.build();

		try {
			Optional<OverAllState> result = agent.invoke("Calculate 50 + 75 and search for information about mathematics");

			assertTrue(result.isPresent(), "Result should be present");
			OverAllState state = result.get();
			assertTrue(state.value("messages").isPresent(), "Messages should be present in state");

			System.out.println("=== Combined Tool Provision Test ===");
			System.out.println(result.get());
		}
		catch (java.util.concurrent.CompletionException e) {
			e.printStackTrace();
			fail("Combined tool provision execution failed: " + e.getMessage());
		}
	}

	/**
	 * Test 6: Using methodTools with multiple tool objects
	 * Demonstrates passing multiple objects with @Tool annotated methods
	 */
	@Test
	public void testMultipleMethodTools() throws Exception {
		CalculatorTools calculatorTools = new CalculatorTools();

		// Another tool class with @Tool methods
		class WeatherTools {
			@Tool(description = "Get current weather for a location")
			public String getWeather(@ToolParam(description = "City name") String city) {
				return "Sunny, 25°C in " + city;
			}
		}

		WeatherTools weatherTools = new WeatherTools();

		ReactAgent agent = ReactAgent.builder()
				.name("multi_method_tool_agent")
				.model(chatModel)
				.description("An agent with multiple method-based tools")
				.instruction("You are a helpful assistant with calculator and weather tools.")
				.methodTools(calculatorTools, weatherTools)  // Multiple tool objects
				.saver(new MemorySaver())
				.enableLogging(true)
				.build();

		try {
			Optional<OverAllState> result = agent.invoke("What is 10 * 8 and what's the weather in Beijing?");

			assertTrue(result.isPresent(), "Result should be present");
			OverAllState state = result.get();
			assertTrue(state.value("messages").isPresent(), "Messages should be present in state");

			System.out.println("=== Multiple MethodTools Test ===");
			System.out.println(result.get());
		}
		catch (java.util.concurrent.CompletionException e) {
			e.printStackTrace();
			fail("Multiple methodTools execution failed: " + e.getMessage());
		}
	}

	/**
	 * Test 7: Using methodTools with no-parameter methods
	 * Demonstrates how to use @Tool annotation on methods without parameters
	 */
	@Test
	public void testMethodToolsWithNoParameters() throws Exception {
		SystemInfoTools systemInfoTools = new SystemInfoTools();
		SystemInfoTools.callCount = 0; // Reset call count

		ReactAgent agent = ReactAgent.builder()
				.name("system_info_agent")
				.model(chatModel)
				.description("An agent that can get system information")
				.instruction("You are a helpful assistant that can provide system information. Use the available tools to get system details.")
				.methodTools(systemInfoTools)  // Pass the object with @Tool annotated methods (no parameters)
				.saver(new MemorySaver())
				.enableLogging(true)
				.build();

		try {
			Optional<OverAllState> result = agent.invoke("What is the current system time? Please use the tool to get it.");

			assertTrue(result.isPresent(), "Result should be present");
			OverAllState state = result.get();
			assertTrue(state.value("messages").isPresent(), "Messages should be present in state");

			// Verify that the tool was called
			assertTrue(SystemInfoTools.callCount > 0, 
					"SystemInfoTools should have been called at least once. Call count: " + SystemInfoTools.callCount);

			System.out.println("=== MethodTools with No Parameters Test ===");
			System.out.println("Tool call count: " + SystemInfoTools.callCount);
			System.out.println(result.get());
		}
		catch (java.util.concurrent.CompletionException e) {
			e.printStackTrace();
			fail("MethodTools with no parameters execution failed: " + e.getMessage());
		}
	}

	/**
	 * Test 8: Using agent.stream() with tool calls
	 * Demonstrates streaming agent execution with tool invocation
	 */
	@Test
	public void testStreamWithToolCalls() throws Exception {
		CalculatorTools calculatorTools = new CalculatorTools();
		CalculatorTools.callCount = 0; // Reset call count

		ReactAgent agent = ReactAgent.builder()
				.name("streaming_calculator_agent")
				.model(chatModel)
				.description("An agent that can perform calculations using streaming")
				.instruction("You are a helpful calculator assistant. Use the available tools to perform calculations.")
				.methodTools(calculatorTools)  // Pass the object with @Tool annotated methods
				.saver(new MemorySaver())
				.enableLogging(true)
				.build();

		try {
			// Use stream() instead of invoke()
			Flux<NodeOutput> stream = agent.stream("What is 23 + 45? Please calculate it step by step.");

			// Track if we received any outputs and if tool was called
			AtomicBoolean receivedOutput = new AtomicBoolean(false);
			AtomicBoolean toolCalled = new AtomicBoolean(false);

			// Process stream outputs
			stream.doOnNext(output -> {
				receivedOutput.set(true);
				
				System.out.println("=== Stream Output ===");
				System.out.println("Node: " + output.node());
				System.out.println("Agent: " + output.agent());
				
				if (output.tokenUsage() != null) {
					System.out.println("Token Usage: " + output.tokenUsage());
				}

				// Check if this is a streaming output with messages
				if (output instanceof StreamingOutput<?> streamingOutput) {
					System.out.println("Streaming Output received");
					
					// Check if tool was called by examining the call count
					if (CalculatorTools.callCount > 0) {
						toolCalled.set(true);
						System.out.println("Tool was called! Call count: " + CalculatorTools.callCount);
					}
				}
			})
			.doOnError(error -> {
				System.err.println("Stream error: " + error.getMessage());
				error.printStackTrace();
				fail("Stream execution failed: " + error.getMessage());
			})
			.doOnComplete(() -> {
				System.out.println("=== Stream completed ===");
				assertTrue(receivedOutput.get(), "Should have received at least one output");
				assertTrue(toolCalled.get() || CalculatorTools.callCount > 0, 
						"Tool should have been called during stream execution");
			})
			.blockLast(); // Block until stream completes

			// Verify tool was actually called
			assertTrue(CalculatorTools.callCount > 0, 
					"Calculator tool should have been called at least once");
			
			System.out.println("=== Stream with Tool Calls Test Completed ===");
			System.out.println("Total tool calls: " + CalculatorTools.callCount);
		}
		catch (Exception e) {
			e.printStackTrace();
			fail("Stream with tool calls execution failed: " + e.getMessage());
		}
	}

}

