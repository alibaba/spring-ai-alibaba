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

import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import com.alibaba.cloud.ai.example.manus.tool.AbstractBaseTool;
import com.alibaba.cloud.ai.example.manus.tool.TerminableTool;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;
import com.alibaba.cloud.ai.example.manus.tool.filesystem.UnifiedDirectoryManager;
import com.alibaba.cloud.ai.example.manus.config.ManusProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.openai.api.OpenAiApi;

/**
 * Map output recording tool for MapReduce workflow Responsible for recording Map stage
 * processing results and task status management
 */
public class MapOutputTool extends AbstractBaseTool<MapOutputTool.MapOutputInput> implements TerminableTool {

	private static final Logger log = LoggerFactory.getLogger(MapOutputTool.class);

	// ==================== 配置常量 ====================

	/**
	 * Task directory name All tasks are stored under this directory
	 */
	private static final String TASKS_DIRECTORY_NAME = "tasks";

	/**
	 * Task status file name Stores task execution status information
	 */
	private static final String TASK_STATUS_FILE_NAME = "status.json";

	/**
	 * Task output file name Stores results after Map stage processing completion
	 */
	private static final String TASK_OUTPUT_FILE_NAME = "output.md";

	/**
	 * Task input file name Stores document fragment content after splitting
	 */
	private static final String TASK_INPUT_FILE_NAME = "input.md";

	/**
	 * Task status: completed
	 */
	private static final String TASK_STATUS_COMPLETED = "completed";

	/**
	 * Task status: failed
	 */
	private static final String TASK_STATUS_FAILED = "failed";

	/**
	 * Internal input class for defining Map output tool input parameters
	 */
	public static class MapOutputInput {

		@com.fasterxml.jackson.annotation.JsonProperty("terminate_columns")
		private List<String> terminateColumns;

		private List<List<Object>> data;

		@com.fasterxml.jackson.annotation.JsonProperty("task_id")
		private String taskId;

		private String status;

		public MapOutputInput() {
		}

		public List<String> getTerminateColumns() {
			return terminateColumns;
		}

		public void setTerminateColumns(List<String> terminateColumns) {
			this.terminateColumns = terminateColumns;
		}

		public List<List<Object>> getData() {
			return data;
		}

		public void setData(List<List<Object>> data) {
			this.data = data;
		}

		public String getTaskId() {
			return taskId;
		}

		public void setTaskId(String taskId) {
			this.taskId = taskId;
		}

		public String getStatus() {
			return status;
		}

		public void setStatus(String status) {
			this.status = status;
		}

	}

	private static final String TOOL_NAME = "map_output_tool";

	private static final String TOOL_DESCRIPTION = """
			Map output recording tool for MapReduce workflow.
			Accepts content after Map stage processing completion, automatically generates filename and creates output file.
			Records task status and manages structured data output.

			Key features:
			- Accept structured Map processing results
			- Automatic output file creation with proper naming
			- Task status tracking and management
			- Support for both completed and failed task statuses
			""";

	/**
	 * Generate parameters JSON with dynamic terminate columns support
	 * @param terminateColumns the columns for structured output
	 * @return JSON string for parameters schema
	 */
	private static String generateParametersJson(List<String> terminateColumns) {
		// If terminateColumns is null or empty, use "content" as default column
		List<String> effectiveColumns = (terminateColumns == null || terminateColumns.isEmpty()) ? List.of("content")
				: terminateColumns;

		// Generate default columns array as JSON string
		StringBuilder defaultColumnsBuilder = new StringBuilder();
		defaultColumnsBuilder.append("[");
		for (int i = 0; i < effectiveColumns.size(); i++) {
			defaultColumnsBuilder.append("\"").append(effectiveColumns.get(i)).append("\"");
			if (i < effectiveColumns.size() - 1) {
				defaultColumnsBuilder.append(", ");
			}
		}
		defaultColumnsBuilder.append("]");

		return """
				{
				    "type": "object",
				    "properties": {
				        "terminate_columns": {
				            "type": "array",
				            "items": {"type": "string"},
				            "description": "Column names for the structured output data",
				            "default": %s
				        },
				        "data": {
				            "type": "array",
				            "items": {
				                "type": "array",
				                "items": {}
				            },
				            "description": "Data rows corresponding to the columns"
				        },
				        "task_id": {
				            "type": "string",
				            "description": "任务ID，用于状态跟踪"
				        },
				        "status": {
				            "type": "string",
				            "enum": ["completed", "failed"],
				            "description": "任务状态"
				        }
				    },
				    "required": ["terminate_columns", "data", "task_id", "status"],
				    "additionalProperties": false
				}
				""".formatted(defaultColumnsBuilder.toString());
	}

