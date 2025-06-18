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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.alibaba.cloud.ai.example.manus.config.ManusProperties;
import com.alibaba.cloud.ai.example.manus.tool.ToolCallBiFunctionDef;
import com.alibaba.cloud.ai.example.manus.tool.code.CodeUtils;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;
import com.alibaba.cloud.ai.example.manus.tool.textOperator.AbstractSmartFileOperator;
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
public class InnerStorageTool extends AbstractSmartFileOperator implements ToolCallBiFunctionDef {

	private static final Logger log = LoggerFactory.getLogger(InnerStorageTool.class);

	private final String workingDirectoryPath;

	private final InnerStorageService innerStorageService;

	private String planId;

	public InnerStorageTool(InnerStorageService innerStorageService) {
		this.innerStorageService = innerStorageService;
		ManusProperties manusProperties = innerStorageService.getManusProperties();
		workingDirectoryPath = CodeUtils.getWorkingDirectory(manusProperties.getBaseDir());
	}

	/**
	 * 测试专用构造函数，直接指定工作目录路径
	 */
	public InnerStorageTool(InnerStorageService innerStorageService, String workingDirectoryPath) {
		this.innerStorageService = innerStorageService;
		this.workingDirectoryPath = workingDirectoryPath;
	}

	@Override
	protected String getWorkingDirectoryPath() {
		return workingDirectoryPath;
	}

	@Override
	protected String getCurrentPlanId() {
		return planId;
	}

	@Override
	protected InnerStorageService getInnerStorageService() {
		return innerStorageService;
	}

	private static final String TOOL_NAME = "inner_storage_tool";

	private static final String TOOL_DESCRIPTION = """
			内部存储工具，用于MapReduce流程中的中间数据管理。
			自动管理基于planID和Agent的目录结构，提供简化的文件操作：
			- append: 向文件追加内容（自动创建文件和目录）
			- replace: 替换文件中的特定文本
			- get_lines: 获取文件的指定行号范围内容
			- search: 在存储的内容中搜索关键词
			- list_contents: 列出当前任务相关的所有内容ID和摘要
			- get_content: 根据内容ID获取详细内容
			- get_description: 获取保存的详细描述内容

			当返回内容过长时，工具会自动存储详细内容并返回摘要和内容ID，以降低上下文压力。

			""";

