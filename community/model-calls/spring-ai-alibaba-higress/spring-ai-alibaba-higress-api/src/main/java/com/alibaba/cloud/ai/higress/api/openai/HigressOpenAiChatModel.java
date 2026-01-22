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

import com.alibaba.cloud.ai.higress.api.openai.HigressOpenAiApi.ChatCompletion;
import com.alibaba.cloud.ai.higress.api.openai.HigressOpenAiApi.ChatCompletion.Choice;
import com.alibaba.cloud.ai.higress.api.openai.HigressOpenAiApi.ChatCompletionMessage;
import com.alibaba.cloud.ai.higress.api.openai.HigressOpenAiApi.ChatCompletionMessage.AudioOutput;
import com.alibaba.cloud.ai.higress.api.openai.HigressOpenAiApi.ChatCompletionMessage.ChatCompletionFunction;
import com.alibaba.cloud.ai.higress.api.openai.HigressOpenAiApi.ChatCompletionMessage.MediaContent;
import com.alibaba.cloud.ai.higress.api.openai.HigressOpenAiApi.ChatCompletionMessage.ToolCall;
import com.alibaba.cloud.ai.higress.api.openai.HigressOpenAiApi.ChatCompletionRequest;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.*;
import org.springframework.ai.chat.model.*;
import org.springframework.ai.chat.observation.ChatModelObservationContext;
import org.springframework.ai.chat.observation.ChatModelObservationConvention;
import org.springframework.ai.chat.observation.ChatModelObservationDocumentation;
import org.springframework.ai.chat.observation.DefaultChatModelObservationConvention;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.model.tool.*;
import org.springframework.ai.openai.api.common.OpenAiApiConstants;
import org.springframework.ai.openai.metadata.support.OpenAiResponseHeaderExtractor;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.ai.support.UsageCalculator;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * {@link ChatModel} and {@link StreamingChatModel} implementation for {@literal OpenAI}
 * backed by {@link HigressOpenAiApi}.</br>
 * </br>
 * 2025-12-10 add：adapt to the qwen model
 *
 * @author Mark Pollack
 * @author Christian Tzolov
 * @author Ueibin Kim
 * @author John Blum
 * @author Josh Long
 * @author Jemin Huh
 * @author Grogdunn
 * @author Hyunjoon Choi
 * @author Mariusz Bernacki
 * @author luocongqiu
 * @author Thomas Vitale
 * @author Ilayaperumal Gopinathan
 * @author Alexandros Pappas
 * @author Soby Chacko
 * @author Jonghoon Park
 * @author shiyu
 * @see ChatModel
 * @see StreamingChatModel
 * @see HigressOpenAiApi
 */
public class HigressOpenAiChatModel implements ChatModel {

	private static final Logger logger = LoggerFactory.getLogger(HigressOpenAiChatModel.class);

	private static final ChatModelObservationConvention DEFAULT_OBSERVATION_CONVENTION = new DefaultChatModelObservationConvention();

	private static final ToolCallingManager DEFAULT_TOOL_CALLING_MANAGER = ToolCallingManager.builder().build();

	/**
	 * The default options used for the chat completion requests.
	 */
	private final HigressOpenAiChatOptions defaultOptions;

	/**
	 * The retry template used to retry the OpenAI API calls.
	 */
	private final RetryTemplate retryTemplate;

	/**
	 * Low-level access to the OpenAI API.
	 */
	private final HigressOpenAiApi higressOpenAiApi;

	/**
	 * Observation registry used for instrumentation.
	 */
	private final ObservationRegistry observationRegistry;

	private final ToolCallingManager toolCallingManager;

	/**
	 * The tool execution eligibility predicate used to determine if a tool can be
	 * executed.
	 */
	private final ToolExecutionEligibilityPredicate toolExecutionEligibilityPredicate;

	/**
	 * Conventions to use for generating observations.
	 */
	private ChatModelObservationConvention observationConvention = DEFAULT_OBSERVATION_CONVENTION;

