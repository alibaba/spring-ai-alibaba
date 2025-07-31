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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Strategy that merges two values, typically used for merging maps or objects
 */
public class MergeStrategy implements KeyStrategy {

	@Override
	public Object apply(Object oldValue, Object newValue) {
		if (newValue == null) {
			return oldValue;
		}

		if (oldValue instanceof Optional<?> oldValueOptional) {
			oldValue = oldValueOptional.orElse(null);
		}

		// If both values are maps, merge them
		if (oldValue instanceof Map && newValue instanceof Map) {
			Map<Object, Object> mergedMap = new HashMap<>((Map<?, ?>) oldValue);
			mergedMap.putAll((Map<?, ?>) newValue);
			return mergedMap;
		}

		// If old value is null, return new value
		if (oldValue == null) {
			return newValue;
		}

		// If new value is a map but old value is not, create a new map with old value as
		// a key
		if (newValue instanceof Map && !(oldValue instanceof Map)) {
			Map<Object, Object> mergedMap = new HashMap<>();
			mergedMap.put("original", oldValue);
			mergedMap.putAll((Map<?, ?>) newValue);
			return mergedMap;
		}

		// If old value is a map but new value is not, add new value to the map
		if (oldValue instanceof Map && !(newValue instanceof Map)) {
			Map<Object, Object> mergedMap = new HashMap<>((Map<?, ?>) oldValue);
			mergedMap.put("additional", newValue);
			return mergedMap;
		}

		// For other cases, return new value (similar to replace strategy)
		return newValue;
	}

}
