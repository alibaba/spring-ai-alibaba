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
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;
import com.alibaba.cloud.ai.example.manus.tool.code.CodeUtils;
import com.alibaba.cloud.ai.example.manus.config.ManusProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.openai.api.OpenAiApi;

/**
 * 数据分割工具，用于MapReduce流程中的数据准备阶段 负责验证文件存在性、识别表格头部信息并进行数据分割处理
 */
public class MapReduceTool implements ToolCallBiFunctionDef<MapReduceTool.MapReduceInput> {

	private static final Logger log = LoggerFactory.getLogger(MapReduceTool.class);

	// ==================== 配置常量 ====================

	/**
	 * 支持的操作类型：数据分割
	 */
	private static final String ACTION_SPLIT_DATA = "split_data";

	/**
	 * 支持的操作类型：记录Map输出
	 */
	private static final String ACTION_RECORD_MAP_OUTPUT = "record_map_output";

	/**
	 * 默认的计划ID前缀 当planId为空时，使用此前缀 + 时间戳生成默认ID
	 */
	private static final String DEFAULT_PLAN_ID_PREFIX = "plan-";

	/**
	 * 任务目录名称 所有任务都存储在此目录下
	 */
	private static final String TASKS_DIRECTORY_NAME = "tasks";

	/**
	 * 任务ID格式模板 用于生成递增的任务ID，如 task_001, task_002
	 */
	private static final String TASK_ID_FORMAT = "task_%03d";

	/**
	 * 任务输入文件名 存储分割后的文档片段内容
	 */
	private static final String TASK_INPUT_FILE_NAME = "input.md";

	/**
	 * 任务状态文件名 存储任务的执行状态信息
	 */
	private static final String TASK_STATUS_FILE_NAME = "status.json";

	/**
	 * 任务输出文件名 存储Map阶段处理完成后的结果
	 */
	private static final String TASK_OUTPUT_FILE_NAME = "output.md";

	/**
	 * 默认的文件分割大小（字符数） 每个任务处理的文件字符数，可根据实际需求调整
	 */
	private static final int DEFAULT_SPLIT_SIZE = 5000;

	/**
	 * 任务状态：待处理
	 */
	private static final String TASK_STATUS_PENDING = "pending";

	/**
	 * 任务状态：已完成
	 */
	private static final String TASK_STATUS_COMPLETED = "completed";

	/**
	 * 任务状态：失败
	 */
	private static final String TASK_STATUS_FAILED = "failed";

	/**
	 * 内部输入类，用于定义MapReduce工具的输入参数
	 */
	public static class MapReduceInput {

		private String action;

		@com.fasterxml.jackson.annotation.JsonProperty("file_path")
		private String filePath;

		@com.fasterxml.jackson.annotation.JsonProperty("return_columns")
		private List<String> returnColumns;

		private String content;

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

		public List<String> getReturnColumns() {
			return returnColumns;
		}

		public void setReturnColumns(List<String> returnColumns) {
			this.returnColumns = returnColumns;
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

	}

	private static final String TOOL_NAME = "map_reduce_tool";

	private static final String TOOL_DESCRIPTION = """
			数据分割工具，用于MapReduce流程中的数据准备阶段和任务状态管理。
			支持的操作：
			- split_data: 自动完成验证文件存在性并进行数据分割处理，支持CSV、TSV、TXT等文本格式的数据文件。
			  输出目录采用inner_storage/{planId}/tasks/task_{taskId}模式，与InnerStorageService统一管理，
			  每个任务都有独立的目录，包含input.md（分割的文档片段）文件。
			- record_map_output: 接受Map阶段处理完成后的内容，自动生成文件名并创建输出文件，记录任务状态。
			  工具会自动在inner_storage/{planId}/tasks/task_{taskId}目录下创建标准化文件结构：
			  input.md（文档片段）、status.json（任务状态）、output.md（模型处理结果），
			  每个任务都有独立的目录和统一的文件命名。

			任务管理特性：
			- 自动生成递增的任务ID
			- 每个任务创建独立的目录：inner_storage/{planId}/tasks/task_{taskId}
			- 标准化目录结构：每个任务目录包含input.md（文档片段）、status.json（状态文件）、output.md（模型输出）
			- 统一的文件命名，便于批量处理和管理
			- 与InnerStorageService统一存储管理
			- 通过 getSplitResults() 方法获取分割文件列表用于任务分配
			- 支持Map阶段完成后的自动文件创建和状态记录管理
			""";

