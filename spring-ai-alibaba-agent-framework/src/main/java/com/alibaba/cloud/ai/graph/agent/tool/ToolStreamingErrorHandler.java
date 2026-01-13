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
package com.alibaba.cloud.ai.graph.agent.tool;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.ToolStreamingOutput;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;

/**
 * Unified error handler for streaming tool execution.
 * Converts errors to ToolStreamingOutput instead of throwing exceptions.
 *
 * <p><b>Critical Design Decision:</b> Never use {@code Flux.error()} or
 * {@code FluxSink.error()} for tool errors. This would interrupt the entire stream
 * and prevent {@code doneMap} from being sent, breaking the GraphResponse contract.</p>
 *
 * <p>Instead, errors are converted to error results that continue through the stream,
 * allowing proper cleanup and state management.</p>
 *
 * <p>Example usage in stream error handling:</p>
 * <pre>{@code
 * flux.onErrorResume(ex -> Flux.just(
 *     ToolStreamingErrorHandler.handleError(toolCall, ex, state, nodeId, agentName)
 * ));
 * }</pre>
 *
 * @author disaster
 * @since 1.0.0
 */
public final class ToolStreamingErrorHandler {

	private ToolStreamingErrorHandler() {
		// utility class
	}

	/**
	 * Creates an error output for a failed tool execution.
	 * The output is marked as final to indicate stream completion.
	 *
	 * @param toolCall the original tool call
	 * @param error the error that occurred
	 * @param stateSnapshot the state snapshot at time of error
	 * @param nodeId the node ID
	 * @param agentName the agent name
	 * @return a ToolStreamingOutput representing the error
	 */
	public static ToolStreamingOutput<ToolResult> handleError(AssistantMessage.ToolCall toolCall, Throwable error,
			OverAllState stateSnapshot, String nodeId, String agentName) {

		String errorMessage = extractErrorMessage(error);
		ToolResult errorResult = ToolResult.text("Error: " + errorMessage).withFinal(true);

		return new ToolStreamingOutput<>(errorResult, nodeId, agentName, stateSnapshot, OutputType.AGENT_TOOL_FINISHED,
				toolCall.id(), toolCall.name());
	}

	/**
	 * Creates an error output with tool identification only (no ToolCall object).
	 *
	 * @param toolCallId the tool call ID
	 * @param toolName the tool name
	 * @param error the error that occurred
	 * @param stateSnapshot the state snapshot at time of error
	 * @param nodeId the node ID
	 * @param agentName the agent name
	 * @return a ToolStreamingOutput representing the error
	 */
	public static ToolStreamingOutput<ToolResult> handleError(String toolCallId, String toolName, Throwable error,
			OverAllState stateSnapshot, String nodeId, String agentName) {

		String errorMessage = extractErrorMessage(error);
		ToolResult errorResult = ToolResult.text("Error: " + errorMessage).withFinal(true);

		return new ToolStreamingOutput<>(errorResult, nodeId, agentName, stateSnapshot, OutputType.AGENT_TOOL_FINISHED,
				toolCallId, toolName);
	}

	/**
	 * Extracts a user-friendly error message from an exception.
	 * Handles common exception types with specific messages.
	 *
	 * @param error the error to extract message from
	 * @return the error message
	 */
	public static String extractErrorMessage(Throwable error) {
		// Unwrap CompletionException
		if (error instanceof CompletionException ce && ce.getCause() != null) {
			error = ce.getCause();
		}

		if (error instanceof TimeoutException) {
			return "Tool execution timed out";
		}
		// Handle Reactor's block(Duration) timeout which throws IllegalStateException
		if (error instanceof IllegalStateException ise && ise.getMessage() != null
				&& ise.getMessage().contains("Timeout on blocking")) {
			return "Tool execution timed out";
		}
		if (error instanceof ToolCancelledException) {
			return "Tool execution was cancelled";
		}
		if (error instanceof InterruptedException) {
			Thread.currentThread().interrupt();
			return "Tool execution was interrupted";
		}

		String message = error.getMessage();
		return message != null ? message : error.getClass().getSimpleName();
	}

	/**
	 * Checks if an error should be considered a timeout.
	 *
	 * @param error the error to check
	 * @return true if the error represents a timeout
	 */
	public static boolean isTimeout(Throwable error) {
		if (error instanceof CompletionException ce && ce.getCause() != null) {
			error = ce.getCause();
		}
		if (error instanceof TimeoutException) {
			return true;
		}
		// Handle Reactor's block(Duration) timeout
		if (error instanceof IllegalStateException ise && ise.getMessage() != null
				&& ise.getMessage().contains("Timeout on blocking")) {
			return true;
		}
		return false;
	}

	/**
	 * Checks if an error should be considered a cancellation.
	 *
	 * @param error the error to check
	 * @return true if the error represents a cancellation
	 */
	public static boolean isCancellation(Throwable error) {
		if (error instanceof CompletionException ce && ce.getCause() != null) {
			error = ce.getCause();
		}
		return error instanceof ToolCancelledException || error instanceof java.util.concurrent.CancellationException;
	}

}
