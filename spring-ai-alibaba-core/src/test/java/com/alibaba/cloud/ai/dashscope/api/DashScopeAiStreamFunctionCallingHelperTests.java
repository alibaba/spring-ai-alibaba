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
package com.alibaba.cloud.ai.dashscope.api;

import java.util.List;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.ChatCompletionChunk;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.ChatCompletionFinishReason;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.ChatCompletionMessage;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.ChatCompletionMessage.ChatCompletionFunction;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.ChatCompletionMessage.Role;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.ChatCompletionMessage.ToolCall;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.ChatCompletionOutput;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.ChatCompletionOutput.Choice;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.TokenUsage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for DashScopeAiStreamFunctionCallingHelper class functionality
 *
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 * @author brianxiadong
 * @since 1.0.0-M2
 */
public class DashScopeAiStreamFunctionCallingHelperTests {

	private DashScopeAiStreamFunctionCallingHelper helper;

	private DashScopeAiStreamFunctionCallingHelper helperWithIncrementalOutput;

	@BeforeEach
	void setUp() {
		// Create default helper (incrementalOutput = false)
		helper = new DashScopeAiStreamFunctionCallingHelper();
		// Create helper with incrementalOutput = true
		helperWithIncrementalOutput = new DashScopeAiStreamFunctionCallingHelper(true);
	}

	@Test
	void testMergeWithNullPrevious() {
		// Test merge logic when previous is null
		ChatCompletionChunk current = createSimpleChunk("request-1", "Hello", Role.ASSISTANT, null);

		ChatCompletionChunk result = helper.merge(null, current);

		assertNotNull(result);
		assertEquals("request-1", result.requestId());
		assertEquals("Hello", result.output().choices().get(0).message().content());
	}

	@Test
	void testMergeWithIncrementalContent() {
		// Test incremental merging of text content
		ChatCompletionChunk previous = createSimpleChunk("request-1", "Hello", Role.ASSISTANT, null);
		ChatCompletionChunk current = createSimpleChunk("request-1", " World", Role.ASSISTANT, null);

		ChatCompletionChunk result = helper.merge(previous, current);

		assertNotNull(result);
		assertEquals("request-1", result.requestId());
		assertEquals(" World", result.output().choices().get(0).message().content());
	}

	@Test
	void testMergeWithToolCalls() {
		// Test merging of tool calls
		ChatCompletionChunk previous = createChunkWithToolCall("request-1", "tool-1", "function-1", "{\"param");
		ChatCompletionChunk current = createChunkWithToolCall("request-1", "tool-1", "function-1", "\":\"value\"}");

		ChatCompletionChunk result = helperWithIncrementalOutput.merge(previous, current);

		assertNotNull(result);
		assertEquals("request-1", result.requestId());
		List<ToolCall> toolCalls = result.output().choices().get(0).message().toolCalls();
		assertNotNull(toolCalls);
		// According to DashScopeAiStreamFunctionCallingHelper implementation, it keeps
		// all tool calls from previous
		// and adds tool calls from current, so there should be 2 tool calls here
		assertEquals(2, toolCalls.size());
		assertEquals("tool-1", toolCalls.get(0).id());
		assertEquals("function-1", toolCalls.get(0).function().name());
		assertEquals("{\"param", toolCalls.get(0).function().arguments());
		assertEquals("tool-1", toolCalls.get(1).id());
		assertEquals("function-1", toolCalls.get(1).function().name());
		assertEquals("\":\"value\"}", toolCalls.get(1).function().arguments());
	}

	@Test
	void testIsStreamingToolFunctionCall() {
		// Test isStreamingToolFunctionCall method
		ChatCompletionChunk chunkWithTool = createChunkWithToolCall("request-1", "tool-1", "function-1",
				"{\"param\":\"value\"}");
		ChatCompletionChunk chunkWithoutTool = createSimpleChunk("request-1", "Hello", Role.ASSISTANT, null);

		assertTrue(helper.isStreamingToolFunctionCall(chunkWithTool));
		assertFalse(helper.isStreamingToolFunctionCall(chunkWithoutTool));
	}

	@Test
	void testIsStreamingToolFunctionCallFinish() {
		// Test isStreamingToolFunctionCallFinish method
		ChatCompletionChunk chunkWithToolFinish = createChunkWithToolCall("request-1", "tool-1", "function-1",
				"{\"param\":\"value\"}", ChatCompletionFinishReason.TOOL_CALLS);
		ChatCompletionChunk chunkWithToolNotFinish = createChunkWithToolCall("request-1", "tool-1", "function-1",
				"{\"param\":\"value\"}", null);

		assertTrue(helper.isStreamingToolFunctionCallFinish(chunkWithToolFinish));
		assertFalse(helper.isStreamingToolFunctionCallFinish(chunkWithToolNotFinish));
	}

	@Test
	void testChunkToChatCompletion() {
		// Test chunkToChatCompletion method
		ChatCompletionChunk chunk = createSimpleChunk("request-1", "Hello", Role.ASSISTANT, null);

		var chatCompletion = helper.chunkToChatCompletion(chunk);

		assertNotNull(chatCompletion);
		assertEquals("request-1", chatCompletion.requestId());
		assertEquals("Hello", chatCompletion.output().choices().get(0).message().content());
	}

	@Test
	void testMergeWithNonIncrementalOutput() {
		// Test merging in non-incremental output mode
		ChatCompletionChunk previous = createChunkWithToolCall("request-1", "tool-1", "function-1", "{\"param");
		ChatCompletionChunk current = createChunkWithToolCall("request-1", "tool-1", "function-1",
				"{\"param\":\"value\"}");

		// Use helper with non-incremental output
		ChatCompletionChunk result = helper.merge(previous, current);

		// In non-incremental output mode, unfinished tool calls should return empty
		// message
		assertNotNull(result);
		assertEquals("request-1", result.requestId());
		assertNotNull(result.output().choices());
		assertEquals(0, result.output().choices().size());
	}

