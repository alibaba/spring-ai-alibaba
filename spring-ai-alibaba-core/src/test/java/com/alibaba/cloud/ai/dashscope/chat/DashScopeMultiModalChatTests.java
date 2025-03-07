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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mockito.Mockito;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.Media;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Tests for DashScope multi-modal chat functionality.
 *
 * @author brianxiadong
 * @since 1.0.0-M5.1
 */
public class DashScopeMultiModalChatTests {

	private static final String TEST_MODEL = "qwen-vl-max-latest";

	private static final String TEST_API_KEY = "test-api-key";

	private static final String TEST_REQUEST_ID = "test-request-id";

	private static final String TEST_PROMPT = "这些是什么？";

	private static final String TEST_RESPONSE = "图片中是一个小女孩和一只狗在户外。";

	private static final String TEST_VIDEO_PROMPT = "这是一组从视频中提取的图片帧，请描述此视频中的内容。";

	private static final String TEST_VIDEO_RESPONSE = "视频展示了一系列连续的画面，内容是...";

	private DashScopeApi dashScopeApi;

	private DashScopeChatModel chatModel;

	private DashScopeChatOptions defaultOptions;

	private ResourceLoader resourceLoader;

	@BeforeEach
	void setUp() {
		// Mock the DashScopeApi
		dashScopeApi = Mockito.mock(DashScopeApi.class);

		// Mock ResourceLoader
		resourceLoader = Mockito.mock(ResourceLoader.class);
		Resource mockResource = new ClassPathResource("multimodel/dog_and_girl.jpeg");
		when(resourceLoader.getResource("classpath:/multimodel/dog_and_girl.jpeg")).thenReturn(mockResource);

		// Setup default options
		defaultOptions = DashScopeChatOptions.builder().withModel(TEST_MODEL).withMultiModel(true).build();

		// Create the chat model with mocked API
		chatModel = new DashScopeChatModel(dashScopeApi, defaultOptions);
	}

	/**
	 * Test image processing with URL-based media
	 */
	@Test
	void testImageWithUrl() throws Exception {
		// Setup mock response
		ChatCompletionMessage responseMessage = new ChatCompletionMessage(TEST_RESPONSE,
				ChatCompletionMessage.Role.ASSISTANT);
		Choice choice = new Choice(ChatCompletionFinishReason.STOP, responseMessage);
		ChatCompletionOutput output = new ChatCompletionOutput(TEST_RESPONSE, List.of(choice));
		TokenUsage usage = new TokenUsage(20, 10, 30);
		ChatCompletion chatCompletion = new ChatCompletion(TEST_REQUEST_ID, output, usage);
		ResponseEntity<ChatCompletion> responseEntity = ResponseEntity.ok(chatCompletion);

		when(dashScopeApi.chatCompletionEntity(any(ChatCompletionRequest.class))).thenReturn(responseEntity);

		// Create media list with URL
		List<Media> mediaList = List.of(new Media(MimeTypeUtils.IMAGE_PNG,
				new URI("https://dashscope.oss-cn-beijing.aliyuncs.com/images/dog_and_girl.jpeg").toURL()));

		// Create user message with media
		UserMessage message = new UserMessage(TEST_PROMPT, mediaList);
		message.getMetadata().put(DashScopeChatModel.MESSAGE_FORMAT, MessageFormat.IMAGE);

		// Create prompt with options
		Prompt prompt = new Prompt(message,
				DashScopeChatOptions.builder().withModel(TEST_MODEL).withMultiModel(true).build());

		// Call the chat model
		ChatResponse response = chatModel.call(prompt);

		// Verify response
		assertThat(response).isNotNull();
		assertThat(response.getResult().getOutput().getText()).isEqualTo(TEST_RESPONSE);
	}

