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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.alibaba.cloud.ai.example.manus.recorder.PlanExecutionRecorder;
import com.alibaba.cloud.ai.example.manus.tool.AbstractBaseTool;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;
import com.alibaba.cloud.ai.example.manus.tool.filesystem.UnifiedDirectoryManager;
import com.alibaba.cloud.ai.example.manus.workflow.SummaryWorkflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.openai.api.OpenAiApi;

/**
 * 内部存储内容获取工具，专门用于智能内容提取和结构化输出 支持AI智能分析和数据提取功能
 */
public class InnerStorageContentTool extends AbstractBaseTool<InnerStorageContentTool.InnerStorageContentInput> {

	private static final Logger log = LoggerFactory.getLogger(InnerStorageContentTool.class);

	/**
	 * 内部存储内容获取输入类
	 */
	public static class InnerStorageContentInput {

		private String action;

		@com.fasterxml.jackson.annotation.JsonProperty("file_name")
		private String fileName;

		@com.fasterxml.jackson.annotation.JsonProperty("folder_name")
		private String folderName;

		@com.fasterxml.jackson.annotation.JsonProperty("query_key")
		private String queryKey;

		private List<String> columns;

		@com.fasterxml.jackson.annotation.JsonProperty("start_line")
		private Integer startLine;

		@com.fasterxml.jackson.annotation.JsonProperty("end_line")
		private Integer endLine;

		public InnerStorageContentInput() {
		}

		public String getAction() {
			return action;
		}

		public void setAction(String action) {
			this.action = action;
		}

		public String getFileName() {
			return fileName;
		}

		public void setFileName(String fileName) {
			this.fileName = fileName;
		}

		public String getFolderName() {
			return folderName;
		}

		public void setFolderName(String folderName) {
			this.folderName = folderName;
		}

		public String getQueryKey() {
			return queryKey;
		}

		public void setQueryKey(String queryKey) {
			this.queryKey = queryKey;
		}

		public List<String> getColumns() {
			return columns;
		}

		public void setColumns(List<String> columns) {
			this.columns = columns;
		}

		public Integer getStartLine() {
			return startLine;
		}

		public void setStartLine(Integer startLine) {
			this.startLine = startLine;
		}

		public Integer getEndLine() {
			return endLine;
		}

		public void setEndLine(Integer endLine) {
			this.endLine = endLine;
		}

	}

	private final UnifiedDirectoryManager directoryManager;

	private final SummaryWorkflow summaryWorkflow;

	private final PlanExecutionRecorder planExecutionRecorder;

	public InnerStorageContentTool(UnifiedDirectoryManager directoryManager, SummaryWorkflow summaryWorkflow,
			PlanExecutionRecorder planExecutionRecorder) {
		this.directoryManager = directoryManager;
		this.summaryWorkflow = summaryWorkflow;
		this.planExecutionRecorder = planExecutionRecorder;
	}

	private static final String TOOL_NAME = "inner_storage_content_tool";

	private static final String TOOL_DESCRIPTION = """
			内部存储内容获取工具，专门用于智能内容提取和结构化输出。
			智能内容提取模式：根据文件名获取详细内容，**必须提供** query_key 和 columns 参数进行智能提取和结构化输出

			支持两种操作模式：
			1. get_content: 从单个文件获取内容（精确文件名匹配或相对路径）
			2. get_folder_content: 从指定文件夹下的所有文件获取内容
			""";

