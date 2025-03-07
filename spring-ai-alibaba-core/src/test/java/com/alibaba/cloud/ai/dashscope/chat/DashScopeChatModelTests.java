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
package com.alibaba.cloud.ai.dashscope.chat;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.ChatCompletion;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.ChatCompletionChunk;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.ChatCompletionOutput;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.ChatCompletionMessage;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.ChatCompletionRequest;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.ChatCompletionOutput.Choice;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.TokenUsage;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.ChatCompletionFinishReason;
import com.alibaba.cloud.ai.dashscope.metadata.DashScopeAiUsage;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mockito.Mockito;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

/**
 * Test cases for DashScopeChatModel. Tests cover basic chat completion, streaming, tool
 * calls, error handling, and various edge cases.
 *
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 * @author brianxiadong
 * @since 1.0.0-M5.1
 */
class DashScopeChatModelTests {

	// Test constants
	private static final String TEST_MODEL = "qwen-turbo";

	private static final String TEST_API_KEY = "test-api-key";

	private static final String TEST_REQUEST_ID = "test-request-id";

	private static final String TEST_PROMPT = "Hello, how are you?";

	private static final String TEST_RESPONSE = "I'm doing well, thank you for asking!";

	private DashScopeApi dashScopeApi;

	private DashScopeChatModel chatModel;

	private DashScopeChatOptions defaultOptions;

	@BeforeEach
	void setUp() {
		// Initialize mock objects and test instances
		dashScopeApi = Mockito.mock(DashScopeApi.class);
		defaultOptions = DashScopeChatOptions.builder()
			.withModel(TEST_MODEL)
			.withTemperature(0.7)
			.withTopP(0.8)
			.withTopK(50)
			.withSeed(1234)
			.build();
		chatModel = new DashScopeChatModel(dashScopeApi, defaultOptions);
	}

	@Test
	void testBasicChatCompletion() {
		// Test basic chat completion with a simple user message
		Message message = new UserMessage(TEST_PROMPT);
		Prompt prompt = new Prompt(List.of(message));

		// Mock API response
		ChatCompletionMessage responseMessage = new ChatCompletionMessage(TEST_RESPONSE,
				ChatCompletionMessage.Role.ASSISTANT);
		Choice choice = new Choice(ChatCompletionFinishReason.STOP, responseMessage);
		ChatCompletionOutput output = new ChatCompletionOutput(TEST_RESPONSE, List.of(choice));
		TokenUsage usage = new TokenUsage(20, 10, 30);
		ChatCompletion chatCompletion = new ChatCompletion(TEST_REQUEST_ID, output, usage);
		ResponseEntity<ChatCompletion> responseEntity = ResponseEntity.ok(chatCompletion);

		when(dashScopeApi.chatCompletionEntity(any(ChatCompletionRequest.class))).thenReturn(responseEntity);

		// Execute test
		ChatResponse response = chatModel.call(prompt);

		// Verify results
		assertThat(response).isNotNull();
		assertThat(response.getResult()).isNotNull();
		assertThat(response.getResult().getOutput()).isInstanceOf(AssistantMessage.class);
		assertThat(response.getResult().getOutput().getText()).isEqualTo(TEST_RESPONSE);
		assertThat(response.getMetadata().getId()).isEqualTo(TEST_REQUEST_ID);
	}

	@Test
	void testStreamChatCompletion() {
		// Test streaming chat completion with chunked responses
		Message message = new UserMessage(TEST_PROMPT);
		Prompt prompt = new Prompt(List.of(message));

		// Mock streaming responses
		ChatCompletionMessage chunkMessage1 = new ChatCompletionMessage("I'm ", ChatCompletionMessage.Role.ASSISTANT);
		ChatCompletionMessage chunkMessage2 = new ChatCompletionMessage("doing ", ChatCompletionMessage.Role.ASSISTANT);
		ChatCompletionMessage chunkMessage3 = new ChatCompletionMessage("well!", ChatCompletionMessage.Role.ASSISTANT);

		Choice choice1 = new Choice(null, chunkMessage1);
		Choice choice2 = new Choice(null, chunkMessage2);
		Choice choice3 = new Choice(ChatCompletionFinishReason.STOP, chunkMessage3);

		ChatCompletionOutput output1 = new ChatCompletionOutput("I'm ", List.of(choice1));
		ChatCompletionOutput output2 = new ChatCompletionOutput("doing ", List.of(choice2));
		ChatCompletionOutput output3 = new ChatCompletionOutput("well!", List.of(choice3));

		ChatCompletionChunk chunk1 = new ChatCompletionChunk(TEST_REQUEST_ID, output1, null);
		ChatCompletionChunk chunk2 = new ChatCompletionChunk(TEST_REQUEST_ID, output2, null);
		ChatCompletionChunk chunk3 = new ChatCompletionChunk(TEST_REQUEST_ID, output3, new TokenUsage(10, 5, 15));

		when(dashScopeApi.chatCompletionStream(any(ChatCompletionRequest.class)))
			.thenReturn(Flux.just(chunk1, chunk2, chunk3));

		// Execute test
		Flux<ChatResponse> responseFlux = chatModel.stream(prompt);

		// Verify results
		StepVerifier.create(responseFlux).assertNext(response -> {
			assertThat(response.getResult().getOutput().getText()).isEqualTo("I'm ");
		}).assertNext(response -> {
			assertThat(response.getResult().getOutput().getText()).isEqualTo("doing ");
		}).assertNext(response -> {
			assertThat(response.getResult().getOutput().getText()).isEqualTo("well!");
			assertThat(response.getMetadata().getUsage()).isNotNull();
		}).verifyComplete();
	}

