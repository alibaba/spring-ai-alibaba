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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Tests for Builder async tool execution configuration methods.
 *
 * <p>
 * Covers the public API for configuring async tool execution in ReactAgent.Builder:
 * - parallelToolExecution(boolean)
 * - maxParallelTools(int)
 * - toolExecutionTimeout(Duration)
 * - wrapSyncToolsAsAsync(boolean)
 * </p>
 *
 * @author Claude
 * @since 1.0.0
 */
@DisplayName("Builder Async Configuration Tests")
class BuilderAsyncConfigTest {

	private final ChatModel mockChatModel = mock(ChatModel.class);

	/**
	 * Test helper class to access protected fields from Builder.
	 */
	private static class TestableBuilder extends DefaultBuilder {

		public boolean getParallelToolExecution() {
			return this.parallelToolExecution;
		}

		public int getMaxParallelTools() {
			return this.maxParallelTools;
		}

		public Duration getToolExecutionTimeout() {
			return this.toolExecutionTimeout;
		}

		public boolean getWrapSyncToolsAsAsync() {
			return this.wrapSyncToolsAsAsync;
		}

	}

	@Nested
	@DisplayName("parallelToolExecution() tests")
	class ParallelToolExecutionTests {

		@Test
		@DisplayName("should default to false")
		void shouldDefaultToFalse() {
			TestableBuilder builder = new TestableBuilder();
			assertFalse(builder.getParallelToolExecution());
		}

		@Test
		@DisplayName("should enable parallel execution when set to true")
		void shouldEnableParallelExecution() {
			TestableBuilder builder = new TestableBuilder();
			builder.parallelToolExecution(true);
			assertTrue(builder.getParallelToolExecution());
		}

		@Test
		@DisplayName("should disable parallel execution when set to false")
		void shouldDisableParallelExecution() {
			TestableBuilder builder = new TestableBuilder();
			builder.parallelToolExecution(true);
			builder.parallelToolExecution(false);
			assertFalse(builder.getParallelToolExecution());
		}

		@Test
		@DisplayName("should return builder for fluent chaining")
		void shouldReturnBuilderForFluent() {
			TestableBuilder builder = new TestableBuilder();
			Builder result = builder.parallelToolExecution(true);
			assertEquals(builder, result);
		}

	}

	@Nested
	@DisplayName("maxParallelTools() tests")
	class MaxParallelToolsTests {

		@Test
		@DisplayName("should default to 5")
		void shouldDefaultToFive() {
			TestableBuilder builder = new TestableBuilder();
			assertEquals(5, builder.getMaxParallelTools());
		}

		@Test
		@DisplayName("should accept valid value of 1")
		void shouldAcceptMinimumValue() {
			TestableBuilder builder = new TestableBuilder();
			builder.maxParallelTools(1);
			assertEquals(1, builder.getMaxParallelTools());
		}

		@Test
		@DisplayName("should accept large values")
		void shouldAcceptLargeValues() {
			TestableBuilder builder = new TestableBuilder();
			builder.maxParallelTools(100);
			assertEquals(100, builder.getMaxParallelTools());
		}

		@Test
		@DisplayName("should throw IllegalArgumentException for zero")
		void shouldThrowForZero() {
			TestableBuilder builder = new TestableBuilder();
			IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> builder.maxParallelTools(0));
			assertEquals("maxParallelTools must be at least 1", ex.getMessage());
		}

		@Test
		@DisplayName("should throw IllegalArgumentException for negative values")
		void shouldThrowForNegativeValues() {
			TestableBuilder builder = new TestableBuilder();
			assertThrows(IllegalArgumentException.class, () -> builder.maxParallelTools(-1));
			assertThrows(IllegalArgumentException.class, () -> builder.maxParallelTools(-100));
		}