	private static final String PARAMETERS = """
			{
				"oneOf": [
					{
						"type": "object",
						"properties": {
							"action": {
								"type": "string",
								"const": "get_content",
								"description": "从单个文件获取内容"
							},
							"file_name": {
								"type": "string",
								"description": "文件名（带扩展名）或相对路径，支持精确匹配"
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
					},
					{
						"type": "object",
						"properties": {
							"action": {
								"type": "string",
								"const": "get_folder_content",
								"description": "从指定文件夹下的所有文件获取内容"
							},
							"folder_name": {
								"type": "string",
								"description": "文件夹名称或相对路径"
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
						"required": ["action", "folder_name", "query_key", "columns"],
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
	public Class<InnerStorageContentInput> getInputType() {
		return InnerStorageContentInput.class;
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

	/**
	 * 执行内部存储内容获取操作
	 */
	@Override
	public ToolExecuteResult run(InnerStorageContentInput input) {
		log.info("InnerStorageContentTool input: action={}, fileName={}, folderName={}, queryKey={}, columns={}",
				input.getAction(), input.getFileName(), input.getFolderName(), input.getQueryKey(), input.getColumns());
		try {
			String action = input.getAction();
			if (action == null) {
				return new ToolExecuteResult("错误：action参数是必需的");
			}

			return switch (action) {
				case "get_content" -> getStoredContent(input.getFileName(), input.getQueryKey(), input.getColumns());
				case "get_folder_content" ->
					getFolderContent(input.getFolderName(), input.getQueryKey(), input.getColumns());
				default -> new ToolExecuteResult("错误：不支持的操作类型 '" + action + "'。支持的操作：get_content, get_folder_content");
			};
		}
		catch (Exception e) {
			log.error("InnerStorageContentTool执行失败", e);
			return new ToolExecuteResult("工具执行失败: " + e.getMessage());
		}
	}

	/**
	 * 根据文件名获取存储的内容，支持AI智能提取和结构化输出
	 */
	private ToolExecuteResult getStoredContent(String fileName, String queryKey, List<String> columns) {
		if (fileName == null || fileName.trim().isEmpty()) {
			return new ToolExecuteResult("错误：file_name参数是必需的");
		}
		if (queryKey == null || queryKey.trim().isEmpty()) {
			return new ToolExecuteResult("错误：query_key参数是必需的，用于指定要提取的内容关键词");
		}
		if (columns == null || columns.isEmpty()) {
			return new ToolExecuteResult("错误：columns参数是必需的，用于指定返回结果的结构化列名");
		}
		try {
			Path planDir = directoryManager.getRootPlanDirectory(rootPlanId);
			Path targetFile = null;

			// 首先尝试精确的相对路径匹配
			if (fileName.contains("/")) {
				Path exactPath = planDir.resolve(fileName);
				if (Files.exists(exactPath) && Files.isRegularFile(exactPath)) {
					targetFile = exactPath;
				}
			}
			else {
				// 如果没有路径分隔符，则在根目录下精确匹配文件名
				List<Path> files = Files.list(planDir).filter(Files::isRegularFile).toList();
				for (Path filePath : files) {
					if (filePath.getFileName().toString().equals(fileName)) {
						targetFile = filePath;
						break;
					}
				}
			}

			if (targetFile == null) {
				return new ToolExecuteResult("未找到文件 '" + fileName + "'。请提供精确的文件名或相对路径。");
			}

			String fileContent = Files.readString(targetFile);
			String actualFileName = planDir.relativize(targetFile).toString();

			log.info("委托给 SummaryWorkflow 处理文件内容提取：文件={}, 查询关键词={}", actualFileName, queryKey);
			Long thinkActRecordId = getCurrentThinkActRecordId();
			String terminateColumnsString = String.join(",", columns);
			String result = summaryWorkflow
				.executeSummaryWorkflow(rootPlanId, actualFileName, fileContent, queryKey, thinkActRecordId,
						terminateColumnsString)
				.get();
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

	/**
	 * 从指定文件夹下的所有文件中获取信息
	 */
	private ToolExecuteResult getFolderContent(String folderName, String queryKey, List<String> columns) {
		if (folderName == null || folderName.trim().isEmpty()) {
			return new ToolExecuteResult("错误：folder_name参数是必需的");
		}
		if (queryKey == null || queryKey.trim().isEmpty()) {
			return new ToolExecuteResult("错误：query_key参数是必需的，用于指定要提取的内容关键词");
		}
		if (columns == null || columns.isEmpty()) {
			return new ToolExecuteResult("错误：columns参数是必需的，用于指定返回结果的结构化列名");
		}
		try {
			Path planDir = directoryManager.getRootPlanDirectory(rootPlanId);
			Path targetFolder = planDir.resolve(folderName);

			if (!Files.exists(targetFolder)) {
				return new ToolExecuteResult("文件夹 '" + folderName + "' 不存在。");
			}

			if (!Files.isDirectory(targetFolder)) {
				return new ToolExecuteResult("'" + folderName + "' 不是一个文件夹。");
			}

			// 获取文件夹下的所有文件
			List<Path> files = Files.list(targetFolder).filter(Files::isRegularFile).toList();

			if (files.isEmpty()) {
				return new ToolExecuteResult("文件夹 '" + folderName + "' 中没有文件。");
			}

			// 合并所有文件内容
			StringBuilder combinedContent = new StringBuilder();
			for (Path file : files) {
				String relativePath = planDir.relativize(file).toString();
				combinedContent.append("=== 文件: ").append(relativePath).append(" ===\n");
				combinedContent.append(Files.readString(file));
				combinedContent.append("\n\n");
			}

			log.info("委托给 SummaryWorkflow 处理文件夹内容提取：文件夹={}, 文件数量={}, 查询关键词={}", folderName, files.size(), queryKey);

			Long thinkActRecordId = getCurrentThinkActRecordId();
			String terminateColumnsString = String.join(",", columns);
			String result = summaryWorkflow
				.executeSummaryWorkflow(rootPlanId, folderName, combinedContent.toString(), queryKey, thinkActRecordId,
						terminateColumnsString)
				.get();
			return new ToolExecuteResult(result);

		}
		catch (IOException e) {
			log.error("获取文件夹内容失败", e);
			return new ToolExecuteResult("获取文件夹内容失败: " + e.getMessage());
		}
		catch (Exception e) {
			log.error("SummaryWorkflow 执行失败", e);
			return new ToolExecuteResult("内容处理失败: " + e.getMessage());
		}
	}

	/**
	 * 获取当前的 think-act 记录ID
	 * @return 当前 think-act 记录ID，如果没有则返回 null
	 */
	private Long getCurrentThinkActRecordId() {
		try {
			Long thinkActRecordId = planExecutionRecorder.getCurrentThinkActRecordId(currentPlanId, rootPlanId);
			if (thinkActRecordId != null) {
				log.info("当前 think-act 记录ID: {}", thinkActRecordId);
				return thinkActRecordId;
			}
			else {
				log.warn("当前没有 think-act 记录ID");
			}
		}
		catch (Exception e) {
			log.warn("Failed to get current think-act record ID: {}", e.getMessage());
		}

		return null;
	}

	@Override
	public String getCurrentToolStateString() {
		try {
			StringBuilder sb = new StringBuilder();
			sb.append("InnerStorageContent 当前状态:\n");
			sb.append("- 存储根目录: ").append(directoryManager.getRootPlanDirectory(rootPlanId)).append("\n");
			Path planDir = directoryManager.getRootPlanDirectory(rootPlanId);
			List<Path> files = Files.exists(planDir) ? Files.list(planDir).filter(Files::isRegularFile).toList()
					: List.of();
			if (files.isEmpty()) {
				sb.append("- 内部文件: 无\n");
			}
			else {
				sb.append("- 内部文件 (").append(files.size()).append("个)\n");
			}
			return sb.toString();
		}
		catch (Exception e) {
			log.error("获取工具状态失败", e);
			return "InnerStorageContent 状态获取失败: " + e.getMessage();
		}
	}

	@Override
	public void cleanup(String planId) {
		// 内容获取工具不需要执行清理操作
		log.info("InnerStorageContentTool cleanup for plan: {}", planId);
	}

}
