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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for ToolCancelledException.
 *
 * @author Claude
 * @since 1.0.0
 */
@DisplayName("ToolCancelledException Tests")
class ToolCancelledExceptionTest {

	@Test
	@DisplayName("constructor with message should set message")
	void constructor_withMessage_shouldSetMessage() {
		String message = "Tool was cancelled";
		ToolCancelledException exception = new ToolCancelledException(message);

		assertEquals(message, exception.getMessage(), "Message should be set");
		assertNull(exception.getCause(), "Cause should be null");
	}

	@Test
	@DisplayName("constructor with message and cause should set both")
	void constructor_withMessageAndCause_shouldSetBoth() {
		String message = "Tool was cancelled";
		RuntimeException cause = new RuntimeException("Original error");
		ToolCancelledException exception = new ToolCancelledException(message, cause);

		assertEquals(message, exception.getMessage(), "Message should be set");
		assertSame(cause, exception.getCause(), "Cause should be set");
	}

	@Test
	@DisplayName("should be a RuntimeException")
	void shouldBeRuntimeException() {
		ToolCancelledException exception = new ToolCancelledException("test");

		assertTrue(exception instanceof RuntimeException, "Should be a RuntimeException");
	}

	@Test
	@DisplayName("should preserve cause chain")
	void shouldPreserveCauseChain() {
		IllegalStateException rootCause = new IllegalStateException("Root cause");
		RuntimeException intermediateCause = new RuntimeException("Intermediate", rootCause);
		ToolCancelledException exception = new ToolCancelledException("Tool cancelled", intermediateCause);

		assertSame(intermediateCause, exception.getCause(), "Immediate cause should be preserved");
		assertSame(rootCause, exception.getCause().getCause(), "Root cause should be preserved");
	}

	@Test
	@DisplayName("null message should be allowed")
	void nullMessage_shouldBeAllowed() {
		ToolCancelledException exception = new ToolCancelledException(null);

		assertNull(exception.getMessage(), "Message should be null");
	}

	@Test
	@DisplayName("null cause should be allowed")
	void nullCause_shouldBeAllowed() {
		ToolCancelledException exception = new ToolCancelledException("message", null);

		assertEquals("message", exception.getMessage());
		assertNull(exception.getCause(), "Cause should be null");
	}

	@Test
	@DisplayName("exception should be throwable and catchable")
	void shouldBeThrowableAndCatchable() {
		boolean caught = false;

		try {
			throw new ToolCancelledException("Test cancellation");
		}
		catch (ToolCancelledException e) {
			caught = true;
			assertEquals("Test cancellation", e.getMessage());
		}

		assertTrue(caught, "Exception should be caught");
	}

	@Test
	@DisplayName("exception should be catchable as RuntimeException")
	void shouldBeCatchableAsRuntimeException() {
		boolean caught = false;

		try {
			throw new ToolCancelledException("Test cancellation");
		}
		catch (RuntimeException e) {
			caught = true;
			assertTrue(e instanceof ToolCancelledException);
		}

		assertTrue(caught, "Exception should be caught as RuntimeException");
	}

}
