/*
 * Copyright 2025 the original author or authors.
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
package com.alibaba.cloud.ai.example.manus.tool.mapreduce;

import java.io.*;
import java.nio.file.*;
import java.nio.file.StandardOpenOption;
import java.util.*;

import com.alibaba.cloud.ai.example.manus.tool.AbstractBaseTool;
import com.alibaba.cloud.ai.example.manus.tool.TerminableTool;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;
import com.alibaba.cloud.ai.example.manus.tool.filesystem.UnifiedDirectoryManager;
import com.alibaba.cloud.ai.example.manus.config.ManusProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.openai.api.OpenAiApi;

/**
 * Reduce operation tool for MapReduce workflow
 * Supports get_lines, append, and replace operations for file manipulation in root plan directory
 */
public class ReduceOperationTool extends AbstractBaseTool<ReduceOperationTool.ReduceOperationInput> implements TerminableTool {

	private static final Logger log = LoggerFactory.getLogger(ReduceOperationTool.class);

	// ==================== 配置常量 ====================

	/**
	 * Supported operation type: get file lines from root plan
	 */
	private static final String ACTION_GET_LINES = "get_lines";

	/**
	 * Supported operation type: append content to file
	 */
	private static final String ACTION_APPEND = "append";

	/**
	 * Supported operation type: replace text in file
	 */
	private static final String ACTION_REPLACE = "replace";

	/**
	 * Maximum lines limit for get_lines operation Single request can retrieve at most
	 * this many lines
	 */
	private static final int MAX_LINES_LIMIT = 500;

	/**
	 * Internal input class for defining Reduce operation tool input parameters
	 */
	public static class ReduceOperationInput {

		private String action;

		@com.fasterxml.jackson.annotation.JsonProperty("file_name")
		private String fileName;

		private String content;

		@com.fasterxml.jackson.annotation.JsonProperty("start_line")
		private Integer startLine;

		@com.fasterxml.jackson.annotation.JsonProperty("end_line")
		private Integer endLine;

		@com.fasterxml.jackson.annotation.JsonProperty("source_text")
		private String sourceText;

		@com.fasterxml.jackson.annotation.JsonProperty("target_text")
		private String targetText;

		public ReduceOperationInput() {
		}

		public String getAction() {
			return action;
		}

		public void setAction(String action) {
			this.action = action;
		}

		public String getFileName() {
			return fileName;
		}

		public void setFileName(String fileName) {
			this.fileName = fileName;
		}

		public String getContent() {
			return content;
		}

		public void setContent(String content) {
			this.content = content;
		}

		public Integer getStartLine() {
			return startLine;
		}

		public void setStartLine(Integer startLine) {
			this.startLine = startLine;
		}

		public Integer getEndLine() {
			return endLine;
		}

		public void setEndLine(Integer endLine) {
			this.endLine = endLine;
		}

		public String getSourceText() {
			return sourceText;
		}

		public void setSourceText(String sourceText) {
			this.sourceText = sourceText;
		}

		public String getTargetText() {
			return targetText;
		}

		public void setTargetText(String targetText) {
			this.targetText = targetText;
		}
	}

	private static final String TOOL_NAME = "reduce_operation_tool";

	private static final String TOOL_DESCRIPTION = """
			Reduce operation tool for MapReduce workflow file manipulation.
			Supports file operations in root plan directory for Reduce stage processing.
			Supported operations:
			- get_lines: Get specified line range content from files in root plan directory (single request max %d lines).
			- append: Append content to file in root plan directory (auto-create file and directory).
			- replace: Replace specific text in file from root plan directory.

			This tool is specifically designed for Reduce stage operations that need to:
			- Read and process Map output results
			- Aggregate, merge, or combine data from multiple Map tasks
			- Generate final consolidated output files
			"""
		.formatted(MAX_LINES_LIMIT);

