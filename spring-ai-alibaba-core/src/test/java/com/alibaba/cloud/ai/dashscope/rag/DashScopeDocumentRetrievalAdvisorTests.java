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
package com.alibaba.cloud.ai.dashscope.rag;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.ChatCompletionFinishReason;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ai.chat.client.advisor.api.AdvisedRequest;
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisorChain;
import org.springframework.ai.chat.model.ChatModel;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Test cases for DashScopeDocumentRetrievalAdvisor. Tests cover document retrieval,
 * prompt generation, and error handling scenarios.
 *
 * @author kevinlin09
 * @author brianxiadong
 * @since 1.0.0-M5.1
 */
class DashScopeDocumentRetrievalAdvisorTests {

	private static final String TEST_CATEGORY_ID = "test-category";

	private static final String TEST_FILE_ID = "test-file-id";

	private static final String TEST_CONTENT = "Test content";

	private static final String TEST_FILE_NAME = "test.txt";

	private static final String TEST_QUERY = "test query";

	@Mock
	private DocumentRetriever documentRetriever;

	@Mock
	private ChatModel chatModel;

	@TempDir
	Path tempDir;

	private DashScopeDocumentRetrievalAdvisor advisor;

	private File testFile;

	@BeforeEach
	void setUp() throws IOException {
		// Initialize mocks and test objects
		MockitoAnnotations.openMocks(this);

		// Create test file
		testFile = tempDir.resolve(TEST_FILE_NAME).toFile();
		Files.writeString(testFile.toPath(), TEST_CONTENT);

		// Set up advisor with document retriever and enable reference
		advisor = new DashScopeDocumentRetrievalAdvisor(documentRetriever, true);
	}

	@Test
	void testGeneratePromptWithDocuments() {
		// Prepare test data
		List<Document> documents = List.of(new Document(TEST_CONTENT));
		when(documentRetriever.retrieve(any(Query.class))).thenReturn(documents);

		// Generate prompt
		Map<String, Object> userParams = new HashMap<>();
		Map<String, Object> adviseContext = new HashMap<>();
		AdvisedRequest request = AdvisedRequest.builder()
			.userText(TEST_QUERY)
			.userParams(userParams)
			.adviseContext(adviseContext)
			.chatModel(chatModel)
			.build();

		// Create a valid ChatResponse with Generation and metadata
		Map<String, Object> metadata = new HashMap<>();
		metadata.put("finishReason", ChatCompletionFinishReason.STOP.name());
		AssistantMessage assistantMessage = new AssistantMessage("Test response", metadata);
		ChatGenerationMetadata generationMetadata = ChatGenerationMetadata.builder()
			.finishReason(ChatCompletionFinishReason.STOP.name())
			.build();
		Generation generation = new Generation(assistantMessage, generationMetadata);
		ChatResponse chatResponse = new ChatResponse(List.of(generation));

		// Create adviseContext with document map
		Map<String, Object> responseAdviseContext = new HashMap<>();
		Map<String, Document> documentMap = new HashMap<>();
		documentMap.put("[1]", documents.get(0));
		responseAdviseContext.put(DashScopeDocumentRetrievalAdvisor.RETRIEVED_DOCUMENTS, documentMap);

		// Create question_answer_context as a Map
		Map<String, String> qaContext = new HashMap<>();
		qaContext.put("context",
				String.format("[1] 【文档名】%s\n【标题】%s\n【正文】%s\n\n",
						documents.get(0).getMetadata().getOrDefault("doc_name", ""),
						documents.get(0).getMetadata().getOrDefault("title", ""), documents.get(0).getText()));
		responseAdviseContext.put("question_answer_context", qaContext);

		AdvisedResponse response = advisor.aroundCall(request,
				chain -> new AdvisedResponse(chatResponse, responseAdviseContext));

		// Verify response
		assertThat(response).isNotNull();
		assertThat(response.adviseContext()).containsKey(DashScopeDocumentRetrievalAdvisor.RETRIEVED_DOCUMENTS);
		assertThat(response.adviseContext()).containsKey("question_answer_context");
	}

