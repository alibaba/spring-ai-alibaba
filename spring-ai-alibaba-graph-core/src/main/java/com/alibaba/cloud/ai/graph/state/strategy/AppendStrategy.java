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
package com.alibaba.cloud.ai.graph.state.strategy;

import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.serializer.AgentInstructionMessage;
import com.alibaba.cloud.ai.graph.state.AppenderChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.unmodifiableList;

public class AppendStrategy implements KeyStrategy {

	private static final Logger log = LoggerFactory.getLogger(AppendStrategy.class);

	private boolean allowDuplicate = true;

	public AppendStrategy() {
	}

	public AppendStrategy(boolean allowDuplicate) {
		this.allowDuplicate = allowDuplicate;
	}

	@Override
	public Object apply(Object oldValue, Object newValue) {
		if (newValue == null) {
			return oldValue;
		}

		if (oldValue instanceof Optional<?> oldValueOptional) {
			oldValue = oldValueOptional.orElse(null);
		}

		boolean oldValueIsList = oldValue instanceof List<?>;

		if (oldValueIsList && newValue instanceof AppenderChannel.RemoveIdentifier<?>) {
			var result = new ArrayList<>((List<Object>) oldValue);
			removeFromList(result, (AppenderChannel.RemoveIdentifier) newValue);
			return unmodifiableList(result);
		}

		List<Object> list = null;
		if (newValue instanceof List) {
			list = new ArrayList<>((List<?>) newValue);
		}
		else if (newValue.getClass().isArray()) {
			list = Arrays.asList((Object[]) newValue);
		}
		else if (newValue instanceof Collection) {
			list = new ArrayList<>((Collection<?>) newValue);
		}

		if (oldValueIsList) {
			List<Object> oldList = (List<Object>) oldValue;
			if (list != null) {
				if (list.isEmpty()) {
					return oldValue;
				}
				if (oldValueIsList) {
					var result = evaluateRemoval((List<Object>) oldValue, list);
					return mergeValuesWithMessageTypeHandling(result, allowDuplicate);
				}
				oldList.addAll(list);
			}
			else {
				oldList.add(newValue);
			}
			return oldList;
		}
		else {
			ArrayList<Object> arrayResult = new ArrayList<>();
			if (list != null) {
				arrayResult.addAll(list);
			}
			else {
				arrayResult.add(newValue);
			}
			return arrayResult;
		}
	}

	private static void removeFromList(List<Object> result, AppenderChannel.RemoveIdentifier<Object> removeIdentifier) {
		for (int i = 0; i < result.size(); i++) {
			if (removeIdentifier.compareTo(result.get(i), i) == 0) {
				result.remove(i);
				break;
			}
		}
	}

	private static AppenderChannel.RemoveData<Object> evaluateRemoval(List<Object> oldValues, List<?> newValues) {

		final var result = new AppenderChannel.RemoveData<>(oldValues, newValues);

		newValues.stream().filter(value -> value instanceof AppenderChannel.RemoveIdentifier<?>).forEach(value -> {
			result.newValues().remove(value);
			var removeIdentifier = (AppenderChannel.RemoveIdentifier<Object>) value;
			removeFromList(result.oldValues(), removeIdentifier);

		});
		return result;

	}

	/**
	 * Merges old values and new values with special handling based on message types:
	 * <ul>
	 *   <li>AssistantMessage: duplicates are not allowed</li>
	 *   <li>UserMessage or AgentInstructionMessage: duplicates are allowed (unless allowDuplicate is false)</li>
	 *   <li>SystemMessage: duplicates are allowed but an error log is printed</li>
	 *   <li>Other types: duplicates are handled according to allowDuplicate flag</li>
	 * </ul>
	 *
	 * @param result the RemoveData containing old and new values
	 * @param allowDuplicate whether to allow duplicates for non-message types
	 * @return merged list with message type-specific deduplication applied
	 */
	private List<Object> mergeValuesWithMessageTypeHandling(AppenderChannel.RemoveData<Object> result, boolean allowDuplicate) {
		List<Object> merged = new ArrayList<>();
		LinkedHashSet<AssistantMessage> seenAssistantMessages = new LinkedHashSet<>();
		LinkedHashSet<Object> seenOtherTypes = allowDuplicate ? null : new LinkedHashSet<>();

		// Process old values - maintain order
		for (Object value : result.oldValues()) {
			if (value instanceof AssistantMessage assistantMessage) {
				// AssistantMessage: not allowed to duplicate
				if (seenAssistantMessages.add(assistantMessage)) {
					merged.add(value);
				}
			}
			else if (value instanceof SystemMessage || value instanceof UserMessage || value instanceof AgentInstructionMessage) {
				// SystemMessage, UserMessage, AgentInstructionMessage: always allow duplicates
				merged.add(value);
			}
			else {
				// Other types: handle according to allowDuplicate flag
				if (allowDuplicate) {
					merged.add(value);
				}
				else {
					if (seenOtherTypes.add(value)) {
						merged.add(value);
					}
				}
			}
		}

		// Process new values - maintain order
		for (Object value : result.newValues()) {
			if (value instanceof AssistantMessage assistantMessage) {
				// AssistantMessage: not allowed to duplicate
				if (seenAssistantMessages.add(assistantMessage)) {
					merged.add(value);
				}
			}
			else if (value instanceof SystemMessage || value instanceof UserMessage || value instanceof AgentInstructionMessage) {
				// SystemMessage, UserMessage, AgentInstructionMessage: always allow duplicates
				merged.add(value);
			}
			else {
				// Other types: handle according to allowDuplicate flag
				if (allowDuplicate) {
					merged.add(value);
				}
				else {
					if (seenOtherTypes.add(value)) {
						merged.add(value);
					}
				}
			}
		}

		// Check for multiple SystemMessages and log error
		long systemMessageCount = merged.stream()
				.filter(SystemMessage.class::isInstance)
				.count();
		if (systemMessageCount > 1) {
			log.error("Multiple SystemMessage instances detected (count: {}). This may cause unexpected behavior.", systemMessageCount);
		}

		return merged;
	}

}
