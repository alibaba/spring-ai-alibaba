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

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;

import java.util.Map;
import java.util.Optional;

/**
 * Defines a contract for actions that can interrupt the execution of a graph.
 * <p>
 * This interface provides two hook points for interruption:
 * <ul>
 *   <li>{@link #interrupt(String, OverAllState, RunnableConfig)} - Called BEFORE the node action's apply() method</li>
 *   <li>{@link #interruptAfter(String, OverAllState, Map, RunnableConfig)} - Called AFTER the node action's apply() method</li>
 * </ul>
 * <p>
 * The execution flow is: interrupt() -> apply() -> interruptAfter()
 */
public interface InterruptableAction {

	/**
	 * Determines whether the graph execution should be interrupted BEFORE the current node executes.
	 * <p>
	 * This method is called before the node action's {@code apply()} method is invoked.
	 * @param nodeId The identifier of the current node being processed.
	 * @param state The current state of the agent.
	 * @param config The runnable configuration.
	 * @return An {@link Optional} containing {@link InterruptionMetadata} if the
	 * execution should be interrupted. Returns an empty {@link Optional} to continue
	 * execution.
	 */
	Optional<InterruptionMetadata> interrupt(String nodeId, OverAllState state, RunnableConfig config);

	/**
	 * Determines whether the graph execution should be interrupted AFTER the current node executes.
	 * <p>
	 * This method is called after the node action's {@code apply()} method has completed,
	 * but before the action result is merged into the state. This allows inspection of the
	 * action result to decide whether to interrupt.
	 * <p>
	 * If this method returns an {@link InterruptionMetadata}, the action result will be
	 * merged into the state and a checkpoint will be created before returning the interruption.
	 * @param nodeId The identifier of the current node being processed.
	 * @param state The current state of the agent (before merging action result).
	 * @param actionResult The result returned by the node action's {@code apply()} method.
	 * @param config The runnable configuration.
	 * @return An {@link Optional} containing {@link InterruptionMetadata} if the
	 * execution should be interrupted. Returns an empty {@link Optional} to continue
	 * execution. Default implementation returns empty (no interruption).
	 */
	default Optional<InterruptionMetadata> interruptAfter(String nodeId, OverAllState state,
			Map<String, Object> actionResult, RunnableConfig config) {
		return Optional.empty();
	}

}
