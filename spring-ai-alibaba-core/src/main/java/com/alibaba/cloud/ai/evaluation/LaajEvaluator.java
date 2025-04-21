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
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.Evaluator;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Title LLM as a judge evaluator.<br>
 * Description LLM as a judge evaluator.<br>
 *
 * @author yuanci.ytb
 * @since 1.0.0-M2
 */

public abstract class LaajEvaluator implements Evaluator {

	private ChatClient.Builder chatClientBuilder;

	private String evaluationPromptText;

	private ObjectMapper objectMapper;

	public LaajEvaluator(ChatClient.Builder chatClientBuilder) {
		this.chatClientBuilder = chatClientBuilder;
		this.evaluationPromptText = getDefaultEvaluationPrompt();
	}

	public LaajEvaluator(ChatClient.Builder chatClientBuilder, String evaluationPromptText) {
		this.chatClientBuilder = chatClientBuilder;
		this.evaluationPromptText = evaluationPromptText;
	}

	public LaajEvaluator(ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper) {
		this.chatClientBuilder = chatClientBuilder;
		this.objectMapper = objectMapper;
		this.evaluationPromptText = getDefaultEvaluationPrompt();
	}

	public LaajEvaluator(ChatClient.Builder chatClientBuilder, String evaluationPromptText, ObjectMapper objectMapper) {
		this.chatClientBuilder = chatClientBuilder;
		this.objectMapper = objectMapper;
		this.evaluationPromptText = evaluationPromptText;
	}

	protected String doGetResponse(EvaluationRequest evaluationRequest) {
		return evaluationRequest.getResponseContent();
	}

	@Override
	public String doGetSupportingData(EvaluationRequest evaluationRequest) {
		List<Document> data = evaluationRequest.getDataList();
		return data.stream()
			.filter(node -> node != null && node.getText() != null)
			.map(Document::getText)
			.collect(Collectors.joining(System.lineSeparator()));
	}

	protected abstract String getDefaultEvaluationPrompt();

	public abstract String getName();

	public ChatClient.Builder getChatClientBuilder() {
		return chatClientBuilder;
	}

	public String getEvaluationPromptText() {
		return evaluationPromptText;
	}

	public ObjectMapper getObjectMapper() {
		return objectMapper;
	}

}
