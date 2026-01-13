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
package com.alibaba.cloud.ai.graph.streaming;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.lang.reflect.Method;

/**
 * Streaming output for tool execution results.
 * Extends StreamingOutput to be compatible with NodeExecutor pipeline.
 *
 * <p>This class provides tool-specific streaming output that includes tool identification
 * (toolCallId and toolName) in addition to the standard streaming output properties.</p>
 *
 * <p>Overrides chunk() to include tool identification in serialized output,
 * working with existing StreamingOutputSerializer.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * ToolResult chunk = ToolResult.chunk("Processing...");
 * ToolStreamingOutput<ToolResult> output = new ToolStreamingOutput<>(
 *     chunk,
 *     "agent_tool",
 *     "myAgent",
 *     state,
 *     OutputType.AGENT_TOOL_STREAMING,
 *     "call_123",
 *     "searchTool"
 * );
 * }</pre>
 *
 * @param <T> the type of chunk data (typically ToolResult)
 * @author disaster
 * @since 1.0.0
 */
public class ToolStreamingOutput<T> extends StreamingOutput<T> {

	private final String toolCallId;

	private final String toolName;

	/**
	 * Creates a new ToolStreamingOutput.
	 * @param data the chunk data
	 * @param nodeId the node ID
	 * @param agentName the agent name
	 * @param state the overall state snapshot
	 * @param outputType the output type (STREAMING or FINISHED)
	 * @param toolCallId the tool call ID from the model
	 * @param toolName the name of the tool being executed
	 */
	public ToolStreamingOutput(T data, String nodeId, String agentName, OverAllState state, OutputType outputType,
			String toolCallId, String toolName) {
		super(data, nodeId, agentName, state, outputType);
		this.toolCallId = toolCallId;
		this.toolName = toolName;
	}

	/**
	 * Gets the tool call ID.
	 * @return the tool call ID
	 */
	public String getToolCallId() {
		return toolCallId;
	}

	/**
	 * Gets the tool name.
	 * @return the tool name
	 */
	public String getToolName() {
		return toolName;
	}

	/**
	 * Gets the chunk data (alias for getOriginData for clarity).
	 * @return the chunk data
	 */
	@JsonIgnore
	public T getChunkData() {
		return getOriginData();
	}

	/**
	 * Checks if this is the final chunk in the stream.
	 * @return true if this is the final chunk
	 */
	@JsonIgnore
	public boolean isFinalChunk() {
		return getOutputType() == OutputType.AGENT_TOOL_FINISHED;
	}

	/**
	 * Overrides chunk() to include toolCallId and toolName in serialized output.
	 * This works with existing StreamingOutputSerializer which calls value.chunk().
	 *
	 * <p>The output format is JSON with tool identification:</p>
	 * <pre>{"toolCallId":"...", "toolName":"...", "data":"..."}</pre>
	 *
	 * @return the JSON-formatted chunk string with tool identification
	 */
	@Override
	public String chunk() {
		T data = getOriginData();
		if (data != null) {
			// JSON format with tool identification for client-side parsing
			return String.format("{\"toolCallId\":\"%s\",\"toolName\":\"%s\",\"data\":%s}", escapeJson(toolCallId),
					escapeJson(toolName), formatData(data));
		}
		return null;
	}

	private String formatData(T data) {
		// Handle ToolResult specially to use actual content instead of debug toString()
		// Use reflection to avoid circular dependency with agent-framework module
		String content = tryGetToolResultContent(data);
		if (content != null) {
			if (content.startsWith("{") || content.startsWith("[")) {
				return content;
			}
			return "\"" + escapeJson(content) + "\"";
		}
		String dataStr = data.toString();
		// Check if it looks like JSON (starts with { or [)
		if (dataStr.startsWith("{") || dataStr.startsWith("[")) {
			return dataStr;
		}
		// Otherwise wrap as JSON string
		return "\"" + escapeJson(dataStr) + "\"";
	}

	/**
	 * Tries to get the content from a ToolResult using reflection.
	 * This avoids a compile-time dependency on the agent-framework module.
	 * Uses method detection instead of class name matching for better robustness.
	 * @param data the data object (possibly a ToolResult)
	 * @return the string result if data is a ToolResult, null otherwise
	 */
	private String tryGetToolResultContent(T data) {
		if (data == null) {
			return null;
		}
		try {
			// Detect ToolResult by checking for toStringResult method existence
			// This is more robust than checking class name
			Method method = data.getClass().getMethod("toStringResult");
			return (String) method.invoke(data);
		}
		catch (NoSuchMethodException e) {
			// Not a ToolResult type - this is expected for other data types
			return null;
		}
		catch (Exception e) {
			// If reflection fails for other reasons, fall back to toString()
			return null;
		}
	}

	private static String escapeJson(String s) {
		if (s == null) {
			return "";
		}
		return s.replace("\\", "\\\\")
			.replace("\"", "\\\"")
			.replace("\n", "\\n")
			.replace("\r", "\\r")
			.replace("\t", "\\t");
	}

	@Override
	public String toString() {
		return String.format("ToolStreamingOutput{toolCallId=%s, toolName=%s, node=%s, outputType=%s, isFinal=%s}",
				toolCallId, toolName, node(), getOutputType(), isFinalChunk());
	}

}
