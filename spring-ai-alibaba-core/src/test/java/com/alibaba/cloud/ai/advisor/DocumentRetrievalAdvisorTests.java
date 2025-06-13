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
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
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
 * Test cases for DocumentRetrievalAdvisor. Tests cover constructor combinations,
 * aroundCall functionality, aroundStream behavior, and document retrieval processing.
 *
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 * @author brianxiadong
 * @since 1.0.0-M5.1
 */
@Disabled("1.0.0-RC1删除API，测试类需要重构")
@ExtendWith(MockitoExtension.class)
class DocumentRetrievalAdvisorTests {

	private static final String TEST_USER_TEXT = "What is Spring AI?";

	private static final String TEST_DOCUMENT_TEXT = "Spring AI is a framework for building AI applications.";

	private static final String TEST_CUSTOM_ADVISE = "Custom advise text";

	@Mock
	private DocumentRetriever documentRetriever;

	@Mock
	private CallAdvisorChain callChain;

	@Mock
	private StreamAdvisorChain streamChain;

	@Mock
	private ChatModel chatModel;

	private DocumentRetrievalAdvisor advisor;

	private Document testDocument;

	private ChatClientRequest testRequest;

	private ChatClientResponse testResponse;

	@BeforeEach
	void setUp() {
		// Initialize with default settings
		advisor = new DocumentRetrievalAdvisor(documentRetriever);

		// Setup test document
		testDocument = new Document(TEST_DOCUMENT_TEXT);

		// Setup test request with chatModel
		testRequest = ChatClientRequest.builder()
			.prompt(new Prompt(TEST_USER_TEXT, ChatOptions.builder().model(DashScopeApi.DEFAULT_CHAT_MODEL).build()))
			.build();

		// Setup test response
		Message message = new AssistantMessage(TEST_DOCUMENT_TEXT);
		Map<String, Object> metadata = new HashMap<>();
		metadata.put("finishReason", "stop");

		// 创建 Generation 和 ChatResponse
		Generation generation = new Generation(new AssistantMessage(TEST_DOCUMENT_TEXT));
		ChatResponse chatResponse = new ChatResponse(List.of(generation));
		testResponse = ChatClientResponse.builder().chatResponse(chatResponse).build();
	}

	/**
	 * Test constructor with default parameters
	 */
	@Test
	void testDefaultConstructor() {
		DocumentRetrievalAdvisor advisor = new DocumentRetrievalAdvisor(documentRetriever);
		assertThat(advisor).isNotNull();
		assertThat(advisor.getName()).isEqualTo("DocumentRetrievalAdvisor");
		assertThat(advisor.getOrder()).isZero();
	}

	/**
	 * Test constructor with custom user text advise
	 */
	@Test
	void testConstructorWithCustomAdvise() {
		DocumentRetrievalAdvisor advisor = new DocumentRetrievalAdvisor(documentRetriever,
				new PromptTemplate(TEST_CUSTOM_ADVISE));
		assertThat(advisor).isNotNull();
		assertThat(advisor.getName()).isEqualTo("DocumentRetrievalAdvisor");
	}

	/**
	 * Test aroundCall with successful document retrieval
	 */
	@Test
	void testAroundCallWithDocuments() {
		// 准备测试数据
		Map<String, Object> metadata = new HashMap<>();
		metadata.put("id", "test-id");
		metadata.put("model", "test-model");
		metadata.put("finishReason", "stop");

		// 创建 Generation 和 ChatResponse
		Generation generation = new Generation(new AssistantMessage(TEST_DOCUMENT_TEXT));
		ChatResponse chatResponse = new ChatResponse(List.of(generation));

		// 创建包含检索文档的 adviseContext
		Map<String, Object> adviseContext = new HashMap<>();
		adviseContext.put(DocumentRetrievalAdvisor.RETRIEVED_DOCUMENTS, List.of(testDocument));
		ChatClientResponse advisedResponse = ChatClientResponse.builder()
			.chatResponse(chatResponse)
			.context(adviseContext)
			.build();

		// Mock behavior
		when(documentRetriever.retrieve(any())).thenReturn(List.of(testDocument));
		when(callChain.nextCall(any())).thenReturn(advisedResponse);

		// Execute test
		ChatClientResponse response = advisor.adviseCall(testRequest, callChain);

		// Verify response
		assertThat(response).isNotNull();
		assertThat(response.chatResponse().getResults()).hasSize(1);

		Object retrievedDocs = response.context().get(DocumentRetrievalAdvisor.RETRIEVED_DOCUMENTS);
		assertThat(retrievedDocs).isInstanceOf(List.class);

		@SuppressWarnings("unchecked")
		List<Document> documents = (List<Document>) retrievedDocs;
		assertThat(documents).hasSize(1).first().isEqualTo(testDocument);
	}

