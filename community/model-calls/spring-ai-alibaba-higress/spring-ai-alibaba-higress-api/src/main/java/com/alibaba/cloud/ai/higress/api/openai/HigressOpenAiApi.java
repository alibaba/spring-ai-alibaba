/*
 * Copyright 2023-2025 the original author or authors.
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

package com.alibaba.cloud.ai.higress.api.openai;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.springframework.ai.model.*;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.ai.openai.api.common.OpenAiApiConstants;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.InterceptingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Single class implementation of the
 * <a href="https://platform.openai.com/docs/api-reference/chat">OpenAI Chat Completion
 * API</a> and <a href="https://platform.openai.com/docs/api-reference/embeddings">OpenAI
 * Embedding API</a>.</br>
 * </br>
 * 2025-12-10 add: Adapt to the Qwen model. The Qwen model can use it and pass some
 * parameters of the characteristic
 *
 * @author Christian Tzolov
 * @author Michael Lavelle
 * @author Mariusz Bernacki
 * @author Thomas Vitale
 * @author David Frizelle
 * @author Alexandros Pappas
 * @author Filip Hrisafov
 * @author shiyu
 */

public class HigressOpenAiApi {

	/**
	 * Returns a builder pre-populated with the current configuration for mutation.
	 */
	public Builder mutate() {
		return new Builder(this);
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final ChatModel DEFAULT_CHAT_MODEL = ChatModel.GPT_4_O;

	public static final String DEFAULT_EMBEDDING_MODEL = EmbeddingModel.TEXT_EMBEDDING_ADA_002.getValue();

	private static final Predicate<String> SSE_DONE_PREDICATE = "[DONE]"::equals;

	// Store config fields for mutate/copy
	private final String baseUrl;

	private final ApiKey apiKey;

	private final MultiValueMap<String, String> headers;

	private final String completionsPath;

	private final String embeddingsPath;

	private final ResponseErrorHandler responseErrorHandler;

	private final RestClient restClient;

	private final WebClient webClient;

	/**
	 * Create a new chat completion api.
	 * @param baseUrl api base URL.
	 * @param apiKey OpenAI apiKey.
	 * @param headers the http headers to use.
	 * @param completionsPath the path to the chat completions endpoint.
	 * @param embeddingsPath the path to the embeddings endpoint.
	 * @param restClientBuilder RestClient builder.
	 * @param webClientBuilder WebClient builder.
	 * @param responseErrorHandler Response error handler.
	 */
	public HigressOpenAiApi(String baseUrl, ApiKey apiKey, MultiValueMap<String, String> headers,
			String completionsPath, String embeddingsPath, RestClient.Builder restClientBuilder,
			WebClient.Builder webClientBuilder, ResponseErrorHandler responseErrorHandler) {
		this(baseUrl, apiKey, headers, completionsPath, embeddingsPath, restClientBuilder,
				webClientBuilder, responseErrorHandler, null);
	}

	/**
	 * Create a new chat completion api with HMAC signing support.
	 * @param baseUrl api base URL.
	 * @param apiKey OpenAI apiKey.
	 * @param headers the http headers to use.
	 * @param completionsPath the path to the chat completions endpoint.
	 * @param embeddingsPath the path to the embeddings endpoint.
	 * @param restClientBuilder RestClient builder.
	 * @param webClientBuilder WebClient builder.
	 * @param responseErrorHandler Response error handler.
	 * @param hmacSigner HMAC signer for request signing, can be null to disable signing.
	 */
	public HigressOpenAiApi(String baseUrl, ApiKey apiKey, MultiValueMap<String, String> headers,
			String completionsPath, String embeddingsPath, RestClient.Builder restClientBuilder,
			WebClient.Builder webClientBuilder, ResponseErrorHandler responseErrorHandler,
			HmacSigner hmacSigner) {
		this.baseUrl = baseUrl;
		this.apiKey = apiKey;
		this.headers = headers;
		this.completionsPath = completionsPath;
		this.embeddingsPath = embeddingsPath;
		this.responseErrorHandler = responseErrorHandler;

		Assert.hasText(completionsPath, "Completions Path must not be null");
		Assert.hasText(embeddingsPath, "Embeddings Path must not be null");
		Assert.notNull(headers, "Headers must not be null");

		// @formatter:off
        Consumer<HttpHeaders> finalHeaders = h -> {
            h.setContentType(MediaType.APPLICATION_JSON);
            h.addAll(headers);
        };
        
        // If an HmacSigner is provided, add a signing interceptor
        RestClient.Builder clientBuilder = restClientBuilder.clone();
        if (hmacSigner != null) {
            // Get or create the ClientHttpRequestFactory
            ClientHttpRequestFactory requestFactory = getOrCreateRequestFactory(clientBuilder);
            
            // Create the signing interceptor
            HigressSigningInterceptor signingInterceptor = new HigressSigningInterceptor(hmacSigner);
            List<org.springframework.http.client.ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
            interceptors.add(signingInterceptor);
            
            // Wrap the existing factory with an InterceptingClientHttpRequestFactory
            InterceptingClientHttpRequestFactory interceptingFactory = 
                    new InterceptingClientHttpRequestFactory(requestFactory, interceptors);
            
            clientBuilder.requestFactory(interceptingFactory);
        }
        
        this.restClient = clientBuilder
                .baseUrl(baseUrl)
                .defaultHeaders(finalHeaders)
                .defaultStatusHandler(responseErrorHandler)
                .build();

        this.webClient = webClientBuilder.clone()
                .baseUrl(baseUrl)
                .defaultHeaders(finalHeaders)
                .build(); // @formatter:on
	}

	/**
	 * 获取或创建 ClientHttpRequestFactory
	 * 如果 restClientBuilder 已经配置了 requestFactory，则使用它；否则创建默认的
	 * 
	 * 注意：由于 RestClient.Builder 没有提供获取当前 requestFactory 的公共方法，
	 * 这里使用反射来获取。如果反射失败，将使用默认的 SimpleClientHttpRequestFactory。
	 */
	private ClientHttpRequestFactory getOrCreateRequestFactory(RestClient.Builder restClientBuilder) {
		// 尝试通过反射获取已设置的 requestFactory
		try {
			java.lang.reflect.Field field = restClientBuilder.getClass().getDeclaredField("requestFactory");
			field.setAccessible(true);
			Object factory = field.get(restClientBuilder);
			if (factory instanceof ClientHttpRequestFactory) {
				// 如果已经是 InterceptingClientHttpRequestFactory，需要特殊处理
				// 这里我们直接使用它，因为 InterceptingClientHttpRequestFactory 可以嵌套
				return (ClientHttpRequestFactory) factory;
			}
		}
		catch (NoSuchFieldException e) {
			// 字段不存在，使用默认 factory
		}
		catch (IllegalAccessException e) {
			// 无法访问字段，使用默认 factory
		}
		catch (Exception e) {
			// 其他异常，使用默认 factory
		}
		// 如果没有找到或获取失败，返回默认的 SimpleClientHttpRequestFactory
		return new SimpleClientHttpRequestFactory();
	}

	/**
	 * Returns a string containing all text values from the given media content list. Only
	 * elements of type "text" are processed and concatenated in order.
	 * @param content The list of {@link ChatCompletionMessage.MediaContent}
	 * @return a string containing all text values from "text" type elements
	 * @throws IllegalArgumentException if content is null
	 */
	public static String getTextContent(List<ChatCompletionMessage.MediaContent> content) {
		Assert.notNull(content, "content cannot be null");

		return content.stream()
			.filter(c -> "text".equals(c.type()))
			.map(ChatCompletionMessage.MediaContent::text)
			.collect(Collectors.joining());
	}

	/**
	 * Creates a model response for the given chat conversation.
	 * @param chatRequest The chat completion request.
	 * @return Entity response with {@link ChatCompletion} as a body and HTTP status code
	 * and headers.
	 */
	public ResponseEntity<ChatCompletion> chatCompletionEntity(ChatCompletionRequest chatRequest) {
		return chatCompletionEntity(chatRequest, new LinkedMultiValueMap<>());
	}

	/**
	 * Creates a model response for the given chat conversation.
	 * @param chatRequest The chat completion request.
	 * @param additionalHttpHeader Optional, additional HTTP headers to be added to the
	 * request.
	 * @return Entity response with {@link ChatCompletion} as a body and HTTP status code
	 * and headers.
	 */
	public ResponseEntity<ChatCompletion> chatCompletionEntity(ChatCompletionRequest chatRequest,
			MultiValueMap<String, String> additionalHttpHeader) {

		Assert.notNull(chatRequest, "The request body can not be null.");
		Assert.isTrue(!chatRequest.stream(), "Request must set the stream property to false.");
		Assert.notNull(additionalHttpHeader, "The additional HTTP headers can not be null.");

		// @formatter:off
        return this.restClient.post()
                .uri(this.completionsPath)
                .headers(headers -> {
                    headers.addAll(additionalHttpHeader);
                    addDefaultHeadersIfMissing(headers);
                })
                .body(chatRequest)
                .retrieve()
                .toEntity(ChatCompletion.class);
        // @formatter:on
	}

	/**
	 * Creates a streaming chat response for the given chat conversation.
	 * @param chatRequest The chat completion request. Must have the stream property set
	 * to true.
	 * @return Returns a {@link Flux} stream from chat completion chunks.
	 */
	public Flux<ChatCompletionChunk> chatCompletionStream(ChatCompletionRequest chatRequest) {
		return chatCompletionStream(chatRequest, new LinkedMultiValueMap<>());
	}

	/**
	 * Creates a streaming chat response for the given chat conversation.
	 * @param chatRequest The chat completion request. Must have the stream property set
	 * to true.
	 * @param additionalHttpHeader Optional, additional HTTP headers to be added to the
	 * request.
	 * @return Returns a {@link Flux} stream from chat completion chunks.
	 */
	public Flux<ChatCompletionChunk> chatCompletionStream(ChatCompletionRequest chatRequest,
			MultiValueMap<String, String> additionalHttpHeader) {

		Assert.notNull(chatRequest, "The request body can not be null.");
		Assert.isTrue(chatRequest.stream(), "Request must set the stream property to true.");
		if (chatRequest.incrementalOutput != null && !chatRequest.incrementalOutput) {
			Assert.isTrue(!chatRequest.enableThinking,
					"Incremental output cannot be used at the same time as enableThinking.");
		}

		AtomicBoolean isInsideTool = new AtomicBoolean(false);

		boolean incrementalOutput = chatRequest.incrementalOutput != null && chatRequest.incrementalOutput;
		HigressOpenAiStreamFunctionCallingHelper chunkMerger = new HigressOpenAiStreamFunctionCallingHelper(
				incrementalOutput);

		// @formatter:off
        return this.webClient.post()
                .uri(this.completionsPath)
                .headers(headers -> {
                    headers.addAll(additionalHttpHeader);
                    addDefaultHeadersIfMissing(headers);
                }) // @formatter:on
			.body(Mono.just(chatRequest), ChatCompletionRequest.class)
			.retrieve()
			.bodyToFlux(String.class)
			// cancels the flux stream after the "[DONE]" is received.
			.takeUntil(SSE_DONE_PREDICATE)
			// filters out the "[DONE]" message.
			.filter(SSE_DONE_PREDICATE.negate())
			.map(content -> ModelOptionsUtils.jsonToObject(content, ChatCompletionChunk.class))
			// Detect is the chunk is part of a streaming function call.
			.map(chunk -> {
				if (chunkMerger.isStreamingToolFunctionCall(chunk)) {
					isInsideTool.set(true);
				}
				return chunk;
			})
			// Group all chunks belonging to the same function call.
			// Flux<ChatCompletionChunk> -> Flux<Flux<ChatCompletionChunk>>
			.windowUntil(chunk -> {
				if (isInsideTool.get() && chunkMerger.isStreamingToolFunctionCallFinish(chunk)) {
					isInsideTool.set(false);
					return true;
				}
				return !isInsideTool.get();
			})
			// Merging the window chunks into a single chunk.
			// Reduce the inner Flux<ChatCompletionChunk> window into a single
			// Mono<ChatCompletionChunk>,
			// Flux<Flux<ChatCompletionChunk>> -> Flux<Mono<ChatCompletionChunk>>
			.concatMapIterable(window -> {
				Mono<ChatCompletionChunk> monoChunk = window.reduce(
						new ChatCompletionChunk(null, null, null, null, null, null, null, null), chunkMerger::merge);
				return List.of(monoChunk);
			})
			// Flux<Mono<ChatCompletionChunk>> -> Flux<ChatCompletionChunk>
			.flatMap(mono -> mono);
	}

	/**
	 * Creates an embedding vector representing the input text or token array.
	 * @param embeddingRequest The embedding request.
	 * @param <T> Type of the entity in the data list. Can be a {@link String} or
	 * {@link List} of tokens (e.g. Integers). For embedding multiple inputs in a single
	 * request, You can pass a {@link List} of {@link String} or {@link List} of
	 * {@link List} of tokens. For example:
	 *
	 * <pre>{@code List.of("text1", "text2", "text3") or List.of(List.of(1, 2, 3), List.of(3, 4, 5))} </pre>
	 * @return Returns list of {@link Embedding} wrapped in {@link EmbeddingList}.
	 */
	public <T> ResponseEntity<EmbeddingList<Embedding>> embeddings(EmbeddingRequest<T> embeddingRequest) {

		Assert.notNull(embeddingRequest, "The request body can not be null.");

		// Input text to embed, encoded as a string or array of tokens. To embed multiple
		// inputs in a single
		// request, pass an array of strings or array of token arrays.
		Assert.notNull(embeddingRequest.input(), "The input can not be null.");
		Assert.isTrue(embeddingRequest.input() instanceof String || embeddingRequest.input() instanceof List,
				"The input must be either a String, or a List of Strings or List of List of integers.");

		// The input must not exceed the max input tokens for the model (8192 tokens for
		// text-embedding-ada-002), cannot
		// be an empty string, and any array must be 2048 dimensions or less.
		if (embeddingRequest.input() instanceof List list) {
			Assert.isTrue(!CollectionUtils.isEmpty(list), "The input list can not be empty.");
			Assert.isTrue(list.size() <= 2048, "The list must be 2048 dimensions or less");
			Assert.isTrue(
					list.get(0) instanceof String || list.get(0) instanceof Integer || list.get(0) instanceof List,
					"The input must be either a String, or a List of Strings or list of list of integers.");
		}

		return this.restClient.post()
			.uri(this.embeddingsPath)
			.headers(this::addDefaultHeadersIfMissing)
			.body(embeddingRequest)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {

			});
	}

