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
package com.alibaba.cloud.ai.graph.agent.tool;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.ToolStreamingOutput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for ToolStreamingErrorHandler.
 *
 * <p>
 * Covers error handling, message extraction, and exception type detection methods.
 * </p>
 *
 * @author Claude
 * @since 1.0.0
 */
@DisplayName("ToolStreamingErrorHandler Tests")
class ToolStreamingErrorHandlerTest {

	private OverAllState testState;

	@BeforeEach
	void setUp() {
		Map<String, Object> stateData = new HashMap<>();
		stateData.put("test_key", "test_value");
		testState = new OverAllState(stateData);
	}

	@Nested
	@DisplayName("handleError with ToolCall Tests")
	class HandleErrorWithToolCallTests {

		@Test
		@DisplayName("should create error output from ToolCall")
		void handleError_shouldCreateErrorOutput_fromToolCall() {
			AssistantMessage.ToolCall toolCall = new AssistantMessage.ToolCall("call_123", "function", "searchTool",
					"{}");
			RuntimeException error = new RuntimeException("Something went wrong");

			ToolStreamingOutput<ToolResult> output = ToolStreamingErrorHandler.handleError(toolCall, error, testState,
					"agent_tool", "testAgent");

			assertNotNull(output);
			assertEquals("call_123", output.getToolCallId());
			assertEquals("searchTool", output.getToolName());
			assertEquals(OutputType.AGENT_TOOL_FINISHED, output.getOutputType());
			assertTrue(output.isFinalChunk());

			ToolResult result = output.getChunkData();
			assertNotNull(result);
			assertTrue(result.getTextContent().contains("Error:"));
			assertTrue(result.getTextContent().contains("Something went wrong"));
			assertTrue(result.isFinal());
		}

		@Test
		@DisplayName("should handle TimeoutException with ToolCall")
		void handleError_shouldHandleTimeout_fromToolCall() {
			AssistantMessage.ToolCall toolCall = new AssistantMessage.ToolCall("call_456", "function", "slowTool",
					"{}");
			TimeoutException error = new TimeoutException("Operation timed out");

			ToolStreamingOutput<ToolResult> output = ToolStreamingErrorHandler.handleError(toolCall, error, testState,
					"agent_tool", "testAgent");

			ToolResult result = output.getChunkData();
			assertTrue(result.getTextContent().contains("timed out"));
		}

	}

	@Nested
	@DisplayName("handleError with IDs Tests")
	class HandleErrorWithIdsTests {

		@Test
		@DisplayName("should create error output from IDs")
		void handleError_shouldCreateErrorOutput_fromIds() {
			RuntimeException error = new RuntimeException("Test error message");

			ToolStreamingOutput<ToolResult> output = ToolStreamingErrorHandler.handleError("call_789", "myTool", error,
					testState, "agent_tool", "testAgent");

			assertNotNull(output);
			assertEquals("call_789", output.getToolCallId());
			assertEquals("myTool", output.getToolName());
			assertEquals(OutputType.AGENT_TOOL_FINISHED, output.getOutputType());

			ToolResult result = output.getChunkData();
			assertTrue(result.getTextContent().contains("Test error message"));
		}

		@Test
		@DisplayName("should preserve node and agent info")
		void handleError_shouldPreserveNodeAndAgentInfo() {
			RuntimeException error = new RuntimeException("Error");

			ToolStreamingOutput<ToolResult> output = ToolStreamingErrorHandler.handleError("id", "tool", error,
					testState, "custom_node", "customAgent");

			assertEquals("custom_node", output.node());
			assertEquals("customAgent", output.agent());
		}

	}

	@Nested
	@DisplayName("extractErrorMessage Tests")
	class ExtractErrorMessageTests {

		@Test
		@DisplayName("should return timeout message for TimeoutException")
		void extractErrorMessage_shouldReturnTimeoutMessage_forTimeoutException() {
			TimeoutException error = new TimeoutException("timeout");

			String message = ToolStreamingErrorHandler.extractErrorMessage(error);

			assertEquals("Tool execution timed out", message);
		}

