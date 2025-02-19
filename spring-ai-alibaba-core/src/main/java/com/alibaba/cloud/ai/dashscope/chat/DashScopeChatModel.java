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

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.ChatCompletion;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.ChatCompletionChunk;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.ChatCompletionFinishReason;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.ChatCompletionMessage;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.ChatCompletionMessage.ChatCompletionFunction;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.ChatCompletionMessage.MediaContent;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.ChatCompletionMessage.ToolCall;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.ChatCompletionOutput;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.ChatCompletionOutput.Choice;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.ChatCompletionRequest;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.ChatCompletionRequestInput;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.ChatCompletionRequestParameter;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.FunctionTool;
import com.alibaba.cloud.ai.dashscope.chat.observation.DashScopeChatModelObservationConvention;
import com.alibaba.cloud.ai.dashscope.common.DashScopeApiConstants;
import com.alibaba.cloud.ai.dashscope.metadata.DashScopeAiUsage;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.model.AbstractToolCallSupport;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.MessageAggregator;
import org.springframework.ai.chat.observation.ChatModelObservationContext;
import org.springframework.ai.chat.observation.ChatModelObservationConvention;
import org.springframework.ai.chat.observation.ChatModelObservationDocumentation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.model.function.FunctionCallbackResolver;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MimeType;

/**
 * {@link ChatModel} implementation for {@literal Alibaba DashScope} backed by
 * {@link Generation}.
 *
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 * @see ChatModel
 * @see com.alibaba.dashscope.aigc.generation
 */
public class DashScopeChatModel extends AbstractToolCallSupport implements ChatModel {

	public static final String MESSAGE_FORMAT = "messageFormat";

	private static final Logger logger = LoggerFactory.getLogger(DashScopeChatModel.class);

	private static final ChatModelObservationConvention DEFAULT_OBSERVATION_CONVENTION = new DashScopeChatModelObservationConvention();

	/** Low-level access to the DashScope API */
	private final DashScopeApi dashscopeApi;

	/** The retry template used to retry the OpenAI API calls. */
	public final RetryTemplate retryTemplate;

	/**
	 * Observation registry used for instrumentation.
	 */
	private final ObservationRegistry observationRegistry;

	/** The default options used for the chat completion requests. */
	private DashScopeChatOptions defaultOptions;

	/**
	 * Conventions to use for generating observations.
	 */
	private ChatModelObservationConvention observationConvention = DEFAULT_OBSERVATION_CONVENTION;

	public DashScopeChatModel(DashScopeApi dashscopeApi) {
		this(dashscopeApi,
				DashScopeChatOptions.builder()
					.withModel(DashScopeApi.DEFAULT_CHAT_MODEL)
					.withTemperature(0.7d)
					.build());
	}

	public DashScopeChatModel(DashScopeApi dashscopeApi, DashScopeChatOptions options) {
		this(dashscopeApi, options, null, RetryUtils.DEFAULT_RETRY_TEMPLATE);
	}

	public DashScopeChatModel(DashScopeApi dashscopeApi, DashScopeChatOptions options,
			FunctionCallbackResolver functionCallbackResolver, RetryTemplate retryTemplate) {
		this(dashscopeApi, options, functionCallbackResolver, List.of(), retryTemplate);
	}

	public DashScopeChatModel(DashScopeApi dashscopeApi, DashScopeChatOptions options,
			FunctionCallbackResolver functionCallbackResolver, List<FunctionCallback> toolFunctionCallbacks,
			RetryTemplate retryTemplate) {

		this(dashscopeApi, options, functionCallbackResolver, toolFunctionCallbacks, retryTemplate,
				ObservationRegistry.NOOP);
	}

