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
package com.alibaba.cloud.ai.graph.agent.hook.unknowntool;

/**
 * Constant keys used by the unknown-tool guard to exchange metadata between the tool
 * node, hook and model interceptor.
 */
public final class UnknownToolGuardConstants {

	/**
	 * Error type value attached to a tool response when the requested tool name cannot
	 * be resolved from the current agent tool registry.
	 */
	public static final String UNKNOWN_TOOL_ERROR_TYPE = "unknown_tool";

	/**
	 * Generic metadata key used by tool responses to expose the concrete error type.
	 */
	public static final String ERROR_TYPE_METADATA_KEY = "errorType";

	/**
	 * Marks that the current {@code ToolResponseMessage} contains at least one unknown-tool
	 * response entry.
	 */
	public static final String UNKNOWN_TOOL_RESPONSE_METADATA_KEY = "unknownToolResponse";

	/**
	 * Stores the tool names originally requested by the model but not found in the
	 * current execution environment.
	 */
	public static final String REQUESTED_TOOL_NAMES_METADATA_KEY = "requestedToolNames";

	/**
	 * Stores the tool names that are actually available to the current agent turn so
	 * the model can correct itself in the next round.
	 */
	public static final String AVAILABLE_TOOL_NAMES_METADATA_KEY = "availableToolNames";

	/**
	 * Counts how many tool-call results inside the current response batch are unknown-tool
	 * failures.
	 */
	public static final String UNKNOWN_TOOL_COUNT_METADATA_KEY = "unknownToolCount";

	/**
	 * Indicates whether every tool call produced in the current assistant turn failed
	 * because the tools were unknown.
	 */
	public static final String ALL_TOOL_CALLS_UNKNOWN_METADATA_KEY = "allToolCallsUnknown";

	/**
	 * Marks the synthetic {@code AgentInstructionMessage} injected by the guard to switch
	 * the agent into final-answer mode.
	 */
	public static final String FINAL_ANSWER_INSTRUCTION_METADATA_KEY = "unknownToolFinalAnswerInstruction";

	private UnknownToolGuardConstants() {
	}

}

