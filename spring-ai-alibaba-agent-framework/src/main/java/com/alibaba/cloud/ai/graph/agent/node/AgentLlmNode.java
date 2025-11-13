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
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.EmptyUsage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;

import org.springframework.util.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import reactor.core.publisher.Flux;

import static com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver.THREAD_ID_DEFAULT;

public class AgentLlmNode implements NodeActionWithConfig {
	public static final String MODEL_NODE_NAME = "model";
	private static final Logger logger = LoggerFactory.getLogger(AgentLlmNode.class);
	public static final String MODEL_ITERATION_KEY = "_MODEL_ITERATION_";

	private String agentName;

	private List<Advisor> advisors = new ArrayList<>();

	private List<ToolCallback> toolCallbacks = new ArrayList<>();

	private List<ModelInterceptor> modelInterceptors = new ArrayList<>();

	private String outputKey;

	private String outputSchema;

	private ChatClient chatClient;

	private String systemPrompt;

	private String instruction;

	private ToolCallingChatOptions toolCallingChatOptions;

	private boolean enableReasoningLog;

	public AgentLlmNode(Builder builder) {
		this.agentName = builder.agentName;
		this.outputKey = builder.outputKey;
		this.outputSchema = builder.outputSchema;
		this.systemPrompt = builder.systemPrompt;
		this.instruction = builder.instruction;
		if (builder.advisors != null) {
			this.advisors = builder.advisors;
		}
		if (builder.toolCallbacks != null) {
			this.toolCallbacks = builder.toolCallbacks;
		}
		if (builder.modelInterceptors != null) {
			this.modelInterceptors = builder.modelInterceptors;
		}
		this.chatClient = builder.chatClient;
		this.toolCallingChatOptions = ToolCallingChatOptions.builder()
				.toolCallbacks(toolCallbacks)
				.internalToolExecutionEnabled(false)
				.build();
		this.enableReasoningLog = builder.enableReasoningLog;;
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

		// add streaming support
		boolean stream = config.metadata("_stream_", new TypeRef<Boolean>(){}).orElse(true);
		if (stream) {
			if (state.value("messages").isEmpty()) {
				throw new IllegalArgumentException("Either 'instruction' or 'includeContents' must be set for Agent.");
			}
			@SuppressWarnings("unchecked")
			List<Message> messages = (List<Message>) state.value("messages").get();
			augmentUserMessage(messages, outputSchema);
			renderTemplatedUserMessage(messages, state.data());

			// Create ModelRequest
			ModelRequest.Builder requestBuilder = ModelRequest.builder()
					.messages(messages)
					.options(toolCallingChatOptions)
					.context(config.metadata().orElse(new HashMap<>()));
			if (StringUtils.hasLength(this.systemPrompt)) {
				requestBuilder.systemMessage(new SystemMessage(this.systemPrompt));
			}
			ModelRequest modelRequest = requestBuilder.build();

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
					Flux<ChatResponse> chatResponseFlux = buildChatClientRequestSpec(request).stream().chatResponse();
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

			// Build the base model call handler
			if (state.value("messages").isEmpty()) {
				throw new IllegalArgumentException("Either 'instruction' or 'includeContents' must be set for Agent.");
			}
			@SuppressWarnings("unchecked")
			List<Message> messages = (List<Message>) state.value("messages").get();
			augmentUserMessage(messages, outputSchema);
			renderTemplatedUserMessage(messages, state.data());

			// Create ModelRequest
			ModelRequest modelRequest = ModelRequest.builder()
					.messages(messages)
					.options(toolCallingChatOptions)
					.context(config.metadata().orElse(new HashMap<>()))
					.build();

			// Create base handler that actually calls the model
			ModelCallHandler baseHandler = request -> {
				try {
					if (enableReasoningLog) {
						String systemPrompt = request.getSystemMessage() != null ? request.getSystemMessage().getText() : "";
						logger.info("[ThreadId {}] Agent {} reasoning round {} with system prompt: {}.", config.threadId().orElse(THREAD_ID_DEFAULT), agentName, iterations.get(), systemPrompt);
					}

					ChatResponse response = buildChatClientRequestSpec(request).call().chatResponse();

					AssistantMessage responseMessage = new AssistantMessage("Empty response from model for unknown reason");
					if (response != null && response.getResult() != null) {
						responseMessage = response.getResult().getOutput();
					}

					if (enableReasoningLog) {
						logger.info("[ThreadId {}] Agent {} reasoning round {} returned: {}.", config.threadId().orElse(THREAD_ID_DEFAULT), agentName, iterations.get(), responseMessage);
					}

					return ModelResponse.of(responseMessage, response);
				} catch (Exception e) {
					logger.error("Exception during streaming model call: ", e);
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
		PromptTemplate promptTemplate = new PromptTemplate(prompt);
		return promptTemplate.render(params);
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

	public void renderTemplatedUserMessage(List<Message> messages, Map<String, Object> params) {
		for (int i = messages.size() - 1; i >= 0; i--) {
			Message message = messages.get(i);
			if (message instanceof AgentInstructionMessage instructionMessage) {
				AgentInstructionMessage newMessage = instructionMessage.mutate().text(renderPromptTemplate(instructionMessage.getText(), params)).build();
				messages.set(i, newMessage);
				break;
			}
		}
	}

	/**
	 * Filter tool callbacks based on the tools specified in ModelRequest.
	 * @param modelRequest the model request containing the list of tool names to filter by
	 * @return filtered list of tool callbacks matching the requested tools
	 */
	private List<ToolCallback> filterToolCallbacks(ModelRequest modelRequest) {
		if (modelRequest == null || modelRequest.getTools() == null || modelRequest.getTools().isEmpty()) {
			return toolCallbacks;
		}

		List<String> requestedTools = modelRequest.getTools();
		return toolCallbacks.stream()
				.filter(callback -> requestedTools.contains(callback.getToolDefinition().name()))
				.toList();
	}

	private ChatClient.ChatClientRequestSpec buildChatClientRequestSpec(ModelRequest modelRequest) {
		List<Message> messages = appendSystemPromptIfNeeded(modelRequest);

		List<ToolCallback> filteredToolCallbacks = filterToolCallbacks(modelRequest);
		this.toolCallingChatOptions = ToolCallingChatOptions.builder()
				.toolCallbacks(filteredToolCallbacks)
				.internalToolExecutionEnabled(false)
				.build();

		ChatClient.ChatClientRequestSpec chatClientRequestSpec = chatClient.prompt()
				.options(toolCallingChatOptions)
				.messages(messages)
				.advisors(advisors);

		return chatClientRequestSpec;
	}

	public String getName() {
		return MODEL_NODE_NAME;
	}

	public static class Builder {
		private String agentName;

		private String outputKey;

		private String outputSchema;

		private String systemPrompt;

		private ChatClient chatClient;

		private List<Advisor> advisors;

		private List<ToolCallback> toolCallbacks;

		private List<ModelInterceptor> modelInterceptors;

		private String instruction;

		private boolean enableReasoningLog;

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

		public AgentLlmNode build() {
			return new AgentLlmNode(this);
		}

	}

}
