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
package com.alibaba.cloud.ai.graph.agent.interceptors;

import com.alibaba.cloud.ai.graph.agent.hook.ToolCallGuardConstants;
import com.alibaba.cloud.ai.graph.agent.hook.toolexecutionfailure.ToolExecutionFailureGuardConstants;
import com.alibaba.cloud.ai.graph.agent.hook.unknowntool.UnknownToolGuardConstants;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallResponse;
import com.alibaba.cloud.ai.graph.agent.interceptor.toolretry.ToolRetryInterceptor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Local unit tests for retry behavior on structured tool execution failures.
 */
class ToolRetryInterceptorStructuredFailureTest {

	@Test
	@DisplayName("should retry structured execution failure responses and eventually succeed")
	void shouldRetryStructuredExecutionFailureResponsesAndEventuallySucceed() {
		ToolRetryInterceptor interceptor = ToolRetryInterceptor.builder()
				.maxRetries(2)
				.initialDelay(1)
				.jitter(false)
				.build();
		AtomicInteger attempts = new AtomicInteger();
		ToolCallRequest request = createRequest("failing_tool");
		ToolCallHandler handler = req -> {
			if (attempts.getAndIncrement() == 0) {
				return ToolCallResponse.builder()
						.toolCallId(req.getToolCallId())
						.toolName(req.getToolName())
						.content("Error: boom")
						.status("error")
						.metadata(Map.of(
								"error", true,
								"errorMessage", "boom",
								ToolCallGuardConstants.ERROR_TYPE_METADATA_KEY,
								ToolExecutionFailureGuardConstants.TOOL_EXECUTION_FAILURE_ERROR_TYPE,
								ToolExecutionFailureGuardConstants.FAILURE_TYPE_METADATA_KEY,
								ToolExecutionFailureGuardConstants.RUNTIME_EXCEPTION_FAILURE_TYPE))
						.build();
			}
			return ToolCallResponse.of(req.getToolCallId(), req.getToolName(), "ok");
		};

		ToolCallResponse response = interceptor.interceptToolCall(request, handler);
		assertFalse(response.isError());
		assertEquals("ok", response.getResult());
		assertEquals(2, attempts.get());
	}

	@Test
	@DisplayName("should not retry unknown tool responses")
	void shouldNotRetryUnknownToolResponses() {
		ToolRetryInterceptor interceptor = ToolRetryInterceptor.builder()
				.maxRetries(2)
				.initialDelay(1)
				.jitter(false)
				.build();
		AtomicInteger attempts = new AtomicInteger();
		ToolCallRequest request = createRequest("missing_tool");
		ToolCallHandler handler = req -> {
			attempts.incrementAndGet();
			return ToolCallResponse.builder()
					.toolCallId(req.getToolCallId())
					.toolName(req.getToolName())
					.content("Error: unknown")
					.status("error")
					.metadata(Map.of(
							"error", true,
							"errorMessage", "unknown",
							ToolCallGuardConstants.ERROR_TYPE_METADATA_KEY,
							UnknownToolGuardConstants.UNKNOWN_TOOL_ERROR_TYPE))
					.build();
		};

		ToolCallResponse response = interceptor.interceptToolCall(request, handler);
		assertTrue(response.isError());
		assertEquals(1, attempts.get());
	}

	@Test
	@DisplayName("should preserve structured error metadata when retries are exhausted")
	void shouldPreserveStructuredErrorMetadataWhenRetriesAreExhausted() {
		ToolRetryInterceptor interceptor = ToolRetryInterceptor.builder()
				.maxRetries(1)
				.initialDelay(1)
				.jitter(false)
				.build();
		AtomicInteger attempts = new AtomicInteger();
		ToolCallRequest request = createRequest("failing_tool");
		ToolCallHandler handler = req -> {
			attempts.incrementAndGet();
			return ToolCallResponse.builder()
					.toolCallId(req.getToolCallId())
					.toolName(req.getToolName())
					.content("Error: boom")
					.status("error")
					.metadata(Map.of(
							"error", true,
							"errorMessage", "boom",
							ToolCallGuardConstants.ERROR_TYPE_METADATA_KEY,
							ToolExecutionFailureGuardConstants.TOOL_EXECUTION_FAILURE_ERROR_TYPE,
							ToolExecutionFailureGuardConstants.FAILURE_TYPE_METADATA_KEY,
							ToolExecutionFailureGuardConstants.RUNTIME_EXCEPTION_FAILURE_TYPE))
					.build();
		};

		ToolCallResponse response = interceptor.interceptToolCall(request, handler);
		assertTrue(response.isError());
		assertEquals(2, attempts.get());
		assertEquals(2, response.getMetadata().get(ToolExecutionFailureGuardConstants.RETRY_ATTEMPTS_METADATA_KEY));
		assertEquals(true,
				response.getMetadata().get(ToolExecutionFailureGuardConstants.RETRY_EXHAUSTED_METADATA_KEY));
	}

	private ToolCallRequest createRequest(String toolName) {
		return ToolCallRequest.builder()
				.toolCall(new AssistantMessage.ToolCall("call-1", "function", toolName, "{}"))
				.context(Map.of())
				.build();
	}

}

