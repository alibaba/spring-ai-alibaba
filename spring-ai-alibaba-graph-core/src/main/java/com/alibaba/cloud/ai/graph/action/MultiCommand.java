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
package com.alibaba.cloud.ai.graph.action;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a command that can route to multiple nodes for parallel execution.
 * This is used when a conditional edge action returns multiple target nodes.
 *
 * @param gotoNodes A list of node identifiers to execute in parallel
 * @param update A map containing key-value pairs representing updates to be merged into the current state
 */
public record MultiCommand(List<String> gotoNodes, Map<String, Object> update) {

	public MultiCommand {
		Objects.requireNonNull(gotoNodes, "gotoNodes cannot be null");
		Objects.requireNonNull(update, "update cannot be null");
		if (gotoNodes.isEmpty()) {
			throw new IllegalArgumentException("gotoNodes cannot be empty");
		}
	}

	/**
	 * Constructs a MultiCommand that specifies only the next nodes to transition to,
	 * with no state updates.
	 * @param gotoNodes The list of nodes to transition to. Cannot be empty.
	 */
	public MultiCommand(List<String> gotoNodes) {
		this(gotoNodes, Map.of());
	}

	/**
	 * Checks if this MultiCommand represents a single node (for backward compatibility).
	 * @return true if there's only one node, false otherwise
	 */
	public boolean isSingleNode() {
		return gotoNodes.size() == 1;
	}

	/**
	 * Converts a single-node MultiCommand to a regular Command.
	 * @return a Command instance if this is a single node, otherwise throws an exception
	 * @throws IllegalStateException if this MultiCommand contains multiple nodes
	 */
	public Command toCommand() {
		if (!isSingleNode()) {
			throw new IllegalStateException("Cannot convert MultiCommand with multiple nodes to Command");
		}
		return new Command(gotoNodes.get(0), update);
	}
}

