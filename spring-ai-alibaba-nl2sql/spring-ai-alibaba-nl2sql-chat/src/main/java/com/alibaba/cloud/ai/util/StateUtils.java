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

package com.alibaba.cloud.ai.util;

import com.alibaba.cloud.ai.graph.OverAllState;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.function.Supplier;

/**
 * State management utility class, providing type-safe state getting methods
 *
 * @author zhangshenghang
 */
public class StateUtils {

	/**
	 * Safely get string type state value
	 */
	public static String getStringValue(OverAllState state, String key) {
		return state.value(key)
			.map(String.class::cast)
			.orElseThrow(() -> new IllegalStateException("State key not found: " + key));
	}

	/**
	 * Safely get string type state value with default value
	 */
	public static String getStringValue(OverAllState state, String key, String defaultValue) {
		return state.value(key).map(String.class::cast).orElse(defaultValue);
	}

	/**
	 * Safely get list type state value
	 */
	@SuppressWarnings("unchecked")
	public static <T> List<T> getListValue(OverAllState state, String key) {
		return state.value(key)
			.map(v -> (List<T>) v)
			.orElseThrow(() -> new IllegalStateException("State key not found: " + key));
	}

	/**
	 * Safely get object type state value
	 */
	public static <T> T getObjectValue(OverAllState state, String key, Class<T> type) {
		return state.value(key)
			.map(type::cast)
			.orElseThrow(() -> new IllegalStateException("State key not found: " + key));
	}

	/**
	 * Safely get object type state value with default value
	 */
	public static <T> T getObjectValue(OverAllState state, String key, Class<T> type, T defaultValue) {
		return state.value(key).map(type::cast).orElse(defaultValue);
	}

	/**
	 * Safely get object type state value with default value supplier
	 */
	public static <T> T getObjectValue(OverAllState state, String key, Class<T> type, Supplier<T> defaultSupplier) {
		return state.value(key).map(type::cast).orElseGet(defaultSupplier);
	}

	/**
	 * Check if state value exists
	 */
	public static boolean hasValue(OverAllState state, String key) {
		return state.value(key).isPresent() && !((String) state.value(key).get()).equals("");
	}

	/**
	 * Get Document list
	 */
	public static List<Document> getDocumentList(OverAllState state, String key) {
		return getListValue(state, key);
	}

	/**
	 * Get list of Document lists
	 */
	public static List<List<Document>> getDocumentListList(OverAllState state, String key) {
		return getListValue(state, key);
	}

}
