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
package com.alibaba.cloud.ai.tool;

import com.alibaba.cloud.ai.tool.observation.ArmsToolCallingObservationContext;
import com.alibaba.cloud.ai.tool.observation.ArmsToolCallingObservationConvention;
import com.alibaba.cloud.ai.tool.observation.ArmsToolCallingObservationDocumentation;
import io.micrometer.observation.ObservationRegistry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.AssistantMessage.ToolCall;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.execution.DefaultToolExecutionExceptionProcessor;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;
import org.springframework.ai.tool.resolution.DelegatingToolCallbackResolver;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Inspired from org.springframework.ai.model.tool.DefaultToolCallingManager.
 *
 * @author Lumian
 */
public class ObservableToolCallingManager implements ToolCallingManager {

	private static final Logger logger = LoggerFactory.getLogger(ObservableToolCallingManager.class);

	// @formatter:off

  private static final ObservationRegistry DEFAULT_OBSERVATION_REGISTRY
      = ObservationRegistry.NOOP;

  private static final ToolCallbackResolver DEFAULT_TOOL_CALLBACK_RESOLVER
      = new DelegatingToolCallbackResolver(List.of());

  private static final ToolExecutionExceptionProcessor DEFAULT_TOOL_EXECUTION_EXCEPTION_PROCESSOR
      = DefaultToolExecutionExceptionProcessor.builder().build();

  private static final ArmsToolCallingObservationConvention DEFAULT_OBSERVATION_CONVENTION = new ArmsToolCallingObservationConvention();

  // @formatter:on

	private final ObservationRegistry observationRegistry;

	private final ToolCallbackResolver toolCallbackResolver;

	private final ToolExecutionExceptionProcessor toolExecutionExceptionProcessor;

	// TODO Mandatory Convention as ARMS implementation until the Spring AI project
	// officially supports for observation
	private final ArmsToolCallingObservationConvention observationConvention = DEFAULT_OBSERVATION_CONVENTION;

	public ObservableToolCallingManager(ObservationRegistry observationRegistry,
			ToolCallbackResolver toolCallbackResolver,
			ToolExecutionExceptionProcessor toolExecutionExceptionProcessor) {
		Assert.notNull(observationRegistry, "observationRegistry cannot be null");
		Assert.notNull(toolCallbackResolver, "toolCallbackResolver cannot be null");
		Assert.notNull(toolExecutionExceptionProcessor, "toolCallExceptionConverter cannot be null");

		this.observationRegistry = observationRegistry;
		this.toolCallbackResolver = toolCallbackResolver;
		this.toolExecutionExceptionProcessor = toolExecutionExceptionProcessor;
	}

	@Override
	public List<ToolDefinition> resolveToolDefinitions(ToolCallingChatOptions chatOptions) {
		Assert.notNull(chatOptions, "chatOptions cannot be null");

		List<ToolCallback> toolCallbacks = new ArrayList<>(chatOptions.getToolCallbacks());
		for (String toolName : chatOptions.getToolNames()) {
			// Skip the tool if it is already present in the request toolCallbacks.
			// That might happen if a tool is defined in the options
			// both as a ToolCallback and as a tool name.
			if (chatOptions.getToolCallbacks()
				.stream()
				.anyMatch(tool -> tool.getToolDefinition().name().equals(toolName))) {
				continue;
			}
			ToolCallback toolCallback = this.toolCallbackResolver.resolve(toolName);
			if (toolCallback == null) {
				throw new IllegalStateException("No ToolCallback found for tool name: " + toolName);
			}
			toolCallbacks.add(toolCallback);
		}

		return toolCallbacks.stream().map(toolCallback -> toolCallback.getToolDefinition()).toList();
	}

