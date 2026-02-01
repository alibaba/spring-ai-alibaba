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
package com.alibaba.cloud.ai.graph.streaming;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.OverAllStateBuilder;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for ToolStreamingOutput.
 *
 * <p>
 * Covers constructor, getters, chunk formatting, JSON escaping, and output type handling.
 * </p>
 *
 * @author Claude
 * @since 1.0.0
 */
@DisplayName("ToolStreamingOutput Tests")
class ToolStreamingOutputTest {

	private OverAllState testState;

	@BeforeEach
	void setUp() {
		testState = OverAllStateBuilder.builder()
			.withKeyStrategy("test_key", new ReplaceStrategy())
			.build()
			.input(Map.of("test_key", "test_value"));
	}

	@Nested
	@DisplayName("Constructor and Getter Tests")
	class ConstructorAndGetterTests {

		@Test
		@DisplayName("should initialize all fields correctly")
		void constructor_shouldInitializeFields() {
			ToolStreamingOutput<String> output = new ToolStreamingOutput<>("test data", "agent_tool", "testAgent",
					testState, OutputType.AGENT_TOOL_STREAMING, "call_123", "searchTool");

			assertEquals("call_123", output.getToolCallId());
			assertEquals("searchTool", output.getToolName());
			assertEquals("test data", output.getChunkData());
			assertEquals("test data", output.getOriginData());
			assertEquals(OutputType.AGENT_TOOL_STREAMING, output.getOutputType());
			assertEquals("agent_tool", output.node());
			assertEquals("testAgent", output.agent());
		}

		@Test
		@DisplayName("getToolCallId() should return correct value")
		void getToolCallId_shouldReturnCorrectValue() {
			ToolStreamingOutput<String> output = createTestOutput("call_abc_123", "myTool",
					OutputType.AGENT_TOOL_STREAMING);

			assertEquals("call_abc_123", output.getToolCallId());
		}

		@Test
		@DisplayName("getToolName() should return correct value")
		void getToolName_shouldReturnCorrectValue() {
			ToolStreamingOutput<String> output = createTestOutput("call_123", "searchAndRetrieveTool",
					OutputType.AGENT_TOOL_STREAMING);

			assertEquals("searchAndRetrieveTool", output.getToolName());
		}

		@Test
		@DisplayName("getChunkData() should return same as getOriginData()")
		void getChunkData_shouldReturnOriginData() {
			String data = "chunk content";
			ToolStreamingOutput<String> output = new ToolStreamingOutput<>(data, "node", "agent", testState,
					OutputType.AGENT_TOOL_STREAMING, "id", "tool");

			assertEquals(data, output.getChunkData());
			assertEquals(output.getOriginData(), output.getChunkData());
		}

	}

	@Nested
	@DisplayName("isFinalChunk Tests")
	class IsFinalChunkTests {

		@Test
		@DisplayName("should return true for AGENT_TOOL_FINISHED type")
		void isFinalChunk_shouldReturnTrue_forFinishedType() {
			ToolStreamingOutput<String> output = createTestOutput("call_123", "tool",
					OutputType.AGENT_TOOL_FINISHED);

			assertTrue(output.isFinalChunk());
		}

		@Test
		@DisplayName("should return false for AGENT_TOOL_STREAMING type")
		void isFinalChunk_shouldReturnFalse_forStreamingType() {
			ToolStreamingOutput<String> output = createTestOutput("call_123", "tool",
					OutputType.AGENT_TOOL_STREAMING);

			assertFalse(output.isFinalChunk());
		}

		@Test
		@DisplayName("should return false for other output types")
		void isFinalChunk_shouldReturnFalse_forOtherTypes() {
			ToolStreamingOutput<String> output = new ToolStreamingOutput<>("data", "node", "agent", testState,
					OutputType.GRAPH_NODE_STREAMING, "call_123", "tool");

			assertFalse(output.isFinalChunk());
		}

	}

	@Nested
	@DisplayName("chunk() Method Tests")
	class ChunkMethodTests {

		@Test
		@DisplayName("should format plain string data correctly")
		void chunk_shouldFormatPlainString() {
			ToolStreamingOutput<String> output = createTestOutput("call_123", "searchTool",
					OutputType.AGENT_TOOL_STREAMING, "Hello World");

			String chunk = output.chunk();

			assertNotNull(chunk);
			assertTrue(chunk.contains("\"toolCallId\":\"call_123\""));
			assertTrue(chunk.contains("\"toolName\":\"searchTool\""));
			assertTrue(chunk.contains("\"data\":\"Hello World\""));
		}

