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
import com.alibaba.cloud.ai.graph.action.NodeActionWithConfig;
import com.alibaba.cloud.ai.graph.serializer.AgentInstructionMessage;
import com.alibaba.cloud.ai.graph.utils.TypeRef;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.InterceptorChain;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.DefaultChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.EmptyUsage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.template.TemplateRenderer;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;

import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import reactor.core.publisher.Flux;

import static com.alibaba.cloud.ai.graph.RunnableConfig.AGENT_MODEL_NAME;
import static com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver.THREAD_ID_DEFAULT;

public class AgentLlmNode implements NodeActionWithConfig {
	private static final Logger logger = LoggerFactory.getLogger(AgentLlmNode.class);
	public static final String MODEL_ITERATION_KEY = "_MODEL_ITERATION_";

	private String agentName;

	private List<Advisor> advisors = new ArrayList<>();

	// FIXME: toolCallbacks should be managed in chatOptions only. Currently it's guaranteed immutable with unmodifiableList.
	private List<ToolCallback> toolCallbacks = new ArrayList<>();

	private List<ToolCallbackProvider> toolCallbackProviders = new ArrayList<>();

	private List<ModelInterceptor> modelInterceptors = new ArrayList<>();

	private String outputKey;

	private String outputSchema;

	private ChatClient chatClient;

	private String systemPrompt;

	private TemplateRenderer templateRenderer;

	private String instruction;

	private ToolCallingChatOptions chatOptions;

