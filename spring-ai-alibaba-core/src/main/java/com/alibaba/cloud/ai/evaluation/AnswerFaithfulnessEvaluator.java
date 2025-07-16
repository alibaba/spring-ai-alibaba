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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;

import java.util.Collections;

/**
 * Title Answer relevancy evaluator.<br>
 * Description Answer relevancy evaluator.<br>
 *
 * @author yuanci.ytb
 * @since 1.0.0-M2
 */

public class AnswerFaithfulnessEvaluator extends LaajEvaluator {

	private static final String DEFAULT_EVALUATION_PROMPT_TEXT = """
			您是一名评测专家，能够基于提供的评分标准和内容信息进行评分。
			您将获得一些FACTS(事实内容)和STUDENT ANSWER。

			以下是评分标准：
			(1) 确保STUDENT ANSWER的内容是基于FACTS的事实内容，不能随意编纂。
			(2) 确保STUDENT ANSWER的内容没有超出FACTS的内容范围外的虚假信息。

			Score:
			得分为1意味着STUDENT ANSWER满足所有标准。这是最高（最佳）得分。
			得分为0意味着STUDENT ANSWER没有满足所有标准。这是最低的得分。

			请逐步解释您的推理，以确保您的推理和结论正确，避免简单地陈述正确答案。

			最终答案按照标准的json格式输出,不要使用markdown的格式, 比如:
			\\{"score": 0.7, "feedback": "STUDENT ANSWER的内容超出了FACTS的事实内容。"\\}

			FACTS: {context}
			STUDENT ANSWER: {student_answer}
			""";

	public AnswerFaithfulnessEvaluator(ChatClient.Builder chatClientBuilder) {
		super(chatClientBuilder);
	}

	public AnswerFaithfulnessEvaluator(ChatClient.Builder chatClientBuilder, String evaluationPromptText) {
		super(chatClientBuilder, evaluationPromptText);
	}

	public AnswerFaithfulnessEvaluator(ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper) {
		super(chatClientBuilder, objectMapper);
	}

	public AnswerFaithfulnessEvaluator(ChatClient.Builder chatClientBuilder, String evaluationPromptText,
			ObjectMapper objectMapper) {
		super(chatClientBuilder, evaluationPromptText, objectMapper);
	}

	@Override
	public EvaluationResponse evaluate(EvaluationRequest evaluationRequest) {
		if (evaluationRequest == null) {
			throw new IllegalArgumentException("EvaluationRequest must not be null");
		}
		var response = doGetResponse(evaluationRequest);
		var context = doGetSupportingData(evaluationRequest);

		String llmEvaluationResponse = getChatClientBuilder().build()
			.prompt()
			.user(userSpec -> userSpec.text(getEvaluationPromptText())
				.param("context", context)
				.param("student_answer", response))
			.call()
			.content();

		JsonNode evaluationResponse = null;
		try {
			evaluationResponse = getObjectMapper().readTree(llmEvaluationResponse);
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}

		float score = (float) evaluationResponse.get("score").asDouble();
		String feedback = evaluationResponse.get("feedback").asText();
		boolean passing = score > 0;
		return new EvaluationResponse(passing, score, feedback, Collections.emptyMap());
	}

	@Override
	protected String getDefaultEvaluationPrompt() {
		return DEFAULT_EVALUATION_PROMPT_TEXT;
	}

	@Override
	public String getName() {
		return "faithfulness";
	}

}
