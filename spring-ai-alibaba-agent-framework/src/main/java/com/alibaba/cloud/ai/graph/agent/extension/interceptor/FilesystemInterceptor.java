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
import com.alibaba.cloud.ai.graph.agent.extension.tools.filesystem.*;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;

import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.tool.ToolCallback;

import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Filesystem interceptor that provides file system management capabilities to agents.
 *
 * This interceptor adds filesystem tools to the agent: ls, read_file, write_file,
 * edit_file, glob, and grep. It enhances the system prompt to guide agents on using
 * these filesystem operations effectively.
 *
 * Key Features:
 * - Pluggable backend system for file storage (local, state-based, composite)
 * - Path validation and security (prevents directory traversal)
 * - Custom tool descriptions support
 * - File metadata tracking (creation/modification timestamps)
 *
 * Note: Large result eviction has been moved to {@link LargeResultEvictionInterceptor}.
 * To enable automatic eviction of large tool results, use both interceptors together.
 *
 * The interceptor automatically:
 * - Injects filesystem tools (ls, read_file, write_file, edit_file, glob, grep)
 * - Provides guidance on proper file path usage (absolute paths required)
 * - Helps agents explore and modify file systems systematically
 *
 * Example:
 * <pre>
 * FilesystemInterceptor interceptor = FilesystemInterceptor.builder()
 *     .basePath("/workspace")
 *     .readOnly(false)
 *     .build();
 * </pre>
 *
 * For automatic large result eviction:
 * <pre>
 * FilesystemInterceptor fsInterceptor = FilesystemInterceptor.builder()
 *     .basePath("/workspace")
 *     .build();
 *
 * LargeResultEvictionInterceptor evictionInterceptor =
 *     LargeResultEvictionInterceptor.builder()
 *         .toolTokenLimitBeforeEvict(20000)
 *         .excludeFilesystemTools()
 *         .build();
 *
 * // Use both interceptors in your agent
 * </pre>
 */
public class FilesystemInterceptor extends ModelInterceptor {

	// Constants
	private static final String EMPTY_CONTENT_WARNING = "System reminder: File exists but has empty contents";
	private static final int DEFAULT_READ_OFFSET = 0;
	private static final int DEFAULT_READ_LIMIT = 500;

	private static final String DEFAULT_SYSTEM_PROMPT = """
			## Filesystem Tools `ls`, `read_file`, `write_file`, `edit_file`, `glob`, `grep`
			
			You have access to a filesystem which you can interact with using these tools.
			All file paths must start with a /.
			Avoid using the root path because you might not have permission to read/write there.
			Try to use the user home directory that is running this process or the current working directory as your base path.
			
			- ls: list files in a directory (requires absolute path)
			- read_file: read a file from the filesystem
			- write_file: write to a file in the filesystem
			- edit_file: edit a file in the filesystem
			- glob: find files matching a pattern (e.g., "**/*.py")
			- grep: search for text within files
			
			## Important Filesystem Usage Notes
			- Always use absolute paths (starting with /)
			- Use `ls` to explore directory structure before reading/editing
			- For large files, use offset and limit parameters in `read_file` to avoid context overflow
			- Use `glob` to find files by pattern matching
			- Use `grep` to search for content across files
			- Always read a file before attempting to edit it
			- Prefer editing existing files over creating new ones when possible
			""";

	private final List<ToolCallback> tools;
	private final String systemPrompt;
	private final String basePath;
	private final boolean readOnly;
	private final Map<String, String> customToolDescriptions;
	// Pattern for directory traversal detection
	private static final Pattern TRAVERSAL_PATTERN = Pattern.compile("\\.\\.|~");

