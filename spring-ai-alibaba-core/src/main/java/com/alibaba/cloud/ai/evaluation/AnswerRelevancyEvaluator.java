/*
 * All rights Reserved, Designed By Alibaba Group Inc.
 * Copyright: Copyright(C) 1999-2024
 * Company  : Alibaba Group Inc.
 */
package com.alibaba.cloud.ai.evaluation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.ai.evaluation.Evaluator;
import org.springframework.ai.model.Content;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Title Answer relevancy evaluator.<br>
 * Description Answer relevancy evaluator.<br>
 * Created at 2024-09-03 11:35
 *
 * @author yuanci.ytb
 * @version 1.0.0
 * @since jdk8
 */

public class AnswerRelevancyEvaluator extends LaajEvaluator {

	private static final String DEFAULT_EVALUATION_PROMPT_TEXT = """
			您是一名评测专家，能够基于提供的评分标准和内容信息进行评分。
			您将获得一个QUESTION, GROUND TRUTH (correct) ANSWER和STUDENT ANSWER。

			以下是评分标准：
			(1) 基于提供的GROUND TRUTH ANSWER作为正确基准答案，对STUDENT ANSWER的事实性、准确性和相关性进行评分。
			(2) 确保STUDENT ANSWER不包含任何冲突的陈述和内容。
			(3) 可以接受STUDENT ANSWER比GROUND TRUTH ANSWER包含更多的信息，只要对于GROUND TRUTH ANSWER保证事实性、准确性和相关性.

			Score:
			得分为1意味着STUDENT ANSWER满足所有标准。这是最高（最佳）得分。
			得分为0意味着STUDENT ANSWER没有满足所有标准。这是最低的得分。

			请逐步解释您的推理，以确保您的推理和结论正确。
			避免简单地陈述正确答案。

			最终答案按照标准的json格式输出, 比如:
			{"score": 0.7, "feedback": "GROUND TRUTH ANSWER与STUDENT ANSWER完全不相关。"}

			QUESTION: {question}
			GROUND TRUTH ANSWER: {correct_answer}
			STUDENT ANSWER: {student_answer}
			""";

	public AnswerRelevancyEvaluator(ChatClient.Builder chatClientBuilder) {
		super(chatClientBuilder);
	}

	public AnswerRelevancyEvaluator(ChatClient.Builder chatClientBuilder, String evaluationPromptText) {
		super(chatClientBuilder, evaluationPromptText);
	}

	public AnswerRelevancyEvaluator(ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper) {
		super(chatClientBuilder, objectMapper);
	}

	public AnswerRelevancyEvaluator(ChatClient.Builder chatClientBuilder, String evaluationPromptText,
			ObjectMapper objectMapper) {
		super(chatClientBuilder, evaluationPromptText, objectMapper);
	}

	@Override
	public EvaluationResponse evaluate(EvaluationRequest evaluationRequest) {
		var response = doGetResponse(evaluationRequest);
		var context = doGetSupportingData(evaluationRequest);

		String llmEvaluationResponse = getChatClientBuilder().build()
			.prompt()
			.user(userSpec -> userSpec.text(getEvaluationPromptText())
				.param("question", evaluationRequest.getUserText())
				.param("correct_answer", context)
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
		return "relevancy";
	}

}
