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
package com.alibaba.cloud.ai.graph.checkpoint;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.state.RemoveByHash;
import com.alibaba.cloud.ai.graph.state.RemoveMessage;
import com.alibaba.cloud.ai.graph.state.ReplaceAllWith;
import com.alibaba.cloud.ai.graph.utils.SerializationUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Checkpoint representation of an ordered reducer input, including key deletions. */
public final class CheckpointWrite {

	private static final String OPERATIONS = "operations";

	private CheckpointWrite() {
	}

	public static Map<String, Object> encode(Map<String, Object> values) {
		Map<String, Object> updates = new HashMap<>();
		Map<String, Object> operations = new HashMap<>();
		List<String> removed = new ArrayList<>();
		values.forEach((key, value) -> {
			if (value == OverAllState.MARK_FOR_REMOVAL) {
				removed.add(key);
			}
			else {
				var operation = encodeOperation(value);
				if (operation == null) {
					updates.put(key, value);
				}
				else {
					operations.put(key, operation);
				}
			}
		});
		var data = new HashMap<String, Object>();
		data.put("updates", SerializationUtils.deepCopyMap(updates));
		data.put("removed", removed);
		if (!operations.isEmpty()) {
			data.put(OPERATIONS, SerializationUtils.deepCopyMap(operations));
		}
		return Map.copyOf(data);
	}

	@SuppressWarnings("unchecked")
	public static Map<String, Object> decode(Map<String, Object> data) {
		var updates = SerializationUtils.deepCopyMap((Map<String, Object>) data.get("updates"));
		var removed = (List<String>) data.get("removed");
		removed.forEach(key -> updates.put(key, OverAllState.MARK_FOR_REMOVAL));
		if (data.containsKey(OPERATIONS)) {
			var operations = (Map<String, Object>) data.get(OPERATIONS);
			operations.forEach((key, operation) -> updates.put(key, decodeOperation((Map<String, Object>) operation)));
		}
		return updates;
	}

	private static Map<String, Object> encodeOperation(Object value) {
		if (value instanceof RemoveByHash<?> remove) {
			return operation("removeByHash", remove.value());
		}
		if (value instanceof RemoveMessage remove) {
			return operation("removeMessage", remove.id());
		}
		if (value instanceof ReplaceAllWith<?> replace) {
			return operation("replaceAll", replace.newValues());
		}
		Collection<?> items = value instanceof Object[] array ? Arrays.asList(array)
				: value instanceof Collection<?> collection ? collection : List.of();
		if (items.stream().anyMatch(item -> item instanceof RemoveByHash<?> || item instanceof RemoveMessage)) {
			var encoded = items.stream().map(item -> {
				var command = encodeOperation(item);
				return command == null ? operation("value", item) : command;
			}).toList();
			return operation("sequence", encoded);
		}
		return null;
	}

	private static Map<String, Object> operation(String type, Object value) {
		var command = new HashMap<String, Object>();
		command.put("type", type);
		command.put("value", value);
		return command;
	}

	@SuppressWarnings("unchecked")
	private static Object decodeOperation(Map<String, Object> operation) {
		Object value = operation.get("value");
		return switch ((String) operation.get("type")) {
			case "removeByHash" -> RemoveByHash.of(SerializationUtils.deepCopyValue(value));
			case "removeMessage" -> new RemoveMessage((String) value);
			case "replaceAll" -> ReplaceAllWith.of((List<?>) SerializationUtils.deepCopyValue(value));
			case "value" -> SerializationUtils.deepCopyValue(value);
			case "sequence" -> ((List<Map<String, Object>>) value).stream()
					.map(CheckpointWrite::decodeOperation).toList();
			default -> throw new IllegalStateException("Unsupported checkpoint write operation '" + operation.get("type") + "'");
		};
	}

	@SuppressWarnings("unchecked")
	public static List<Map<String, Object>> decodeAll(Object value) {
		if (value == null) {
			return List.of();
		}
		return ((List<Map<String, Object>>) value).stream().map(CheckpointWrite::decode).toList();
	}

}