	public HigressOpenAiChatModel(HigressOpenAiApi higressOpenAiApi, HigressOpenAiChatOptions defaultOptions,
			ToolCallingManager toolCallingManager, RetryTemplate retryTemplate,
			ObservationRegistry observationRegistry) {
		this(higressOpenAiApi, defaultOptions, toolCallingManager, retryTemplate, observationRegistry,
				new DefaultToolExecutionEligibilityPredicate());
	}

	public HigressOpenAiChatModel(HigressOpenAiApi higressOpenAiApi, HigressOpenAiChatOptions defaultOptions,
			ToolCallingManager toolCallingManager, RetryTemplate retryTemplate, ObservationRegistry observationRegistry,
			ToolExecutionEligibilityPredicate toolExecutionEligibilityPredicate) {
		Assert.notNull(higressOpenAiApi, "higressOpenAiApi cannot be null");
		Assert.notNull(defaultOptions, "defaultOptions cannot be null");
		Assert.notNull(toolCallingManager, "toolCallingManager cannot be null");
		Assert.notNull(retryTemplate, "retryTemplate cannot be null");
		Assert.notNull(observationRegistry, "observationRegistry cannot be null");
		Assert.notNull(toolExecutionEligibilityPredicate, "toolExecutionEligibilityPredicate cannot be null");
		this.higressOpenAiApi = higressOpenAiApi;
		this.defaultOptions = defaultOptions;
		this.toolCallingManager = toolCallingManager;
		this.retryTemplate = retryTemplate;
		this.observationRegistry = observationRegistry;
		this.toolExecutionEligibilityPredicate = toolExecutionEligibilityPredicate;
	}

	@Override
	public ChatResponse call(Prompt prompt) {
		// Before moving any further, build the final request Prompt,
		// merging runtime and default options.
		Prompt requestPrompt = buildRequestPrompt(prompt);
		return this.internalCall(requestPrompt, null);
	}

	public ChatResponse internalCall(Prompt prompt, ChatResponse previousChatResponse) {

		ChatCompletionRequest request = createRequest(prompt, false);

		ChatModelObservationContext observationContext = ChatModelObservationContext.builder()
			.prompt(prompt)
			.provider(OpenAiApiConstants.PROVIDER_NAME)
			.build();

		ChatResponse response = ChatModelObservationDocumentation.CHAT_MODEL_OPERATION
			.observation(this.observationConvention, DEFAULT_OBSERVATION_CONVENTION, () -> observationContext,
					this.observationRegistry)
			.observe(() -> {
				ResponseEntity<ChatCompletion> completionEntity = this.retryTemplate.execute(
						ctx -> this.higressOpenAiApi.chatCompletionEntity(request, getAdditionalHttpHeaders(prompt)));

				var chatCompletion = completionEntity.getBody();

				if (chatCompletion == null) {
					logger.warn("No chat completion returned for prompt: {}", prompt);
					return new ChatResponse(List.of());
				}

				List<Choice> choices = chatCompletion.choices();
				if (choices == null) {
					logger.warn("No choices returned for prompt: {}", prompt);
					return new ChatResponse(List.of());
				}

			// @formatter:off
				List<Generation> generations = choices.stream().map(choice -> {
					Map<String, Object> metadata = Map.of(
							"id", chatCompletion.id() != null ? chatCompletion.id() : "",
							"role", choice.message().role() != null ? choice.message().role().name() : "",
							"index", choice.index() != null ? choice.index() : 0,
							"finishReason", choice.finishReason() != null ? choice.finishReason().name() : "",
							"refusal", StringUtils.hasText(choice.message().refusal()) ? choice.message().refusal() : "",
							"annotations", choice.message().annotations() != null ? choice.message().annotations() : List.of(Map.of()),
                            //<font color="red"></font>
                            "reasoningContent", StringUtils.hasText(choice.message().reasoningContent()) ? choice.message().reasoningContent() : "");
					return buildGeneration(choice, metadata, request);
				}).toList();
				// @formatter:on

				RateLimit rateLimit = OpenAiResponseHeaderExtractor.extractAiResponseHeaders(completionEntity);

				// Current usage
				HigressOpenAiApi.Usage usage = chatCompletion.usage();
				Usage currentChatResponseUsage = usage != null ? getDefaultUsage(usage) : new EmptyUsage();
				Usage accumulatedUsage = UsageCalculator.getCumulativeUsage(currentChatResponseUsage,
						previousChatResponse);
				ChatResponse chatResponse = new ChatResponse(generations,
						from(chatCompletion, rateLimit, accumulatedUsage));

				observationContext.setResponse(chatResponse);

				return chatResponse;

			});
		if (this.toolExecutionEligibilityPredicate.isToolExecutionRequired(prompt.getOptions(), response)) {
			var toolExecutionResult = this.toolCallingManager.executeToolCalls(prompt, response);
			if (toolExecutionResult.returnDirect()) {
				// Return tool execution result directly to the client.
				return ChatResponse.builder()
					.from(response)
					.generations(ToolExecutionResult.buildGenerations(toolExecutionResult))
					.build();
			}
			else {
				// Send the tool execution result back to the model.
				return this.internalCall(new Prompt(toolExecutionResult.conversationHistory(), prompt.getOptions()),
						response);
			}
		}
		// ReactorMdcPropagation.disable();
		return response;
	}

