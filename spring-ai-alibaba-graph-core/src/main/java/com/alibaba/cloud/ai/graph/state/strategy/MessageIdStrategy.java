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
package com.alibaba.cloud.ai.graph.state.strategy;

import com.alibaba.cloud.ai.graph.state.RemoveMessage;

import org.springframework.ai.chat.messages.Message;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Appends anonymous messages and replaces identified messages in place. Message IDs
 * belong to the conversation history and may be set via message metadata.
 */
public class MessageIdStrategy extends AppendStrategy {

	/** Message metadata key for a stable history ID, independent of tool call IDs. */
	public static final String MESSAGE_ID = "messageId";

	@Override
	public Object apply(Object oldValue, Object newValue) {
		if (newValue == null) {
			return oldValue;
		}

		// Retain support for existing ReplaceAllWith and RemoveByHash updates.
		var combined = (List<?>) super.apply(oldValue == null ? List.of() : oldValue, newValue);
		var messages = new LinkedHashMap<Object, Object>();
		int anonymous = 0;
		for (Object value : combined) {
			if (value instanceof RemoveMessage remove) {
				messages.remove(remove.id());
			}
			else if (value instanceof Message message
					&& message.getMetadata().get(MESSAGE_ID) instanceof String id && !id.isBlank()) {
				messages.put(id, message);
			}
			else {
				messages.put(anonymous++, value);
			}
		}
		return new ArrayList<>(messages.values());
	}

}
