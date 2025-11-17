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

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.converter.FormatProvider;
import org.springframework.ai.tool.ToolCallback;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class DefaultBuilder extends Builder {

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
					this.customObservationConvention);

			if (chatOptions != null) {
				clientBuilder.defaultOptions(chatOptions);
			}

			chatClient = clientBuilder.build();
		}

		AgentLlmNode.Builder llmNodeBuilder = AgentLlmNode.builder().agentName(this.name).chatClient(chatClient);

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

		// Extract interceptor tools
		List<ToolCallback> interceptorTools = new ArrayList<>();
		if (CollectionUtils.isNotEmpty(modelInterceptors)) {
			interceptorTools = modelInterceptors.stream()
				.flatMap(interceptor -> interceptor.getTools().stream())
				.toList();
		}

		// Combine all tools: regularTools + regularTools
		List<ToolCallback> allTools = new ArrayList<>();
		allTools.addAll(interceptorTools);
		allTools.addAll(regularTools);

		// Set combined tools to LLM node
		if (CollectionUtils.isNotEmpty(allTools)) {
			llmNodeBuilder.toolCallbacks(allTools);
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

		toolNode = toolBuilder.build();

		return new ReactAgent(llmNode, toolNode, buildConfig(), this);
	}

}