	private static final String PARAMETERS_JSON = """
			{
			    "oneOf": [
			        {
			            "type": "object",
			            "properties": {
			                "action": {
			                    "type": "string",
			                    "const": "get_lines"
			                },
			                "file_name": {
			                    "type": "string",
			                    "description": "文件名（带扩展名），从根计划目录获取"
			                },
			                "start_line": {
			                    "type": "integer",
			                    "description": "起始行号，默认为1"
			                },
			                "end_line": {
			                    "type": "integer",
			                    "description": "结束行号，默认为文件末尾"
			                }
			            },
			            "required": ["action", "file_name"],
			            "additionalProperties": false
			        },
			        {
			            "type": "object",
			            "properties": {
			                "action": {
			                    "type": "string",
			                    "const": "append"
			                },
			                "file_name": {
			                    "type": "string",
			                    "description": "文件名（带扩展名），在根计划目录中操作"
			                },
			                "content": {
			                    "type": "string",
			                    "description": "要追加的内容"
			                }
			            },
			            "required": ["action", "file_name", "content"],
			            "additionalProperties": false
			        },
			        {
			            "type": "object",
			            "properties": {
			                "action": {
			                    "type": "string",
			                    "const": "replace"
			                },
			                "file_name": {
			                    "type": "string",
			                    "description": "文件名（带扩展名），在根计划目录中操作"
			                },
			                "source_text": {
			                    "type": "string",
			                    "description": "要被替换的文本"
			                },
			                "target_text": {
			                    "type": "string",
			                    "description": "替换后的文本"
			                }
			            },
			            "required": ["action", "file_name", "source_text", "target_text"],
			            "additionalProperties": false
			        }
			    ]
			}
			""";

	private UnifiedDirectoryManager unifiedDirectoryManager;

	private ManusProperties manusProperties;

	// 共享状态管理器，用于管理多个Agent实例间的共享状态
	private MapReduceSharedStateManager sharedStateManager;

	// Track if any operation has completed, allowing termination
	private volatile boolean operationCompleted = false;

	public ReduceOperationTool(String planId, ManusProperties manusProperties, MapReduceSharedStateManager sharedStateManager,
			UnifiedDirectoryManager unifiedDirectoryManager) {
		this.currentPlanId = planId;
		this.manusProperties = manusProperties;
		this.unifiedDirectoryManager = unifiedDirectoryManager;
		this.sharedStateManager = sharedStateManager;
	}

	/**
	 * 设置共享状态管理器
	 */
	public void setSharedStateManager(MapReduceSharedStateManager sharedStateManager) {
		this.sharedStateManager = sharedStateManager;
	}

	@Override
	public String getName() {
		return TOOL_NAME;
	}
    
	/** 
	 * Get task directory list
	 */
	public List<String> getSplitResults() {
		if (sharedStateManager != null && currentPlanId != null) {
			return sharedStateManager.getSplitResults(currentPlanId);
		}
		return new ArrayList<>();
	}

	@Override
	public String getDescription() {
		return TOOL_DESCRIPTION;
	}

	@Override
	public String getParameters() {
		return PARAMETERS_JSON;
	}

	@Override
	public Class<ReduceOperationInput> getInputType() {
		return ReduceOperationInput.class;
	}

	@Override
	public String getServiceGroup() {
		return "data-processing";
	}

	public static OpenAiApi.FunctionTool getToolDefinition() {
		OpenAiApi.FunctionTool.Function function = new OpenAiApi.FunctionTool.Function(TOOL_DESCRIPTION, TOOL_NAME,
				PARAMETERS_JSON);
		return new OpenAiApi.FunctionTool(function);
	}

	/**
	 * Execute Reduce operation
	 */
	@Override
	public ToolExecuteResult run(ReduceOperationInput input) {
		log.info("ReduceOperationTool input: action={}, fileName={}", input.getAction(), input.getFileName());
		try {
			String action = input.getAction();
			if (action == null) {
				return new ToolExecuteResult("Error: action parameter is required");
			}

			ToolExecuteResult result = switch (action) {
				case ACTION_GET_LINES -> {
					String fileName = input.getFileName();
					Integer startLine = input.getStartLine();
					Integer endLine = input.getEndLine();

					if (fileName == null || fileName.trim().isEmpty()) {
						yield new ToolExecuteResult("Error: file_name parameter is required");
					}

					yield getFileLines(fileName, startLine, endLine);
				}
				case ACTION_APPEND -> {
					String fileName = input.getFileName();
					String content = input.getContent();

					if (fileName == null || fileName.trim().isEmpty()) {
						yield new ToolExecuteResult("Error: file_name parameter is required");
					}

					yield appendToFile(fileName, content);
				}
				case ACTION_REPLACE -> {
					String fileName = input.getFileName();
					String sourceText = input.getSourceText();
					String targetText = input.getTargetText();

					if (fileName == null || fileName.trim().isEmpty()) {
						yield new ToolExecuteResult("Error: file_name parameter is required");
					}
					if (sourceText == null || targetText == null) {
						yield new ToolExecuteResult("Error: source_text and target_text parameters are required");
					}

					yield replaceInFile(fileName, sourceText, targetText);
				}
				default -> new ToolExecuteResult("Unknown operation: " + action + ". Supported operations: "
						+ ACTION_GET_LINES + ", " + ACTION_APPEND + ", " + ACTION_REPLACE);
			};

			// Mark operation as completed for termination capability
			this.operationCompleted = true;
			return result;

		}
		catch (Exception e) {
			log.error("ReduceOperationTool execution failed", e);
			return new ToolExecuteResult("Tool execution failed: " + e.getMessage());
		}
	}