	private static final String PARAMETERS = """
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
			                "return_columns": {
			                    "type": "array",
			                    "items": {
			                        "type": "string"
			                    },
			                    "description": "返回结果的列名，用于结构化输出"
			                }
			            },
			            "required": ["action", "file_path"],
			            "additionalProperties": false
			        },		        {
			           "type": "object",
			           "properties": {
			               "action": {
			                   "type": "string",
			                   "const": "record_map_output"
			               },
			               "content": {
			                   "type": "string",
			                   "description": "Map阶段处理完成后的输出内容"
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
			           "required": ["action", "content", "task_id", "status"],
			           "additionalProperties": false
			       }
			    ]
			}
			""";

	private String planId;

	private String workingDirectoryPath;

	private ManusProperties manusProperties;

	// 共享状态管理器，用于管理多个Agent实例间的共享状态
	private MapReduceSharedStateManager sharedStateManager;

	private static final ObjectMapper objectMapper = new ObjectMapper();

	// public MapReduceTool() {
	// // 注意：默认构造函数中无法注入依赖，需要后续设置
	// }

	// public MapReduceTool(ManusProperties manusProperties) {
	// this.manusProperties = manusProperties;
	// this.workingDirectoryPath =
	// CodeUtils.getWorkingDirectory(manusProperties.getBaseDir());
	// }

	// public MapReduceTool(String planId, ManusProperties manusProperties) {
	// this.planId = planId;
	// this.manusProperties = manusProperties;
	// this.workingDirectoryPath =
	// CodeUtils.getWorkingDirectory(manusProperties.getBaseDir());
	// }

	public MapReduceTool(String planId, ManusProperties manusProperties,
			MapReduceSharedStateManager sharedStateManager) {
		this.planId = planId;
		this.manusProperties = manusProperties;
		this.workingDirectoryPath = CodeUtils.getWorkingDirectory(manusProperties.getBaseDir());
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
		return PARAMETERS;
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
		OpenAiApi.FunctionTool.Function function = new OpenAiApi.FunctionTool.Function(TOOL_DESCRIPTION, TOOL_NAME,
				PARAMETERS);
		return new OpenAiApi.FunctionTool(function);
	}

	/**
	 * 执行MapReduce操作，接受强类型输入对象
	 */
	public ToolExecuteResult run(MapReduceInput input) {
		log.info("MapReduceTool input: action={}, filePath={}", input.getAction(), input.getFilePath());
		try {
			String action = input.getAction();
			if (action == null) {
				return new ToolExecuteResult("错误：action参数是必需的");
			}

			return switch (action) {
				case ACTION_SPLIT_DATA -> {
					String filePath = input.getFilePath();
					List<String> columns = input.getReturnColumns();

					if (filePath == null) {
						yield new ToolExecuteResult("错误：file_path参数是必需的");
					}

					// 存储返回列信息
					if (columns != null && sharedStateManager != null) {
						sharedStateManager.setReturnColumns(planId, columns);
					}

					yield processFileOrDirectory(filePath);
				}
				case ACTION_RECORD_MAP_OUTPUT -> {
					String content = input.getContent();
					String taskId = input.getTaskId();
					String status = input.getStatus();

					if (content == null) {
						yield new ToolExecuteResult("错误：content参数是必需的");
					}
					if (taskId == null) {
						yield new ToolExecuteResult("错误：task_id参数是必需的");
					}
					if (status == null) {
						yield new ToolExecuteResult("错误：status参数是必需的");
					}

					yield recordMapTaskOutput(content, taskId, status);
				}
				default -> new ToolExecuteResult(
						"未知操作: " + action + "。支持的操作: " + ACTION_SPLIT_DATA + ", " + ACTION_RECORD_MAP_OUTPUT);
			};

		}
		catch (Exception e) {
			log.error("MapReduceTool执行失败", e);
			return new ToolExecuteResult("工具执行失败: " + e.getMessage());
		}
	}

