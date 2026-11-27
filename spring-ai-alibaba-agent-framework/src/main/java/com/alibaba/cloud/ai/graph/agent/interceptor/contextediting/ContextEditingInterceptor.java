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
package com.alibaba.cloud.ai.graph.agent.interceptor.contextediting;

import com.alibaba.cloud.ai.graph.agent.context.ToolResultArchive;
import com.alibaba.cloud.ai.graph.agent.hook.TokenCounter;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.part.MessagePart;
import org.springframework.ai.chat.messages.part.TextPart;
import org.springframework.ai.chat.messages.part.ToolCallPart;
import org.springframework.ai.chat.messages.part.ToolResultPart;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Context editing interceptor that clears older tool results once the conversation
 * grows beyond a configurable token threshold.
 *
 * This mirrors Anthropic's context editing capabilities by managing tool result
 * history to stay within token limits.
 *
 * Example:
 * ContextEditingInterceptor interceptor = ContextEditingInterceptor.builder()
 *     .trigger(100000)
 *     .keep(3)
 *     .clearAtLeast(1000)
 *     .build();
 */
public class ContextEditingInterceptor extends ModelInterceptor {

	private static final Logger log = LoggerFactory.getLogger(ContextEditingInterceptor.class);
	private static final String DEFAULT_PLACEHOLDER = "[cleared]";

	private final int trigger;
	private final int clearAtLeast;
	private final int keep;
	private final boolean clearToolInputs;
	private final Set<String> excludeTools;
	private final String placeholder;
	private final TokenCounter tokenCounter;

	private ContextEditingInterceptor(Builder builder) {
		this.trigger = builder.trigger;
		this.clearAtLeast = builder.clearAtLeast;
		this.keep = builder.keep;
		this.clearToolInputs = builder.clearToolInputs;
		this.excludeTools = builder.excludeTools != null
				? new HashSet<>(builder.excludeTools)
				: new HashSet<>();
		this.placeholder = builder.placeholder;
		this.tokenCounter = builder.tokenCounter;
	}

	public static Builder builder() {
		return new Builder();
	}

	@Override
	public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
		List<Message> messages = new ArrayList<>(request.getMessages());

		int tokens = tokenCounter.countTokens(messages);

		if (tokens <= trigger) {
			// Token count is below trigger, no editing needed
			return handler.call(request);
		}

		log.info("Token count {} exceeds trigger {}, clearing tool results", tokens, trigger);

		// Find tool messages that can be cleared
		List<ClearableToolMessage> candidates = findClearableCandidates(messages);

		if (candidates.isEmpty()) {
			log.debug("No tool messages to clear");
			return handler.call(request);
		}

		int target = clearAtLeast > 0 ? clearAtLeast : tokens - trigger;
		int clearedTokens = 0;
		for (ClearableToolMessage candidate : candidates) {
			messages.set(candidate.index(), candidate.replacement());
			clearedTokens += candidate.estimatedTokens();
			if (clearedTokens >= target) {
				break;
			}
		}

		return handler.call(ModelRequest.builder(request).messages(messages).build());
	}

	private List<ClearableToolMessage> findClearableCandidates(List<Message> messages) {
		List<ClearableToolMessage> candidates = new ArrayList<>();
		for (int i = 0; i < messages.size(); i++) {
			Message original = messages.get(i);
			Message replacement = clearToolContent(original);
			if (replacement != original) {
				candidates.add(new ClearableToolMessage(i, replacement, tokenCounter.countTokens(List.of(original))));
			}
		}
		return candidates.subList(0, Math.max(0, candidates.size() - keep));
	}

	private Message clearToolContent(Message message) {
		if (message instanceof ToolResponseMessage response
				&& response.getResults().stream().noneMatch(result -> excludeTools.contains(result.name()))) {
			var edited = response.getParts().stream().map(this::clearResult).toList();
			if (!edited.equals(response.getParts())) {
				return ToolResponseMessage.builder().parts(edited).metadata(response.getMetadata()).build();
			}
		}
		if (clearToolInputs && message instanceof AssistantMessage assistant
				&& assistant.getToolCalls().stream().noneMatch(call -> excludeTools.contains(call.name()))) {
			var edited = assistant.getParts().stream().map(this::clearCall).toList();
			if (!edited.equals(assistant.getParts())) {
				return AssistantMessage.builder().parts(edited).properties(assistant.getMetadata()).build();
			}
		}
		return message;
	}

	private MessagePart clearResult(MessagePart part) {
		if (part instanceof ToolResultPart result && result.payload() == null && !ToolResultArchive.isArchived(result)) {
			return new ToolResultPart(result.id(), result.name(), List.of(TextPart.of(placeholder)),
					result.isError(), null, result.attributes());
		}
		return part;
	}

	private MessagePart clearCall(MessagePart part) {
		// A provider may verify the payload against the original call.
		if (part instanceof ToolCallPart call && call.payload() == null) {
			var original = call.toolCall();
			var cleared = new AssistantMessage.ToolCall(original.id(), original.type(), original.name(), "{}");
			return new ToolCallPart(cleared, null, call.attributes());
		}
		return part;
	}

	@Override
	public String getName() {
		return "ContextEditing";
	}

	private record ClearableToolMessage(int index, Message replacement, int estimatedTokens) {
	}

	public static class Builder {
		private int trigger = 100000;
		private int clearAtLeast = 0;
		private int keep = 3;
		private boolean clearToolInputs = false;
		private Set<String> excludeTools;
		private String placeholder = DEFAULT_PLACEHOLDER;
		private TokenCounter tokenCounter = TokenCounter.approximateMsgCounter();

		public Builder trigger(int trigger) {
			this.trigger = trigger;
			return this;
		}

		public Builder clearAtLeast(int clearAtLeast) {
			this.clearAtLeast = clearAtLeast;
			return this;
		}

		public Builder keep(int keep) {
			this.keep = keep;
			return this;
		}

		public Builder clearToolInputs(boolean clearToolInputs) {
			this.clearToolInputs = clearToolInputs;
			return this;
		}

		public Builder excludeTools(Set<String> excludeTools) {
			this.excludeTools = excludeTools;
			return this;
		}

		public Builder excludeTools(String... toolNames) {
			this.excludeTools = new HashSet<>(Arrays.asList(toolNames));
			return this;
		}

		public Builder placeholder(String placeholder) {
			this.placeholder = placeholder;
			return this;
		}

		public Builder tokenCounter(TokenCounter tokenCounter) {
			this.tokenCounter = tokenCounter;
			return this;
		}

		public ContextEditingInterceptor build() {
			return new ContextEditingInterceptor(this);
		}
	}
}