	private void addDefaultHeadersIfMissing(HttpHeaders headers) {
		if (!headers.containsKey(HttpHeaders.AUTHORIZATION) && !(this.apiKey instanceof NoopApiKey)) {
			headers.setBearerAuth(this.apiKey.getValue());
		}
	}

	// Package-private getters for mutate/copy
	String getBaseUrl() {
		return this.baseUrl;
	}

	ApiKey getApiKey() {
		return this.apiKey;
	}

	MultiValueMap<String, String> getHeaders() {
		return this.headers;
	}

	String getCompletionsPath() {
		return this.completionsPath;
	}

	String getEmbeddingsPath() {
		return this.embeddingsPath;
	}

	ResponseErrorHandler getResponseErrorHandler() {
		return this.responseErrorHandler;
	}

	/**
	 * OpenAI Chat Completion Models.
	 * <p>
	 * This enum provides a selective list of chat completion models available through the
	 * OpenAI API, along with their key features and links to the official OpenAI
	 * documentation for further details.
	 * <p>
	 * The models are grouped by their capabilities and intended use cases. For each
	 * model, a brief description is provided, highlighting its strengths, limitations,
	 * and any specific features. When available, the description also includes
	 * information about the model's context window, maximum output tokens, and knowledge
	 * cutoff date.
	 * <p>
	 * <b>References:</b> <a href="https://platform.openai.com/docs/models">OpenAI Models
	 * Documentation</a>
	 */
	public enum ChatModel implements ChatModelDescription {

		// --- Reasoning Models ---

		/**
		 * <b>o4-mini</b> is the latest small o-series model. It's optimized for fast,
		 * effective reasoning with exceptionally efficient performance in coding and
		 * visual tasks.
		 * <p>
		 * Context window: 200,000 tokens. Max output tokens: 100,000 tokens. Knowledge
		 * cutoff: June 1, 2024.
		 * <p>
		 * Model ID: o4-mini
		 * <p>
		 * See: <a href="https://platform.openai.com/docs/models/o4-mini">o4-mini</a>
		 */
		O4_MINI("o4-mini"),

		/**
		 * <b>o3</b> is a well-rounded and powerful model across domains. It sets a new
		 * standard for math, science, coding, and visual reasoning tasks. It also excels
		 * at technical writing and instruction-following. Use it to think through
		 * multi-step problems that involve analysis across text, code, and images.
		 * <p>
		 * Context window: 200,000 tokens. Max output tokens: 100,000 tokens. Knowledge
		 * cutoff: June 1, 2024.
		 * <p>
		 * Model ID: o3
		 * <p>
		 * See: <a href="https://platform.openai.com/docs/models/o3">o3</a>
		 */
		O3("o3"),

		/**
		 * <b>o3-mini</b> is a small reasoning model, providing high intelligence at cost
		 * and latency targets similar to o1-mini. o3-mini supports key developer
		 * features, like Structured Outputs, function calling, Batch API.
		 * <p>
		 * The knowledge cutoff for o3-mini models is October, 2023.
		 * <p>
		 * Context window: 200,000 tokens. Max output tokens: 100,000 tokens. Knowledge
		 * cutoff: October 1, 2023.
		 * <p>
		 * Model ID: o3-mini
		 * <p>
		 * See: <a href="https://platform.openai.com/docs/models/o3-mini">o3-mini</a>
		 */
		O3_MINI("o3-mini"),

		/**
		 * The <b>o1</b> series of models are trained with reinforcement learning to
		 * perform complex reasoning. o1 models think before they answer, producing a long
		 * internal chain of thought before responding to the user.
		 * <p>
		 * Context window: 200,000 tokens. Max output tokens: 100,000 tokens. Knowledge
		 * cutoff: October 1, 2023.
		 * <p>
		 * Model ID: o1
		 * <p>
		 * See: <a href="https://platform.openai.com/docs/models/o1">o1</a>
		 */
		O1("o1"),

		/**
		 * <b>o1-mini</b> is a faster and more affordable reasoning model compared to o1.
		 * o1-mini currently only supports text inputs and outputs.
		 * <p>
		 * Context window: 128,000 tokens. Max output tokens: 65,536 tokens. Knowledge
		 * cutoff: October 1, 2023.
		 * <p>
		 * Model ID: o1-mini
		 * <p>
		 * See: <a href="https://platform.openai.com/docs/models/o1-mini">o1-mini</a>
		 */
		O1_MINI("o1-mini"),

		/**
		 * The <b>o1-pro</b> model, part of the o1 series trained with reinforcement
		 * learning for complex reasoning, uses more compute to think harder and provide
		 * consistently better answers.
		 * <p>
		 * Note: o1-pro is available in the Responses API only to enable support for
		 * multi-turn model interactions and other advanced API features.
		 * <p>
		 * Context window: 200,000 tokens. Max output tokens: 100,000 tokens. Knowledge
		 * cutoff: October 1, 2023.
		 * <p>
		 * Model ID: o1-pro
		 * <p>
		 * See: <a href="https://platform.openai.com/docs/models/o1-pro">o1-pro</a>
		 */
		O1_PRO("o1-pro"),

