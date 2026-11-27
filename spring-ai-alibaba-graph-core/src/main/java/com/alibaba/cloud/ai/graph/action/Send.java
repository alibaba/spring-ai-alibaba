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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Schedules a separate invocation of a node with an input overlay visible only to that task.
 *
 * @param node a route or a node ID in the conditional edge mappings
 * @param input values that override the task's copy of the graph state
 */
public record Send(String node, Map<String, Object> input) {

	public Send {
		Objects.requireNonNull(node, "node cannot be null");
		input = Collections.unmodifiableMap(new LinkedHashMap<>(input));
	}

	public Send(String node) {
		this(node, Map.of());
	}

}