	/**
	 * 处理文件或目录的完整流程：验证存在性 -> 分割数据
	 */
	private ToolExecuteResult processFileOrDirectory(String filePath) {
		try {
			// 确保 planId 存在，如果为空则使用默认值
			if (planId == null || planId.trim().isEmpty()) {
				planId = DEFAULT_PLAN_ID_PREFIX + System.currentTimeMillis();
				log.info("planId 为空，使用默认值: {}", planId);
			}

			// 验证文件或文件夹存在性
			// 确保工作目录已初始化
			if (workingDirectoryPath == null) {
				if (manusProperties != null) {
					workingDirectoryPath = CodeUtils.getWorkingDirectory(manusProperties.getBaseDir());
				}
				else {
					// 如果没有 manusProperties，使用默认方式
					workingDirectoryPath = CodeUtils.getWorkingDirectory(null);
				}
			}

			// 根据路径类型进行处理
			Path path;
			if (Paths.get(filePath).isAbsolute()) {
				// 如果是绝对路径，直接使用
				path = Paths.get(filePath);
			}
			else {
				// 如果是相对路径，基于工作目录解析
				path = Paths.get(workingDirectoryPath).resolve(filePath);
			}

			if (!Files.exists(path)) {
				return new ToolExecuteResult("错误：文件或目录不存在: " + path.toAbsolutePath().toString());
			}

			boolean isFile = Files.isRegularFile(path);
			boolean isDirectory = Files.isDirectory(path);

			// 确定输出目录 - 存储到 inner_storage/{planId}/tasks 目录
			Path planDir = getPlanDirectory(planId);
			Path tasksPath = planDir.resolve(TASKS_DIRECTORY_NAME);
			ensureDirectoryExists(tasksPath);

			List<String> allTaskDirs = new ArrayList<>();

			if (isFile && isTextFile(path.toString())) {
				// 处理单个文件
				SplitResult result = splitSingleFileToTasks(path, null, DEFAULT_SPLIT_SIZE, tasksPath, null);
				allTaskDirs.addAll(result.taskDirs);

			}
			else if (isDirectory) {
				// 处理文件夹中的所有文本文件
				List<Path> textFiles = Files.list(path)
					.filter(Files::isRegularFile)
					.filter(p -> isTextFile(p.toString()))
					.collect(Collectors.toList());

				for (Path file : textFiles) {
					SplitResult result = splitSingleFileToTasks(file, null, DEFAULT_SPLIT_SIZE, tasksPath, null);
					allTaskDirs.addAll(result.taskDirs);
				}
			}

			// 更新分割结果
			if (sharedStateManager != null) {
				sharedStateManager.setSplitResults(planId, allTaskDirs);
			}

			// 生成简洁的返回结果
			StringBuilder result = new StringBuilder();
			result.append("切分文件成功");
			result.append("，创建了").append(allTaskDirs.size()).append("个任务目录");

			// 如果有返回列要求，添加返回列信息
			if (sharedStateManager != null) {
				List<String> returnColumns = sharedStateManager.getReturnColumns(planId);
				if (!returnColumns.isEmpty()) {
					result.append("，返回列：").append(returnColumns);
				}
			}

			String resultStr = result.toString();
			if (sharedStateManager != null) {
				sharedStateManager.setLastOperationResult(planId, resultStr);
			}
			return new ToolExecuteResult(resultStr);

		}
		catch (Exception e) {
			String error = "处理失败: " + e.getMessage();
			log.error(error, e);
			return new ToolExecuteResult(error);
		}
	}