	private UnifiedDirectoryManager unifiedDirectoryManager;

	// 共享状态管理器，用于管理多个Agent实例间的共享状态
	private MapReduceSharedStateManager sharedStateManager;

	// Class-level terminate columns configuration - takes precedence over input
	// parameters
	private List<String> terminateColumns;

	// Track if map output recording has completed, allowing termination
	private volatile boolean mapOutputRecorded = false;

	private static final ObjectMapper objectMapper = new ObjectMapper();

	// Main constructor with List<String> terminateColumns
	public MapOutputTool(String planId, ManusProperties manusProperties, MapReduceSharedStateManager sharedStateManager,
			UnifiedDirectoryManager unifiedDirectoryManager, List<String> terminateColumns) {
		this.currentPlanId = planId;
		this.unifiedDirectoryManager = unifiedDirectoryManager;
		this.sharedStateManager = sharedStateManager;
		this.terminateColumns = terminateColumns;
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

	@Override
	public String getDescription() {
		return TOOL_DESCRIPTION;
	}

	@Override
	public String getParameters() {
		return generateParametersJson(terminateColumns);
	}

	@Override
	public Class<MapOutputInput> getInputType() {
		return MapOutputInput.class;
	}

	@Override
	public String getServiceGroup() {
		return "data-processing";
	}

	public static OpenAiApi.FunctionTool getToolDefinition() {
		// Use default terminate columns for static tool definition
		return getToolDefinition(null);
	}

	public static OpenAiApi.FunctionTool getToolDefinition(List<String> terminateColumns) {
		String parameters = generateParametersJson(terminateColumns);
		OpenAiApi.FunctionTool.Function function = new OpenAiApi.FunctionTool.Function(TOOL_DESCRIPTION, TOOL_NAME,
				parameters);
		return new OpenAiApi.FunctionTool(function);
	}

	/**
	 * Execute Map output recording operation
	 */
	@Override
	public ToolExecuteResult run(MapOutputInput input) {
		log.info("MapOutputTool input: taskId={}, status={}", input.getTaskId(), input.getStatus());
		try {
			List<List<Object>> data = input.getData();
			String taskId = input.getTaskId();
			String status = input.getStatus();

			// Use class-level terminateColumns if specified, otherwise use input
			// parameter
			List<String> effectiveTerminateColumns = null;
			if (this.terminateColumns != null && !this.terminateColumns.isEmpty()) {
				// Use class-level terminate columns directly
				effectiveTerminateColumns = new ArrayList<>(this.terminateColumns);
			}
			else if (input.getTerminateColumns() != null && !input.getTerminateColumns().isEmpty()) {
				effectiveTerminateColumns = input.getTerminateColumns();
			}

			if (effectiveTerminateColumns == null || effectiveTerminateColumns.isEmpty()) {
				return new ToolExecuteResult("Error: terminate_columns parameter is required");
			}
			if (data == null || data.isEmpty()) {
				return new ToolExecuteResult("Error: data parameter is required");
			}
			if (taskId == null) {
				return new ToolExecuteResult("Error: task_id parameter is required");
			}
			if (status == null) {
				return new ToolExecuteResult("Error: status parameter is required");
			}

			// Validate status values
			if (!TASK_STATUS_COMPLETED.equals(status) && !TASK_STATUS_FAILED.equals(status)) {
				return new ToolExecuteResult(
						"Error: status must be either '" + TASK_STATUS_COMPLETED + "' or '" + TASK_STATUS_FAILED + "'");
			}

			// Convert structured data to content string
			String content = formatStructuredData(effectiveTerminateColumns, data);
			return recordMapTaskOutput(content, taskId, status);

		}
		catch (Exception e) {
			log.error("MapOutputTool execution failed", e);
			return new ToolExecuteResult("Tool execution failed: " + e.getMessage());
		}
	}

	/**
	 * Format structured data similar to TerminateTool
	 * @param terminateColumns the column names
	 * @param data the data rows
	 * @return formatted string representation of the structured data
	 */
	private String formatStructuredData(List<String> terminateColumns, List<List<Object>> data) {
		StringBuilder sb = new StringBuilder();
		sb.append("Structured Map output data:\n");
		sb.append("Columns: ").append(terminateColumns).append("\n");
		sb.append("Data:\n");
		for (List<Object> row : data) {
			sb.append("  ").append(row).append("\n");
		}
		return sb.toString();
	}

	/**
	 * Record Map task output result and status
	 */
	private ToolExecuteResult recordMapTaskOutput(String content, String taskId, String status) {
		try {
			// Get timeout configuration for this operation
			Integer timeout = getMapReduceTimeout();
			log.debug("Recording Map task output with timeout: {} seconds", timeout);

			// Ensure planId exists
			if (currentPlanId == null || currentPlanId.trim().isEmpty()) {
				return new ToolExecuteResult("Error: currentPlanId not set, cannot record task status");
			}
			// Locate task directory - use hierarchical structure:
			// inner_storage/{rootPlanId}/{currentPlanId}/tasks/{taskId}
			Path rootPlanDir = getPlanDirectory(rootPlanId);
			Path currentPlanDir = rootPlanDir.resolve(currentPlanId);
			Path taskDir = currentPlanDir.resolve(TASKS_DIRECTORY_NAME).resolve(taskId);

			if (!Files.exists(taskDir)) {
				return new ToolExecuteResult("Error: Task directory does not exist: " + taskId);
			}

			// Create output.md file
			Path outputFile = taskDir.resolve(TASK_OUTPUT_FILE_NAME);
			// Write processing content directly without adding extra metadata information
			Files.write(outputFile, content.getBytes());
			String outputFilePath = outputFile.toAbsolutePath().toString();
			// Update task status file
			Path statusFile = taskDir.resolve(TASK_STATUS_FILE_NAME);
			TaskStatus taskStatus;

			if (Files.exists(statusFile)) {
				// Read existing status
				String existingStatusJson = new String(Files.readAllBytes(statusFile));
				taskStatus = objectMapper.readValue(existingStatusJson, TaskStatus.class);
			}
			else {
				// Create new status
				taskStatus = new TaskStatus();
				taskStatus.taskId = taskId;
				taskStatus.inputFile = taskDir.resolve(TASK_INPUT_FILE_NAME).toAbsolutePath().toString();
			}

			// Update status information
			taskStatus.outputFilePath = outputFilePath;
			taskStatus.status = status;
			taskStatus.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

			// Store in shared state manager
			if (sharedStateManager != null) {
				MapReduceSharedStateManager.TaskStatus sharedTaskStatus = new MapReduceSharedStateManager.TaskStatus();
				sharedTaskStatus.taskId = taskStatus.taskId;
				sharedTaskStatus.inputFile = taskStatus.inputFile;
				sharedTaskStatus.outputFilePath = taskStatus.outputFilePath;
				sharedTaskStatus.status = taskStatus.status;
				sharedTaskStatus.timestamp = taskStatus.timestamp;
				sharedStateManager.recordMapTaskStatus(currentPlanId, taskId, sharedTaskStatus);
			}

			// Write updated status file
			String statusJson = objectMapper.writeValueAsString(taskStatus);
			Files.write(statusFile, statusJson.getBytes());

			// Mark that map output has been recorded, allowing termination
			this.mapOutputRecorded = true;

			String result = String.format("Task %s status recorded: %s, output file: %s", taskId, status,
					TASK_OUTPUT_FILE_NAME);

			String resultStr = result.toString();
			if (sharedStateManager != null) {
				sharedStateManager.setLastOperationResult(currentPlanId, resultStr);
			}
			log.info(result);
			return new ToolExecuteResult(result);

		}
		catch (Exception e) {
			String error = "Recording Map task status failed: " + e.getMessage();
			log.error(error, e);
			return new ToolExecuteResult(error);
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
		log.info("MapOutputTool cleanup completed for planId: {}", planId);
	}

	@Override
	public ToolExecuteResult apply(MapOutputInput input, ToolContext toolContext) {
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
	 * Get class-level terminate columns configuration
	 * @return terminate columns as comma-separated string
	 */
	public String getTerminateColumns() {
		if (this.terminateColumns == null || this.terminateColumns.isEmpty()) {
			return null;
		}
		return String.join(",", this.terminateColumns);
	}

	/**
	 * Get class-level terminate columns configuration as List
	 * @return terminate columns as List<String>
	 */
	public List<String> getTerminateColumnsList() {
		return this.terminateColumns == null ? null : new ArrayList<>(this.terminateColumns);
	}

	/**
	 * Task status internal class
	 */
	@SuppressWarnings("unused")
	private static class TaskStatus {

		public String taskId;

		public String inputFile;

		public String outputFilePath;

		public String status;

		public String timestamp;

	}

	/**
	 * Get MapReduce operation timeout configuration
	 * @return Timeout in seconds, returns default value of 300 seconds if not configured
	 */
	private Integer getMapReduceTimeout() {
		// For now, use default timeout until the configuration is added to
		// ManusProperties
		return 300; // Default timeout is 5 minutes
	}

	// ==================== TerminableTool interface implementation
	// ====================

	@Override
	public boolean canTerminate() {
		// MapOutputTool can be terminated only after map output has been recorded
		// This ensures that the tool completes its processing cycle before termination
		return mapOutputRecorded;
	}

}
