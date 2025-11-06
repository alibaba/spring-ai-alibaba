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
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import reactor.core.publisher.Flux;

public class AgentLlmNode implements NodeActionWithConfig {

	private List<Advisor> advisors = new ArrayList<>();

	private List<ToolCallback> toolCallbacks = new ArrayList<>();

	private List<ModelInterceptor> modelInterceptors = new ArrayList<>();

	private String outputKey;

	private String outputSchema;

	private ChatClient chatClient;

	private String instruction;

	private ToolCallingChatOptions toolCallingChatOptions;

	public AgentLlmNode(Builder builder) {
		this.outputKey = builder.outputKey;
		this.outputSchema = builder.outputSchema;
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
			ModelRequest modelRequest = ModelRequest.builder()
					.messages(messages)
					.options(toolCallingChatOptions)
					.context(config.metadata().orElse(new HashMap<>()))
					.build();

			// Create base handler that actually calls the model with streaming
			ModelCallHandler baseHandler = request -> {
				try {
					Flux<ChatResponse> chatResponseFlux = buildChatClientRequestSpec(request).stream().chatResponse();
					return ModelResponse.of(chatResponseFlux);
				} catch (Exception e) {
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
			AssistantMessage responseOutput;

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
					ChatResponse response = buildChatClientRequestSpec(request).call().chatResponse();
					return ModelResponse.of(response.getResult().getOutput());
				} catch (Exception e) {
					return ModelResponse.of(new AssistantMessage("Exception: " + e.getMessage()));
				}
			};

			// Chain interceptors if any
			ModelCallHandler chainedHandler = InterceptorChain.chainModelInterceptors(
					modelInterceptors, baseHandler);

			// Execute the chained handler
			ModelResponse modelResponse = chainedHandler.call(modelRequest);
			responseOutput = (AssistantMessage) modelResponse.getMessage();

			Map<String, Object> updatedState = new HashMap<>();
			updatedState.put("messages", responseOutput);
			if (StringUtils.hasLength(this.outputKey)) {
				updatedState.put(this.outputKey, responseOutput);
			}

			return updatedState;
		}
	}

	public void setAdvisors(List<Advisor> advisors) {
		this.advisors = advisors;
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
                // 将outputSchema进行转义，避免PromptTemplate渲染时报错
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

		List<ToolCallback> filteredToolCallbacks = filterToolCallbacks(modelRequest);
		this.toolCallingChatOptions = ToolCallingChatOptions.builder()
				.toolCallbacks(filteredToolCallbacks)
				.internalToolExecutionEnabled(false)
				.build();

		ChatClient.ChatClientRequestSpec chatClientRequestSpec = chatClient.prompt()
				.options(toolCallingChatOptions)
				.messages(modelRequest.getMessages())
				.advisors(advisors);

		return chatClientRequestSpec;
	}

	public static class Builder {

		private String outputKey;

		private String outputSchema;

		private ChatClient chatClient;

		private List<Advisor> advisors;

		private List<ToolCallback> toolCallbacks;

		private List<ModelInterceptor> modelInterceptors;

		private String instruction;

		public Builder outputKey(String outputKey) {
			this.outputKey = outputKey;
			return this;
		}

		public Builder outputSchema(String outputSchema) {
			this.outputSchema = outputSchema;
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

		public AgentLlmNode build() {
			return new AgentLlmNode(this);
		}

	}

}