	@Test
	void testMergeWithNonIncrementalOutputFinished() {
		// Test merging of finished tool calls in non-incremental output mode
		ChatCompletionChunk previous = createChunkWithToolCall("request-1", "tool-1", "function-1", "{\"param");
		ChatCompletionChunk current = createChunkWithToolCall("request-1", "tool-1", "function-1",
				"{\"param\":\"value\"}", ChatCompletionFinishReason.TOOL_CALLS);

		// Use helper with non-incremental output
		ChatCompletionChunk result = helper.merge(previous, current);

		// In non-incremental output mode, finished tool calls should return current
		// message
		assertNotNull(result);
		assertEquals("request-1", result.requestId());
		assertNotNull(result.output().choices());
		assertEquals(1, result.output().choices().size());
		assertNotNull(result.output().choices().get(0).message());
		List<ToolCall> toolCalls = result.output().choices().get(0).message().toolCalls();
		assertNotNull(toolCalls);
		assertEquals(1, toolCalls.size());
		assertEquals("tool-1", toolCalls.get(0).id());
		assertEquals("function-1", toolCalls.get(0).function().name());
		assertEquals("{\"param\":\"value\"}", toolCalls.get(0).function().arguments());
	}

	@Test
	void testMergeWithMultipleToolCalls() {
		// Create previous chunk with multiple tool calls
		ChatCompletionChunk previous = createChunkWithMultipleToolCalls("request-1");
		// Create current chunk with a single tool call
		ChatCompletionChunk current = createChunkWithToolCall("request-1", "tool-2", "function-2",
				"{\"param2\":\"value2\"}");

		ChatCompletionChunk result = helperWithIncrementalOutput.merge(previous, current);

		assertNotNull(result);
		assertEquals("request-1", result.requestId());
		List<ToolCall> toolCalls = result.output().choices().get(0).message().toolCalls();
		assertNotNull(toolCalls);
		// According to DashScopeAiStreamFunctionCallingHelper implementation, it keeps
		// all tool calls from previous
		// and adds tool calls from current, so there should be 3 tool calls here
		assertEquals(3, toolCalls.size());
		assertEquals("tool-1", toolCalls.get(0).id());
		assertEquals("function-1", toolCalls.get(0).function().name());
		assertEquals("{\"param1\":\"value1\"}", toolCalls.get(0).function().arguments());
		assertEquals("tool-2", toolCalls.get(1).id());
		assertEquals("function-2", toolCalls.get(1).function().name());
		assertEquals("{\"param2\":\"value2\"}", toolCalls.get(1).function().arguments());
		assertEquals("tool-2", toolCalls.get(2).id());
		assertEquals("function-2", toolCalls.get(2).function().name());
		assertEquals("{\"param2\":\"value2\"}", toolCalls.get(2).function().arguments());
	}

	// Helper method: Create a simple ChatCompletionChunk
	private ChatCompletionChunk createSimpleChunk(String requestId, String content, Role role,
			ChatCompletionFinishReason finishReason) {
		ChatCompletionMessage message = new ChatCompletionMessage(content, role);
		Choice choice = new Choice(finishReason, message);
		ChatCompletionOutput output = new ChatCompletionOutput(null, List.of(choice));
		TokenUsage usage = new TokenUsage(10, 5, 15);
		return new ChatCompletionChunk(requestId, output, usage);
	}

	// Helper method: Create a ChatCompletionChunk with tool call
	private ChatCompletionChunk createChunkWithToolCall(String requestId, String toolId, String functionName,
			String arguments) {
		return createChunkWithToolCall(requestId, toolId, functionName, arguments, null);
	}

	// Helper method: Create a ChatCompletionChunk with tool call and finish reason
	private ChatCompletionChunk createChunkWithToolCall(String requestId, String toolId, String functionName,
			String arguments, ChatCompletionFinishReason finishReason) {
		ChatCompletionFunction function = new ChatCompletionFunction(functionName, arguments);
		ToolCall toolCall = new ToolCall(toolId, "function", function);
		ChatCompletionMessage message = new ChatCompletionMessage("", Role.ASSISTANT, null, null, List.of(toolCall),
				null);
		Choice choice = new Choice(finishReason, message);
		ChatCompletionOutput output = new ChatCompletionOutput(null, List.of(choice));
		TokenUsage usage = new TokenUsage(10, 5, 15);
		return new ChatCompletionChunk(requestId, output, usage);
	}

	// Helper method: Create a ChatCompletionChunk with multiple tool calls
	private ChatCompletionChunk createChunkWithMultipleToolCalls(String requestId) {
		ChatCompletionFunction function1 = new ChatCompletionFunction("function-1", "{\"param1\":\"value1\"}");
		ToolCall toolCall1 = new ToolCall("tool-1", "function", function1);
		ChatCompletionFunction function2 = new ChatCompletionFunction("function-2", "{\"param2\":\"value2\"}");
		ToolCall toolCall2 = new ToolCall("tool-2", "function", function2);
		ChatCompletionMessage message = new ChatCompletionMessage("", Role.ASSISTANT, null, null,
				List.of(toolCall1, toolCall2), null);
		Choice choice = new Choice(null, message);
		ChatCompletionOutput output = new ChatCompletionOutput(null, List.of(choice));
		TokenUsage usage = new TokenUsage(10, 5, 15);
		return new ChatCompletionChunk(requestId, output, usage);
	}

}
