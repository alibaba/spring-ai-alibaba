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

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.document.DocumentWithScore;
import com.alibaba.cloud.ai.model.RerankModel;
import com.alibaba.cloud.ai.model.RerankRequest;
import com.alibaba.cloud.ai.model.RerankResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
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
 * Test class for RetrievalRerankAdvisor
 *
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 * @author brianxiadong
 * @since 1.0.0-M5.1
 */
@Disabled("1.0.0-RC1 API removed, test class needs refactoring")
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
	private CallAdvisorChain callChain;

	@Mock
	private StreamAdvisorChain streamChain;

	@Mock
	private ChatModel chatModel;

	private RetrievalRerankAdvisor advisor;

	private Document testDocument;

	private ChatClientRequest testRequest;

	private ChatClientResponse testResponse;

	@BeforeEach
	void setUp() {
		// Initialize with default settings
		advisor = new RetrievalRerankAdvisor(vectorStore, rerankModel);

		// Set test documents
		testDocument = new Document(TEST_DOCUMENT_TEXT);

		// Set test request
		testRequest = ChatClientRequest.builder()
			.prompt(new Prompt(TEST_USER_TEXT, ChatOptions.builder().model(DashScopeApi.DEFAULT_CHAT_MODEL).build()))

			.build();

		// Set test response
		Generation generation = new Generation(new AssistantMessage(TEST_DOCUMENT_TEXT));
		ChatResponse chatResponse = new ChatResponse(List.of(generation));
		testResponse = ChatClientResponse.builder().chatResponse(chatResponse).build();
	}

	/**
	 * Test default constructor
	 */
	@Test
	void testDefaultConstructor() {
		RetrievalRerankAdvisor advisor = new RetrievalRerankAdvisor(vectorStore, rerankModel);
		assertThat(advisor).isNotNull();
		assertThat(advisor.getName()).isEqualTo("RetrievalRerankAdvisor");
		assertThat(advisor.getOrder()).isZero();
	}

	/**
	 * Test constructor with custom score
	 */
	@Test
	void testConstructorWithScore() {
		RetrievalRerankAdvisor advisor = new RetrievalRerankAdvisor(vectorStore, rerankModel, TEST_MIN_SCORE);
		assertThat(advisor).isNotNull();
		assertThat(advisor.getName()).isEqualTo("RetrievalRerankAdvisor");
	}

	/**
	 * Test constructor with search request
	 */
	@Test
	void testConstructorWithSearchRequest() {
		SearchRequest searchRequest = SearchRequest.builder().build();
		RetrievalRerankAdvisor advisor = new RetrievalRerankAdvisor(vectorStore, rerankModel, searchRequest);
		assertThat(advisor).isNotNull();
		assertThat(advisor.getName()).isEqualTo("RetrievalRerankAdvisor");
	}

	/**
	 * Test successful document retrieval and reranking aroundCall
	 */
	@Test
	void testAroundCallWithDocuments() {
		// Prepare test data
		List<Document> documents = List.of(testDocument);
		DocumentWithScore documentWithScore = DocumentWithScore.builder()
			.withDocument(testDocument)
			.withScore(0.8)
			.build();

		// Create adviseContext containing document list
		Map<String, Object> adviseContext = new HashMap<>();
		adviseContext.put(RetrievalRerankAdvisor.RETRIEVED_DOCUMENTS, documents);
		ChatClientResponse mockResponse = ChatClientResponse.builder()
			.chatResponse(testResponse.chatResponse())
			.context(adviseContext)
			.build();

		// Set Mock behavior
		when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(documents);
		when(rerankModel.call(any(RerankRequest.class))).thenReturn(new RerankResponse(List.of(documentWithScore)));
		when(callChain.nextCall(any())).thenReturn(mockResponse);

		// Execute test
		ChatClientResponse response = advisor.adviseCall(testRequest, callChain);

		// Verify response
		assertThat(response).isNotNull();
		assertThat(response.chatResponse().getResults()).hasSize(1);
		assertThat(response.context()).containsKey(RetrievalRerankAdvisor.RETRIEVED_DOCUMENTS);
		assertThat(response.context().get(RetrievalRerankAdvisor.RETRIEVED_DOCUMENTS)).isNotNull()
			.isInstanceOf(List.class)
			.asList()
			.hasSize(1)
			.contains(testDocument);
	}

	/**
	 * Test aroundCall with empty document retrieval
	 */
	@Test
	void testAroundCallWithEmptyDocuments() {
		// Set Mock behavior to return empty list
		when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(Collections.emptyList());

		// Create adviseContext containing empty document list
		Map<String, Object> adviseContext = new HashMap<>();
		adviseContext.put(RetrievalRerankAdvisor.RETRIEVED_DOCUMENTS, Collections.emptyList());
		ChatClientResponse mockResponse = ChatClientResponse.builder()
			.chatResponse(testResponse.chatResponse())
			.context(adviseContext)
			.build();

		when(callChain.nextCall(any())).thenReturn(mockResponse);

		// Execute test
		ChatClientResponse response = advisor.adviseCall(testRequest, callChain);

		// Verify response
		assertThat(response).isNotNull();
		assertThat(response.context()).containsKey(RetrievalRerankAdvisor.RETRIEVED_DOCUMENTS);
		assertThat(response.context().get(RetrievalRerankAdvisor.RETRIEVED_DOCUMENTS)).isNotNull()
			.isInstanceOf(List.class)
			.asList()
			.isEmpty();
	}

	/**
	 * Test streaming aroundStream
	 */
	@Test
	void testAroundStream() {
		// Prepare test data
		List<Document> documents = List.of(testDocument);
		DocumentWithScore documentWithScore = DocumentWithScore.builder()
			.withDocument(testDocument)
			.withScore(0.8)
			.build();

		// Set Mock behavior
		when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(documents);
		when(rerankModel.call(any(RerankRequest.class))).thenReturn(new RerankResponse(List.of(documentWithScore)));
		when(streamChain.nextStream(any())).thenReturn(Flux.just(testResponse));

		// Execute test
		Flux<ChatClientResponse> responseFlux = advisor.adviseStream(testRequest, streamChain);

		// Verify response
		StepVerifier.create(responseFlux).assertNext(response -> {
			assertThat(response).isNotNull();
			assertThat(response.chatResponse().getResults()).hasSize(1);
		}).verifyComplete();
	}

	/**
	 * Test filter expression handling
	 */
	@Test
	void testFilterExpression() {
		// Prepare test data
		Map<String, Object> context = new HashMap<>();
		context.put(RetrievalRerankAdvisor.FILTER_EXPRESSION, "type == 'test'");

		ChatClientRequest request = ChatClientRequest.builder()
			.prompt(new Prompt(TEST_USER_TEXT, ChatOptions.builder().model(DashScopeApi.DEFAULT_CHAT_MODEL).build()))
			.context(context)
			.build();

		// Create adviseContext containing filter expression
		Map<String, Object> responseContext = new HashMap<>();
		responseContext.put(RetrievalRerankAdvisor.FILTER_EXPRESSION, "type == 'test'");
		responseContext.put(RetrievalRerankAdvisor.RETRIEVED_DOCUMENTS, List.of(testDocument));
		ChatClientResponse mockResponse = ChatClientResponse.builder()
			.chatResponse(testResponse.chatResponse())
			.context(responseContext)
			.build();

		// Set Mock behavior
		when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(testDocument));
		when(callChain.nextCall(any())).thenReturn(mockResponse);

		// Execute test
		ChatClientResponse response = advisor.adviseCall(request, callChain);

		// Verify response
		assertThat(response).isNotNull();
		assertThat(response.context()).containsKey(RetrievalRerankAdvisor.FILTER_EXPRESSION);
		assertThat(response.context().get(RetrievalRerankAdvisor.FILTER_EXPRESSION)).isNotNull()
			.isEqualTo("type == 'test'");
	}

	/**
	 * Test filtering documents below minimum score
	 */
	@Test
	void testScoreFiltering() {
		// Create advisor with custom minimum score
		advisor = new RetrievalRerankAdvisor(vectorStore, rerankModel, 0.9);

		// Prepare test data
		List<Document> documents = List.of(testDocument);
		DocumentWithScore documentWithScore = DocumentWithScore.builder()
			.withDocument(testDocument)
			.withScore(0.8) // Score below minimum value
			.build();

		// Create adviseContext containing empty document list
		Map<String, Object> adviseContext = new HashMap<>();
		adviseContext.put(RetrievalRerankAdvisor.RETRIEVED_DOCUMENTS, Collections.emptyList());
		ChatClientResponse mockResponse = ChatClientResponse.builder()
			.chatResponse(testResponse.chatResponse())
			.context(adviseContext)
			.build();

		// Set Mock behavior
		when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(documents);
		when(rerankModel.call(any(RerankRequest.class))).thenReturn(new RerankResponse(List.of(documentWithScore)));
		when(callChain.nextCall(any())).thenReturn(mockResponse);

		// Execute test
		ChatClientResponse response = advisor.adviseCall(testRequest, callChain);

		// Verify response - should filter out low-score documents
		assertThat(response).isNotNull();
		assertThat(response.context()).containsKey(RetrievalRerankAdvisor.RETRIEVED_DOCUMENTS);
		assertThat(response.context().get(RetrievalRerankAdvisor.RETRIEVED_DOCUMENTS)).isNotNull()
			.isInstanceOf(List.class)
			.asList()
			.isEmpty();
	}

}
