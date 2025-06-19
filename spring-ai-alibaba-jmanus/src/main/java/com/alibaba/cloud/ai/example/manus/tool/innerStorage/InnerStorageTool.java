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
package com.alibaba.cloud.ai.example.manus.tool.innerStorage;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Map;

import com.alibaba.cloud.ai.example.manus.tool.ToolCallBiFunctionDef;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;
import com.alibaba.cloud.ai.example.manus.workflow.SummaryWorkflow;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.function.FunctionToolCallback;

/**
 *
 * 内部存储工具，用于MapReduce流程中的中间数据管理 自动管理基于planID和Agent的目录结构，提供简化的文件操作
 * 支持智能内容管理：当返回内容过长时自动存储并返回摘要
 *
 */
public class InnerStorageTool implements ToolCallBiFunctionDef {

	private static final Logger log = LoggerFactory.getLogger(InnerStorageTool.class);

	private final InnerStorageService innerStorageService;
	private final SummaryWorkflow summaryWorkflow;

	private String planId;

	// get_lines 操作的最大行数限制
	private static final int MAX_LINES_LIMIT = 100;

	public InnerStorageTool(InnerStorageService innerStorageService, SummaryWorkflow summaryWorkflow) {
		this.innerStorageService = innerStorageService;
		this.summaryWorkflow = summaryWorkflow;
	}

	/**
	 * 测试专用构造函数
	 */
	public InnerStorageTool(InnerStorageService innerStorageService, SummaryWorkflow summaryWorkflow, String testWorkingDirectoryPath) {
		this.innerStorageService = innerStorageService;
		this.summaryWorkflow = summaryWorkflow;
		// 测试构造函数保留向后兼容性，但不再使用workingDirectoryPath参数
	}

	private static final String TOOL_NAME = "inner_storage_tool";

	private static final String TOOL_DESCRIPTION = """
			内部存储工具，Agent流程内的数据管理。
			自动管理基于planID，提供简化的文件操作：
			- append: 向特定文件文件追加内容（自动创建文件和目录）
			- replace: 替换文件中的特定文本
			- get_lines: 获取文件的指定行号范围内容（单次最多%d行）
			- get_content: 根据文件名或索引获取详细内容，**必须提供** query_key 和 columns 参数进行AI智能提取和结构化输出

			当返回内容过长时，工具会自动存储详细内容并返回摘要和内容ID，以降低上下文压力。

			""".formatted(MAX_LINES_LIMIT);

