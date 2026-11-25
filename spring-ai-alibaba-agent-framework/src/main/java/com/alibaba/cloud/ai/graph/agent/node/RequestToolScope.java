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

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.tool.ToolCallbackUtils;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.core.Ordered;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The callbacks selected for one model request, with names retained in saved messages. */
final class RequestToolScope {

	static final String TOOL_NAMES_METADATA = "_REQUEST_TOOL_NAMES_";

	private final Map<String, ToolCallback> callbacks = new LinkedHashMap<>();

	private final List<String> names;

	RequestToolScope(List<ToolCallback> tools) {
		for (ToolCallback tool : tools) {
			callbacks.putIfAbsent(tool.getToolDefinition().name(), tool);
		}
		this.names = List.copyOf(callbacks.keySet());
	}

	static String contextKey(String agentName) {
		return "_REQUEST_TOOL_SCOPE_:" + agentName;
	}

	static RequestToolScope get(RunnableConfig config, String agentName) {
		return (RequestToolScope) config.context().get(contextKey(agentName));
	}

	ToolCallback resolve(String name) {
		return callbacks.get(name);
	}

	AssistantMessage mark(AssistantMessage message) {
		Map<String, Object> metadata = new HashMap<>(message.getMetadata());
		metadata.put(TOOL_NAMES_METADATA, names);
		return message.mutate().properties(metadata).build();
	}

	/** Reconciles explicit callbacks with ChatClient defaults before the model call. */
	record RequestAdvisor(RunnableConfig config, String agentName, List<ToolCallback> callbacks,
			boolean selectionChanged) implements CallAdvisor, StreamAdvisor {

		@Override
		public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
			return chain.nextCall(prepare(request));
		}

		@Override
		public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
			return chain.nextStream(prepare(request));
		}

		private ChatClientRequest prepare(ChatClientRequest request) {
			if (!(request.prompt().getOptions() instanceof ToolCallingChatOptions options)) {
				return request;
			}
			List<ToolCallback> effectiveCallbacks = selectionChanged ? callbacks
					: ToolCallbackUtils.deduplicateByName(callbacks, options.getToolCallbacks());
			config.context().put(contextKey(agentName), new RequestToolScope(effectiveCallbacks));
			if (effectiveCallbacks.equals(options.getToolCallbacks())) {
				return request;
			}
			var prompt = new Prompt(request.prompt().getInstructions(),
					options.mutate().toolCallbacks(effectiveCallbacks).build());
			return new ChatClientRequest(prompt, request.context());
		}

		@Override
		public String getName() {
			return "RequestToolsAdvisor";
		}

		@Override
		public int getOrder() {
			return Ordered.HIGHEST_PRECEDENCE;
		}
	}

}