	private FilesystemInterceptor(Builder builder) {
		this.basePath = builder.basePath;
		this.readOnly = builder.readOnly;
		this.systemPrompt = builder.systemPrompt != null ? builder.systemPrompt : DEFAULT_SYSTEM_PROMPT;
		this.customToolDescriptions = builder.customToolDescriptions != null
			? new HashMap<>(builder.customToolDescriptions)
			: new HashMap<>();

		// Create filesystem tools using factory methods with custom or default descriptions
		List<ToolCallback> toolList = new ArrayList<>();
		toolList.add(ListFilesTool.createListFilesToolCallback(
			basePath,
			customToolDescriptions.getOrDefault("ls", ListFilesTool.DESCRIPTION)
		));
		toolList.add(ReadFileTool.createReadFileToolCallback(
			basePath,
			customToolDescriptions.getOrDefault("read_file", ReadFileTool.DESCRIPTION)
		));

		if (!readOnly) {
			toolList.add(WriteFileTool.createWriteFileToolCallback(
				basePath,
				customToolDescriptions.getOrDefault("write_file", WriteFileTool.DESCRIPTION)
			));
			toolList.add(EditFileTool.createEditFileToolCallback(
				basePath,
				customToolDescriptions.getOrDefault("edit_file", EditFileTool.DESCRIPTION)
			));
		}

		toolList.add(GlobTool.createGlobToolCallback(
			basePath,
			customToolDescriptions.getOrDefault("glob", GlobTool.DESCRIPTION)
		));
		toolList.add(GrepTool.createGrepToolCallback(
			basePath,
			customToolDescriptions.getOrDefault("grep", GrepTool.DESCRIPTION)
		));

		this.tools = Collections.unmodifiableList(toolList);
	}

	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Validate and normalize file path for security.
	 * Prevents directory traversal attacks by checking for ".." and "~".
	 *
	 * @param path The path to validate
	 * @param allowedPrefixes Optional list of allowed path prefixes
	 * @return Normalized canonical path
	 * @throws IllegalArgumentException if path is invalid
	 */
	public static String validatePath(String path, List<String> allowedPrefixes) {
		if (TRAVERSAL_PATTERN.matcher(path).find()) {
			throw new IllegalArgumentException("Path traversal not allowed: " + path);
		}

		// Normalize path
		String normalized = path.replace("\\", "/");
		normalized = Paths.get(normalized).normalize().toString().replace("\\", "/");

		if (!normalized.startsWith("/")) {
			normalized = "/" + normalized;
		}

		// Check allowed prefixes if specified
		if (allowedPrefixes != null && !allowedPrefixes.isEmpty()) {
			boolean hasValidPrefix = false;
			for (String prefix : allowedPrefixes) {
				if (normalized.startsWith(prefix)) {
					hasValidPrefix = true;
					break;
				}
			}
			if (!hasValidPrefix) {
				throw new IllegalArgumentException(
					"Path must start with one of " + allowedPrefixes + ": " + path
				);
			}
		}

		return normalized;
	}

	@Override
	public List<ToolCallback> getTools() {
		return tools;
	}

	@Override
	public String getName() {
		return "Filesystem";
	}

	@Override
	public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
		SystemMessage enhancedSystemMessage;

		if (request.getSystemMessage() == null) {
			enhancedSystemMessage = new SystemMessage(this.systemPrompt);
		} else {
			enhancedSystemMessage = new SystemMessage(request.getSystemMessage().getText() + "\n\n" + systemPrompt);
		}

		// Create enhanced request
		ModelRequest enhancedRequest = ModelRequest.builder(request)
				.systemMessage(enhancedSystemMessage)
				.build();

		// Call the handler with enhanced request
		return handler.call(enhancedRequest);
	}

	/**
	 * Builder for FilesystemInterceptor with comprehensive configuration options.
	 *
	 * Note: Token limit and large result eviction features have been moved to
	 * {@link LargeResultEvictionInterceptor}. Use both interceptors together if needed.
	 */
	public static class Builder {
		private String systemPrompt;
		private String basePath = "/";
		private boolean readOnly = false;
		private Map<String, String> customToolDescriptions;
		private FilesystemBackend backend;

		/**
		 * Set custom system prompt to guide filesystem usage.
		 * Set to null to disable system prompt injection.
		 */
		public Builder systemPrompt(String systemPrompt) {
			this.systemPrompt = systemPrompt;
			return this;
		}

		/**
		 * Set the base path for filesystem operations.
		 * All file operations will be relative to this path.
		 * Default: "/"
		 */
		public Builder basePath(String basePath) {
			this.basePath = basePath;
			return this;
		}

		/**
		 * Set whether the filesystem should be read-only.
		 * When true, write_file and edit_file tools are not provided.
		 * Default: false
		 */
		public Builder readOnly(boolean readOnly) {
			this.readOnly = readOnly;
			return this;
		}

		/**
		 * Set custom tool descriptions to override defaults.
		 * Map keys should be tool names: "ls", "read_file", "write_file",
		 * "edit_file", "glob", "grep".
		 *
		 * Example:
		 * <pre>
		 * Map&lt;String, String&gt; customDescs = Map.of(
		 *     "read_file", "Custom read file description",
		 *     "write_file", "Custom write file description"
		 * );
		 * builder.customToolDescriptions(customDescs);
		 * </pre>
		 *
		 */
		public Builder customToolDescriptions(Map<String, String> customToolDescriptions) {
			this.customToolDescriptions = customToolDescriptions;
			return this;
		}

		/**
		 * Add a single custom tool description.
		 * Convenience method for adding one description at a time.
		 *
		 * @param toolName Name of the tool ("ls", "read_file", etc.)
		 * @param description Custom description for the tool
		 */
		public Builder addCustomToolDescription(String toolName, String description) {
			if (this.customToolDescriptions == null) {
				this.customToolDescriptions = new HashMap<>();
			}
			this.customToolDescriptions.put(toolName, description);
			return this;
		}

		public FilesystemInterceptor build() {
			return new FilesystemInterceptor(this);
		}
	}

}

