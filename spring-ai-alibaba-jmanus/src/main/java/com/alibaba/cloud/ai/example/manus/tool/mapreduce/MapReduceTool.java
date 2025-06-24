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
import com.fasterxml.jackson.core.type.TypeReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.function.FunctionToolCallback;

/**
 * 数据分割工具，用于MapReduce流程中的数据准备阶段 负责验证文件存在性、识别表格头部信息并进行数据分割处理
 */
public class MapReduceTool implements ToolCallBiFunctionDef {

	private static final Logger log = LoggerFactory.getLogger(MapReduceTool.class);

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

	private String lastOperationResult = "";

	private String lastProcessedFile = "";

	private List<String> splitResults = new ArrayList<>();
	
	private List<String> returnColumns = new ArrayList<>();

	// Map任务状态管理
	private Map<String, TaskStatus> mapTaskStatuses = new HashMap<>();
	
	// 任务计数器，用于生成任务ID
	private int taskCounter = 1;

	private static final ObjectMapper objectMapper = new ObjectMapper();

	public MapReduceTool() {
	}


	public MapReduceTool(ManusProperties manusProperties) {
		this.manusProperties = manusProperties;
		this.workingDirectoryPath = CodeUtils.getWorkingDirectory(manusProperties.getBaseDir());
	}

