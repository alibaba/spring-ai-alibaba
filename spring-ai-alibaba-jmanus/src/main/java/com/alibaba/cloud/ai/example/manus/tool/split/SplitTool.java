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
package com.alibaba.cloud.ai.example.manus.tool.split;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

import com.alibaba.cloud.ai.example.manus.tool.ToolCallBiFunctionDef;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;
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
public class SplitTool implements ToolCallBiFunctionDef {

	private static final Logger log = LoggerFactory.getLogger(SplitTool.class);

	private static final String TOOL_NAME = "split_tool";

	private static final String TOOL_DESCRIPTION = """
			数据分割工具，用于MapReduce流程中的数据准备阶段。
			支持的操作：
			- split_data: 自动完成验证文件存在性并进行数据分割处理，支持CSV、TSV、TXT等文本格式的数据文件。
			  输出目录采用map+taskId模式，每个任务都有独立的目录和状态跟踪文件。
			- get_undispatched_task: 获取一个未分配的任务信息，用于任务调度和管理。
			- mark_task_success: 标记指定任务为成功状态，用于任务完成确认和状态管理。
			
			任务管理特性：
			- 自动生成递增的任务ID
			- 在map+taskId目录中创建task_status.log文件跟踪任务状态
			- 支持任务状态查询和管理
			- 支持手动标记任务完成状态
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
			        },
			        {
			            "type": "object",
			            "properties": {
			                "action": {
			                    "type": "string",
			                    "const": "get_undispatched_task"
			                }
			            },
			            "required": ["action"],
			            "additionalProperties": false
			        },
			        {
			            "type": "object",
			            "properties": {
			                "action": {
			                    "type": "string",
			                    "const": "mark_task_success"
			                },
			                "task_id": {
			                    "type": "string",
			                    "description": "要标记为成功的任务ID"
			                },
			                "result_summary": {
			                    "type": "string",
			                    "description": "任务完成的结果摘要（可选）"
			                }
			            },
			            "required": ["action", "task_id"],
			            "additionalProperties": false
			        }
			    ]
			}
			""";

	private String planId;

	private String lastOperationResult = "";

	private String lastProcessedFile = "";

	private List<String> splitResults = new ArrayList<>();
	
	private List<String> returnColumns = new ArrayList<>();
	
	private static int taskIdCounter = 1;
	
	private static final Map<String, TaskInfo> taskMap = new HashMap<>();

	private static final ObjectMapper objectMapper = new ObjectMapper();
	
	/**
	 * 任务信息类
	 */
	private static class TaskInfo {
		String taskId;
		String filePath;
		List<String> splitFiles;
		boolean isFinished;
		String status;
		long createdTime;
		
		TaskInfo(String taskId, String filePath) {
			this.taskId = taskId;
			this.filePath = filePath;
			this.splitFiles = new ArrayList<>();
			this.isFinished = false;
			this.status = "CREATED";
			this.createdTime = System.currentTimeMillis();
		}
	}

	public SplitTool() {
	}

	public SplitTool(String planId) {
		this.planId = planId;
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
		return FunctionToolCallback.builder(TOOL_NAME, new SplitTool())
			.description(TOOL_DESCRIPTION)
			.inputSchema(PARAMETERS)
			.inputType(String.class)
			.build();
	}