	private boolean enableReasoningLog;

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
			this.toolCallbacks = builder.toolCallbacks;
		}
		if (builder.toolCallbackProviders != null) {
			this.toolCallbackProviders = builder.toolCallbackProviders;
		}
		if (builder.modelInterceptors != null) {
			this.modelInterceptors = builder.modelInterceptors;
		}
		this.chatClient = builder.chatClient;
		this.chatOptions = buildChatOptions(builder.chatOptions, this.toolCallbacks);
		this.enableReasoningLog = builder.enableReasoningLog;
	}

	public static Builder builder() {
		return new Builder();
	}

	public void setToolCallbacks(List<ToolCallback> toolCallbacks) {
		this.toolCallbacks = toolCallbacks;
	}

	public void setModelInterceptors(List<ModelInterceptor> modelInterceptors) {
		this.modelInterceptors = modelInterceptors;
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
		List<Message> messages = new ArrayList<>();
		if (state.value("messages").isEmpty()) {
			// try with "input" key, which is more commonly used in graph input when agent is used as a node.
			if (state.value("input").isPresent()) {
				messages.add(new UserMessage(state.value("input").get().toString()));
			} else {
				throw new IllegalArgumentException("Either 'instruction' or 'includeContents' must be set for Agent.");
			}
		} else {
			messages = (List<Message>) state.value("messages").get();
		}

		augmentUserMessage(messages, outputSchema);
		renderTemplatedUserMessage(messages, state.data(), config.metadata());

		// Dynamically resolve all tools (static + providers)
		List<ToolCallback> currentTools = resolveAllTools();

		// Create ModelRequest
		ModelRequest.Builder requestBuilder = ModelRequest.builder()
				.messages(messages)
				.options(this.chatOptions != null ? this.chatOptions.copy() : null)
				.context(config.metadata().orElse(new HashMap<>()));

	       // Extract tool names and descriptions from currentTools and pass them to ModelRequest
	       if (currentTools != null && !currentTools.isEmpty()) {
	           List<String> toolNames = new ArrayList<>();
	           Map<String, String> toolDescriptions = new HashMap<>();
	           for (ToolCallback callback : currentTools) {
	               String name = callback.getToolDefinition().name();
	               String description = callback.getToolDefinition().description();
	               toolNames.add(name);
	               if (description != null && !description.isEmpty()) {
	                   toolDescriptions.put(name, description);
	               }
	           }
	           requestBuilder.tools(toolNames);
	           requestBuilder.toolDescriptions(toolDescriptions);
	       }

		if (StringUtils.hasLength(this.systemPrompt)) {
			requestBuilder.systemMessage(new SystemMessage(this.systemPrompt));
		}

		if (StringUtils.hasLength(this.instruction)) {
			List<Message> messagesWithInstruction = new ArrayList<>();
			messagesWithInstruction.add(new UserMessage(this.instruction));
			messagesWithInstruction.addAll(messages);
			requestBuilder.messages(messagesWithInstruction);
		}

		ModelRequest modelRequest = requestBuilder.build();

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
					Flux<ChatResponse> chatResponseFlux = buildChatClientRequestSpec(request, config, currentTools).stream().chatResponse();
					if (enableReasoningLog) {
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
					return ModelResponse.of(chatResponseFlux);
				} catch (Exception e) {
					logger.error("Exception during streaming model call: ", e);
					return ModelResponse.of(new AssistantMessage("Exception: " + e.getMessage()));
				}
			};

			// Chain interceptors if any
			ModelCallHandler chainedHandler = InterceptorChain.chainModelInterceptors(
					modelInterceptors, baseHandler);

			// Execute the chained handler
			ModelResponse modelResponse = chainedHandler.call(modelRequest);
			return Map.of(StringUtils.hasLength(this.outputKey) ? this.outputKey : "messages", modelResponse.getMessage());
		} else {
			// Create base handler that actually calls the model
			ModelCallHandler baseHandler = request -> {
				try {
					if (enableReasoningLog) {
						String systemPrompt = request.getSystemMessage() != null ? request.getSystemMessage().getText() : "";
						logger.info("[ThreadId {}] Agent {} reasoning round {} with system prompt: {}.", config.threadId().orElse(THREAD_ID_DEFAULT), agentName, iterations.get(), systemPrompt);
					}

					ChatResponse response = buildChatClientRequestSpec(request, config, currentTools).call().chatResponse();

					AssistantMessage responseMessage = new AssistantMessage("Empty response from model for unknown reason");
					if (response != null && response.getResult() != null) {
						responseMessage = response.getResult().getOutput();
					}

					if (enableReasoningLog) {
						logger.info("[ThreadId {}] Agent {} reasoning round {} returned: {}.", config.threadId().orElse(THREAD_ID_DEFAULT), agentName, iterations.get(), responseMessage);
					}

					return ModelResponse.of(responseMessage, response);
				} catch (Exception e) {
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

			Map<String, Object> updatedState = new HashMap<>();
			updatedState.put("_TOKEN_USAGE_", tokenUsage);
			updatedState.put("messages", modelResponse.getMessage());
			if (StringUtils.hasLength(this.outputKey)) {
				updatedState.put(this.outputKey, modelResponse.getMessage());
			}

			return updatedState;
		}
	}

	public void setAdvisors(List<Advisor> advisors) {
		this.advisors = advisors;
	}

	/**
	 * Dynamically resolve all tools from static toolCallbacks and toolCallbackProviders.
	 * This method is called on each LLM invocation to support dynamic tool discovery (e.g., MCP).
	 *
	 * @return combined list of all available tools
	 */
	private List<ToolCallback> resolveAllTools() {
		List<ToolCallback> allTools = new ArrayList<>(this.toolCallbacks);
		
		// Dynamically get tools from providers
		for (ToolCallbackProvider provider : toolCallbackProviders) {
			ToolCallback[] providerTools = provider.getToolCallbacks();
			if (providerTools != null && providerTools.length > 0) {
				allTools.addAll(List.of(providerTools));
			}
		}
		
		return allTools;
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

	/**
	 * Build chat options by merging toolCallbacks with the provided chatOptions.
	 * If chatOptions is null or not of type ToolCallingChatOptions, create a new ToolCallingChatOptions.
	 * If chatOptions is ToolCallingChatOptions, merge toolCallbacks (toolCallbacks takes precedence).
	 *
	 * @param chatOptions the original chat options
	 * @param toolCallbacks the tool callbacks to be included
	 * @return merged ToolCallingChatOptions
	 */
	@Nullable
	private ToolCallingChatOptions buildChatOptions(ChatOptions chatOptions, List<ToolCallback> toolCallbacks) {
		if (chatOptions == null && (toolCallbacks == null || toolCallbacks.isEmpty())) {
			return null;
		}

		if (chatOptions != null) {
			if (chatOptions instanceof ToolCallingChatOptions builderToolCallingOptions) {
				ToolCallingChatOptions copiedOptions = builderToolCallingOptions.copy();

				List<ToolCallback> mergedToolCallbacks = new ArrayList<>(toolCallbacks);
				// Add callbacks from chatOptions that are not already present (toolCallbacks takes precedence)
				for (ToolCallback callback : builderToolCallingOptions.getToolCallbacks()) {
					boolean exists = mergedToolCallbacks.stream()
							.anyMatch(tc -> tc.getToolDefinition().name().equals(callback.getToolDefinition().name()));
					if (!exists) {
						mergedToolCallbacks.add(callback);
					}
				}

				copiedOptions.setToolCallbacks(mergedToolCallbacks);
				copiedOptions.setInternalToolExecutionEnabled(false);
				return copiedOptions;
			} else {
				logger.warn("The provided chatOptions is not of type ToolCallingChatOptions (actual type: {}). " +
								"It will not take effect. Creating a new ToolCallingChatOptions with toolCallbacks instead.",
						chatOptions.getClass().getName());
			}
		}

		return ToolCallingChatOptions.builder()
				.toolCallbacks(toolCallbacks)
				.internalToolExecutionEnabled(false)
				.build();
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
					messages.set(i, userMessage.mutate().text(userMessage.getText() + System.lineSeparator() + outputSchema).build());
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

	/**
	 * Filter tool callbacks based on the tools specified in ModelRequest.
	 * @param modelRequest the model request containing the list of tool names to filter by
	 * @param currentTools the dynamically resolved tools to filter from
	 * @return filtered list of tool callbacks matching the requested tools
	 */
	private List<ToolCallback> filterToolCallbacks(ModelRequest modelRequest, List<ToolCallback> currentTools) {
		List<ToolCallback> toolCallbacks = new ArrayList<>();
		if (modelRequest == null) {
			toolCallbacks.addAll(currentTools);
			return toolCallbacks;
		}

		if (modelRequest.getOptions() != null && modelRequest.getOptions().getToolCallbacks() != null) {
			toolCallbacks.addAll(modelRequest.getOptions().getToolCallbacks());
		} else {
			// do nothing

			// by default, buildChatOptions() makes sure 'modelRequest.getOptions().getToolCallbacks()' is always set.
			// this leaves room for users to disable all tools by setting empty toolCallbacks in options.
		}

		List<String> requestedTools = modelRequest.getTools();
		if (requestedTools == null || requestedTools.isEmpty()) {
			return toolCallbacks;
		}
		return new ArrayList<>(toolCallbacks.stream()
				.filter(callback -> requestedTools.contains(callback.getToolDefinition().name()))
				.toList());
	}

	private ChatClient.ChatClientRequestSpec buildChatClientRequestSpec(ModelRequest modelRequest, RunnableConfig config, List<ToolCallback> currentTools) {
		List<Message> messages = appendSystemPromptIfNeeded(modelRequest);

		// NOTICE! If both tools(ToolSelectionInterceptor) and options are customized in ModelRequest, tools will override toolcall setting in options.
		List<ToolCallback> filteredToolCallbacks = filterToolCallbacks(modelRequest, currentTools);

		if (!CollectionUtils.isEmpty(modelRequest.getDynamicToolCallbacks())) {
			filteredToolCallbacks.addAll(modelRequest.getDynamicToolCallbacks());
			// FIXME, use RunnableConfig to pass dynamic tool callbacks to tool node via config context (internal use)
			config.context().put(RunnableConfig.DYNAMIC_TOOL_CALLBACKS_METADATA_KEY, modelRequest.getDynamicToolCallbacks());
		}

		var promptSpec = this.chatClient.prompt()
                .messages(messages)
                .advisors(this.advisors);

        ToolCallingChatOptions requestOptions = modelRequest.getOptions();

        if (requestOptions != null) {
			ToolCallingChatOptions copiedOptions = requestOptions.copy();
            copiedOptions.setToolCallbacks(filteredToolCallbacks);
			// force disable internal tool execution to avoid conflict with Agent framework's tool execution management.
            copiedOptions.setInternalToolExecutionEnabled(false);
            promptSpec.options(copiedOptions);
        } else {
			// Check if user has set default options in ChatModel or ChatClient.
			if (promptSpec instanceof DefaultChatClient.DefaultChatClientRequestSpec defaultChatClientRequestSpec) {
				ChatOptions options = defaultChatClientRequestSpec.getChatOptions();
				// If no default options set, create new ToolCallingChatOptions with filtered tool callbacks and toolExecution disabled.
				if (options == null) {
					options = ToolCallingChatOptions.builder()
							.toolCallbacks(filteredToolCallbacks)
							.internalToolExecutionEnabled(false)
							.build();
					defaultChatClientRequestSpec.options(options);
				}
				// If options is ToolCallingChatOptions, set filtered tool callbacks and toolExecution disabled.
				else if (options instanceof ToolCallingChatOptions toolCallingChatOptions) {
					ToolCallingChatOptions copiedOptions = toolCallingChatOptions.copy();
					copiedOptions.setToolCallbacks(filteredToolCallbacks);
					copiedOptions.setInternalToolExecutionEnabled(false);
					defaultChatClientRequestSpec.options(copiedOptions);
				}
			} else if (!filteredToolCallbacks.isEmpty()) {
				promptSpec.tools(filteredToolCallbacks);
			}
        }

        return promptSpec;
	}

	public String getName() {
		return AGENT_MODEL_NAME;
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

		private List<ToolCallbackProvider> toolCallbackProviders;

		private List<ModelInterceptor> modelInterceptors;

		private String instruction;

		private boolean enableReasoningLog;

		private ChatOptions chatOptions;

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

		public Builder toolCallbackProviders(List<ToolCallbackProvider> toolCallbackProviders) {
			this.toolCallbackProviders = toolCallbackProviders;
			return this;
		}

		public Builder modelInterceptors(List<ModelInterceptor> modelInterceptors) {
			this.modelInterceptors = modelInterceptors;
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