	/**
	 * Get specified line range content from files in root plan directory Similar to
	 * InnerStorageTool.getFileLines() but reads from root plan directory instead of
	 * subtask directory
	 */
	private ToolExecuteResult getFileLines(String fileName, Integer startLine, Integer endLine) {
		try {
			if (fileName == null || fileName.trim().isEmpty()) {
				return new ToolExecuteResult("Error: file_name parameter is required");
			}

			// Get file from root plan directory instead of subtask directory
			Path rootPlanDir = getPlanDirectory(rootPlanId);
			Path filePath = rootPlanDir.resolve(fileName);

			if (!Files.exists(filePath)) {
				return new ToolExecuteResult("Error: File does not exist: " + fileName);
			}

			List<String> lines = Files.readAllLines(filePath);

			if (lines.isEmpty()) {
				return new ToolExecuteResult("File is empty");
			}

			// Set default values
			int start = (startLine != null && startLine > 0) ? startLine - 1 : 0;
			int end = (endLine != null && endLine > 0) ? Math.min(endLine, lines.size()) : lines.size();

			// Validate range
			if (start >= lines.size()) {
				return new ToolExecuteResult("Start line number exceeds file range");
			}

			if (start >= end) {
				return new ToolExecuteResult("Start line number cannot be greater than or equal to end line number");
			}

			// Check lines limit
			int requestedLines = end - start;
			if (requestedLines > MAX_LINES_LIMIT) {
				return new ToolExecuteResult(String.format(
						"Requested lines %d exceeds maximum limit %d lines. Please reduce line range or use multiple calls to get content.",
						requestedLines, MAX_LINES_LIMIT));
			}

			StringBuilder result = new StringBuilder();
			result.append(
					String.format("File: %s (lines %d-%d, total %d lines)\n", fileName, start + 1, end, lines.size()));
			result.append("=".repeat(50)).append("\n");

			// Smart content truncation based on context size
			int contextSizeLimit = getInfiniteContextTaskContextSize();
			int currentLength = result.length();
			boolean truncated = false;

			for (int i = start; i < end; i++) {
				String lineContent = String.format("%4d: %s\n", i + 1, lines.get(i));
				if (currentLength + lineContent.length() > contextSizeLimit) {
					truncated = true;
					break;
				}
				result.append(lineContent);
				currentLength += lineContent.length();
			}

			// If content was truncated, add ellipsis to indicate more content exists
			if (truncated) {
				result.append("...\n");
			}

			return new ToolExecuteResult(result.toString());

		}
		catch (IOException e) {
			log.error("Failed to read file lines", e);
			return new ToolExecuteResult("Failed to read file lines: " + e.getMessage());
		}
	}

	/**
	 * Append content to file in root plan directory Similar to
	 * InnerStorageTool.appendToFile() but operates on root plan directory
	 */
	private ToolExecuteResult appendToFile(String fileName, String content) {
		try {
			if (fileName == null || fileName.trim().isEmpty()) {
				return new ToolExecuteResult("Error: file_name parameter is required");
			}
			if (content == null) {
				content = "";
			}

			// Get file from root plan directory
			Path rootPlanDir = getPlanDirectory(rootPlanId);
			ensureDirectoryExists(rootPlanDir);

			// Get file path and append content
			Path filePath = rootPlanDir.resolve(fileName);

			String resultMessage;
			// If file doesn't exist, create new file
			if (!Files.exists(filePath)) {
				Files.writeString(filePath, content);
				log.info("File created and content added: {}", fileName);
				resultMessage = String.format("File created successfully and content added: %s", fileName);
			}
			else {
				// Append content (add newline)
				Files.writeString(filePath, "\n" + content, StandardOpenOption.APPEND);
				log.info("Content appended to file: {}", fileName);
				resultMessage = String.format("Content appended successfully: %s", fileName);
			}

			// Read the file and get last 3 lines with line numbers
			List<String> lines = Files.readAllLines(filePath);
			StringBuilder result = new StringBuilder();
			result.append(resultMessage).append("\n\n");
			result.append("Last 3 lines of file:\n");
			result.append("-".repeat(30)).append("\n");

			int totalLines = lines.size();
			int startLine = Math.max(0, totalLines - 3);

			for (int i = startLine; i < totalLines; i++) {
				result.append(String.format("%4d: %s\n", i + 1, lines.get(i)));
			}

			return new ToolExecuteResult(result.toString());

		}
		catch (IOException e) {
			log.error("Failed to append to file", e);
			return new ToolExecuteResult("Failed to append to file: " + e.getMessage());
		}
	}