	@Override
	public ToolExecutionResult executeToolCalls(Prompt prompt, ChatResponse chatResponse) {
		Assert.notNull(prompt, "prompt cannot be null");
		Assert.notNull(chatResponse, "chatResponse cannot be null");

		Optional<Generation> toolCallGeneration = chatResponse.getResults()
			.stream()
			.filter(g -> !CollectionUtils.isEmpty(g.getOutput().getToolCalls()))
			.findFirst();

		if (toolCallGeneration.isEmpty()) {
			throw new IllegalStateException("No tool call requested by the chat model");
		}

		AssistantMessage assistantMessage = toolCallGeneration.get().getOutput();

		ToolContext toolContext = buildToolContext(prompt, assistantMessage);

		InternalToolExecutionResult internalToolExecutionResult = executeToolCall(prompt, assistantMessage,
				toolContext);

		List<Message> conversationHistory = buildConversationHistoryAfterToolExecution(prompt.getInstructions(),
				assistantMessage, internalToolExecutionResult.toolResponseMessage());

		return ToolExecutionResult.builder()
			.conversationHistory(conversationHistory)
			.returnDirect(internalToolExecutionResult.returnDirect())
			.build();
	}

	private static ToolContext buildToolContext(Prompt prompt, AssistantMessage assistantMessage) {
		Map<String, Object> toolContextMap = Map.of();

		if (prompt.getOptions() instanceof ToolCallingChatOptions toolCallingChatOptions
				&& !CollectionUtils.isEmpty(toolCallingChatOptions.getToolContext())) {
			toolContextMap = new HashMap<>(toolCallingChatOptions.getToolContext());

			toolContextMap.put(ToolContext.TOOL_CALL_HISTORY,
					buildConversationHistoryBeforeToolExecution(prompt, assistantMessage));
		}

		return new ToolContext(toolContextMap);
	}

	private static List<Message> buildConversationHistoryBeforeToolExecution(Prompt prompt,
			AssistantMessage assistantMessage) {
		List<Message> messageHistory = new ArrayList<>(prompt.copy().getInstructions());
		messageHistory.add(new AssistantMessage(assistantMessage.getText(), assistantMessage.getMetadata(),
				assistantMessage.getToolCalls()));
		return messageHistory;
	}

	/**
	 * Execute the tool call and return the response message.
	 */
	private InternalToolExecutionResult executeToolCall(Prompt prompt, AssistantMessage assistantMessage,
			ToolContext toolContext) {
		List<ToolCallback> toolCallbacks = List.of();
		if (prompt.getOptions() instanceof ToolCallingChatOptions toolCallingChatOptions) {
			toolCallbacks = toolCallingChatOptions.getToolCallbacks();
		}

		List<ToolResponseMessage.ToolResponse> toolResponses = new ArrayList<>();

		Boolean returnDirect = null;

		for (AssistantMessage.ToolCall toolCall : assistantMessage.getToolCalls()) {

			logger.debug("Executing tool call: {}", toolCall.name());

			String toolName = toolCall.name();
			String toolInputArguments = toolCall.arguments();

			ToolCallback toolCallback = toolCallbacks.stream()
				.filter(tool -> toolName.equals(tool.getToolDefinition().name()))
				.findFirst()
				.orElseGet(() -> this.toolCallbackResolver.resolve(toolName));

			if (toolCallback == null) {
				throw new IllegalStateException("No ToolCallback found for tool name: " + toolName);
			}

			if (returnDirect == null) {
				returnDirect = toolCallback.getToolMetadata().returnDirect();
			}
			else {
				returnDirect = returnDirect && toolCallback.getToolMetadata().returnDirect();
			}

			ArmsToolCallingObservationContext observationContext = ArmsToolCallingObservationContext.builder()
				.toolCall(toolCall)
				.description(toolCallback.getToolDefinition().description())
				.returnDirect(returnDirect)
				.build();

			String toolResult = ArmsToolCallingObservationDocumentation.EXECUTE_TOOL_OPERATION
				.observation(this.observationConvention, DEFAULT_OBSERVATION_CONVENTION, () -> observationContext,
						this.observationRegistry)
				.observe(() -> {
					String result;
					try {
						result = toolCallback.call(toolInputArguments, toolContext);
					}
					catch (ToolExecutionException ex) {
						observationContext.setError(ex);
						result = toolExecutionExceptionProcessor.process(ex);
					}

					observationContext.setToolResult(result);
					return result;
				});

			toolResponses.add(new ToolResponseMessage.ToolResponse(toolCall.id(), toolName, toolResult));
		}

		return new InternalToolExecutionResult(new ToolResponseMessage(toolResponses, Map.of()), returnDirect);
	}

