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
 * Data split tool for MapReduce workflow data preparation phase Responsible for
 * validating file existence and performing data split processing
 */
public class DataSplitTool extends AbstractBaseTool<DataSplitTool.DataSplitInput> implements TerminableTool {

	private static final Logger log = LoggerFactory.getLogger(DataSplitTool.class);

	// ==================== 配置常量 ====================

	/**
	 * Default plan ID prefix When planId is empty, use this prefix + timestamp to
	 * generate default ID
	 */
	private static final String DEFAULT_PLAN_ID_PREFIX = "plan-";

	/**
	 * Task directory name All tasks are stored under this directory
	 */
	private static final String TASKS_DIRECTORY_NAME = "tasks";

	/**
	 * Task ID format template Used to generate incremental task IDs like task_001,
	 * task_002
	 */
	private static final String TASK_ID_FORMAT = "task_%03d";

	/**
	 * Task input file name Stores document fragment content after splitting
	 */
	private static final String TASK_INPUT_FILE_NAME = "input.md";

	/**
	 * Task status file name Stores task execution status information
	 */
	private static final String TASK_STATUS_FILE_NAME = "status.json";

	/**
	 * Task status: pending
	 */
	private static final String TASK_STATUS_PENDING = "pending";

	/**
	 * Internal input class for defining data split tool input parameters
	 */
	public static class DataSplitInput {

		@com.fasterxml.jackson.annotation.JsonProperty("file_path")
		private String filePath;

		@com.fasterxml.jackson.annotation.JsonProperty("terminate_columns")
		private List<String> terminateColumns;

		public DataSplitInput() {
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

	}

	private static final String TOOL_NAME = "data_split_tool";

	private static final String TOOL_DESCRIPTION = """
			Data split tool for MapReduce workflow data preparation phase.
			Automatically validates file existence and performs data split processing.
			Supports CSV, TSV, TXT and other text format data files.

			Key features:
			- File and directory existence validation
			- Automatic text file detection and processing
			- Task directory structure creation with metadata
			- Support for both single files and directories
			""";

	private static final String PARAMETERS_JSON = """
			{
			    "type": "object",
			    "properties": {
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
			    "required": ["file_path"],
			    "additionalProperties": false
			}
			""";

	private UnifiedDirectoryManager unifiedDirectoryManager;

	private ManusProperties manusProperties;

	// 共享状态管理器，用于管理多个Agent实例间的共享状态
	private MapReduceSharedStateManager sharedStateManager;

	// Track if split operation has completed, allowing termination
	private volatile boolean splitCompleted = false;

	private static final ObjectMapper objectMapper = new ObjectMapper();

