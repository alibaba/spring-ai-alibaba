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

import com.alibaba.cloud.ai.example.manus.tool.ToolCallBiFunctionDef;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;
import com.alibaba.cloud.ai.example.manus.workflow.SummaryWorkflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.openai.api.OpenAiApi;

/**
 * 内部存储内容获取工具，专门用于智能内容提取和结构化输出
 * 支持AI智能分析和数据提取功能
 */
public class InnerStorageContentTool implements ToolCallBiFunctionDef<InnerStorageContentTool.InnerStorageContentInput> {

	private static final Logger log = LoggerFactory.getLogger(InnerStorageContentTool.class);

	/**
	 * 内部存储内容获取输入类
	 */
	public static class InnerStorageContentInput {

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

	private final InnerStorageService innerStorageService;
	private final SummaryWorkflow summaryWorkflow;
	private String planId;

	public InnerStorageContentTool(InnerStorageService innerStorageService, SummaryWorkflow summaryWorkflow) {
		this.innerStorageService = innerStorageService;
		this.summaryWorkflow = summaryWorkflow;
	}

	private static final String TOOL_NAME = "inner_storage_content_tool";

	private static final String TOOL_DESCRIPTION = """
			内部存储内容获取工具，专门用于智能内容提取和结构化输出。
			支持两种模式：
			1. 智能内容提取模式：根据文件名或索引获取详细内容，**必须提供** query_key 和 columns 参数进行AI智能提取和结构化输出
			2. 按行获取模式：根据文件名或索引获取指定行号范围的原始内容，需要提供 start_line 和 end_line 参数，单次请求最多能返回500行数据。
			
			支持按文件名模糊匹配或按索引号精确查找。

			当返回内容过长时，工具会自动存储详细内容并返回摘要和内容ID，以降低上下文压力。
			""";

	private static final String PARAMETERS = """
			{
			    "oneOf": [
			        {
			            "type": "object",
			            "properties": {
			                "file_name": {
			                    "type": "string",
			                    "description": "文件名（带扩展名）或文件索引号（如 '1', '2'）"
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
			            "required": ["file_name", "query_key", "columns"],
			            "additionalProperties": false
			        },
			        {
			            "type": "object",
			            "properties": {
			                "file_name": {
			                    "type": "string",
			                    "description": "文件名（带扩展名）或文件索引号（如 '1', '2'）"
			                },
			                "start_line": {
			                    "type": "integer",
			                    "description": "起始行号（从1开始）"
			                },
			                "end_line": {
			                    "type": "integer",
			                    "description": "结束行号（包含该行）。注意：单次最多返回500行，可多次调用获取更多内容"
			                }
			            },
			            "required": ["file_name", "start_line", "end_line"],
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

	/**
	 * 执行内部存储内容获取操作
	 */
	public ToolExecuteResult run(InnerStorageContentInput input) {
		log.info("InnerStorageContentTool input: fileName={}, queryKey={}, columns={}, startLine={}, endLine={}", 
				input.getFileName(), input.getQueryKey(), input.getColumns(), input.getStartLine(), input.getEndLine());
		try {
			// Check operation mode based on input parameters
			boolean isTextByLinesMode = input.getStartLine() != null && input.getEndLine() != null;
			
			if (isTextByLinesMode) {
				return getTextByLines(input.getFileName(), input.getStartLine(), input.getEndLine());
			} else {
				return getStoredContent(input.getFileName(), input.getQueryKey(), input.getColumns());
			}
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

		// 严格要求queryKey和columns参数
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
				return new ToolExecuteResult("未找到文件名为 '" + fileName + "' 的内容。" + "请使用文件索引号（如 '1', '2'）或文件名的一部分来查找内容。");
			}

			// 委托给 SummaryWorkflow 进行处理
			log.info("委托给 SummaryWorkflow 处理文件内容提取：文件={}, 查询关键词={}", actualFileName, queryKey);

			String result = summaryWorkflow.executeSummaryWorkflow(planId, actualFileName, fileContent, queryKey)
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

	/**
	 * Get text content by line numbers from inner storage files
	 */
	private ToolExecuteResult getTextByLines(String fileName, Integer startLine, Integer endLine) {
		if (fileName == null || fileName.trim().isEmpty()) {
			return new ToolExecuteResult("错误：file_name参数是必需的");
		}

		// Parameter validation similar to TextFileOperator
		if (startLine < 1 || endLine < 1) {
			return new ToolExecuteResult("错误：行号必须大于0");
		}
		if (startLine > endLine) {
			return new ToolExecuteResult("错误：起始行号不能大于结束行号");
		}

		// Check 500-line limit
		int requestedLines = endLine - startLine + 1;
		if (requestedLines > 500) {
			return new ToolExecuteResult("错误：单次最多返回500行内容。请求行数: " + requestedLines + "。请分批获取内容。");
		}

		try {
			String fileContent = null;
			String actualFileName = null;

			// Try to get file content by numeric index
			try {
				int index = Integer.parseInt(fileName) - 1; // Convert to 0-based index
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
				// Not a number, try to find by file name
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

			// Split content into lines
			String[] lines = fileContent.split("\n");

			if (lines.length == 0) {
				return new ToolExecuteResult("文件 '" + actualFileName + "' 是空文件");
			}

			// Validate line number range
			if (startLine > lines.length) {
				return new ToolExecuteResult("错误：起始行号 " + startLine + " 超出文件范围。文件只有 " + lines.length + " 行。");
			}

			// Adjust endLine if it exceeds file length
			int actualEndLine = Math.min(endLine, lines.length);

			// Extract lines (convert to 0-based indexing)
			StringBuilder result = new StringBuilder();
			result.append("文件: ").append(actualFileName).append("\n");
			result.append("行号范围: ").append(startLine).append(" - ").append(actualEndLine).append("\n");
			result.append("内容:\n");

			for (int i = startLine - 1; i < actualEndLine; i++) {
				result.append(String.format("%d: %s\n", i + 1, lines[i]));
			}

			return new ToolExecuteResult(result.toString());

		}
		catch (IOException e) {
			log.error("获取文件行内容失败", e);
			return new ToolExecuteResult("获取文件行内容失败: " + e.getMessage());
		}
		catch (Exception e) {
			log.error("处理文件行内容失败", e);
			return new ToolExecuteResult("处理文件行内容失败: " + e.getMessage());
		}
	}

	@Override
	public String getCurrentToolStateString() {
		try {
			StringBuilder sb = new StringBuilder();
			sb.append("InnerStorageContent 当前状态:\n");
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
			return "InnerStorageContent 状态获取失败: " + e.getMessage();
		}
	}

	@Override
	public void cleanup(String planId) {
		// 内容获取工具不需要执行清理操作
		log.info("InnerStorageContentTool cleanup for plan: {}", planId);
	}

	@Override
	public ToolExecuteResult apply(InnerStorageContentInput input, ToolContext toolContext) {
		return run(input);
	}
}