	@Test
	void testSystemMessage() {
		// Test chat completion with system message
		SystemMessage systemMessage = new SystemMessage("You are a helpful assistant.");
		UserMessage userMessage = new UserMessage("Hello!");

		// Mock API response
		String response = "Hello! How can I help you today?";
		ChatCompletionMessage responseMessage = new ChatCompletionMessage(response,
				ChatCompletionMessage.Role.ASSISTANT);
		Choice choice = new Choice(ChatCompletionFinishReason.STOP, responseMessage);
		ChatCompletionOutput output = new ChatCompletionOutput(response, List.of(choice));

		// Add non-null TokenUsage with zero values
		TokenUsage usage = new TokenUsage(0, 0, 0);

		ChatCompletion completion = new ChatCompletion("test-id", output, usage);

		when(dashScopeApi.chatCompletionEntity(any())).thenReturn(ResponseEntity.ok(completion));

		// Test with system message
		Prompt prompt = new Prompt(List.of(systemMessage, userMessage));
		ChatResponse chatResponse = chatModel.call(prompt);

		assertThat(chatResponse).isNotNull();
		assertThat(chatResponse.getResults().get(0).getOutput().getText()).isEqualTo(response);
	}

	@Test
	void testToolCalls() {
		// Test tool calls functionality
		FunctionCallback weatherCallback = mock(FunctionCallback.class);
		when(weatherCallback.getName()).thenReturn("get_weather");
		when(weatherCallback.getDescription()).thenReturn("Get weather information");

		// Create options with tool
		DashScopeChatOptions options = DashScopeChatOptions.builder()
			.withModel("qwen-turbo")
			.withFunctionCallbacks(List.of(weatherCallback))
			.build();

		DashScopeChatModel toolChatModel = new DashScopeChatModel(dashScopeApi, options);

		// Mock API responses for tool call
		String toolCallResponse = "{\"name\": \"get_weather\", \"arguments\": \"{\\\"location\\\": \\\"Beijing\\\"}\"}";
		ChatCompletionMessage toolMessage = new ChatCompletionMessage(toolCallResponse,
				ChatCompletionMessage.Role.ASSISTANT);
		Choice toolChoice = new Choice(ChatCompletionFinishReason.TOOL_CALLS, toolMessage);

		// Add non-null TokenUsage with zero values
		TokenUsage usage = new TokenUsage(0, 0, 0);

		ChatCompletionOutput toolOutput = new ChatCompletionOutput(toolCallResponse, List.of(toolChoice));
		ChatCompletion toolCompletion = new ChatCompletion("test-id", toolOutput, usage);

		when(dashScopeApi.chatCompletionEntity(any())).thenReturn(ResponseEntity.ok(toolCompletion));

		// Test tool call
		Message message = new UserMessage("What's the weather like?");
		Prompt prompt = new Prompt(List.of(message), options);
		ChatResponse response = toolChatModel.call(prompt);

		assertThat(response).isNotNull();
		assertThat(response.getResults().get(0).getOutput().getText()).contains("get_weather");
	}

