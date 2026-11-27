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
package com.alibaba.cloud.ai.graph.agent.extension.interceptor;

import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import com.alibaba.cloud.ai.graph.agent.tool.ToolCallResults;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.part.TextPart;
import org.springframework.ai.chat.messages.part.ToolResultPart;
import org.springframework.ai.model.tool.ToolCallDecisions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Middleware to patch dangling tool calls in the messages history.
 *
 * <p>This interceptor handles situations where an AI message contains tool calls
 * that don't have corresponding ToolResponseMessages in the conversation history.
 * This can happen when tool execution is interrupted or when new messages arrive
 * before tool responses can be added.</p>
 *
 * <p>The interceptor automatically adds cancellation messages for any dangling
 * tool calls, preventing errors when the conversation is sent to the model.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * PatchToolCallsInterceptor interceptor = PatchToolCallsInterceptor.builder().build();
 * agent.addInterceptor(interceptor);
 * </pre>
 */
public class PatchToolCallsInterceptor extends ModelInterceptor {

	private static final Logger log = LoggerFactory.getLogger(PatchToolCallsInterceptor.class);

	private static final String CANCELLATION_MESSAGE_TEMPLATE =
			"Tool call %s with id %s was cancelled - another message came in before it could be completed.";

	private PatchToolCallsInterceptor(Builder builder) {
		// Currently no configuration options, but builder pattern allows future extensibility
	}

	public static Builder builder() {
		return new Builder();
	}

	@Override
	public String getName() {
		return "PatchToolCalls";
	}

	@Override
	public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
		List<Message> messages = request.getMessages();

		if (messages == null || messages.isEmpty()) {
			return handler.call(request);
		}

		List<Message> patched = patchDanglingToolCalls(messages);
		if (patched == messages) {
			return handler.call(request);
		}
		return handler.call(ModelRequest.builder(request).messages(patched).build());
	}

	private List<Message> patchDanglingToolCalls(List<Message> messages) {
		List<Message> patchedMessages = new ArrayList<>();
		boolean hasPatches = false;
		for (int i = 0; i < messages.size();) {
			Message message = messages.get(i);
			if (!(message instanceof AssistantMessage assistant) || !assistant.hasToolCalls()) {
				patchedMessages.add(message);
				i++;
				continue;
			}
			int end = ToolCallResults.responseEnd(messages, i);
			patchedMessages.addAll(messages.subList(i, end));
			var results = messages.subList(i + 1, end).stream().map(ToolResponseMessage.class::cast)
					.flatMap(response -> response.getResults().stream()).toList();
			var settled = ToolCallResults.settledKeys(assistant, results);
			var keys = ToolCallDecisions.toolCallKeys(assistant);
			var calls = assistant.getToolCalls();
			List<ToolResultPart> missing = new ArrayList<>();
			for (int callIndex = 0; callIndex < calls.size(); callIndex++) {
				String key = keys.get(callIndex);
				if (!settled.contains(key)) {
					var call = calls.get(callIndex);
					String cancellation = String.format(CANCELLATION_MESSAGE_TEMPLATE, call.name(), key);
					var result = new ToolResultPart(call.id(), call.name(), List.of(TextPart.of(cancellation)),
							true, null, Map.of());
					missing.add(ToolCallResults.withCallKey(result, key));
					log.debug("Patching dangling tool call {} ({})", call.name(), key);
				}
			}
			if (!missing.isEmpty()) {
				patchedMessages.add(ToolResponseMessage.builder().metadata(Map.of("patched", true))
						.results(missing).build());
				hasPatches = true;
			}
			i = end;
		}
		return hasPatches ? patchedMessages : messages;
	}

	/**
	 * Builder for creating PatchToolCallsInterceptor instances.
	 */
	public static class Builder {

		public Builder() {
		}

		/**
		 * Build the PatchToolCallsInterceptor instance.
		 * @return A new PatchToolCallsInterceptor
		 */
		public PatchToolCallsInterceptor build() {
			return new PatchToolCallsInterceptor(this);
		}
	}
}