		// --- Flagship Models ---

		/**
		 * <b>GPT-4.1</b> is the flagship model for complex tasks. It is well suited for
		 * problem solving across domains.
		 * <p>
		 * Context window: 1,047,576 tokens. Max output tokens: 32,768 tokens. Knowledge
		 * cutoff: June 1, 2024.
		 * <p>
		 * Model ID: gpt-4.1
		 * <p>
		 * See: <a href="https://platform.openai.com/docs/models/gpt-4.1">gpt-4.1</a>
		 */
		GPT_4_1("gpt-4.1"),

		/**
		 * <b>GPT-4o</b> (“o” for “omni”) is the versatile, high-intelligence flagship
		 * model. It accepts both text and image inputs, and produces text outputs
		 * (including Structured Outputs). It is considered the best model for most tasks,
		 * and the most capable model outside of the o-series models.
		 * <p>
		 * Context window: 128,000 tokens. Max output tokens: 16,384 tokens. Knowledge
		 * cutoff: October 1, 2023.
		 * <p>
		 * Model ID: gpt-4o
		 * <p>
		 * See: <a href="https://platform.openai.com/docs/models/gpt-4o">gpt-4o</a>
		 */
		GPT_4_O("gpt-4o"),

		/**
		 * The <b>chatgpt-4o-latest</b> model ID continuously points to the version of
		 * GPT-4o used in ChatGPT. It is updated frequently when there are significant
		 * changes to ChatGPT's GPT-4o model.
		 * <p>
		 * Context window: 128,000 tokens. Max output tokens: 16,384 tokens. Knowledge
		 * cutoff: October 1, 2023.
		 * <p>
		 * Model ID: chatgpt-4o-latest
		 * <p>
		 * See: <a href=
		 * "https://platform.openai.com/docs/models/chatgpt-4o-latest">chatgpt-4o-latest</a>
		 */
		CHATGPT_4_O_LATEST("chatgpt-4o-latest"),

		/**
		 * <b>GPT-4o Audio Preview</b> represents a preview release of models that accept
		 * audio inputs and outputs via the Chat Completions REST API.
		 * <p>
		 * Context window: 128,000 tokens. Max output tokens: 16,384 tokens. Knowledge
		 * cutoff: October 1, 2023.
		 * <p>
		 * Model ID: gpt-4o-audio-preview
		 * <p>
		 * See: <a href=
		 * "https://platform.openai.com/docs/models/gpt-4o-audio-preview">gpt-4o-audio-preview</a>
		 */
		GPT_4_O_AUDIO_PREVIEW("gpt-4o-audio-preview"),

		// --- Cost-Optimized Models ---

		/**
		 * <b>GPT-4.1-mini</b> provides a balance between intelligence, speed, and cost
		 * that makes it an attractive model for many use cases.
		 * <p>
		 * Context window: 1,047,576 tokens. Max output tokens: 32,768 tokens. Knowledge
		 * cutoff: June 1, 2024.
		 * <p>
		 * Model ID: gpt-4.1-mini
		 * <p>
		 * See:
		 * <a href="https://platform.openai.com/docs/models/gpt-4.1-mini">gpt-4.1-mini</a>
		 */
		GPT_4_1_MINI("gpt-4.1-mini"),

		/**
		 * <b>GPT-4.1-nano</b> is the fastest, most cost-effective GPT-4.1 model.
		 * <p>
		 * Context window: 1,047,576 tokens. Max output tokens: 32,768 tokens. Knowledge
		 * cutoff: June 1, 2024.
		 * <p>
		 * Model ID: gpt-4.1-nano
		 * <p>
		 * See:
		 * <a href="https://platform.openai.com/docs/models/gpt-4.1-nano">gpt-4.1-nano</a>
		 */
		GPT_4_1_NANO("gpt-4.1-nano"),

		/**
		 * <b>GPT-4o-mini</b> is a fast, affordable small model for focused tasks. It
		 * accepts both text and image inputs and produces text outputs (including
		 * Structured Outputs). It is ideal for fine-tuning, and model outputs from a
		 * larger model like GPT-4o can be distilled to GPT-4o-mini to produce similar
		 * results at lower cost and latency.
		 * <p>
		 * Context window: 128,000 tokens. Max output tokens: 16,384 tokens. Knowledge
		 * cutoff: October 1, 2023.
		 * <p>
		 * Model ID: gpt-4o-mini
		 * <p>
		 * See:
		 * <a href="https://platform.openai.com/docs/models/gpt-4o-mini">gpt-4o-mini</a>
		 */
		GPT_4_O_MINI("gpt-4o-mini"),

		/**
		 * <b>GPT-4o-mini Audio Preview</b> is a preview release model that accepts audio
		 * inputs and outputs and can be used in the Chat Completions REST API.
		 * <p>
		 * Context window: 128,000 tokens. Max output tokens: 16,384 tokens. Knowledge
		 * cutoff: October 1, 2023.
		 * <p>
		 * Model ID: gpt-4o-mini-audio-preview
		 * <p>
		 * See: <a href=
		 * "https://platform.openai.com/docs/models/gpt-4o-mini-audio-preview">gpt-4o-mini-audio-preview</a>
		 */
		GPT_4_O_MINI_AUDIO_PREVIEW("gpt-4o-mini-audio-preview"),

		// --- Realtime Models ---

		/**
		 * <b>GPT-4o Realtime</b> model, is capable of responding to audio and text inputs
		 * in realtime over WebRTC or a WebSocket interface.
		 * <p>
		 * Context window: 128,000 tokens. Max output tokens: 4,096 tokens. Knowledge
		 * cutoff: October 1, 2023.
		 * <p>
		 * Model ID: gpt-4o-realtime-preview
		 * <p>
		 * See: <a href=
		 * "https://platform.openai.com/docs/models/gpt-4o-realtime-preview">gpt-4o-realtime-preview</a>
		 */
		GPT_4O_REALTIME_PREVIEW("gpt-4o-realtime-preview"),

		/**
		 * <b>GPT-4o-mini Realtime</b> model, is capable of responding to audio and text
		 * inputs in realtime over WebRTC or a WebSocket interface.
		 * <p>
		 * Context window: 128,000 tokens. Max output tokens: 4,096 tokens. Knowledge
		 * cutoff: October 1, 2023.
		 * <p>
		 * Model ID: gpt-4o-mini-realtime-preview
		 * <p>
		 * See: <a href=
		 * "https://platform.openai.com/docs/models/gpt-4o-mini-realtime-preview">gpt-4o-mini-realtime-preview</a>
		 */
		GPT_4O_MINI_REALTIME_PREVIEW("gpt-4o-mini-realtime-preview\n"),

		// --- Older GPT Models ---

		/**
		 * <b>GPT-4 Turbo</b> is the next generation of GPT-4, an older high-intelligence
		 * GPT model. It was designed to be a cheaper, better version of GPT-4. Today, we
		 * recommend using a newer model like GPT-4o.
		 * <p>
		 * Context window: 128,000 tokens. Max output tokens: 4,096 tokens. Knowledge
		 * cutoff: Dec 01, 2023.
		 * <p>
		 * Model ID: gpt-4-turbo
		 * <p>
		 * See:
		 * <a href="https://platform.openai.com/docs/models/gpt-4-turbo">gpt-4-turbo</a>
		 */
		GPT_4_TURBO("gpt-4-turbo"),

		/**
		 * <b>GPT-4</b> is an older version of a high-intelligence GPT model, usable in
		 * Chat Completions. Vision capabilities may not be available.
		 * <p>
		 * Context window: 128,000 tokens. Max output tokens: 4,096 tokens. Knowledge
		 * cutoff: Dec 01, 2023.
		 * <p>
		 * Model ID: gpt-4
		 * <p>
		 * See: <a href="https://platform.openai.com/docs/models/gpt-4">gpt-4</a>
		 */
		GPT_4("gpt-4"),

		/**
		 * <b>GPT-3.5 Turbo</b> models can understand and generate natural language or
		 * code and have been optimized for chat using the Chat Completions API but work
		 * well for non-chat tasks as well. Generally lower cost but less capable than
		 * GPT-4 models.
		 * <p>
		 * As of July 2024, GPT-4o mini is recommended over gpt-3.5-turbo for most use
		 * cases.
		 * <p>
		 * Context window: 16,385 tokens. Max output tokens: 4,096 tokens. Knowledge
		 * cutoff: September, 2021.
		 * <p>
		 * Model ID: gpt-3.5-turbo
		 * <p>
		 * See: <a href=
		 * "https://platform.openai.com/docs/models/gpt-3.5-turbo">gpt-3.5-turbo</a>
		 */
		GPT_3_5_TURBO("gpt-3.5-turbo"),

		/**
		 * <b>GPT-3.5 Turbo Instruct</b> has similar capabilities to GPT-3 era models.
		 * Compatible with the legacy Completions endpoint and not Chat Completions.
		 * <p>
		 * Context window: 4,096 tokens. Max output tokens: 4,096 tokens. Knowledge
		 * cutoff: September, 2021.
		 */
		GPT_3_5_TURBO_INSTRUCT("gpt-3.5-turbo-instruct"),

		/**
		 * <b>GPT-4o Search Preview</b> is a specialized model for web search in Chat
		 * Completions. It is trained to understand and execute web search queries. See
		 * the web search guide for more information.
		 */
		GPT_4_O_SEARCH_PREVIEW("gpt-4o-search-preview"),

		/**
		 * <b>GPT-4o mini Search Preview</b> is a specialized model for web search in Chat
		 * Completions. It is trained to understand and execute web search queries. See
		 * the web search guide for more information.
		 */
		GPT_4_O_MINI_SEARCH_PREVIEW("gpt-4o-mini-search-preview");

		public final String value;

		ChatModel(String value) {
			this.value = value;
		}

		public String getValue() {
			return this.value;
		}

		@Override
		public String getName() {
			return this.value;
		}

	}