		@Test
		@DisplayName("should handle null data")
		void chunk_shouldHandleNullData() {
			ToolStreamingOutput<String> output = new ToolStreamingOutput<>(null, "node", "agent", testState,
					OutputType.AGENT_TOOL_STREAMING, "call_123", "tool");

			String chunk = output.chunk();

			assertNull(chunk);
		}

		@Test
		@DisplayName("should preserve JSON-like strings")
		void chunk_shouldPreserveJsonStrings() {
			String jsonData = "{\"key\":\"value\",\"count\":42}";
			ToolStreamingOutput<String> output = createTestOutput("call_123", "tool",
					OutputType.AGENT_TOOL_STREAMING, jsonData);

			String chunk = output.chunk();

			assertNotNull(chunk);
			assertTrue(chunk.contains("\"data\":{\"key\":\"value\",\"count\":42}"));
		}

		@Test
		@DisplayName("should preserve JSON array strings")
		void chunk_shouldPreserveJsonArrayStrings() {
			String jsonArray = "[\"item1\",\"item2\",\"item3\"]";
			ToolStreamingOutput<String> output = createTestOutput("call_123", "tool",
					OutputType.AGENT_TOOL_STREAMING, jsonArray);

			String chunk = output.chunk();

			assertNotNull(chunk);
			assertTrue(chunk.contains("\"data\":[\"item1\",\"item2\",\"item3\"]"));
		}

	}

	@Nested
	@DisplayName("JSON Escaping Tests")
	class JsonEscapingTests {

		@Test
		@DisplayName("should escape newlines in data")
		void escapeJson_shouldEscapeNewlines() {
			ToolStreamingOutput<String> output = createTestOutput("call_123", "tool",
					OutputType.AGENT_TOOL_STREAMING, "line1\nline2");

			String chunk = output.chunk();

			assertNotNull(chunk);
			assertTrue(chunk.contains("\\n"));
			assertFalse(chunk.contains("\n\""));
		}

		@Test
		@DisplayName("should escape quotes in data")
		void escapeJson_shouldEscapeQuotes() {
			ToolStreamingOutput<String> output = createTestOutput("call_123", "tool",
					OutputType.AGENT_TOOL_STREAMING, "say \"hello\"");

			String chunk = output.chunk();

			assertNotNull(chunk);
			assertTrue(chunk.contains("\\\"hello\\\""));
		}

		@Test
		@DisplayName("should escape backslashes in data")
		void escapeJson_shouldEscapeBackslashes() {
			ToolStreamingOutput<String> output = createTestOutput("call_123", "tool",
					OutputType.AGENT_TOOL_STREAMING, "path\\to\\file");

			String chunk = output.chunk();

			assertNotNull(chunk);
			assertTrue(chunk.contains("\\\\"));
		}

		@Test
		@DisplayName("should escape carriage returns")
		void escapeJson_shouldEscapeCarriageReturns() {
			ToolStreamingOutput<String> output = createTestOutput("call_123", "tool",
					OutputType.AGENT_TOOL_STREAMING, "line1\rline2");

			String chunk = output.chunk();

			assertNotNull(chunk);
			assertTrue(chunk.contains("\\r"));
		}

		@Test
		@DisplayName("should escape tabs")
		void escapeJson_shouldEscapeTabs() {
			ToolStreamingOutput<String> output = createTestOutput("call_123", "tool",
					OutputType.AGENT_TOOL_STREAMING, "col1\tcol2");

			String chunk = output.chunk();

			assertNotNull(chunk);
			assertTrue(chunk.contains("\\t"));
		}

		@Test
		@DisplayName("should escape toolCallId and toolName")
		void escapeJson_shouldEscapeToolFields() {
			ToolStreamingOutput<String> output = new ToolStreamingOutput<>("data", "node", "agent", testState,
					OutputType.AGENT_TOOL_STREAMING, "call\"123", "tool\"name");

			String chunk = output.chunk();

			assertNotNull(chunk);
			assertTrue(chunk.contains("call\\\"123"));
			assertTrue(chunk.contains("tool\\\"name"));
		}

		@Test
		@DisplayName("should handle empty string")
		void escapeJson_shouldHandleEmptyString() {
			ToolStreamingOutput<String> output = createTestOutput("call_123", "tool",
					OutputType.AGENT_TOOL_STREAMING, "");

			String chunk = output.chunk();

			assertNotNull(chunk);
			assertTrue(chunk.contains("\"data\":\"\""));
		}

	}

	@Nested
	@DisplayName("toString Tests")
	class ToStringTests {

