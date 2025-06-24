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
public class SplitTool implements ToolCallBiFunctionDef {

	private static final Logger log = LoggerFactory.getLogger(SplitTool.class);

	private static final String TOOL_NAME = "split_tool";

	private static final String TOOL_DESCRIPTION = """
			数据分割工具，用于MapReduce流程中的数据准备阶段和任务状态管理。
			支持的操作：
			- split_data: 自动完成验证文件存在性并进行数据分割处理，支持CSV、TSV、TXT等文本格式的数据文件。
			  输出目录采用inner_storage/{planId}/split_output模式，与InnerStorageService统一管理，每个任务都有独立的目录和状态跟踪文件。
			- record_map_output: 记录Map阶段处理完成后的输出文件，并更新对应的任务状态文件。
			  用于跟踪每个Map任务的完成状态和输出文件位置。
			
			任务管理特性：
			- 自动生成递增的任务ID
			- 在inner_storage/{planId}/map_status目录中创建任务状态文件跟踪任务状态
			- 与InnerStorageService统一存储管理
			- 通过 getSplitResults() 方法获取分割文件列表用于任务分配
			- 支持Map阶段完成后的状态记录和输出文件管理
			""";

	private static final String PARAMETERS = """
			{
			    "type": "object",
			    "properties": {
			        "action": {
			            "type": "string",
			            "enum": ["split_data", "record_map_output"]
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
			        },
			        "output_file_path": {
			            "type": "string",
			            "description": "Map阶段处理完成后的输出文件路径"
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
			    "required": ["action"],
			    "additionalProperties": false
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

	private static final ObjectMapper objectMapper = new ObjectMapper();

	public SplitTool() {
	}


	public SplitTool(ManusProperties manusProperties) {
		this.manusProperties = manusProperties;
		this.workingDirectoryPath = CodeUtils.getWorkingDirectory(manusProperties.getBaseDir());
	}

	public SplitTool(String planId, ManusProperties manusProperties) {
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
		return FunctionToolCallback.builder(TOOL_NAME, new SplitTool())
			.description(TOOL_DESCRIPTION)
			.inputSchema(PARAMETERS)
			.inputType(String.class)
			.build();
	}

	public static FunctionToolCallback<String, ToolExecuteResult> getFunctionToolCallback(String planId, ManusProperties manusProperties) {
		return FunctionToolCallback.builder(TOOL_NAME, new SplitTool(planId, manusProperties))
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
					String outputFilePath = (String) toolInputMap.get("output_file_path");
					String taskId = (String) toolInputMap.get("task_id");
					String status = (String) toolInputMap.get("status");
					
					if (outputFilePath == null) {
						yield new ToolExecuteResult("错误：output_file_path参数是必需的");
					}
					if (taskId == null) {
						yield new ToolExecuteResult("错误：task_id参数是必需的");
					}
					if (status == null) {
						yield new ToolExecuteResult("错误：status参数是必需的");
					}
					
					yield recordMapTaskOutput(outputFilePath, taskId, status);
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

			// 确定输出目录 - 存储到 inner_storage/{planId}/split_output 目录
			Path planDir = getPlanDirectory(planId);
			Path outputPath = planDir.resolve("split_output");
			ensureDirectoryExists(outputPath);

			List<String> allSplitFiles = new ArrayList<>();

			if (isFile && isTextFile(filePath)) {
				// 处理单个文件
				SplitResult result = splitSingleFile(path, null, 1000, outputPath, null);
				allSplitFiles.addAll(result.splitFiles);

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
				}
			}

			// 更新分割结果
			splitResults = allSplitFiles;
			
			// 生成简洁的返回结果
			StringBuilder result = new StringBuilder();
			result.append("切分文件成功");
			result.append("，切分为").append(allSplitFiles.size()).append("个文件");
			
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

		List<String> splitFiles;

		SplitResult(List<String> splitFiles) {
			this.splitFiles = splitFiles;
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

		return new SplitResult(splitFiles);
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
	 * 获取分割结果文件列表
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

}