	private static final String PARAMETERS = """
			{
			    "oneOf": [
			        {
			            "type": "object",
			            "properties": {
			                "action": {
			                    "type": "string",
			                    "const": "append"
			                },
			                "file_name": {
			                    "type": "string",
			                    "description": "文件名（带扩展名）"
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
			                    "description": "文件名（带扩展名）"
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
			        },
			        {
			            "type": "object",
			            "properties": {
			                "action": {
			                    "type": "string",
			                    "const": "get_lines"
			                },
			                "file_name": {
			                    "type": "string",
			                    "description": "文件名（带扩展名）"
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
			        },		        {
		            "type": "object",
		            "properties": {
		                "action": {
		                    "type": "string",
		                    "const": "get_content"
		                },
		                "file_name": {
		                    "type": "string",
		                    "description": "文件名（带扩展名）"
		                },
		                "query_key": {
		                    "type": "string",
		                    "description": "相关问题或希望提取的内容关键词，必须提供"
		                },
		                "columns": {
		                    "type": "array",
		                    "items": {
		                        "type": "string"
		                    },
		                    "description": "返回结果的列名，用于结构化输出，必须提供。返回的结果可以是一个列表"
		                }
		            },
		            "required": ["action", "file_name", "query_key", "columns"],
		            "additionalProperties": false
		        }
			    ]
			}
			""";

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
		return "default-service-group";
	}

	public static OpenAiApi.FunctionTool getToolDefinition() {
		OpenAiApi.FunctionTool.Function function = new OpenAiApi.FunctionTool.Function(TOOL_DESCRIPTION, TOOL_NAME,
				PARAMETERS);
		return new OpenAiApi.FunctionTool(function);
	}

	public static FunctionToolCallback<String, ToolExecuteResult> getFunctionToolCallback(
			InnerStorageService innerStorageService, SummaryWorkflow summaryWorkflow) {
		return FunctionToolCallback.builder(TOOL_NAME, new InnerStorageTool(innerStorageService, summaryWorkflow))
			.description(TOOL_DESCRIPTION)
			.inputSchema(PARAMETERS)
			.inputType(String.class)
			.build();
	}

	public static FunctionToolCallback<String, ToolExecuteResult> getFunctionToolCallback(String planId,
			InnerStorageService innerStorageService, SummaryWorkflow summaryWorkflow) {
		InnerStorageTool tool = new InnerStorageTool(innerStorageService, summaryWorkflow);
		tool.setPlanId(planId);
		return FunctionToolCallback.builder(TOOL_NAME, tool)
			.description(TOOL_DESCRIPTION)
			.inputSchema(PARAMETERS)
			.inputType(String.class)
			.build();
	}

	public ToolExecuteResult run(String toolInput) {
		log.info("InnerStorageTool toolInput: {}", toolInput);
		try {
			Map<String, Object> toolInputMap = new ObjectMapper().readValue(toolInput,
					new TypeReference<Map<String, Object>>() {
					});

			String action = (String) toolInputMap.get("action");
			if (action == null) {
				return new ToolExecuteResult("错误：action参数是必需的");
			}

			return switch (action) {
				case "append" -> {
					String fileName = (String) toolInputMap.get("file_name");
					String content = (String) toolInputMap.get("content");
					yield appendToFile(fileName, content);
				}
				case "replace" -> {
					String fileName = (String) toolInputMap.get("file_name");
					String sourceText = (String) toolInputMap.get("source_text");
					String targetText = (String) toolInputMap.get("target_text");
					yield replaceInFile(fileName, sourceText, targetText);
				}
				case "get_lines" -> {
					String fileName = (String) toolInputMap.get("file_name");
					Integer startLine = (Integer) toolInputMap.get("start_line");
					Integer endLine = (Integer) toolInputMap.get("end_line");
					yield getFileLines(fileName, startLine, endLine);
				}
				case "get_content" -> {
					String fileName = (String) toolInputMap.get("file_name");
					String queryKey = (String) toolInputMap.get("query_key");
					@SuppressWarnings("unchecked")
					List<String> columns = (List<String>) toolInputMap.get("columns");
					yield getStoredContent(fileName, queryKey, columns);
				}
				default -> new ToolExecuteResult("未知操作: " + action + "。支持的操作: append, replace, get_lines, get_content");
			};

		}
		catch (Exception e) {
			log.error("InnerStorageTool执行失败", e);
			return new ToolExecuteResult("工具执行失败: " + e.getMessage());
		}
	}

	/**
	 * 追加内容到文件
	 */
	private ToolExecuteResult appendToFile(String fileName, String content) {
		try {
			if (fileName == null || fileName.trim().isEmpty()) {
				return new ToolExecuteResult("错误：file_name参数是必需的");
			}
			if (content == null) {
				content = "";
			}
		// 确保计划目录存在 - 直接使用计划目录，不使用 agent 子目录
		Path planDir = innerStorageService.getPlanDirectory(planId);
		innerStorageService.ensureDirectoryExists(planDir);

		// 获取文件路径并追加内容 - 直接在计划目录下创建文件
		Path filePath = planDir.resolve(fileName);

			// 如果文件不存在，创建新文件
			if (!Files.exists(filePath)) {
				Files.writeString(filePath, content);
				return new ToolExecuteResult(String.format("文件创建成功并添加内容: %s", fileName));
			}
			else {
				// 追加内容（添加换行符）
				Files.writeString(filePath, "\n" + content, StandardOpenOption.APPEND);
				return new ToolExecuteResult(String.format("内容追加成功: %s", fileName));
			}

		}
		catch (IOException e) {
			log.error("追加文件失败", e);
			return new ToolExecuteResult("追加文件失败: " + e.getMessage());
		}
	}

	/**
	 * 替换文件中的文本
	 */
	private ToolExecuteResult replaceInFile(String fileName, String sourceText, String targetText) {
		try {
			if (fileName == null || fileName.trim().isEmpty()) {
				return new ToolExecuteResult("错误：file_name参数是必需的");
			}
			if (sourceText == null || targetText == null) {			return new ToolExecuteResult("错误：source_text和target_text参数都是必需的");
		}

		Path planDir = innerStorageService.getPlanDirectory(planId);
		Path filePath = planDir.resolve(fileName);

		if (!Files.exists(filePath)) {
				return new ToolExecuteResult("错误：文件不存在: " + fileName);
			}

			String content = Files.readString(filePath);
			String newContent = content.replace(sourceText, targetText);
			Files.writeString(filePath, newContent);

			return new ToolExecuteResult(String.format("文本替换成功: %s", fileName));

		}
		catch (IOException e) {
			log.error("替换文件文本失败", e);
			return new ToolExecuteResult("替换文件文本失败: " + e.getMessage());
		}
	}

	/**
	 * 获取文件的指定行号内容
	 */
	private ToolExecuteResult getFileLines(String fileName, Integer startLine, Integer endLine) {
		try {
			if (fileName == null || fileName.trim().isEmpty()) {			return new ToolExecuteResult("错误：file_name参数是必需的");
		}

		Path planDir = innerStorageService.getPlanDirectory(planId);
		Path filePath = planDir.resolve(fileName);

		if (!Files.exists(filePath)) {
				return new ToolExecuteResult("错误：文件不存在: " + fileName);
			}

			List<String> lines = Files.readAllLines(filePath);

			if (lines.isEmpty()) {
				return new ToolExecuteResult("文件为空");
			}

			// 设置默认值
			int start = (startLine != null && startLine > 0) ? startLine - 1 : 0;
			int end = (endLine != null && endLine > 0) ? Math.min(endLine, lines.size()) : lines.size();

			// 验证范围
			if (start >= lines.size()) {
				return new ToolExecuteResult("起始行号超出文件范围");
			}

			if (start >= end) {
				return new ToolExecuteResult("起始行号不能大于或等于结束行号");
			}

			// 检查行数限制
			int requestedLines = end - start;
			if (requestedLines > MAX_LINES_LIMIT) {
				return new ToolExecuteResult(String.format("请求的行数 %d 超过最大限制 %d 行。请减少行数范围或使用多次调用获取内容。", 
					requestedLines, MAX_LINES_LIMIT));
			}

			StringBuilder result = new StringBuilder();
			result.append(String.format("文件: %s (第%d-%d行，共%d行)\n", fileName, start + 1, end, lines.size()));
			result.append("=".repeat(50)).append("\n");

			for (int i = start; i < end; i++) {
				result.append(String.format("%4d: %s\n", i + 1, lines.get(i)));
			}

			return new ToolExecuteResult(result.toString());

		}
		catch (IOException e) {
			log.error("读取文件行失败", e);
			return new ToolExecuteResult("读取文件行失败: " + e.getMessage());
		}
	}




	/**
	 * 根据文件名或索引获取存储的内容，支持AI智能提取和结构化输出
	 * 现在委托给 SummaryWorkflow 处理
	 */
	private ToolExecuteResult getStoredContent(String fileName, String queryKey, List<String> columns) {
		if (fileName == null || fileName.trim().isEmpty()) {
			return new ToolExecuteResult("错误：file_name参数是必需的");
		}

		// 严格要求queryKey和columns参数 - 不提供向后兼容
		if (queryKey == null || queryKey.trim().isEmpty()) {
			return new ToolExecuteResult("错误：query_key参数是必需的，用于指定要提取的内容关键词");
		}
		if (columns == null || columns.isEmpty()) {
			return new ToolExecuteResult("错误：columns参数是必需的，用于指定返回结果的结构化列名");
		}

		try {
			String fileContent = null;
			String actualFileName = null;
			
			// 尝试按数字索引获取文件内容
			try {
				int index = Integer.parseInt(fileName) - 1; // 转换为0基索引
				List<InnerStorageService.FileInfo> files = innerStorageService.getDirectoryFiles(planId);

				if (index >= 0 && index < files.size()) {
					InnerStorageService.FileInfo file = files.get(index);
					Path planDir = innerStorageService.getPlanDirectory(planId);
					Path filePath = planDir.resolve(file.getRelativePath());

					if (Files.exists(filePath)) {
						fileContent = Files.readString(filePath);
						actualFileName = file.getRelativePath();
					}
				}
			}
			catch (NumberFormatException e) {
				// 不是数字，尝试按文件名查找
				List<InnerStorageService.FileInfo> files = innerStorageService.getDirectoryFiles(planId);
				for (InnerStorageService.FileInfo file : files) {
					if (file.getRelativePath().contains(fileName)) {
						Path planDir = innerStorageService.getPlanDirectory(planId);
						Path filePath = planDir.resolve(file.getRelativePath());

						if (Files.exists(filePath)) {
							fileContent = Files.readString(filePath);
							actualFileName = file.getRelativePath();
							break;
						}
					}
				}
			}

			if (fileContent == null) {
				return new ToolExecuteResult("未找到文件名为 '" + fileName + "' 的内容。" +
					"请使用文件索引号（如 '1', '2'）或文件名的一部分来查找内容。");
			}

			// 委托给 SummaryWorkflow 进行处理
			log.info("委托给 SummaryWorkflow 处理文件内容提取：文件={}, 查询关键词={}, 输出列={}", 
					actualFileName, queryKey, columns);
			
			String result = summaryWorkflow.executeSummaryWorkflow(actualFileName, fileContent, queryKey, columns)
					.get(); // 阻塞等待结果
			
			return new ToolExecuteResult(result);

		}
		catch (IOException e) {
			log.error("获取存储内容失败", e);
			return new ToolExecuteResult("获取内容失败: " + e.getMessage());
		}
		catch (Exception e) {
			log.error("SummaryWorkflow 执行失败", e);
			return new ToolExecuteResult("内容处理失败: " + e.getMessage());
		}
	}

	@Override
	public String getCurrentToolStateString() {
		try {
			StringBuilder sb = new StringBuilder();
			sb.append("InnerStorage 当前状态:\n");
			sb.append("- Plan ID: ").append(planId != null ? planId : "未设置").append("\n");
			sb.append("- 存储根目录: ").append(innerStorageService.getInnerStorageRoot()).append("\n");

			// 获取当前目录下的所有文件信息
			List<InnerStorageService.FileInfo> files = innerStorageService.getDirectoryFiles(planId);

			if (files.isEmpty()) {
				sb.append("- 内部文件: 无\n");
			}
			else {
				sb.append("- 内部文件 (").append(files.size()).append("个):\n");
				for (InnerStorageService.FileInfo file : files) {
					sb.append("  ").append(file.toString()).append("\n");
				}
			}

			return sb.toString();
		}
		catch (Exception e) {
			log.error("获取工具状态失败", e);
			return "InnerStorage 状态获取失败: " + e.getMessage();
		}
	}

	@Override
	public void cleanup(String planId) {
		if (planId != null) {
			log.info("Cleaning up inner storage for plan: {}", planId);
			innerStorageService.cleanupPlan(planId);
		}
	}

	@Override
	public ToolExecuteResult apply(String s, ToolContext toolContext) {
		return run(s);
	}

}
