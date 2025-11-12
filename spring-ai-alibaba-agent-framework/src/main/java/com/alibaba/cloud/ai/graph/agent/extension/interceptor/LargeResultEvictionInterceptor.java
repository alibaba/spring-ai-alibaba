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
package com.alibaba.cloud.ai.graph.agent.extension.interceptor;

import com.alibaba.cloud.ai.graph.agent.extension.file.FilesystemBackend;
import com.alibaba.cloud.ai.graph.agent.extension.file.LocalFilesystemBackend;
import com.alibaba.cloud.ai.graph.agent.extension.file.WriteResult;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallResponse;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolInterceptor;

import java.util.*;

/**
 * Tool interceptor that automatically evicts large tool results to the filesystem.
 *
 * This interceptor monitors tool call responses and when they exceed a configurable
 * token limit, it automatically saves the full result to a file and returns a truncated
 * message with a pointer to the file location.
 *
 * Key Features (from Python's FilesystemMiddleware.wrap_tool_call):
 * - Automatic detection of large results (>20000 tokens by default)
 * - Eviction to /large_tool_results/ directory
 * - Content sample (first 10 lines) included in response
 * - Exclusion of filesystem tools (they handle their own results)
 * - Tool call ID sanitization for safe file paths
 *
 * Example:
 * <pre>
 * LargeResultEvictionInterceptor interceptor = LargeResultEvictionInterceptor.builder()
 *     .toolTokenLimitBeforeEvict(20000)
 *     .excludeTools(Set.of("ls", "read_file", "write_file"))
 *     .build();
 * </pre>
 */
public class LargeResultEvictionInterceptor extends ToolInterceptor {

	// Constants from Python implementation
	private static final int MAX_LINE_LENGTH = 2000;
	private static final int LINE_NUMBER_WIDTH = 6;
	private static final int DEFAULT_TOOL_TOKEN_LIMIT = 20000;
	private static final int SAMPLE_LINES_COUNT = 10;
	private static final int SAMPLE_LINE_MAX_LENGTH = 1000;
	private static final String LARGE_RESULTS_DIR = System.getProperty("user.dir") + "/large_tool_results/";

	private static final String TOO_LARGE_TOOL_MSG = """
			Tool result too large, the result of this tool call %s was saved in the filesystem at this path: %s
			You can read the result from the filesystem by using the read_file tool, but make sure to only read part of the result at a time.
			You can do this by specifying an offset and limit in the read_file tool call.
			For example, to read the first 100 lines, you can use the read_file tool with offset=0 and limit=100.
			
			Here are the first 10 lines of the result:
			%s
			""";

	private final Integer toolTokenLimitBeforeEvict;
	private final Set<String> excludedTools;
	private final FilesystemBackend backend;

	private LargeResultEvictionInterceptor(Builder builder) {
		this.toolTokenLimitBeforeEvict = builder.toolTokenLimitBeforeEvict;
		this.excludedTools = builder.excludedTools != null
			? new HashSet<>(builder.excludedTools)
			: new HashSet<>();
		this.backend = builder.backend;
	}

	public static Builder builder() {
		return new Builder();
	}

	@Override
	public String getName() {
		return "LargeResultEviction";
	}

	@Override
	public ToolCallResponse interceptToolCall(ToolCallRequest request, ToolCallHandler handler) {
		// Execute the tool call
		ToolCallResponse response = handler.call(request);

		// Check if we should evict the result
		if (!shouldEvictResult(request.getToolName(), response.getResult())) {
			return response;
		}

		// Process large message and return modified response
		return processLargeResult(response, request.getToolCallId());
	}

	/**
	 * Determine if a tool result should be evicted to filesystem.
	 *
	 * @param toolName The name of the tool
	 * @param result The tool result content
	 * @return true if the result should be evicted
	 */
	private boolean shouldEvictResult(String toolName, String result) {
		// Don't evict if feature is disabled
		if (toolTokenLimitBeforeEvict == null) {
			return false;
		}

		// Don't evict excluded tools (e.g., filesystem tools)
		if (excludedTools.contains(toolName)) {
			return false;
		}

		// Don't evict if result is null or empty
		if (result == null || result.isEmpty()) {
			return false;
		}

		// Check if content exceeds token limit (approximation: 4 chars per token)
		return result.length() > 4 * toolTokenLimitBeforeEvict;
	}

	/**
	 * Process large tool result by evicting to filesystem.
	 *
	 * @param response Original tool response
	 * @param toolCallId Tool call ID
	 * @return Modified response with pointer to file
	 */
	private ToolCallResponse processLargeResult(ToolCallResponse response, String toolCallId) {
		String content = response.getResult();

		// Sanitize tool call ID for safe file path
		String sanitizedId = sanitizeToolCallId(toolCallId);
		String filePath = LARGE_RESULTS_DIR + sanitizedId;

		// Write content to filesystem via backend
		if (backend != null) {
			WriteResult writeResult = backend.write(filePath, content);
			if (writeResult.getError() != null) {
				// Log warning but continue with eviction message
				System.err.println("Warning: Failed to write large result to filesystem: " + writeResult.getError());
			}
		} else {
			System.err.println("Warning: Backend not configured, large result not persisted to filesystem");
		}

		// Extract first N lines as sample
		String contentSample = extractContentSample(content);

		// Create modified message with pointer to file
		String evictedMessage = String.format(TOO_LARGE_TOOL_MSG,
			toolCallId, filePath, contentSample);

		// Return modified response
		return ToolCallResponse.builder()
			.content(evictedMessage)
			.toolName(response.getToolName())
			.toolCallId(response.getToolCallId())
			.status("evicted_to_filesystem")
			.build();
	}