	/**
	 * Test aroundCall with empty document retrieval
	 */
	@Test
	void testAroundCallWithEmptyDocuments() {
		// Setup document retriever mock to return empty list
		when(documentRetriever.retrieve(any(Query.class))).thenReturn(Collections.emptyList());

		// Mock the response to include empty documents list in adviseContext
		Map<String, Object> adviseContext = new HashMap<>();
		adviseContext.put(DocumentRetrievalAdvisor.RETRIEVED_DOCUMENTS, Collections.emptyList());
		ChatClientResponse mockResponse = ChatClientResponse.builder()
			.chatResponse(testResponse.chatResponse())
			.context(adviseContext)
			.build();
		when(callChain.nextCall(any(ChatClientRequest.class))).thenReturn(mockResponse);

		// Execute aroundCall
		ChatClientResponse response = advisor.adviseCall(testRequest, callChain);

		// Verify response
		assertThat(response).isNotNull();
		assertThat(response.context().get(DocumentRetrievalAdvisor.RETRIEVED_DOCUMENTS))
			.isEqualTo(Collections.emptyList());
	}

	/**
	 * Test aroundStream with successful document retrieval
	 */
	@Test
	void testAroundStream() {
		// 准备测试数据
		Map<String, Object> metadata = new HashMap<>();
		metadata.put("id", "test-id");
		metadata.put("model", "test-model");
		metadata.put("finishReason", "stop");

		// 创建 Generation 和 ChatResponse
		Generation generation = new Generation(new AssistantMessage(TEST_DOCUMENT_TEXT));
		ChatResponse chatResponse = new ChatResponse(List.of(generation));

		// 创建包含检索文档的 adviseContext
		Map<String, Object> adviseContext = new HashMap<>();
		adviseContext.put(DocumentRetrievalAdvisor.RETRIEVED_DOCUMENTS, List.of(testDocument));
		ChatClientResponse advisedResponse = ChatClientResponse.builder()
			.chatResponse(chatResponse)
			.context(adviseContext)
			.build();

		// Mock behavior
		when(documentRetriever.retrieve(any())).thenReturn(List.of(testDocument));
		when(streamChain.nextStream(any())).thenReturn(Flux.just(advisedResponse));

		// Execute test
		Flux<ChatClientResponse> responseFlux = advisor.adviseStream(testRequest, streamChain);

		// Verify response
		StepVerifier.create(responseFlux).assertNext(response -> {
			assertThat(response).isNotNull();
			assertThat(response.chatResponse().getResults()).hasSize(1);

			Object retrievedDocs = response.context().get(DocumentRetrievalAdvisor.RETRIEVED_DOCUMENTS);
			assertThat(retrievedDocs).isInstanceOf(List.class);

			@SuppressWarnings("unchecked")
			List<Document> documents = (List<Document>) retrievedDocs;
			assertThat(documents).hasSize(1).first().isEqualTo(testDocument);
		}).verifyComplete();
	}

	/**
	 * Test metadata handling in after processing
	 */
	@Test
	void testMetadataHandling() {
		// 准备测试数据
		Map<String, Object> metadata = new HashMap<>();
		metadata.put("id", "test-id");
		metadata.put("model", "test-model");
		metadata.put("custom-key", "custom-value");
		metadata.put("finishReason", "stop");

		// 创建带有元数据的 Generation 和 ChatResponse
		Generation generation = new Generation(new AssistantMessage(TEST_DOCUMENT_TEXT));
		ChatResponse chatResponse = new ChatResponse(List.of(generation));

		// 创建包含检索文档的 adviseContext
		Map<String, Object> adviseContext = new HashMap<>();
		adviseContext.put(DocumentRetrievalAdvisor.RETRIEVED_DOCUMENTS, List.of(testDocument));
		adviseContext.putAll(metadata); // 将元数据添加到 adviseContext 中
		ChatClientResponse advisedResponse = ChatClientResponse.builder()
			.chatResponse(chatResponse)
			.context(adviseContext)
			.build();

		// Mock behavior
		when(documentRetriever.retrieve(any())).thenReturn(List.of(testDocument));
		when(callChain.nextCall(any())).thenReturn(advisedResponse);

		// Execute test
		ChatClientResponse response = advisor.adviseCall(testRequest, callChain);

		// Verify response
		assertThat(response).isNotNull();
		assertThat(response.context()).isNotNull();
		assertThat(response.context().get("id")).isEqualTo("test-id");
		assertThat(response.context().get("model")).isEqualTo("test-model");
		assertThat(response.context().get("custom-key")).isEqualTo("custom-value");
		assertThat(response.context().get("finishReason")).isEqualTo("stop");
	}

	/**
	 * Test user text advise formatting
	 */
	@Test
	void testUserTextAdviseFormatting() {
		// Setup document retriever mock
		when(documentRetriever.retrieve(any(Query.class))).thenReturn(List.of(testDocument));
		when(callChain.nextCall(any(ChatClientRequest.class))).thenReturn(testResponse);

		// Execute aroundCall
		advisor.adviseCall(testRequest, callChain);

		// Verify that the user text is properly formatted with the advise text
		assertThat(testRequest.prompt().getUserMessage().getText()).contains(TEST_USER_TEXT);
	}

}
