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
package io.agentscope.core.tool;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;

import static org.junit.jupiter.api.Assertions.*;

class ToolkitTest {

	private Toolkit toolkit;

	@BeforeEach
	void setUp() {
		toolkit = new Toolkit();
	}

	@Test
	void testRegisterToolWithAnnotatedMethods() {
		// Given
		TestToolObject testTool = new TestToolObject();

		// When
		toolkit.registerTool(testTool);

		// Then
		ToolCallback[] callbacks = toolkit.toolCallbackProvider().getToolCallbacks();
		assertEquals(3, callbacks.length);

		// Verify tool names and descriptions
		boolean foundCalculator = false;
		boolean foundGreeting = false;
		boolean foundWeather = false;

		for (ToolCallback callback : callbacks) {
			String toolName = callback.getToolDefinition().name();
			switch (toolName) {
				case "calculator":
					foundCalculator = true;
					assertEquals("A simple calculator", callback.getToolDefinition().description());
					break;
				case "greeting":
					foundGreeting = true;
					assertEquals("Tool: greeting", callback.getToolDefinition().description());
					break;
				case "get_weather":
					foundWeather = true;
					assertEquals("Get weather information", callback.getToolDefinition().description());
					break;
			}
		}

		assertTrue(foundCalculator);
		assertTrue(foundGreeting);
		assertTrue(foundWeather);
	}

	@Test
	void testRegisterToolWithNoAnnotatedMethods() {
		// Given
		Object plainObject = new Object();

		// When
		toolkit.registerTool(plainObject);

		// Then
		ToolCallback[] callbacks = toolkit.toolCallbackProvider().getToolCallbacks();
		assertEquals(0, callbacks.length);
	}

	@Test
	void testRegisterNullTool() {
		// When & Then
		assertThrows(IllegalArgumentException.class, () -> toolkit.registerTool(null));
	}

	// Test tool object with various annotated methods
	static class TestToolObject {

		@Tool(name = "calculator", description = "A simple calculator")
		public int add(AddRequest request) {
			return request.a + request.b;
		}

		@Tool
		public String greeting() {
			return "Hello, World!";
		}

		@Tool(name = "get_weather", description = "Get weather information", returnDirect = true)
		public String getWeather(String location) {
			return "Weather in " + location + ": Sunny, 25°C";
		}

		// Method without @Tool annotation - should be ignored
		public String notATool() {
			return "This should not be registered";
		}

	}

	// Request class for the calculator tool
	static class AddRequest {

		public int a;

		public int b;

	}

}
