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
package com.alibaba.cloud.ai.graph.action;

import com.alibaba.cloud.ai.graph.OverAllState;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents the outcome of a {@link CommandAction} within a LangGraph4j graph. A
 * {@code Command} encapsulates instructions for the graph's next step, including an
 * optional target node to transition to and a map of updates to be applied to the
 * {@link OverAllState}.
 *
 * @param gotoNode containing the name of the next node to execute.
 * @param update A {@link Map} containing key-value pairs representing updates to be
 * merged into the current agent state. An empty map indicates no state updates.
 */
public record Command(String gotoNode, Map<String, Object> update) {

	public Command {
		Objects.requireNonNull(gotoNode, "gotoNode cannot be null");
		Objects.requireNonNull(update, "update cannot be null");
	}

	/**
	 * Constructs a {@code Command} that specifies only the next node to transition to,
	 * with no state updates. If {@code gotoNode} is null, it will be treated as an empty
	 * {@link Optional}.
	 * @param gotoNode The name of the next node to transition to. Can be null.
	 */
	public Command(String gotoNode) {
		this(gotoNode, Map.of());
	}

}
