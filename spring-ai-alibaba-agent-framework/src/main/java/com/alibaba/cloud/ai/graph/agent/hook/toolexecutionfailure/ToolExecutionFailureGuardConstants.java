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
package com.alibaba.cloud.ai.graph.agent.hook.toolexecutionfailure;

/**
 * Constant keys used by the tool-execution-failure guard to exchange metadata between
 * the tool node, retry interceptor, hook and final-answer interceptor.
 */
public final class ToolExecutionFailureGuardConstants {

	/**
	 * Generic metadata key used by tool responses to expose the concrete error type.
	 */
	public static final String ERROR_TYPE_METADATA_KEY = "errorType";

	/**
	 * Error type value attached to a tool response when the tool exists but its execution
	 * fails.
	 */
	public static final String TOOL_EXECUTION_FAILURE_ERROR_TYPE = "tool_execution_failure";

	/**
	 * Metadata key storing a normalized failure category such as timeout, cancellation or
	 * runtime exception.
	 */
	public static final String FAILURE_TYPE_METADATA_KEY = "failureType";

	/**
	 * Failure type for a {@code ToolExecutionException} handled by the configured
	 * {@code ToolExecutionExceptionProcessor}.
	 */
	public static final String TOOL_EXECUTION_EXCEPTION_FAILURE_TYPE = "tool_execution_exception";

	/**
	 * Failure type for ordinary runtime failures thrown while executing a tool.
	 */
	public static final String RUNTIME_EXCEPTION_FAILURE_TYPE = "runtime_exception";

	/**
	 * Failure type for tool execution timeout.
	 */
	public static final String TIMEOUT_FAILURE_TYPE = "timeout";

	/**
	 * Failure type for cancellation triggered by the runtime.
	 */
	public static final String CANCELLATION_FAILURE_TYPE = "cancellation";

	/**
	 * Failure type for interrupted execution.
	 */
	public static final String INTERRUPTED_FAILURE_TYPE = "interrupted";

	/**
	 * Failure type used when a tool call completes without producing any response.
	 */
	public static final String MISSING_RESPONSE_FAILURE_TYPE = "missing_response";

	/**
	 * Metadata key recording the retry attempts already consumed by the tool retry
	 * interceptor.
	 */
	public static final String RETRY_ATTEMPTS_METADATA_KEY = "retryAttempts";

	/**
	 * Metadata flag indicating that retry attempts have been exhausted for the current
	 * tool call.
	 */
	public static final String RETRY_EXHAUSTED_METADATA_KEY = "retryExhausted";

	/**
	 * Marks that the current {@code ToolResponseMessage} contains at least one structured
	 * tool-execution failure.
	 */
	public static final String TOOL_EXECUTION_FAILURE_RESPONSE_METADATA_KEY =
			"toolExecutionFailureResponse";

	/**
	 * Stores the names of tools whose execution failed in the current response batch.
	 */
	public static final String FAILED_TOOL_NAMES_METADATA_KEY = "failedToolNames";

	/**
	 * Stores the tool-call identifiers whose execution failed in the current response
	 * batch.
	 */
	public static final String FAILED_TOOL_CALL_IDS_METADATA_KEY = "failedToolCallIds";

	/**
	 * Counts how many tool-call results inside the current response batch are execution
	 * failures.
	 */
	public static final String FAILED_TOOL_COUNT_METADATA_KEY = "failedToolCount";

	/**
	 * Indicates whether every executed tool call in the current assistant turn failed at
	 * execution time.
	 */
	public static final String ALL_TOOL_CALLS_FAILED_METADATA_KEY = "allToolCallsFailed";

	/**
	 * Stores the distinct normalized failure categories observed in the current response
	 * batch.
	 */
	public static final String FAILURE_TYPES_METADATA_KEY = "failureTypes";

	/**
	 * Marks the synthetic {@code AgentInstructionMessage} injected by the guard to switch
	 * the agent into final-answer mode after repeated execution failures.
	 */
	public static final String FINAL_ANSWER_INSTRUCTION_METADATA_KEY =
			"toolExecutionFailureFinalAnswerInstruction";

	private ToolExecutionFailureGuardConstants() {
	}

}


