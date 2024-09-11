/*
 * All rights Reserved, Designed By Alibaba Group Inc.
 * Copyright: Copyright(C) 1999-2024
 * Company  : Alibaba Group Inc.
 */
package com.alibaba.cloud.ai.evaluation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.Evaluator;
import org.springframework.ai.model.Content;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Title LLM as a judge evaluator.<br>
 * Description LLM as a judge evaluator.<br>
 * Created at 2024-09-03 19:59
 *
 * @author yuanci.ytb
 * @version 1.0.0
 * @since jdk8
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

	protected String doGetSupportingData(EvaluationRequest evaluationRequest) {
		List<Content> data = evaluationRequest.getDataList();
		return data.stream()
			.filter(node -> node != null && node.getContent() != null)
			.map(Content::getContent)
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