		@Test
		@DisplayName("should return formatted string with all fields")
		void toString_shouldReturnFormattedString() {
			ToolStreamingOutput<String> output = createTestOutput("call_123", "searchTool",
					OutputType.AGENT_TOOL_STREAMING);

			String str = output.toString();

			assertNotNull(str);
			assertTrue(str.contains("ToolStreamingOutput"));
			assertTrue(str.contains("toolCallId=call_123"));
			assertTrue(str.contains("toolName=searchTool"));
			assertTrue(str.contains("outputType=AGENT_TOOL_STREAMING"));
			assertTrue(str.contains("isFinal=false"));
		}

		@Test
		@DisplayName("should show isFinal=true for finished type")
		void toString_shouldShowIsFinalTrue_forFinishedType() {
			ToolStreamingOutput<String> output = createTestOutput("call_123", "tool",
					OutputType.AGENT_TOOL_FINISHED);

			String str = output.toString();

			assertTrue(str.contains("isFinal=true"));
		}

	}

	/**
	 * Tests for JSON validation fix (P2 bug).
	 * Verifies that brace-delimited non-JSON strings are correctly escaped
	 * instead of being inlined as raw JSON.
	 */
	@Nested
	@DisplayName("JSON Validation Tests (P2 Bug Fix)")
	class JsonValidationTests {

		@Test
		@DisplayName("should escape brace-delimited non-JSON string {username}")
		void chunk_shouldEscapeBraceDelimitedNonJson_username() {
			ToolStreamingOutput<String> output = createTestOutput("call_123", "tool",
					OutputType.AGENT_TOOL_STREAMING, "{username}");

			String chunk = output.chunk();

			assertNotNull(chunk);
			// Should be escaped as a string, not inlined as JSON
			assertTrue(chunk.contains("\"data\":\"{username}\""),
					"Expected {username} to be escaped as string but got: " + chunk);
		}

		@Test
		@DisplayName("should escape unquoted key-value {foo: bar}")
		void chunk_shouldEscapeUnquotedKeyValue() {
			ToolStreamingOutput<String> output = createTestOutput("call_123", "tool",
					OutputType.AGENT_TOOL_STREAMING, "{foo: bar}");

			String chunk = output.chunk();

			assertNotNull(chunk);
			// Should be escaped as a string since it's not valid JSON
			assertTrue(chunk.contains("\"data\":\"{foo: bar}\""),
					"Expected {foo: bar} to be escaped as string but got: " + chunk);
		}

		@Test
		@DisplayName("should escape array-like non-JSON [a, b, c]")
		void chunk_shouldEscapeArrayLikeNonJson() {
			ToolStreamingOutput<String> output = createTestOutput("call_123", "tool",
					OutputType.AGENT_TOOL_STREAMING, "[a, b, c]");

			String chunk = output.chunk();

			assertNotNull(chunk);
			// Should be escaped as a string since it's not valid JSON array
			assertTrue(chunk.contains("\"data\":\"[a, b, c]\""),
					"Expected [a, b, c] to be escaped as string but got: " + chunk);
		}

		@Test
		@DisplayName("should escape brace string with nested content {user {name}}")
		void chunk_shouldEscapeNestedBraceContent() {
			ToolStreamingOutput<String> output = createTestOutput("call_123", "tool",
					OutputType.AGENT_TOOL_STREAMING, "{user {name}}");

			String chunk = output.chunk();

			assertNotNull(chunk);
			// Should be escaped as a string
			assertTrue(chunk.contains("\"data\":\"{user {name}}\""),
					"Expected {user {name}} to be escaped as string but got: " + chunk);
		}

		@Test
		@DisplayName("should escape partial JSON-like content")
		void chunk_shouldEscapePartialJsonContent() {
			ToolStreamingOutput<String> output = createTestOutput("call_123", "tool",
					OutputType.AGENT_TOOL_STREAMING, "{\"name\":}");

			String chunk = output.chunk();

			assertNotNull(chunk);
			// Invalid JSON (missing value) should be escaped
			assertTrue(chunk.contains("\"data\":\"{\\\"name\\\":}\""),
					"Expected incomplete JSON to be escaped as string but got: " + chunk);
		}

		@Test
		@DisplayName("should preserve valid empty JSON object")
		void chunk_shouldPreserveValidEmptyJsonObject() {
			ToolStreamingOutput<String> output = createTestOutput("call_123", "tool",
					OutputType.AGENT_TOOL_STREAMING, "{}");

			String chunk = output.chunk();

			assertNotNull(chunk);
			// Valid empty JSON should be inlined
			assertTrue(chunk.contains("\"data\":{}"),
					"Expected {} to be inlined as JSON but got: " + chunk);
		}