	public static FunctionToolCallback<String, ToolExecuteResult> getFunctionToolCallback(String planId) {
		return FunctionToolCallback.builder(TOOL_NAME, new SplitTool(planId))
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
				case "get_undispatched_task" -> {
					yield getUndispatchedTask();
				}
				case "mark_task_success" -> {
					String taskId = (String) toolInputMap.get("task_id");
					String resultSummary = (String) toolInputMap.get("result_summary");
					
					if (taskId == null) {
						yield new ToolExecuteResult("错误：task_id参数是必需的");
					}
					
					yield markTaskSuccess(taskId, resultSummary);
				}
				default -> new ToolExecuteResult("未知操作: " + action + "。支持的操作: split_data, get_undispatched_task, mark_task_success");
			};

		}
		catch (Exception e) {
			log.error("SplitTool执行失败", e);
			return new ToolExecuteResult("工具执行失败: " + e.getMessage());
		}
	}

	/**
	 * 处理文件或目录的完整流程：验证存在性 -> 分割数据 -> 管理任务
	 */
	private ToolExecuteResult processFileOrDirectory(String filePath) {
		StringBuilder finalResult = new StringBuilder();

		try {
			// 生成任务ID
			String taskId = generateTaskId();
			TaskInfo taskInfo = new TaskInfo(taskId, filePath);
			taskMap.put(taskId, taskInfo);
			
			// 步骤1: 验证文件或文件夹存在性
			finalResult.append("=== 步骤1: 验证文件存在性 ===\n");
			Path path = Paths.get(filePath);
			if (!Files.exists(path)) {
				taskInfo.status = "FAILED";
				taskInfo.isFinished = true;
				updateTaskStatus(taskId, "FAILED");
				return new ToolExecuteResult("错误：文件或目录不存在: " + filePath);
			}

			boolean isFile = Files.isRegularFile(path);
			boolean isDirectory = Files.isDirectory(path);

			finalResult.append("- 任务ID: ").append(taskId).append("\n");
			finalResult.append("- 路径: ").append(filePath).append("\n");
			finalResult.append("- 类型: ").append(isFile ? "文件" : (isDirectory ? "文件夹" : "其他")).append("\n");

			if (isFile) {
				long size = Files.size(path);
				finalResult.append("- 文件大小: ").append(formatFileSize(size)).append("\n");
			}
			else if (isDirectory) {
				long fileCount = Files.list(path).count();
				finalResult.append("- 包含文件数: ").append(fileCount).append("\n");
			}
			finalResult.append("\n");

			// 步骤2: 执行数据分割
			finalResult.append("=== 步骤2: 执行数据分割 ===\n");
			taskInfo.status = "PROCESSING";
			updateTaskStatus(taskId, "PROCESSING");

			// 确定输出目录 - 使用map+taskId模式
			Path outputPath = Paths.get("split_output", "map" + taskId);
			Files.createDirectories(outputPath);
			finalResult.append("- 输出目录: ").append(outputPath.toString()).append("\n");

			List<String> allSplitFiles = new ArrayList<>();
			int totalProcessedLines = 0;

			if (isFile && isTextFile(filePath)) {
				// 处理单个文件
				SplitResult result = splitSingleFile(path, null, 1000, outputPath, null);
				allSplitFiles.addAll(result.splitFiles);
				totalProcessedLines += result.totalLines;

			}
			else if (isDirectory) {
				// 处理文件夹中的所有文本文件
				List<Path> textFiles = Files.list(path)
					.filter(Files::isRegularFile)
					.filter(p -> isTextFile(p.toString()))
					.collect(Collectors.toList());

				for (Path file : textFiles) {
					SplitResult result = splitSingleFile(file, null, 1000, outputPath, null);
					allSplitFiles.addAll(result.splitFiles);
					totalProcessedLines += result.totalLines;
				}
			}

			finalResult.append("- 总处理行数: ").append(totalProcessedLines).append("\n");
			finalResult.append("- 生成分割文件数: ").append(allSplitFiles.size()).append("\n");
			finalResult.append("- 分割文件列表:\n");
			for (String file : allSplitFiles) {
				finalResult.append("  ").append(file).append("\n");
			}

			// 更新任务信息
			taskInfo.splitFiles = allSplitFiles;
			taskInfo.status = "COMPLETED";
			taskInfo.isFinished = true;
			updateTaskStatus(taskId, "COMPLETED");

			splitResults = allSplitFiles;
			lastOperationResult = finalResult.toString();
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

		List<String> splitFiles;

		int totalLines;

		SplitResult(List<String> splitFiles, int totalLines) {
			this.splitFiles = splitFiles;
			this.totalLines = totalLines;
		}

	}

	/**
	 * 分割单个文件
	 */
	private SplitResult splitSingleFile(Path filePath, String headers, int splitSize, Path outputPath, String delimiter)
			throws IOException {
		List<String> splitFiles = new ArrayList<>();
		String fileName = filePath.getFileName().toString();
		String baseName = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
		String extension = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf('.')) : ".txt";

		int totalLineCount = 0;
		try (BufferedReader reader = Files.newBufferedReader(filePath)) {
			String line;
			int lineCount = 0;
			int fileIndex = 1;
			BufferedWriter writer = null;
			String currentOutputFile = null;

			while ((line = reader.readLine()) != null) {
				// 创建新的分割文件
				if (lineCount % splitSize == 0) {
					if (writer != null) {
						writer.close();
					}

					currentOutputFile = outputPath.resolve(baseName + "_part" + fileIndex + extension).toString();
					splitFiles.add(currentOutputFile);
					writer = Files.newBufferedWriter(Paths.get(currentOutputFile));

					// 如果有头部信息，每个文件都写入头部
					if (headers != null && !headers.trim().isEmpty() && totalLineCount > 0) {
						writer.write(headers);
						writer.newLine();
					}

					fileIndex++;
				}

				writer.write(line);
				writer.newLine();
				lineCount++;
				totalLineCount++;
			}

			if (writer != null) {
				writer.close();
			}
		}

		return new SplitResult(splitFiles, totalLineCount);
	}

	/**
	 * 判断是否为文本文件
	 */
	private boolean isTextFile(String fileName) {
		String lowercaseFileName = fileName.toLowerCase();
		return lowercaseFileName.endsWith(".csv") || lowercaseFileName.endsWith(".tsv")
				|| lowercaseFileName.endsWith(".txt") || lowercaseFileName.endsWith(".dat")
				|| lowercaseFileName.endsWith(".log");
	}

	/**
	 * 格式化文件大小
	 */
	private String formatFileSize(long size) {
		if (size < 1024)
			return size + " B";
		if (size < 1024 * 1024)
			return String.format("%.1f KB", size / 1024.0);
		if (size < 1024 * 1024 * 1024)
			return String.format("%.1f MB", size / (1024.0 * 1024.0));
		return String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
	}

	@Override
	public String getCurrentToolStateString() {
		StringBuilder sb = new StringBuilder();
		sb.append("SplitTool 当前状态:\n");
		sb.append("- Plan ID: ").append(planId != null ? planId : "未设置").append("\n");
		sb.append("- 最后处理文件: ").append(lastProcessedFile.isEmpty() ? "无" : lastProcessedFile).append("\n");
		sb.append("- 最后操作结果: ").append(lastOperationResult.isEmpty() ? "无" : "已完成").append("\n");
		sb.append("- 分割文件数: ").append(splitResults.size()).append("\n");

		return sb.toString();
	}

	@Override
	public void cleanup(String planId) {
		// 清理资源
		splitResults.clear();
		lastOperationResult = "";
		lastProcessedFile = "";
		log.info("SplitTool cleanup completed for planId: {}", planId);
	}

	@Override
	public ToolExecuteResult apply(String s, ToolContext toolContext) {
		return run(s);
	}

	/**
	 * 生成任务ID
	 */
	private synchronized String generateTaskId() {
		return String.valueOf(taskIdCounter++);
	}
	
	/**
	 * 更新任务状态到文件
	 */
	private void updateTaskStatus(String taskId, String status) {
		try {
			Path taskDir = Paths.get("split_output", "map" + taskId);
			Files.createDirectories(taskDir);
			
			Path statusFile = taskDir.resolve("task_status.log");
			String statusContent = String.format("TaskID: %s%nStatus: %s%nTimestamp: %s%n", 
				taskId, status, new java.util.Date());
			
			Files.writeString(statusFile, statusContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			log.info("Updated task status: {} -> {}", taskId, status);
		} catch (IOException e) {
			log.error("Failed to update task status for taskId: {}", taskId, e);
		}
	}
	
	/**
	 * 获取未分配的任务
	 */
	private ToolExecuteResult getUndispatchedTask() {
		try {
			// 查找未完成的任务
			TaskInfo undispatchedTask = taskMap.values().stream()
				.filter(task -> !task.isFinished)
				.findFirst()
				.orElse(null);
			
			if (undispatchedTask == null) {
				return new ToolExecuteResult("没有找到未分配的任务");
			}
			
			StringBuilder result = new StringBuilder();
			result.append("=== 未分配任务信息 ===\n");
			result.append("- 任务ID: ").append(undispatchedTask.taskId).append("\n");
			result.append("- 文件路径: ").append(undispatchedTask.filePath).append("\n");
			result.append("- 状态: ").append(undispatchedTask.status).append("\n");
			result.append("- 创建时间: ").append(new java.util.Date(undispatchedTask.createdTime)).append("\n");
			result.append("- 分割文件数: ").append(undispatchedTask.splitFiles.size()).append("\n");
			
			if (!undispatchedTask.splitFiles.isEmpty()) {
				result.append("- 分割文件列表:\n");
				for (String file : undispatchedTask.splitFiles) {
					result.append("  ").append(file).append("\n");
				}
			}
			
			// 如果有返回列要求，添加结构化信息
			if (!returnColumns.isEmpty()) {
				result.append("- 返回列: ").append(returnColumns).append("\n");
			}
			
			return new ToolExecuteResult(result.toString());
			
		} catch (Exception e) {
			log.error("获取未分配任务失败", e);
			return new ToolExecuteResult("获取未分配任务失败: " + e.getMessage());
		}
	}

	/**
	 * 标记任务为成功状态
	 */
	private ToolExecuteResult markTaskSuccess(String taskId, String resultSummary) {
		try {
			// 检查任务是否存在
			TaskInfo taskInfo = taskMap.get(taskId);
			if (taskInfo == null) {
				return new ToolExecuteResult("错误：任务ID不存在: " + taskId);
			}
			
			// 更新任务状态
			taskInfo.status = "SUCCESS";
			taskInfo.isFinished = true;
			
			// 更新状态文件
			updateTaskStatusWithSummary(taskId, "SUCCESS", resultSummary);
			
			StringBuilder result = new StringBuilder();
			result.append("=== 任务标记成功 ===\n");
			result.append("- 任务ID: ").append(taskId).append("\n");
			result.append("- 原状态: ").append("COMPLETED -> SUCCESS").append("\n");
			result.append("- 文件路径: ").append(taskInfo.filePath).append("\n");
			result.append("- 分割文件数: ").append(taskInfo.splitFiles.size()).append("\n");
			result.append("- 更新时间: ").append(new java.util.Date()).append("\n");
			
			if (resultSummary != null && !resultSummary.trim().isEmpty()) {
				result.append("- 结果摘要: ").append(resultSummary).append("\n");
			}
			
			if (!taskInfo.splitFiles.isEmpty()) {
				result.append("- 相关文件:\n");
				for (String file : taskInfo.splitFiles) {
					result.append("  ").append(file).append("\n");
				}
			}
			
			log.info("Task {} marked as SUCCESS by LLM", taskId);
			return new ToolExecuteResult(result.toString());
			
		} catch (Exception e) {
			log.error("标记任务成功失败", e);
			return new ToolExecuteResult("标记任务成功失败: " + e.getMessage());
		}
	}
	
	/**
	 * 更新任务状态到文件（带摘要）
	 */
	private void updateTaskStatusWithSummary(String taskId, String status, String summary) {
		try {
			Path taskDir = Paths.get("split_output", "map" + taskId);
			Files.createDirectories(taskDir);
			
			Path statusFile = taskDir.resolve("task_status.log");
			StringBuilder statusContent = new StringBuilder();
			statusContent.append(String.format("TaskID: %s%n", taskId));
			statusContent.append(String.format("Status: %s%n", status));
			statusContent.append(String.format("Timestamp: %s%n", new java.util.Date()));
			
			if (summary != null && !summary.trim().isEmpty()) {
				statusContent.append(String.format("Summary: %s%n", summary));
			}
			
			Files.writeString(statusFile, statusContent.toString(), 
				StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			log.info("Updated task status with summary: {} -> {}", taskId, status);
		} catch (IOException e) {
			log.error("Failed to update task status with summary for taskId: {}", taskId, e);
		}
	}

}
