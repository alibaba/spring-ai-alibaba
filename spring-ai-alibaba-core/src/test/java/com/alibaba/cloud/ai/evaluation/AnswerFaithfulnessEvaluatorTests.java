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
 * Tests for {@link AnswerFaithfulnessEvaluator}. Tests cover constructor variations,
 * evaluation of faithful and unfaithful answers, and error handling scenarios.
 *
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 * @author brianxiadong
 * @since 1.0.0-M5.1
 */
class AnswerFaithfulnessEvaluatorTests {

	// Test constants
	private static final String TEST_FACTS = "The Earth is the third planet from the Sun and the only astronomical object known to harbor life.";

	private static final String TEST_STUDENT_ANSWER = "The Earth is the third planet from the Sun and supports life.";

	private static final String CUSTOM_PROMPT = "Custom evaluation prompt text";

	private ChatClient chatClient;

	private ChatClient.Builder chatClientBuilder;

	private AnswerFaithfulnessEvaluator evaluator;

	@BeforeEach
	void setUp() {
		// Initialize mocks and evaluator
		chatClient = Mockito.mock(ChatClient.class);
		chatClientBuilder = Mockito.mock(ChatClient.Builder.class);
		when(chatClientBuilder.build()).thenReturn(chatClient);

		// Initialize evaluator with ObjectMapper
		ObjectMapper objectMapper = new ObjectMapper();
		evaluator = new AnswerFaithfulnessEvaluator(chatClientBuilder, objectMapper);
	}

	/**
	 * Test constructor with ChatClient.Builder. Verifies that evaluator is created with
	 * default prompt.
	 */
	@Test
	void testConstructorWithBuilder() {
		AnswerFaithfulnessEvaluator evaluator = new AnswerFaithfulnessEvaluator(chatClientBuilder);
		assertThat(evaluator).isNotNull();
		assertThat(evaluator.getName()).isEqualTo("faithfulness");
	}

	/**
	 * Test constructor with custom evaluation prompt. Verifies that evaluator uses the
	 * custom prompt.
	 */
	@Test
	void testConstructorWithCustomPrompt() {
		AnswerFaithfulnessEvaluator evaluator = new AnswerFaithfulnessEvaluator(chatClientBuilder, CUSTOM_PROMPT);
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
		AnswerFaithfulnessEvaluator evaluator = new AnswerFaithfulnessEvaluator(chatClientBuilder, objectMapper);
		assertThat(evaluator).isNotNull();
		assertThat(evaluator.getObjectMapper()).isEqualTo(objectMapper);
	}

	/**
	 * Test evaluation when the student answer is faithful to the facts. Should return a
	 * passing evaluation with high score.
	 */
	@Test
	void testEvaluateFaithfulAnswer() {
		// Mock chat client to return a high score response
		mockChatResponse("{\"score\": 1.0, \"feedback\": \"The answer is faithful to the facts.\"}");

		// Create evaluation request with faithful answer
		EvaluationRequest request = createEvaluationRequest(TEST_STUDENT_ANSWER, TEST_FACTS);

		// Evaluate and verify
		EvaluationResponse response = evaluator.evaluate(request);
		assertThat(response.getScore()).isEqualTo(1.0f);
		assertThat(response.getFeedback()).isEqualTo("The answer is faithful to the facts.");
	}

	/**
	 * Test evaluation when the student answer contains fabricated information. Should
	 * return a failing evaluation with low score.
	 */
	@Test
	void testEvaluateUnfaithfulAnswer() {
		// Mock chat client to return a low score response
		mockChatResponse("{\"score\": 0.0, \"feedback\": \"The answer contains fabricated information.\"}");

		// Create evaluation request with unfaithful answer
		String unfaithfulAnswer = "The Earth is the third planet and has three moons.";
		EvaluationRequest request = createEvaluationRequest(unfaithfulAnswer, TEST_FACTS);

		// Evaluate and verify
		EvaluationResponse response = evaluator.evaluate(request);
		assertThat(response.getScore()).isEqualTo(0.0f);
		assertThat(response.getFeedback()).isEqualTo("The answer contains fabricated information.");
	}

	/**
	 * Test evaluation with empty facts. Should handle empty facts gracefully.
	 */
	@Test
	void testEvaluateWithEmptyFacts() {
		// Mock chat client response for empty facts
		mockChatResponse("{\"score\": 0.0, \"feedback\": \"No facts provided for evaluation.\"}");

		// Create evaluation request with empty facts
		EvaluationRequest request = createEvaluationRequest(TEST_STUDENT_ANSWER, "");

		// Evaluate and verify
		EvaluationResponse response = evaluator.evaluate(request);
		assertThat(response.getScore()).isEqualTo(0.0f);
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
		EvaluationRequest request = createEvaluationRequest(TEST_STUDENT_ANSWER, TEST_FACTS);

		// Evaluate and verify exception is thrown
		assertThatThrownBy(() -> evaluator.evaluate(request)).isInstanceOf(RuntimeException.class)
			.hasRootCauseInstanceOf(Exception.class);
	}

	/**
	 * Test the evaluator's name is correctly set.
	 */
	@Test
	void testEvaluatorName() {
		assertThat(evaluator.getName()).isEqualTo("faithfulness");
	}

	/**
	 * Helper method to create evaluation request
	 */
	private EvaluationRequest createEvaluationRequest(String studentAnswer, String facts) {
		Document document = new Document(facts);
		return new EvaluationRequest("", Collections.singletonList(document), studentAnswer);
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
