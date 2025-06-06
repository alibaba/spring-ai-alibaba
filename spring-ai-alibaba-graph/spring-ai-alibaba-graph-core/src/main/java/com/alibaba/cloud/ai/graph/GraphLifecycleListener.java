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
package com.alibaba.cloud.ai.graph;

import java.util.Map;

/**
 * Interface for listening to graph lifecycle events, allowing callbacks to be triggered
 * at key points during graph execution. Classes implementing this interface can listen
 * for and respond to events such as node start, error occurrence, and node completion
 * within the graph.
 *
 * @author disaster
 * @version 1.0.0
 */
public interface GraphLifecycleListener {

	/**
	 * Callback triggered when a node in the graph starts execution.
	 * @param nodeId The unique identifier of the node.
	 * @param state The current state of the graph at the start of the node execution.
	 */
	default void onStart(String nodeId, Map<String, Object> state) {
	}

	/**
	 * Callback triggered when an error occurs during the execution of a graph node.
	 * @param nodeId The unique identifier of the node where the error occurred.
	 * @param state The state of the graph at the time of the error.
	 * @param ex The exception that was thrown during node execution.
	 */
	default void onError(String nodeId, Map<String, Object> state, Throwable ex) {
	}

	/**
	 * Callback triggered when a node completes its execution successfully.
	 * @param nodeId The unique identifier of the completed node.
	 * @param state The final state of the graph after node completion.
	 */
	default void onComplete(String nodeId, Map<String, Object> state) {
	}

}