	@Override
	public Flux<ChatResponse> stream(Prompt prompt) {
		// Before moving any further, build the final request Prompt,
		// merging runtime and default options.
		Prompt requestPrompt = buildRequestPrompt(prompt);
		return internalStream(requestPrompt, null);
	}

	public Flux<ChatResponse> internalStream(Prompt prompt, ChatResponse previousChatResponse) {
		return Flux.deferContextual(contextView -> {
			ChatCompletionRequest request = createRequest(prompt, true);

			if (request.outputModalities() != null
					&& request.outputModalities().contains(HigressOpenAiApi.OutputModality.AUDIO)) {
				logger.warn("Audio output is not supported for streaming requests. Removing audio output.");
				throw new IllegalArgumentException("Audio output is not supported for streaming requests.");
			}

			if (request.audioParameters() != null) {
				logger.warn("Audio parameters are not supported for streaming requests. Removing audio parameters.");
				throw new IllegalArgumentException("Audio parameters are not supported for streaming requests.");
			}

			Flux<HigressOpenAiApi.ChatCompletionChunk> completionChunks = this.higressOpenAiApi
				.chatCompletionStream(request, getAdditionalHttpHeaders(prompt));

			// For chunked responses, only the first chunk contains the choice role.
			// The rest of the chunks with same ID share the same role.
			ConcurrentHashMap<String, String> roleMap = new ConcurrentHashMap<>();

			final ChatModelObservationContext observationContext = ChatModelObservationContext.builder()
				.prompt(prompt)
				.provider(OpenAiApiConstants.PROVIDER_NAME)
				.build();

			Observation observation = ChatModelObservationDocumentation.CHAT_MODEL_OPERATION.observation(
					this.observationConvention, DEFAULT_OBSERVATION_CONVENTION, () -> observationContext,
					this.observationRegistry);

			observation.parentObservation(contextView.getOrDefault(ObservationThreadLocalAccessor.KEY, null)).start();

			// Convert the ChatCompletionChunk into a ChatCompletion to be able to reuse
			// the function call handling logic.
			Flux<ChatResponse> chatResponse = completionChunks.map(this::chunkToChatCompletion)
				.switchMap(chatCompletion -> Mono.just(chatCompletion).map(chatCompletion2 -> {
					try {
						// If an id is not provided, set to "NO_ID" (for compatible APIs).
						String id = chatCompletion2.id() == null ? "NO_ID" : chatCompletion2.id();

						List<Generation> generations = chatCompletion2.choices().stream().map(choice -> { // @formatter:off
							if (choice.message().role() != null) {
								roleMap.putIfAbsent(id, choice.message().role().name());
							}
							Map<String, Object> metadata = Map.of(
									"id", id,
									"role", roleMap.getOrDefault(id, ""),
									"index", choice.index() != null ? choice.index() : 0,
									"finishReason", choice.finishReason() != null ? choice.finishReason().name() : "",
									"refusal", StringUtils.hasText(choice.message().refusal()) ? choice.message().refusal() : "",
									"annotations", choice.message().annotations() != null ? choice.message().annotations() : List.of(),
                                    "reasoningContent", StringUtils.hasText(choice.message().reasoningContent()) ? choice.message().reasoningContent() : ""
                            );
							return buildGeneration(choice, metadata, request);
						}).toList();
						// @formatter:on
						HigressOpenAiApi.Usage usage = chatCompletion2.usage();
						Usage currentChatResponseUsage = usage != null ? getDefaultUsage(usage) : new EmptyUsage();
						Usage accumulatedUsage = UsageCalculator.getCumulativeUsage(currentChatResponseUsage,
								previousChatResponse);
						return new ChatResponse(generations, from(chatCompletion2, null, accumulatedUsage));
					}
					catch (Exception e) {
						logger.error("Error processing chat completion", e);
						return new ChatResponse(List.of());
					}
					// When in stream mode and enabled to include the usage, the OpenAI
					// Chat completion response would have the usage set only in its
					// final response. Hence, the following overlapping buffer is
					// created to store both the current and the subsequent response
					// to accumulate the usage from the subsequent response.
				}))
				.buffer(2, 1)
				.map(bufferList -> {
					ChatResponse firstResponse = bufferList.get(0);
					if (request.streamOptions() != null && request.streamOptions().includeUsage()) {
						if (bufferList.size() == 2) {
							ChatResponse secondResponse = bufferList.get(1);
							if (secondResponse != null && secondResponse.getMetadata() != null) {
								// This is the usage from the final Chat response for a
								// given Chat request.
								Usage usage = secondResponse.getMetadata().getUsage();
								if (!UsageCalculator.isEmpty(usage)) {
									// Store the usage from the final response to the
									// penultimate response for accumulation.
									return new ChatResponse(firstResponse.getResults(),
											from(firstResponse.getMetadata(), usage));
								}
							}
						}
					}
					return firstResponse;
				});

			// @formatter:off
			Flux<ChatResponse> flux = chatResponse.flatMap(response -> {
				if (this.toolExecutionEligibilityPredicate.isToolExecutionRequired(prompt.getOptions(), response)) {
					return Flux.defer(() -> {

						// FIXME: bounded elastic needs to be used since tool calling
						//  is currently only synchronous
                        //ReactorMdcPropagation.enable();
						var toolExecutionResult = this.toolCallingManager.executeToolCalls(prompt, response);
						if (toolExecutionResult.returnDirect()) {
							// Return tool execution result directly to the client.
							return Flux.just(ChatResponse.builder().from(response)
									.generations(ToolExecutionResult.buildGenerations(toolExecutionResult))
									.build());
						}
						else {
							// Send the tool execution result back to the model.
							return this.internalStream(new Prompt(toolExecutionResult.conversationHistory(), prompt.getOptions()),
									response);
						}
					}).subscribeOn(Schedulers.boundedElastic());
				}
				else {
					return Flux.just(response);
				}
			})
			.doOnError(observation::error)
			.doFinally(s -> observation.stop())
			.contextWrite(ctx -> ctx.put(ObservationThreadLocalAccessor.KEY, observation));
			// @formatter:on
			// ReactorMdcPropagation.disable();
			return new MessageAggregator().aggregate(flux, observationContext::setResponse);

		});
	}