	@Test
	void testStreamToolCalls() {
		// Test streaming tool calls
		FunctionCallback weatherCallback = mock(FunctionCallback.class);
		when(weatherCallback.getName()).thenReturn("get_weather");
		when(weatherCallback.getDescription()).thenReturn("Get weather information");

		DashScopeChatOptions options = DashScopeChatOptions.builder()
			.withModel("qwen-turbo")
			.withFunctionCallbacks(List.of(weatherCallback))
			.withStream(true)
			.build();

		DashScopeChatModel toolChatModel = new DashScopeChatModel(dashScopeApi, options);

		// Mock streaming tool call responses
		String chunk1 = "{\"name\": \"get_";
		String chunk2 = "weather\", \"arguments\": \"{\\\"location\\\"";
		String chunk3 = ": \\\"Beijing\\\"}\"}";

		ChatCompletionMessage message1 = new ChatCompletionMessage(chunk1, ChatCompletionMessage.Role.ASSISTANT);
		ChatCompletionMessage message2 = new ChatCompletionMessage(chunk2, ChatCompletionMessage.Role.ASSISTANT);
		ChatCompletionMessage message3 = new ChatCompletionMessage(chunk3, ChatCompletionMessage.Role.ASSISTANT);

		Choice choice1 = new Choice(null, message1);
		Choice choice2 = new Choice(null, message2);
		Choice choice3 = new Choice(ChatCompletionFinishReason.TOOL_CALLS, message3);

		ChatCompletionChunk chunk1Response = new ChatCompletionChunk("test-id",
				new ChatCompletionOutput(chunk1, List.of(choice1)), null);
		ChatCompletionChunk chunk2Response = new ChatCompletionChunk("test-id",
				new ChatCompletionOutput(chunk2, List.of(choice2)), null);
		ChatCompletionChunk chunk3Response = new ChatCompletionChunk("test-id",
				new ChatCompletionOutput(chunk3, List.of(choice3)), new TokenUsage(15, 25, 40));

		when(dashScopeApi.chatCompletionStream(any()))
			.thenReturn(Flux.just(chunk1Response, chunk2Response, chunk3Response));

		Message message = new UserMessage("What's the weather like?");
		Prompt prompt = new Prompt(List.of(message), options);
		List<ChatResponse> responses = toolChatModel.stream(prompt).collectList().block();

		assertThat(responses).isNotNull();
		assertThat(responses).hasSize(3);
		assertThat(responses.get(0).getResults().get(0).getOutput().getText()).isEqualTo(chunk1);
		assertThat(responses.get(1).getResults().get(0).getOutput().getText()).isEqualTo(chunk2);
		assertThat(responses.get(2).getResults().get(0).getOutput().getText()).isEqualTo(chunk3);
	}

	@Test
    void testErrorHandling() {
        // Test error handling
        when(dashScopeApi.chatCompletionEntity(any()))
                .thenThrow(new RuntimeException("API Error"));

        Message message = new UserMessage("Test message");
        Prompt prompt = new Prompt(List.of(message));

        assertThatThrownBy(() -> chatModel.call(prompt))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("API Error");
    }

	@Test
	void testEmptyResponse() {
		// Test handling of empty response
		ChatCompletionOutput output = new ChatCompletionOutput("", Collections.emptyList());
		// Add non-null TokenUsage with zero values
		TokenUsage usage = new TokenUsage(0, 0, 0);
		ChatCompletion completion = new ChatCompletion("test-id", output, usage);

		when(dashScopeApi.chatCompletionEntity(any())).thenReturn(ResponseEntity.ok(completion));

		Message message = new UserMessage("Test message");
		Prompt prompt = new Prompt(List.of(message));
		ChatResponse response = chatModel.call(prompt);

		assertThat(response).isNotNull();
		assertThat(response.getResults()).isEmpty();
		// Verify usage metadata
		assertThat(response.getMetadata().getUsage()).isNotNull();
		DashScopeAiUsage aiUsage = (DashScopeAiUsage) response.getMetadata().getUsage();
		assertThat(aiUsage.getPromptTokens()).isZero();
		assertThat(aiUsage.getGenerationTokens()).isZero();
		assertThat(aiUsage.getTotalTokens()).isZero();
	}

	@Test
	void testEmptyPrompt() {
		// Test handling of empty prompt
		Prompt emptyPrompt = new Prompt(Collections.emptyList());
		assertThatThrownBy(() -> chatModel.call(emptyPrompt)).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Prompt");
	}

	@Test
	void testNullPrompt() {
		// Test handling of null prompt
		Prompt prompt = null;
		assertThatThrownBy(() -> chatModel.call(prompt)).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Prompt");
	}

	@Test
	void testCustomMetadata() {
		// Test custom metadata handling
		Message message = new UserMessage(TEST_PROMPT);
		Prompt prompt = new Prompt(List.of(message));

		Map<String, Object> customMetadata = Map.of("custom_field", "custom_value", "timestamp",
				System.currentTimeMillis());

		ChatCompletionMessage responseMessage = new ChatCompletionMessage(TEST_RESPONSE,
				ChatCompletionMessage.Role.ASSISTANT);
		Choice choice = new Choice(ChatCompletionFinishReason.STOP, responseMessage);
		ChatCompletionOutput output = new ChatCompletionOutput(TEST_RESPONSE, List.of(choice));
		TokenUsage usage = new TokenUsage(20, 10, 30);
		ChatCompletion chatCompletion = new ChatCompletion(TEST_REQUEST_ID, output, usage);
		ResponseEntity<ChatCompletion> responseEntity = ResponseEntity.ok(chatCompletion);

		when(dashScopeApi.chatCompletionEntity(any())).thenReturn(responseEntity);

		ChatResponse response = chatModel.call(prompt);

		assertThat(response.getMetadata()).isNotNull();
		assertThat(response.getMetadata().getId()).isEqualTo(TEST_REQUEST_ID);
		DashScopeAiUsage aiUsage = (DashScopeAiUsage) response.getMetadata().getUsage();
		assertThat(aiUsage.getPromptTokens()).isEqualTo(10L);
		assertThat(aiUsage.getGenerationTokens()).isEqualTo(20L);
		assertThat(aiUsage.getTotalTokens()).isEqualTo(30L);
	}

