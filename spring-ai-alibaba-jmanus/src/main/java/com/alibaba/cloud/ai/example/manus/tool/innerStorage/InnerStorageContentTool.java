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

			支持按文件名模糊匹配。
			""";

	private static final String PARAMETERS = """
			{
				"type": "object",
				"properties": {
					"action": {
						"type": "string",
						"enum": ["get_content"],
						"description": "操作类型，目前支持 get_content"
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
		log.info("InnerStorageContentTool input: action={}, fileName={}, queryKey={}, columns={}", input.getAction(),
				input.getFileName(), input.getQueryKey(), input.getColumns());
		try {
			// Only support intelligent content extraction mode
			return getStoredContent(input.getFileName(), input.getQueryKey(), input.getColumns());
		}
		catch (Exception e) {
			log.error("InnerStorageContentTool执行失败", e);
			return new ToolExecuteResult("工具执行失败: " + e.getMessage());
		}
	}

	/**
	 * 根据文件名或索引获取存储的内容，支持AI智能提取和结构化输出
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
			String fileContent = null;
			String actualFileName = null;
			Path planDir = directoryManager.getRootPlanDirectory(rootPlanId);
			// 只做文件名模糊查找
			List<Path> files = Files.list(planDir).filter(Files::isRegularFile).toList();
			for (Path filePath : files) {
				if (filePath.getFileName().toString().contains(fileName)) {
					fileContent = Files.readString(filePath);
					actualFileName = planDir.relativize(filePath).toString();
					break;
				}
			}
			if (fileContent == null) {
				return new ToolExecuteResult("未找到文件名为 '" + fileName + "' 的内容。请使用文件名的一部分来查找内容。");
			}
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