	private MultiValueMap<String, String> getAdditionalHttpHeaders(Prompt prompt) {

		Map<String, String> headers = new HashMap<>(this.defaultOptions.getHttpHeaders());
		if (prompt.getOptions() != null && prompt.getOptions() instanceof HigressOpenAiChatOptions chatOptions) {
			headers.putAll(chatOptions.getHttpHeaders());
		}

		// 从 headers 中，生成签名串
		// 根据签名串，调用签名

		return CollectionUtils.toMultiValueMap(
				headers.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> List.of(e.getValue()))));
	}

	private Generation buildGeneration(Choice choice, Map<String, Object> metadata, ChatCompletionRequest request) {
		List<AssistantMessage.ToolCall> toolCalls = choice.message().toolCalls() == null ? List.of()
				: choice.message()
					.toolCalls()
					.stream()
					.map(toolCall -> new AssistantMessage.ToolCall(toolCall.id(), "function",
							toolCall.function().name(), toolCall.function().arguments()))
					.toList();

		String finishReason = (choice.finishReason() != null ? choice.finishReason().name() : "");
		var generationMetadataBuilder = ChatGenerationMetadata.builder().finishReason(finishReason);

		List<Media> media = new ArrayList<>();
		String textContent = choice.message().content();
		var audioOutput = choice.message().audioOutput();
		if (audioOutput != null) {
			String mimeType = String.format("audio/%s", request.audioParameters().format().name().toLowerCase());
			byte[] audioData = Base64.getDecoder().decode(audioOutput.data());
			Resource resource = new ByteArrayResource(audioData);
			Media.builder().mimeType(MimeTypeUtils.parseMimeType(mimeType)).data(resource).id(audioOutput.id()).build();
			media.add(Media.builder()
				.mimeType(MimeTypeUtils.parseMimeType(mimeType))
				.data(resource)
				.id(audioOutput.id())
				.build());
			if (!StringUtils.hasText(textContent)) {
				textContent = audioOutput.transcript();
			}
			generationMetadataBuilder.metadata("audioId", audioOutput.id());
			generationMetadataBuilder.metadata("audioExpiresAt", audioOutput.expiresAt());
		}

		if (Boolean.TRUE.equals(request.logprobs())) {
			generationMetadataBuilder.metadata("logprobs", choice.logprobs());
		}

		var assistantMessage = new AssistantMessage(textContent, metadata, toolCalls, media);
		return new Generation(assistantMessage, generationMetadataBuilder.build());
	}

	private ChatResponseMetadata from(ChatCompletion result, RateLimit rateLimit, Usage usage) {
		Assert.notNull(result, "OpenAI ChatCompletionResult must not be null");
		var builder = ChatResponseMetadata.builder()
			.id(result.id() != null ? result.id() : "")
			.usage(usage)
			.model(result.model() != null ? result.model() : "")
			.keyValue("created", result.created() != null ? result.created() : 0L)
			.keyValue("system-fingerprint", result.systemFingerprint() != null ? result.systemFingerprint() : "");

		if (rateLimit != null) {
			builder.rateLimit(rateLimit);
		}
		return builder.build();
	}

	private ChatResponseMetadata from(ChatResponseMetadata chatResponseMetadata, Usage usage) {
		Assert.notNull(chatResponseMetadata, "OpenAI ChatResponseMetadata must not be null");
		var builder = ChatResponseMetadata.builder()
			.id(chatResponseMetadata.getId() != null ? chatResponseMetadata.getId() : "")
			.usage(usage)
			.model(chatResponseMetadata.getModel() != null ? chatResponseMetadata.getModel() : "");
		if (chatResponseMetadata.getRateLimit() != null) {
			builder.rateLimit(chatResponseMetadata.getRateLimit());
		}
		return builder.build();
	}

	/**
	 * Convert the ChatCompletionChunk into a ChatCompletion. The Usage is set to null.
	 * @param chunk the ChatCompletionChunk to convert
	 * @return the ChatCompletion
	 */
	private ChatCompletion chunkToChatCompletion(HigressOpenAiApi.ChatCompletionChunk chunk) {
		List<Choice> choices = chunk.choices()
			.stream()
			.map(chunkChoice -> new Choice(chunkChoice.finishReason(), chunkChoice.index(), chunkChoice.delta(),
					chunkChoice.logprobs()))
			.toList();

		return new ChatCompletion(chunk.id(), choices, chunk.created(), chunk.model(), chunk.serviceTier(),
				chunk.systemFingerprint(), "chat.completion", chunk.usage());
	}

	private DefaultUsage getDefaultUsage(HigressOpenAiApi.Usage usage) {
		return new DefaultUsage(usage.promptTokens(), usage.completionTokens(), usage.totalTokens(), usage);
	}

	Prompt buildRequestPrompt(Prompt prompt) {
		// Process runtime options
		HigressOpenAiChatOptions runtimeOptions = null;
		if (prompt.getOptions() != null) {
			if (prompt.getOptions() instanceof ToolCallingChatOptions toolCallingChatOptions) {
				runtimeOptions = ModelOptionsUtils.copyToTarget(toolCallingChatOptions, ToolCallingChatOptions.class,
						HigressOpenAiChatOptions.class);
			}
			else {
				runtimeOptions = ModelOptionsUtils.copyToTarget(prompt.getOptions(), ChatOptions.class,
						HigressOpenAiChatOptions.class);
			}
		}

		// Define request options by merging runtime options and default options
		HigressOpenAiChatOptions requestOptions = ModelOptionsUtils.merge(runtimeOptions, this.defaultOptions,
				HigressOpenAiChatOptions.class);

		// Merge @JsonIgnore-annotated options explicitly since they are ignored by
		// Jackson, used by ModelOptionsUtils.
		if (runtimeOptions != null) {
			if (runtimeOptions.getTopK() != null) {
				logger.warn("The topK option is not supported by OpenAI chat models. Ignoring.");
			}

			requestOptions.setHttpHeaders(
					mergeHttpHeaders(runtimeOptions.getHttpHeaders(), this.defaultOptions.getHttpHeaders()));
			requestOptions.setInternalToolExecutionEnabled(
					ModelOptionsUtils.mergeOption(runtimeOptions.getInternalToolExecutionEnabled(),
							this.defaultOptions.getInternalToolExecutionEnabled()));
			requestOptions.setToolNames(ToolCallingChatOptions.mergeToolNames(runtimeOptions.getToolNames(),
					this.defaultOptions.getToolNames()));
			requestOptions.setToolCallbacks(ToolCallingChatOptions.mergeToolCallbacks(runtimeOptions.getToolCallbacks(),
					this.defaultOptions.getToolCallbacks()));
			requestOptions.setToolContext(ToolCallingChatOptions.mergeToolContext(runtimeOptions.getToolContext(),
					this.defaultOptions.getToolContext()));
		}
		else {
			requestOptions.setHttpHeaders(this.defaultOptions.getHttpHeaders());
			requestOptions.setInternalToolExecutionEnabled(this.defaultOptions.getInternalToolExecutionEnabled());
			requestOptions.setToolNames(this.defaultOptions.getToolNames());
			requestOptions.setToolCallbacks(this.defaultOptions.getToolCallbacks());
			requestOptions.setToolContext(this.defaultOptions.getToolContext());
		}

		ToolCallingChatOptions.validateToolCallbacks(requestOptions.getToolCallbacks());

		return new Prompt(prompt.getInstructions(), requestOptions);
	}

	private Map<String, String> mergeHttpHeaders(Map<String, String> runtimeHttpHeaders,
			Map<String, String> defaultHttpHeaders) {
		var mergedHttpHeaders = new HashMap<>(defaultHttpHeaders);
		mergedHttpHeaders.putAll(runtimeHttpHeaders);
		return mergedHttpHeaders;
	}

	/**
	 * Accessible for testing.
	 */
	ChatCompletionRequest createRequest(Prompt prompt, boolean stream) {

		List<ChatCompletionMessage> chatCompletionMessages = prompt.getInstructions().stream().map(message -> {
			if (message.getMessageType() == MessageType.USER || message.getMessageType() == MessageType.SYSTEM) {
				Object content = message.getText();
				if (message instanceof UserMessage userMessage) {
					if (!CollectionUtils.isEmpty(userMessage.getMedia())) {
						List<MediaContent> contentList = new ArrayList<>(List.of(new MediaContent(message.getText())));

						contentList.addAll(userMessage.getMedia().stream().map(this::mapToMediaContent).toList());

						content = contentList;
					}
				}

				return List.of(new ChatCompletionMessage(content,
						ChatCompletionMessage.Role.valueOf(message.getMessageType().name())));
			}
			else if (message.getMessageType() == MessageType.ASSISTANT) {
				var assistantMessage = (AssistantMessage) message;
				List<ToolCall> toolCalls = null;
				if (!CollectionUtils.isEmpty(assistantMessage.getToolCalls())) {
					toolCalls = assistantMessage.getToolCalls().stream().map(toolCall -> {
						var function = new ChatCompletionFunction(toolCall.name(), toolCall.arguments());
						return new ToolCall(toolCall.id(), toolCall.type(), function);
					}).toList();
				}
				AudioOutput audioOutput = null;
				if (!CollectionUtils.isEmpty(assistantMessage.getMedia())) {
					Assert.isTrue(assistantMessage.getMedia().size() == 1,
							"Only one media content is supported for assistant messages");
					audioOutput = new AudioOutput(assistantMessage.getMedia().get(0).getId(), null, null, null);

				}
				return List.of(new ChatCompletionMessage(assistantMessage.getText(),
						ChatCompletionMessage.Role.ASSISTANT, null, null, toolCalls, null, audioOutput, null, ""));
			}
			else if (message.getMessageType() == MessageType.TOOL) {
				ToolResponseMessage toolMessage = (ToolResponseMessage) message;

				toolMessage.getResponses()
					.forEach(response -> Assert.isTrue(response.id() != null, "ToolResponseMessage must have an id"));
				return toolMessage.getResponses()
					.stream()
					.map(tr -> new ChatCompletionMessage(tr.responseData(), ChatCompletionMessage.Role.TOOL, tr.name(),
							tr.id(), null, null, null, null, ""))
					.toList();
			}
			else {
				throw new IllegalArgumentException("Unsupported message type: " + message.getMessageType());
			}
		}).flatMap(List::stream).toList();

		ChatCompletionRequest request = new ChatCompletionRequest(chatCompletionMessages, stream);

		HigressOpenAiChatOptions requestOptions = (HigressOpenAiChatOptions) prompt.getOptions();
		request = ModelOptionsUtils.merge(requestOptions, request, ChatCompletionRequest.class);

		// Add the tool definitions to the request's tools parameter.
		List<ToolDefinition> toolDefinitions = this.toolCallingManager.resolveToolDefinitions(requestOptions);
		if (!CollectionUtils.isEmpty(toolDefinitions)) {
			request = ModelOptionsUtils.merge(
					HigressOpenAiChatOptions.builder().tools(this.getFunctionTools(toolDefinitions)).build(), request,
					ChatCompletionRequest.class);
		}

		// Remove `streamOptions` from the request if it is not a streaming request
		if (request.streamOptions() != null && !stream) {
			logger.warn("Removing streamOptions from the request as it is not a streaming request!");
			request = request.streamOptions(null);
		}

		return request;
	}

	private MediaContent mapToMediaContent(Media media) {
		var mimeType = media.getMimeType();
		if (MimeTypeUtils.parseMimeType("audio/mp3").equals(mimeType)) {
			return new MediaContent(
					new MediaContent.InputAudio(fromAudioData(media.getData()), MediaContent.InputAudio.Format.MP3));
		}
		if (MimeTypeUtils.parseMimeType("audio/wav").equals(mimeType)) {
			return new MediaContent(
					new MediaContent.InputAudio(fromAudioData(media.getData()), MediaContent.InputAudio.Format.WAV));
		}
		if (MimeTypeUtils.parseMimeType("application/pdf").equals(mimeType)) {
			return new MediaContent(new MediaContent.InputFile(media.getName(),
					this.fromMediaData(media.getMimeType(), media.getData())));
		}
		else {
			return new MediaContent(
					new MediaContent.ImageUrl(this.fromMediaData(media.getMimeType(), media.getData())));
		}
	}

	private String fromAudioData(Object audioData) {
		if (audioData instanceof byte[] bytes) {
			return Base64.getEncoder().encodeToString(bytes);
		}
		throw new IllegalArgumentException("Unsupported audio data type: " + audioData.getClass().getSimpleName());
	}

	private String fromMediaData(MimeType mimeType, Object mediaContentData) {
		if (mediaContentData instanceof byte[] bytes) {
			// Assume the bytes are an image. So, convert the bytes to a base64 encoded
			// following the prefix pattern.
			return String.format("data:%s;base64,%s", mimeType.toString(), Base64.getEncoder().encodeToString(bytes));
		}
		else if (mediaContentData instanceof String text) {
			// Assume the text is a URLs or a base64 encoded image prefixed by the user.
			return text;
		}
		else {
			throw new IllegalArgumentException(
					"Unsupported media data type: " + mediaContentData.getClass().getSimpleName());
		}
	}

	private List<HigressOpenAiApi.FunctionTool> getFunctionTools(List<ToolDefinition> toolDefinitions) {
		return toolDefinitions.stream().map(toolDefinition -> {
			var function = new HigressOpenAiApi.FunctionTool.Function(toolDefinition.description(),
					toolDefinition.name(), toolDefinition.inputSchema());
			return new HigressOpenAiApi.FunctionTool(function);
		}).toList();
	}

	@Override
	public ChatOptions getDefaultOptions() {
		return HigressOpenAiChatOptions.fromOptions(this.defaultOptions);
	}

	@Override
	public String toString() {
		return "HigressOpenAiChatModel [defaultOptions=" + this.defaultOptions + "]";
	}

	/**
	 * Use the provided convention for reporting observation data
	 * @param observationConvention The provided convention
	 */
	public void setObservationConvention(ChatModelObservationConvention observationConvention) {
		Assert.notNull(observationConvention, "observationConvention cannot be null");
		this.observationConvention = observationConvention;
	}

	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Returns a builder pre-populated with the current configuration for mutation.
	 */
	public Builder mutate() {
		return new Builder(this);
	}

	@Override
	public HigressOpenAiChatModel clone() {
		return this.mutate().build();
	}

	public static final class Builder {

		// Copy constructor for mutate()
		public Builder(HigressOpenAiChatModel model) {
			this.higressOpenAiApi = model.higressOpenAiApi;
			this.defaultOptions = model.defaultOptions;
			this.toolCallingManager = model.toolCallingManager;
			this.toolExecutionEligibilityPredicate = model.toolExecutionEligibilityPredicate;
			this.retryTemplate = model.retryTemplate;
			this.observationRegistry = model.observationRegistry;
		}

		private HigressOpenAiApi higressOpenAiApi;

		private HigressOpenAiChatOptions defaultOptions = HigressOpenAiChatOptions.builder()
			.model(HigressOpenAiApi.DEFAULT_CHAT_MODEL)
			.temperature(0.7)
			.build();

		private ToolCallingManager toolCallingManager;

		private ToolExecutionEligibilityPredicate toolExecutionEligibilityPredicate = new DefaultToolExecutionEligibilityPredicate();

		private RetryTemplate retryTemplate = RetryUtils.DEFAULT_RETRY_TEMPLATE;

		private ObservationRegistry observationRegistry = ObservationRegistry.NOOP;

		private Builder() {
		}

		public Builder higressOpenAiApi(HigressOpenAiApi higressOpenAiApi) {
			this.higressOpenAiApi = higressOpenAiApi;
			return this;
		}

		public Builder defaultOptions(HigressOpenAiChatOptions defaultOptions) {
			this.defaultOptions = defaultOptions;
			return this;
		}

		public Builder toolCallingManager(ToolCallingManager toolCallingManager) {
			this.toolCallingManager = toolCallingManager;
			return this;
		}

		public Builder toolExecutionEligibilityPredicate(
				ToolExecutionEligibilityPredicate toolExecutionEligibilityPredicate) {
			this.toolExecutionEligibilityPredicate = toolExecutionEligibilityPredicate;
			return this;
		}

		public Builder retryTemplate(RetryTemplate retryTemplate) {
			this.retryTemplate = retryTemplate;
			return this;
		}

		public Builder observationRegistry(ObservationRegistry observationRegistry) {
			this.observationRegistry = observationRegistry;
			return this;
		}

		public HigressOpenAiChatModel build() {
			if (this.toolCallingManager != null) {
				return new HigressOpenAiChatModel(this.higressOpenAiApi, this.defaultOptions, this.toolCallingManager,
						this.retryTemplate, this.observationRegistry, this.toolExecutionEligibilityPredicate);
			}
			return new HigressOpenAiChatModel(this.higressOpenAiApi, this.defaultOptions, DEFAULT_TOOL_CALLING_MANAGER,
					this.retryTemplate, this.observationRegistry, this.toolExecutionEligibilityPredicate);
		}

	}

}