	/**
	 * 分割结果类
	 */
	private static class SplitResult {

		List<String> taskDirs;

		SplitResult(List<String> taskDirs) {
			this.taskDirs = taskDirs;
		}

	}

	/**
	 * 将单个文件分割成任务目录结构
	 */
	private SplitResult splitSingleFileToTasks(Path filePath, String headers, int splitSize, Path tasksPath,
			String delimiter) throws IOException {
		List<String> taskDirs = new ArrayList<>();
		String fileName = filePath.getFileName().toString();

		try (BufferedReader reader = Files.newBufferedReader(filePath)) {
			String line;
			StringBuilder currentContent = new StringBuilder();

			while ((line = reader.readLine()) != null) {
				// 添加行内容和换行符
				String lineWithNewline = line + "\n";

				// 检查添加这一行后是否会超过字符数限制
				if (currentContent.length() + lineWithNewline.length() > splitSize && currentContent.length() > 0) {
					// 如果会超过限制且当前内容不为空，先保存当前内容
					String taskDir = createTaskDirectory(tasksPath, currentContent.toString(), fileName);
					taskDirs.add(taskDir);
					currentContent = new StringBuilder();
				}

				// 添加当前行
				currentContent.append(lineWithNewline);
			}

			// 处理剩余内容
			if (currentContent.length() > 0) {
				String taskDir = createTaskDirectory(tasksPath, currentContent.toString(), fileName);
				taskDirs.add(taskDir);
			}
		}

		return new SplitResult(taskDirs);
	}

	/**
	 * 创建任务目录结构
	 */
	private String createTaskDirectory(Path tasksPath, String content, String originalFileName) throws IOException {
		// 生成任务ID
		String taskId = null;
		if (sharedStateManager != null) {
			taskId = sharedStateManager.getNextTaskId(planId);
		}
		else {
			// 回退方案：使用默认格式
			taskId = String.format(TASK_ID_FORMAT, 1);
		}

		Path taskDir = tasksPath.resolve(taskId);
		ensureDirectoryExists(taskDir);

		// 创建 input.md 文件
		Path inputFile = taskDir.resolve(TASK_INPUT_FILE_NAME);
		StringBuilder inputContent = new StringBuilder();
		inputContent.append("# 文档片段\n\n");
		inputContent.append("**原始文件:** ").append(originalFileName).append("\n\n");
		inputContent.append("**任务ID:** ").append(taskId).append("\n\n");
		inputContent.append("## 内容\n\n");
		inputContent.append("```\n");
		inputContent.append(content);
		inputContent.append("```\n");

		Files.write(inputFile, inputContent.toString().getBytes());

		// 创建初始状态文件 status.json
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
	 * 判断是否为文本文件
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
		if (sharedStateManager != null && planId != null) {
			return sharedStateManager.getCurrentToolStateString(planId);
		}

		// 回退方案
		StringBuilder sb = new StringBuilder();
		// sb.append("MapReduceTool 当前状态:\n");
		// sb.append("- Plan ID: ").append(planId != null ? planId : "未设置").append("\n");
		// sb.append("- 共享状态管理器: ").append(sharedStateManager != null ? "已连接" : "未连接").append("\n");
		return sb.toString();
	}

