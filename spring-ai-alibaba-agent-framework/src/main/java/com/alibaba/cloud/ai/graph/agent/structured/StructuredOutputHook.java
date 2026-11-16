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
package com.alibaba.cloud.ai.graph.agent.structured;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.agent.hook.ModelHook;

import org.springframework.ai.chat.messages.AssistantMessage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Internal AFTER_MODEL hook that derives the structured result from the assistant message.
 *
 * <p>
 * It runs after the user-provided AFTER_MODEL hooks (for example output PII redaction) and
 * before the graph checkpoint is written, so the {@code structured_output} state value
 * matches the message that is exposed to callers and is present in the saved state.
 * </p>
 */
@HookPositions({HookPosition.AFTER_MODEL})
public class StructuredOutputHook extends ModelHook {

	private final Class<?> outputType;

	public StructuredOutputHook(Class<?> outputType) {
		this.outputType = outputType;
	}

	@Override
	public String getName() {
		return StructuredOutputHook.class.getSimpleName();
	}

	@Override
	public int getOrder() {
		// AFTER_MODEL hooks execute in reverse list order, so the highest precedence makes
		// this hook run last, after all user output rewriting (for example PII redaction).
		return Integer.MIN_VALUE;
	}

	@Override
	public CompletableFuture<Map<String, Object>> afterModel(OverAllState state, RunnableConfig config) {
		AssistantMessage lastMessage = findLastAssistantMessage(state);
		if (lastMessage == null || lastMessage.hasToolCalls()) {
			// Intermediate ReAct round; the final answer is not available yet.
			return CompletableFuture.completedFuture(Map.of());
		}
		if (StructuredOutputSupport.isModelFailure(lastMessage)) {
			// A provider failure may contain JSON in its diagnostic message; it must not
			// be exposed as a successful structured result.
			return CompletableFuture.completedFuture(Map.of());
		}
		Object structuredOutput = StructuredOutputSupport.parseStructuredOutput(lastMessage.getText(), outputType);
		Map<String, Object> update = new HashMap<>();
		update.put(StructuredOutputSupport.STATE_KEY,
				structuredOutput != null ? structuredOutput : OverAllState.MARK_FOR_REMOVAL);
		return CompletableFuture.completedFuture(update);
	}

	private AssistantMessage findLastAssistantMessage(OverAllState state) {
		Object messages = state.value("messages").orElse(null);
		if (messages instanceof AssistantMessage assistantMessage) {
			return assistantMessage;
		}
		if (messages instanceof List<?> messageList) {
			for (int i = messageList.size() - 1; i >= 0; i--) {
				if (messageList.get(i) instanceof AssistantMessage assistantMessage) {
					return assistantMessage;
				}
			}
		}
		return null;
	}

}
