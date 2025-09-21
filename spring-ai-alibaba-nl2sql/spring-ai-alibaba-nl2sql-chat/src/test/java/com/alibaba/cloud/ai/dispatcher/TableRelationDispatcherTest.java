/*
 * Copyright 2025 the original author or authors.
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

package com.alibaba.cloud.ai.dispatcher;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.alibaba.cloud.ai.constant.Constant.*;
import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TableRelationDispatcherTest {

	private TableRelationDispatcher dispatcher;

	private OverAllState state;

	@BeforeEach
	void setUp() {
		dispatcher = new TableRelationDispatcher();

		// Initialize state
		state = new OverAllState();
		state.registerKeyAndStrategy(TABLE_RELATION_EXCEPTION_OUTPUT, new ReplaceStrategy());
		state.registerKeyAndStrategy(TABLE_RELATION_RETRY_COUNT, new ReplaceStrategy());
		state.registerKeyAndStrategy(TABLE_RELATION_OUTPUT, new ReplaceStrategy());
	}

	@Test
	void testSuccessfulExecution() throws Exception {
		// Set success state
		state.updateState(Map.of(TABLE_RELATION_OUTPUT, "mock_generator_output"));

		// Execute test
		String result = dispatcher.apply(state);

		// Verify routing decision
		assertEquals(PLANNER_NODE, result);
	}

	@Test
	void testRetryableError_WithinRetryLimit() throws Exception {
		// Set retryable error, retry count not reached limit
		state.updateState(Map.of(TABLE_RELATION_EXCEPTION_OUTPUT, "RETRYABLE: Connection timeout",
				TABLE_RELATION_RETRY_COUNT, 2));

		// execute test
		String result = dispatcher.apply(state);

		// Verify should retry
		assertEquals(TABLE_RELATION_NODE, result);
	}

	@Test
	void testRetryableError_ExceedsRetryLimit() throws Exception {
		// Set retryable error, but retry count reached limit
		state.updateState(Map.of(TABLE_RELATION_EXCEPTION_OUTPUT, "RETRYABLE: Connection timeout",
				TABLE_RELATION_RETRY_COUNT, 3));

		// execute test
		String result = dispatcher.apply(state);

		// Verify should terminate
		assertEquals(END, result);
	}

	@Test
	void testNonRetryableError() throws Exception {
		// Set non-retryable error
		state.updateState(Map.of(TABLE_RELATION_EXCEPTION_OUTPUT,
				"NON_RETRYABLE: Feature not supported: converting to class boolean", TABLE_RELATION_RETRY_COUNT, 1));

		// execute test
		String result = dispatcher.apply(state);

		// Verify should terminate
		assertEquals(END, result);
	}

	@Test
	void testNoOutput_NoError() throws Exception {
		// Set no output no error state
		state.updateState(Map.of());

		// execute test
		String result = dispatcher.apply(state);

		// Verify should terminate
		assertEquals(END, result);
	}

	@Test
	void testRetryCountBoundary() throws Exception {
		// Test retry count boundary case
		state.updateState(
				Map.of(TABLE_RELATION_EXCEPTION_OUTPUT, "RETRYABLE: Network error", TABLE_RELATION_RETRY_COUNT, 2 // Just
																													// within
																													// the
																													// limit
				));

		String result = dispatcher.apply(state);
		assertEquals(TABLE_RELATION_NODE, result);

		// Test reaching the limit
		state.updateState(
				Map.of(TABLE_RELATION_EXCEPTION_OUTPUT, "RETRYABLE: Network error", TABLE_RELATION_RETRY_COUNT, 3 // 达到限制
				));

		result = dispatcher.apply(state);
		assertEquals(END, result);
	}

	@Test
	void testMixedErrorTypes() throws Exception {
		// Test handling of different error types
		String[] retryableErrors = { "RETRYABLE: timeout", "RETRYABLE: connection" };

		String[] nonRetryableErrors = { "NON_RETRYABLE: boolean conversion", "NON_RETRYABLE: configuration error" };

		// Test retryable errors
		for (String errorType : retryableErrors) {
			state.updateState(Map.of(TABLE_RELATION_EXCEPTION_OUTPUT, errorType, TABLE_RELATION_RETRY_COUNT, 1));

			String result = dispatcher.apply(state);
			assertEquals(TABLE_RELATION_NODE, result, "Should retry for: " + errorType);
		}

		// Test non-retryable errors
		for (String errorType : nonRetryableErrors) {
			state.updateState(Map.of(TABLE_RELATION_EXCEPTION_OUTPUT, errorType, TABLE_RELATION_RETRY_COUNT, 1));

			String result = dispatcher.apply(state);
			assertEquals(END, result, "Should end for: " + errorType);
		}
	}

	@Test
	void testZeroRetryCount() throws Exception {
		// Test case with retry count 0
		state.updateState(Map.of(TABLE_RELATION_EXCEPTION_OUTPUT, "RETRYABLE: Connection timeout",
				TABLE_RELATION_RETRY_COUNT, 0));

		String result = dispatcher.apply(state);
		assertEquals(TABLE_RELATION_NODE, result);
	}

	@Test
	void testNullRetryCount() throws Exception {
		// Test case with null retry count (should be treated as 0)
		state.updateState(Map.of(TABLE_RELATION_EXCEPTION_OUTPUT, "RETRYABLE: Connection timeout"
		// 不设置 TABLE_RELATION_RETRY_COUNT
		));

		String result = dispatcher.apply(state);
		assertEquals(TABLE_RELATION_NODE, result);
	}

	@Test
	void testSuccessWithPreviousError() throws Exception {
		// Test case with previous error but now successful
		state.updateState(Map.of(TABLE_RELATION_OUTPUT, "success_output", TABLE_RELATION_EXCEPTION_OUTPUT, "", // 错误已清除
				TABLE_RELATION_RETRY_COUNT, 0));

		String result = dispatcher.apply(state);
		assertEquals(PLANNER_NODE, result);
	}

}
