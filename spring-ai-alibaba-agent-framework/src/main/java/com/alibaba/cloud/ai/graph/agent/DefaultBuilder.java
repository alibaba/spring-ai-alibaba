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
package com.alibaba.cloud.ai.graph.agent;

import com.alibaba.cloud.ai.graph.agent.interceptor.Interceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolInterceptor;
import com.alibaba.cloud.ai.graph.agent.node.AgentLlmNode;
import com.alibaba.cloud.ai.graph.agent.node.AgentToolNode;
import io.micrometer.observation.ObservationRegistry;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.converter.FormatProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;

import org.springframework.ai.tool.execution.DefaultToolExecutionExceptionProcessor;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DefaultBuilder extends Builder {

	private static final Logger logger = LoggerFactory.getLogger(DefaultBuilder.class);

	public static final String POSSIBLE_LLM_TOOL_NAME_CHANGE_WARNING
				= "LLM may have adapted the tool name '{}', especially if the name was truncated due to length limits. If this is the case, you can customize the prefixing and processing logic using McpToolNamePrefixGenerator";

	@Override
	public ReactAgent build() {

		// Validate name is not empty
		if (!StringUtils.hasText(this.name)) {
			throw new IllegalArgumentException("Agent name must not be empty");
		}

		// Validate either chatClient or model is provided
		if (chatClient == null && model == null) {
			throw new IllegalArgumentException("Either chatClient or model must be provided");
		}

		if (chatClient == null) {

			ChatClient.Builder clientBuilder = ChatClient.builder(model, this.observationRegistry == null ? ObservationRegistry.NOOP : this.observationRegistry,
					this.customObservationConvention, this.advisorObservationConvention);

			if (chatOptions != null) {
				clientBuilder.defaultOptions(chatOptions);
			}

			chatClient = clientBuilder.build();
		}

		AgentLlmNode.Builder llmNodeBuilder = AgentLlmNode.builder()
				.agentName(this.name)
				.chatOptions(chatOptions)
				.chatClient(chatClient);

		if (outputKey != null && !outputKey.isEmpty()) {
			llmNodeBuilder.outputKey(outputKey);
		}

		if (systemPrompt != null) {
			llmNodeBuilder.systemPrompt(systemPrompt);
		}

		String outputSchema = null;
		if (StringUtils.hasLength(this.outputSchema) ) {
			outputSchema = this.outputSchema;
		} else if (this.outputType != null) {
			FormatProvider formatProvider = new BeanOutputConverter<>(this.outputType);
			outputSchema = formatProvider.getFormat();
		}

		if (StringUtils.hasLength(outputSchema)) {
			llmNodeBuilder.outputSchema(outputSchema);
		}

		// Separate unified interceptors by type
		if (CollectionUtils.isNotEmpty(interceptors)) {
			modelInterceptors = new ArrayList<>();
			toolInterceptors = new ArrayList<>();

			for (Interceptor interceptor : interceptors) {
				if (interceptor instanceof ModelInterceptor) {
					modelInterceptors.add((ModelInterceptor) interceptor);
				}
				if (interceptor instanceof ToolInterceptor) {
					toolInterceptors.add((ToolInterceptor) interceptor);
				}
			}
		}

		// Collect tools from interceptors
		// - regularTools: user-provided tools
		// - interceptorTools: tools from interceptors
		List<ToolCallback> regularTools = new ArrayList<>();

		// Extract regular tools from user-provided tools
		if (CollectionUtils.isNotEmpty(tools)) {
			regularTools.addAll(tools);
		}

		if (CollectionUtils.isNotEmpty(toolCallbackProviders)) {
			for (var provider : toolCallbackProviders) {
				regularTools.addAll(List.of(provider.getToolCallbacks()));
			}
		}

		if (CollectionUtils.isNotEmpty(toolNames)) {
			for (String toolName : toolNames) {
				// Skip the tool if it is already present in the request toolCallbacks.
				// That might happen if a tool is defined in the options
				// both as a ToolCallback and as a tool name.
				if (regularTools.stream().anyMatch(tool -> tool.getToolDefinition().name().equals(toolName))) {
					continue;
				}

				if (this.resolver == null) {
					throw new IllegalStateException("ToolCallbackResolver is null; cannot resolve tool name: " + toolName);
				}
				ToolCallback toolCallback = this.resolver.resolve(toolName);
				if (toolCallback == null) {
					logger.warn(POSSIBLE_LLM_TOOL_NAME_CHANGE_WARNING, toolName);
					throw new IllegalStateException("No ToolCallback found for tool name: " + toolName);
				}
				regularTools.add(toolCallback);
			}
		}

		// If regularTools is empty and resolver is provided, try to extract tools from resolver
		if (regularTools.isEmpty() && this.resolver != null) {
			// Check if resolver also implements ToolCallbackProvider
			if (this.resolver instanceof ToolCallbackProvider provider) {
				ToolCallback[] resolverTools = provider.getToolCallbacks();
				if (resolverTools != null && resolverTools.length > 0) {
					regularTools.addAll(List.of(resolverTools));
					if (logger.isDebugEnabled()) {
						logger.debug("Extracted {} tools from ToolCallbackResolver (ToolCallbackProvider)", resolverTools.length);
					}
				}
			}
			else {
				// This is a fallback for resolvers that don't implement ToolCallbackProvider
				try {
					Field toolsField = this.resolver.getClass().getDeclaredField("tools");
					toolsField.setAccessible(true);
					Object toolsObj = toolsField.get(this.resolver);
					if (toolsObj instanceof java.util.Map) {
						@SuppressWarnings("unchecked")
						java.util.Map<String, ToolCallback> toolsMap = (java.util.Map<String, ToolCallback>) toolsObj;
						if (!toolsMap.isEmpty()) {
							regularTools.addAll(toolsMap.values());
							if (logger.isDebugEnabled()) {
								logger.debug("Extracted {} tools from ToolCallbackResolver via reflection", toolsMap.size());
							}
						}
					}
				}
				catch (NoSuchFieldException | IllegalAccessException | ClassCastException e) {
					// Reflection failed, resolver doesn't have accessible tools field
					// This is expected for some resolver implementations
					if (logger.isTraceEnabled()) {
						logger.trace("Could not extract tools from resolver via reflection: {}", e.getMessage());
					}
				}
			}
		}

		// Extract interceptor tools
		List<ToolCallback> interceptorTools = new ArrayList<>();
		if (CollectionUtils.isNotEmpty(modelInterceptors)) {
			interceptorTools = modelInterceptors.stream()
				.flatMap(interceptor -> interceptor.getTools().stream())
				.toList();
		}

		// Combine all tools: interceptorTools + regularTools
		List<ToolCallback> allTools = new ArrayList<>();
		allTools.addAll(interceptorTools);
		allTools.addAll(regularTools);

		// Set combined tools to LLM node
		if (CollectionUtils.isNotEmpty(allTools)) {
			llmNodeBuilder.toolCallbacks(Collections.unmodifiableList(allTools));
		}

		if (enableLogging) {
			llmNodeBuilder.enableReasoningLog(true);
		}

		AgentLlmNode llmNode = llmNodeBuilder.build();

		// Setup tool node with all available tools
		AgentToolNode toolNode;
		AgentToolNode.Builder toolBuilder = AgentToolNode.builder().agentName(this.name);

		if (resolver != null) {
			toolBuilder.toolCallbackResolver(resolver);
		}
		if (CollectionUtils.isNotEmpty(allTools)) {
			toolBuilder.toolCallbacks(allTools);
		}

		if (enableLogging) {
			toolBuilder.enableActingLog(true);
		}
		if (toolExecutionExceptionProcessor == null) {
			toolBuilder.toolExecutionExceptionProcessor(DefaultToolExecutionExceptionProcessor.builder()
					.alwaysThrow(false)
					.build());
		} else {
			toolBuilder.toolExecutionExceptionProcessor(toolExecutionExceptionProcessor);
		}

		if (toolContext != null && !toolContext.isEmpty()) {
			toolBuilder.toolContext(toolContext);
		}

		toolNode = toolBuilder.build();

		return new ReactAgent(llmNode, toolNode, buildConfig(), this);
	}

}

