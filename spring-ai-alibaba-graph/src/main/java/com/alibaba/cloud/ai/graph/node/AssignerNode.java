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
package com.alibaba.cloud.ai.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author yHong
 * @version 1.0
 * @since 2025/7/23
 */
public class AssignerNode implements NodeAction {

	public enum WriteMode {

		OVER_WRITE, APPEND, CLEAR, INPUT_CONSTANT

	}

	/**
	 * description of a single assignment operation
	 */
	public record AssignItem(String targetKey, String inputKey, WriteMode writeMode, Object inputValue) {
		public AssignItem(String targetKey, String inputKey, WriteMode writeMode) {
			this(targetKey, inputKey, writeMode, null);
		}

		public AssignItem(String targetKey, Object inputValue) {
			this(targetKey, null, WriteMode.INPUT_CONSTANT, inputValue);
		}

		public AssignItem(String targetKey) {
			this(targetKey, null, WriteMode.OVER_WRITE);
		}
	}

	private final List<AssignItem> items;

	/**
	 * supports batch assign
	 */
	public AssignerNode(List<AssignItem> items) {
		this.items = items;
	}

	/**
	 * supports single assign
	 */
	public AssignerNode(String targetKey, String inputKey, WriteMode writeMode) {
		this.items = List.of(new AssignItem(targetKey, inputKey, writeMode));
	}

	@Override
	public Map<String, Object> apply(OverAllState state) {
		Map<String, Object> updates = new HashMap<>();
		for (AssignItem item : items) {
			Object value = state.value(item.inputKey()).orElse(null);
			Object targetValue = state.value(item.targetKey()).orElse(null);

			Object result = switch (item.writeMode()) {
				case OVER_WRITE -> value;
				case APPEND -> {
					if (targetValue instanceof List && value != null) {
						List<Object> newList = new ArrayList<>((List<?>) targetValue);
						if (value instanceof Collection<?> col) {
							newList.addAll(col);
						}
						else {
							newList.add(value);
						}
						yield newList;
					}
					else if (value != null) {
						if (value instanceof Collection<?> col) {
							yield new ArrayList<>(col);
						}
						else {
							yield new ArrayList<>(List.of(value));
						}
					}
					else {
						throw new IllegalArgumentException(
								"Cannot append to non-list value for key: " + item.targetKey());
					}
				}
				case CLEAR -> {
					if (targetValue instanceof List) {
						yield new ArrayList<>();
					}
					else if (targetValue instanceof Map) {
						yield new HashMap<>();
					}
					else if (targetValue instanceof String) {
						yield "";
					}
					else if (targetValue instanceof Number) {
						yield 0;
					}
					else {
						yield null;
					}
				}
				case INPUT_CONSTANT -> item.inputValue();
				default -> throw new IllegalArgumentException("Invalid write mode: " + item.writeMode());
			};
			updates.put(item.targetKey, result);
		}
		return updates;
	}

	// Builder pattern
	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private List<AssignItem> items = new ArrayList<>();

		public Builder setItems(List<AssignItem> items) {
			this.items = new ArrayList<>(items);
			return this;
		}

		public Builder addItem(String targetKey, String inputKey, WriteMode writeMode) {
			items.add(new AssignItem(targetKey, inputKey, writeMode));
			return this;
		}

		public Builder addConst(String targetKey, Object inputValue) {
			items.add(new AssignItem(targetKey, inputValue));
			return this;
		}

		public Builder addClear(String targetKey) {
			items.add(new AssignItem(targetKey));
			return this;
		}

		public Builder addItem(AssignItem item) {
			items.add(item);
			return this;
		}

		public AssignerNode build() {
			return new AssignerNode(items);
		}

	}

	public List<AssignItem> getItems() {
		return items;
	}

}