	/**
	 * Test image processing with binary resource
	 */
	@Test
	void testImageWithBinaryResource() {
		// Setup mock response
		ChatCompletionMessage responseMessage = new ChatCompletionMessage(TEST_RESPONSE,
				ChatCompletionMessage.Role.ASSISTANT);
		Choice choice = new Choice(ChatCompletionFinishReason.STOP, responseMessage);
		ChatCompletionOutput output = new ChatCompletionOutput(TEST_RESPONSE, List.of(choice));
		TokenUsage usage = new TokenUsage(20, 10, 30);
		ChatCompletion chatCompletion = new ChatCompletion(TEST_REQUEST_ID, output, usage);
		ResponseEntity<ChatCompletion> responseEntity = ResponseEntity.ok(chatCompletion);

		when(dashScopeApi.chatCompletionEntity(any(ChatCompletionRequest.class))).thenReturn(responseEntity);

		// Create user message with resource media
		UserMessage message = new UserMessage(TEST_PROMPT,
				new Media(MimeTypeUtils.IMAGE_JPEG, new ClassPathResource("multimodel/dog_and_girl.jpeg")));
		message.getMetadata().put(DashScopeChatModel.MESSAGE_FORMAT, MessageFormat.IMAGE);

		// Create prompt with options
		Prompt prompt = new Prompt(message,
				DashScopeChatOptions.builder().withModel(TEST_MODEL).withMultiModel(true).build());

		// Call the chat model
		ChatResponse response = chatModel.call(prompt);

		// Verify response
		assertThat(response).isNotNull();
		assertThat(response.getResult().getOutput().getText()).isEqualTo(TEST_RESPONSE);
	}

	/**
	 * Test video processing with multiple frames
	 */
	@Test
	void testVideoWithMultipleFrames() {
		// Setup mock response
		ChatCompletionMessage responseMessage = new ChatCompletionMessage(TEST_VIDEO_RESPONSE,
				ChatCompletionMessage.Role.ASSISTANT);
		Choice choice = new Choice(ChatCompletionFinishReason.STOP, responseMessage);
		ChatCompletionOutput output = new ChatCompletionOutput(TEST_VIDEO_RESPONSE, List.of(choice));
		TokenUsage usage = new TokenUsage(20, 10, 30);
		ChatCompletion chatCompletion = new ChatCompletion(TEST_REQUEST_ID, output, usage);
		ResponseEntity<ChatCompletion> responseEntity = ResponseEntity.ok(chatCompletion);

		when(dashScopeApi.chatCompletionEntity(any(ChatCompletionRequest.class))).thenReturn(responseEntity);

		// Create media list with multiple frames (simulating video frames)
		List<Media> mediaList = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			mediaList.add(new Media(MimeTypeUtils.IMAGE_JPEG, new ClassPathResource("multimodel/dog_and_girl.jpeg")));
		}

		// Create user message with media
		UserMessage message = new UserMessage(TEST_VIDEO_PROMPT, mediaList);
		message.getMetadata().put(DashScopeChatModel.MESSAGE_FORMAT, MessageFormat.VIDEO);

		// Create prompt with options
		Prompt prompt = new Prompt(message,
				DashScopeChatOptions.builder().withModel(TEST_MODEL).withMultiModel(true).build());

		// Call the chat model
		ChatResponse response = chatModel.call(prompt);

