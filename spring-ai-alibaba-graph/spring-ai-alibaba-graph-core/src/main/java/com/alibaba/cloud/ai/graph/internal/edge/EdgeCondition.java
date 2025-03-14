/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.graph.internal.edge;

import java.util.Collections;
import java.util.Map;

import com.alibaba.cloud.ai.graph.action.AsyncEdgeAction;
import com.alibaba.cloud.ai.graph.state.AgentState;

import static java.lang.String.format;

/**
 * Represents a condition associated with an edge in a graph.
 * <p>
 * the edge condition.
 */
public class EdgeCondition {
	/**
	 * The action to be performed asynchronously when the edge condition is met.
	 */
	private AsyncEdgeAction action;
	/**
	 * A map of string key-value pairs representing additional mappings for
	 */
	private Map<String, String> mappings;

	/**
	 * Mappings map.
	 *
	 * @return the map
	 */
	public Map<String, String> mappings() {
		return Collections.unmodifiableMap(mappings);
	}

	/**
	 * Action async edge action.
	 *
	 * @return the async edge action
	 */
	public AsyncEdgeAction action() {
		return action;
	}

	/**
	 * Instantiates a new Edge condition.
	 *
	 * @param action   the action
	 * @param mappings the mappings
	 */
	public EdgeCondition(AsyncEdgeAction action, Map<String, String> mappings) {
		this.action = action;
		this.mappings = mappings;
	}

	@Override
	public String toString() {
		return format("EdgeCondition[ %s, mapping=%s", action != null ? "action" : "null", mappings);
	}

}
