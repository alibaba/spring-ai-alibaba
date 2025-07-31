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
	/**
	 * Internal input class for defining Map output tool input parameters
	 */
	public static class MapOutputInput {

		@com.fasterxml.jackson.annotation.JsonProperty("task_id")
		private String taskId;

		private List<List<Object>> data;

		@com.fasterxml.jackson.annotation.JsonProperty("has_value")
		private boolean hasValue;

		public MapOutputInput() {
		}

		public String getTaskId() {
			return taskId;
		}

		public void setTaskId(String taskId) {
			this.taskId = taskId;
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

	private static final String TOOL_NAME = "map_output_tool";

	private static String getToolDescription() {
		String baseDescription = """
				Map output recording tool for MapReduce workflow.
				接受 Map 阶段处理完成后的内容，自动生成文件名并创建输出文件。
				记录任务状态并管理结构化数据输出。

				**重要参数说明：**
				- task_id: 字符串，任务ID标识符，用于标识当前正在处理的Map任务（必需）
				- has_value: 布尔值，表示是否有有效数据
				  - 如果没有找到任何有效数据，设置为 false
				  - 如果有数据需要输出，设置为 true
				- data: 当 has_value 为 true 时必须提供数据
				""";

		return baseDescription;
	}

	/**
	 * Generate parameters JSON for MapOutputTool with predefined columns format
	 * @return JSON string for parameters schema
	 */
	private static String generateParametersJson() {
		String columnsDesc = "数据行列表";

		return """
				{
				    "type": "object",
				    "properties": {
				        "task_id": {
				            "type": "string",
				            "description": "任务ID标识符，用于标识当前正在处理的Map任务"
				        },
				        "has_value": {
				            "type": "boolean",
				            "description": "是否有有效数据。如果没有找到任何有效数据设置为false，有数据时设置为true"
				        },
				        "data": {
				            "type": "array",
				            "items": {
				                "type": "array",
				                "items": {"type": "string"}
				            },
				            "description": "%s（仅当has_value为true时需要提供）"
				        }
				    },
				    "required": ["task_id", "has_value"],
				    "additionalProperties": false
				}
				""".formatted(columnsDesc);
	}

	private UnifiedDirectoryManager unifiedDirectoryManager;

	// 共享状态管理器，用于管理多个Agent实例间的共享状态
	private MapReduceSharedStateManager sharedStateManager;

	

	// Track if map output recording has completed, allowing termination
	private volatile boolean mapOutputRecorded = false;

	private final ObjectMapper objectMapper;

	// Main constructor
	public MapOutputTool(String planId, ManusProperties manusProperties, MapReduceSharedStateManager sharedStateManager,
			UnifiedDirectoryManager unifiedDirectoryManager, ObjectMapper objectMapper) {
		this.currentPlanId = planId;
		this.unifiedDirectoryManager = unifiedDirectoryManager;
		this.sharedStateManager = sharedStateManager;
		this.objectMapper = objectMapper;
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
		return getToolDescription();
	}

	@Override
	public String getParameters() {
		return generateParametersJson();
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
		String parameters = generateParametersJson();
		String description = getToolDescription();
		OpenAiApi.FunctionTool.Function function = new OpenAiApi.FunctionTool.Function(description, TOOL_NAME,
				parameters);
		return new OpenAiApi.FunctionTool(function);
	}

	/**
	 * Execute Map output recording operation
	 */
	@Override
	public ToolExecuteResult run(MapOutputInput input) {
		log.info("MapOutputTool input: taskId={}, hasValue={}", input.getTaskId(), input.isHasValue());
		try {
			String taskId = input.getTaskId();
			List<List<Object>> data = input.getData();
			boolean hasValue = input.isHasValue();

			// Validate taskId
			if (taskId == null || taskId.trim().isEmpty()) {
				return new ToolExecuteResult("Error: task_id parameter is required");
			}

			

			// Check hasValue logic
			if (hasValue) {
				// When hasValue is true, data must be provided
				if (data == null || data.isEmpty()) {
					return new ToolExecuteResult("Error: data parameter is required when has_value is true");
				}
				// Convert structured data to content string
				String content = formatStructuredData(data);
				return recordMapTaskOutput(content, taskId);
			}
			else {
				// When hasValue is false, create empty output
				return recordMapTaskOutput("", taskId);
			}

		}
		catch (Exception e) {
			log.error("MapOutputTool execution failed", e);
			return new ToolExecuteResult("Tool execution failed: " + e.getMessage());
		}
	}

	/**
	 * Format structured data similar to TerminateTool
	 * @param data the data rows
	 * @return formatted string representation of the structured data
	 */
	private String formatStructuredData(List<List<Object>> data) {
		StringBuilder sb = new StringBuilder();
		sb.append("Data:\n");
		for (List<Object> row : data) {
			sb.append("  ").append(row).append("\n");
		}
		return sb.toString();
	}

	/**
	 * Record Map task output result with completed status by default Task ID is provided
	 * as parameter instead of being obtained from the current execution context
	 */
	private ToolExecuteResult recordMapTaskOutput(String content, String taskId) {
		try {
			// Get timeout configuration for this operation
			Integer timeout = getMapReduceTimeout();
			log.debug("Recording Map task output with timeout: {} seconds", timeout);

			// Ensure planId exists
			if (currentPlanId == null || currentPlanId.trim().isEmpty()) {
				return new ToolExecuteResult("Error: currentPlanId not set, cannot record task status");
			}
			// Validate taskId parameter
			if (taskId == null || taskId.trim().isEmpty()) {
				return new ToolExecuteResult("Error: taskId parameter is required for recording output");
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
			taskStatus.status = "completed";
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

			String result = String.format("Task %s status recorded: %s, output file: %s", taskId, "completed",
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
		// Since terminateColumns is removed, always return null
		return null;
	}

	/**
	 * Get class-level terminate columns configuration as List
	 * @return terminate columns as List<String>
	 */
	public List<String> getTerminateColumnsList() {
		// Since terminateColumns is removed, always return null
		return null;
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
