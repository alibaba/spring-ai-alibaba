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
package com.alibaba.cloud.ai.graph.agent.tool;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.part.ToolResultPart;
import org.springframework.ai.model.tool.ToolCallDecisions;

/**
 * Correlates graph tool results with Spring AI's confirmation keys. For calls that
 * need a position key, the original provider ID stays on the wire result.
 */
public final class ToolCallResults {

	private static final String CALL_KEY_ATTRIBUTE = "spring.ai.alibaba.toolCallKey";

	private ToolCallResults() {
	}

	public static ToolResultPart withCallKey(ToolResultPart result, String key) {
		if (!key.startsWith(ToolCallDecisions.POSITION_KEY_PREFIX)) {
			return result;
		}
		Map<String, String> attributes = new HashMap<>(result.attributes());
		attributes.put(CALL_KEY_ATTRIBUTE, key);
		return result.withAttributes(attributes);
	}


	public static int responseEnd(List<Message> messages, int assistantIndex) {
		int end = assistantIndex + 1;
		while (end < messages.size() && messages.get(end) instanceof ToolResponseMessage) {
			end++;
		}
		return end;
	}

	/**
	 * Matches the results of one assistant message. Explicit position keys and
	 * unique provider IDs take precedence. Older histories may only record the
	 * provider ID and name, so each such result can settle at most one position.
	 */
	public static Set<String> settledKeys(AssistantMessage assistant, List<ToolResultPart> results) {
		List<String> keys = ToolCallDecisions.toolCallKeys(assistant);
		var calls = assistant.getToolCalls();
		Set<String> settled = new HashSet<>();
		List<ToolResultPart> legacy = new ArrayList<>();
		for (ToolResultPart result : results) {
			String key = callKey(result);
			if (key != null && keys.contains(key)) {
				settled.add(key);
			}
			else if (!result.attributes().containsKey(CALL_KEY_ATTRIBUTE)) {
				legacy.add(result);
			}
		}
		for (ToolResultPart result : legacy) {
			for (int i = 0; i < calls.size(); i++) {
				var call = calls.get(i);
				String key = keys.get(i);
				if (key.startsWith(ToolCallDecisions.POSITION_KEY_PREFIX)
						&& Objects.equals(call.id(), result.id())
						&& (result.name() == null || Objects.equals(call.name(), result.name()))
						&& settled.add(key)) {
					break;
				}
			}
		}
		return settled;
	}

	public static List<ToolResultPart> inCallOrder(List<String> keys, List<ToolResultPart> results) {
		Map<String, Integer> positions = new HashMap<>();
		for (int i = 0; i < keys.size(); i++) {
			positions.put(keys.get(i), i);
		}
		return results.stream()
				.sorted(Comparator.comparingInt(result -> positions.getOrDefault(callKey(result), Integer.MAX_VALUE)))
				.toList();
	}

	private static @Nullable String callKey(ToolResultPart result) {
		String position = result.attributes().get(CALL_KEY_ATTRIBUTE);
		if (position != null) {
			return position;
		}
		String id = result.id();
		return id != null && !id.startsWith(ToolCallDecisions.POSITION_KEY_PREFIX) ? id : null;
	}

}
