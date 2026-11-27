/*
 * Copyright 2024-2026 the original author or authors.
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
package com.alibaba.cloud.ai.graph.agent.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.GraphLifecycleListener;
import com.alibaba.cloud.ai.graph.action.NodeActionWithConfig;
import com.alibaba.cloud.ai.graph.agent.context.CheckpointContextCompaction;
import com.alibaba.cloud.ai.graph.agent.context.ContextWindowPolicy;
import com.alibaba.cloud.ai.graph.serializer.AgentInstructionMessage;
import com.alibaba.cloud.ai.graph.utils.TypeRef;
import com.alibaba.cloud.ai.graph.utils.FinishReasonMetadata;
import com.alibaba.cloud.ai.graph.utils.ChatResponseStreamAggregator;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.InterceptorChain;
import com.alibaba.cloud.ai.graph.agent.tool.ToolCallbackUtils;
import com.alibaba.cloud.ai.graph.agent.interceptor.StreamingModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.modelfallback.ModelFallbackInterceptor;
import com.alibaba.cloud.ai.graph.agent.model.ModelRoutingPolicy;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientAttributes;
import org.springframework.ai.chat.client.DefaultChatClient.DefaultChatClientRequestSpec;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.api.ToolAdvisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.part.TextPart;
import org.springframework.ai.chat.metadata.EmptyUsage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.template.TemplateRenderer;
import org.springframework.ai.tool.ToolCallback;

import org.springframework.util.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.function.UnaryOperator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import reactor.core.publisher.Flux;

import static com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver.THREAD_ID_DEFAULT;

public class AgentLlmNode implements NodeActionWithConfig {
	private static final Logger logger = LoggerFactory.getLogger(AgentLlmNode.class);
	public static final String MODEL_ITERATION_KEY = "_MODEL_ITERATION_";

	private String agentName;

	private List<Advisor> advisors = new ArrayList<>();

	private volatile List<ToolCallback> toolCallbacks = List.of();

	private List<ModelInterceptor> modelInterceptors = new ArrayList<>();

	private List<StreamingModelInterceptor> streamingInterceptors = new ArrayList<>();
	private List<UnaryOperator<AssistantMessage>> streamingOutputFilters = List.of();

	private String outputKey;

	private String outputSchema;

	private ChatClient chatClient;

	private String systemPrompt;

	private TemplateRenderer templateRenderer;

	private String instruction;

	private ToolCallingChatOptions chatOptions;

	private boolean enableReasoningLog;

	private final ContextWindowPolicy contextWindowPolicy;

	private final ModelRoutingPolicy modelRoutingPolicy;

	private final CheckpointContextCompaction checkpointContextCompaction;

	public AgentLlmNode(Builder builder) {
		this.agentName = builder.agentName;
		this.outputKey = builder.outputKey;
		this.outputSchema = builder.outputSchema;
		this.systemPrompt = builder.systemPrompt;
		this.instruction = builder.instruction;
		this.templateRenderer = builder.templateRenderer;
		if (builder.advisors != null) {
			this.advisors = builder.advisors;
		}
		if (builder.toolCallbacks != null) {
			this.toolCallbacks = List.copyOf(builder.toolCallbacks);
		}
		if (builder.modelInterceptors != null) {
			this.modelInterceptors = builder.modelInterceptors;
		}
		if (builder.streamingInterceptors != null) {
			this.streamingInterceptors = builder.streamingInterceptors;
		}
		this.chatClient = builder.chatClient;
		this.chatOptions = builder.chatOptions instanceof ToolCallingChatOptions options ? options : null;
		this.enableReasoningLog = builder.enableReasoningLog;
		this.contextWindowPolicy = builder.contextWindowPolicy;
		this.modelRoutingPolicy = builder.modelRoutingPolicy;
		this.checkpointContextCompaction = builder.checkpointContextCompaction;
	}

	public static Builder builder() {
		return new Builder();
	}

	public void setToolCallbacks(List<ToolCallback> toolCallbacks) {
		this.toolCallbacks = List.copyOf(toolCallbacks);
	}

	public void setModelInterceptors(List<ModelInterceptor> modelInterceptors) {
		this.modelInterceptors = modelInterceptors;
	}

	public void setStreamingOutputFilters(List<UnaryOperator<AssistantMessage>> filters) {
		this.streamingOutputFilters = List.copyOf(filters);
	}

	public void setStreamingInterceptors(List<StreamingModelInterceptor> streamingInterceptors) {
		this.streamingInterceptors = streamingInterceptors;
	}

	public void setInstruction(String instruction) {
		this.instruction = instruction;
	}

	public void setSystemPrompt(String systemPrompt) {
		this.systemPrompt = systemPrompt;
	}

	@Override
	public Map<String, Object> apply(OverAllState state, RunnableConfig config) throws Exception {
		if (enableReasoningLog && logger.isDebugEnabled()) {
			logger.debug("[ThreadId {}] Agent {} start reasoning.", config.threadId()
					.orElse(THREAD_ID_DEFAULT), agentName);
		}

		// Check and manage iteration counter
		final AtomicInteger iterations;
		if (!config.context().containsKey(MODEL_ITERATION_KEY)) {
			iterations = new AtomicInteger(0);
			config.context().put(MODEL_ITERATION_KEY, iterations);
		} else {
			iterations = (AtomicInteger) config.context().get(MODEL_ITERATION_KEY);
			iterations.incrementAndGet();
		}

		// Check and manage messages
		List<Message> messages = new ArrayList<>(state.<List<Message>>value("messages").orElseGet(List::of));
		if (messages.isEmpty()) {
			// try with "input" key, which is more commonly used in graph input when agent is used as a node.
			if (state.value("input").isPresent()) {
				messages.add(new UserMessage(state.value("input").get().toString()));
			} else {
				throw new IllegalArgumentException("Either 'instruction' or 'includeContents' must be set for Agent.");
			}
		}

		Map<String, Object> updatedState = new HashMap<>();
		if (this.checkpointContextCompaction != null) {
			String key = CheckpointContextCompaction.stateKey(this.agentName);
			var prepared = this.checkpointContextCompaction.prepare(messages, state.value(key).orElse(null));
			messages = new ArrayList<>(prepared.messages());
			if (!prepared.checkpointUpdate().isEmpty()) {
				updatedState.put(key, prepared.checkpointUpdate());
			}
		}

		augmentUserMessage(messages, outputSchema);
		renderTemplatedUserMessage(messages, state.data(), config.metadata());

		// Create ModelRequest; include state in context so interceptors (e.g. handoffs step-config) can read it
		Map<String, Object> contextMap = new HashMap<>(state.data());
		Map<String, Object> metadata = config.metadata().orElse(new HashMap<>());
		if (!metadata.isEmpty()) {
			contextMap.putAll(metadata);
		}
		var configuredCallbacks = ToolCallbackUtils.deduplicateByName(this.toolCallbacks,
				this.chatOptions != null ? this.chatOptions.getToolCallbacks() : null);
		var initialOptions = this.chatOptions != null
				? this.chatOptions.mutate().toolCallbacks(configuredCallbacks).build() : null;
		var scope = new RequestToolScope(agentName, configuredCallbacks);
		ModelRequest.Builder requestBuilder = ModelRequest.builder()
				.messages(messages)
				.options(initialOptions)
				.context(contextMap);

		List<String> toolNames = new ArrayList<>();
		Map<String, String> toolDescriptions = new HashMap<>();
		for (ToolCallback callback : configuredCallbacks) {
			var definition = callback.getToolDefinition();
			toolNames.add(definition.name());
			if (StringUtils.hasLength(definition.description())) {
				toolDescriptions.put(definition.name(), definition.description());
			}
		}
		requestBuilder.availableTools(toolNames).toolDescriptions(toolDescriptions);

		if (StringUtils.hasLength(this.systemPrompt)) {
			requestBuilder.systemMessage(new SystemMessage(this.systemPrompt));
		}

		ModelRequest modelRequest = route(requestBuilder.build(), state, updatedState);
		boolean propagateModelErrors = this.contextWindowPolicy != null || this.modelRoutingPolicy != null
				|| this.modelInterceptors.stream().anyMatch(ModelFallbackInterceptor.class::isInstance);

		// add streaming support
		boolean stream = config.metadata("_stream_", new TypeRef<Boolean>(){}).orElse(true);
		if (stream) {
			// Create base handler that actually calls the model with streaming
			ModelCallHandler baseHandler = request -> {
				try {
					if (enableReasoningLog) {
						String systemPrompt = request.getSystemMessage() != null ? request.getSystemMessage().getText() : "";
						if (logger.isDebugEnabled()) {
							logger.debug("[ThreadId {}] Agent {} reasoning with system prompt: {}", config.threadId()
									.orElse(THREAD_ID_DEFAULT), agentName, systemPrompt);
						}
					}
					Flux<ChatResponse> chatResponseFlux = buildChatClientRequestSpec(request, modelRequest, configuredCallbacks, scope).stream().chatResponse();
					if (enableReasoningLog && streamingOutputFilters.isEmpty()) {
						chatResponseFlux = chatResponseFlux.doOnNext(chatResponse -> {
							if (chatResponse != null && chatResponse.getResult() != null && chatResponse.getResult().getOutput() != null) {
								if (chatResponse.getResult().getOutput().hasToolCalls()) {
									logger.info("[ThreadId {}] Agent {} reasoning round {} streaming output: {}",
											config.threadId().orElse(THREAD_ID_DEFAULT), agentName, iterations.get(), chatResponse.getResult().getOutput().getToolCalls());
								} else {
									logger.info("[ThreadId {}] Agent {} reasoning round {} streaming output: {}",
											config.threadId()
													.orElse(THREAD_ID_DEFAULT), agentName, iterations.get(), chatResponse.getResult()
													.getOutput().getText());
								}
							}
						});
					}
					if (streamingInterceptors != null && !streamingInterceptors.isEmpty()) {
						chatResponseFlux = InterceptorChain.applyStreamingInterceptors(
								streamingInterceptors, chatResponseFlux, request);
					}
					return ModelResponse.of(chatResponseFlux);
				} catch (RuntimeException e) {
					if (propagateModelErrors || request.getModelTarget() != null) {
						throw e;
					}
					logger.error("Exception during streaming model call: ", e);
					return ModelResponse.of(new AssistantMessage("Exception: " + e.getMessage()));
				}
			};

			// Chain interceptors if any
			ModelCallHandler chainedHandler = InterceptorChain.chainModelInterceptors(
					modelInterceptors, baseHandler);

			// Execute the chained handler
			ModelResponse modelResponse = chainedHandler.call(modelRequest);
			Object output = modelResponse.getMessage();
			if (!streamingOutputFilters.isEmpty()) {
				if (output instanceof Flux<?> chunks) {
					output = ChatResponseStreamAggregator.aggregate(chunks.cast(ChatResponse.class),
							config.mergeReasoningContent()).map(this::filterStreamedResponse).flux();
				}
				else if (output instanceof AssistantMessage assistant) {
					output = filterStreamedMessage(assistant);
				}
			}
			if (output instanceof Flux<?> chunks) {
				output = chunks.map(chunk -> chunk instanceof ChatResponse response
						? markStreamedResponse(response, scope, config) : chunk);
			}
			else if (output instanceof AssistantMessage assistant && assistant.hasToolCalls()) {
				output = scope.mark(assistant, config);
			}
			updatedState.put(StringUtils.hasLength(this.outputKey) ? this.outputKey : "messages", output);
			return updatedState;
		} else {
			// Create base handler that actually calls the model
			ModelCallHandler baseHandler = request -> {
				try {
					if (enableReasoningLog) {
						String systemPrompt = request.getSystemMessage() != null ? request.getSystemMessage().getText() : "";
						logger.info("[ThreadId {}] Agent {} reasoning round {} with system prompt: {}.", config.threadId().orElse(THREAD_ID_DEFAULT), agentName, iterations.get(), systemPrompt);
					}

					ChatResponse response = buildChatClientRequestSpec(request, modelRequest, configuredCallbacks, scope).call().chatResponse();

					AssistantMessage responseMessage = new AssistantMessage("Empty response from model for unknown reason");
					if (response != null && response.getResult() != null && response.getResult().getOutput() != null) {
						var generation = response.getResult();
						responseMessage = FinishReasonMetadata.apply(generation.getOutput(), generation.getMetadata());
					}

					if (enableReasoningLog && streamingOutputFilters.isEmpty()) {
						logger.info("[ThreadId {}] Agent {} reasoning round {} returned: {}.", config.threadId().orElse(THREAD_ID_DEFAULT), agentName, iterations.get(), responseMessage);
					}

					return ModelResponse.of(responseMessage, response);
				} catch (RuntimeException e) {
					if (propagateModelErrors || request.getModelTarget() != null) {
						throw e;
					}
					logger.error("Exception during invoking model call: ", e);
					return ModelResponse.of(new AssistantMessage("Exception: " + e.getMessage()));
				}
			};

			// Chain interceptors if any
			ModelCallHandler chainedHandler = InterceptorChain.chainModelInterceptors(
					modelInterceptors, baseHandler);

			if (enableReasoningLog) {
				logger.info("[ThreadId {}] Agent {} reasoning round {} model chain has started.", config.threadId().orElse(THREAD_ID_DEFAULT), agentName, iterations.get());
			}

			// Execute the chained handler
			ModelResponse modelResponse = chainedHandler.call(modelRequest);
			Usage tokenUsage = modelResponse.getChatResponse() != null ? modelResponse.getChatResponse().getMetadata()
					.getUsage() : new EmptyUsage();

			updatedState.put("_TOKEN_USAGE_", tokenUsage);
			var output = modelResponse.getMessage() instanceof AssistantMessage assistant && assistant.hasToolCalls()
					? scope.mark(assistant, config) : modelResponse.getMessage();
			updatedState.put("messages", output);
			if (StringUtils.hasLength(this.outputKey)) {
				updatedState.put(this.outputKey, output);
			}

			return updatedState;
		}
	}

	private ModelRequest route(ModelRequest request, OverAllState state, Map<String, Object> updates) {
		if (this.modelRoutingPolicy == null) {
			return request;
		}
		String key = "_MODEL_ROUTE_:" + this.agentName;
		var executionId = state.value(GraphLifecycleListener.EXECUTION_ID_KEY, String.class);
		String name;
		if (executionId.isPresent() && state.value(key).orElse(null) instanceof Map<?, ?> route
				&& executionId.get().equals(route.get("executionId")) && route.get("name") instanceof String saved) {
			name = saved;
		}
		else {
			name = this.modelRoutingPolicy.select(request);
			executionId.ifPresent(id -> updates.put(key, Map.of("executionId", id, "name", name)));
		}
		return ModelRequest.builder(request).modelTarget(this.modelRoutingPolicy.target(name)).build();
	}

	private ChatResponse markStreamedResponse(ChatResponse response, RequestToolScope scope, RunnableConfig config) {
		// Spring AI also aggregates tool calls emitted in legacy response metadata.
		boolean metadataCalls = response.getMetadata() != null
				&& response.getMetadata().get("toolCalls") instanceof List<?> calls && !calls.isEmpty();
		if (metadataCalls && response.getResults().isEmpty()) {
			return new ChatResponse(List.of(new Generation(scope.mark(new AssistantMessage(""), config))),
					response.getMetadata());
		}
		List<Generation> marked = response.getResults().stream()
				.map(generation -> generation.getOutput() != null && (metadataCalls || generation.getOutput().hasToolCalls())
						? new Generation(scope.mark(generation.getOutput(), config), generation.getMetadata())
						: generation)
				.toList();
		return new ChatResponse(marked, response.getMetadata());
	}

	private ChatResponse filterStreamedResponse(ChatResponse response) {
		var generation = response.getResult();
		var filtered = filterStreamedMessage(generation.getOutput());
		return new ChatResponse(List.of(new Generation(filtered, generation.getMetadata())), response.getMetadata());
	}

	private AssistantMessage filterStreamedMessage(AssistantMessage output) {
		for (var filter : streamingOutputFilters) {
			output = filter.apply(output);
		}
		return output;
	}

	public void setAdvisors(List<Advisor> advisors) {
		this.advisors = advisors;
	}

	private List<Message> appendSystemPromptIfNeeded(ModelRequest modelRequest) {
		// Create a new list and copy messages from modelRequest
		List<Message> messages = new ArrayList<>(modelRequest.getMessages());

		// FIXME, there should have only one SystemMessage.
		//  Users may have added SystemMessages in hooks or somewhere else, simply remove will cause unexpected agent behaviour.
//		messages.removeIf(message -> message instanceof SystemMessage);

		// Add the SystemMessage from modelRequest at the beginning if present
		if (modelRequest.getSystemMessage() != null) {
			messages.add(0, modelRequest.getSystemMessage());
		}

		long systemMessageCount = messages.stream()
				.filter(message -> message instanceof SystemMessage)
				.count();

		if (systemMessageCount > 2) {
			logger.warn("Detected {} SystemMessages in the message list. There should typically be only one SystemMessage. " +
					"Multiple SystemMessages may cause unexpected behavior or model confusion.", systemMessageCount);
		}

		return messages;
	}

	private String renderPromptTemplate(String prompt, Map<String, Object> params) {
		PromptTemplate.Builder builder = PromptTemplate.builder().template(prompt);
		if (templateRenderer != null) {
			builder.renderer(templateRenderer);
		}
		return builder.build().render(params);
	}

	public void augmentUserMessage(List<Message> messages, String outputSchema) {
		if (!StringUtils.hasText(outputSchema)) {
			return;
		}

		for (int i = messages.size() - 1; i >= 0; i--) {
			Message message = messages.get(i);
			if (message instanceof UserMessage userMessage) {
				// Check if outputSchema is already present to avoid duplication
				if (!userMessage.getText().contains(outputSchema)) {
					messages.set(i, userMessage.mutate().part(TextPart.of(System.lineSeparator() + outputSchema)).build());
				}
				break;
			}
			if (message instanceof AgentInstructionMessage templatedUserMessage) {
                String newOutputSchema = outputSchema.replace("{", "\\{").replace("}", "\\}");
                // Check if outputSchema is already present to avoid duplication
                if (!templatedUserMessage.getText().contains(newOutputSchema)) {
                	messages.set(i, templatedUserMessage.mutate().text(templatedUserMessage.getText() + System.lineSeparator() + newOutputSchema).build());
                }
				break;
			}

			if (i == 0) {
				messages.add(new UserMessage(outputSchema));
			}
		}
	}

	public void renderTemplatedUserMessage(List<Message> messages, Map<String, Object> params, Optional<Map<String, Object>> metadata) {
		// Process params to create a new Map
		Map<String, Object> processedParams = new HashMap<>();
		if (params != null) {
			for (Map.Entry<String, Object> entry : params.entrySet()) {
				String key = entry.getKey();
				Object value = entry.getValue();
				
				// Exclude key "messages"
				if ("messages".equals(key)) {
					continue;
				}
				
				// Exclude List type values
				if (value instanceof List) {
					continue;
				}
				
				// Convert Message type to String using getText()
				if (value instanceof Message message) {
					processedParams.put(key, message.getText());
				} else {
					// Keep other types as is
					processedParams.put(key, value);
				}
			}
		}

		for (int i = messages.size() - 1; i >= 0; i--) {
			Message message = messages.get(i);
			if (message instanceof AgentInstructionMessage instructionMessage && !instructionMessage.isRendered()) {
				AgentInstructionMessage newMessage = instructionMessage.mutate().text(renderPromptTemplate(instructionMessage.getText(), processedParams)).rendered(true).build();
				messages.set(i, newMessage);
				break;
			}
		}
	}

	private ChatClient.ChatClientRequestSpec buildChatClientRequestSpec(ModelRequest request,
			ModelRequest originalRequest, List<ToolCallback> configuredCallbacks, RequestToolScope scope) {
		var options = request.getOptions();
		var originalOptions = originalRequest.getOptions();
		var optionCallbacks = options != null ? options.getToolCallbacks() : null;
		var target = request.getModelTarget();
		var targetModel = target == null ? null : target.model();
		var targetOptions = targetModel == null ? null : targetModel.getOptions();
		var policy = target != null && target.contextWindowPolicy() != null
				? target.contextWindowPolicy() : this.contextWindowPolicy;
		boolean replaceDefaults = options != null && !Objects.equals(optionCallbacks,
				originalOptions != null ? originalOptions.getToolCallbacks() : null);
		var requestCallbacks = replaceDefaults ? ToolCallbackUtils.deduplicateByName(optionCallbacks)
				: configuredCallbacks;
		var selection = request.hasToolSelection() ? request.getTools() : null;

		var promptSpec = this.chatClient.prompt()
				.messages(appendSystemPromptIfNeeded(request))
				.advisors(advisorSpec -> advisorSpec.param(
						ChatClientAttributes.TOOL_CALLING_ADVISOR_AUTO_REGISTER.getKey(), false))
				.advisors(this.advisors);
		var prepare = scope.prepareAdvisor(requestCallbacks, request.getDynamicToolCallbacks(), replaceDefaults,
				selection, targetOptions);
		var capture = scope.captureAdvisor(selection, policy, targetModel);
		if (promptSpec instanceof DefaultChatClientRequestSpec spec) {
			// Graph owns dispatch and approval. On equal order ChatClient runs the
			// last registered advisor first, so put capture before other registrations.
			spec.getAdvisors().removeIf(ToolAdvisor.class::isInstance);
			spec.getAdvisors().add(0, capture);
			spec.getAdvisors().add(prepare);
		}
		else {
			promptSpec.advisors(prepare, capture);
		}
		if (options != null && targetModel == null) {
			// Keep provider options. Callback merging is handled after ChatClient has
			// applied its model defaults and tool providers.
			promptSpec.options(options.mutate().toolCallbacks((List<ToolCallback>) null));
		}
		return promptSpec;
	}

	public static class Builder {
		private String agentName;

		private String outputKey;

		private String outputSchema;

		private String systemPrompt;

		private TemplateRenderer templateRenderer;

		private ChatClient chatClient;

		private List<Advisor> advisors;

		private List<ToolCallback> toolCallbacks;

		private List<ModelInterceptor> modelInterceptors;

		private List<StreamingModelInterceptor> streamingInterceptors;

		private String instruction;

		private boolean enableReasoningLog;

		private ChatOptions chatOptions;

		private ContextWindowPolicy contextWindowPolicy;

		private ModelRoutingPolicy modelRoutingPolicy;

		public Builder modelRouting(ModelRoutingPolicy policy) {
			this.modelRoutingPolicy = policy;
			return this;
		}

		private CheckpointContextCompaction checkpointContextCompaction;

		public Builder checkpointContextCompaction(CheckpointContextCompaction compaction) {
			this.checkpointContextCompaction = compaction;
			return this;
		}

		public Builder contextWindowPolicy(ContextWindowPolicy policy) {
			this.contextWindowPolicy = policy;
			return this;
		}

		public Builder agentName(String agentName) {
			this.agentName = agentName;
			return this;
		}

		public Builder outputKey(String outputKey) {
			this.outputKey = outputKey;
			return this;
		}

		public Builder outputSchema(String outputSchema) {
			this.outputSchema = outputSchema;
			return this;
		}

		public Builder systemPrompt(String systemPrompt) {
			this.systemPrompt = systemPrompt;
			return this;
		}

		public Builder templateRenderer(TemplateRenderer templateRenderer) {
			this.templateRenderer = templateRenderer;
			return this;
		}

		public Builder advisors(List<Advisor> advisors) {
			this.advisors = advisors;
			return this;
		}

		public Builder toolCallbacks(List<ToolCallback> toolCallbacks) {
			this.toolCallbacks = toolCallbacks;
			return this;
		}

		public Builder modelInterceptors(List<ModelInterceptor> modelInterceptors) {
			this.modelInterceptors = modelInterceptors;
			return this;
		}

		public Builder streamingInterceptors(List<StreamingModelInterceptor> streamingInterceptors) {
			this.streamingInterceptors = streamingInterceptors;
			return this;
		}

		public Builder chatClient(ChatClient chatClient) {
			this.chatClient = chatClient;
			return this;
		}

		public Builder instruction(String instruction) {
			this.instruction = instruction;
			return this;
		}

		public Builder enableReasoningLog(boolean enableReasoningLog) {
			this.enableReasoningLog = enableReasoningLog;
			return this;
		}

		public Builder chatOptions(ChatOptions chatOptions) {
			this.chatOptions = chatOptions;
			return this;
		}

		public AgentLlmNode build() {
			return new AgentLlmNode(this);
		}

	}

}
