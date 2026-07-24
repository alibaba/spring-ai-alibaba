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

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Middleware that normalizes tool-calling {@link AssistantMessage}s whose text content
 * is {@code null} before they are sent back to the model.
 *
 * <p>When a ReAct loop performs a tool call, the assistant message that carries the
 * {@code tool_calls} typically has no textual content, so {@link AssistantMessage#getText()}
 * is {@code null}. On the second {@code /chat/completions} call this message is sent back to
 * the model together with the tool result. Spring AI's OpenAI/DeepSeek-compatible
 * serialization layer drops the {@code content} field entirely when the text is {@code null},
 * producing a payload such as:</p>
 *
 * <pre>{@code
 * { "role": "assistant", "tool_calls": [ ... ] }
 * }</pre>
 *
 * <p>instead of:</p>
 *
 * <pre>{@code
 * { "role": "assistant", "content": null, "tool_calls": [ ... ] }
 * }</pre>
 *
 * <p>Stricter DeepSeek-compatible backends reject the former with a validation error
 * ({@code content} field required). This interceptor rebuilds such assistant messages with
 * an empty-string content ({@code ""}) while preserving their {@code toolCalls}, metadata,
 * and media, which is accepted by these backends.</p>
 *
 * <p>This is a compatibility shim: the ideal fix belongs in the upstream Spring AI serializer.
 * It is registered as a default {@link ModelInterceptor} in the agent framework and can be
 * disabled via {@code ReactAgent.builder().assistantMessageSanitizerEnabled(false)} for users
 * who require a literal {@code "content": null} once upstream provides it.</p>
 *
 * @see <a href="https://github.com/alibaba/spring-ai-alibaba/issues/4561">Issue #4561</a>
 */
public class AssistantMessageSanitizerInterceptor extends ModelInterceptor {

	private static final Logger log = LoggerFactory.getLogger(AssistantMessageSanitizerInterceptor.class);

	private static final String NAME = "AssistantMessageSanitizer";

	private AssistantMessageSanitizerInterceptor(Builder builder) {
		// Currently no configuration options, but builder pattern allows future extensibility
	}

	public static Builder builder() {
		return new Builder();
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
		List<Message> messages = request.getMessages();

		if (messages == null || messages.isEmpty()) {
			return handler.call(request);
		}

		List<Message> sanitized = sanitizeMessages(messages);

		// Only rebuild the request when at least one message was rewritten.
		if (sanitized != messages) {
			ModelRequest sanitizedRequest = ModelRequest.builder(request)
					.messages(sanitized)
					.build();
			return handler.call(sanitizedRequest);
		}

		return handler.call(request);
	}

	/**
	 * Rewrite tool-calling {@link AssistantMessage}s with {@code null} text so their content
	 * becomes an empty string, preserving tool calls, metadata, and media.
	 *
	 * @param messages the original message list
	 * @return a new list with normalized messages, or the original list if nothing needed fixing
	 */
	private List<Message> sanitizeMessages(List<Message> messages) {
		boolean needsFix = false;
		for (Message msg : messages) {
			if (needsSanitizing(msg)) {
				needsFix = true;
				break;
			}
		}

		if (!needsFix) {
			return messages;
		}

		List<Message> sanitized = new ArrayList<>(messages.size());
		for (Message msg : messages) {
			if (needsSanitizing(msg)) {
				AssistantMessage assistantMsg = (AssistantMessage) msg;
				// text == null -> use empty string; keep toolCalls, metadata and media intact.
				AssistantMessage fixed = AssistantMessage.builder()
						.content("")
						.toolCalls(assistantMsg.getToolCalls())
						.properties(assistantMsg.getMetadata())
						.media(assistantMsg.getMedia())
						.build();
				if (log.isDebugEnabled()) {
					log.debug("[{}] AssistantMessage text was null with tool calls; patched content to empty string", NAME);
				}
				sanitized.add(fixed);
			} else {
				sanitized.add(msg);
			}
		}

		return sanitized;
	}

	private static boolean needsSanitizing(Message msg) {
		return msg instanceof AssistantMessage assistantMsg
				&& assistantMsg.getText() == null
				&& assistantMsg.hasToolCalls();
	}

	/**
	 * Builder for creating {@link AssistantMessageSanitizerInterceptor} instances.
	 */
	public static class Builder {

		public Builder() {
		}

		/**
		 * Build the {@link AssistantMessageSanitizerInterceptor} instance.
		 * @return a new {@link AssistantMessageSanitizerInterceptor}
		 */
		public AssistantMessageSanitizerInterceptor build() {
			return new AssistantMessageSanitizerInterceptor(this);
		}
	}
}
