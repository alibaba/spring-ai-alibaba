/*
 * Copyright 2025 the original author or authors.
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
package com.alibaba.cloud.ai.example.manus.context;

import java.util.Objects;

public final class ContextKey<T> {

	private final String name;

	private final Class<T> type;

	/**
	 * Private constructor to enforce factory method usage.
	 * @param name The unique name of the key
	 * @param type The class type of the value
	 */
	private ContextKey(String name, Class<T> type) {
		this.name = Objects.requireNonNull(name, "Key name cannot be null");
		this.type = Objects.requireNonNull(type, "Key type cannot be null");
	}

	/**
	 * Creates a new ContextKey with the specified name and type.
	 * @param <T> The type of value for this key
	 * @param name The unique name of the key
	 * @param type The class representing the value type
	 * @return A new ContextKey instance
	 * @throws NullPointerException if name or type is null
	 */
	public static <T> ContextKey<T> of(String name, Class<T> type) {
		return new ContextKey<>(name, type);
	}

	/**
	 * Creates a new ContextKey with the specified name and raw type. This method is
	 * useful for generic types where compile-time type safety is relaxed.
	 * @param <T> The type of value for this key
	 * @param name The unique name of the key
	 * @param rawType The raw class type
	 * @return A new ContextKey instance
	 * @throws NullPointerException if name or rawType is null
	 */
	@SuppressWarnings("unchecked")
	public static <T> ContextKey<T> ofGeneric(String name, Class<?> rawType) {
		return new ContextKey<>(name, (Class<T>) rawType);
	}

	/**
	 * Gets the name of this key.
	 * @return The key name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Gets the type class of this key.
	 * @return The type class
	 */
	public Class<T> getType() {
		return type;
	}

	/**
	 * Checks if the given value is compatible with this key's type.
	 * @param value The value to check
	 * @return true if the value can be assigned to this key's type, false otherwise
	 */
	public boolean isCompatibleType(Object value) {
		return value == null || type.isInstance(value);
	}

	/**
	 * Safely casts the given value to this key's type.
	 * @param value The value to cast
	 * @return The value cast to type T
	 * @throws ClassCastException if the value is not compatible with the type
	 */
	@SuppressWarnings("unchecked")
	public T cast(Object value) {
		if (value == null) {
			return null;
		}
		if (!type.isInstance(value)) {
			throw new ClassCastException(String.format("Cannot cast value of type %s to %s for key '%s'",
					value.getClass().getSimpleName(), type.getSimpleName(), name));
		}
		return (T) value;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		ContextKey<?> that = (ContextKey<?>) obj;
		return Objects.equals(name, that.name) && Objects.equals(type, that.type);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, type);
	}

	@Override
	public String toString() {
		return String.format("ContextKey{name='%s', type=%s}", name, type.getSimpleName());
	}

}
