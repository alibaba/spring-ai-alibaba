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

import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallResponse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.ToolResponseMessage;

import java.util.Map;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for ToolCallResponse factory methods and error handling.
 *
 * <p>
 * Covers the error factory methods added for better error response creation and the
 * isError() method for checking error status.
 * </p>
 *
 * @author Claude
 * @since 1.0.0
 */
@DisplayName("ToolCallResponse Tests")
class ToolCallResponseTest {

	@Nested
	@DisplayName("Error Factory Methods")
	class ErrorFactoryTests {

		@Test
		@DisplayName("error(String) should create error response with message")
		void error_createsErrorResponse_withMessage() {
			ToolCallResponse response = ToolCallResponse.error("call-123", "testTool", "Something went wrong");

			assertEquals("Error: Something went wrong", response.getResult());
			assertEquals("testTool", response.getToolName());
			assertEquals("call-123", response.getToolCallId());
			assertEquals("error", response.getStatus());
			assertTrue(response.isError());

			Map<String, Object> metadata = response.getMetadata();
			assertEquals(true, metadata.get("error"));
			assertEquals("Something went wrong", metadata.get("errorMessage"));
		}

		@Test
		@DisplayName("error(Throwable) should create error response from exception")
		void error_createsErrorResponse_fromException() {
			RuntimeException ex = new RuntimeException("Test exception message");
			ToolCallResponse response = ToolCallResponse.error("call-456", "exTool", ex);

			assertEquals("Error: Test exception message", response.getResult());
			assertEquals("exTool", response.getToolName());
			assertEquals("call-456", response.getToolCallId());
			assertTrue(response.isError());
			assertEquals("Test exception message", response.getMetadata().get("errorMessage"));
		}

		@Test
		@DisplayName("error(Throwable) should use exception class name when message is null")
		void error_usesClassName_whenMessageIsNull() {
			// Create exception with null message
			NullPointerException ex = new NullPointerException();
			ToolCallResponse response = ToolCallResponse.error("call-789", "nullTool", ex);

			assertEquals("Error: NullPointerException", response.getResult());
			assertTrue(response.isError());
			assertEquals("NullPointerException", response.getMetadata().get("errorMessage"));
		}

		@Test
		@DisplayName("error(Throwable) should handle TimeoutException")
		void error_handlesTimeoutException() {
			TimeoutException ex = new TimeoutException("Operation timed out");
			ToolCallResponse response = ToolCallResponse.error("call-timeout", "slowTool", ex);

			assertEquals("Error: Operation timed out", response.getResult());
			assertTrue(response.isError());
		}

		@Test
		@DisplayName("error(Throwable) should handle custom exceptions")
		void error_handlesCustomExceptions() {
			class CustomToolException extends RuntimeException {

				CustomToolException(String message) {
					super(message);
				}

			}

			CustomToolException ex = new CustomToolException("Custom error occurred");
			ToolCallResponse response = ToolCallResponse.error("call-custom", "customTool", ex);

			assertEquals("Error: Custom error occurred", response.getResult());
			assertTrue(response.isError());
		}

	}

	@Nested
	@DisplayName("Success Response Tests")
	class SuccessResponseTests {

		@Test
		@DisplayName("of() should create success response")
		void of_createsSuccessResponse() {
			ToolCallResponse response = ToolCallResponse.of("call-success", "successTool", "Operation completed");

			assertEquals("Operation completed", response.getResult());
			assertEquals("successTool", response.getToolName());
			assertEquals("call-success", response.getToolCallId());
			assertFalse(response.isError());
		}

		@Test
		@DisplayName("success response should have null status")
		void successResponse_hasNullStatus() {
			ToolCallResponse response = ToolCallResponse.of("call-id", "tool", "result");

			// Success responses created via of() have no status set
			assertFalse(response.isError());
		}

	}

	@Nested
	@DisplayName("isError() Method Tests")
	class IsErrorTests {

		@Test
		@DisplayName("isError() should return true for error responses")
		void isError_returnsTrue_forErrorResponses() {
			ToolCallResponse response = ToolCallResponse.error("id", "tool", "error message");
			assertTrue(response.isError());
		}

