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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.alibaba.cloud.ai.advisor.DocumentRetrievalAdvisor;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetrievalAdvisor;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetriever;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetrieverOptions;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.context.TestPropertySource;

/**
 * Title React agent test cases.<br/>
 * Description React agent test cases.<br/>
 *
 * @author yuanci.ytb
 * @since 1.0.0-M2
 */

@TestPropertySource("classpath:application.yml")
@SpringBootTest(classes = EvaluationIT.class)
@EnableAutoConfiguration()
public class EvaluationIT {

	private static final Logger logger = LoggerFactory.getLogger(EvaluationIT.class);

	@Autowired
	private DashScopeChatModel dashscopeChatModel;

	@Autowired
	private DashScopeApi dashscopeApi;

	@Autowired
	private ObjectMapper objectMapper;

	@Value("classpath:/prompts/eval/correctness-evaluator.st")
	private Resource correctnessResource;

	@Value("classpath:/prompts/eval/qa-relevancy-evaluator.st")
	private Resource relevancyResource;

	@Value("classpath:/prompts/eval/qa-faithfulness-evaluator.st")
	private Resource faithfulnessResource;

	@Value("classpath:/prompts/rag/system-qa.st")
	private Resource systemQaResource;

	@Test
	void correctnessEvaluateTest() throws IOException {
		ChatClient chatClient = ChatClient.builder(dashscopeChatModel).build();

		String userText = "你是谁?";
		String expectedResult = "我是通义千问大模型.";

		ChatResponse response = chatClient.prompt().user(userText).call().chatResponse();
		String content = response.getResult().getOutput().getContent();
		Assertions.assertNotNull(content);

		AnswerCorrectnessEvaluator evaluator = new AnswerCorrectnessEvaluator(ChatClient.builder(dashscopeChatModel),
				correctnessResource.getContentAsString(StandardCharsets.UTF_8));
		EvaluationRequest evaluationRequest = new EvaluationRequest(userText,
				List.of(Document.builder().text(expectedResult).build()), content);
		EvaluationResponse evaluationResponse = evaluator.evaluate(evaluationRequest);

		Assertions.assertTrue(evaluationResponse.isPass());
		Assertions.assertEquals(1.0f, evaluationResponse.getScore());
	}

	@Test
	void relevanceEvaluateTest() throws IOException {
		DocumentRetriever retriever = new DashScopeDocumentRetriever(dashscopeApi,
				DashScopeDocumentRetrieverOptions.builder().withIndexName("spring-ai知识库").build());

		ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
			.defaultAdvisors(new DocumentRetrievalAdvisor(retriever,
					systemQaResource.getContentAsString(StandardCharsets.UTF_8)))
			.build();

		String userText = "如何使用阿里云百炼的prompt工程?";
		ChatResponse response = chatClient.prompt().user(userText).call().chatResponse();
		String content = response.getResult().getOutput().getContent();
		Assertions.assertNotNull(content);

		String expectedResult = "Prompt工程Prompt工程通过设计和改进prompt使大模型能够更准确、可靠地执行特定任务,平台为您提供了Prompt模板、Prompt优化等一系列Prompt工程工具。 Prompt模板 Prompt优化原始prompt 优化后prompt你的任务是将用户提供的中文词汇转换为遵循编程规范的变量名。请接收输入的中文字符串: S{name}, 并将 请将用户提供的中文词汇转换为遵循编程规范的变量名。接收输入的中文字符串为: S{name}, 请使用驼峰命其转换为合法、易读的编程变量名(使用驼峰命名法或下划线连接方式均可),同时确保输出的变量名不包含任 名法或下划线连接方式将其转换为合法、易读的编程变量名,并确保输出的变量名不包含任何保留字。";
		AnswerRelevancyEvaluator evaluator = new AnswerRelevancyEvaluator(ChatClient.builder(dashscopeChatModel),
				relevancyResource.getContentAsString(StandardCharsets.UTF_8), objectMapper);
		EvaluationRequest evaluationRequest = new EvaluationRequest(userText,
				List.of(Document.builder().text(expectedResult).build()), content);
		EvaluationResponse evaluationResponse = evaluator.evaluate(evaluationRequest);

		Assertions.assertTrue(evaluationResponse.isPass());
		Assertions.assertTrue(evaluationResponse.getScore() > 0.0f);

		logger.info("content: {}", content);

	}

	@Test
	void faithfulnessEvaluateTest() throws IOException {
		DocumentRetriever retriever = new DashScopeDocumentRetriever(dashscopeApi,
				DashScopeDocumentRetrieverOptions.builder().withIndexName("spring-ai知识库").build());

		ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
			.defaultAdvisors(new DocumentRetrievalAdvisor(retriever,
					systemQaResource.getContentAsString(StandardCharsets.UTF_8)))
			.build();

		String userText = "如何使用阿里云百炼的prompt工程?";
		ChatResponse response = chatClient.prompt().user(userText).call().chatResponse();
		String content = response.getResult().getOutput().getContent();
		Assertions.assertNotNull(content);

		List<Document> documents = response.getMetadata().get(DashScopeDocumentRetrievalAdvisor.RETRIEVED_DOCUMENTS);
		Assertions.assertNotNull(documents);

		AnswerFaithfulnessEvaluator evaluator = new AnswerFaithfulnessEvaluator(ChatClient.builder(dashscopeChatModel),
				faithfulnessResource.getContentAsString(StandardCharsets.UTF_8), objectMapper);
		EvaluationRequest evaluationRequest = new EvaluationRequest(userText, new ArrayList<>(documents), content);
		EvaluationResponse evaluationResponse = evaluator.evaluate(evaluationRequest);

		Assertions.assertTrue(evaluationResponse.isPass());
		Assertions.assertTrue(evaluationResponse.getScore() > 0.0f);
	}

}