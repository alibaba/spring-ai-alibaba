/*
 * Copyright 2023-2024 the original author or authors.
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

package com.alibaba.cloud.ai.example.dotprompt;

import com.alibaba.cloud.ai.example.dotprompt.model.TextRequest;
import com.alibaba.cloud.ai.example.dotprompt.model.TextResponse;
import com.alibaba.cloud.ai.example.dotprompt.service.TextProcessingService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

@SpringBootTest
class TextProcessingTests {

	@Autowired
	private TextProcessingService textProcessingService;

	@MockBean
	private ChatClient chatClient;

	@Test
	void translateText() {
		// Mock ChatClient response
		mockChatClientResponse("Hola, mundo!");

		TextRequest request = new TextRequest();
		request.setModel("googleai/gemini-1.5-flash");
		request.setText("Hello, world!");
		request.setFrom("English");
		request.setTo("Spanish");

		TextResponse response = textProcessingService.translate(request);

		assertThat(response.getModel()).isEqualTo("googleai/gemini-1.5-flash");
		assertThat(response.getPrompt()).contains("Translate the following text");
		assertThat(response.getResult()).isEqualTo("Hola, mundo!");
	}

	@Test
	void summarizeText() {
		// Mock ChatClient response
		mockChatClientResponse("Brief summary: Important points discussed.");

		TextRequest request = new TextRequest();
		request.setModel("googleai/gemini-1.5-flash");
		request.setText("This is a long text that needs to be summarized...");
		request.setLength("brief");
		request.setFormat("bullet points");

		TextResponse response = textProcessingService.summarize(request);

		assertThat(response.getModel()).isEqualTo("googleai/gemini-1.5-flash");
		assertThat(response.getPrompt()).contains("Summarize the following text");
		assertThat(response.getResult()).contains("Brief summary");
	}

	@Test
	void analyzeText() {
		// Mock ChatClient response
		mockChatClientResponse("Analysis: Positive sentiment, formal style, high clarity");

		TextRequest request = new TextRequest();
		request.setModel("googleai/gemini-1.5-flash");
		request.setText("This is a sample text for analysis.");
		request.setAspects(Arrays.asList("emotion", "style", "clarity"));

		TextResponse response = textProcessingService.analyze(request);

		assertThat(response.getModel()).isEqualTo("googleai/gemini-1.5-flash");
		assertThat(response.getPrompt()).contains("Analyze the following text");
		assertThat(response.getResult()).contains("Analysis:");
	}

	private void mockChatClientResponse(String content) {
		ChatClientRequestSpec promptResponse = mock(ChatClientRequestSpec.class);
		CallResponseSpec callResponse = mock(CallResponseSpec.class);
		ChatResponse chatResponse = mock(ChatResponse.class);
		Generation chatResult = mock(Generation.class);
		AssistantMessage assistantMessage = new AssistantMessage(content);

		when(chatClient.prompt(any(Prompt.class))).thenReturn(promptResponse);
		when(promptResponse.call()).thenReturn(callResponse);
		when(callResponse.chatResponse()).thenReturn(chatResponse);
		when(chatResponse.getResult()).thenReturn(chatResult);
		when(chatResult.getOutput()).thenReturn(assistantMessage);
	}

}
