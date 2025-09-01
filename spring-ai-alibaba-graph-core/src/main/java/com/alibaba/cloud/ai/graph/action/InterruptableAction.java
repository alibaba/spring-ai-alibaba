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

import java.util.Optional;

/**
 * Defines a contract for actions that can interrupt the execution of a graph.
 *
 */
public interface InterruptableAction {

	/**
	 * Determines whether the graph execution should be interrupted at the current node.
	 * @param nodeId The identifier of the current node being processed.
	 * @param state The current state of the agent.
	 * @return An {@link Optional} containing {@link InterruptionMetadata} if the
	 * execution should be interrupted. Returns an empty {@link Optional} to continue
	 * execution.
	 */
	Optional<InterruptionMetadata> interrupt(String nodeId, OverAllState state);

}
