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
package com.alibaba.cloud.ai.graph.agent.interceptor.toolemulator;

import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallResponse;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolInterceptor;

import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tool interceptor that emulates specified tools using an LLM instead of executing them.
 *
 * This interceptor allows selective emulation of tools for testing purposes.
 * By default (when tools=null), all tools are emulated. You can specify which
 * tools to emulate by passing a list of tool names.
 *
 * Example:
 * // Emulate all tools (default behavior)
 * ToolEmulatorInterceptor emulator = ToolEmulatorInterceptor.builder()
 *     .model(chatModel)
 *     .build();
 *
 * // Emulate specific tools by name
 * ToolEmulatorInterceptor emulator = ToolEmulatorInterceptor.builder()
 *     .model(chatModel)
 *     .addTool("get_weather")
 *     .addTool("get_user_location")
 *     .build();
 *
 * // Emulate all except specified tools
 * ToolEmulatorInterceptor emulator = ToolEmulatorInterceptor.builder()
 *     .model(chatModel)
 *     .emulateAllTools(false)  // Only emulate specified tools
 *     .addTool("expensive_api")
 *     .build();
 */
public class ToolEmulatorInterceptor extends ToolInterceptor {

	private static final Logger log = LoggerFactory.getLogger(ToolEmulatorInterceptor.class);

	private final ChatModel emulatorModel;
	private final boolean emulateAll;
	private final Set<String> toolsToEmulate;
	private final String promptTemplate;

	private ToolEmulatorInterceptor(Builder builder) {
		this.emulatorModel = builder.emulatorModel;
		this.emulateAll = builder.emulateAll;
		this.toolsToEmulate = new HashSet<>(builder.toolsToEmulate);
		this.promptTemplate = builder.promptTemplate;
	}

	public static Builder builder() {
		return new Builder();
	}

	@Override
	public String getName() {
		return "ToolEmulator";
	}

	@Override
	public ToolCallResponse interceptToolCall(ToolCallRequest request, ToolCallHandler handler) {
		String toolName = request.getToolName();

		// Check if this tool should be emulated
		boolean shouldEmulate = emulateAll || toolsToEmulate.contains(toolName);

		if (!shouldEmulate) {
			// Let it execute normally by calling the handler
			return handler.call(request);
		}

		log.info("Emulating tool call: {}", toolName);

		try {
			// Build prompt for emulator LLM
			String prompt = String.format(promptTemplate,
					toolName,
					"No description available", // TODO: Tool descriptions not available in ToolCallRequest
					request.getArguments());

			// Get emulated response from LLM
			ChatResponse response = emulatorModel.call(new Prompt(new UserMessage(prompt)));
			String emulatedResult = response.getResult().getOutput().getText();

			log.debug("Emulated tool '{}' returned: {}", toolName,
					emulatedResult.length() > 100 ? emulatedResult.substring(0, 100) + "..." : emulatedResult);

			// Short-circuit: return emulated result without executing real tool
			return ToolCallResponse.of(request.getToolCallId(), toolName, emulatedResult);

		}
		catch (Exception e) {
			log.error("Failed to emulate tool call for: {}", toolName, e);
			// Fall back to actual execution on emulation failure
			return handler.call(request);
		}
	}

	public static class Builder {
		private final Set<String> toolsToEmulate = new HashSet<>();
		private ChatModel emulatorModel;
		private boolean emulateAll = true; // Default: emulate all tools
		private String promptTemplate = """
				You are emulating a tool call for testing purposes.
				
				Tool: %s
				Description: %s
				Arguments: %s
				
				Generate a realistic response that this tool would return given these arguments.
				Return ONLY the tool's output, no explanation or preamble.
				Introduce variation into your responses.
				""";

		/**
		 * Set the chat model used for emulation.
		 * Required.
		 */
		public Builder model(ChatModel model) {
			this.emulatorModel = model;
			return this;
		}

		/**
		 * Add a tool name to emulate.
		 * If emulateAllTools is true, this is ignored.
		 */
		public Builder addTool(String toolName) {
			this.toolsToEmulate.add(toolName);
			return this;
		}

		/**
		 * Add multiple tool names to emulate.
		 */
		public Builder addTools(Collection<String> toolNames) {
			this.toolsToEmulate.addAll(toolNames);
			return this;
		}

		/**
		 * Set whether to emulate all tools or only specified ones.
		 * Default is true (emulate all tools).
		 *
		 * Set to false to only emulate tools added via addTool().
		 */
		public Builder emulateAllTools(boolean emulateAll) {
			this.emulateAll = emulateAll;
			return this;
		}

		/**
		 * Set a custom prompt template for emulation.
		 * The template should accept 3 string format arguments:
		 * 1. Tool name
		 * 2. Tool description
		 * 3. Tool arguments (JSON)
		 */
		public Builder promptTemplate(String template) {
			this.promptTemplate = template;
			return this;
		}

		public ToolEmulatorInterceptor build() {
			if (emulatorModel == null) {
				throw new IllegalStateException("Emulator model is required");
			}
			return new ToolEmulatorInterceptor(this);
		}
	}
}