	/**
	 * The reason the model stopped generating tokens.
	 */
	public enum ChatCompletionFinishReason {

		/**
		 * The model hit a natural stop point or a provided stop sequence.
		 */
		@JsonProperty("stop")
		STOP,
		/**
		 * The maximum number of tokens specified in the request was reached.
		 */
		@JsonProperty("length")
		LENGTH,
		/**
		 * The content was omitted due to a flag from our content filters.
		 */
		@JsonProperty("content_filter")
		CONTENT_FILTER,
		/**
		 * The model called a tool.
		 */
		@JsonProperty("tool_calls")
		TOOL_CALLS,
		/**
		 * Only for compatibility with Mistral AI API.
		 */
		@JsonProperty("tool_call")
		TOOL_CALL

	}

	/**
	 * OpenAI Embeddings Models:
	 * <a href="https://platform.openai.com/docs/models/embeddings">Embeddings</a>.
	 */
	public enum EmbeddingModel {

		/**
		 * Most capable embedding model for both english and non-english tasks. DIMENSION:
		 * 3072
		 */
		TEXT_EMBEDDING_3_LARGE("text-embedding-3-large"),

		/**
		 * Increased performance over 2nd generation ada embedding model. DIMENSION: 1536
		 */
		TEXT_EMBEDDING_3_SMALL("text-embedding-3-small"),

		/**
		 * Most capable 2nd generation embedding model, replacing 16 first generation
		 * models. DIMENSION: 1536
		 */
		TEXT_EMBEDDING_ADA_002("text-embedding-ada-002");

		public final String value;

		EmbeddingModel(String value) {
			this.value = value;
		}

		public String getValue() {
			return this.value;
		}

	}

	/**
	 * Represents a tool the model may call. Currently, only functions are supported as a
	 * tool.
	 */
	@JsonInclude(Include.NON_NULL)
	public static class FunctionTool {

		/**
		 * The type of the tool. Currently, only 'function' is supported.
		 */
		@JsonProperty("type")
		private Type type = Type.FUNCTION;

		/**
		 * The function definition.
		 */
		@JsonProperty("function")
		private Function function;

		public FunctionTool() {

		}

		/**
		 * Create a tool of type 'function' and the given function definition.
		 * @param type the tool type
		 * @param function function definition
		 */
		public FunctionTool(Type type, Function function) {
			this.type = type;
			this.function = function;
		}

		/**
		 * Create a tool of type 'function' and the given function definition.
		 * @param function function definition.
		 */
		public FunctionTool(Function function) {
			this(Type.FUNCTION, function);
		}

		public Type getType() {
			return this.type;
		}

		public Function getFunction() {
			return this.function;
		}

		public void setType(Type type) {
			this.type = type;
		}

		public void setFunction(Function function) {
			this.function = function;
		}

		/**
		 * Create a tool of type 'function' and the given function definition.
		 */
		public enum Type {

			/**
			 * Function tool type.
			 */
			@JsonProperty("function")
			FUNCTION

		}

		/**
		 * Function definition.
		 */
		@JsonInclude(Include.NON_NULL)
		public static class Function {

			@JsonProperty("description")
			private String description;

			@JsonProperty("name")
			private String name;

			@JsonProperty("parameters")
			private Map<String, Object> parameters;

			@JsonProperty("strict")
			Boolean strict;

			@JsonIgnore
			private String jsonSchema;

			/**
			 * NOTE: Required by Jackson, JSON deserialization!
			 */
			@SuppressWarnings("unused")
			private Function() {
			}

			/**
			 * Create tool function definition.
			 * @param description A description of what the function does, used by the
			 * model to choose when and how to call the function.
			 * @param name The name of the function to be called. Must be a-z, A-Z, 0-9,
			 * or contain underscores and dashes, with a maximum length of 64.
			 * @param parameters The parameters the functions accepts, described as a JSON
			 * Schema object. To describe a function that accepts no parameters, provide
			 * the value {"type": "object", "properties": {}}.
			 * @param strict Whether to enable strict schema adherence when generating the
			 * function call. If set to true, the model will follow the exact schema
			 * defined in the parameters field. Only a subset of JSON Schema is supported
			 * when strict is true.
			 */
			public Function(String description, String name, Map<String, Object> parameters, Boolean strict) {
				this.description = description;
				this.name = name;
				this.parameters = parameters;
				this.strict = strict;
			}

			/**
			 * Create tool function definition.
			 * @param description tool function description.
			 * @param name tool function name.
			 * @param jsonSchema tool function schema as json.
			 */
			public Function(String description, String name, String jsonSchema) {
				this(description, name, ModelOptionsUtils.jsonToMap(jsonSchema), null);
			}

			public String getDescription() {
				return this.description;
			}

			public String getName() {
				return this.name;
			}

			public Map<String, Object> getParameters() {
				return this.parameters;
			}

			public void setDescription(String description) {
				this.description = description;
			}

			public void setName(String name) {
				this.name = name;
			}

			public void setParameters(Map<String, Object> parameters) {
				this.parameters = parameters;
			}

			public Boolean getStrict() {
				return this.strict;
			}

			public void setStrict(Boolean strict) {
				this.strict = strict;
			}

			public String getJsonSchema() {
				return this.jsonSchema;
			}

			public void setJsonSchema(String jsonSchema) {
				this.jsonSchema = jsonSchema;
				if (jsonSchema != null) {
					this.parameters = ModelOptionsUtils.jsonToMap(jsonSchema);
				}
			}

		}

	}

	/**
	 * The type of modality for the model completion.
	 */
	public enum OutputModality {

		// @formatter:off
        @JsonProperty("audio")
        AUDIO,
        @JsonProperty("text")
        TEXT
        // @formatter:on

	}

