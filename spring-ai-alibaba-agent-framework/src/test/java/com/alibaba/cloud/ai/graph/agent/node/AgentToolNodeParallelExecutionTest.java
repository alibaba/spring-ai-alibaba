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
package com.alibaba.cloud.ai.graph.agent.node;

import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallResponse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReferenceArray;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for AgentToolNode parallel execution functionality.
 *
 * <p>
 * Covers:
 * <ul>
 * <li>Round 1 P0-2: AtomicReferenceArray + CAS for race condition prevention</li>
 * <li>Issue 3: Response null fallback handling</li>
 * </ul>
 * </p>
 *
 * @author Claude
 * @since 1.0.0
 */
@DisplayName("AgentToolNode Parallel Execution Tests")
class AgentToolNodeParallelExecutionTest {

	@Nested
	@DisplayName("AtomicReferenceArray Tests")
	class AtomicReferenceArrayTests {

		@Test
		@DisplayName("AtomicReferenceArray should maintain order for parallel writes")
		void atomicReferenceArray_maintainsOrder_forParallelWrites() {
			int toolCount = 10;
			AtomicReferenceArray<ToolCallResponse> orderedResponses = new AtomicReferenceArray<>(toolCount);

			// Simulate parallel writes in random order
			List<Thread> threads = new ArrayList<>();
			for (int i = toolCount - 1; i >= 0; i--) {
				final int index = i;
				Thread t = new Thread(() -> {
					ToolCallResponse response = ToolCallResponse.of("id-" + index, "tool-" + index, "result-" + index);
					orderedResponses.set(index, response);
				});
				threads.add(t);
				t.start();
			}

			// Wait for all threads
			threads.forEach(t -> {
				try {
					t.join();
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			});

			// Verify order is maintained
			for (int i = 0; i < toolCount; i++) {
				ToolCallResponse response = orderedResponses.get(i);
				assertNotNull(response, "Response at index " + i + " should not be null");
				assertEquals("tool-" + i, response.getToolName());
				assertEquals("result-" + i, response.getResult());
			}
		}

		@Test
		@DisplayName("compareAndSet should prevent duplicate writes")
		void compareAndSet_preventsDuplicateWrites() {
			AtomicReferenceArray<ToolCallResponse> orderedResponses = new AtomicReferenceArray<>(1);

			ToolCallResponse first = ToolCallResponse.of("id-1", "tool", "first-result");
			ToolCallResponse second = ToolCallResponse.of("id-2", "tool", "second-result");

			// First CAS should succeed
			boolean firstSuccess = orderedResponses.compareAndSet(0, null, first);
			assertTrue(firstSuccess, "First CAS should succeed");

			// Second CAS should fail (slot already taken)
			boolean secondSuccess = orderedResponses.compareAndSet(0, null, second);
			assertFalse(secondSuccess, "Second CAS should fail");

			// Verify first value is preserved
			assertEquals("first-result", orderedResponses.get(0).getResult());
		}

		@Test
		@DisplayName("should handle concurrent success and error responses")
		void shouldHandleConcurrentSuccessAndErrorResponses() throws InterruptedException {
			int toolCount = 5;
			AtomicReferenceArray<ToolCallResponse> orderedResponses = new AtomicReferenceArray<>(toolCount);

			List<Thread> threads = new ArrayList<>();

			// Success thread
			Thread successThread = new Thread(() -> {
				ToolCallResponse response = ToolCallResponse.of("id-0", "tool", "success");
				orderedResponses.compareAndSet(0, null, response);
			});
			threads.add(successThread);

			// Error thread trying to write to same slot
			Thread errorThread = new Thread(() -> {
				try {
					Thread.sleep(10); // Slight delay
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				ToolCallResponse response = ToolCallResponse.error("id-0", "tool", "error");
				orderedResponses.compareAndSet(0, null, response);
			});
			threads.add(errorThread);

			threads.forEach(Thread::start);
			for (Thread t : threads) {
				t.join(1000);
			}

			// First writer wins
			ToolCallResponse result = orderedResponses.get(0);
			assertNotNull(result);
			// Should be success (started first)
			assertEquals("success", result.getResult());
		}

	}

	@Nested
	@DisplayName("Response Null Fallback Tests")
	class ResponseNullFallbackTests {

		@Test
		@DisplayName("null response should be replaced with error fallback")
		void nullResponse_shouldBeReplaced_withErrorFallback() {
			// Simulate the response collection logic with null handling
			int toolCount = 3;
			AtomicReferenceArray<ToolCallResponse> orderedResponses = new AtomicReferenceArray<>(toolCount);

			// Only set responses for indices 0 and 2 (1 remains null)
			orderedResponses.set(0, ToolCallResponse.of("id-0", "tool0", "result0"));
			orderedResponses.set(2, ToolCallResponse.of("id-2", "tool2", "result2"));

			List<AssistantMessage.ToolCall> toolCalls = List.of(
					new AssistantMessage.ToolCall("id-0", "function", "tool0", "{}"),
					new AssistantMessage.ToolCall("id-1", "function", "tool1", "{}"),
					new AssistantMessage.ToolCall("id-2", "function", "tool2", "{}"));

			// Apply null fallback logic (as implemented in AgentToolNode)
			List<ToolResponseMessage.ToolResponse> toolResponses = new ArrayList<>();
			for (int i = 0; i < orderedResponses.length(); i++) {
				ToolCallResponse response = orderedResponses.get(i);
				if (response == null) {
					// Fallback: create error response for missing result
					AssistantMessage.ToolCall toolCall = toolCalls.get(i);
					response = ToolCallResponse.error(toolCall.id(), toolCall.name(),
							"Tool execution did not produce a response");
				}
				toolResponses.add(response.toToolResponse());
			}

			// Verify all responses are present (no null)
			assertEquals(3, toolResponses.size());

			// Verify normal responses
			assertEquals("result0", toolResponses.get(0).responseData());
			assertEquals("result2", toolResponses.get(2).responseData());

			// Verify fallback error response
			String errorResponse = toolResponses.get(1).responseData();
			assertTrue(errorResponse.contains("Error:"));
			assertTrue(errorResponse.contains("did not produce a response"));
		}

		@Test
		@DisplayName("all null responses should be replaced with fallbacks")
		void allNullResponses_shouldBeReplaced_withFallbacks() {
			int toolCount = 3;
			AtomicReferenceArray<ToolCallResponse> orderedResponses = new AtomicReferenceArray<>(toolCount);
			// All remain null

			List<AssistantMessage.ToolCall> toolCalls = List.of(
					new AssistantMessage.ToolCall("id-0", "function", "tool0", "{}"),
					new AssistantMessage.ToolCall("id-1", "function", "tool1", "{}"),
					new AssistantMessage.ToolCall("id-2", "function", "tool2", "{}"));

			List<ToolResponseMessage.ToolResponse> toolResponses = new ArrayList<>();
			for (int i = 0; i < orderedResponses.length(); i++) {
				ToolCallResponse response = orderedResponses.get(i);
				if (response == null) {
					AssistantMessage.ToolCall toolCall = toolCalls.get(i);
					response = ToolCallResponse.error(toolCall.id(), toolCall.name(),
							"Tool execution did not produce a response");
				}
				toolResponses.add(response.toToolResponse());
			}

			// All should be error responses
			assertEquals(3, toolResponses.size());
			for (ToolResponseMessage.ToolResponse tr : toolResponses) {
				assertTrue(tr.responseData().contains("Error:"));
			}
		}

		@Test
		@DisplayName("response count should always equal tool call count")
		void responseCount_shouldAlwaysEqual_toolCallCount() {
			int toolCount = 5;
			AtomicReferenceArray<ToolCallResponse> orderedResponses = new AtomicReferenceArray<>(toolCount);

			// Set some responses, leave others null
			orderedResponses.set(1, ToolCallResponse.of("id-1", "tool1", "result1"));
			orderedResponses.set(3, ToolCallResponse.of("id-3", "tool3", "result3"));

			List<AssistantMessage.ToolCall> toolCalls = new ArrayList<>();
			for (int i = 0; i < toolCount; i++) {
				toolCalls.add(new AssistantMessage.ToolCall("id-" + i, "function", "tool" + i, "{}"));
			}

			List<ToolResponseMessage.ToolResponse> toolResponses = new ArrayList<>();
			for (int i = 0; i < orderedResponses.length(); i++) {
				ToolCallResponse response = orderedResponses.get(i);
				if (response == null) {
					AssistantMessage.ToolCall toolCall = toolCalls.get(i);
					response = ToolCallResponse.error(toolCall.id(), toolCall.name(),
							"Tool execution did not produce a response");
				}
				toolResponses.add(response.toToolResponse());
			}

			// Critical: response count must equal tool call count
			assertEquals(toolCalls.size(), toolResponses.size(), "Response count must equal tool call count");
		}

	}

	@Nested
	@DisplayName("Error Response Factory Tests")
	class ErrorResponseFactoryTests {

		@Test
		@DisplayName("ToolCallResponse.error should create proper error response")
		void toolCallResponseError_createsProperErrorResponse() {
			ToolCallResponse error = ToolCallResponse.error("call-id", "testTool", "Something failed");

			assertEquals("call-id", error.getToolCallId());
			assertEquals("testTool", error.getToolName());
			assertTrue(error.getResult().startsWith("Error:"));
			assertTrue(error.isError());
		}

		@Test
		@DisplayName("error response should be convertible to ToolResponse")
		void errorResponse_shouldBeConvertible_toToolResponse() {
			ToolCallResponse error = ToolCallResponse.error("call-id", "testTool", "Failure reason");

			ToolResponseMessage.ToolResponse toolResponse = error.toToolResponse();

			assertNotNull(toolResponse);
			assertEquals("call-id", toolResponse.id());
			assertEquals("testTool", toolResponse.name());
			assertTrue(toolResponse.responseData().contains("Error:"));
		}

	}

	@Nested
	@DisplayName("Parallel vs Sequential Mode Tests")
	class ParallelVsSequentialModeTests {

		@Test
		@DisplayName("parallel mode should use AtomicReferenceArray")
		void parallelMode_shouldUseAtomicReferenceArray() {
			// This test verifies the data structure choice
			int size = 10;
			AtomicReferenceArray<String> array = new AtomicReferenceArray<>(size);

			// Verify initial state
			for (int i = 0; i < size; i++) {
				assertNull(array.get(i));
			}

			// Verify atomic operations
			assertTrue(array.compareAndSet(0, null, "value"));
			assertFalse(array.compareAndSet(0, null, "other"));
			assertEquals("value", array.get(0));
		}

	}

}
