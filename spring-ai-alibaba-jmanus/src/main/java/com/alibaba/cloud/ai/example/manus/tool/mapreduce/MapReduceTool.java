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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import com.alibaba.cloud.ai.example.manus.tool.ToolCallBiFunctionDef;
import com.alibaba.cloud.ai.example.manus.tool.TerminableTool;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;
import com.alibaba.cloud.ai.example.manus.tool.code.CodeUtils;
import com.alibaba.cloud.ai.example.manus.config.ManusProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.openai.api.OpenAiApi;

/**
 * Data split tool for MapReduce workflow data preparation phase
 * Responsible for validating file existence, identifying table header information and performing data split processing
 * 
 * Supports class-level terminate columns configuration that takes precedence over input parameters.
 * When class-level terminateColumns is specified, input parameter terminate_columns will be ignored.
 */
public class MapReduceTool implements ToolCallBiFunctionDef<MapReduceTool.MapReduceInput>, TerminableTool {

	private static final Logger log = LoggerFactory.getLogger(MapReduceTool.class);

	// ==================== 配置常量 ====================

	/**
	 * Supported operation type: data splitting
	 */
	private static final String ACTION_SPLIT_DATA = "split_data";

	/**
	 * Supported operation type: record Map output
	 */
	private static final String ACTION_RECORD_MAP_OUTPUT = "record_map_output";

	/**
	 * Default plan ID prefix
	 * When planId is empty, use this prefix + timestamp to generate default ID
	 */
	private static final String DEFAULT_PLAN_ID_PREFIX = "plan-";

	/**
	 * Task directory name
	 * All tasks are stored under this directory
	 */
	private static final String TASKS_DIRECTORY_NAME = "tasks";

	/**
	 * Task ID format template
	 * Used to generate incremental task IDs like task_001, task_002
	 */
	private static final String TASK_ID_FORMAT = "task_%03d";

	/**
	 * Task input file name
	 * Stores document fragment content after splitting
	 */
	private static final String TASK_INPUT_FILE_NAME = "input.md";

	/**
	 * Task status file name
	 * Stores task execution status information
	 */
	private static final String TASK_STATUS_FILE_NAME = "status.json";

	/**
	 * Task output file name
	 * Stores results after Map stage processing completion
	 */
	private static final String TASK_OUTPUT_FILE_NAME = "output.md";

	/**
	 * Default file split size (character count)
	 * Number of file characters each task processes, adjustable based on actual needs
	 */
	private static final int DEFAULT_SPLIT_SIZE = 5000;

	/**
	 * Task status: pending
	 */
	private static final String TASK_STATUS_PENDING = "pending";

	/**
	 * Task status: completed
	 */
	private static final String TASK_STATUS_COMPLETED = "completed";

	/**
	 * Task status: failed
	 */
	private static final String TASK_STATUS_FAILED = "failed";

	/**
	 * Internal input class for defining MapReduce tool input parameters
	 */
	public static class MapReduceInput {

		private String action;

		@com.fasterxml.jackson.annotation.JsonProperty("file_path")
		private String filePath;

		@com.fasterxml.jackson.annotation.JsonProperty("terminate_columns")
		private List<String> terminateColumns;

		private String content;

		private List<List<Object>> data;

		@com.fasterxml.jackson.annotation.JsonProperty("task_id")
		private String taskId;

		private String status;

		public MapReduceInput() {
		}

		public String getAction() {
			return action;
		}

		public void setAction(String action) {
			this.action = action;
		}

		public String getFilePath() {
			return filePath;
		}

		public void setFilePath(String filePath) {
			this.filePath = filePath;
		}

		public List<String> getTerminateColumns() {
			return terminateColumns;
		}

		public void setTerminateColumns(List<String> terminateColumns) {
			this.terminateColumns = terminateColumns;
		}

		public String getContent() {
			return content;
		}

		public void setContent(String content) {
			this.content = content;
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

		public List<List<Object>> getData() {
			return data;
		}

		public void setData(List<List<Object>> data) {
			this.data = data;
		}

	}

	private static final String TOOL_NAME = "map_reduce_tool";