	/**
	 * Creates a model request for the given chat conversation.
	 *
	 * @param messages A list of messages comprising the conversation so far.
	 * @param model ID of the model to use.
	 * @param store Whether to store the output of this chat completion request for use in
	 * OpenAI's model distillation or evals products.
	 * @param metadata Developer-defined tags and values used for filtering completions in
	 * the OpenAI's dashboard.
	 * @param frequencyPenalty Number between -2.0 and 2.0. Positive values penalize new
	 * tokens based on their existing frequency in the text so far, decreasing the model's
	 * likelihood to repeat the same line verbatim.
	 * @param logitBias Modify the likelihood of specified tokens appearing in the
	 * completion. Accepts a JSON object that maps tokens (specified by their token ID in
	 * the tokenizer) to an associated bias value from -100 to 100. Mathematically, the
	 * bias is added to the logits generated by the model prior to sampling. The exact
	 * effect will vary per model, but values between -1 and 1 should decrease or increase
	 * likelihood of selection; values like -100 or 100 should result in a ban or
	 * exclusive selection of the relevant token.
	 * @param logprobs Whether to return log probabilities of the output tokens or not. If
	 * true, returns the log probabilities of each output token returned in the 'content'
	 * of 'message'.
	 * @param topLogprobs An integer between 0 and 5 specifying the number of most likely
	 * tokens to return at each token position, each with an associated log probability.
	 * 'logprobs' must be set to 'true' if this parameter is used.
	 * @param maxTokens The maximum number of tokens that can be generated in the chat
	 * completion. This value can be used to control costs for text generated via API.
	 * This value is now deprecated in favor of max_completion_tokens, and is not
	 * compatible with o1 series models. The field is retained for use with other openai
	 * models and openai compatible models.
	 * @param maxCompletionTokens An upper bound for the number of tokens that can be
	 * generated for a completion, including visible output tokens and reasoning tokens.
	 * @param n How many chat completion choices to generate for each input message. Note
	 * that you will be charged based on the number of generated tokens across all the
	 * choices. Keep n as 1 to minimize costs.
	 * @param outputModalities Output types that you would like the model to generate for
	 * this request. Most models are capable of generating text, which is the default:
	 * ["text"]. The gpt-4o-audio-preview model can also be used to generate audio. To
	 * request that this model generate both text and audio responses, you can use:
	 * ["text", "audio"].
	 * @param audioParameters Parameters for audio output. Required when audio output is
	 * requested with outputModalities: ["audio"].
	 * @param presencePenalty Number between -2.0 and 2.0. Positive values penalize new
	 * tokens based on whether they appear in the text so far, increasing the model's
	 * likelihood to talk about new topics.
	 * @param responseFormat An object specifying the format that the model must output.
	 * Setting to { "type": "json_object" } enables JSON mode, which guarantees the
	 * message the model generates is valid JSON.
	 * @param seed This feature is in Beta. If specified, our system will make a best
	 * effort to sample deterministically, such that repeated requests with the same seed
	 * and parameters should return the same result. Determinism is not guaranteed, and
	 * you should refer to the system_fingerprint response parameter to monitor changes in
	 * the backend.
	 * @param serviceTier Specifies the latency tier to use for processing the request.
	 * This parameter is relevant for customers subscribed to the scale tier service. When
	 * this parameter is set, the response body will include the service_tier utilized.
	 * @param stop Up to 4 sequences where the API will stop generating further tokens.
	 * @param stream If set, partial message deltas will be sent.Tokens will be sent as
	 * data-only server-sent events as they become available, with the stream terminated
	 * by a data: [DONE] message.
	 * @param streamOptions Options for streaming response. Only set this when you set.
	 * @param temperature What sampling temperature to use, between 0 and 1. Higher values
	 * like 0.8 will make the output more random, while lower values like 0.2 will make it
	 * more focused and deterministic. We generally recommend altering this or top_p but
	 * not both.
	 * @param topP An alternative to sampling with temperature, called nucleus sampling,
	 * where the model considers the results of the tokens with top_p probability mass. So
	 * 0.1 means only the tokens comprising the top 10% probability mass are considered.
	 * We generally recommend altering this or temperature but not both.
	 * @param tools A list of tools the model may call. Currently, only functions are
	 * supported as a tool. Use this to provide a list of functions the model may generate
	 * JSON inputs for.
	 * @param toolChoice Controls which (if any) function is called by the model. none
	 * means the model will not call a function and instead generates a message. auto
	 * means the model can pick between generating a message or calling a function.
	 * Specifying a particular function via {"type: "function", "function": {"name":
	 * "my_function"}} forces the model to call that function. none is the default when no
	 * functions are present. auto is the default if functions are present. Use the
	 * {@link ToolChoiceBuilder} to create the tool choice value.
	 * @param user A unique identifier representing your end-user, which can help OpenAI
	 * to monitor and detect abuse.
	 * @param parallelToolCalls If set to true, the model will call all functions in the
	 * tools list in parallel. Otherwise, the model will call the functions in the tools
	 * list in the order they are provided.
	 * @param reasoningEffort Constrains effort on reasoning for reasoning models.
	 * Currently supported values are low, medium, and high. Reducing reasoning effort can
	 * result in faster responses and fewer tokens used on reasoning in a response.
	 * @param webSearchOptions Options for web search.
	 * @param enableThinking Adapt to the qwen model: Open the model thinking mode，When
	 * this mode is turned on, the model's responses will have an additional
	 * reasoning_content field
	 * @param enableSearch Adapt to the qwen model: Turn on the model's network search
	 * function
	 * @param searchOptions Adapt to the qwen model: Configuration parameters for
	 * networked search of models
	 * @param incrementalOutput Adapt to the qwen model: Whether to turn on incremental
	 * mode false（default）：Outputs the entire sequence that has currently been generated
	 * one at a time, with the final output being the complete result. true：Incremental
	 * output, i.e., the subsequent output content does not contain what has already been
	 * output. read these segments one by one in real time to get the complete result.
	 */
	@JsonInclude(Include.NON_NULL)
	public record ChatCompletionRequest(// @formatter:off
                                        @JsonProperty("messages") List<ChatCompletionMessage> messages,
                                        @JsonProperty("model") String model,
                                        @JsonProperty("store") Boolean store,
                                        @JsonProperty("metadata") Map<String, String> metadata,
                                        @JsonProperty("frequency_penalty") Double frequencyPenalty,
                                        @JsonProperty("logit_bias") Map<String, Integer> logitBias,
                                        @JsonProperty("logprobs") Boolean logprobs,
                                        @JsonProperty("top_logprobs") Integer topLogprobs,
                                        @JsonProperty("max_tokens") Integer maxTokens, // original field for specifying token usage.
                                        @JsonProperty("max_completion_tokens") Integer maxCompletionTokens, // new field for gpt-o1 and other reasoning models
                                        @JsonProperty("n") Integer n,
                                        @JsonProperty("modalities") List<OutputModality> outputModalities,
                                        @JsonProperty("audio") AudioParameters audioParameters,
                                        @JsonProperty("presence_penalty") Double presencePenalty,
                                        @JsonProperty("response_format") ResponseFormat responseFormat,
                                        @JsonProperty("seed") Integer seed,
                                        @JsonProperty("service_tier") String serviceTier,
                                        @JsonProperty("stop") List<String> stop,
                                        @JsonProperty("stream") Boolean stream,
                                        @JsonProperty("stream_options") StreamOptions streamOptions,
                                        @JsonProperty("temperature") Double temperature,
                                        @JsonProperty("top_p") Double topP,
                                        @JsonProperty("tools") List<FunctionTool> tools,
                                        @JsonProperty("tool_choice") Object toolChoice,
                                        @JsonProperty("parallel_tool_calls") Boolean parallelToolCalls,
                                        @JsonProperty("user") String user,
                                        @JsonProperty("reasoning_effort") String reasoningEffort,
                                        @JsonProperty("web_search_options") WebSearchOptions webSearchOptions,
                                        @JsonProperty("enable_thinking") Boolean enableThinking,
                                        @JsonProperty("enable_search") Boolean enableSearch,
                                        @JsonProperty("search_options") QwenOpenAiCompatibleSearchOptions searchOptions,
                                        @JsonProperty("incremental_output") Boolean incrementalOutput) {

        /**
         * Shortcut constructor for a chat completion request with the given messages, model and temperature.
         *
         * @param messages A list of messages comprising the conversation so far.
         * @param model ID of the model to use.
         * @param temperature What sampling temperature to use, between 0 and 1.
         */
        public ChatCompletionRequest(List<ChatCompletionMessage> messages, String model, Double temperature) {
            this(messages, model, null, null, null, null, null, null, null, null, null, null, null, null, null,
                    null, null, null, false, null, temperature, null,
                    null, null, null, null, null, null,null,null,null,null);
        }

        /**
         * Shortcut constructor for a chat completion request with text and audio output.
         *
         * @param messages A list of messages comprising the conversation so far.
         * @param model ID of the model to use.
         * @param audio Parameters for audio output. Required when audio output is requested with outputModalities: ["audio"].
         */
        public ChatCompletionRequest(List<ChatCompletionMessage> messages, String model, AudioParameters audio, boolean stream) {
            this(messages, model, null, null, null, null, null, null,
                    null, null, null, List.of(OutputModality.AUDIO, OutputModality.TEXT), audio, null, null,
                    null, null, null, stream, null, null, null,
                    null, null, null, null, null, null,null,null,null,null);
        }

        /**
         * Shortcut constructor for a chat completion request with the given messages, model, temperature and control for streaming.
         *
         * @param messages A list of messages comprising the conversation so far.
         * @param model ID of the model to use.
         * @param temperature What sampling temperature to use, between 0 and 1.
         * @param stream If set, partial message deltas will be sent.Tokens will be sent as data-only server-sent events
         * as they become available, with the stream terminated by a data: [DONE] message.
         */
        public ChatCompletionRequest(List<ChatCompletionMessage> messages, String model, Double temperature, boolean stream) {
            this(messages, model, null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, stream, null, temperature, null,
                    null, null, null, null, null, null,null,null,null,null);
        }

        /**
         * Shortcut constructor for a chat completion request with the given messages, model, tools and tool choice.
         * Streaming is set to false, temperature to 0.8 and all other parameters are null.
         *
         * @param messages A list of messages comprising the conversation so far.
         * @param model ID of the model to use.
         * @param tools A list of tools the model may call. Currently, only functions are supported as a tool.
         * @param toolChoice Controls which (if any) function is called by the model.
         */
        public ChatCompletionRequest(List<ChatCompletionMessage> messages, String model,
                                     List<FunctionTool> tools, Object toolChoice) {
            this(messages, model, null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, false, null, 0.8, null,
                    tools, toolChoice, null, null, null, null,null,null,null,null);
        }

        /**
         * Shortcut constructor for a chat completion request with the given messages for streaming.
         *
         * @param messages A list of messages comprising the conversation so far.
         * @param stream If set, partial message deltas will be sent.Tokens will be sent as data-only server-sent events
         * as they become available, with the stream terminated by a data: [DONE] message.
         */
        public ChatCompletionRequest(List<ChatCompletionMessage> messages, Boolean stream) {
            this(messages, null, null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, stream, null, null, null,
                    null, null, null, null, null, null,null,null,null,null);
        }

        /**
         * Sets the {@link StreamOptions} for this request.
         *
         * @param streamOptions The new stream options to use.
         * @return A new {@link ChatCompletionRequest} with the specified stream options.
         */
        public ChatCompletionRequest streamOptions(StreamOptions streamOptions) {
            return new ChatCompletionRequest(this.messages, this.model, this.store, this.metadata, this.frequencyPenalty, this.logitBias, this.logprobs,
                    this.topLogprobs, this.maxTokens, this.maxCompletionTokens, this.n, this.outputModalities, this.audioParameters, this.presencePenalty,
                    this.responseFormat, this.seed, this.serviceTier, this.stop, this.stream, streamOptions, this.temperature, this.topP,
                    this.tools, this.toolChoice, this.parallelToolCalls, this.user, this.reasoningEffort, this.webSearchOptions,this.enableThinking,this.enableSearch,this.searchOptions
                    , this.incrementalOutput
            );
        }

        /**
         * Helper factory that creates a tool_choice of type 'none', 'auto' or selected function by name.
         */
        public static class ToolChoiceBuilder {
            /**
             * Model can pick between generating a message or calling a function.
             */
            public static final String AUTO = "auto";
            /**
             * Model will not call a function and instead generates a message
             */
            public static final String NONE = "none";

            /**
             * Specifying a particular function forces the model to call that function.
             */
            public static Object FUNCTION(String functionName) {
                return Map.of("type", "function", "function", Map.of("name", functionName));
            }
        }

        /**
         * Parameters for audio output. Required when audio output is requested with outputModalities: ["audio"].
         * @param voice Specifies the voice type.
         * @param format Specifies the output audio format.
         */
        @JsonInclude(Include.NON_NULL)
        public record AudioParameters(
                @JsonProperty("voice") Voice voice,
                @JsonProperty("format") AudioResponseFormat format) {

            /**
             * Specifies the voice type.
             */
            public enum Voice {
                /** Alloy voice */
                @JsonProperty("alloy") ALLOY,
                /** Ash voice */
                @JsonProperty("ash") ASH,
                /** Ballad voice */
                @JsonProperty("ballad") BALLAD,
                /** Coral voice */
                @JsonProperty("coral") CORAL,
                /** Echo voice */
                @JsonProperty("echo") ECHO,
                /** Fable voice */
                @JsonProperty("fable") FABLE,
                /** Onyx voice */
                @JsonProperty("onyx") ONYX,
                /** Nova voice */
                @JsonProperty("nova") NOVA,
                /** Sage voice */
                @JsonProperty("sage") SAGE,
                /** Shimmer voice */
                @JsonProperty("shimmer") SHIMMER
            }

            /**
             * Specifies the output audio format.
             */
            public enum AudioResponseFormat {
                /** MP3 format */
                @JsonProperty("mp3") MP3,
                /** FLAC format */
                @JsonProperty("flac") FLAC,
                /** OPUS format */
                @JsonProperty("opus") OPUS,
                /** PCM16 format */
                @JsonProperty("pcm16") PCM16,
                /** WAV format */
                @JsonProperty("wav") WAV
            }
        }

        /**
         * @param includeUsage If set, an additional chunk will be streamed
         * before the data: [DONE] message. The usage field on this chunk
         * shows the token usage statistics for the entire request, and
         * the choices field will always be an empty array. All other chunks
         * will also include a usage field, but with a null value.
         */
        @JsonInclude(Include.NON_NULL)
        public record StreamOptions(
                @JsonProperty("include_usage") Boolean includeUsage) {

            public static StreamOptions INCLUDE_USAGE = new StreamOptions(true);
        }

        /**
         * This tool searches the web for relevant results to use in a response.
         *
         * @param searchContextSize
         * @param userLocation
         */
        @JsonInclude(Include.NON_NULL)
        public record WebSearchOptions(@JsonProperty("search_context_size") SearchContextSize searchContextSize,
                                       @JsonProperty("user_location") UserLocation userLocation) {

            /**
             * High level guidance for the amount of context window space to use for the
             * search. One of low, medium, or high. medium is the default.
             */
            public enum SearchContextSize {

                /**
                 * Low context size.
                 */
                @JsonProperty("low")
                LOW,

                /**
                 * Medium context size. This is the default.
                 */
                @JsonProperty("medium")
                MEDIUM,

                /**
                 * High context size.
                 */
                @JsonProperty("high")
                HIGH

            }

            /**
             * Approximate location parameters for the search.
             *
             * @param type The type of location approximation. Always "approximate".
             * @param approximate The approximate location details.
             */
            @JsonInclude(Include.NON_NULL)
            public record UserLocation(@JsonProperty("type") String type,
                                       @JsonProperty("approximate") Approximate approximate) {

                @JsonInclude(Include.NON_NULL)
                public record Approximate(@JsonProperty("city") String city, @JsonProperty("country") String country,
                                          @JsonProperty("region") String region, @JsonProperty("timezone") String timezone) {
                }
            }

        }

    } // @formatter:on