		@Test
		@DisplayName("should return builder for fluent chaining")
		void shouldReturnBuilderForFluent() {
			TestableBuilder builder = new TestableBuilder();
			Builder result = builder.maxParallelTools(10);
			assertEquals(builder, result);
		}

	}

	@Nested
	@DisplayName("toolExecutionTimeout() tests")
	class ToolExecutionTimeoutTests {

		@Test
		@DisplayName("should default to 5 minutes")
		void shouldDefaultToFiveMinutes() {
			TestableBuilder builder = new TestableBuilder();
			assertEquals(Duration.ofMinutes(5), builder.getToolExecutionTimeout());
		}

		@Test
		@DisplayName("should accept custom duration")
		void shouldAcceptCustomDuration() {
			TestableBuilder builder = new TestableBuilder();
			builder.toolExecutionTimeout(Duration.ofSeconds(30));
			assertEquals(Duration.ofSeconds(30), builder.getToolExecutionTimeout());
		}

		@Test
		@DisplayName("should accept very short timeout")
		void shouldAcceptShortTimeout() {
			TestableBuilder builder = new TestableBuilder();
			builder.toolExecutionTimeout(Duration.ofMillis(100));
			assertEquals(Duration.ofMillis(100), builder.getToolExecutionTimeout());
		}

		@Test
		@DisplayName("should accept very long timeout")
		void shouldAcceptLongTimeout() {
			TestableBuilder builder = new TestableBuilder();
			builder.toolExecutionTimeout(Duration.ofHours(1));
			assertEquals(Duration.ofHours(1), builder.getToolExecutionTimeout());
		}

		@Test
		@DisplayName("should throw NullPointerException for null timeout")
		void shouldThrowForNullTimeout() {
			TestableBuilder builder = new TestableBuilder();
			assertThrows(NullPointerException.class, () -> builder.toolExecutionTimeout(null));
		}

		@Test
		@DisplayName("should return builder for fluent chaining")
		void shouldReturnBuilderForFluent() {
			TestableBuilder builder = new TestableBuilder();
			Builder result = builder.toolExecutionTimeout(Duration.ofMinutes(10));
			assertEquals(builder, result);
		}

	}

	@Nested
	@DisplayName("wrapSyncToolsAsAsync() tests")
	class WrapSyncToolsAsAsyncTests {

		@Test
		@DisplayName("should default to false")
		void shouldDefaultToFalse() {
			TestableBuilder builder = new TestableBuilder();
			assertFalse(builder.getWrapSyncToolsAsAsync());
		}

		@Test
		@DisplayName("should enable wrapping when set to true")
		void shouldEnableWrapping() {
			TestableBuilder builder = new TestableBuilder();
			builder.wrapSyncToolsAsAsync(true);
			assertTrue(builder.getWrapSyncToolsAsAsync());
		}

		@Test
		@DisplayName("should disable wrapping when set to false")
		void shouldDisableWrapping() {
			TestableBuilder builder = new TestableBuilder();
			builder.wrapSyncToolsAsAsync(true);
			builder.wrapSyncToolsAsAsync(false);
			assertFalse(builder.getWrapSyncToolsAsAsync());
		}

		@Test
		@DisplayName("should return builder for fluent chaining")
		void shouldReturnBuilderForFluent() {
			TestableBuilder builder = new TestableBuilder();
			Builder result = builder.wrapSyncToolsAsAsync(true);
			assertEquals(builder, result);
		}

	}

	@Nested
	@DisplayName("Fluent API chaining tests")
	class FluentApiTests {

		@Test
		@DisplayName("should support chaining all async configuration methods")
		void shouldSupportChainingAllMethods() {
			TestableBuilder builder = new TestableBuilder();

			Builder result = builder.parallelToolExecution(true)
				.maxParallelTools(10)
				.toolExecutionTimeout(Duration.ofMinutes(10))
				.wrapSyncToolsAsAsync(true);

			assertEquals(builder, result);
			assertTrue(builder.getParallelToolExecution());
			assertEquals(10, builder.getMaxParallelTools());
			assertEquals(Duration.ofMinutes(10), builder.getToolExecutionTimeout());
			assertTrue(builder.getWrapSyncToolsAsAsync());
		}

		@Test
		@DisplayName("should support mixing async config with other builder methods")
		void shouldSupportMixingWithOtherMethods() {
			TestableBuilder builder = new TestableBuilder();

			assertDoesNotThrow(() -> builder.name("test-agent")
				.parallelToolExecution(true)
				.maxParallelTools(5)
				.toolExecutionTimeout(Duration.ofMinutes(2))
				.wrapSyncToolsAsAsync(true)
				.enableLogging(true));
		}

	}

}
