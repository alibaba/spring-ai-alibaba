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
package com.alibaba.cloud.ai.graph;

import java.util.List;

/**
 * Lifecycle event for invoking a graph node action. A streaming action can
 * produce its result publisher before that publisher completes.
 *
 * @param nodeId the graph node that owns the action
 * @param taskId the identity shared by the attempts of a task
 * @param parentTaskId the enclosing task, or null for a top-level node
 * @param attempt the one-based attempt number
 * @param phase the stage of this action invocation
 * @param error the failure when the phase is FAILED
 * @param agentName the agent or graph that owns the action
 * @param nodePath graph and node IDs from the top-level run to this action
 */
public record TaskAttemptEvent(String nodeId, String taskId, String parentTaskId, int attempt, Phase phase,
		Throwable error, String agentName, List<String> nodePath) {

	public TaskAttemptEvent {
		nodePath = List.copyOf(nodePath);
	}

	public TaskAttemptEvent(String nodeId, String taskId, String parentTaskId, int attempt, Phase phase, Throwable error) {
		this(nodeId, taskId, parentTaskId, attempt, phase, error, "", List.of(nodeId));
	}

	public enum Phase {

		STARTED, RESULT_READY, FAILED, CANCELLED

	}

}