	public DataSplitTool(String planId, ManusProperties manusProperties, MapReduceSharedStateManager sharedStateManager,
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

	@Override
	public String getDescription() {
		return TOOL_DESCRIPTION;
	}

	@Override
	public String getParameters() {
		return PARAMETERS_JSON;
	}

	@Override
	public Class<DataSplitInput> getInputType() {
		return DataSplitInput.class;
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
	 * Execute data split operation
	 */
	@Override
	public ToolExecuteResult run(DataSplitInput input) {
		log.info("DataSplitTool input: filePath={}", input.getFilePath());
		try {
			String filePath = input.getFilePath();
			if (filePath == null) {
				return new ToolExecuteResult("Error: file_path parameter is required");
			}

			return processFileOrDirectory(filePath);

		}
		catch (Exception e) {
			log.error("DataSplitTool execution failed", e);
			return new ToolExecuteResult("Tool execution failed: " + e.getMessage());
		}
	}

	/**
	 * Process complete workflow for file or directory: validate existence -> split data
	 */
	private ToolExecuteResult processFileOrDirectory(String filePath) {
		try {
			// Ensure planId exists, use default if empty
			if (currentPlanId == null || currentPlanId.trim().isEmpty()) {
				currentPlanId = DEFAULT_PLAN_ID_PREFIX + System.currentTimeMillis();
				log.info("currentPlanId is empty, using default value: {}", currentPlanId);
			}

			// Validate file or folder existence
			// Use UnifiedDirectoryManager to get working directory path
			String workingDirectoryPath = unifiedDirectoryManager.getWorkingDirectoryPath();
			// Process based on path type
			Path path = null;
			boolean foundInInnerStorage = false;

			// First, try to find file in inner storage directory
			if (!Paths.get(filePath).isAbsolute()) {
				// Check in inner storage first
				Path planDir = getPlanDirectory(rootPlanId);
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
					// Also check if file exists in inner storage and provide helpful
					// message
					Path planDir = getPlanDirectory(currentPlanId);
					Path innerStoragePath = planDir.resolve(filePath);
					if (Files.exists(innerStoragePath)) {
						errorMsg += "\nNote: File exists in inner storage at: "
								+ innerStoragePath.toAbsolutePath().toString();
					}
					else {
						errorMsg += "\nSearched in: working directory and inner storage ("
								+ planDir.toAbsolutePath().toString() + ")";
					}
				}
				return new ToolExecuteResult(errorMsg);
			}

			boolean isFile = Files.isRegularFile(path);
			boolean isDirectory = Files.isDirectory(path);

			// Determine output directory - store to
			// inner_storage/{rootPlanId}/{currentPlanId}/tasks directory
			// This creates a hierarchical structure where sub-plan data is stored under
			// the root plan
			Path rootPlanDir = getPlanDirectory(rootPlanId);
			Path currentPlanDir = rootPlanDir.resolve(currentPlanId);
			Path tasksPath = currentPlanDir.resolve(TASKS_DIRECTORY_NAME);
			ensureDirectoryExists(tasksPath);

			List<String> allTaskDirs = new ArrayList<>();

			if (isFile && isTextFile(path.toString())) {
				// Process single file - use infinite context task context size
				int splitSize = getInfiniteContextTaskContextSize();
				SplitResult result = splitSingleFileToTasks(path, null, splitSize, tasksPath, null);
				allTaskDirs.addAll(result.taskDirs);

			}
			else if (isDirectory) {
				// Process all text files in directory - use infinite context task context
				// size
				int splitSize = getInfiniteContextTaskContextSize();
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
				sharedStateManager.setSplitResults(currentPlanId, allTaskDirs);
			}

			// Generate concise return result
			StringBuilder result = new StringBuilder();
			result.append("File splitting successful");
			result.append(", created ").append(allTaskDirs.size()).append(" task directories");

			String resultStr = result.toString();
			if (sharedStateManager != null) {
				sharedStateManager.setLastOperationResult(currentPlanId, resultStr);
			}

			// Mark that split operation has completed, allowing termination
			this.splitCompleted = true;

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
					// If would exceed limit and current content is not empty, save
					// current content first
					String taskDir = createTaskDirectory(tasksPath, currentContent.toString(), fileName);
					taskDirs.add(taskDir);
					currentContent = new StringBuilder();
				}

				// Handle oversized single line when currentContent is empty
				if (currentContent.length() == 0 && lineWithNewline.length() > splitSize) {
					// Split the oversized line into multiple chunks
					String lineContent = lineWithNewline;
					int startIndex = 0;
					int chunkCount = 0;

					log.warn("Line exceeds split size limit ({} chars > {} chars), splitting into chunks: file={}",
							lineWithNewline.length(), splitSize, fileName);

					while (startIndex < lineContent.length()) {
						int endIndex = Math.min(startIndex + splitSize, lineContent.length());
						String chunk = lineContent.substring(startIndex, endIndex);
						chunkCount++;

						// Create task directory for each chunk
						String taskDir = createTaskDirectory(tasksPath, chunk, fileName);
						taskDirs.add(taskDir);

						startIndex = endIndex;
					}

					log.info("Oversized line split into {} chunks for file: {}", chunkCount, fileName);
				}
				else {
					// Add current line normally
					currentContent.append(lineWithNewline);
				}
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
			taskId = sharedStateManager.getNextTaskId(currentPlanId);
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
		log.info("DataSplitTool cleanup completed for planId: {}", planId);
	}

	@Override
	public ToolExecuteResult apply(DataSplitInput input, ToolContext toolContext) {
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

	// ==================== TerminableTool interface implementation
	// ====================

	@Override
	public boolean canTerminate() {
		// DataSplitTool can be terminated only after split operation has completed
		return splitCompleted;
	}

}