	/**
	 * Sanitize tool call ID for use in file paths.
	 * Removes non-alphanumeric characters except underscore and hyphen.
	 * Equivalent to Python's sanitize_tool_call_id.
	 *
	 * @param toolCallId Original tool call ID
	 * @return Sanitized ID safe for file paths
	 */
	private static String sanitizeToolCallId(String toolCallId) {
		if (toolCallId == null) {
			return "unknown";
		}
		return toolCallId.replaceAll("[^a-zA-Z0-9_-]", "_");
	}

	/**
	 * Extract a sample of content (first N lines, truncated).
	 * Equivalent to Python's format_content_with_line_numbers.
	 *
	 * @param content Full content
	 * @return Formatted sample with line numbers
	 */
	private static String extractContentSample(String content) {
		String[] lines = content.split("\n");
		List<String> sampleLines = new ArrayList<>();

		for (int i = 0; i < Math.min(SAMPLE_LINES_COUNT, lines.length); i++) {
			String line = lines[i];
			// Truncate very long lines
			if (line.length() > SAMPLE_LINE_MAX_LENGTH) {
				line = line.substring(0, SAMPLE_LINE_MAX_LENGTH) + "... (truncated)";
			}
			sampleLines.add(line);
		}

		return formatContentWithLineNumbers(sampleLines, 1);
	}

	/**
	 * Format content with line numbers.
	 *
	 * @param lines Lines to format
	 * @param startLine Starting line number (1-based)
	 * @return Formatted content with line numbers
	 */
	private static String formatContentWithLineNumbers(List<String> lines, int startLine) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			// Truncate overly long lines for display
			if (line.length() > MAX_LINE_LENGTH) {
				line = line.substring(0, MAX_LINE_LENGTH) + "... (line truncated)";
			}
			result.append(String.format("%6d\t%s\n", startLine + i, line));
		}
		return result.toString();
	}

	/**
	 * Builder for LargeResultEvictionInterceptor.
	 */
	public static class Builder {
		private Integer toolTokenLimitBeforeEvict = DEFAULT_TOOL_TOKEN_LIMIT;
		private Set<String> excludedTools;
		private FilesystemBackend backend;

		/**
		 * Set token limit before evicting tool results to filesystem.
		 * When a tool result exceeds this limit (in tokens, approximated as chars/4),
		 * it will be automatically saved to /large_tool_results/ and the agent
		 * will receive a pointer to the file instead of the full content.
		 *
		 * Set to null to disable automatic eviction.
		 * Default: 20000 tokens
		 *
		 * Equivalent to Python's tool_token_limit_before_evict parameter.
		 */
		public Builder toolTokenLimitBeforeEvict(Integer toolTokenLimitBeforeEvict) {
			this.toolTokenLimitBeforeEvict = toolTokenLimitBeforeEvict;
			return this;
		}

		/**
		 * Set tools to exclude from eviction.
		 * These tools will never have their results evicted, even if they exceed
		 * the token limit. Useful for filesystem tools that already manage large
		 * content efficiently.
		 *
		 * Default: Empty set (no exclusions)
		 *
		 * Example:
		 * <pre>
		 * builder.excludeTools(Set.of("ls", "read_file", "write_file",
		 *                             "edit_file", "glob", "grep"));
		 * </pre>
		 */
		public Builder excludeTools(Set<String> excludedTools) {
			this.excludedTools = excludedTools;
			return this;
		}

		/**
		 * Add a single tool to the exclusion list.
		 * Convenience method for excluding tools one at a time.
		 *
		 * @param toolName Name of tool to exclude
		 */
		public Builder excludeTool(String toolName) {
			if (this.excludedTools == null) {
				this.excludedTools = new HashSet<>();
			}
			this.excludedTools.add(toolName);
			return this;
		}

		/**
		 * Set custom backend for file storage operations.
		 * The backend is used to write large results to persistent storage.
		 *
		 * NOTE: Backend implementation is optional. If not provided, eviction
		 * will still work by replacing the result text, but the full content
		 * won't be persisted to storage.
		 */
		public Builder backend(FilesystemBackend backend) {
			this.backend = backend;
			return this;
		}

		/**
		 * Convenience method to automatically exclude standard filesystem tools.
		 * Excludes: ls, read_file, write_file, edit_file, glob, grep
		 */
		public Builder excludeFilesystemTools() {
			return excludeTools(Set.of("ls", "read_file", "write_file",
				"edit_file", "glob", "grep"));
		}

		public LargeResultEvictionInterceptor build() {
			// Auto-create default LocalFilesystemBackend if not provided and eviction is enabled
			if (this.backend == null && this.toolTokenLimitBeforeEvict != null) {
				this.backend = new LocalFilesystemBackend(null, false, 10);
			}
			return new LargeResultEvictionInterceptor(this);
		}
	}
}