	/**
	 * We have to assume that tool calls is ordered in streaming mode.
	 */
	private static AssistantMessage mergeToolCalls(AssistantMessage assistantMessage) {
		ArrayList<AssistantMessage.ToolCall> toolCalls = new ArrayList<>();
		Iterator<ToolCall> iterator = assistantMessage.getToolCalls().iterator();
		AtomicReference<StringBuilder> argumentsContentRef = new AtomicReference<>(new StringBuilder());
		String id = null;
		String type = null;
		String name = null;
		while (iterator.hasNext()) {
			ToolCall toolCallChunk = iterator.next();
			if (StringUtils.hasText(toolCallChunk.id()) && StringUtils.hasText(toolCallChunk.name())) {
				if (StringUtils.hasText(id) && StringUtils.hasText(name)) {
					// save previous one
					toolCalls.add(new AssistantMessage.ToolCall(id, type, name, argumentsContentRef.get().toString()));
					argumentsContentRef.set(new StringBuilder());
				}
				id = toolCallChunk.id();
				type = toolCallChunk.type();
				name = toolCallChunk.name();
			}
			if (StringUtils.hasText(toolCallChunk.arguments())) {
				argumentsContentRef.get().append(toolCallChunk.arguments());
			}
		}

		if (StringUtils.hasText(id) && StringUtils.hasText(name)) {
			// save last one
			toolCalls.add(new AssistantMessage.ToolCall(id, type, name, argumentsContentRef.get().toString()));
			argumentsContentRef.set(new StringBuilder());
		}
		return new AssistantMessage(assistantMessage.getText(), assistantMessage.getMetadata(), toolCalls,
				assistantMessage.getMedia());
	}

	private List<Message> buildConversationHistoryAfterToolExecution(List<Message> previousMessages,
			AssistantMessage assistantMessage, ToolResponseMessage toolResponseMessage) {
		List<Message> messages = new ArrayList<>(previousMessages);
		messages.add(assistantMessage);
		messages.add(toolResponseMessage);
		return messages;
	}

	private record InternalToolExecutionResult(ToolResponseMessage toolResponseMessage, boolean returnDirect) {
	}

	public static ObservableToolCallingManager.Builder builder() {
		return new ObservableToolCallingManager.Builder();
	}

	public static class Builder {

		private ObservationRegistry observationRegistry = DEFAULT_OBSERVATION_REGISTRY;

		private ToolCallbackResolver toolCallbackResolver = DEFAULT_TOOL_CALLBACK_RESOLVER;

		private ToolExecutionExceptionProcessor toolExecutionExceptionProcessor = DEFAULT_TOOL_EXECUTION_EXCEPTION_PROCESSOR;

		private Builder() {
		}

		public ObservableToolCallingManager.Builder observationRegistry(ObservationRegistry observationRegistry) {
			this.observationRegistry = observationRegistry;
			return this;
		}

		public ObservableToolCallingManager.Builder toolCallbackResolver(ToolCallbackResolver toolCallbackResolver) {
			this.toolCallbackResolver = toolCallbackResolver;
			return this;
		}

		public ObservableToolCallingManager.Builder toolExecutionExceptionProcessor(
				ToolExecutionExceptionProcessor toolExecutionExceptionProcessor) {
			this.toolExecutionExceptionProcessor = toolExecutionExceptionProcessor;
			return this;
		}

		public ObservableToolCallingManager build() {
			return new ObservableToolCallingManager(observationRegistry, toolCallbackResolver,
					toolExecutionExceptionProcessor);
		}

	}

}
