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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.ApiKey;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DashScopeDocumentAnalysisAdvisorTest {

	private DashScopeDocumentAnalysisAdvisor advisor;

	@BeforeEach
	void setUp() {
		ApiKey apiKey = new SimpleApiKey("test-api-key");
		advisor = new DashScopeDocumentAnalysisAdvisor(apiKey);
	}

	@Test
	void constructor_withValidApiKey_initializesSuccessfully() {
		ApiKey apiKey = new SimpleApiKey("test-api-key");
		DashScopeDocumentAnalysisAdvisor testAdvisor = new DashScopeDocumentAnalysisAdvisor(apiKey);
		assertNotNull(testAdvisor);
	}

	@Test
	void constructor_withEmptyApiKey_throwsException() {
		assertThrows(IllegalArgumentException.class, () -> new DashScopeDocumentAnalysisAdvisor(null));
	}

	@Test
	void before_withNullResource_returnsOriginalRequest() {
		ChatClientRequest chatClientRequest = ChatClientRequest.builder().prompt(new Prompt()).build();

		ChatClientRequest result = advisor.before(chatClientRequest, null);

		assertEquals(chatClientRequest, result);
	}

	@Test
	void after_withNullChatResponse_buildsNewResponse() {
		ChatClientResponse chatClientResponse = ChatClientResponse.builder().build();

		ChatClientResponse result = advisor.after(chatClientResponse, null);

		assertEquals(chatClientResponse, result);
	}

	@Test
	void after_withExistingChatResponse_addsMetadata() {
		Map<String, Object> context = new HashMap<>();
		context.put(DashScopeDocumentAnalysisAdvisor.UPLOAD_RESPONSE,
				ResponseEntity.ok(new DashScopeDocumentAnalysisAdvisor.UploadResponse("file-123", "file", 100,
						"test.txt", "file-extract", "success", "now")));

		ChatResponse chatResponse = ChatResponse.builder().generations(List.of()).build();
		ChatClientResponse chatClientResponse = ChatClientResponse.builder()
			.chatResponse(chatResponse)
			.context(context)
			.build();

		ChatClientResponse result = advisor.after(chatClientResponse, null);

		assertNotNull(result.chatResponse());
		assertNotNull(result.chatResponse().getMetadata().get(DashScopeDocumentAnalysisAdvisor.UPLOAD_RESPONSE));
	}

	@Test
	@Tag("integration")
	@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
	void test_upload() {

		ApiKey apiKey = new SimpleApiKey(System.getenv("AI_DASHSCOPE_API_KEY"));
		DashScopeDocumentAnalysisAdvisor advisor = new DashScopeDocumentAnalysisAdvisor(apiKey);

		Resource testFile = new DefaultResourceLoader().getResource("classpath:/test-file.pdf");

		ResponseEntity<DashScopeDocumentAnalysisAdvisor.UploadResponse> uploadResponse = advisor.upload(testFile);

		assertThat(uploadResponse).isNotNull();
		assertThat(uploadResponse.getBody()).isNotNull();
		assertThat(uploadResponse.getBody().id()).isNotNull();
		assertThat(uploadResponse.getBody().id()).startsWith("file-fe-");
	}

	@Test
	@Tag("integration")
	@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
	void test_before() {
		ApiKey apiKey = new SimpleApiKey(System.getenv("AI_DASHSCOPE_API_KEY"));
		DashScopeDocumentAnalysisAdvisor advisor = new DashScopeDocumentAnalysisAdvisor(apiKey);

		Resource testFile = new DefaultResourceLoader().getResource("classpath:/test-file.pdf");

		UserMessage userMessage = UserMessage.builder().text("What is the content of this document?").build();
		SystemMessage systemMessage = SystemMessage.builder().text("You are a helpful assistant.").build();
		ChatClientRequest chatClientRequestOrigen = ChatClientRequest.builder()
			.prompt(Prompt.builder().messages(List.of(userMessage, systemMessage)).build())
			.context(DashScopeDocumentAnalysisAdvisor.RESOURCE, testFile)
			.build();
		ChatClientRequest chatClientRequest = advisor.before(chatClientRequestOrigen, null);

		assertThat(chatClientRequest).isNotNull();
		assertThat(chatClientRequest.context().get(DashScopeDocumentAnalysisAdvisor.UPLOAD_RESPONSE)).isNotNull();
		ResponseEntity<DashScopeDocumentAnalysisAdvisor.UploadResponse> uploadResponseEntity = (ResponseEntity<DashScopeDocumentAnalysisAdvisor.UploadResponse>) chatClientRequest
			.context()
			.get(DashScopeDocumentAnalysisAdvisor.UPLOAD_RESPONSE);
		assertThat(uploadResponseEntity.getBody()).isNotNull();
		assertThat(uploadResponseEntity.getBody().id()).isNotEmpty();
		assertThat(uploadResponseEntity.getBody().id()).startsWith("file-fe-");
		assertThat(chatClientRequest.prompt().getSystemMessage().getText()).startsWith("fileid://");
	}

}