	/**
	 * Message comprising the conversation.
	 *
	 * @param rawContent The contents of the message. Can be either a {@link MediaContent}
	 * or a {@link String}. The response message content is always a {@link String}.
	 * @param role The role of the messages author. Could be one of the {@link Role}
	 * types.
	 * @param name An optional name for the participant. Provides the model information to
	 * differentiate between participants of the same role. In case of Function calling,
	 * the name is the function name that the message is responding to.
	 * @param toolCallId Tool call that this message is responding to. Only applicable for
	 * the {@link Role#TOOL} role and null otherwise.
	 * @param toolCalls The tool calls generated by the model, such as function calls.
	 * Applicable only for {@link Role#ASSISTANT} role and null otherwise.
	 * @param refusal The refusal message by the assistant. Applicable only for
	 * {@link Role#ASSISTANT} role and null otherwise.
	 * @param audioOutput Audio response from the model.
	 * @param annotations Annotations for the message, when applicable, as when using the
	 * web search tool.
	 * @param reasoningContent This property has a value when enableThinking is true, and
	 * the output is the model's thinking
	 *
	 */
	@JsonInclude(Include.NON_NULL)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record ChatCompletionMessage(// @formatter:off
                                        @JsonProperty("content") Object rawContent,
                                        @JsonProperty("role") Role role,
                                        @JsonProperty("name") String name,
                                        @JsonProperty("tool_call_id") String toolCallId,
                                        @JsonProperty("tool_calls") @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY) List<ToolCall> toolCalls,
                                        @JsonProperty("refusal") String refusal,
                                        @JsonProperty("audio") AudioOutput audioOutput,
                                        @JsonProperty("annotations") List<Annotation> annotations,
                                        @JsonProperty("reasoning_content") String reasoningContent
    ) { // @formatter:on

		/**
		 * Create a chat completion message with the given content and role. All other
		 * fields are null.
		 * @param content The contents of the message.
		 * @param role The role of the author of this message.
		 */
		public ChatCompletionMessage(Object content, Role role) {
			this(content, role, null, null, null, null, null, null, null);
		}

		/**
		 * Get message content as String.
		 */
		public String content() {
			if (this.rawContent == null) {
				return "";
			}

			if (this.rawContent instanceof String text) {
				return text;
			}

			if (this.rawContent instanceof List list) {
				if (list.isEmpty()) {
					return "";
				}

				Object object = list.get(0);
				if (object instanceof Map map) {
					if (map.isEmpty() || map.get("text") == null) {
						return "";
					}

					return map.get("text").toString();
				}
			}
			throw new IllegalStateException("The content is not valid!");
		}

		/**
		 * The role of the author of this message.
		 */
		public enum Role {

			/**
			 * System message.
			 */
			@JsonProperty("system")
			SYSTEM,
			/**
			 * User message.
			 */
			@JsonProperty("user")
			USER,
			/**
			 * Assistant message.
			 */
			@JsonProperty("assistant")
			ASSISTANT,
			/**
			 * Tool message.
			 */
			@JsonProperty("tool")
			TOOL

		}

		/**
		 * An array of content parts with a defined type. Each MediaContent can be of
		 * either "text", "image_url", or "input_audio" type. Only one option allowed.
		 *
		 * @param type Content type, each can be of type text or image_url.
		 * @param text The text content of the message.
		 * @param imageUrl The image content of the message. You can pass multiple images
		 * by adding multiple image_url content parts. Image input is only supported when
		 * using the gpt-4-visual-preview model.
		 * @param inputAudio Audio content part.
		 */
		@JsonInclude(Include.NON_NULL)
		@JsonIgnoreProperties(ignoreUnknown = true)
		public record MediaContent(// @formatter:off
                                   @JsonProperty("type") String type,
                                   @JsonProperty("text") String text,
                                   @JsonProperty("image_url") ImageUrl imageUrl,
                                   @JsonProperty("input_audio") InputAudio inputAudio,
                                   @JsonProperty("file") InputFile inputFile) { // @formatter:on

			/**
			 * Shortcut constructor for a text content.
			 * @param text The text content of the message.
			 */
			public MediaContent(String text) {
				this("text", text, null, null, null);
			}

			/**
			 * Shortcut constructor for an image content.
			 * @param imageUrl The image content of the message.
			 */
			public MediaContent(ImageUrl imageUrl) {
				this("image_url", null, imageUrl, null, null);
			}

			/**
			 * Shortcut constructor for an audio content.
			 * @param inputAudio The audio content of the message.
			 */
			public MediaContent(InputAudio inputAudio) {
				this("input_audio", null, null, inputAudio, null);
			}

			/**
			 * Shortcut constructor for a file content
			 * @param inputFile The file content of the message.
			 */
			public MediaContent(InputFile inputFile) {
				this("file", null, null, null, inputFile);
			}

			/**
			 * @param data Base64 encoded audio data.
			 * @param format The format of the encoded audio data. Currently supports
			 * "wav" and "mp3".
			 */
			@JsonInclude(Include.NON_NULL)
			public record InputAudio(// @formatter:off
                                     @JsonProperty("data") String data,
                                     @JsonProperty("format") Format format) {

                public enum Format {
                    /** MP3 audio format */
                    @JsonProperty("mp3") MP3,
                    /** WAV audio format */
                    @JsonProperty("wav") WAV
                } // @formatter:on
			}

			/**
			 * Shortcut constructor for an image content.
			 *
			 * @param url Either a URL of the image or the base64 encoded image data. The
			 * base64 encoded image data must have a special prefix in the following
			 * format: "data:{mimetype};base64,{base64-encoded-image-data}".
			 * @param detail Specifies the detail level of the image.
			 */
			@JsonInclude(Include.NON_NULL)
			public record ImageUrl(@JsonProperty("url") String url, @JsonProperty("detail") String detail) {

				public ImageUrl(String url) {
					this(url, null);
				}

			}

			/**
			 * Constructor for base64-encoded file
			 *
			 * @param filename name of the file
			 * @param fileData file data with format
			 * "data:{mimetype};base64,{base64-encoded-image-data}".
			 */
			public record InputFile(@JsonProperty("filename") String filename,
					@JsonProperty("file_data") String fileData) {

			}

		}

		/**
		 * The relevant tool call.
		 *
		 * @param index The index of the tool call in the list of tool calls. Required in
		 * case of streaming.
		 * @param id The ID of the tool call. This ID must be referenced when you submit
		 * the tool outputs in using the Submit tool outputs to run endpoint.
		 * @param type The type of tool call the output is required for. For now, this is
		 * always function.
		 * @param function The function definition.
		 */
		@JsonInclude(Include.NON_NULL)
		@JsonIgnoreProperties(ignoreUnknown = true)
		public record ToolCall(// @formatter:off
                               @JsonProperty("index") Integer index,
                               @JsonProperty("id") String id,
                               @JsonProperty("type") String type,
                               @JsonProperty("function") ChatCompletionFunction function) { // @formatter:on

			public ToolCall(String id, String type, ChatCompletionFunction function) {
				this(null, id, type, function);
			}

		}

		/**
		 * The function definition.
		 *
		 * @param name The name of the function.
		 * @param arguments The arguments that the model expects you to pass to the
		 * function.
		 */
		@JsonInclude(Include.NON_NULL)
		@JsonIgnoreProperties(ignoreUnknown = true)
		public record ChatCompletionFunction(// @formatter:off
                                             @JsonProperty("name") String name,
                                             @JsonProperty("arguments") String arguments) { // @formatter:on
		}

		/**
		 * Audio response from the model.
		 *
		 * @param id Unique identifier for the audio response from the model.
		 * @param data Audio output from the model.
		 * @param expiresAt When the audio content will no longer be available on the
		 * server.
		 * @param transcript Transcript of the audio output from the model.
		 */
		@JsonInclude(Include.NON_NULL)
		@JsonIgnoreProperties(ignoreUnknown = true)
		public record AudioOutput(// @formatter:off
                                  @JsonProperty("id") String id,
                                  @JsonProperty("data") String data,
                                  @JsonProperty("expires_at") Long expiresAt,
                                  @JsonProperty("transcript") String transcript
        ) { // @formatter:on
		}

		/**
		 * Represents an annotation within a message, specifically for URL citations.
		 */
		@JsonInclude(Include.NON_NULL)
		public record Annotation(@JsonProperty("type") String type,
				@JsonProperty("url_citation") UrlCitation urlCitation) {
			/**
			 * A URL citation when using web search.
			 *
			 * @param endIndex The index of the last character of the URL citation in the
			 * message.
			 * @param startIndex The index of the first character of the URL citation in
			 * the message.
			 * @param title The title of the web resource.
			 * @param url The URL of the web resource.
			 */
			@JsonInclude(Include.NON_NULL)
			public record UrlCitation(@JsonProperty("end_index") Integer endIndex,
					@JsonProperty("start_index") Integer startIndex, @JsonProperty("title") String title,
					@JsonProperty("url") String url) {
			}
		}
	}

	/**
	 * Represents a chat completion response returned by model, based on the provided
	 * input.
	 *
	 * @param id A unique identifier for the chat completion.
	 * @param choices A list of chat completion choices. Can be more than one if n is
	 * greater than 1.
	 * @param created The Unix timestamp (in seconds) of when the chat completion was
	 * created.
	 * @param model The model used for the chat completion.
	 * @param serviceTier The service tier used for processing the request. This field is
	 * only included if the service_tier parameter is specified in the request.
	 * @param systemFingerprint This fingerprint represents the backend configuration that
	 * the model runs with. Can be used in conjunction with the seed request parameter to
	 * understand when backend changes have been made that might impact determinism.
	 * @param object The object type, which is always chat.completion.
	 * @param usage Usage statistics for the completion request.
	 */
	@JsonInclude(Include.NON_NULL)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record ChatCompletion(// @formatter:off
                                 @JsonProperty("id") String id,
                                 @JsonProperty("choices") List<Choice> choices,
                                 @JsonProperty("created") Long created,
                                 @JsonProperty("model") String model,
                                 @JsonProperty("service_tier") String serviceTier,
                                 @JsonProperty("system_fingerprint") String systemFingerprint,
                                 @JsonProperty("object") String object,
                                 @JsonProperty("usage") Usage usage
    ) { // @formatter:on

		/**
		 * Chat completion choice.
		 *
		 * @param finishReason The reason the model stopped generating tokens.
		 * @param index The index of the choice in the list of choices.
		 * @param message A chat completion message generated by the model.
		 * @param logprobs Log probability information for the choice.
		 */
		@JsonInclude(Include.NON_NULL)
		@JsonIgnoreProperties(ignoreUnknown = true)
		public record Choice(// @formatter:off
                             @JsonProperty("finish_reason") ChatCompletionFinishReason finishReason,
                             @JsonProperty("index") Integer index,
                             @JsonProperty("message") ChatCompletionMessage message,
                             @JsonProperty("logprobs") LogProbs logprobs) { // @formatter:on
		}

	}

	/**
	 * Log probability information for the choice.
	 *
	 * @param content A list of message content tokens with log probability information.
	 * @param refusal A list of message refusal tokens with log probability information.
	 */
	@JsonInclude(Include.NON_NULL)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record LogProbs(@JsonProperty("content") List<Content> content,
			@JsonProperty("refusal") List<Content> refusal) {

		/**
		 * Message content tokens with log probability information.
		 *
		 * @param token The token.
		 * @param logprob The log probability of the token.
		 * @param probBytes A list of integers representing the UTF-8 bytes representation
		 * of the token. Useful in instances where characters are represented by multiple
		 * tokens and their byte representations must be combined to generate the correct
		 * text representation. Can be null if there is no bytes representation for the
		 * token.
		 * @param topLogprobs List of the most likely tokens and their log probability, at
		 * this token position. In rare cases, there may be fewer than the number of
		 * requested top_logprobs returned.
		 */
		@JsonInclude(Include.NON_NULL)
		@JsonIgnoreProperties(ignoreUnknown = true)
		public record Content(// @formatter:off
                              @JsonProperty("token") String token,
                              @JsonProperty("logprob") Float logprob,
                              @JsonProperty("bytes") List<Integer> probBytes,
                              @JsonProperty("top_logprobs") List<TopLogProbs> topLogprobs) { // @formatter:on

			/**
			 * The most likely tokens and their log probability, at this token position.
			 *
			 * @param token The token.
			 * @param logprob The log probability of the token.
			 * @param probBytes A list of integers representing the UTF-8 bytes
			 * representation of the token. Useful in instances where characters are
			 * represented by multiple tokens and their byte representations must be
			 * combined to generate the correct text representation. Can be null if there
			 * is no bytes representation for the token.
			 */
			@JsonInclude(Include.NON_NULL)
			@JsonIgnoreProperties(ignoreUnknown = true)
			public record TopLogProbs(// @formatter:off
                                      @JsonProperty("token") String token,
                                      @JsonProperty("logprob") Float logprob,
                                      @JsonProperty("bytes") List<Integer> probBytes) { // @formatter:on
			}

		}

	}

	// Embeddings API

	/**
	 * Usage statistics for the completion request.
	 *
	 * @param completionTokens Number of tokens in the generated completion. Only
	 * applicable for completion requests.
	 * @param promptTokens Number of tokens in the prompt.
	 * @param totalTokens Total number of tokens used in the request (prompt +
	 * completion).
	 * @param promptTokensDetails Breakdown of tokens used in the prompt.
	 * @param completionTokenDetails Breakdown of tokens used in a completion.
	 */
	@JsonInclude(Include.NON_NULL)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Usage(// @formatter:off
                        @JsonProperty("completion_tokens") Integer completionTokens,
                        @JsonProperty("prompt_tokens") Integer promptTokens,
                        @JsonProperty("total_tokens") Integer totalTokens,
                        @JsonProperty("prompt_tokens_details") PromptTokensDetails promptTokensDetails,
                        @JsonProperty("completion_tokens_details") CompletionTokenDetails completionTokenDetails
    ) { // @formatter:on

		public Usage(Integer completionTokens, Integer promptTokens, Integer totalTokens) {
			this(completionTokens, promptTokens, totalTokens, null, null);
		}

		/**
		 * Breakdown of tokens used in the prompt
		 *
		 * @param audioTokens Audio input tokens present in the prompt.
		 * @param cachedTokens Cached tokens present in the prompt.
		 */
		@JsonInclude(Include.NON_NULL)
		@JsonIgnoreProperties(ignoreUnknown = true)
		public record PromptTokensDetails(// @formatter:off
                                          @JsonProperty("audio_tokens") Integer audioTokens,
                                          @JsonProperty("cached_tokens") Integer cachedTokens) { // @formatter:on
		}

		/**
		 * Breakdown of tokens used in a completion.
		 *
		 * @param reasoningTokens Number of tokens generated by the model for reasoning.
		 * @param acceptedPredictionTokens Number of tokens generated by the model for
		 * accepted predictions.
		 * @param audioTokens Number of tokens generated by the model for audio.
		 * @param rejectedPredictionTokens Number of tokens generated by the model for
		 * rejected predictions.
		 */
		@JsonInclude(Include.NON_NULL)
		@JsonIgnoreProperties(ignoreUnknown = true)
		public record CompletionTokenDetails(// @formatter:off
                                             @JsonProperty("reasoning_tokens") Integer reasoningTokens,
                                             @JsonProperty("accepted_prediction_tokens") Integer acceptedPredictionTokens,
                                             @JsonProperty("audio_tokens") Integer audioTokens,
                                             @JsonProperty("rejected_prediction_tokens") Integer rejectedPredictionTokens) { // @formatter:on
		}
	}

	/**
	 * Represents a streamed chunk of a chat completion response returned by model, based
	 * on the provided input.
	 *
	 * @param id A unique identifier for the chat completion. Each chunk has the same ID.
	 * @param choices A list of chat completion choices. Can be more than one if n is
	 * greater than 1.
	 * @param created The Unix timestamp (in seconds) of when the chat completion was
	 * created. Each chunk has the same timestamp.
	 * @param model The model used for the chat completion.
	 * @param serviceTier The service tier used for processing the request. This field is
	 * only included if the service_tier parameter is specified in the request.
	 * @param systemFingerprint This fingerprint represents the backend configuration that
	 * the model runs with. Can be used in conjunction with the seed request parameter to
	 * understand when backend changes have been made that might impact determinism.
	 * @param object The object type, which is always 'chat.completion.chunk'.
	 * @param usage Usage statistics for the completion request. Present in the last chunk
	 * only if the StreamOptions.includeUsage is set to true.
	 */
	@JsonInclude(Include.NON_NULL)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record ChatCompletionChunk(// @formatter:off
                                      @JsonProperty("id") String id,
                                      @JsonProperty("choices") List<ChunkChoice> choices,
                                      @JsonProperty("created") Long created,
                                      @JsonProperty("model") String model,
                                      @JsonProperty("service_tier") String serviceTier,
                                      @JsonProperty("system_fingerprint") String systemFingerprint,
                                      @JsonProperty("object") String object,
                                      @JsonProperty("usage") Usage usage) { // @formatter:on

		/**
		 * Chat completion choice.
		 *
		 * @param finishReason The reason the model stopped generating tokens.
		 * @param index The index of the choice in the list of choices.
		 * @param delta A chat completion delta generated by streamed model responses.
		 * @param logprobs Log probability information for the choice.
		 */
		@JsonInclude(Include.NON_NULL)
		@JsonIgnoreProperties(ignoreUnknown = true)
		public record ChunkChoice(// @formatter:off
                                  @JsonProperty("finish_reason") ChatCompletionFinishReason finishReason,
                                  @JsonProperty("index") Integer index,
                                  @JsonProperty("delta") ChatCompletionMessage delta,
                                  @JsonProperty("logprobs") LogProbs logprobs) { // @formatter:on

		}

	}

	/**
	 * Represents an embedding vector returned by embedding endpoint.
	 *
	 * @param index The index of the embedding in the list of embeddings.
	 * @param embedding The embedding vector, which is a list of floats. The length of
	 * vector depends on the model.
	 * @param object The object type, which is always 'embedding'.
	 */
	@JsonInclude(Include.NON_NULL)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Embedding(// @formatter:off
                            @JsonProperty("index") Integer index,
                            @JsonProperty("embedding") float[] embedding,
                            @JsonProperty("object") String object) { // @formatter:on

		/**
		 * Create an embedding with the given index, embedding and object type set to
		 * 'embedding'.
		 * @param index The index of the embedding in the list of embeddings.
		 * @param embedding The embedding vector, which is a list of floats. The length of
		 * vector depends on the model.
		 */
		public Embedding(Integer index, float[] embedding) {
			this(index, embedding, "embedding");
		}

	}

	/**
	 * Creates an embedding vector representing the input text.
	 *
	 * @param <T> Type of the input.
	 * @param input Input text to embed, encoded as a string or array of tokens. To embed
	 * multiple inputs in a single request, pass an array of strings or array of token
	 * arrays. The input must not exceed the max input tokens for the model (8192 tokens
	 * for text-embedding-ada-002), cannot be an empty string, and any array must be 2048
	 * dimensions or less.
	 * @param model ID of the model to use.
	 * @param encodingFormat The format to return the embeddings in. Can be either float
	 * or base64.
	 * @param dimensions The number of dimensions the resulting output embeddings should
	 * have. Only supported in text-embedding-3 and later models.
	 * @param user A unique identifier representing your end-user, which can help OpenAI
	 * to monitor and detect abuse.
	 */
	@JsonInclude(Include.NON_NULL)
	public record EmbeddingRequest<T>(// @formatter:off
                                      @JsonProperty("input") T input,
                                      @JsonProperty("model") String model,
                                      @JsonProperty("encoding_format") String encodingFormat,
                                      @JsonProperty("dimensions") Integer dimensions,
                                      @JsonProperty("user") String user) { // @formatter:on

		/**
		 * Create an embedding request with the given input, model and encoding format set
		 * to float.
		 * @param input Input text to embed.
		 * @param model ID of the model to use.
		 */
		public EmbeddingRequest(T input, String model) {
			this(input, model, "float", null, null);
		}

		/**
		 * Create an embedding request with the given input. Encoding format is set to
		 * float and user is null and the model is set to 'text-embedding-ada-002'.
		 * @param input Input text to embed.
		 */
		public EmbeddingRequest(T input) {
			this(input, DEFAULT_EMBEDDING_MODEL);
		}

	}

	/**
	 * List of multiple embedding responses.
	 *
	 * @param <T> Type of the entities in the data list.
	 * @param object Must have value "list".
	 * @param data List of entities.
	 * @param model ID of the model to use.
	 * @param usage Usage statistics for the completion request.
	 */
	@JsonInclude(Include.NON_NULL)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record EmbeddingList<T>(// @formatter:off
                                   @JsonProperty("object") String object,
                                   @JsonProperty("data") List<T> data,
                                   @JsonProperty("model") String model,
                                   @JsonProperty("usage") Usage usage) { // @formatter:on
	}

	public static class Builder {

		public Builder() {
		}

		// Copy constructor for mutate()
		public Builder(HigressOpenAiApi api) {
			this.baseUrl = api.getBaseUrl();
			this.apiKey = api.getApiKey();
			this.headers = new LinkedMultiValueMap<>(api.getHeaders());
			this.completionsPath = api.getCompletionsPath();
			this.embeddingsPath = api.getEmbeddingsPath();
			this.restClientBuilder = api.restClient != null ? api.restClient.mutate() : RestClient.builder();
			this.webClientBuilder = api.webClient != null ? api.webClient.mutate() : WebClient.builder();
			this.responseErrorHandler = api.getResponseErrorHandler();
		}

		private String baseUrl = OpenAiApiConstants.DEFAULT_BASE_URL;

		private ApiKey apiKey;

		private MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();

		private String completionsPath = "/v1/chat/completions";

		private String embeddingsPath = "/v1/embeddings";

		private RestClient.Builder restClientBuilder = RestClient.builder();

		private WebClient.Builder webClientBuilder = WebClient.builder();

		private ResponseErrorHandler responseErrorHandler = RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER;

		private HmacSigner hmacSigner;

		public Builder baseUrl(String baseUrl) {
			Assert.hasText(baseUrl, "baseUrl cannot be null or empty");
			this.baseUrl = baseUrl;
			return this;
		}

		public Builder apiKey(ApiKey apiKey) {
			Assert.notNull(apiKey, "apiKey cannot be null");
			this.apiKey = apiKey;
			return this;
		}

		public Builder apiKey(String simpleApiKey) {
			this.apiKey = new SimpleApiKey(simpleApiKey);
			return this;
		}

		public Builder headers(MultiValueMap<String, String> headers) {
			Assert.notNull(headers, "headers cannot be null");
			this.headers = headers;
			return this;
		}

		public Builder completionsPath(String completionsPath) {
			Assert.hasText(completionsPath, "completionsPath cannot be null or empty");
			this.completionsPath = completionsPath;
			return this;
		}

		public Builder embeddingsPath(String embeddingsPath) {
			Assert.hasText(embeddingsPath, "embeddingsPath cannot be null or empty");
			this.embeddingsPath = embeddingsPath;
			return this;
		}

		public Builder restClientBuilder(RestClient.Builder restClientBuilder) {
			Assert.notNull(restClientBuilder, "restClientBuilder cannot be null");
			this.restClientBuilder = restClientBuilder;
			return this;
		}

		public Builder webClientBuilder(WebClient.Builder webClientBuilder) {
			Assert.notNull(webClientBuilder, "webClientBuilder cannot be null");
			this.webClientBuilder = webClientBuilder;
			return this;
		}

		public Builder responseErrorHandler(ResponseErrorHandler responseErrorHandler) {
			Assert.notNull(responseErrorHandler, "responseErrorHandler cannot be null");
			this.responseErrorHandler = responseErrorHandler;
			return this;
		}

		/**
		 * 设置HMAC签名器，用于对HTTP请求进行签名
		 * @param hmacSigner HMAC签名器，如果为null则禁用签名
		 * @return Builder实例
		 */
		public Builder hmacSigner(HmacSigner hmacSigner) {
			this.hmacSigner = hmacSigner;
			return this;
		}

		/**
		 * 设置HMAC签名密钥，自动创建HmacSigner
		 * @param secretKey HMAC签名密钥
		 * @return Builder实例
		 */
		public Builder hmacSecretKey(String secretKey) {
			if (secretKey != null && !secretKey.isEmpty()) {
				this.hmacSigner = new HmacSigner(secretKey);
			}
			else {
				this.hmacSigner = null;
			}
			return this;
		}

		public HigressOpenAiApi build() {
			Assert.notNull(this.apiKey, "apiKey must be set");
			return new HigressOpenAiApi(this.baseUrl, this.apiKey, this.headers, this.completionsPath,
					this.embeddingsPath, this.restClientBuilder, this.webClientBuilder, this.responseErrorHandler,
					this.hmacSigner);
		}

	}

	/**
	 * @param forcedSearch By default, the model determines whether networking is required
	 * or not. if the business scenario strongly relies on real-time information, you can
	 * set the forced_search parameter to true to force networking search. -- true: Force
	 * network search -- false (default): The model determines if networking is required.
	 * @param enableSearchExtension When users query information such as weather, stock
	 * prices, etc., generic web search results may not be accurate enough. you can turn
	 * on the vertical search feature to enable the model to refer to the retrieved
	 * vertical data for more accurate results. set @param enable_search_extension to true
	 * to turn on this feature. -- true: Enabled. -- false (default): Not enabled.
	 * @param searchStrategy The search volume level strategy can be set through
	 * `search_strategy` based on requirements for cost, effect, and response speed. The
	 * available values for `search_strategy` are: -- "turbo" (default): Balances response
	 * speed and search effect, suitable for most scenarios. -- "max": Employs a more
	 * comprehensive search strategy, invoking multiple source search engines to obtain
	 * more detailed search results, but the response time may be longer. -- "agent": Can
	 * invoke online search tools and large models multiple times to achieve multi-round
	 * information retrieval and content integration.
	 * @author shiyu
	 */
	@JsonInclude(Include.NON_NULL)
	public record QwenOpenAiCompatibleSearchOptions(@JsonProperty("forced_search") Boolean forcedSearch,
			@JsonProperty("enable_search_extension") Boolean enableSearchExtension,
			@JsonProperty("search_strategy") String searchStrategy) {

		public static Builder builder() {
			return new Builder();
		}

		public static class Builder {

			private Boolean forcedSearch = false;

			private Boolean enableSearchExtension = false;

			private String searchStrategy = "turbo";

			public Builder forcedSearch(Boolean forcedSearch) {
				this.forcedSearch = forcedSearch;
				return this;
			}

			public Builder enableSearchExtension(Boolean enableSearchExtension) {
				this.enableSearchExtension = enableSearchExtension;
				return this;
			}

			public Builder searchStrategy(String searchStrategy) {
				this.searchStrategy = searchStrategy;
				return this;
			}

			public QwenOpenAiCompatibleSearchOptions build() {
				return new QwenOpenAiCompatibleSearchOptions(forcedSearch, enableSearchExtension, searchStrategy);
			}

		}
	}

}
