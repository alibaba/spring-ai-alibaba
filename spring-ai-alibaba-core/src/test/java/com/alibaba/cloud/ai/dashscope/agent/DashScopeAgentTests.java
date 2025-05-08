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

import com.alibaba.cloud.ai.dashscope.api.DashScopeAgentApi;
import com.alibaba.cloud.ai.dashscope.api.DashScopeAgentApi.DashScopeAgentRequest;
import com.alibaba.cloud.ai.dashscope.api.DashScopeAgentApi.DashScopeAgentResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Test cases for DashScopeAgent.
 *
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 * @author brianxiadong
 * @since 1.0.0-M2
 */
@ExtendWith(MockitoExtension.class)
class DashScopeAgentTests {

	private static final String TEST_APP_ID = "test-app-id";

	private static final String TEST_USER_MESSAGE = "Hello, AI!";

	private static final String TEST_ASSISTANT_RESPONSE = "Hello, Human!";

	@Mock
	private DashScopeAgentApi dashScopeAgentApi;

	private DashScopeAgent agent;

	private DashScopeAgentOptions options;

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

		// Create agent options
		options = DashScopeAgentOptions.builder()
			.withAppId(TEST_APP_ID)
			.withSessionId("test-session")
			.withMemoryId("test-memory")
			.withIncrementalOutput(false)
			.withHasThoughts(false)
			.withBizParams(testBizParams)
			.build();

		// Create agent instance
		agent = new DashScopeAgent(dashScopeAgentApi, options);
	}

	/**
	 * Test successful call with valid prompt
	 */
	@Test
	void testSuccessfulCall() {
		// Prepare test data
		Message message = new UserMessage(TEST_USER_MESSAGE);
		Prompt prompt = new Prompt(List.of(message), options);

		// Create mock response
		DashScopeAgentResponse response = createMockResponse();

		// Mock API behavior
		when(dashScopeAgentApi.call(any(DashScopeAgentRequest.class))).thenReturn(ResponseEntity.ok(response));

		// Execute test
		var result = agent.call(prompt);

		// Verify response
		assertThat(result).isNotNull();
		assertThat(result.getResults()).hasSize(1);
		var assistantMessage = result.getResults().get(0).getOutput();
		assertThat(assistantMessage.toString()).contains(TEST_ASSISTANT_RESPONSE);
		assertThat(result.getResults().get(0).getMetadata().getFinishReason()).isEqualTo("stop");
	}

	/**
	 * Test call with null response
	 */
	@Test
	void testCallWithNullResponse() {
		// Prepare test data
		Message message = new UserMessage(TEST_USER_MESSAGE);
		Prompt prompt = new Prompt(List.of(message), options);

		// Mock API behavior to return null
		when(dashScopeAgentApi.call(any(DashScopeAgentRequest.class))).thenReturn(null);

		// Execute test
		var result = agent.call(prompt);

		// Verify response is null
		assertThat(result).isNull();
	}

	/**
	 * Test call with null prompt
	 */
	@Test
	void testCallWithNullPrompt() {
		// Execute test and verify exception
		assertThatThrownBy(() -> agent.call(null)).isInstanceOf(IllegalArgumentException.class)
			.hasMessage("option is null");
	}

	/**
	 * Test call with null appId
	 */
	@Test
	void testCallWithNullAppId() {
		// Prepare test data with null appId
		DashScopeAgentOptions optionsWithNullAppId = DashScopeAgentOptions.builder().withAppId("").build();
		Message message = new UserMessage(TEST_USER_MESSAGE);
		Prompt prompt = new Prompt(List.of(message), optionsWithNullAppId);

		// Execute test and verify exception
		assertThatThrownBy(() -> agent.call(prompt)).isInstanceOf(IllegalArgumentException.class)
			.hasMessage("appId must be set");
	}

	/**
	 * Test successful stream with valid prompt
	 */
	@Test
	void testSuccessfulStream() {
		// Prepare test data
		Message message = new UserMessage(TEST_USER_MESSAGE);
		Prompt prompt = new Prompt(List.of(message), options);

		// Create mock response
		DashScopeAgentResponse response = createMockResponse();

		// Mock API behavior
		when(dashScopeAgentApi.stream(any(DashScopeAgentRequest.class))).thenReturn(Flux.just(response));

		// Execute test
		var resultFlux = agent.stream(prompt);

		// Verify stream response
		StepVerifier.create(resultFlux).assertNext(result -> {
			assertThat(result).isNotNull();
			assertThat(result.getResults()).hasSize(1);
			var assistantMessage = result.getResults().get(0).getOutput();
			assertThat(assistantMessage.toString()).contains(TEST_ASSISTANT_RESPONSE);
			assertThat(result.getResults().get(0).getMetadata().getFinishReason()).isEqualTo("stop");
		}).verifyComplete();
	}

	/**
	 * Test stream with null prompt
	 */
	@Test
	void testStreamWithNullPrompt() {
		// Execute test and verify exception
		assertThatThrownBy(() -> agent.stream(null)).isInstanceOf(IllegalArgumentException.class)
			.hasMessage("option is null");
	}

	/**
	 * Test default constructor
	 */
	@Test
	void testDefaultConstructor() {
		// Create agent with default constructor
		DashScopeAgent defaultAgent = new DashScopeAgent(dashScopeAgentApi);

		// Verify default options are set
		Message message = new UserMessage(TEST_USER_MESSAGE);
		DashScopeAgentOptions defaultOptions = DashScopeAgentOptions.builder().withAppId(TEST_APP_ID).build();
		Prompt prompt = new Prompt(List.of(message), defaultOptions);

		// Create mock response
		DashScopeAgentResponse response = createMockResponse();

		// Mock API behavior
		when(dashScopeAgentApi.call(any(DashScopeAgentRequest.class))).thenReturn(ResponseEntity.ok(response));

		// Execute test
		var result = defaultAgent.call(prompt);

		// Verify response
		assertThat(result).isNotNull();
		assertThat(result.getResults()).hasSize(1);
	}

	/**
	 * Helper method to create a mock DashScopeAgentResponse
	 */
	private DashScopeAgentResponse createMockResponse() {
		// Create thoughts list
		var thought = new DashScopeAgentResponse.DashScopeAgentResponseOutput.DashScopeAgentResponseOutputThoughts(
				"test thought", "test action type", "test action name", "test action", "test input stream",
				"test input", "test response", "test observation", "test reasoning content");
		List<DashScopeAgentResponse.DashScopeAgentResponseOutput.DashScopeAgentResponseOutputThoughts> thoughts = List
			.of(thought);

		// Create doc references list (empty for this test)
		List<DashScopeAgentResponse.DashScopeAgentResponseOutput.DashScopeAgentResponseOutputDocReference> docReferences = List
			.of();

		// Create output with all required parameters
		var output = new DashScopeAgentResponse.DashScopeAgentResponseOutput(TEST_ASSISTANT_RESPONSE, // text
				"stop", // finishReason
				"test-session", // sessionId
				thoughts, // thoughts list
				docReferences // docReferences list
		);

		// Create usage with models list
		var usageModel = new DashScopeAgentResponse.DashScopeAgentResponseUsage.DashScopeAgentResponseUsageModels(
				"test-model", 10, 20);
		var usage = new DashScopeAgentResponse.DashScopeAgentResponseUsage(List.of(usageModel));

		return new DashScopeAgentResponse(null, "request-123", null, null, output, usage);
	}

}
