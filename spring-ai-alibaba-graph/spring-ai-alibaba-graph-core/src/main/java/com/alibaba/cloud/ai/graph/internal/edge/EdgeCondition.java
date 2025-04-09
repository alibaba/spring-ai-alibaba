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
package com.alibaba.cloud.ai.graph.internal.edge;

import com.alibaba.cloud.ai.graph.action.AsyncEdgeAction;

import java.util.Map;

import static java.lang.String.format;

/**
 * Represents a condition associated with an edge in a graph.
 *
 * @param action The action to be performed asynchronously when the edge condition is met.
 * @param mappings A map of string key-value pairs representing additional mappings for
 * the edge condition.
 */
public record EdgeCondition(AsyncEdgeAction action, Map<String, String> mappings) {

	@Override
	public String toString() {
		return format("EdgeCondition[ %s, mapping=%s", action != null ? "action" : "null", mappings);
	}

}