		@Test
		@DisplayName("should return cancellation message for ToolCancelledException")
		void extractErrorMessage_shouldReturnCancellationMessage_forToolCancelledException() {
			ToolCancelledException error = new ToolCancelledException("cancelled");

			String message = ToolStreamingErrorHandler.extractErrorMessage(error);

			assertEquals("Tool execution was cancelled", message);
		}

		@Test
		@DisplayName("should return interrupted message for InterruptedException")
		void extractErrorMessage_shouldReturnInterruptedMessage_forInterruptedException() {
			InterruptedException error = new InterruptedException("interrupted");

			String message = ToolStreamingErrorHandler.extractErrorMessage(error);

			assertEquals("Tool execution was interrupted", message);
			// Note: Thread interrupt flag will be set
		}

		@Test
		@DisplayName("should unwrap CompletionException")
		void extractErrorMessage_shouldUnwrap_completionException() {
			TimeoutException cause = new TimeoutException("inner timeout");
			CompletionException wrapped = new CompletionException(cause);

			String message = ToolStreamingErrorHandler.extractErrorMessage(wrapped);

			assertEquals("Tool execution timed out", message);
		}

		@Test
		@DisplayName("should return exception message when present")
		void extractErrorMessage_shouldReturnExceptionMessage_whenPresent() {
			RuntimeException error = new RuntimeException("Custom error message");

			String message = ToolStreamingErrorHandler.extractErrorMessage(error);

			assertEquals("Custom error message", message);
		}

		@Test
		@DisplayName("should return class name when message is null")
		void extractErrorMessage_shouldReturnClassName_whenMessageNull() {
			RuntimeException error = new RuntimeException((String) null);

			String message = ToolStreamingErrorHandler.extractErrorMessage(error);

			assertEquals("RuntimeException", message);
		}

		@Test
		@DisplayName("should unwrap nested CompletionException")
		void extractErrorMessage_shouldUnwrapNested_completionException() {
			RuntimeException root = new RuntimeException("Root cause");
			CompletionException wrapped = new CompletionException(root);

			String message = ToolStreamingErrorHandler.extractErrorMessage(wrapped);

			assertEquals("Root cause", message);
		}

	}

	@Nested
	@DisplayName("isTimeout Tests")
	class IsTimeoutTests {

		@Test
		@DisplayName("should return true for TimeoutException")
		void isTimeout_shouldReturnTrue_forTimeoutException() {
			TimeoutException error = new TimeoutException();

			assertTrue(ToolStreamingErrorHandler.isTimeout(error));
		}

		@Test
		@DisplayName("should return true for wrapped TimeoutException")
		void isTimeout_shouldReturnTrue_forWrappedTimeoutException() {
			TimeoutException cause = new TimeoutException();
			CompletionException wrapped = new CompletionException(cause);

			assertTrue(ToolStreamingErrorHandler.isTimeout(wrapped));
		}

		@Test
		@DisplayName("should return false for other exceptions")
		void isTimeout_shouldReturnFalse_forOtherExceptions() {
			RuntimeException error = new RuntimeException("Not a timeout");

			assertFalse(ToolStreamingErrorHandler.isTimeout(error));
		}

		@Test
		@DisplayName("should return false for wrapped non-timeout exception")
		void isTimeout_shouldReturnFalse_forWrappedNonTimeout() {
			RuntimeException cause = new RuntimeException("Not timeout");
			CompletionException wrapped = new CompletionException(cause);

			assertFalse(ToolStreamingErrorHandler.isTimeout(wrapped));
		}

	}

	@Nested
	@DisplayName("isCancellation Tests")
	class IsCancellationTests {

		@Test
		@DisplayName("should return true for ToolCancelledException")
		void isCancellation_shouldReturnTrue_forToolCancelledException() {
			ToolCancelledException error = new ToolCancelledException("cancelled");

			assertTrue(ToolStreamingErrorHandler.isCancellation(error));
		}

		@Test
		@DisplayName("should return true for CancellationException")
		void isCancellation_shouldReturnTrue_forCancellationException() {
			CancellationException error = new CancellationException("cancelled");

			assertTrue(ToolStreamingErrorHandler.isCancellation(error));
		}

		@Test
		@DisplayName("should return true for wrapped cancellation")
		void isCancellation_shouldReturnTrue_forWrappedCancellation() {
			CancellationException cause = new CancellationException();
			CompletionException wrapped = new CompletionException(cause);

			assertTrue(ToolStreamingErrorHandler.isCancellation(wrapped));
		}

