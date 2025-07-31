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
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

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
 * Reduce operation tool for MapReduce workflow Supports append operation for file
 * manipulation in root plan directory
 */
public class ReduceOperationTool extends AbstractBaseTool<ReduceOperationTool.ReduceOperationInput>
		implements TerminableTool {

	private static final Logger log = LoggerFactory.getLogger(ReduceOperationTool.class);

	// ==================== 配置常量 ====================

	/**
	 * Fixed file name for reduce operations
	 */
	private static final String REDUCE_FILE_NAME = "reduce_output.md";

	/**
	 * Internal input class for defining Reduce operation tool input parameters
	 */
	public static class ReduceOperationInput {

		private List<List<Object>> data;

		@com.fasterxml.jackson.annotation.JsonProperty("has_value")
		private boolean hasValue;

		public ReduceOperationInput() {
		}

		public List<List<Object>> getData() {
			return data;
		}

		public void setData(List<List<Object>> data) {
			this.data = data;
		}

		public boolean isHasValue() {
			return hasValue;
		}

		public void setHasValue(boolean hasValue) {
			this.hasValue = hasValue;
		}

	}

	private static final String TOOL_NAME = "reduce_operation_tool";

	private static String getToolDescription() {
		return """
				Reduce operation tool for MapReduce workflow file manipulation.
				Aggregates and merges data from multiple Map tasks and generates final consolidated output.

				**重要参数说明：**
				- has_value: 布尔值，表示是否有有效数据需要写入
				  - 如果没有找到任何有效数据，设置为 false
				  - 如果有数据需要输出，设置为 true
				- data: 当 has_value 为 true 时必须提供数据

				**IMPORTANT**: 操作完成后工具将自动终止。
				请在单次调用中完成所有内容输出。
				""";
	}

	/**
	 * Generate parameters JSON for ReduceOperationTool with predefined columns format
	 * @return JSON string for parameters schema
	 */
	private static String generateParametersJson() {
		return """
				{
				    "type": "object",
				    "properties": {
				        "has_value": {
				            "type": "boolean",
				            "description": "是否有有效数据需要写入。如果没有找到任何有效数据设置为false，有数据时设置为true"
				        },
				        "data": {
				            "type": "array",
				            "items": {
				                "type": "array",
				                "items": {"type": "string"}
				            },
				            "description": "数据行列表"
				        }
				    },
				    "required": ["has_value"],
				    "additionalProperties": false
				}
				""";
	}

	private UnifiedDirectoryManager unifiedDirectoryManager;

	// 共享状态管理器，用于管理多个Agent实例间的共享状态
	private final MapReduceSharedStateManager sharedStateManager;

	// ==================== TerminableTool 相关字段 ====================

	// 线程安全锁，用于保护append操作和终止状态
	private final ReentrantLock operationLock = new ReentrantLock();

	// 终止状态相关字段
	private volatile boolean isTerminated = false;

	private String lastTerminationMessage = "";

	private String terminationTimestamp = "";

	public ReduceOperationTool(String planId, ManusProperties manusProperties,
			MapReduceSharedStateManager sharedStateManager, UnifiedDirectoryManager unifiedDirectoryManager) {
		this.currentPlanId = planId;
		this.unifiedDirectoryManager = unifiedDirectoryManager;
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
		return getToolDescription();
	}

	@Override
	public String getParameters() {
		return generateParametersJson();
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
		String parameters = generateParametersJson();
		String description = getToolDescription();
		OpenAiApi.FunctionTool.Function function = new OpenAiApi.FunctionTool.Function(description, TOOL_NAME,
				parameters);
		return new OpenAiApi.FunctionTool(function);
	}

	/**
	 * Execute Reduce operation
	 */
	@Override
	public ToolExecuteResult run(ReduceOperationInput input) {
		log.info("ReduceOperationTool input: hasValue={}", input.isHasValue());
		try {
			List<List<Object>> data = input.getData();
			boolean hasValue = input.isHasValue();

			// Check hasValue logic
			if (hasValue) {
				// When hasValue is true, data must be provided
				if (data == null || data.isEmpty()) {
					return new ToolExecuteResult("Error: data parameter is required when has_value is true");
				}

				// Validate data structure
				ToolExecuteResult validationResult = validateDataStructure(data);
				if (validationResult != null) {
					return validationResult; // Return validation error
				}

				// Convert structured data to JSON format and append
				String jsonContent = formatStructuredDataAsJson(data);
				return appendToFile(REDUCE_FILE_NAME, jsonContent);
			}
			else {
				// When hasValue is false, do not append anything but still mark as
				// terminated
				this.isTerminated = true;
				this.lastTerminationMessage = "No data to append, operation completed";
				this.terminationTimestamp = java.time.LocalDateTime.now().toString();
				log.info("Tool marked as terminated (no data to append) for planId: {}", currentPlanId);
				return new ToolExecuteResult("No data to append, operation completed successfully");
			}

		}
		catch (Exception e) {
			log.error("ReduceOperationTool execution failed", e);
			// Mark as terminated even on failure
			this.isTerminated = true;
			this.lastTerminationMessage = "Operation failed: " + e.getMessage();
			this.terminationTimestamp = java.time.LocalDateTime.now().toString();
			return new ToolExecuteResult("Tool execution failed: " + e.getMessage());
		}
	}

	/**
	 * Validate data structure
	 * @param data the input data
	 * @return ToolExecuteResult with error message if validation fails, null if valid
	 */
	private ToolExecuteResult validateDataStructure(List<List<Object>> data) {
		// Perform basic validation to ensure data is not empty
		if (data == null || data.isEmpty()) {
			return new ToolExecuteResult("Error: data parameter is required when has_value is true");
		}
		
		// Validate each row
		for (int i = 0; i < data.size(); i++) {
			List<Object> row = data.get(i);
			if (row == null || row.isEmpty()) {
				String error = String.format("""
						数据结构不一致！
						第%d行数据为空或无效
						""", i + 1);
				return new ToolExecuteResult(error);
			}
		}
		
		return null; // Validation passed
	}

	/**
	 * Format structured data as JSON
	 * @param data the input data
	 * @return formatted JSON string
	 */
	private String formatStructuredDataAsJson(List<List<Object>> data) {
		StringBuilder jsonBuilder = new StringBuilder();
		jsonBuilder.append("{\n");
		jsonBuilder.append("  \"data\": [\n");

		for (int i = 0; i < data.size(); i++) {
			List<Object> row = data.get(i);
			jsonBuilder.append("    [");
			for (int j = 0; j < row.size(); j++) {
				Object value = row.get(j);
				// JSON escape string values
				if (value instanceof String) {
					jsonBuilder.append("\"").append(((String) value).replace("\"", "\\\"")).append("\"");
				}
				else {
					jsonBuilder.append(value);
				}
				if (j < row.size() - 1) {
					jsonBuilder.append(", ");
				}
			}
			jsonBuilder.append("]");
			if (i < data.size() - 1) {
				jsonBuilder.append(",");
			}
			jsonBuilder.append("\n");
		}

		jsonBuilder.append("  ]\n");
		jsonBuilder.append("}\n");

		return jsonBuilder.toString();
	}

	/**
	 * Append content to file in root plan directory Similar to
	 * InnerStorageTool.appendToFile() but operates on root plan directory This method is
	 * thread-safe and will set termination status after execution
	 */
	private ToolExecuteResult appendToFile(String fileName, String content) {
		operationLock.lock();
		try {
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

			String resultStr = result.toString();
			if (sharedStateManager != null) {
				sharedStateManager.setLastOperationResult(currentPlanId, resultStr);
			}

			// 设置终止状态
			this.isTerminated = true;
			this.lastTerminationMessage = "Append operation completed successfully";
			this.terminationTimestamp = java.time.LocalDateTime.now().toString();
			log.info("Tool marked as terminated after append operation for planId: {}", currentPlanId);

			return new ToolExecuteResult(resultStr);

		}
		catch (IOException e) {
			log.error("Failed to append to file", e);
			// 即使失败也设置终止状态
			this.isTerminated = true;
			this.lastTerminationMessage = "Append operation failed: " + e.getMessage();
			this.terminationTimestamp = java.time.LocalDateTime.now().toString();
			return new ToolExecuteResult("Failed to append to file: " + e.getMessage());
		}
		finally {
			operationLock.unlock();
		}
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
	 * Get class-level expected return info configuration
	 * @return expected return info (always null as it's no longer used)
	 */
	public String getTerminateColumns() {
		return null;
	}

	/**
	 * Get class-level expected return info configuration
	 * @return expected return info (always null as it's no longer used)
	 */
	public String getTerminateColumnsList() {
		return null;
	}

	// ==================== TerminableTool interface implementation
	// ====================

	@Override
	public boolean canTerminate() {
		// 检查是否已经执行了append操作，如果执行了则可以终止
		return isTerminated;
	}

	/**
	 * 获取终止状态信息，包含原有状态和终止相关状态
	 */
	@Override
	public String getCurrentToolStateString() {
		StringBuilder sb = new StringBuilder();

		// 原有的共享状态信息
		if (sharedStateManager != null && currentPlanId != null) {
			sb.append(sharedStateManager.getCurrentToolStateString(currentPlanId));
			sb.append("\n\n");
		}

		// 简化的终止状态信息
		sb.append(String.format("ReduceOperationTool: %s", isTerminated ? "🛑 Terminated" : "⚡ Active"));

		return sb.toString();
	}

	/**
	 * 检查工具是否已经终止
	 */
	public boolean isTerminated() {
		return isTerminated;
	}

	/**
	 * 获取最后的终止消息
	 */
	public String getLastTerminationMessage() {
		return lastTerminationMessage;
	}

	/**
	 * 获取终止时间戳
	 */
	public String getTerminationTimestamp() {
		return terminationTimestamp;
	}

}