	@Override
	public void cleanup(String planId) {
		// 清理共享状态
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
	 * 获取任务目录列表
	 */
	public List<String> getSplitResults() {
		if (sharedStateManager != null && planId != null) {
			return sharedStateManager.getSplitResults(planId);
		}
		return new ArrayList<>();
	}

	/**
	 * 获取内部存储的根目录路径
	 */
	private Path getInnerStorageRoot() {
		if (workingDirectoryPath == null) {
			// 使用 CodeUtils.getWorkingDirectory 来获取工作目录，与 InnerStorageService 保持一致
			if (manusProperties != null) {
				workingDirectoryPath = CodeUtils.getWorkingDirectory(manusProperties.getBaseDir());
			}
			else {
				// 如果没有 manusProperties，使用默认方式
				workingDirectoryPath = CodeUtils.getWorkingDirectory(null);
			}
		}
		return Paths.get(workingDirectoryPath, "inner_storage");
	}

	/**
	 * 获取计划目录路径
	 */
	private Path getPlanDirectory(String planId) {
		return getInnerStorageRoot().resolve(planId);
	}

	/**
	 * 确保目录存在
	 */
	private void ensureDirectoryExists(Path directory) throws IOException {
		if (!Files.exists(directory)) {
			Files.createDirectories(directory);
			log.debug("Created directory: {}", directory);
		}
	}

	/**
	 * 记录Map任务的输出结果和状态
	 */
	private ToolExecuteResult recordMapTaskOutput(String content, String taskId, String status) {
		try {
			// 确保 planId 存在
			if (planId == null || planId.trim().isEmpty()) {
				return new ToolExecuteResult("错误：planId未设置，无法记录任务状态");
			}
			// 定位任务目录
			Path planDir = getPlanDirectory(planId);
			Path taskDir = planDir.resolve(TASKS_DIRECTORY_NAME).resolve(taskId);

			if (!Files.exists(taskDir)) {
				return new ToolExecuteResult("错误：任务目录不存在: " + taskId);
			}

			// 创建 output.md 文件
			Path outputFile = taskDir.resolve(TASK_OUTPUT_FILE_NAME);
			// 直接写入处理内容，不添加额外的元数据信息
			Files.write(outputFile, content.getBytes());
			String outputFilePath = outputFile.toAbsolutePath().toString();
			// 更新任务状态文件
			Path statusFile = taskDir.resolve(TASK_STATUS_FILE_NAME);
			TaskStatus taskStatus;

			if (Files.exists(statusFile)) {
				// 读取现有状态
				String existingStatusJson = new String(Files.readAllBytes(statusFile));
				taskStatus = objectMapper.readValue(existingStatusJson, TaskStatus.class);
			}
			else {
				// 创建新状态
				taskStatus = new TaskStatus();
				taskStatus.taskId = taskId;
				taskStatus.inputFile = taskDir.resolve(TASK_INPUT_FILE_NAME).toAbsolutePath().toString();
			}

			// 更新状态信息
			taskStatus.outputFilePath = outputFilePath;
			taskStatus.status = status;
			taskStatus.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

			// 存储到共享状态管理器中
			if (sharedStateManager != null) {
				MapReduceSharedStateManager.TaskStatus sharedTaskStatus = new MapReduceSharedStateManager.TaskStatus();
				sharedTaskStatus.taskId = taskStatus.taskId;
				sharedTaskStatus.inputFile = taskStatus.inputFile;
				sharedTaskStatus.outputFilePath = taskStatus.outputFilePath;
				sharedTaskStatus.status = taskStatus.status;
				sharedTaskStatus.timestamp = taskStatus.timestamp;
				sharedStateManager.recordMapTaskStatus(planId, taskId, sharedTaskStatus);
			}

			// 写入更新后的状态文件
			String statusJson = objectMapper.writeValueAsString(taskStatus);
			Files.write(statusFile, statusJson.getBytes());
			String result = String.format("任务 %s 状态已记录：%s，输出文件：%s", taskId, status, TASK_OUTPUT_FILE_NAME);
			log.info(result);
			return new ToolExecuteResult(result);

		}
		catch (Exception e) {
			String error = "记录Map任务状态失败: " + e.getMessage();
			log.error(error, e);
			return new ToolExecuteResult(error);
		}
	}

	/**
	 * 任务状态内部类
	 */
	@SuppressWarnings("unused")
	private static class TaskStatus {

		public String taskId;

		public String inputFile;

		public String outputFilePath;

		public String status;

		public String timestamp;

	}

}