	public DashScopeChatModel(DashScopeApi dashscopeApi, DashScopeChatOptions options,
			FunctionCallbackResolver functionCallbackResolver, List<FunctionCallback> toolFunctionCallbacks,
			RetryTemplate retryTemplate, ObservationRegistry observationRegistry) {

		super(functionCallbackResolver, options, toolFunctionCallbacks);

		Assert.notNull(dashscopeApi, "DashScopeApi must not be null");
		Assert.notNull(options, "Options must not be null");
		Assert.notNull(retryTemplate, "RetryTemplate must not be null");
		Assert.isTrue(CollectionUtils.isEmpty(options.getFunctionCallbacks()),
				"The default function callbacks must be set via the toolFunctionCallbacks constructor parameter");
		Assert.notNull(observationRegistry, "ObservationRegistry must not be null");

		this.dashscopeApi = dashscopeApi;
		this.defaultOptions = options;
		this.retryTemplate = retryTemplate;
		this.observationRegistry = observationRegistry;
	}

	@Override
	public ChatResponse call(Prompt prompt) {

		ChatModelObservationContext observationContext = ChatModelObservationContext.builder()
			.prompt(prompt)
			.provider(DashScopeApiConstants.PROVIDER_NAME)
			.requestOptions(prompt.getOptions() != null ? prompt.getOptions() : this.defaultOptions)
			.build();

		ChatResponse chatResponse = ChatModelObservationDocumentation.CHAT_MODEL_OPERATION
			.observation(this.observationConvention, DEFAULT_OBSERVATION_CONVENTION, () -> observationContext,
					this.observationRegistry)
			.observe(() -> {
				DashScopeApi.ChatCompletionRequest request = createRequest(prompt, false);

				ResponseEntity<ChatCompletion> completionEntity = this.retryTemplate
					.execute(ctx -> this.dashscopeApi.chatCompletionEntity(request));

				var chatCompletion = completionEntity.getBody();

				if (chatCompletion == null) {
					logger.warn("No chat completion returned for prompt: {}", prompt);
					return new ChatResponse(List.of());
				}

				List<ChatCompletionOutput.Choice> choices = chatCompletion.output().choices();

				List<Generation> generations = choices.stream().map(choice -> {
			// @formatter:off
						Map<String, Object> metadata = Map.of(
								"id", chatCompletion.requestId(),
								"role", choice.message().role() != null ? choice.message().role().name() : "",
								"finishReason", choice.finishReason() != null ? choice.finishReason().name() : "");
						// @formatter:on
					return buildGeneration(choice, metadata);
				}).toList();

				ChatResponse response = new ChatResponse(generations, from(completionEntity.getBody()));

				observationContext.setResponse(response);

				return response;
			});

		if (!isProxyToolCalls(prompt, this.defaultOptions) && isToolCall(chatResponse,
				Set.of(ChatCompletionFinishReason.TOOL_CALLS.name(), ChatCompletionFinishReason.STOP.name()))) {
			var toolCallConversation = handleToolCalls(prompt, chatResponse);
			// Recursively call the call method with the tool call message
			// conversation that contains the call responses.
			return this.call(new Prompt(toolCallConversation, prompt.getOptions()));
		}

		return chatResponse;
	}

	@Override
	public ChatOptions getDefaultOptions() {
		return DashScopeChatOptions.fromOptions(this.defaultOptions);
	}