	public MapReduceTool(String planId, ManusProperties manusProperties) {
		this.planId = planId;
		this.manusProperties = manusProperties;
		this.workingDirectoryPath = CodeUtils.getWorkingDirectory(manusProperties.getBaseDir());
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
	public Class<?> getInputType() {
		return String.class;
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

	public static FunctionToolCallback<String, ToolExecuteResult> getFunctionToolCallback() {
		return FunctionToolCallback.builder(TOOL_NAME, new MapReduceTool())
			.description(TOOL_DESCRIPTION)
			.inputSchema(PARAMETERS)
			.inputType(String.class)
			.build();
	}

	public static FunctionToolCallback<String, ToolExecuteResult> getFunctionToolCallback(String planId, ManusProperties manusProperties) {
		return FunctionToolCallback.builder(TOOL_NAME, new MapReduceTool(planId, manusProperties))
			.description(TOOL_DESCRIPTION)
			.inputSchema(PARAMETERS)
			.inputType(String.class)
			.build();
	}

	public ToolExecuteResult run(String toolInput) {
		log.info("SplitTool toolInput: {}", toolInput);
		try {
			Map<String, Object> toolInputMap = objectMapper.readValue(toolInput,
					new TypeReference<Map<String, Object>>() {
					});

			String action = (String) toolInputMap.get("action");
			if (action == null) {
				return new ToolExecuteResult("错误：action参数是必需的");
			}

			return switch (action) {
				case "split_data" -> {
					String filePath = (String) toolInputMap.get("file_path");
					@SuppressWarnings("unchecked")
					List<String> columns = (List<String>) toolInputMap.get("return_columns");
					
					if (filePath == null) {
						yield new ToolExecuteResult("错误：file_path参数是必需的");
					}
					
					// 存储返回列信息
					if (columns != null) {
						this.returnColumns = new ArrayList<>(columns);
					}
					
					yield processFileOrDirectory(filePath);
				}
				case "record_map_output" -> {
					String content = (String) toolInputMap.get("content");
					String taskId = (String) toolInputMap.get("task_id");
					String status = (String) toolInputMap.get("status");
					
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
				default -> new ToolExecuteResult("未知操作: " + action + "。支持的操作: split_data, record_map_output");
			};

		}
		catch (Exception e) {
			log.error("SplitTool执行失败", e);
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
				planId = "plan-" + System.currentTimeMillis();
				log.info("planId 为空，使用默认值: {}", planId);
			}
			
			// 验证文件或文件夹存在性
			// 确保工作目录已初始化
			if (workingDirectoryPath == null) {
				if (manusProperties != null) {
					workingDirectoryPath = CodeUtils.getWorkingDirectory(manusProperties.getBaseDir());
				} else {
					// 如果没有 manusProperties，使用默认方式
					workingDirectoryPath = CodeUtils.getWorkingDirectory(null);
				}
			}
			
			// 根据路径类型进行处理
			Path path;
			if (Paths.get(filePath).isAbsolute()) {
				// 如果是绝对路径，直接使用
				path = Paths.get(filePath);
			} else {
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
			Path tasksPath = planDir.resolve("tasks");
			ensureDirectoryExists(tasksPath);

			List<String> allTaskDirs = new ArrayList<>();

			if (isFile && isTextFile(path.toString())) {
				// 处理单个文件
				SplitResult result = splitSingleFileToTasks(path, null, 1000, tasksPath, null);
				allTaskDirs.addAll(result.taskDirs);

			}
			else if (isDirectory) {
				// 处理文件夹中的所有文本文件
				List<Path> textFiles = Files.list(path)
					.filter(Files::isRegularFile)
					.filter(p -> isTextFile(p.toString()))
					.collect(Collectors.toList());

				for (Path file : textFiles) {
					SplitResult result = splitSingleFileToTasks(file, null, 1000, tasksPath, null);
					allTaskDirs.addAll(result.taskDirs);
				}
			}

			// 更新分割结果
			splitResults = allTaskDirs;
			
			// 生成简洁的返回结果
			StringBuilder result = new StringBuilder();
			result.append("切分文件成功");
			result.append("，创建了").append(allTaskDirs.size()).append("个任务目录");
			
			// 如果有返回列要求，添加返回列信息
			if (!returnColumns.isEmpty()) {
				result.append("，返回列：").append(returnColumns);
			}
			
			lastOperationResult = result.toString();
			return new ToolExecuteResult(lastOperationResult);

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
	private SplitResult splitSingleFileToTasks(Path filePath, String headers, int splitSize, Path tasksPath, String delimiter)
			throws IOException {
		List<String> taskDirs = new ArrayList<>();
		String fileName = filePath.getFileName().toString();

		try (BufferedReader reader = Files.newBufferedReader(filePath)) {
			String line;
			int lineCount = 0;
			StringBuilder currentContent = new StringBuilder();
			
			while ((line = reader.readLine()) != null) {
				currentContent.append(line).append("\n");
				lineCount++;
				
				// 当达到分割大小时，创建新的任务目录
				if (lineCount % splitSize == 0) {
					String taskDir = createTaskDirectory(tasksPath, currentContent.toString(), fileName);
					taskDirs.add(taskDir);
					currentContent = new StringBuilder();
				}
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
		String taskId = String.format("task_%03d", taskCounter++);
		Path taskDir = tasksPath.resolve(taskId);
		ensureDirectoryExists(taskDir);
		
		// 创建 input.md 文件
		Path inputFile = taskDir.resolve("input.md");
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
		Path statusFile = taskDir.resolve("status.json");
		TaskStatus initialStatus = new TaskStatus();
		initialStatus.taskId = taskId;
		initialStatus.status = "pending";
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
		StringBuilder sb = new StringBuilder();
		sb.append("MapReduceTool 当前状态:\n");
		sb.append("- Plan ID: ").append(planId != null ? planId : "未设置").append("\n");
		sb.append("- 最后处理文件: ").append(lastProcessedFile.isEmpty() ? "无" : lastProcessedFile).append("\n");
		sb.append("- 最后操作结果: ").append(lastOperationResult.isEmpty() ? "无" : "已完成").append("\n");
		sb.append("- 任务目录数: ").append(splitResults.size()).append("\n");

		return sb.toString();
	}

	@Override
	public void cleanup(String planId) {
		// 清理资源
		splitResults.clear();
		lastOperationResult = "";
		lastProcessedFile = "";
		mapTaskStatuses.clear();
		taskCounter = 1;
		log.info("MapReduceTool cleanup completed for planId: {}", planId);
	}

	@Override
	public ToolExecuteResult apply(String s, ToolContext toolContext) {
		return run(s);
	}

	/**
	 * 获取任务目录列表
	 */
	public List<String> getSplitResults() {
		return new ArrayList<>(splitResults);
	}
	
	/**
	 * 获取内部存储的根目录路径
	 */
	private Path getInnerStorageRoot() {
		if (workingDirectoryPath == null) {
			// 使用 CodeUtils.getWorkingDirectory 来获取工作目录，与 InnerStorageService 保持一致
			if (manusProperties != null) {
				workingDirectoryPath = CodeUtils.getWorkingDirectory(manusProperties.getBaseDir());
			} else {
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
			Path taskDir = planDir.resolve("tasks").resolve(taskId);
			
			if (!Files.exists(taskDir)) {
				return new ToolExecuteResult("错误：任务目录不存在: " + taskId);
			}

			// 创建 output.md 文件
			Path outputFile = taskDir.resolve("output.md");
			StringBuilder outputContent = new StringBuilder();
			outputContent.append("# 任务处理结果\n\n");
			outputContent.append("**任务ID:** ").append(taskId).append("\n\n");
			outputContent.append("**处理状态:** ").append(status).append("\n\n");
			outputContent.append("**处理时间:** ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n\n");
			outputContent.append("## 处理结果\n\n");
			outputContent.append(content).append("\n");
			
			Files.write(outputFile, outputContent.toString().getBytes());
			String outputFilePath = outputFile.toAbsolutePath().toString();

			// 更新任务状态文件
			Path statusFile = taskDir.resolve("status.json");
			TaskStatus taskStatus;
			
			if (Files.exists(statusFile)) {
				// 读取现有状态
				String existingStatusJson = new String(Files.readAllBytes(statusFile));
				taskStatus = objectMapper.readValue(existingStatusJson, TaskStatus.class);
			} else {
				// 创建新状态
				taskStatus = new TaskStatus();
				taskStatus.taskId = taskId;
				taskStatus.inputFile = taskDir.resolve("input.md").toAbsolutePath().toString();
			}
			
			// 更新状态信息
			taskStatus.outputFilePath = outputFilePath;
			taskStatus.status = status;
			taskStatus.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

			// 存储到内存中
			mapTaskStatuses.put(taskId, taskStatus);

			// 写入更新后的状态文件
			String statusJson = objectMapper.writeValueAsString(taskStatus);
			Files.write(statusFile, statusJson.getBytes());

			String result = String.format("任务 %s 状态已记录：%s，输出文件：output.md", taskId, status);
			log.info(result);
			return new ToolExecuteResult(result);

		} catch (Exception e) {
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