	private static final String PARAMETERS = """
			{
			    "type": "object",
			    "properties": {
			        "action": {
			            "type": "string",
			            "description": "(required) 操作类型: 'append', 'replace', 'get_lines', 'search', 'list_contents', 'get_content', 'get_description'",
			            "enum": ["append", "replace", "get_lines", "search", "list_contents", "get_content", "get_description"]
			        },
			        "file_name": {
			            "type": "string",
			            "description": "(required for file operations) 文件名（带扩展名），不需要带目录路径，工具会自动处理目录结构"
			        },
			        "content": {
			            "type": "string",
			            "description": "(required for append) 要追加的内容"
			        },
			        "source_text": {
			            "type": "string",
			            "description": "(required for replace) 要被替换的文本"
			        },
			        "target_text": {
			            "type": "string",
			            "description": "(required for replace) 替换后的文本"
			        },
			        "start_line": {
			            "type": "integer",
			            "description": "(optional for get_lines) 起始行号，默认为1"
			        },
			        "end_line": {
			            "type": "integer",
			            "description": "(optional for get_lines) 结束行号，默认为文件末尾"
			        },
			        "keyword": {
			            "type": "string",
			            "description": "(required for search) 搜索关键词"
			        },
			        "content_id": {
			            "type": "string",
			            "description": "(required for get_content) 内容ID，用于获取特定的存储内容"
			        }
			    },
			    "required": ["action"]
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
		return "inner-storage";
	}

	public static OpenAiApi.FunctionTool getToolDefinition() {
		OpenAiApi.FunctionTool.Function function = new OpenAiApi.FunctionTool.Function(TOOL_DESCRIPTION, TOOL_NAME,
				PARAMETERS);
		return new OpenAiApi.FunctionTool(function);
	}

	public static FunctionToolCallback<String, ToolExecuteResult> getFunctionToolCallback(
			InnerStorageService innerStorageService) {
		return FunctionToolCallback.builder(TOOL_NAME, new InnerStorageTool(innerStorageService))
			.description(TOOL_DESCRIPTION)
			.inputSchema(PARAMETERS)
			.inputType(String.class)
			.build();
	}

	public static FunctionToolCallback<String, ToolExecuteResult> getFunctionToolCallback(String planId,
			InnerStorageService innerStorageService) {
		InnerStorageTool tool = new InnerStorageTool(innerStorageService);
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
					ToolExecuteResult result = appendToFile(fileName, content);
					yield processResult(result, "append", fileName);
				}
				case "replace" -> {
					String fileName = (String) toolInputMap.get("file_name");
					String sourceText = (String) toolInputMap.get("source_text");
					String targetText = (String) toolInputMap.get("target_text");
					ToolExecuteResult result = replaceInFile(fileName, sourceText, targetText);
					yield processResult(result, "replace", fileName);
				}
				case "get_lines" -> {
					String fileName = (String) toolInputMap.get("file_name");
					Integer startLine = (Integer) toolInputMap.get("start_line");
					Integer endLine = (Integer) toolInputMap.get("end_line");
					ToolExecuteResult result = getFileLines(fileName, startLine, endLine);
					yield processResult(result, "get_lines", fileName);
				}
				case "search" -> {
					String keyword = (String) toolInputMap.get("keyword");
					ToolExecuteResult result = searchContent(keyword);
					yield processResult(result, "search", null);
				}
				case "list_contents" -> {
					ToolExecuteResult result = listStoredContents();
					yield processResult(result, "list_contents", null);
				}
				case "get_content" -> {
					String contentId = (String) toolInputMap.get("content_id");
					yield getStoredContent(contentId);
				}
				case "get_description" -> {
					// 获取所有自动存储的内容作为描述
					List<InnerStorageService.FileInfo> autoStoredFiles = innerStorageService
						.searchAutoStoredFiles(workingDirectoryPath, planId, "");
					if (!autoStoredFiles.isEmpty()) {
						StringBuilder desc = new StringBuilder();
						desc.append("任务 ").append(planId).append(" 的自动存储内容概览:\n\n");
						for (int i = 0; i < autoStoredFiles.size(); i++) {
							InnerStorageService.FileInfo file = autoStoredFiles.get(i);
							desc.append(String.format("[%d] %s (%d bytes)\n", i + 1, file.getRelativePath(),
									file.getSize()));
						}
						yield new ToolExecuteResult(desc.toString());
					}
					else {
						yield new ToolExecuteResult("未找到任何自动存储的内容");
					}
				}
				case "set_agent" -> new ToolExecuteResult("错误：set_agent 操作已不再支持。Agent 应该在工具初始化时设置。");
				default -> new ToolExecuteResult("未知操作: " + action);
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

			// 确保目录存在
			String agentName = innerStorageService.getPlanAgent(planId);
			Path agentDir = innerStorageService.getAgentDirectory(workingDirectoryPath, planId, agentName);
			innerStorageService.ensureDirectoryExists(agentDir);

			// 获取文件路径并追加内容
			Path filePath = innerStorageService.getFilePath(workingDirectoryPath, planId, fileName);

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
			if (sourceText == null || targetText == null) {
				return new ToolExecuteResult("错误：source_text和target_text参数都是必需的");
			}

			Path filePath = innerStorageService.getFilePath(workingDirectoryPath, planId, fileName);

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
			if (fileName == null || fileName.trim().isEmpty()) {
				return new ToolExecuteResult("错误：file_name参数是必需的");
			}

			Path filePath = innerStorageService.getFilePath(workingDirectoryPath, planId, fileName);

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
	 * 搜索存储内容中的关键词
	 */
	private ToolExecuteResult searchContent(String keyword) {
		if (keyword == null || keyword.trim().isEmpty()) {
			return new ToolExecuteResult("错误：keyword参数是必需的");
		}

		try {
			List<InnerStorageService.FileInfo> files = innerStorageService.getDirectoryFiles(workingDirectoryPath,
					planId);
			StringBuilder searchResults = new StringBuilder();
			searchResults.append("🔍 搜索关键词: '").append(keyword).append("'\n\n");

			int foundCount = 0;
			for (InnerStorageService.FileInfo fileInfo : files) {
				Path planDir = innerStorageService.getPlanDirectory(workingDirectoryPath, planId);
				Path filePath = planDir.resolve(fileInfo.getRelativePath());

				if (Files.exists(filePath)) {
					try {
						List<String> lines = Files.readAllLines(filePath);
						List<String> matchingLines = new ArrayList<>();

						for (int i = 0; i < lines.size(); i++) {
							if (lines.get(i).toLowerCase().contains(keyword.toLowerCase())) {
								matchingLines.add(String.format("  行 %d: %s", i + 1, lines.get(i).length() > 100
										? lines.get(i).substring(0, 100) + "..." : lines.get(i)));
							}
						}

						if (!matchingLines.isEmpty()) {
							foundCount++;
							searchResults.append("📁 ").append(fileInfo.getRelativePath()).append("\n");
							for (String line : matchingLines) {
								searchResults.append(line).append("\n");
							}
							searchResults.append("\n");
						}
					}
					catch (IOException e) {
						log.warn("无法读取文件进行搜索: {}", filePath, e);
					}
				}
			}

			// 搜索自动存储的内容
			List<InnerStorageService.FileInfo> autoStoredFiles = getAutoStoredFiles();
			if (!autoStoredFiles.isEmpty()) {
				for (InnerStorageService.FileInfo file : autoStoredFiles) {
					Path planDir = innerStorageService.getPlanDirectory(workingDirectoryPath, planId);
					Path filePath = planDir.resolve(file.getRelativePath());

					if (Files.exists(filePath)) {
						try {
							String content = Files.readString(filePath);
							if (content.toLowerCase().contains(keyword.toLowerCase())) {
								foundCount++;
								searchResults.append("🤖 自动存储: ").append(file.getRelativePath()).append("\n");
								searchResults.append("  匹配内容包含关键词，使用 get_content 获取详细内容\n\n");
								break;
							}
						}
						catch (IOException e) {
							log.warn("无法读取自动存储文件进行搜索: {}", filePath, e);
						}
					}
				}
			}

			if (foundCount == 0) {
				searchResults.append("❌ 未找到包含关键词 '").append(keyword).append("' 的内容");
			}
			else {
				searchResults.insert(0, String.format("✅ 找到 %d 个匹配项\n\n", foundCount));
			}

			return new ToolExecuteResult(searchResults.toString());

		}
		catch (Exception e) {
			log.error("搜索内容失败", e);
			return new ToolExecuteResult("搜索失败: " + e.getMessage());
		}
	}

	/**
	 * 获取自动存储的文件（以 auto_ 开头的文件）
	 */
	private List<InnerStorageService.FileInfo> getAutoStoredFiles() {
		List<InnerStorageService.FileInfo> allFiles = innerStorageService.getDirectoryFiles(workingDirectoryPath,
				planId);
		return allFiles.stream()
			.filter(file -> file.getRelativePath().contains("auto_"))
			.collect(java.util.stream.Collectors.toList());
	}

	/**
	 * 列出当前任务相关的所有存储内容
	 */
	private ToolExecuteResult listStoredContents() {
		try {
			StringBuilder contentList = new StringBuilder();
			contentList.append("📋 当前任务存储内容列表\n\n");

			// 列出文件内容
			List<InnerStorageService.FileInfo> files = innerStorageService.getDirectoryFiles(workingDirectoryPath,
					planId);
			if (!files.isEmpty()) {
				contentList.append("📁 文件内容:\n");
				for (int i = 0; i < files.size(); i++) {
					InnerStorageService.FileInfo file = files.get(i);
					contentList.append(String.format("  [%d] %s (%d bytes, %s)\n", i + 1, file.getRelativePath(),
							file.getSize(), file.getLastModified()));
				}
				contentList.append("\n");
			}

			// 列出自动存储的内容
			List<InnerStorageService.FileInfo> autoStoredFiles = getAutoStoredFiles();
			if (!autoStoredFiles.isEmpty()) {
				contentList.append("🤖 自动存储的内容:\n");
				for (int i = 0; i < autoStoredFiles.size(); i++) {
					InnerStorageService.FileInfo file = autoStoredFiles.get(i);
					contentList.append(String.format("  [auto_%d] %s (%d bytes, %s)\n", i + 1, file.getRelativePath(),
							file.getSize(), file.getLastModified()));
				}
				contentList.append("\n");
			}

			if (files.isEmpty() && autoStoredFiles.isEmpty()) {
				contentList.append("❌ 当前任务没有存储的内容");
			}
			else {
				contentList.append("💡 提示:\n");
				contentList.append("  - 使用 get_lines 操作读取文件内容\n");
				contentList.append("  - 使用 get_content 操作根据ID获取内容\n");
				contentList.append("  - 使用 search 操作搜索关键词");
			}

			return new ToolExecuteResult(contentList.toString());

		}
		catch (Exception e) {
			log.error("列出存储内容失败", e);
			return new ToolExecuteResult("列出内容失败: " + e.getMessage());
		}
	}

	/**
	 * 根据内容ID获取存储的内容
	 */
	private ToolExecuteResult getStoredContent(String contentId) {
		if (contentId == null || contentId.trim().isEmpty()) {
			return new ToolExecuteResult("错误：content_id参数是必需的");
		}

		try {
			// 尝试解析内容ID
			if ("desc".equals(contentId)) {
				// 获取自动存储内容的概览作为描述
				List<InnerStorageService.FileInfo> autoStoredFiles = innerStorageService
					.searchAutoStoredFiles(workingDirectoryPath, planId, "");
				if (!autoStoredFiles.isEmpty()) {
					StringBuilder desc = new StringBuilder();
					desc.append("任务 ").append(planId).append(" 的自动存储内容详情:\n\n");
					for (InnerStorageService.FileInfo file : autoStoredFiles) {
						try {
							String content = innerStorageService.readFileContent(workingDirectoryPath, planId,
									file.getRelativePath());
							desc.append("📄 ").append(file.getRelativePath()).append(":\n");
							desc.append(content).append("\n\n");
						}
						catch (IOException e) {
							desc.append("❌ 无法读取文件: ").append(file.getRelativePath()).append("\n\n");
						}
					}
					return new ToolExecuteResult(desc.toString());
				}
				else {
					return new ToolExecuteResult("未找到任何自动存储的内容");
				}
			}

			// 尝试按数字索引获取文件内容
			try {
				int index = Integer.parseInt(contentId) - 1; // 转换为0基索引
				List<InnerStorageService.FileInfo> files = innerStorageService.getDirectoryFiles(workingDirectoryPath,
						planId);

				if (index >= 0 && index < files.size()) {
					InnerStorageService.FileInfo file = files.get(index);
					// 使用 planDirectory + relativePath 来构建完整路径
					Path planDir = innerStorageService.getPlanDirectory(workingDirectoryPath, planId);
					Path filePath = planDir.resolve(file.getRelativePath());

					if (Files.exists(filePath)) {
						String content = Files.readString(filePath);
						ToolExecuteResult result = new ToolExecuteResult(
								String.format("📁 文件: %s\n%s\n%s", file.getRelativePath(), "=".repeat(50), content));
						return processResult(result, "get_content", file.getRelativePath());
					}
				}
			}
			catch (NumberFormatException e) {
				// 不是数字，尝试按文件名查找
				List<InnerStorageService.FileInfo> files = innerStorageService.getDirectoryFiles(workingDirectoryPath,
						planId);
				for (InnerStorageService.FileInfo file : files) {
					if (file.getRelativePath().contains(contentId)) {
						Path planDir = innerStorageService.getPlanDirectory(workingDirectoryPath, planId);
						Path filePath = planDir.resolve(file.getRelativePath());

						if (Files.exists(filePath)) {
							String content = Files.readString(filePath);
							ToolExecuteResult result = new ToolExecuteResult(String.format("📁 文件: %s\n%s\n%s",
									file.getRelativePath(), "=".repeat(50), content));
							return processResult(result, "get_content", file.getRelativePath());
						}
					}
				}
			}

			return new ToolExecuteResult("未找到内容ID为 '" + contentId + "' 的内容。请使用 list_contents 查看可用的内容ID。");

		}
		catch (IOException e) {
			log.error("获取存储内容失败", e);
			return new ToolExecuteResult("获取内容失败: " + e.getMessage());
		}
	}

	@Override
	public String getCurrentToolStateString() {
		try {
			StringBuilder sb = new StringBuilder();
			sb.append("InnerStorage 当前状态:\n");
			sb.append("- Plan ID: ").append(planId != null ? planId : "未设置").append("\n");
			sb.append("- Agent: ").append(innerStorageService.getPlanAgent(planId)).append("\n");
			sb.append("- 工作目录: ").append(workingDirectoryPath).append("\n");

			// 获取当前目录下的所有文件信息
			List<InnerStorageService.FileInfo> files = innerStorageService.getDirectoryFiles(workingDirectoryPath,
					planId);

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
			innerStorageService.cleanupPlan(workingDirectoryPath, planId);
		}
	}

	@Override
	public ToolExecuteResult apply(String s, ToolContext toolContext) {
		return run(s);
	}

}