	@Override
	public Flux<ChatResponse> stream(Prompt prompt) {

		return Flux.deferContextual(contextView -> {
			ChatCompletionRequest request = createRequest(prompt, true);

			Flux<ChatCompletionChunk> completionChunks = this.retryTemplate
				.execute(ctx -> this.dashscopeApi.chatCompletionStream(request));

			// For chunked responses, only the first chunk contains the choice role.
			// The rest of the chunks with same ID share the same role.
			ConcurrentHashMap<String, String> roleMap = new ConcurrentHashMap<>();

			ChatModelObservationContext observationContext = ChatModelObservationContext.builder()
				.prompt(prompt)
				.provider(DashScopeApiConstants.PROVIDER_NAME)
				.requestOptions(prompt.getOptions() != null ? prompt.getOptions() : this.defaultOptions)
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
						@SuppressWarnings("null")
						String requestId = chatCompletion2.requestId();

				// @formatter:off
							List<Generation> generations = chatCompletion2.output().choices().stream().map(choice -> {
								if (choice.message().role() != null) {
									roleMap.putIfAbsent(requestId, choice.message().role().name());
								}
								Map<String, Object> metadata = Map.of(
										"id", chatCompletion2.requestId(),
										"role", roleMap.getOrDefault(requestId, ""),
										"finishReason", choice.finishReason() != null ? choice.finishReason().name() : "");
								return buildGeneration(choice, metadata);
							}).toList();
							// @formatter:on

						if (chatCompletion2.usage() != null) {
							return new ChatResponse(generations, from(chatCompletion2));
						}
						else {
							return new ChatResponse(generations);
						}
					}
					catch (Exception e) {
						logger.error("Error processing chat completion", e);
						return new ChatResponse(List.of());
					}

				}));

			// @formatter:off
			Flux<ChatResponse> flux = chatResponse.flatMap(response -> {
				if (!isProxyToolCalls(prompt, this.defaultOptions) &&
						isToolCall(response, Set.of(ChatCompletionFinishReason.TOOL_CALLS.name(), ChatCompletionFinishReason.STOP.name()))) {
					var toolCallConversation = handleToolCalls(prompt, response);
					// Recursively call the stream method with the tool call message
					// conversation that contains the call responses.
					return this.stream(new Prompt(toolCallConversation, prompt.getOptions()));
				}
				else {
					return Flux.just(response);
				}
			})
			.doOnError(observation::error)
			.doFinally(s -> observation.stop())
			.contextWrite(ctx -> ctx.put(ObservationThreadLocalAccessor.KEY, observation));

			return new MessageAggregator().aggregate(flux, observationContext::setResponse);
		});
	}

	private static Generation buildGeneration(Choice choice, Map<String, Object> metadata) {
		List<AssistantMessage.ToolCall> toolCalls = choice.message().toolCalls() == null ? List.of()
				: choice.message()
					.toolCalls()
					.stream()
					.map(toolCall -> new AssistantMessage.ToolCall(toolCall.id(), "function",
							toolCall.function().name(), toolCall.function().arguments()))
					.toList();

		var assistantMessage = new AssistantMessage(choice.message().content(), metadata, toolCalls);
		String finishReason = (choice.finishReason() != null ? choice.finishReason().name() : "");
		var generationMetadata = ChatGenerationMetadata.builder().finishReason(finishReason).build();
		return new Generation(assistantMessage, generationMetadata);
	}

	/**
	 * Convert the ChatCompletionChunk into a ChatCompletion. The Usage is set to null.
	 * @param chunk the ChatCompletionChunk to convert
	 * @return the ChatCompletion
	 */
	private ChatCompletion chunkToChatCompletion(ChatCompletionChunk chunk) {
		return new ChatCompletion(chunk.requestId(),
				new ChatCompletionOutput(chunk.output().text(), chunk.output().choices()), chunk.usage());
	}

	private ChatResponseMetadata from(ChatCompletion result) {
		Assert.notNull(result, "DashScopeAi ChatCompletionResult must not be null");
		return ChatResponseMetadata.builder()
			.id(result.requestId())
			.usage(DashScopeAiUsage.from(result.usage()))
			.model("")
			.build();
	}

	/**
	 * Accessible for testing.
	 */
	ChatCompletionRequest createRequest(Prompt prompt, boolean stream) {
		Set<String> enabledToolsToUse = new HashSet<>();

		DashScopeChatOptions options = DashScopeChatOptions.builder().build();
		if (prompt.getOptions() != null) {
			DashScopeChatOptions updatedRuntimeOptions = ModelOptionsUtils.copyToTarget(prompt.getOptions(),
					ChatOptions.class, DashScopeChatOptions.class);

			enabledToolsToUse.addAll(this.runtimeFunctionCallbackConfigurations(updatedRuntimeOptions));
			options = ModelOptionsUtils.merge(updatedRuntimeOptions, options, DashScopeChatOptions.class);
		}

		if (!CollectionUtils.isEmpty(this.defaultOptions.getFunctions())) {
			enabledToolsToUse.addAll(this.defaultOptions.getFunctions());
		}

		options = ModelOptionsUtils.merge(options, this.defaultOptions, DashScopeChatOptions.class);

		if (!CollectionUtils.isEmpty(enabledToolsToUse)) {
			options.setTools(this.getFunctionTools(enabledToolsToUse));
		}

		List<ChatCompletionMessage> chatCompletionMessages = prompt.getInstructions().stream().map(message -> {
			if (message.getMessageType() == MessageType.USER || message.getMessageType() == MessageType.SYSTEM) {
				Object content = message.getText();
				if (message instanceof UserMessage userMessage) {
					if (!CollectionUtils.isEmpty(userMessage.getMedia())) {
						content = convertMediaContent(userMessage);
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
				return List.of(new ChatCompletionMessage(assistantMessage.getContent(),
						ChatCompletionMessage.Role.ASSISTANT, null, null, toolCalls));
			}
			else if (message.getMessageType() == MessageType.TOOL) {
				ToolResponseMessage toolMessage = (ToolResponseMessage) message;

				toolMessage.getResponses().forEach(response -> {
					Assert.isTrue(response.id() != null, "ToolResponseMessage must have an id");
					Assert.isTrue(response.name() != null, "ToolResponseMessage must have a name");
				});

				return toolMessage.getResponses()
					.stream()
					.map(tr -> new ChatCompletionMessage(tr.responseData(), ChatCompletionMessage.Role.TOOL, tr.name(),
							tr.id(), null))
					.toList();
			}
			else {
				throw new IllegalArgumentException("Unsupported message type: " + message.getMessageType());
			}
		}).flatMap(List::stream).toList();

		boolean multiModel = options.getMultiModel();
		return new ChatCompletionRequest(options.getModel(), new ChatCompletionRequestInput(chatCompletionMessages),
				toDashScopeRequestParameter(options, stream), stream, multiModel);
	}

	private List<MediaContent> convertMediaContent(UserMessage message) {
		MessageFormat format = MessageFormat.IMAGE;
		if (message.getMetadata().get(MESSAGE_FORMAT) instanceof MessageFormat messageFormat) {
			format = messageFormat;
		}

		List<MediaContent> contentList = new ArrayList<>();
		if (format == MessageFormat.VIDEO) {
			MediaContent mediaContent = new MediaContent(message.getContent());
			contentList.add(mediaContent);

			List<String> mediaList = message.getMedia()
				.stream()
				.map(media -> this.fromMediaData(media.getMimeType(), media.getData()))
				.toList();

			contentList.add(new MediaContent("video", null, null, mediaList));
		}
		else {
			MediaContent mediaContent = new MediaContent(message.getContent());
			contentList.add(mediaContent);

			contentList.addAll(message.getMedia()
				.stream()
				.map(media -> new MediaContent("image", null, this.fromMediaData(media.getMimeType(), media.getData()),
						null))
				.toList());
		}

		return contentList;
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

	private List<FunctionTool> getFunctionTools(Set<String> functionNames) {
		return this.resolveFunctionCallbacks(functionNames).stream().map(functionCallback -> {
			var function = new FunctionTool.Function(functionCallback.getDescription(), functionCallback.getName(),
					functionCallback.getInputTypeSchema());
			return new FunctionTool(function);
		}).toList();
	}

	private ChatCompletionRequestParameter toDashScopeRequestParameter(DashScopeChatOptions options, boolean stream) {

		if (options == null) {
			return new ChatCompletionRequestParameter();
		}

		Boolean incrementalOutput = stream && options.getIncrementalOutput();
		return new ChatCompletionRequestParameter(
				"message",
				options.getSeed(),
				options.getMaxTokens(),
				options.getTopP(),
				options.getTopK(),
				options.getRepetitionPenalty(),
				options.getPresencePenalty(),
				options.getTemperature(),
				options.getStop(),
				options.getEnableSearch(),
				options.getResponseFormat(),
				incrementalOutput,
				options.getTools(),
				options.getToolChoice(),
				stream, options.getVlHighResolutionImages()
		);
	}

	/**
	 * Use the provided convention for reporting observation data
	 * @param observationConvention The provided convention
	 */
	public void setObservationConvention(ChatModelObservationConvention observationConvention) {
		Assert.notNull(observationConvention, "observationConvention cannot be null");
		this.observationConvention = observationConvention;
	}

}
