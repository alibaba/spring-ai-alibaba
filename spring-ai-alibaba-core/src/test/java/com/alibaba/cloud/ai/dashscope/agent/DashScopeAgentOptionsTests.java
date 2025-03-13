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
package com.alibaba.cloud.ai.dashscope.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test cases for DashScopeAgentOptions.
 *
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 * @author brianxiadong
 * @since 1.0.0-M2
 */
class DashScopeAgentOptionsTests {

	private static final String TEST_APP_ID = "test-app-id";

	private static final String TEST_SESSION_ID = "test-session-id";

	private static final String TEST_MEMORY_ID = "test-memory-id";

	private ObjectMapper objectMapper;

	private JsonNode testBizParams;

	@BeforeEach
	void setUp() {
		// Initialize ObjectMapper and create test bizParams
		objectMapper = new ObjectMapper();
		ObjectNode bizParams = objectMapper.createObjectNode();
		bizParams.put("key1", "value1");
		bizParams.put("key2", "value2");
		testBizParams = bizParams;
	}

	/**
	 * Test builder pattern for creating DashScopeAgentOptions
	 */
	@Test
	void testBuilder() {
		// Create options using builder
		DashScopeAgentOptions options = DashScopeAgentOptions.builder()
			.withAppId(TEST_APP_ID)
			.withSessionId(TEST_SESSION_ID)
			.withMemoryId(TEST_MEMORY_ID)
			.withIncrementalOutput(true)
			.withHasThoughts(true)
			.withBizParams(testBizParams)
			.build();

		// Verify all fields are set correctly
		assertThat(options.getAppId()).isEqualTo(TEST_APP_ID);
		assertThat(options.getSessionId()).isEqualTo(TEST_SESSION_ID);
		assertThat(options.getMemoryId()).isEqualTo(TEST_MEMORY_ID);
		assertThat(options.getIncrementalOutput()).isTrue();
		assertThat(options.getHasThoughts()).isTrue();
		assertThat(options.getBizParams()).isEqualTo(testBizParams);
	}

	/**
	 * Test copy functionality of DashScopeAgentOptions
	 */
	@Test
	void testCopy() {
		// Create original options
		DashScopeAgentOptions original = DashScopeAgentOptions.builder()
			.withAppId(TEST_APP_ID)
			.withSessionId(TEST_SESSION_ID)
			.withIncrementalOutput(true)
			.withHasThoughts(true)
			.withBizParams(testBizParams)
			.build();

		// Create copy using copy() method
		DashScopeAgentOptions copy = (DashScopeAgentOptions) original.copy();

		// Verify copied options match original
		assertThat(copy.getAppId()).isEqualTo(original.getAppId());
		assertThat(copy.getSessionId()).isEqualTo(original.getSessionId());
		assertThat(copy.getIncrementalOutput()).isEqualTo(original.getIncrementalOutput());
		assertThat(copy.getHasThoughts()).isEqualTo(original.getHasThoughts());
		assertThat(copy.getBizParams()).isEqualTo(original.getBizParams());
	}

	/**
	 * Test default values of ChatOptions interface methods
	 */
	@Test
	void testChatOptionsDefaults() {
		DashScopeAgentOptions options = DashScopeAgentOptions.builder().build();

		// Verify default values for ChatOptions interface methods
		assertThat(options.getModel()).isNull();
		assertThat(options.getFrequencyPenalty()).isNull();
		assertThat(options.getMaxTokens()).isNull();
		assertThat(options.getPresencePenalty()).isNull();
		assertThat(options.getStopSequences()).isNull();
		assertThat(options.getTemperature()).isEqualTo(0d);
		assertThat(options.getTopP()).isEqualTo(0d);
		assertThat(options.getTopK()).isEqualTo(0);
	}

	/**
	 * Test toString method for proper string representation
	 */
	@Test
	void testToString() {
		DashScopeAgentOptions options = DashScopeAgentOptions.builder()
			.withAppId(TEST_APP_ID)
			.withSessionId(TEST_SESSION_ID)
			.withMemoryId(TEST_MEMORY_ID)
			.withIncrementalOutput(true)
			.withHasThoughts(false)
			.withBizParams(testBizParams)
			.build();

		String toString = options.toString();

		// Verify toString contains all field values
		assertThat(toString).contains(TEST_APP_ID)
			.contains(TEST_SESSION_ID)
			.contains(TEST_MEMORY_ID)
			.contains("incrementalOutput=true")
			.contains("hasThoughts=false")
			.contains(testBizParams.toString());
	}

	/**
	 * Test setters for all fields
	 */
	@Test
	void testSetters() {
		DashScopeAgentOptions options = new DashScopeAgentOptions();

		// Set values using setters
		options.setAppId(TEST_APP_ID);
		options.setSessionId(TEST_SESSION_ID);
		options.setMemoryId(TEST_MEMORY_ID);
		options.setIncrementalOutput(true);
		options.setHasThoughts(true);
		options.setBizParams(testBizParams);

		// Verify all values are set correctly
		assertThat(options.getAppId()).isEqualTo(TEST_APP_ID);
		assertThat(options.getSessionId()).isEqualTo(TEST_SESSION_ID);
		assertThat(options.getMemoryId()).isEqualTo(TEST_MEMORY_ID);
		assertThat(options.getIncrementalOutput()).isTrue();
		assertThat(options.getHasThoughts()).isTrue();
		assertThat(options.getBizParams()).isEqualTo(testBizParams);
	}

}
