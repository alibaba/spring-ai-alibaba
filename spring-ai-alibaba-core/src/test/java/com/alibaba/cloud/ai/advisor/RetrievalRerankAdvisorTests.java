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
package com.alibaba.cloud.ai.advisor;

import com.alibaba.cloud.ai.document.DocumentWithScore;
import com.alibaba.cloud.ai.model.RerankModel;
import com.alibaba.cloud.ai.model.RerankRequest;
import com.alibaba.cloud.ai.model.RerankResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.document.Document;
import org.springframework.ai.chat.client.advisor.api.AdvisedRequest;
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAroundAdvisorChain;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * RetrievalRerankAdvisor 的测试类
 *
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 * @author brianxiadong
 * @since 1.0.0-M5.1
 */
@ExtendWith(MockitoExtension.class)
class RetrievalRerankAdvisorTests {

	private static final String TEST_USER_TEXT = "什么是 Spring AI?";

	private static final String TEST_DOCUMENT_TEXT = "Spring AI 是一个用于构建 AI 应用的框架。";

	private static final Double TEST_MIN_SCORE = 0.5;

	@Mock
	private VectorStore vectorStore;

	@Mock
	private RerankModel rerankModel;

	@Mock
	private CallAroundAdvisorChain callChain;

	@Mock
	private StreamAroundAdvisorChain streamChain;

	@Mock
	private ChatModel chatModel;

	private RetrievalRerankAdvisor advisor;

	private Document testDocument;

	private AdvisedRequest testRequest;

	private AdvisedResponse testResponse;

	@BeforeEach
	void setUp() {
		// 使用默认设置初始化
		advisor = new RetrievalRerankAdvisor(vectorStore, rerankModel);

		// 设置测试文档
		testDocument = new Document(TEST_DOCUMENT_TEXT);

		// 设置测试请求
		testRequest = AdvisedRequest.builder()
			.userText(TEST_USER_TEXT)
			.userParams(new HashMap<>())
			.adviseContext(new HashMap<>())
			.chatModel(chatModel)
			.build();

		// 设置测试响应
		Generation generation = new Generation(new AssistantMessage(TEST_DOCUMENT_TEXT));
		ChatResponse chatResponse = new ChatResponse(List.of(generation));
		testResponse = new AdvisedResponse(chatResponse, new HashMap<>());
	}

	/**
	 * 测试默认构造函数
	 */
	@Test
	void testDefaultConstructor() {
		RetrievalRerankAdvisor advisor = new RetrievalRerankAdvisor(vectorStore, rerankModel);
		assertThat(advisor).isNotNull();
		assertThat(advisor.getName()).isEqualTo("RetrievalRerankAdvisor");
		assertThat(advisor.getOrder()).isZero();
	}

	/**
	 * 测试带有自定义分数的构造函数
	 */
	@Test
	void testConstructorWithScore() {
		RetrievalRerankAdvisor advisor = new RetrievalRerankAdvisor(vectorStore, rerankModel, TEST_MIN_SCORE);
		assertThat(advisor).isNotNull();
		assertThat(advisor.getName()).isEqualTo("RetrievalRerankAdvisor");
	}

	/**
	 * 测试带有搜索请求的构造函数
	 */
	@Test
	void testConstructorWithSearchRequest() {
		SearchRequest searchRequest = SearchRequest.builder().build();
		RetrievalRerankAdvisor advisor = new RetrievalRerankAdvisor(vectorStore, rerankModel, searchRequest);
		assertThat(advisor).isNotNull();
		assertThat(advisor.getName()).isEqualTo("RetrievalRerankAdvisor");
	}

	/**
	 * 测试成功检索和重排序文档的 aroundCall
	 */
	@Test
	void testAroundCallWithDocuments() {
		// 准备测试数据
		List<Document> documents = List.of(testDocument);
		DocumentWithScore documentWithScore = DocumentWithScore.builder()
			.withDocument(testDocument)
			.withScore(0.8)
			.build();

		// 创建包含文档列表的 adviseContext
		Map<String, Object> adviseContext = new HashMap<>();
		adviseContext.put(RetrievalRerankAdvisor.RETRIEVED_DOCUMENTS, documents);
		AdvisedResponse mockResponse = new AdvisedResponse(testResponse.response(), adviseContext);

		// 设置 Mock 行为
		when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(documents);
		when(rerankModel.call(any(RerankRequest.class))).thenReturn(new RerankResponse(List.of(documentWithScore)));
		when(callChain.nextAroundCall(any())).thenReturn(mockResponse);

		// 执行测试
		AdvisedResponse response = advisor.aroundCall(testRequest, callChain);

		// 验证响应
		assertThat(response).isNotNull();
		assertThat(response.response().getResults()).hasSize(1);
		assertThat(response.adviseContext()).containsKey(RetrievalRerankAdvisor.RETRIEVED_DOCUMENTS);
		assertThat(response.adviseContext().get(RetrievalRerankAdvisor.RETRIEVED_DOCUMENTS)).isNotNull()
			.isInstanceOf(List.class)
			.asList()
			.hasSize(1)
			.contains(testDocument);
	}

