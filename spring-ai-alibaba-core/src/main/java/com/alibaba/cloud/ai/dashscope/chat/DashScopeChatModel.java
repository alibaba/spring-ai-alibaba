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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.ChatCompletion;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.ChatCompletionChunk;
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
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.metadata.EmptyUsage;
import org.springframework.ai.chat.metadata.Usage;
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
import org.springframework.ai.model.tool.DefaultToolExecutionEligibilityPredicate;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionEligibilityPredicate;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.ai.support.UsageCalculator;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MimeType;
import org.springframework.util.StringUtils;

/**
 * {@link ChatModel} implementation for {@literal Alibaba DashScope} backed by
 * {@link Generation}.
 *
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 * @see ChatModel
 */
public class DashScopeChatModel implements ChatModel {

	private static final Logger logger = LoggerFactory.getLogger(DashScopeChatModel.class);

	public static final String DEFAULT_MODEL_NAME = DashScopeApi.DEFAULT_CHAT_MODEL;

	public static final Double DEFAULT_TEMPERATURE = 0.7;

	private static final ChatModelObservationConvention DEFAULT_OBSERVATION_CONVENTION = new DashScopeChatModelObservationConvention();

	private static final ToolCallingManager DEFAULT_TOOL_CALLING_MANAGER = ToolCallingManager.builder().build();

	/**
	 * The default options used for the chat completion requests.
	 */
	private DashScopeChatOptions defaultOptions;

	/**
	 * Low-level access to the DashScope API
	 */
	private final DashScopeApi dashscopeApi;

	/**
	 * The retry template used to retry the OpenAI API calls.
	 */
	public final RetryTemplate retryTemplate;

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

	public DashScopeChatModel(DashScopeApi dashscopeApi, DashScopeChatOptions defaultOptions,
			ToolCallingManager toolCallingManager, RetryTemplate retryTemplate, ObservationRegistry observationRegistry,
			ToolExecutionEligibilityPredicate toolExecutionEligibilityPredicate) {

		Assert.notNull(dashscopeApi, "dashscopeApi cannot be null");
		Assert.notNull(defaultOptions, "defaultOptions cannot be null");
		Assert.notNull(toolCallingManager, "toolCallingManager cannot be null");
		Assert.notNull(retryTemplate, "retryTemplate cannot be null");
		Assert.notNull(observationRegistry, "observationRegistry cannot be null");
		Assert.notNull(toolExecutionEligibilityPredicate, "toolExecutionEligibilityPredicate cannot be null");

		this.dashscopeApi = dashscopeApi;
		this.defaultOptions = defaultOptions;
		this.toolCallingManager = toolCallingManager;
		this.retryTemplate = retryTemplate;
		this.observationRegistry = observationRegistry;
		this.toolExecutionEligibilityPredicate = toolExecutionEligibilityPredicate;
	}

	@Override
	public ChatResponse call(Prompt prompt) {
		Assert.notNull(prompt, "Prompt must not be null");
		Assert.isTrue(!CollectionUtils.isEmpty(prompt.getInstructions()), "Prompt messages must not be empty");
		Prompt requestPrompt = buildRequestPrompt(prompt);
		return internalCall(requestPrompt, null);
	}

	@Override
	public ChatOptions getDefaultOptions() {
		return DashScopeChatOptions.fromOptions(this.defaultOptions);
	}

