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
import com.alibaba.cloud.ai.graph.state.AppenderChannel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.unmodifiableList;

public class AppendStrategy implements KeyStrategy {

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
					List<Object> mergedList = Stream.concat(result.oldValues().stream(), result.newValues().stream())
						.distinct()
						.collect(Collectors.toList());
					return mergedList;
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
			arrayResult.add(newValue);
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

}