	/**
     * 测试空文档检索的 aroundCall
     */
    @Test
    void testAroundCallWithEmptyDocuments() {
        // 设置 Mock 行为返回空列表
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(Collections.emptyList());

        // 创建包含空文档列表的 adviseContext
        Map<String, Object> adviseContext = new HashMap<>();
        adviseContext.put(RetrievalRerankAdvisor.RETRIEVED_DOCUMENTS, Collections.emptyList());
        AdvisedResponse mockResponse = new AdvisedResponse(testResponse.response(), adviseContext);

        when(callChain.nextAroundCall(any())).thenReturn(mockResponse);

        // 执行测试
        AdvisedResponse response = advisor.aroundCall(testRequest, callChain);

        // 验证响应
        assertThat(response).isNotNull();
        assertThat(response.adviseContext()).containsKey(RetrievalRerankAdvisor.RETRIEVED_DOCUMENTS);
        assertThat(response.adviseContext().get(RetrievalRerankAdvisor.RETRIEVED_DOCUMENTS))
                .isNotNull()
                .isInstanceOf(List.class)
                .asList()
                .isEmpty();
    }

	/**
	 * 测试流式处理的 aroundStream
	 */
	@Test
	void testAroundStream() {
		// 准备测试数据
		List<Document> documents = List.of(testDocument);
		DocumentWithScore documentWithScore = DocumentWithScore.builder()
			.withDocument(testDocument)
			.withScore(0.8)
			.build();

		// 设置 Mock 行为
		when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(documents);
		when(rerankModel.call(any(RerankRequest.class))).thenReturn(new RerankResponse(List.of(documentWithScore)));
		when(streamChain.nextAroundStream(any())).thenReturn(Flux.just(testResponse));

		// 执行测试
		Flux<AdvisedResponse> responseFlux = advisor.aroundStream(testRequest, streamChain);

		// 验证响应
		StepVerifier.create(responseFlux).assertNext(response -> {
			assertThat(response).isNotNull();
			assertThat(response.response().getResults()).hasSize(1);
		}).verifyComplete();
	}

	/**
	 * 测试过滤表达式处理
	 */
	@Test
	void testFilterExpression() {
		// 准备测试数据
		Map<String, Object> context = new HashMap<>();
		context.put(RetrievalRerankAdvisor.FILTER_EXPRESSION, "type == 'test'");

		AdvisedRequest request = AdvisedRequest.builder()
			.userText(TEST_USER_TEXT)
			.adviseContext(context)
			.chatModel(chatModel)
			.build();

		// 创建包含过滤表达式的 adviseContext
		Map<String, Object> responseContext = new HashMap<>();
		responseContext.put(RetrievalRerankAdvisor.FILTER_EXPRESSION, "type == 'test'");
		responseContext.put(RetrievalRerankAdvisor.RETRIEVED_DOCUMENTS, List.of(testDocument));
		AdvisedResponse mockResponse = new AdvisedResponse(testResponse.response(), responseContext);

		// 设置 Mock 行为
		when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(testDocument));
		when(callChain.nextAroundCall(any())).thenReturn(mockResponse);

		// 执行测试
		AdvisedResponse response = advisor.aroundCall(request, callChain);

		// 验证响应
		assertThat(response).isNotNull();
		assertThat(response.adviseContext()).containsKey(RetrievalRerankAdvisor.FILTER_EXPRESSION);
		assertThat(response.adviseContext().get(RetrievalRerankAdvisor.FILTER_EXPRESSION)).isNotNull()
			.isEqualTo("type == 'test'");
	}

	/**
	 * 测试低于最小分数的文档过滤
	 */
	@Test
	void testScoreFiltering() {
		// 创建带有自定义最小分数的 advisor
		advisor = new RetrievalRerankAdvisor(vectorStore, rerankModel, 0.9);

		// 准备测试数据
		List<Document> documents = List.of(testDocument);
		DocumentWithScore documentWithScore = DocumentWithScore.builder()
			.withDocument(testDocument)
			.withScore(0.8) // 分数低于最小值
			.build();

		// 创建包含空文档列表的 adviseContext
		Map<String, Object> adviseContext = new HashMap<>();
		adviseContext.put(RetrievalRerankAdvisor.RETRIEVED_DOCUMENTS, Collections.emptyList());
		AdvisedResponse mockResponse = new AdvisedResponse(testResponse.response(), adviseContext);

		// 设置 Mock 行为
		when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(documents);
		when(rerankModel.call(any(RerankRequest.class))).thenReturn(new RerankResponse(List.of(documentWithScore)));
		when(callChain.nextAroundCall(any())).thenReturn(mockResponse);

		// 执行测试
		AdvisedResponse response = advisor.aroundCall(testRequest, callChain);

		// 验证响应 - 应该过滤掉低分文档
		assertThat(response).isNotNull();
		assertThat(response.adviseContext()).containsKey(RetrievalRerankAdvisor.RETRIEVED_DOCUMENTS);
		assertThat(response.adviseContext().get(RetrievalRerankAdvisor.RETRIEVED_DOCUMENTS)).isNotNull()
			.isInstanceOf(List.class)
			.asList()
			.isEmpty();
	}

}
