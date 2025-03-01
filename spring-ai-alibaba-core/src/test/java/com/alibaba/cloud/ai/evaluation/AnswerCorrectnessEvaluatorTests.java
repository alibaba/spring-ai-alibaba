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
package com.alibaba.cloud.ai.evaluation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link AnswerCorrectnessEvaluator}. Tests cover constructor variations,
 * evaluation of correct and incorrect answers, and error handling scenarios.
 *
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 * @author brianxiadong
 * @since 1.0.0-M5.1
 */
class AnswerCorrectnessEvaluatorTests {

	// Test constants
	private static final String TEST_QUERY = "What is Spring AI?";

	private static final String TEST_RESPONSE = "Spring AI is a framework for building AI applications.";

	private static final String TEST_CONTEXT = "Spring AI is a framework for building AI applications.";

	private static final String CUSTOM_PROMPT = "Custom evaluation prompt text";

	private ChatClient chatClient;

	private ChatClient.Builder chatClientBuilder;

	private AnswerCorrectnessEvaluator evaluator;

	@BeforeEach
	void setUp() {
		// Initialize mocks and evaluator
		chatClient = Mockito.mock(ChatClient.class);
		chatClientBuilder = Mockito.mock(ChatClient.Builder.class);
		when(chatClientBuilder.build()).thenReturn(chatClient);
		evaluator = new AnswerCorrectnessEvaluator(chatClientBuilder);
	}

	/**
	 * Test constructor with ChatClient.Builder. Verifies that evaluator is created with
	 * default prompt.
	 */
	@Test
	void testConstructorWithBuilder() {
		AnswerCorrectnessEvaluator evaluator = new AnswerCorrectnessEvaluator(chatClientBuilder);
		assertThat(evaluator).isNotNull();
		assertThat(evaluator.getName()).isEqualTo("correctness");
	}

	/**
	 * Test constructor with custom evaluation prompt. Verifies that evaluator uses the
	 * custom prompt.
	 */
	@Test
	void testConstructorWithCustomPrompt() {
		AnswerCorrectnessEvaluator evaluator = new AnswerCorrectnessEvaluator(chatClientBuilder, CUSTOM_PROMPT);
		assertThat(evaluator).isNotNull();
		assertThat(evaluator.getEvaluationPromptText()).isEqualTo(CUSTOM_PROMPT);
	}

	/**
	 * Test constructor with ObjectMapper. Verifies that evaluator is created with custom
	 * ObjectMapper.
	 */
	@Test
	void testConstructorWithObjectMapper() {
		ObjectMapper objectMapper = new ObjectMapper();
		AnswerCorrectnessEvaluator evaluator = new AnswerCorrectnessEvaluator(chatClientBuilder, objectMapper);
		assertThat(evaluator).isNotNull();
		assertThat(evaluator.getObjectMapper()).isEqualTo(objectMapper);
	}

	/**
	 * Test evaluation when the answer is correct according to the context. Should return
	 * a passing evaluation with score 1.0.
	 */
	@Test
	void testEvaluateCorrectAnswer() {
		// Mock chat client to return "YES" for correct answer
		mockChatResponse("YES");

		// Create evaluation request with matching response and context
		EvaluationRequest request = createEvaluationRequest(TEST_QUERY, TEST_RESPONSE, TEST_CONTEXT);

		// Evaluate and verify
		EvaluationResponse response = evaluator.evaluate(request);
		assertThat(response.getScore()).isEqualTo(1.0f);
	}

	/**
	 * Test evaluation when the answer is incorrect or inconsistent with the context.
	 * Should return a failing evaluation with score 0.0.
	 */
	@Test
	void testEvaluateIncorrectAnswer() {
		// Mock chat client to return "NO" for incorrect answer
		mockChatResponse("NO");

		// Create evaluation request with incorrect response
		EvaluationRequest request = createEvaluationRequest(TEST_QUERY, "Spring AI is a database management system.",
				TEST_CONTEXT);

		// Evaluate and verify
		EvaluationResponse response = evaluator.evaluate(request);
		assertThat(response.getScore()).isEqualTo(0.0f);
	}

	/**
	 * Test evaluation with empty context. Should handle empty context gracefully.
	 */
	@Test
	void testEvaluateWithEmptyContext() {
		// Mock chat client response
		mockChatResponse("NO");

		// Create evaluation request with empty context
		EvaluationRequest request = createEvaluationRequest(TEST_QUERY, TEST_RESPONSE, "");

		// Evaluate and verify
		EvaluationResponse response = evaluator.evaluate(request);
		assertThat(response.getScore()).isEqualTo(0.0f);
	}

	/**
	 * Test evaluation with null request. Should throw IllegalArgumentException.
	 */
	@Test
	void testEvaluateWithNullRequest() {
		assertThatThrownBy(() -> evaluator.evaluate(null)).isInstanceOf(IllegalArgumentException.class);
	}

	/**
	 * Test the evaluator's name is correctly set.
	 */
	@Test
	void testEvaluatorName() {
		assertThat(evaluator.getName()).isEqualTo("correctness");
	}

	/**
	 * Helper method to create evaluation request
	 */
	private EvaluationRequest createEvaluationRequest(String query, String response, String context) {
		Document document = new Document(context);
		return new EvaluationRequest(query, Collections.singletonList(document), response);
	}

	/**
	 * Helper method to mock chat client response
	 */
	private void mockChatResponse(String content) {
		ChatClient.ChatClientRequestSpec requestSpec = Mockito.mock(ChatClient.ChatClientRequestSpec.class);
		ChatClient.CallResponseSpec responseSpec = Mockito.mock(ChatClient.CallResponseSpec.class);

		// Mock the chain of method calls
		when(chatClient.prompt()).thenReturn(requestSpec);
		when(requestSpec.user(any(Consumer.class))).thenReturn(requestSpec);
		when(requestSpec.call()).thenReturn(responseSpec);
		when(responseSpec.content()).thenReturn(content);
	}

}