		// Verify response
		assertThat(response).isNotNull();
		assertThat(response.getResult().getOutput().getText()).isEqualTo(TEST_VIDEO_RESPONSE);
	}

	/**
	 * Test streaming response with image input
	 */
	@Test
	void testStreamImageResponse() {
		// Setup mock streaming response
		ChatCompletionMessage chunkMessage1 = new ChatCompletionMessage("图片中是一个", ChatCompletionMessage.Role.ASSISTANT);
		ChatCompletionMessage chunkMessage2 = new ChatCompletionMessage("小女孩和一只狗在户外。",
				ChatCompletionMessage.Role.ASSISTANT);

		Choice choice1 = new Choice(null, chunkMessage1);
		Choice choice2 = new Choice(ChatCompletionFinishReason.STOP, chunkMessage2);

		ChatCompletionOutput output1 = new ChatCompletionOutput("图片中是一个", List.of(choice1));
		ChatCompletionOutput output2 = new ChatCompletionOutput("小女孩和一只狗在户外。", List.of(choice2));

		ChatCompletionChunk chunk1 = new ChatCompletionChunk(TEST_REQUEST_ID, output1, null);
		ChatCompletionChunk chunk2 = new ChatCompletionChunk(TEST_REQUEST_ID, output2, new TokenUsage(10, 5, 15));

		when(dashScopeApi.chatCompletionStream(any(ChatCompletionRequest.class))).thenReturn(Flux.just(chunk1, chunk2));

		// Create user message with resource media
		UserMessage message = new UserMessage(TEST_PROMPT,
				new Media(MimeTypeUtils.IMAGE_JPEG, new ClassPathResource("multimodel/dog_and_girl.jpeg")));
		message.getMetadata().put(DashScopeChatModel.MESSAGE_FORMAT, MessageFormat.IMAGE);

		// Create prompt with options
		Prompt prompt = new Prompt(message,
				DashScopeChatOptions.builder().withModel(TEST_MODEL).withMultiModel(true).build());

		// Call the streaming API
		Flux<ChatResponse> responseFlux = chatModel.stream(prompt);

		// Verify streaming response
		StepVerifier.create(responseFlux).assertNext(response -> {
			assertThat(response.getResult().getOutput().getText()).isEqualTo("图片中是一个");
		}).assertNext(response -> {
			assertThat(response.getResult().getOutput().getText()).isEqualTo("小女孩和一只狗在户外。");
		}).verifyComplete();
	}

	// =============== 集成测试案例 ===============

	/**
	 * Integration test for image processing with URL This test will only run if
	 * DASHSCOPE_API_KEY environment variable is set
	 */
	@Test
	@Tag("integration")
	@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
	void integrationTestImageWithUrl() throws Exception {
		// Create real API client
		String apiKey = System.getenv("AI_DASHSCOPE_API_KEY");
		DashScopeApi realApi = new DashScopeApi(apiKey);

		// Create real chat model
		DashScopeChatModel realChatModel = new DashScopeChatModel(realApi);

		// Create media list with URL
		List<Media> mediaList = List.of(new Media(MimeTypeUtils.IMAGE_PNG,
				new URI("https://dashscope.oss-cn-beijing.aliyuncs.com/images/dog_and_girl.jpeg").toURL()));

		// Create user message with media
		UserMessage message = new UserMessage(TEST_PROMPT, mediaList);
		message.getMetadata().put(DashScopeChatModel.MESSAGE_FORMAT, MessageFormat.IMAGE);

		// Create prompt
		Prompt prompt = new Prompt(message,
				DashScopeChatOptions.builder().withModel(TEST_MODEL).withMultiModel(true).build());

		// Call the chat model
		ChatResponse response = realChatModel.call(prompt);

		// Verify response
		assertThat(response).isNotNull();
		assertThat(response.getResult().getOutput().getText()).isNotEmpty();
		System.out.println("Image URL Response: " + response.getResult().getOutput().getText());
	}

	/**
	 * Integration test for image processing with binary resource This test will only run
	 * if DASHSCOPE_API_KEY environment variable is set
	 */
	@Test
	@Tag("integration")
	@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
	void integrationTestImageWithBinaryResource() throws IOException {
		// Create real API client
		String apiKey = System.getenv("AI_DASHSCOPE_API_KEY");
		DashScopeApi realApi = new DashScopeApi(apiKey);

		// Create real chat model
		DashScopeChatModel realChatModel = new DashScopeChatModel(realApi);

		// Create user message with resource media
		UserMessage message = new UserMessage(TEST_PROMPT,
				new Media(MimeTypeUtils.IMAGE_JPEG, new ClassPathResource("multimodel/dog_and_girl.jpeg")));
		message.getMetadata().put(DashScopeChatModel.MESSAGE_FORMAT, MessageFormat.IMAGE);

		// Create prompt
		Prompt prompt = new Prompt(message,
				DashScopeChatOptions.builder().withModel(TEST_MODEL).withMultiModel(true).build());

		// Call the chat model
		ChatResponse response = realChatModel.call(prompt);

		// Verify response
		assertThat(response).isNotNull();
		assertThat(response.getResult().getOutput().getText()).isNotEmpty();
		System.out.println("Binary Image Response: " + response.getResult().getOutput().getText());
	}

	/**
	 * Integration test for video processing with multiple frames This test will only run
	 * if DASHSCOPE_API_KEY environment variable is set
	 */
	@Test
	@Tag("integration")
	@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
	void integrationTestVideoWithMultipleFrames() throws IOException {
		// Create real API client
		String apiKey = System.getenv("AI_DASHSCOPE_API_KEY");
		DashScopeApi realApi = new DashScopeApi(apiKey);

		// Create real chat model
		DashScopeChatModel realChatModel = new DashScopeChatModel(realApi);

		// Create media list with multiple frames (simulating video frames)
		List<Media> mediaList = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			mediaList.add(new Media(MimeTypeUtils.IMAGE_JPEG, new ClassPathResource("multimodel/dog_and_girl.jpeg")));
		}

		// Create user message with media
		UserMessage message = new UserMessage(TEST_VIDEO_PROMPT, mediaList);
		message.getMetadata().put(DashScopeChatModel.MESSAGE_FORMAT, MessageFormat.VIDEO);

		// Create prompt
		Prompt prompt = new Prompt(message,
				DashScopeChatOptions.builder().withModel(TEST_MODEL).withMultiModel(true).build());

		// Call the chat model
		ChatResponse response = realChatModel.call(prompt);

		// Verify response
		assertThat(response).isNotNull();
		assertThat(response.getResult().getOutput().getText()).isNotEmpty();
		System.out.println("Video Frames Response: " + response.getResult().getOutput().getText());
	}

	/**
	 * Integration test for streaming response with image input This test will only run if
	 * DASHSCOPE_API_KEY environment variable is set
	 */
	@Test
	@Tag("integration")
	@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
	void integrationTestStreamImageResponse() throws IOException {
		// Create real API client
		String apiKey = System.getenv("AI_DASHSCOPE_API_KEY");
		DashScopeApi realApi = new DashScopeApi(apiKey);

		// Create real chat model
		DashScopeChatModel realChatModel = new DashScopeChatModel(realApi);

		// Create user message with resource media
		UserMessage message = new UserMessage(TEST_PROMPT,
				new Media(MimeTypeUtils.IMAGE_JPEG, new ClassPathResource("multimodel/dog_and_girl.jpeg")));
		message.getMetadata().put(DashScopeChatModel.MESSAGE_FORMAT, MessageFormat.IMAGE);

		// Create prompt
		Prompt prompt = new Prompt(message,
				DashScopeChatOptions.builder().withModel(TEST_MODEL).withMultiModel(true).build());

		// Call the streaming API
		Flux<ChatResponse> responseFlux = realChatModel.stream(prompt);

		// Collect all responses
		AtomicReference<StringBuilder> responseBuilder = new AtomicReference<>(new StringBuilder());

		// Verify streaming response
		responseFlux.doOnNext(response -> {
			String content = response.getResult().getOutput().getText();
			System.out.println("Streaming chunk: " + content);
			responseBuilder.get().append(content);
		}).blockLast(Duration.ofSeconds(30));

		// Verify final response
		String finalResponse = responseBuilder.get().toString();
		assertThat(finalResponse).isNotEmpty();
		System.out.println("Final streaming response: " + finalResponse);
	}

	/**
	 * Integration test for image analysis with custom prompt This test will only run if
	 * DASHSCOPE_API_KEY environment variable is set
	 */
	@Test
	@Tag("integration")
	@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
	void integrationTestImageAnalysisWithCustomPrompt() throws IOException {
		// Create real API client
		String apiKey = System.getenv("AI_DASHSCOPE_API_KEY");
		DashScopeApi realApi = new DashScopeApi(apiKey);

		// Create real chat model
		DashScopeChatModel realChatModel = new DashScopeChatModel(realApi);

		// Create user message with resource media and custom prompt
		UserMessage message = new UserMessage("请详细描述这张图片中的场景，包括人物、动物、环境等细节，并分析图片的情感基调。",
				new Media(MimeTypeUtils.IMAGE_JPEG, new ClassPathResource("multimodel/dog_and_girl.jpeg")));
		message.getMetadata().put(DashScopeChatModel.MESSAGE_FORMAT, MessageFormat.IMAGE);

		// Create prompt
		Prompt prompt = new Prompt(message,
				DashScopeChatOptions.builder().withModel(TEST_MODEL).withMultiModel(true).build());

		// Call the chat model
		ChatResponse response = realChatModel.call(prompt);

		// Verify response
		assertThat(response).isNotNull();
		assertThat(response.getResult().getOutput().getText()).isNotEmpty();
		System.out.println("Image Analysis Response: " + response.getResult().getOutput().getText());
	}

}