	private static final String TOOL_DESCRIPTION = """
			Data split tool for MapReduce workflow data preparation stage and task status management.
			Supported operations:
			- split_data: Automatically complete file existence validation and perform data split processing, supports CSV, TSV, TXT and other text format data files.
			  Output directory uses inner_storage/{planId}/tasks/task_{taskId} pattern, unified management with InnerStorageService,
			  each task has independent directory containing input.md (split document fragments) file.
			- record_map_output: Accepts content after Map stage processing completion, automatically generates filename and creates output file, records task status.
			  Tool automatically creates standardized file structure under inner_storage/{planId}/tasks/task_{taskId} directory:
			  input.md (document fragments), status.json (task status), output.md (model processing results),
			  each task has independent directory and unified file naming.

			Task management features:
			- Automatically generate incremental task IDs
			- Create independent directory for each task: inner_storage/{planId}/tasks/task_{taskId}
			- Standardized directory structure: each task directory contains input.md (document fragments), status.json (status file), output.md (model output)
			- Unified file naming for easy batch processing and management
			- Unified storage management with InnerStorageService
			- Get split file list for task assignment through getSplitResults() method
			- Support automatic file creation and status record management after Map stage completion
			""";

	/**
	 * Generate parameters JSON with dynamic terminate columns support like TerminateTool
	 * @param terminateColumns the columns for structured output
	 * @return JSON string for parameters schema
	 */
	private static String generateParametersJson(List<String> terminateColumns) {
		// If terminateColumns is null or empty, use "content" as default column
		List<String> effectiveColumns = (terminateColumns == null || terminateColumns.isEmpty()) ? 
			List.of("content") : terminateColumns;
		
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
			    "oneOf": [
			        {
			            "type": "object",
			            "properties": {
			                "action": {
			                    "type": "string",
			                    "const": "split_data"
			                },
			                "file_path": {
			                    "type": "string",
			                    "description": "要处理的文件或文件夹路径"
			                },
			                "terminate_columns": {
			                    "type": "array",
			                    "items": {
			                        "type": "string"
			                    },
			                    "description": "终止结果的列名，用于结构化输出"
			                }
			            },
			            "required": ["action", "file_path"],
			            "additionalProperties": false
			        },
			        {
			           "type": "object",
			           "properties": {
			               "action": {
			                   "type": "string",
			                   "const": "record_map_output"
			               },
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
			           "required": ["action", "terminate_columns", "data", "task_id", "status"],
			           "additionalProperties": false
			       }
			    ]
			}
			""".formatted(defaultColumnsBuilder.toString());
	}

	private String planId;

	private String workingDirectoryPath;

	private ManusProperties manusProperties;

	// 共享状态管理器，用于管理多个Agent实例间的共享状态
	private MapReduceSharedStateManager sharedStateManager;

	// Class-level terminate columns configuration - takes precedence over input parameters
	private List<String> terminateColumns;

	// Track if map output recording has completed, allowing termination
	private volatile boolean mapOutputRecorded = false;

	private static final ObjectMapper objectMapper = new ObjectMapper();

	// Backward compatibility constructor without terminateColumns
	public MapReduceTool(String planId, ManusProperties manusProperties,
			MapReduceSharedStateManager sharedStateManager) {
		this(planId, manusProperties, sharedStateManager, (List<String>) null);
	}

	// Convenience constructor with comma-separated string
	public MapReduceTool(String planId, ManusProperties manusProperties,
			MapReduceSharedStateManager sharedStateManager, String terminateColumnsString) {
		this.planId = planId;
		this.manusProperties = manusProperties;
		this.workingDirectoryPath = CodeUtils.getWorkingDirectory(manusProperties.getBaseDir());
		this.sharedStateManager = sharedStateManager;
		
		// Parse comma-separated string into List<String>
		if (terminateColumnsString != null && !terminateColumnsString.trim().isEmpty()) {
			this.terminateColumns = Arrays.asList(terminateColumnsString.split(","))
				.stream()
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.collect(Collectors.toList());
		} else {
			this.terminateColumns = null;
		}
	}

	// Main constructor with List<String> terminateColumns
	public MapReduceTool(String planId, ManusProperties manusProperties,
			MapReduceSharedStateManager sharedStateManager, List<String> terminateColumns) {
		this.planId = planId;
		this.manusProperties = manusProperties;
		this.workingDirectoryPath = CodeUtils.getWorkingDirectory(manusProperties.getBaseDir());
		this.sharedStateManager = sharedStateManager;
		this.terminateColumns = terminateColumns;
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
		// Get terminate columns from shared state manager if available
		List<String> terminateColumns = null;
		if (sharedStateManager != null && planId != null) {
			terminateColumns = sharedStateManager.getReturnColumns(planId);
		}
		return generateParametersJson(terminateColumns);
	}

	@Override
	public Class<MapReduceInput> getInputType() {
		return MapReduceInput.class;
	}

	@Override
	public boolean isReturnDirect() {
		return false;
	}

	@Override
	public void setPlanId(String planId) {
		this.planId = planId;
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
	 * Execute MapReduce operation, accepts strongly typed input object
	 */
	public ToolExecuteResult run(MapReduceInput input) {
		log.info("MapReduceTool input: action={}, filePath={}", input.getAction(), input.getFilePath());
		try {
			String action = input.getAction();
			if (action == null) {
				return new ToolExecuteResult("Error: action parameter is required");
			}

			return switch (action) {
				case ACTION_SPLIT_DATA -> {
					String filePath = input.getFilePath();
					List<String> inputTerminateColumns = input.getTerminateColumns();

					if (filePath == null) {
						yield new ToolExecuteResult("Error: file_path parameter is required");
					}

					// Use class-level terminateColumns if specified, otherwise use input parameter
					List<String> effectiveTerminateColumns = null;
					if (this.terminateColumns != null && !this.terminateColumns.isEmpty()) {
						// Use class-level terminate columns directly
						effectiveTerminateColumns = new ArrayList<>(this.terminateColumns);
					} else if (inputTerminateColumns != null) {
						effectiveTerminateColumns = inputTerminateColumns;
					}

					// Store terminate column information
					if (effectiveTerminateColumns != null && sharedStateManager != null) {
						sharedStateManager.setReturnColumns(planId, effectiveTerminateColumns);
					}

					yield processFileOrDirectory(filePath);
				}
				case ACTION_RECORD_MAP_OUTPUT -> {
					List<String> inputTerminateColumns = input.getTerminateColumns();
					List<List<Object>> data = input.getData();
					String taskId = input.getTaskId();
					String status = input.getStatus();

					// Use class-level terminateColumns if specified, otherwise use input parameter
					List<String> effectiveTerminateColumns = null;
					if (this.terminateColumns != null && !this.terminateColumns.isEmpty()) {
						// Use class-level terminate columns directly
						effectiveTerminateColumns = new ArrayList<>(this.terminateColumns);
					} else if (inputTerminateColumns != null) {
						effectiveTerminateColumns = inputTerminateColumns;
					}

					if (effectiveTerminateColumns == null || effectiveTerminateColumns.isEmpty()) {
						yield new ToolExecuteResult("Error: terminate_columns parameter is required");
					}
					if (data == null || data.isEmpty()) {
						yield new ToolExecuteResult("Error: data parameter is required");
					}
					if (taskId == null) {
						yield new ToolExecuteResult("Error: task_id parameter is required");
					}
					if (status == null) {
						yield new ToolExecuteResult("Error: status parameter is required");
					}
					
					// Validate status values
					if (!TASK_STATUS_COMPLETED.equals(status) && !TASK_STATUS_FAILED.equals(status)) {
						yield new ToolExecuteResult("Error: status must be either '" + TASK_STATUS_COMPLETED + "' or '" + TASK_STATUS_FAILED + "'");
					}

					// Convert structured data to content string
					String content = formatStructuredData(effectiveTerminateColumns, data);
					yield recordMapTaskOutput(content, taskId, status);
				}
				default -> new ToolExecuteResult(
						"Unknown operation: " + action + ". Supported operations: " + ACTION_SPLIT_DATA + ", " + ACTION_RECORD_MAP_OUTPUT);
			};

		}
		catch (Exception e) {
			log.error("MapReduceTool execution failed", e);
			return new ToolExecuteResult("Tool execution failed: " + e.getMessage());
		}
	}

	/**
	 * Process complete workflow for file or directory: validate existence -> split data
	 */
	private ToolExecuteResult processFileOrDirectory(String filePath) {
		try {
			// Ensure planId exists, use default if empty
			if (planId == null || planId.trim().isEmpty()) {
				planId = DEFAULT_PLAN_ID_PREFIX + System.currentTimeMillis();
				log.info("planId is empty, using default value: {}", planId);
			}

			// Validate file or folder existence
			// Ensure working directory is initialized
			if (workingDirectoryPath == null) {
				if (manusProperties != null) {
					workingDirectoryPath = CodeUtils.getWorkingDirectory(manusProperties.getBaseDir());
				}
				else {
					// If no manusProperties, use default method
					workingDirectoryPath = CodeUtils.getWorkingDirectory(null);
				}
			}
		// Process based on path type
		Path path = null;
		boolean foundInInnerStorage = false;
		
		// First, try to find file in inner storage directory (similar to InnerStorageTool)
		if (!Paths.get(filePath).isAbsolute()) {
			// Check in inner storage first
			Path planDir = getPlanDirectory(planId);
			Path innerStoragePath = planDir.resolve(filePath);
			
			if (Files.exists(innerStoragePath)) {
				path = innerStoragePath;
				foundInInnerStorage = true;
				log.info("Found file in inner storage: {}", path.toAbsolutePath());
			}
		}
		
		// If not found in inner storage, try working directory
		if (path == null) {
			if (Paths.get(filePath).isAbsolute()) {
				// If absolute path, use directly
				path = Paths.get(filePath);
			}
			else {
				// If relative path, resolve based on working directory
				path = Paths.get(workingDirectoryPath).resolve(filePath);
			}
			log.info("Checking file in working directory: {}", path.toAbsolutePath());
		}
		if (!Files.exists(path)) {
			String errorMsg = "Error: File or directory does not exist: " + path.toAbsolutePath().toString();
			if (!foundInInnerStorage) {
				// Also check if file exists in inner storage and provide helpful message
				Path planDir = getPlanDirectory(planId);
				Path innerStoragePath = planDir.resolve(filePath);
				if (Files.exists(innerStoragePath)) {
					errorMsg += "\nNote: File exists in inner storage at: " + innerStoragePath.toAbsolutePath().toString();
				} else {
					errorMsg += "\nSearched in: working directory and inner storage (" + planDir.toAbsolutePath().toString() + ")";
				}
			}
			return new ToolExecuteResult(errorMsg);
		}

			boolean isFile = Files.isRegularFile(path);
			boolean isDirectory = Files.isDirectory(path);

			// Determine output directory - store to inner_storage/{planId}/tasks directory
			Path planDir = getPlanDirectory(planId);
			Path tasksPath = planDir.resolve(TASKS_DIRECTORY_NAME);
			ensureDirectoryExists(tasksPath);

			List<String> allTaskDirs = new ArrayList<>();

			// Check if infinite context is enabled for enhanced processing
			boolean infiniteContextEnabled = isInfiniteContextEnabled();
			if (infiniteContextEnabled) {
				log.info("Infinite context enabled for plan: {}, parallel threads: {}, context size: {}", 
					planId, getInfiniteContextParallelThreads(), getInfiniteContextTaskContextSize());
			}

			if (isFile && isTextFile(path.toString())) {
				// Process single file with dynamic split size
				int splitSize = infiniteContextEnabled ? getInfiniteContextTaskContextSize() : getSplitSize();
				SplitResult result = splitSingleFileToTasks(path, null, splitSize, tasksPath, null);
				allTaskDirs.addAll(result.taskDirs);

			}
			else if (isDirectory) {
				// Process all text files in directory with dynamic split size
				int splitSize = infiniteContextEnabled ? getInfiniteContextTaskContextSize() : getSplitSize();
				List<Path> textFiles = Files.list(path)
					.filter(Files::isRegularFile)
					.filter(p -> isTextFile(p.toString()))
					.collect(Collectors.toList());

				for (Path file : textFiles) {
					SplitResult result = splitSingleFileToTasks(file, null, splitSize, tasksPath, null);
					allTaskDirs.addAll(result.taskDirs);
				}
			}

			// Update split results
			if (sharedStateManager != null) {
				sharedStateManager.setSplitResults(planId, allTaskDirs);
			}

			// Generate concise return result
			StringBuilder result = new StringBuilder();
			result.append("File splitting successful");
			result.append(", created ").append(allTaskDirs.size()).append(" task directories");

			// If terminate columns are required, add terminate column information
			if (sharedStateManager != null) {
				List<String> terminateColumns = sharedStateManager.getReturnColumns(planId);
				if (!terminateColumns.isEmpty()) {
					result.append(", terminate columns: ").append(terminateColumns);
				}
			}

			String resultStr = result.toString();
			if (sharedStateManager != null) {
				sharedStateManager.setLastOperationResult(planId, resultStr);
			}
			
			// Mark that split data operation has completed, allowing termination
			this.mapOutputRecorded = true;
			
			return new ToolExecuteResult(resultStr);

		}
		catch (Exception e) {
			String error = "Processing failed: " + e.getMessage();
			log.error(error, e);
			return new ToolExecuteResult(error);
		}
	}

	/**
	 * Split results class
	 */
	private static class SplitResult {

		List<String> taskDirs;

		SplitResult(List<String> taskDirs) {
			this.taskDirs = taskDirs;
		}

	}

	/**
	 * Split single file into task directory structure
	 */
	private SplitResult splitSingleFileToTasks(Path filePath, String headers, int splitSize, Path tasksPath,
			String delimiter) throws IOException {
		List<String> taskDirs = new ArrayList<>();
		String fileName = filePath.getFileName().toString();

		try (BufferedReader reader = Files.newBufferedReader(filePath)) {
			String line;
			StringBuilder currentContent = new StringBuilder();

			while ((line = reader.readLine()) != null) {
				// Add line content and newline character
				String lineWithNewline = line + "\n";

				// Check if adding this line would exceed character limit
				if (currentContent.length() + lineWithNewline.length() > splitSize && currentContent.length() > 0) {
					// If would exceed limit and current content is not empty, save current content first
					String taskDir = createTaskDirectory(tasksPath, currentContent.toString(), fileName);
					taskDirs.add(taskDir);
					currentContent = new StringBuilder();
				}

				// Add current line
				currentContent.append(lineWithNewline);
			}

			// Process remaining content
			if (currentContent.length() > 0) {
				String taskDir = createTaskDirectory(tasksPath, currentContent.toString(), fileName);
				taskDirs.add(taskDir);
			}
		}

		return new SplitResult(taskDirs);
	}

	/**
	 * Create task directory structure
	 */
	private String createTaskDirectory(Path tasksPath, String content, String originalFileName) throws IOException {
		// Generate task ID
		String taskId = null;
		if (sharedStateManager != null) {
			taskId = sharedStateManager.getNextTaskId(planId);
		}
		else {
			// Fallback solution: use default format
			taskId = String.format(TASK_ID_FORMAT, 1);
		}

		Path taskDir = tasksPath.resolve(taskId);
		ensureDirectoryExists(taskDir);

		// Create input.md file
		Path inputFile = taskDir.resolve(TASK_INPUT_FILE_NAME);
		StringBuilder inputContent = new StringBuilder();
		inputContent.append("# Document Fragment\n\n");
		inputContent.append("**Original File:** ").append(originalFileName).append("\n\n");
		inputContent.append("**Task ID:** ").append(taskId).append("\n\n");
		inputContent.append("## Content\n\n");
		inputContent.append("```\n");
		inputContent.append(content);
		inputContent.append("```\n");

		Files.write(inputFile, inputContent.toString().getBytes());

		// Create initial status file status.json
		Path statusFile = taskDir.resolve(TASK_STATUS_FILE_NAME);
		TaskStatus initialStatus = new TaskStatus();
		initialStatus.taskId = taskId;
		initialStatus.status = TASK_STATUS_PENDING;
		initialStatus.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
		initialStatus.inputFile = inputFile.toAbsolutePath().toString();

		String statusJson = objectMapper.writeValueAsString(initialStatus);
		Files.write(statusFile, statusJson.getBytes());

		return taskDir.toAbsolutePath().toString();
	}

	/**
	 * Check if file is a text file
	 */
	private boolean isTextFile(String fileName) {
		String lowercaseFileName = fileName.toLowerCase();
		return lowercaseFileName.endsWith(".csv") || lowercaseFileName.endsWith(".tsv")
				|| lowercaseFileName.endsWith(".txt") || lowercaseFileName.endsWith(".dat")
				|| lowercaseFileName.endsWith(".log") || lowercaseFileName.endsWith(".json")
				|| lowercaseFileName.endsWith(".xml") || lowercaseFileName.endsWith(".yaml")
				|| lowercaseFileName.endsWith(".yml") || lowercaseFileName.endsWith(".md");
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

	@Override
	public String getCurrentToolStateString() {
		if (sharedStateManager != null && planId != null) {
			return sharedStateManager.getCurrentToolStateString(planId);
		}

		// Fallback solution
		StringBuilder sb = new StringBuilder();
		// sb.append("MapReduceTool current status:\n");
		// sb.append("- Plan ID: ").append(planId != null ? planId : "Not set").append("\n");
		// sb.append("- Shared state manager: ").append(sharedStateManager != null ? "Connected" : "Not connected").append("\n");
		return sb.toString();
	}

	@Override
	public void cleanup(String planId) {
		// Clean up shared state
		if (sharedStateManager != null && planId != null) {
			sharedStateManager.cleanupPlanState(planId);
		}
		log.info("MapReduceTool cleanup completed for planId: {}", planId);
	}

	@Override
	public ToolExecuteResult apply(MapReduceInput input, ToolContext toolContext) {
		return run(input);
	}

	/**
	 * Get task directory list
	 */
	public List<String> getSplitResults() {
		if (sharedStateManager != null && planId != null) {
			return sharedStateManager.getSplitResults(planId);
		}
		return new ArrayList<>();
	}

	/**
	 * Get inner storage root directory path
	 */
	private Path getInnerStorageRoot() {
		if (workingDirectoryPath == null) {
			// Use CodeUtils.getWorkingDirectory to get working directory, consistent with InnerStorageService
			if (manusProperties != null) {
				workingDirectoryPath = CodeUtils.getWorkingDirectory(manusProperties.getBaseDir());
			}
			else {
				// If no manusProperties, use default method
				workingDirectoryPath = CodeUtils.getWorkingDirectory(null);
			}
		}
		return Paths.get(workingDirectoryPath, "inner_storage");
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
		if (!Files.exists(directory)) {
			Files.createDirectories(directory);
			log.debug("Created directory: {}", directory);
		}
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
			if (planId == null || planId.trim().isEmpty()) {
				return new ToolExecuteResult("Error: planId not set, cannot record task status");
			}
			// Locate task directory
			Path planDir = getPlanDirectory(planId);
			Path taskDir = planDir.resolve(TASKS_DIRECTORY_NAME).resolve(taskId);

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
				sharedStateManager.recordMapTaskStatus(planId, taskId, sharedTaskStatus);
			}

			// Write updated status file
			String statusJson = objectMapper.writeValueAsString(taskStatus);
			Files.write(statusFile, statusJson.getBytes());
			
			// Mark that map output has been recorded, allowing termination
			this.mapOutputRecorded = true;
			
			String result = String.format("Task %s status recorded: %s, output file: %s", taskId, status, TASK_OUTPUT_FILE_NAME);
			log.info(result);
			return new ToolExecuteResult(result);

		}
		catch (Exception e) {
			String error = "Recording Map task status failed: " + e.getMessage();
			log.error(error, e);
			return new ToolExecuteResult(error);
		}
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
		// For now, use default timeout until the configuration is added to ManusProperties
		return 300; // Default timeout is 5 minutes
	}

	/**
	 * Get split size configuration for MapReduce operations
	 * @return Split size in characters, returns default value if not configured
	 */
	private Integer getSplitSize() {
		// For now, use default split size until the configuration is added to ManusProperties
		return DEFAULT_SPLIT_SIZE;
	}

	/**
	 * Check if infinite context is enabled
	 * @return true if infinite context is enabled, false otherwise
	 */
	private boolean isInfiniteContextEnabled() {
		if (manusProperties != null) {
			Boolean enabled = manusProperties.getInfiniteContextEnabled();
			return enabled != null ? enabled : false;
		}
		return false;
	}

	/**
	 * Get infinite context parallel thread count
	 * @return Number of parallel threads for infinite context processing
	 */
	private Integer getInfiniteContextParallelThreads() {
		if (manusProperties != null) {
			Integer threads = manusProperties.getInfiniteContextParallelThreads();
			return threads != null ? threads : 4; // Default 4 threads
		}
		return 4; // Default 4 threads
	}

	/**
	 * Get infinite context task context size
	 * @return Context size for infinite context tasks
	 */
	private Integer getInfiniteContextTaskContextSize() {
		if (manusProperties != null) {
			Integer contextSize = manusProperties.getInfiniteContextTaskContextSize();
			return contextSize != null ? contextSize : 8192; // Default 8192 characters
		}
		return 8192; // Default 8192 characters
	}

	/**
	 * Get ManusProperties instance - provides access to all configuration values
	 * @return ManusProperties instance for configuration access
	 */
	public ManusProperties getManusProperties() {
		return this.manusProperties;
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

	// ==================== TerminableTool interface implementation ====================

	@Override
	public boolean canTerminate() {
		// MapReduceTool can be terminated only after map output has been recorded
		// This ensures that the tool completes its processing cycle before termination
		return mapOutputRecorded;
	}

}