	@Test
    void testGeneratePromptWithEmptyDocuments() {
        // Prepare test data
        when(documentRetriever.retrieve(any(Query.class))).thenReturn(Collections.emptyList());

        // Generate prompt
        Map<String, Object> userParams = new HashMap<>();
        Map<String, Object> adviseContext = new HashMap<>();
        AdvisedRequest request = AdvisedRequest.builder()
                .userText(TEST_QUERY)
                .userParams(userParams)
                .adviseContext(adviseContext)
                .chatModel(chatModel)
                .build();

        // Create a valid ChatResponse with Generation and metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("finishReason", ChatCompletionFinishReason.STOP.name());
        AssistantMessage assistantMessage = new AssistantMessage("Test response", metadata);
        ChatGenerationMetadata generationMetadata = ChatGenerationMetadata.builder()
                .finishReason(ChatCompletionFinishReason.STOP.name())
                .build();
        Generation generation = new Generation(assistantMessage, generationMetadata);
        ChatResponse chatResponse = new ChatResponse(List.of(generation));

        // Create adviseContext with empty document map
        Map<String, Object> responseAdviseContext = new HashMap<>();
        Map<String, Document> documentMap = new HashMap<>();
        responseAdviseContext.put(DashScopeDocumentRetrievalAdvisor.RETRIEVED_DOCUMENTS, documentMap);

        AdvisedResponse response = advisor.aroundCall(request,
                chain -> new AdvisedResponse(chatResponse, responseAdviseContext));

        // Verify response
        assertThat(response).isNotNull();
        assertThat(response.adviseContext()).containsKey(DashScopeDocumentRetrievalAdvisor.RETRIEVED_DOCUMENTS);
        assertThat((Map<?, ?>) response.adviseContext().get(DashScopeDocumentRetrievalAdvisor.RETRIEVED_DOCUMENTS))
                .isEmpty();
    }

	@Test
	void testProcessChatResponse() {
		// Prepare test data
		List<Document> documents = List.of(new Document(TEST_CONTENT));
		when(documentRetriever.retrieve(any(Query.class))).thenReturn(documents);

		// Create chat response with metadata
		Map<String, Object> metadata = new HashMap<>();
		metadata.put("finishReason", ChatCompletionFinishReason.STOP.name());
		AssistantMessage assistantMessage = new AssistantMessage("Test response", metadata);
		ChatGenerationMetadata generationMetadata = ChatGenerationMetadata.builder()
			.finishReason(ChatCompletionFinishReason.STOP.name())
			.build();
		Generation generation = new Generation(assistantMessage, generationMetadata);
		ChatResponse chatResponse = new ChatResponse(List.of(generation));

		// Process response
		Map<String, Object> userParams = new HashMap<>();
		Map<String, Object> adviseContext = new HashMap<>();
		AdvisedRequest request = AdvisedRequest.builder()
			.userText(TEST_QUERY)
			.userParams(userParams)
			.adviseContext(adviseContext)
			.chatModel(chatModel)
			.build();
		AdvisedResponse response = advisor.aroundCall(request,
				chain -> new AdvisedResponse(chatResponse, new HashMap<>()));

		// Verify response
		assertThat(response).isNotNull();
		assertThat(response.response()).isNotNull();
		assertThat(response.response().getResult().getOutput().getText()).isEqualTo("Test response");
	}

	@Test
	void testAroundCallWithEmptyDocuments() {
		// Given
		DashScopeDocumentRetrievalAdvisor advisor = new DashScopeDocumentRetrievalAdvisor(documentRetriever, true);
		AdvisedRequest request = AdvisedRequest.builder().userText("test message").chatModel(chatModel).build();

		// When
		when(documentRetriever.retrieve(any(Query.class))).thenReturn(Collections.emptyList());

		// Create a valid ChatResponse with Generation and metadata
		Map<String, Object> metadata = new HashMap<>();
		metadata.put("finishReason", ChatCompletionFinishReason.STOP.name());
		AssistantMessage assistantMessage = new AssistantMessage("Test response", metadata);
		ChatGenerationMetadata generationMetadata = ChatGenerationMetadata.builder()
			.finishReason(ChatCompletionFinishReason.STOP.name())
			.build();
		Generation generation = new Generation(assistantMessage, generationMetadata);
		ChatResponse chatResponse = new ChatResponse(List.of(generation));

		// Create adviseContext with retrieved documents
		Map<String, Object> adviseContext = new HashMap<>();
		Map<String, Document> documentMap = new HashMap<>();
		adviseContext.put(DashScopeDocumentRetrievalAdvisor.RETRIEVED_DOCUMENTS, documentMap);

		AdvisedResponse response = advisor.aroundCall(request,
				chain -> new AdvisedResponse(chatResponse, adviseContext));

		// Then
		assertThat(response).isNotNull();
		assertThat(response.response()).isNotNull();
		assertThat(response.adviseContext()).containsKey(DashScopeDocumentRetrievalAdvisor.RETRIEVED_DOCUMENTS);
	}

