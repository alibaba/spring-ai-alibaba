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
package com.alibaba.cloud.ai.graph.internal.edge;

import com.alibaba.cloud.ai.graph.action.AsyncCommandAction;
import com.alibaba.cloud.ai.graph.action.AsyncMultiCommandAction;

import java.util.Map;

import static java.lang.String.format;

/**
 * Represents a condition associated with an edge in a graph.
 * Supports both single-node routing (AsyncCommandAction) and multi-node parallel routing (AsyncMultiCommandAction).
 *
 * @param action The action to be performed asynchronously when the edge condition is met.
 *               Can be either AsyncCommandAction (single node) or AsyncMultiCommandAction (multiple nodes).
 * @param mappings A map of string key-value pairs representing additional mappings for
 * the edge condition.
 */
public record EdgeCondition(Object action, Map<String, String> mappings) {

	public EdgeCondition {
		if (action != null && !(action instanceof AsyncCommandAction) && !(action instanceof AsyncMultiCommandAction)) {
			throw new IllegalArgumentException("Action must be either AsyncCommandAction or AsyncMultiCommandAction");
		}
	}

	/**
	 * Creates an EdgeCondition with AsyncCommandAction (single node routing).
	 */
	public static EdgeCondition single(AsyncCommandAction action, Map<String, String> mappings) {
		return new EdgeCondition(action, mappings);
	}

	/**
	 * Creates an EdgeCondition with AsyncMultiCommandAction (multi-node parallel routing).
	 */
	public static EdgeCondition multi(AsyncMultiCommandAction action, Map<String, String> mappings) {
		return new EdgeCondition(action, mappings);
	}

	/**
	 * Checks if this condition supports multi-node routing.
	 * @return true if the action is AsyncMultiCommandAction, false otherwise
	 */
	public boolean isMultiCommand() {
		return action instanceof AsyncMultiCommandAction;
	}

	/**
	 * Gets the action as AsyncCommandAction if it's a single-node action.
	 * @return the AsyncCommandAction, or null if it's a multi-node action
	 */
	public AsyncCommandAction singleAction() {
		return action instanceof AsyncCommandAction ? (AsyncCommandAction) action : null;
	}

	/**
	 * Gets the action as AsyncMultiCommandAction if it's a multi-node action.
	 * @return the AsyncMultiCommandAction, or null if it's a single-node action
	 */
	public AsyncMultiCommandAction multiAction() {
		return action instanceof AsyncMultiCommandAction ? (AsyncMultiCommandAction) action : null;
	}

	@Override
	public String toString() {
		return format("EdgeCondition[ %s, mapping=%s", action != null ? "action" : "null", mappings);
	}

}
