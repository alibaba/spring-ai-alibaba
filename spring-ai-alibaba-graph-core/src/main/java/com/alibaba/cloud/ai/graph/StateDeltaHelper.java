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
package com.alibaba.cloud.ai.graph;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


public final class StateDeltaHelper {

	private StateDeltaHelper() {
	}

	/**
	 * Normalizes update values using {@link DeltaAwareKeyStrategy} if applicable.
	 * @param currentState current state data
	 * @param updateState update state data
	 * @param keyStrategies strategy map
	 * @param forceDelta when {@code true}, always invokes delta strategy for matching keys
	 * @return normalized update map
	 */
	public static Map<String, Object> normalizeWithDeltaStrategies(Map<String, Object> currentState,
			Map<String, Object> updateState, Map<String, KeyStrategy> keyStrategies, boolean forceDelta) {
		if (updateState == null || updateState.isEmpty()) {
			return Map.of();
		}

		Map<String, Object> normalized = new HashMap<>();
		for (Map.Entry<String, Object> entry : updateState.entrySet()) {
			String key = entry.getKey();
			Object newValue = entry.getValue();
			KeyStrategy strategy = keyStrategies != null ? keyStrategies.get(key) : null;
			Object currentValue = currentState != null ? currentState.get(key) : null;

			if (strategy instanceof DeltaAwareKeyStrategy deltaStrategy
					&& (forceDelta || isLikelyMaterializedUpdate(currentValue, newValue))) {
				Object delta = deltaStrategy.computeDelta(currentValue, newValue);
				if (delta != null) {
					normalized.put(key, delta);
				}
			}
			else {
				normalized.put(key, newValue);
			}
		}
		return normalized;
	}

	private static boolean isLikelyMaterializedUpdate(Object currentValue, Object newValue) {
		if (newValue == null) {
			return false;
		}
		if (currentValue instanceof Optional<?> currentOptional) {
			currentValue = currentOptional.orElse(null);
		}

		List<?> currentList = asList(currentValue);
		List<?> newList = asList(newValue);
		if (currentList == null || newList == null || newList.size() < currentList.size()) {
			return false;
		}

		for (int i = 0; i < currentList.size(); i++) {
			if (!java.util.Objects.equals(currentList.get(i), newList.get(i))) {
				return false;
			}
		}
		return true;
	}

	private static List<?> asList(Object value) {
		if (value instanceof List<?> list) {
			return list;
		}
		if (value instanceof Collection<?> collection) {
			return new ArrayList<>(collection);
		}
		if (value != null && value.getClass().isArray()) {
			if (value instanceof Object[] objArray) {
				return Arrays.asList(objArray);
			}
		}
		return null;
	}

}
