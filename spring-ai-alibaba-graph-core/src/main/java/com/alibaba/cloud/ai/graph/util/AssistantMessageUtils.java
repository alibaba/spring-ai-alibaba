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
package com.alibaba.cloud.ai.graph.util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

/**
 * Utility methods for working with {@link AssistantMessage} instances inside
 * {@link ChatResponse} objects. Centralizes the extraction logic so it can be
 * reused across different components without duplication.
 */
public final class AssistantMessageUtils {

	private AssistantMessageUtils() {
	}

	/**
	 * Extracts the most appropriate AssistantMessage from a ChatResponse for streaming.
	 * <p>
	 * Prefers AssistantMessage generations that contain tool calls, then falls back to
	 * the last non-null AssistantMessage. Returns {@code null} when no suitable
	 * AssistantMessage exists (e.g. usage-only chunks).
	 * @param response chat response to inspect
	 * @return selected assistant message or {@code null}
	 */
	public static AssistantMessage extractAssistantMessage(ChatResponse response) {
		return extractAssistantMessage(response, null);
	}

	/**
	 * Extracts the most appropriate AssistantMessage from a ChatResponse for streaming.
	 * <p>
	 * Prefers AssistantMessage generations that contain tool calls, then falls back to
	 * the last non-null AssistantMessage. Returns {@code null} when no suitable
	 * AssistantMessage exists (e.g. usage-only chunks).
	 * @param response chat response to inspect
	 * @param log optional logger for debug logging when extraction fails
	 * @return selected assistant message or {@code null}
	 */
	public static AssistantMessage extractAssistantMessage(ChatResponse response, Logger log) {
		if (response == null) {
			return null;
		}

		try {
			List<Generation> generations = response.getResults();
			if (generations != null && !generations.isEmpty()) {
				AssistantMessage fallback = null;
				for (Generation generation : generations) {
					if (generation == null) {
						continue;
					}
					// Generation#getOutput() already returns AssistantMessage in current
					// Spring AI versions, so we avoid pattern matching here to stay
					// compatible with compilers where "expression type is a subtype of
					// pattern type" is rejected.
					AssistantMessage assistantMessage = generation.getOutput();
					if (assistantMessage != null) {
						if (assistantMessage.hasToolCalls()) {
							// Prefer the first message that contains tool calls
							return assistantMessage;
						}
						// Remember the last non-null assistant message as a fallback
						fallback = assistantMessage;
					}
				}
				if (fallback != null) {
					return fallback;
				}
			}
		}
		catch (Exception ex) {
			// Defensive: if the underlying implementation changes and getResults() fails,
			// fall back to getResult().
			if (log != null && log.isDebugEnabled()) {
				log.debug("Failed to extract AssistantMessage from all generations, falling back to getResult()",
						ex);
			}
		}

		if (response.getResult() != null) {
			AssistantMessage assistant = response.getResult().getOutput();
			if (assistant != null) {
				return assistant;
			}
		}

		return null;
	}

	/**
	 * Merge tool calls from a previously aggregated AssistantMessage and the current
	 * streaming chunk. Tool calls are de-duplicated by id while preserving insertion
	 * order. Existing entries are not overwritten by newer chunks with the same id.
	 * @param lastToolCalls tool calls from the last aggregated message (may be null or
	 * empty)
	 * @param currentToolCalls tool calls from the current chunk (may be null or empty)
	 * @return merged, de-duplicated list of tool calls (never null)
	 */
	public static List<AssistantMessage.ToolCall> mergeToolCalls(List<AssistantMessage.ToolCall> lastToolCalls,
			List<AssistantMessage.ToolCall> currentToolCalls) {
		boolean lastEmpty = lastToolCalls == null || lastToolCalls.isEmpty();
		boolean currentEmpty = currentToolCalls == null || currentToolCalls.isEmpty();

		if (lastEmpty && currentEmpty) {
			return List.of();
		}
		if (lastEmpty) {
			return currentToolCalls;
		}
		if (currentEmpty) {
			return lastToolCalls;
		}

		Map<String, AssistantMessage.ToolCall> toolCallMap = new LinkedHashMap<>();
		for (AssistantMessage.ToolCall call : lastToolCalls) {
			toolCallMap.put(call.id(), call);
		}
		for (AssistantMessage.ToolCall call : currentToolCalls) {
			toolCallMap.putIfAbsent(call.id(), call);
		}
		return List.copyOf(toolCallMap.values());
	}

}
