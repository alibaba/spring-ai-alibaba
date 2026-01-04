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
package com.alibaba.cloud.ai.graph.agent.interceptor.missingtoolcall;

import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.tool.ToolCallback;

import java.util.ArrayList;
import java.util.List;

/**
 * Model interceptor that detects when the model should have called tools but didn't.
 *
 * <p>This interceptor monitors model responses and retries when:
 * <ul>
 *   <li>Tools are available in the request</li>
 *   <li>The model response is an AssistantMessage without tool calls</li>
 * </ul>
 *
 * <p>This helps handle cases where the model "hallucinates" and fails to use available tools,
 * which can cause the ReAct loop to terminate prematurely.
 *
 * <p>Example usage:
 * <pre>
 * MissingToolCallDetectionInterceptor interceptor = MissingToolCallDetectionInterceptor.builder()
 *     .maxRetries(2)
 *     .build();
 * </pre>
 */
public class MissingToolCallDetectionInterceptor extends ModelInterceptor {

	private static final String TOOL_CALL_PROMPT = 
		"Please use the available tools to complete this task. Your previous response did not include any tool calls, but tools are available and should be used.";

	private final int maxRetries;
	private final String retryPrompt;

	private MissingToolCallDetectionInterceptor(Builder builder) {
		this.maxRetries = builder.maxRetries;
		this.retryPrompt = builder.retryPrompt != null ? builder.retryPrompt : TOOL_CALL_PROMPT;
	}

	public static Builder builder() {
		return new Builder();
	}

	@Override
	public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
		// Execute the model call first
		ModelResponse response = handler.call(request);

		// Check if tools are available
		List<String> tools = request.getTools();
		List<ToolCallback> dynamicTools = request.getDynamicToolCallbacks();
		boolean hasTools = (tools != null && !tools.isEmpty()) || 
		                  (dynamicTools != null && !dynamicTools.isEmpty());

		if (!hasTools) {
			// No tools available, nothing to check
			return response;
		}

		// Check the response message
		if (!hasToolCalls(response)) {
			// Model did not call tools, but tools are available
			// Retry if maxRetries is set
			if (maxRetries > 0) {
				return retryWithToolPrompt(request, handler);
			}
		}

		return response;
	}

	/**
	 * Check if the response contains tool calls.
	 */
	private boolean hasToolCalls(ModelResponse response) {
		Object messageObj = response.getMessage();
		if (!(messageObj instanceof AssistantMessage)) {
			return false;
		}
		return ((AssistantMessage) messageObj).hasToolCalls();
	}

	/**
	 * Retry the model call with a prompt encouraging tool usage.
	 */
	private ModelResponse retryWithToolPrompt(
			ModelRequest request,
			ModelCallHandler handler) {

		ModelResponse lastResponse = null;
		for (int attempt = 0; attempt < maxRetries; attempt++) {
			ModelRequest retryRequest = createRetryRequest(request);
			ModelResponse retryResponse = handler.call(retryRequest);

			if (hasToolCalls(retryResponse)) {
				return retryResponse;
			}

			lastResponse = retryResponse;
		}

		return lastResponse;
	}

	/**
	 * Create a retry request with an additional user message prompting tool usage.
	 */
	private ModelRequest createRetryRequest(ModelRequest originalRequest) {
		// Add a user message to prompt tool usage
		List<Message> enhancedMessages = new ArrayList<>(originalRequest.getMessages());
		enhancedMessages.add(new UserMessage(retryPrompt));

		// Build the enhanced request, preserving system message
		ModelRequest.Builder builder = ModelRequest.builder(originalRequest)
				.messages(enhancedMessages);
		
		// Preserve system message if it exists
		if (originalRequest.getSystemMessage() != null) {
			builder.systemMessage(originalRequest.getSystemMessage());
		}

		return builder.build();
	}

	@Override
	public String getName() {
		return "MissingToolCallDetection";
	}

	/**
	 * Builder for creating MissingToolCallDetectionInterceptor instances.
	 */
	public static class Builder {
		private int maxRetries = 0;
		private String retryPrompt;

		/**
		 * Set the maximum number of retries when tool calls are missing.
		 * When set to 0 (default), no retries will be performed.
		 * When set to a positive number, the interceptor will retry the model call
		 * with a prompt encouraging tool usage.
		 * 
		 * @param maxRetries Maximum number of retries (must be >= 0)
		 */
		public Builder maxRetries(int maxRetries) {
			if (maxRetries < 0) {
				throw new IllegalArgumentException("maxRetries must be >= 0");
			}
			this.maxRetries = maxRetries;
			return this;
		}

		/**
		 * Set a custom prompt to use during retries.
		 * If not set, a default prompt will be used.
		 * 
		 * @param retryPrompt Custom prompt message to encourage tool usage
		 */
		public Builder retryPrompt(String retryPrompt) {
			this.retryPrompt = retryPrompt;
			return this;
		}

		/**
		 * Build the MissingToolCallDetectionInterceptor instance.
		 * @return A new MissingToolCallDetectionInterceptor
		 */
		public MissingToolCallDetectionInterceptor build() {
			return new MissingToolCallDetectionInterceptor(this);
		}
	}
}