		@Test
		@DisplayName("should preserve valid empty JSON array")
		void chunk_shouldPreserveValidEmptyJsonArray() {
			ToolStreamingOutput<String> output = createTestOutput("call_123", "tool",
					OutputType.AGENT_TOOL_STREAMING, "[]");

			String chunk = output.chunk();

			assertNotNull(chunk);
			// Valid empty array should be inlined
			assertTrue(chunk.contains("\"data\":[]"),
					"Expected [] to be inlined as JSON but got: " + chunk);
		}

		@Test
		@DisplayName("should preserve valid nested JSON")
		void chunk_shouldPreserveValidNestedJson() {
			String validJson = "{\"user\":{\"name\":\"John\",\"age\":30},\"active\":true}";
			ToolStreamingOutput<String> output = createTestOutput("call_123", "tool",
					OutputType.AGENT_TOOL_STREAMING, validJson);

			String chunk = output.chunk();

			assertNotNull(chunk);
			// Valid JSON should be inlined without extra quotes
			assertTrue(chunk.contains("\"data\":{\"user\":{\"name\":\"John\",\"age\":30},\"active\":true}"),
					"Expected valid nested JSON to be inlined but got: " + chunk);
		}

		@Test
		@DisplayName("should preserve valid JSON array with objects")
		void chunk_shouldPreserveValidJsonArrayWithObjects() {
			String validJsonArray = "[{\"id\":1},{\"id\":2}]";
			ToolStreamingOutput<String> output = createTestOutput("call_123", "tool",
					OutputType.AGENT_TOOL_STREAMING, validJsonArray);

			String chunk = output.chunk();

			assertNotNull(chunk);
			// Valid JSON array should be inlined
			assertTrue(chunk.contains("\"data\":[{\"id\":1},{\"id\":2}]"),
					"Expected valid JSON array to be inlined but got: " + chunk);
		}

		@Test
		@DisplayName("should escape template variable style string")
		void chunk_shouldEscapeTemplateVariableStyle() {
			ToolStreamingOutput<String> output = createTestOutput("call_123", "tool",
					OutputType.AGENT_TOOL_STREAMING, "Hello {name}, welcome!");

			String chunk = output.chunk();

			assertNotNull(chunk);
			// Not JSON (doesn't start with { or [), should be plain string
			assertTrue(chunk.contains("\"data\":\"Hello {name}, welcome!\""),
					"Expected template string to be escaped as string but got: " + chunk);
		}

	}

	@Nested
	@DisplayName("ToolResult Reflection Tests")
	class ToolResultReflectionTests {

		@Test
		@DisplayName("should use toStringResult() for objects with that method")
		void chunk_shouldUseToStringResult_whenMethodExists() {
			// Create a mock object that has toStringResult() method
			MockToolResult mockResult = new MockToolResult("test content");
			ToolStreamingOutput<MockToolResult> output = new ToolStreamingOutput<>(mockResult, "node", "agent",
					testState, OutputType.AGENT_TOOL_STREAMING, "call_123", "tool");

			String chunk = output.chunk();

			assertNotNull(chunk);
			// Should use the content from toStringResult()
			assertTrue(chunk.contains("test content"));
		}

		@Test
		@DisplayName("should fall back to toString() for objects without toStringResult()")
		void chunk_shouldFallbackToString_whenNoToStringResult() {
			SimpleObject obj = new SimpleObject("fallback value");
			ToolStreamingOutput<SimpleObject> output = new ToolStreamingOutput<>(obj, "node", "agent", testState,
					OutputType.AGENT_TOOL_STREAMING, "call_123", "tool");

			String chunk = output.chunk();

			assertNotNull(chunk);
			assertTrue(chunk.contains("SimpleObject"));
		}

	}

	// Helper methods

	private ToolStreamingOutput<String> createTestOutput(String toolCallId, String toolName, OutputType outputType) {
		return createTestOutput(toolCallId, toolName, outputType, "test data");
	}

	private ToolStreamingOutput<String> createTestOutput(String toolCallId, String toolName, OutputType outputType,
			String data) {
		return new ToolStreamingOutput<>(data, "agent_tool", "testAgent", testState, outputType, toolCallId, toolName);
	}

	/**
	 * Mock class that simulates ToolResult with toStringResult() method.
	 */
	static class MockToolResult {

		private final String content;

		MockToolResult(String content) {
			this.content = content;
		}

		public String toStringResult() {
			return content;
		}

		@Override
		public String toString() {
			return "MockToolResult{content='" + content + "'}";
		}

	}

	/**
	 * Simple object without toStringResult() method.
	 */
	static class SimpleObject {

		private final String value;

		SimpleObject(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return "SimpleObject{value='" + value + "'}";
		}

	}

}