	/**
	 * Replace text in file from root plan directory Similar to
	 * InnerStorageTool.replaceInFile() but operates on root plan directory
	 */
	private ToolExecuteResult replaceInFile(String fileName, String sourceText, String targetText) {
		try {
			if (fileName == null || fileName.trim().isEmpty()) {
				return new ToolExecuteResult("Error: file_name parameter is required");
			}
			if (sourceText == null || targetText == null) {
				return new ToolExecuteResult("Error: source_text and target_text parameters are required");
			}

			// Get file from root plan directory
			Path rootPlanDir = getPlanDirectory(rootPlanId);
			Path filePath = rootPlanDir.resolve(fileName);

			if (!Files.exists(filePath)) {
				return new ToolExecuteResult("Error: File does not exist: " + fileName);
			}

			String fileContent = Files.readString(filePath);
			String newContent = fileContent.replace(sourceText, targetText);
			Files.writeString(filePath, newContent);

			log.info("Text replaced in file: {}", fileName);

			// Read the file and get last 3 lines with line numbers
			List<String> lines = Files.readAllLines(filePath);
			StringBuilder result = new StringBuilder();
			result.append(String.format("Text replacement successful: %s", fileName)).append("\n\n");
			result.append("Last 3 lines of file:\n");
			result.append("-".repeat(30)).append("\n");

			int totalLines = lines.size();
			int startLine = Math.max(0, totalLines - 3);

			for (int i = startLine; i < totalLines; i++) {
				result.append(String.format("%4d: %s\n", i + 1, lines.get(i)));
			}

			return new ToolExecuteResult(result.toString());

		}
		catch (IOException e) {
			log.error("Failed to replace text in file", e);
			return new ToolExecuteResult("Failed to replace text in file: " + e.getMessage());
		}
	}

	@Override
	public String getCurrentToolStateString() {
		if (sharedStateManager != null && currentPlanId != null) {
			return sharedStateManager.getCurrentToolStateString(currentPlanId);
		}

		// Fallback solution
		StringBuilder sb = new StringBuilder();
		return sb.toString();
	}

	@Override
	public void cleanup(String planId) {
		// Clean up shared state
		if (sharedStateManager != null && planId != null) {
			sharedStateManager.cleanupPlanState(planId);
		}
		log.info("ReduceOperationTool cleanup completed for planId: {}", planId);
	}

	@Override
	public ToolExecuteResult apply(ReduceOperationInput input, ToolContext toolContext) {
		return run(input);
	}

	/**
	 * Get inner storage root directory path
	 */
	private Path getInnerStorageRoot() {
		return unifiedDirectoryManager.getInnerStorageRoot();
	}

	/**
	 * Get plan directory path
	 */
	private Path getPlanDirectory(String planId) {
		return getInnerStorageRoot().resolve(planId);
	}

	/**
	 * Ensure directory exists
	 */
	private void ensureDirectoryExists(Path directory) throws IOException {
		unifiedDirectoryManager.ensureDirectoryExists(directory);
	}

	/**
	 * Get infinite context task context size
	 * @return Context size for infinite context tasks
	 */
	private Integer getInfiniteContextTaskContextSize() {
		if (manusProperties != null) {
			Integer contextSize = manusProperties.getInfiniteContextTaskContextSize();
			return contextSize != null ? contextSize : 20000; // Default 20000 characters
		}
		return 20000; // Default 20000 characters
	}

	// ==================== TerminableTool interface implementation
	// ====================

	@Override
	public boolean canTerminate() {
		// ReduceOperationTool can be terminated after any operation has completed
		// This allows flexible termination for various Reduce operations
		return operationCompleted;
	}

}