	@Test
	void testInvalidModelName() {
		// Test handling of invalid model name
		DashScopeChatOptions invalidOptions = DashScopeChatOptions.builder().withModel("invalid-model").build();

		DashScopeChatModel invalidModel = new DashScopeChatModel(dashScopeApi, invalidOptions);
		Message message = new UserMessage(TEST_PROMPT);
		Prompt prompt = new Prompt(List.of(message));

		when(dashScopeApi.chatCompletionEntity(any())).thenThrow(new RuntimeException("Invalid model name"));

		assertThatThrownBy(() -> invalidModel.call(prompt)).isInstanceOf(RuntimeException.class)
			.hasMessage("Invalid model name");
	}

	@Test
	void testMultipleMessagesInPrompt() {
		// Test handling of multiple messages in prompt
		SystemMessage systemMessage = new SystemMessage("You are a helpful assistant.");
		UserMessage userMessage1 = new UserMessage("Hello!");
		AssistantMessage assistantMessage = new AssistantMessage("Hi! How can I help you?");
		UserMessage userMessage2 = new UserMessage("What's the weather?");

		ChatCompletionMessage responseMessage = new ChatCompletionMessage("It's sunny today!",
				ChatCompletionMessage.Role.ASSISTANT);
		Choice choice = new Choice(ChatCompletionFinishReason.STOP, responseMessage);
		ChatCompletionOutput output = new ChatCompletionOutput("It's sunny today!", List.of(choice));
		// Add non-null TokenUsage with zero values
		TokenUsage usage = new TokenUsage(0, 0, 0);
		ChatCompletion completion = new ChatCompletion("test-id", output, usage);

		when(dashScopeApi.chatCompletionEntity(any())).thenReturn(ResponseEntity.ok(completion));

		Prompt prompt = new Prompt(List.of(systemMessage, userMessage1, assistantMessage, userMessage2));
		ChatResponse response = chatModel.call(prompt);

		assertThat(response).isNotNull();
		assertThat(response.getResult().getOutput().getText()).isEqualTo("It's sunny today!");
	}

	@Test
	@Tag("integration")
	@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
	void testDeepseekR1Integration() {
		// Create real DashScope API instance with actual API key
		String apiKey = System.getenv("AI_DASHSCOPE_API_KEY");
		if (apiKey == null || apiKey.isEmpty()) {
			// Skip test if API key is not available
			return;
		}

		// Initialize real DashScope API and chat model
		DashScopeApi realApi = new DashScopeApi(apiKey);
		DashScopeChatOptions deepseekOptions = DashScopeChatOptions.builder()
			.withModel("deepseek-r1") // Use deepseek-r1 model
			.withTemperature(0.7)
			.withTopP(0.8)
			.withTopK(50)
			.withSeed(1234)
			.build();
		DashScopeChatModel deepseekModel = new DashScopeChatModel(realApi, deepseekOptions);

		// Create a complex prompt with multiple messages
		SystemMessage systemMessage = new SystemMessage(
				"You are a helpful AI assistant who is knowledgeable about programming.");
		UserMessage userMessage = new UserMessage(
				"Write a simple Java function to calculate the factorial of a number.");

		Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

		// Call the model and verify response
		ChatResponse response = deepseekModel.call(prompt);

		// Verify the response
		assertThat(response).isNotNull();
		assertThat(response.getResult()).isNotNull();
		assertThat(response.getResult().getOutput()).isInstanceOf(AssistantMessage.class);
		assertThat(response.getResult().getOutput().getText()).containsAnyOf("public", "factorial", "return", "int");

		// Verify metadata and usage information
		assertThat(response.getMetadata()).isNotNull();
		assertThat(response.getMetadata().getUsage()).isNotNull();
		DashScopeAiUsage aiUsage = (DashScopeAiUsage) response.getMetadata().getUsage();
		assertThat(aiUsage.getTotalTokens()).isPositive();

		// Verify reasoning content exists
		Object reasoningContent = response.getMetadata().get("reasoning_content");
		assertThat(reasoningContent).isNotNull();
	}

}
