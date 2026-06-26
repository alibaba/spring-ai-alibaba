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

import com.alibaba.cloud.ai.graph.KeyStrategy;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Strategy that merges two values, typically used for merging maps or mutable objects
 */
public class MergeStrategy implements KeyStrategy {

	@Override
	public Object apply(Object oldValue, Object newValue) {
		if (oldValue instanceof Optional<?> oldValueOptional) {
			oldValue = oldValueOptional.orElse(null);
		}

		if (newValue instanceof Optional<?> newValueOptional) {
			newValue = newValueOptional.orElse(null);
		}

		if (newValue == null) {
			return oldValue;
		}

		if (oldValue == null) {
			return newValue;
		}

		if (oldValue instanceof Map && newValue instanceof Map) {
			Map<Object, Object> mergedMap = new HashMap<>((Map<?, ?>) oldValue);
			mergedMap.putAll((Map<?, ?>) newValue);
			return mergedMap;
		}

		if (oldValue.getClass() != newValue.getClass()) {
			throw new IllegalArgumentException(
					     "Cannot merge incompatible types: " +
					     oldValue.getClass().getName() + " and " + newValue.getClass().getName()
			);
		}

		if (shouldMergeObjectFields(oldValue.getClass())) {
			return mergeObjectFields(oldValue, newValue).orElse(newValue);
		}

		return newValue;
	}

	private Optional<Object> mergeObjectFields(Object oldValue, Object newValue) {
		try {
			Object mergedValue = createInstance(oldValue.getClass());
			for (Field field : mergeableFields(oldValue.getClass())) {
				field.setAccessible(true);
				Object oldFieldValue = field.get(oldValue);
				Object newFieldValue = field.get(newValue);
				field.set(mergedValue, mergeFieldValue(oldFieldValue, newFieldValue));
			}
			return Optional.of(mergedValue);
		}
		catch (ReflectiveOperationException | RuntimeException ex) {
			return Optional.empty();
		}
	}

	private Object mergeFieldValue(Object oldValue, Object newValue) {
		if (newValue == null) {
			return oldValue;
		}

		if (oldValue == null) {
			return newValue;
		}

		if (oldValue instanceof Map && newValue instanceof Map) {
			Map<Object, Object> mergedMap = new HashMap<>((Map<?, ?>) oldValue);
			mergedMap.putAll((Map<?, ?>) newValue);
			return mergedMap;
		}

		if (oldValue.getClass() == newValue.getClass() && shouldMergeObjectFields(oldValue.getClass())) {
			return mergeObjectFields(oldValue, newValue).orElse(newValue);
		}

		return newValue;
	}

	private Object createInstance(Class<?> valueType) throws ReflectiveOperationException {
		Constructor<?> constructor = valueType.getDeclaredConstructor();
		constructor.setAccessible(true);
		return constructor.newInstance();
	}

	private static boolean shouldMergeObjectFields(Class<?> valueType) {
		if (valueType.isPrimitive() || valueType.isEnum() || valueType.isArray()) {
			return false;
		}

		if (Map.class.isAssignableFrom(valueType) || Iterable.class.isAssignableFrom(valueType)) {
			return false;
		}

		Package valuePackage = valueType.getPackage();
		if (valuePackage != null) {
			String packageName = valuePackage.getName();
			if (packageName.startsWith("java.") || packageName.startsWith("javax.")
					|| packageName.startsWith("jakarta.")) {
				return false;
			}
		}

		try {
			valueType.getDeclaredConstructor();
		}
		catch (NoSuchMethodException ex) {
			return false;
		}

		List<Field> fields = mergeableFields(valueType);
		return !fields.isEmpty() && fields.stream().noneMatch(field -> Modifier.isFinal(field.getModifiers()));
	}

	private static List<Field> mergeableFields(Class<?> valueType) {
		List<Field> fields = new ArrayList<>();
		Class<?> currentType = valueType;
		while (currentType != null && currentType != Object.class) {
			for (Field field : currentType.getDeclaredFields()) {
				int modifiers = field.getModifiers();
				if (!field.isSynthetic() && !Modifier.isStatic(modifiers)) {
					fields.add(field);
				}
			}
			currentType = currentType.getSuperclass();
		}
		return fields;
	}

}
