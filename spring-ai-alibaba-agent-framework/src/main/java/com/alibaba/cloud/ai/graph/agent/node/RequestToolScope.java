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
import com.alibaba.cloud.ai.graph.agent.context.ContextWindowPolicy;
import com.alibaba.cloud.ai.graph.agent.tool.ToolCallbackUtils;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.core.Ordered;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.UnaryOperator;

import reactor.core.publisher.Flux;

/** Binds one model reply to the tools on its outbound model request. */
final class RequestToolScope {

	private static final String METADATA_KEY = "_REQUEST_TOOL_SCOPE_";

	private final String agentName;
	private final String contextKey = METADATA_KEY + UUID.randomUUID();
	private final Map<String, ToolCallback> registered = new LinkedHashMap<>();

	private volatile List<ToolCallback> legacyCallbacks = List.of();
	private volatile CapturedTools captured = new CapturedTools(Map.of(), Map.of());

	RequestToolScope(String agentName, List<ToolCallback> registered) {
		this.agentName = Objects.toString(agentName, "");
		registered.forEach(callback -> this.registered.put(callback.getToolDefinition().name(), callback));
	}

	ToolsAdvisor prepareAdvisor(List<ToolCallback> requestCallbacks, List<ToolCallback> dynamicCallbacks,
			boolean replaceDefaults, List<String> selection, ChatOptions targetOptions) {
		return new ToolsAdvisor("PrepareRequestTools", Ordered.HIGHEST_PRECEDENCE, request -> {
			var options = request.prompt().getOptions();
			var defaults = options instanceof ToolCallingChatOptions toolOptions ? toolOptions.getToolCallbacks() : null;
			var callbacks = replaceDefaults ? requestCallbacks
					: ToolCallbackUtils.deduplicateByName(requestCallbacks, defaults);
			var prepared = select(ToolCallbackUtils.deduplicateByName(callbacks, dynamicCallbacks), selection);
			var outbound = targetOptions == null ? request : new ChatClientRequest(
					new Prompt(request.prompt().getInstructions(), targetOptions), request.context());
			legacyCallbacks = outbound.prompt().getOptions() instanceof ToolCallingChatOptions ? List.of() : prepared;
			return withCallbacks(outbound, prepared);
		});
	}

	Advisor captureAdvisor(List<String> selection, ContextWindowPolicy policy, ChatModel target) {
		var capture = new ToolsAdvisor("CaptureRequestTools", Ordered.LOWEST_PRECEDENCE - 1, request -> {
			var options = request.prompt().getOptions();
			var callbacks = options instanceof ToolCallingChatOptions toolOptions
					? select(ToolCallbackUtils.deduplicateByName(toolOptions.getToolCallbacks()), selection)
					: legacyCallbacks;
			Map<String, ToolCallback> active = new LinkedHashMap<>();
			Map<String, String> resumable = new LinkedHashMap<>();
			for (var callback : callbacks) {
				String name = callback.getToolDefinition().name();
				active.put(name, callback);
				if (registered.get(name) == callback) {
					resumable.put(name, binding(callback));
				}
			}
			captured = new CapturedTools(Map.copyOf(active), Map.copyOf(resumable));
			return withCallbacks(request, callbacks);
		});
		return policy == null && target == null ? capture : new ContextWindowAdvisor(policy, capture, target);
	}

	private static List<ToolCallback> select(List<ToolCallback> callbacks, List<String> selection) {
		return selection == null ? callbacks
				: callbacks.stream().filter(callback -> selection.contains(callback.getToolDefinition().name())).toList();
	}

	private static ChatClientRequest withCallbacks(ChatClientRequest request, List<ToolCallback> callbacks) {
		if (request.prompt().getOptions() instanceof ToolCallingChatOptions options
				&& !Objects.equals(options.getToolCallbacks(), callbacks)) {
			// Prompt.mutate() in Spring AI 2.1.0 copies tool results through the
			// legacy text-only view. Retain the original messages and their parts.
			var prompt = new Prompt(request.prompt().getInstructions(), options.mutate().toolCallbacks(callbacks).build());
			return new ChatClientRequest(prompt, request.context());
		}
		return request;
	}

	AssistantMessage mark(AssistantMessage message, RunnableConfig config) {
		config.context().put(contextKey, this);
		Map<String, Object> properties = new LinkedHashMap<>(message.getMetadata());
		properties.put(METADATA_KEY, Map.of("agent", agentName, "key", contextKey,
				"bindings", captured.checkpointBindings()));
		return message.mutate().properties(properties).build();
	}

	static boolean isScoped(AssistantMessage message) {
		return message.getMetadata().containsKey(METADATA_KEY);
	}

	static ToolCallback resolve(AssistantMessage message, String agentName, String name, RunnableConfig config,
			ToolCallback registeredCallback) {
		if (!(message.getMetadata().get(METADATA_KEY) instanceof Map<?, ?> marker)
				|| !Objects.toString(agentName, "").equals(marker.get("agent"))
				|| !(marker.get("key") instanceof String key)) {
			return null;
		}
		if (config.context().get(key) instanceof RequestToolScope scope) {
			var snapshot = scope.captured;
			var callback = snapshot.callbacks().get(name);
			// An unfinished request can use its actual callbacks, provided any registered
			// implementation it relied on has not been replaced or removed.
			return snapshot.checkpointBindings().containsKey(name) && callback != registeredCallback ? null : callback;
		}
		// A checkpoint stores bindings only for tools registered on the agent. Request
		// closures supplied by advisors/interceptors cannot be reconstructed by name.
		if (registeredCallback != null && marker.get("bindings") instanceof Map<?, ?> bindings
				&& binding(registeredCallback).equals(bindings.get(name))) {
			return registeredCallback;
		}
		return null;
	}

	static void clear(AssistantMessage message, RunnableConfig config) {
		if (message.getMetadata().get(METADATA_KEY) instanceof Map<?, ?> marker
				&& marker.get("key") instanceof String key
				&& config.context().get(key) instanceof RequestToolScope scope) {
			config.context().remove(key, scope);
		}
	}

	private static String binding(ToolCallback callback) {
		var definition = callback.getToolDefinition();
		var metadata = callback.getToolMetadata();
		String signature = String.join("\u0000", callback.getClass().getName(), definition.name(),
				Objects.toString(definition.description(), ""), Objects.toString(definition.inputSchema(), ""),
				Boolean.toString(metadata.returnDirect()), Boolean.toString(metadata.requiresConfirmation()));
		try {
			var digest = MessageDigest.getInstance("SHA-256").digest(signature.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 is unavailable", ex);
		}
	}

	private record CapturedTools(Map<String, ToolCallback> callbacks, Map<String, String> checkpointBindings) {
	}

	record ToolsAdvisor(String getName, int getOrder, UnaryOperator<ChatClientRequest> prepare)
			implements CallAdvisor, StreamAdvisor {

		@Override
		public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
			return chain.nextCall(prepare.apply(request));
		}

		@Override
		public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
			return chain.nextStream(prepare.apply(request));
		}
	}
}