	@Test
	void testAroundCallWithNullDocuments() {
		// Given
		DashScopeDocumentRetrievalAdvisor advisor = new DashScopeDocumentRetrievalAdvisor(documentRetriever, true);
		AdvisedRequest request = AdvisedRequest.builder().userText("test message").chatModel(chatModel).build();

		// When
		when(documentRetriever.retrieve(any(Query.class))).thenReturn(Collections.emptyList());

		// Create a valid ChatResponse with Generation and metadata
		Map<String, Object> metadata = new HashMap<>();
		metadata.put("finishReason", ChatCompletionFinishReason.STOP.name());
		AssistantMessage assistantMessage = new AssistantMessage("Test response", metadata);
		ChatGenerationMetadata generationMetadata = ChatGenerationMetadata.builder()
			.finishReason(ChatCompletionFinishReason.STOP.name())
			.build();
		Generation generation = new Generation(assistantMessage, generationMetadata);
		ChatResponse chatResponse = new ChatResponse(List.of(generation));

		// Create adviseContext with retrieved documents
		Map<String, Object> adviseContext = new HashMap<>();
		Map<String, Document> documentMap = new HashMap<>();
		adviseContext.put(DashScopeDocumentRetrievalAdvisor.RETRIEVED_DOCUMENTS, documentMap);

		AdvisedResponse response = advisor.aroundCall(request,
				chain -> new AdvisedResponse(chatResponse, adviseContext));

		// Then
		assertThat(response).isNotNull();
		assertThat(response.response()).isNotNull();
		assertThat(response.adviseContext()).containsKey(DashScopeDocumentRetrievalAdvisor.RETRIEVED_DOCUMENTS);
		assertThat((Map<?, ?>) response.adviseContext().get(DashScopeDocumentRetrievalAdvisor.RETRIEVED_DOCUMENTS))
			.isEmpty();
	}

	@Test
	void testAroundCallWithDocuments() {
		// Given
		DashScopeDocumentRetrievalAdvisor advisor = new DashScopeDocumentRetrievalAdvisor(documentRetriever, true);
		AdvisedRequest request = AdvisedRequest.builder().userText("test message").chatModel(chatModel).build();

		// When
		List<Document> documents = Arrays.asList(new Document("test document 1"), new Document("test document 2"));
		when(documentRetriever.retrieve(any(Query.class))).thenReturn(documents);

		// Create a valid ChatResponse with Generation and metadata
		Map<String, Object> metadata = new HashMap<>();
		metadata.put("finishReason", ChatCompletionFinishReason.STOP.name());
		AssistantMessage assistantMessage = new AssistantMessage("Test response", metadata);
		ChatGenerationMetadata generationMetadata = ChatGenerationMetadata.builder()
			.finishReason(ChatCompletionFinishReason.STOP.name())
			.build();
		Generation generation = new Generation(assistantMessage, generationMetadata);
		ChatResponse chatResponse = new ChatResponse(List.of(generation));

		// Create adviseContext with retrieved documents
		Map<String, Object> adviseContext = new HashMap<>();
		Map<String, Document> documentMap = new HashMap<>();
		for (int i = 0; i < documents.size(); i++) {
			documentMap.put(String.format("[%d]", i + 1), documents.get(i));
		}
		adviseContext.put(DashScopeDocumentRetrievalAdvisor.RETRIEVED_DOCUMENTS, documentMap);

		AdvisedResponse response = advisor.aroundCall(request,
				chain -> new AdvisedResponse(chatResponse, adviseContext));

		// Then
		assertThat(response).isNotNull();
		assertThat(response.response()).isNotNull();
		assertThat(response.adviseContext()).containsKey(DashScopeDocumentRetrievalAdvisor.RETRIEVED_DOCUMENTS);
	}

}