		@Test
		@DisplayName("should return true for wrapped ToolCancelledException")
		void isCancellation_shouldReturnTrue_forWrappedToolCancelledException() {
			ToolCancelledException cause = new ToolCancelledException("cancelled");
			CompletionException wrapped = new CompletionException(cause);

			assertTrue(ToolStreamingErrorHandler.isCancellation(wrapped));
		}

		@Test
		@DisplayName("should return false for other exceptions")
		void isCancellation_shouldReturnFalse_forOtherExceptions() {
			RuntimeException error = new RuntimeException("Not cancelled");

			assertFalse(ToolStreamingErrorHandler.isCancellation(error));
		}

		@Test
		@DisplayName("should return false for TimeoutException")
		void isCancellation_shouldReturnFalse_forTimeoutException() {
			TimeoutException error = new TimeoutException();

			assertFalse(ToolStreamingErrorHandler.isCancellation(error));
		}

	}

	@Nested
	@DisplayName("Edge Case Tests")
	class EdgeCaseTests {

		@Test
		@DisplayName("should handle CompletionException with null cause")
		void extractErrorMessage_shouldHandleCompletionException_withNullCause() {
			CompletionException error = new CompletionException("message", null);

			String message = ToolStreamingErrorHandler.extractErrorMessage(error);

			// Should return message or class name
			assertNotNull(message);
		}

		@Test
		@DisplayName("should handle exception with empty message")
		void extractErrorMessage_shouldHandleExceptionWithEmptyMessage() {
			RuntimeException error = new RuntimeException("");

			String message = ToolStreamingErrorHandler.extractErrorMessage(error);

			// Empty string is valid, should return it
			assertEquals("", message);
		}

	}

	@Nested
	@DisplayName("Reactor Blocking Timeout Tests")
	class ReactorBlockingTimeoutTests {

		@Test
		@DisplayName("extractErrorMessage should return timeout message for Reactor blocking timeout")
		void extractErrorMessage_shouldReturnTimeoutMessage_forReactorBlockingTimeout() {
			// Simulates Reactor's block(Duration) timeout
			IllegalStateException error = new IllegalStateException(
					"Timeout on blocking read for 5000 MILLISECONDS");

			String message = ToolStreamingErrorHandler.extractErrorMessage(error);

			assertEquals("Tool execution timed out", message);
		}

		@Test
		@DisplayName("extractErrorMessage should not match non-timeout IllegalStateException")
		void extractErrorMessage_shouldNotMatch_nonTimeoutIllegalStateException() {
			IllegalStateException error = new IllegalStateException("Some other error");

			String message = ToolStreamingErrorHandler.extractErrorMessage(error);

			assertEquals("Some other error", message);
		}

		@Test
		@DisplayName("isTimeout should return true for Reactor blocking timeout")
		void isTimeout_shouldReturnTrue_forReactorBlockingTimeout() {
			IllegalStateException error = new IllegalStateException("Timeout on blocking read");

			assertTrue(ToolStreamingErrorHandler.isTimeout(error));
		}

		@Test
		@DisplayName("isTimeout should return true for wrapped Reactor blocking timeout")
		void isTimeout_shouldReturnTrue_forWrappedReactorBlockingTimeout() {
			IllegalStateException cause = new IllegalStateException("Timeout on blocking read for 1000");
			CompletionException wrapped = new CompletionException(cause);

			assertTrue(ToolStreamingErrorHandler.isTimeout(wrapped));
		}

		@Test
		@DisplayName("handleError should handle Reactor blocking timeout with ToolCall")
		void handleError_shouldHandleReactorBlockingTimeout_fromToolCall() {
			AssistantMessage.ToolCall toolCall = new AssistantMessage.ToolCall("call_123", "function", "streamTool",
					"{}");
			IllegalStateException error = new IllegalStateException("Timeout on blocking read");

			ToolStreamingOutput<ToolResult> output = ToolStreamingErrorHandler.handleError(toolCall, error, testState,
					"agent_tool", "testAgent");

			ToolResult result = output.getChunkData();
			assertTrue(result.getTextContent().contains("timed out"));
		}

	}

}
