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
import org.springframework.ai.document.Document;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;

import java.util.Collections;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link AnswerRelevancyEvaluator}. Tests cover constructor variations,
 * evaluation of relevant and irrelevant answers, and error handling scenarios.
 *
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 * @author brianxiadong
 * @since 1.0.0-M5.1
 */
class AnswerRelevancyEvaluatorTests {

	// Test constants
	private static final String TEST_QUESTION = "What is the capital of France?";

	private static final String TEST_CORRECT_ANSWER = "The capital of France is Paris, which is also the largest city in the country.";

	private static final String TEST_STUDENT_ANSWER = "Paris is the capital city of France.";

	private static final String CUSTOM_PROMPT = "Custom evaluation prompt text";

	private ChatClient chatClient;

	private ChatClient.Builder chatClientBuilder;

	private AnswerRelevancyEvaluator evaluator;

	@BeforeEach
	void setUp() {
		// Initialize mocks and evaluator
		chatClient = Mockito.mock(ChatClient.class);
		chatClientBuilder = Mockito.mock(ChatClient.Builder.class);
		when(chatClientBuilder.build()).thenReturn(chatClient);

		// Initialize evaluator with ObjectMapper to avoid NPE
		ObjectMapper objectMapper = new ObjectMapper();
		evaluator = new AnswerRelevancyEvaluator(chatClientBuilder, objectMapper);
	}

	/**
	 * Test constructor with ChatClient.Builder. Verifies that evaluator is created with
	 * default prompt.
	 */
	@Test
	void testConstructorWithBuilder() {
		AnswerRelevancyEvaluator evaluator = new AnswerRelevancyEvaluator(chatClientBuilder);
		assertThat(evaluator).isNotNull();
		assertThat(evaluator.getName()).isEqualTo("relevancy");
	}

	/**
	 * Test constructor with custom evaluation prompt. Verifies that evaluator uses the
	 * custom prompt.
	 */
	@Test
	void testConstructorWithCustomPrompt() {
		AnswerRelevancyEvaluator evaluator = new AnswerRelevancyEvaluator(chatClientBuilder, CUSTOM_PROMPT);
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
		AnswerRelevancyEvaluator evaluator = new AnswerRelevancyEvaluator(chatClientBuilder, objectMapper);
		assertThat(evaluator).isNotNull();
		assertThat(evaluator.getObjectMapper()).isEqualTo(objectMapper);
	}

	/**
	 * Test evaluation when the student answer is relevant and accurate. Should return a
	 * passing evaluation with high score.
	 */
	@Test
	void testEvaluateRelevantAnswer() {
		// Mock chat client to return a high score response
		mockChatResponse("{\"score\": 1.0, \"feedback\": \"The answer is accurate and relevant.\"}");

		// Create evaluation request with relevant answer
		EvaluationRequest request = createEvaluationRequest(TEST_QUESTION, TEST_STUDENT_ANSWER, TEST_CORRECT_ANSWER);

		// Evaluate and verify
		EvaluationResponse response = evaluator.evaluate(request);
		assertThat(response.getScore()).isEqualTo(1.0f);
		assertThat(response.getFeedback()).isEqualTo("The answer is accurate and relevant.");
	}

	/**
	 * Test evaluation when the student answer is irrelevant. Should return a failing
	 * evaluation with low score.
	 */
	@Test
	void testEvaluateIrrelevantAnswer() {
		// Mock chat client to return a low score response
		mockChatResponse("{\"score\": 0.0, \"feedback\": \"The answer is completely irrelevant to the question.\"}");

		// Create evaluation request with irrelevant answer
		String irrelevantAnswer = "London is the capital of England.";
		EvaluationRequest request = createEvaluationRequest(TEST_QUESTION, irrelevantAnswer, TEST_CORRECT_ANSWER);

		// Evaluate and verify
		EvaluationResponse response = evaluator.evaluate(request);
		assertThat(response.getScore()).isEqualTo(0.0f);
		assertThat(response.getFeedback()).isEqualTo("The answer is completely irrelevant to the question.");
	}

	/**
	 * Test evaluation with partially correct answer. Should return a moderate score based
	 * on relevancy.
	 */
	@Test
	void testEvaluatePartiallyCorrectAnswer() {
		// Mock chat client to return a moderate score response
		mockChatResponse(
				"{\"score\": 0.5, \"feedback\": \"The answer is partially relevant but lacks important details.\"}");

		// Create evaluation request with partially correct answer
		String partialAnswer = "Paris is in France.";
		EvaluationRequest request = createEvaluationRequest(TEST_QUESTION, partialAnswer, TEST_CORRECT_ANSWER);

		// Evaluate and verify
		EvaluationResponse response = evaluator.evaluate(request);
		assertThat(response.getScore()).isEqualTo(0.5f);
		assertThat(response.getFeedback()).isEqualTo("The answer is partially relevant but lacks important details.");
	}

	/**
	 * Test evaluation with empty correct answer. Should handle empty correct answer
	 * gracefully.
	 */
	@Test
	void testEvaluateWithEmptyCorrectAnswer() {
		// Mock chat client response for empty correct answer
		mockChatResponse("{\"score\": 0.0, \"feedback\": \"No correct answer provided for evaluation.\"}");

		// Create evaluation request with empty correct answer
		EvaluationRequest request = createEvaluationRequest(TEST_QUESTION, TEST_STUDENT_ANSWER, "");

		// Evaluate and verify
		EvaluationResponse response = evaluator.evaluate(request);
		assertThat(response.getScore()).isEqualTo(0.0f);
		assertThat(response.getFeedback()).isEqualTo("No correct answer provided for evaluation.");
	}

	/**
	 * Test evaluation with null request. Should throw IllegalArgumentException.
	 */
	@Test
	void testEvaluateWithNullRequest() {
		assertThatThrownBy(() -> evaluator.evaluate(null)).isInstanceOf(IllegalArgumentException.class)
			.hasMessage("EvaluationRequest must not be null");
	}

	/**
	 * Test evaluation with invalid JSON response. Should handle JSON parsing error
	 * gracefully.
	 */
	@Test
	void testEvaluateWithInvalidJsonResponse() {
		// Mock chat client to return invalid JSON
		mockChatResponse("Invalid JSON response");

		// Create evaluation request
		EvaluationRequest request = createEvaluationRequest(TEST_QUESTION, TEST_STUDENT_ANSWER, TEST_CORRECT_ANSWER);

		// Evaluate and verify exception is thrown
		assertThatThrownBy(() -> evaluator.evaluate(request)).isInstanceOf(RuntimeException.class)
			.hasRootCauseInstanceOf(Exception.class);
	}

	/**
	 * Test the evaluator's name is correctly set.
	 */
	@Test
	void testEvaluatorName() {
		assertThat(evaluator.getName()).isEqualTo("relevancy");
	}

	/**
	 * Helper method to create evaluation request
	 */
	private EvaluationRequest createEvaluationRequest(String question, String studentAnswer, String correctAnswer) {
		Document document = new Document(correctAnswer);
		return new EvaluationRequest(question, Collections.singletonList(document), studentAnswer);
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