	public ChatResponse internalCall(Prompt prompt, ChatResponse previousChatResponse) {
		ChatCompletionRequest request = createRequest(prompt, false);

		ChatModelObservationContext observationContext = ChatModelObservationContext.builder()
			.prompt(prompt)
			.provider(DashScopeApiConstants.PROVIDER_NAME)
			.build();

		ChatResponse response = ChatModelObservationDocumentation.CHAT_MODEL_OPERATION
			.observation(this.observationConvention, DEFAULT_OBSERVATION_CONVENTION, () -> observationContext,
					this.observationRegistry)
			.observe(() -> {

				ResponseEntity<ChatCompletion> completionEntity = this.retryTemplate
					.execute(ctx -> dashscopeApi.chatCompletionEntity(request));

				var completionResponse = completionEntity.getBody();

				ChatResponse chatResponse = toChatResponse(completionResponse, previousChatResponse, request, null);

				observationContext.setResponse(chatResponse);

				return chatResponse;
			});

		if (toolExecutionEligibilityPredicate.isToolExecutionRequired(prompt.getOptions(), response)) {
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

		return response;
	}

	@Override
	public Flux<ChatResponse> stream(Prompt prompt) {
		Assert.notNull(prompt, "Prompt must not be null");
		Assert.isTrue(!CollectionUtils.isEmpty(prompt.getInstructions()), "Prompt messages must not be empty");

		// Before moving any further, build the final request Prompt,
		// merging runtime and default options.
		Prompt requestPrompt = buildRequestPrompt(prompt);
		return this.internalStream(requestPrompt, null);
	}

	public Flux<ChatResponse> internalStream(Prompt prompt, ChatResponse previousChatResponse) {
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
				.build();

			Observation observation = ChatModelObservationDocumentation.CHAT_MODEL_OPERATION.observation(
					this.observationConvention, DEFAULT_OBSERVATION_CONVENTION, () -> observationContext,
					this.observationRegistry);

			observation.parentObservation(contextView.getOrDefault(ObservationThreadLocalAccessor.KEY, null)).start();

			// Convert the ChatCompletionChunk into a ChatCompletion to be able to reuse
			// the function call handling logic.
			Flux<ChatResponse> chatResponse = completionChunks.map(this::chunkToChatCompletion)
				.switchMap(chatCompletion -> Mono.just(chatCompletion)
					.map(chatCompletion2 -> toChatResponse(chatCompletion2, previousChatResponse, request, roleMap)));

			// @formatter:off
			Flux<ChatResponse> flux = chatResponse.flatMap(response -> {
					if (toolExecutionEligibilityPredicate.isToolExecutionRequired(prompt.getOptions(), response)) {
						return Flux.defer(
								() -> {
									var toolExecutionResult = this.toolCallingManager.executeToolCalls(prompt, response);
									if (toolExecutionResult.returnDirect()) {
										// Return tool execution result directly to the client.
										return Flux.just(ChatResponse.builder().from(response)
												.generations(ToolExecutionResult.buildGenerations(toolExecutionResult))
												.build());
									} else {
										// Send the tool execution result back to the model.
										return this.internalStream(new Prompt(toolExecutionResult.conversationHistory(), prompt.getOptions()),
												response);
									}
								}
						).subscribeOn(Schedulers.boundedElastic());

					}
					else {
						return Flux.just(response);
					}
				})
				.doOnError(observation::error)
				.doFinally(s -> observation.stop())
				.contextWrite(ctx -> ctx.put(ObservationThreadLocalAccessor.KEY, observation));
			// @formatter:on

			return new MessageAggregator().aggregate(flux, observationContext::setResponse);
		});
	}

	private static String finishReasonToMetadataValue(DashScopeApi.ChatCompletionFinishReason finishReason) {
		if (finishReason == null || finishReason == DashScopeApi.ChatCompletionFinishReason.NULL) {
			return "";
		}
		return finishReason.name();
	}

	private ChatResponse toChatResponse(ChatCompletion chatCompletion, ChatResponse previousChatResponse,
			ChatCompletionRequest request, ConcurrentHashMap<String, String> roleMap) {

		if (chatCompletion == null) {
			logger.warn("Null chat completion returned");
			return new ChatResponse(List.of());
		}

		List<ChatCompletionOutput.Choice> choices = chatCompletion.output().choices();
		if (choices == null) {
			logger.warn("No choices returned");
			return new ChatResponse(List.of());
		}

		ConcurrentHashMap<String, String> finalRoleMap = roleMap == null ? new ConcurrentHashMap<>() : roleMap;

		List<Generation> generations = choices.stream().map(choice -> {

			if (choice.message().role() != null) {
				finalRoleMap.putIfAbsent(chatCompletion.requestId(), choice.message().role().name());
			}

			// @formatter:off
			Map<String, Object> metadata = Map.of(
					"id", chatCompletion.requestId(),
					"role", finalRoleMap.getOrDefault(chatCompletion.requestId(), ""),
					"finishReason", finishReasonToMetadataValue(choice.finishReason()),
					"reasoningContent", StringUtils.hasText(choice.message().reasoningContent()) ? choice.message().reasoningContent() : "");
			// @formatter:on
			return buildGeneration(choice, metadata, request);
		}).toList();

		DashScopeApi.TokenUsage usage = chatCompletion.usage();
		Usage currentChatResponseUsage = usage != null ? this.getDefaultUsage(usage) : new EmptyUsage();
		Usage accumulatedUsage = UsageCalculator.getCumulativeUsage(currentChatResponseUsage, previousChatResponse);

		return new ChatResponse(generations, this.from(chatCompletion, accumulatedUsage));
	}

	public DashScopeChatOptions getDashScopeChatOptions() {
		return this.defaultOptions;
	}

	public void setDashScopeChatOptions(DashScopeChatOptions options) {
		this.defaultOptions = options;
	}

	private static Generation buildGeneration(Choice choice, Map<String, Object> metadata,
			ChatCompletionRequest request) {
		List<AssistantMessage.ToolCall> toolCalls = choice.message().toolCalls() == null ? List.of()
				: choice.message()
					.toolCalls()
					.stream()
					.map(toolCall -> new AssistantMessage.ToolCall(toolCall.id(), "function",
							toolCall.function().name(), toolCall.function().arguments()))
					.toList();

		String finishReason = finishReasonToMetadataValue(choice.finishReason());
		var generationMetadataBuilder = ChatGenerationMetadata.builder().finishReason(finishReason);

		var assistantMessage = new AssistantMessage(choice.message().content(), metadata, toolCalls);
		return new Generation(assistantMessage, generationMetadataBuilder.build());
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

	private ChatResponseMetadata from(ChatCompletion result, Usage usage) {
		Assert.notNull(result, "DashScopeAi ChatCompletionResult must not be null");
		return ChatResponseMetadata.builder().id(result.requestId()).usage(usage).model("").build();
	}

	private DefaultUsage getDefaultUsage(DashScopeApi.TokenUsage usage) {
		return new DefaultUsage(usage.inputTokens(), usage.outputTokens(), usage.inputTokens() + usage.outputTokens(),
				usage);
	}

	Prompt buildRequestPrompt(Prompt prompt) {
		// Process runtime options
		DashScopeChatOptions runtimeOptions = null;
		if (prompt.getOptions() != null) {
			if (prompt.getOptions() instanceof ToolCallingChatOptions toolCallingChatOptions) {
				runtimeOptions = ModelOptionsUtils.copyToTarget(toolCallingChatOptions, ToolCallingChatOptions.class,
						DashScopeChatOptions.class);
			}
			else {
				runtimeOptions = ModelOptionsUtils.copyToTarget(prompt.getOptions(), ChatOptions.class,
						DashScopeChatOptions.class);
			}
		}

		// Define request options by merging runtime options and default options
		DashScopeChatOptions requestOptions = ModelOptionsUtils.merge(runtimeOptions, this.defaultOptions,
				DashScopeChatOptions.class);

		// Merge @JsonIgnore-annotated options explicitly since they are ignored by
		// Jackson, used by ModelOptionsUtils.
		if (runtimeOptions != null) {
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
			requestOptions.setInternalToolExecutionEnabled(this.defaultOptions.getInternalToolExecutionEnabled());
			requestOptions.setToolNames(this.defaultOptions.getToolNames());
			requestOptions.setToolCallbacks(this.defaultOptions.getToolCallbacks());
			requestOptions.setToolContext(this.defaultOptions.getToolContext());
		}

		ToolCallingChatOptions.validateToolCallbacks(requestOptions.getToolCallbacks());

		return new Prompt(prompt.getInstructions(), requestOptions);
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
				return List.of(new ChatCompletionMessage(assistantMessage.getText(),
						ChatCompletionMessage.Role.ASSISTANT, null, null, toolCalls, null));
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
							tr.id(), null, null))
					.toList();
			}
			else {
				throw new IllegalArgumentException("Unsupported message type: " + message.getMessageType());
			}
		}).flatMap(List::stream).toList();

		DashScopeChatOptions requestOptions = (DashScopeChatOptions) prompt.getOptions();

		// Add the tool definitions to the request's tools parameter.
		List<ToolDefinition> toolDefinitions = this.toolCallingManager.resolveToolDefinitions(requestOptions);
		if (!CollectionUtils.isEmpty(toolDefinitions)) {
			requestOptions.setTools(getFunctionTools(toolDefinitions));
		}

		boolean multiModel = requestOptions.getMultiModel();

		return new ChatCompletionRequest(requestOptions.getModel(),
				new ChatCompletionRequestInput(chatCompletionMessages),
				toDashScopeRequestParameter(requestOptions, stream), stream, multiModel);
	}

	private List<MediaContent> convertMediaContent(UserMessage message) {
		MessageFormat format = MessageFormat.IMAGE;
		if (message.getMetadata().get(DashScopeApiConstants.MESSAGE_FORMAT) instanceof MessageFormat messageFormat) {
			format = messageFormat;
		}

		List<MediaContent> contentList = new ArrayList<>();
		if (format == MessageFormat.VIDEO) {
			MediaContent mediaContent = new MediaContent(message.getText());
			contentList.add(mediaContent);

			List<String> mediaList = message.getMedia()
				.stream()
				.map(media -> this.fromMediaData(media.getMimeType(), media.getData()))
				.toList();

			contentList.add(new MediaContent("video", null, null, mediaList));
		}
		else {
			MediaContent mediaContent = new MediaContent(message.getText());
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

	private List<FunctionTool> getFunctionTools(List<ToolDefinition> toolDefinitions) {
		return toolDefinitions.stream().map(toolDefinition -> {
			var function = new FunctionTool.Function(toolDefinition.description(), toolDefinition.name(),
					toolDefinition.inputSchema());
			return new FunctionTool(function);
		}).toList();
	}

	private ChatCompletionRequestParameter toDashScopeRequestParameter(DashScopeChatOptions options, boolean stream) {

		if (options == null) {
			return new ChatCompletionRequestParameter();
		}

		Boolean incrementalOutput = stream && options.getIncrementalOutput();
		return new ChatCompletionRequestParameter("message", options.getSeed(), options.getMaxTokens(),
				options.getTopP(), options.getTopK(), options.getRepetitionPenalty(), options.getPresencePenalty(),
				options.getTemperature(), options.getStop(), options.getEnableSearch(), options.getResponseFormat(),
				incrementalOutput, options.getTools(), options.getToolChoice(), stream,
				options.getVlHighResolutionImages(), options.getEnableThinking());
	}

	/**
	 * Use the provided convention for reporting observation data
	 * @param observationConvention The provided convention
	 */
	public void setObservationConvention(ChatModelObservationConvention observationConvention) {
		Assert.notNull(observationConvention, "observationConvention cannot be null");
		this.observationConvention = observationConvention;
	}

	/**
	 * Returns a builder pre-populated with the current configuration for mutation.
	 */
	public Builder mutate() {
		return new Builder(this);
	}

	@Override
	public DashScopeChatModel clone() {
		return this.mutate().build();
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {

		private Builder() {
		}

		public Builder(DashScopeChatModel dashScopeChatModel) {
			this.dashScopeApi = dashScopeChatModel.dashscopeApi;
			this.defaultOptions = dashScopeChatModel.defaultOptions;
			this.toolCallingManager = dashScopeChatModel.toolCallingManager;
			this.retryTemplate = dashScopeChatModel.retryTemplate;
			this.observationRegistry = dashScopeChatModel.observationRegistry;
			this.toolExecutionEligibilityPredicate = dashScopeChatModel.toolExecutionEligibilityPredicate;
		}

		private DashScopeApi dashScopeApi;

		private DashScopeChatOptions defaultOptions = DashScopeChatOptions.builder()
			.withModel(DEFAULT_MODEL_NAME)
			.withTemperature(DEFAULT_TEMPERATURE)
			.build();

		private RetryTemplate retryTemplate = RetryUtils.DEFAULT_RETRY_TEMPLATE;

		private ToolCallingManager toolCallingManager;

		private ToolExecutionEligibilityPredicate toolExecutionEligibilityPredicate = new DefaultToolExecutionEligibilityPredicate();

		private ObservationRegistry observationRegistry = ObservationRegistry.NOOP;

		public Builder dashScopeApi(DashScopeApi dashScopeApi) {
			this.dashScopeApi = dashScopeApi;
			return this;
		}

		public Builder defaultOptions(DashScopeChatOptions defaultOptions) {
			this.defaultOptions = defaultOptions;
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

		public Builder toolCallingManager(ToolCallingManager toolCallingManager) {
			this.toolCallingManager = toolCallingManager;
			return this;
		}

		public Builder observationRegistry(ObservationRegistry observationRegistry) {
			this.observationRegistry = observationRegistry;
			return this;
		}

		public DashScopeChatModel build() {

			if (this.toolCallingManager != null) {
				return new DashScopeChatModel(this.dashScopeApi, this.defaultOptions, this.toolCallingManager,
						this.retryTemplate, this.observationRegistry, this.toolExecutionEligibilityPredicate);
			}

			return new DashScopeChatModel(this.dashScopeApi, this.defaultOptions, DEFAULT_TOOL_CALLING_MANAGER,
					this.retryTemplate, this.observationRegistry, this.toolExecutionEligibilityPredicate);
		}

	}

}
