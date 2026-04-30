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
package com.alibaba.cloud.ai.graph.agent.hook;

/**
 * Shared metadata key constants used across multiple tool-call guard implementations.
 *
 * <p>This class holds keys that act as cross-cutting infrastructure between the tool
 * node and the individual guard hooks. Guard-specific keys remain in their own
 * constants classes (e.g. {@code UnknownToolGuardConstants},
 * {@code ToolExecutionFailureGuardConstants}).</p>
 */
public final class ToolCallGuardConstants {

	/**
	 * Generic metadata key used by tool responses to expose the concrete error type.
	 * The value stored under this key is a discriminator string such as
	 * {@code "unknown_tool"} or {@code "tool_execution_failure"} that lets guard hooks
	 * and interceptors identify which kind of failure occurred without inspecting the
	 * human-readable error message.
	 */
	public static final String ERROR_TYPE_METADATA_KEY = "errorType";

	/**
	 * Indicates whether every tool call in the current assistant turn resulted in an
	 * error of any kind (unknown tool, execution failure, or a mix of both).
	 * <p>This flag is set by {@code AgentToolNode} so that guard hooks can detect
	 * mixed-failure scenarios where neither {@code allToolCallsUnknown} nor
	 * {@code allToolCallsFailed} is {@code true} on its own.</p>
	 */
	public static final String ALL_TOOL_CALLS_ERRORED_METADATA_KEY = "allToolCallsErrored";

	private ToolCallGuardConstants() {
	}

}