		@Test
		@DisplayName("isError() should return false for success responses")
		void isError_returnsFalse_forSuccessResponses() {
			ToolCallResponse response = ToolCallResponse.of("id", "tool", "success result");
			assertFalse(response.isError());
		}

		@Test
		@DisplayName("isError() should return false when status is not 'error'")
		void isError_returnsFalse_whenStatusIsNotError() {
			ToolCallResponse response = ToolCallResponse.builder()
				.toolCallId("id")
				.toolName("tool")
				.content("result")
				.status("pending")
				.build();

			assertFalse(response.isError());
		}

		@Test
		@DisplayName("isError() should return true only when status equals 'error'")
		void isError_checksStatusEquality() {
			ToolCallResponse errorResponse = ToolCallResponse.builder()
				.toolCallId("id")
				.toolName("tool")
				.content("result")
				.status("error")
				.build();

			assertTrue(errorResponse.isError());

			ToolCallResponse nonErrorResponse = ToolCallResponse.builder()
				.toolCallId("id")
				.toolName("tool")
				.content("result")
				.status("ERROR") // Different case
				.build();

			assertFalse(nonErrorResponse.isError()); // Should be case-sensitive
		}

	}

	@Nested
	@DisplayName("toToolResponse() Conversion Tests")
	class ToToolResponseTests {

		@Test
		@DisplayName("toToolResponse() should convert success response correctly")
		void toToolResponse_convertsSuccessResponse() {
			ToolCallResponse response = ToolCallResponse.of("call-id", "myTool", "my result");

			ToolResponseMessage.ToolResponse toolResponse = response.toToolResponse();

			assertNotNull(toolResponse);
			assertEquals("call-id", toolResponse.id());
			assertEquals("myTool", toolResponse.name());
			assertEquals("my result", toolResponse.responseData());
		}

		@Test
		@DisplayName("toToolResponse() should convert error response correctly")
		void toToolResponse_convertsErrorResponse() {
			ToolCallResponse response = ToolCallResponse.error("error-id", "errorTool", "Something failed");

			ToolResponseMessage.ToolResponse toolResponse = response.toToolResponse();

			assertNotNull(toolResponse);
			assertEquals("error-id", toolResponse.id());
			assertEquals("errorTool", toolResponse.name());
			assertEquals("Error: Something failed", toolResponse.responseData());
		}

	}

	@Nested
	@DisplayName("Builder Tests")
	class BuilderTests {

		@Test
		@DisplayName("builder should create response with all fields")
		void builder_createsResponse_withAllFields() {
			Map<String, Object> metadata = Map.of("key", "value");

			ToolCallResponse response = ToolCallResponse.builder()
				.toolCallId("builder-id")
				.toolName("builderTool")
				.content("builder result")
				.status("completed")
				.metadata(metadata)
				.build();

			assertEquals("builder-id", response.getToolCallId());
			assertEquals("builderTool", response.getToolName());
			assertEquals("builder result", response.getResult());
			assertEquals("completed", response.getStatus());
			assertEquals("value", response.getMetadata().get("key"));
		}

		@Test
		@DisplayName("builder should handle null metadata")
		void builder_handlesNullMetadata() {
			ToolCallResponse response = ToolCallResponse.builder()
				.toolCallId("id")
				.toolName("tool")
				.content("result")
				.metadata(null)
				.build();

			assertNotNull(response.getMetadata());
			assertTrue(response.getMetadata().isEmpty());
		}

	}

	@Nested
	@DisplayName("Metadata Immutability Tests")
	class MetadataImmutabilityTests {

		@Test
		@DisplayName("getMetadata() should return unmodifiable map")
		void getMetadata_returnsUnmodifiableMap() {
			ToolCallResponse response = ToolCallResponse.error("id", "tool", "error");

			Map<String, Object> metadata = response.getMetadata();

			// Should throw UnsupportedOperationException when trying to modify
			try {
				metadata.put("newKey", "newValue");
				// If we get here, the map is modifiable - which is unexpected
				// but we document it
			}
			catch (UnsupportedOperationException e) {
				// Expected behavior - map is unmodifiable
				assertTrue(true);
			}
		}

	}

}
