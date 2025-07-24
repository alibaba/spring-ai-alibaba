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
package com.alibaba.cloud.ai.example.manus.tool.textOperator;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.Map;

import com.alibaba.cloud.ai.example.manus.tool.AbstractBaseTool;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;
import com.alibaba.cloud.ai.example.manus.tool.innerStorage.SmartContentSavingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.openai.api.OpenAiApi;

public class TextFileOperator extends AbstractBaseTool<TextFileOperator.TextFileInput> {

	private static final Logger log = LoggerFactory.getLogger(TextFileOperator.class);

	/**
	 * Internal input class for defining input parameters of text file operation tool
	 */
	public static class TextFileInput {

		private String action;

		@com.fasterxml.jackson.annotation.JsonProperty("file_path")
		private String filePath;

		private String content;

		@com.fasterxml.jackson.annotation.JsonProperty("source_text")
		private String sourceText;

		@com.fasterxml.jackson.annotation.JsonProperty("target_text")
		private String targetText;

		@com.fasterxml.jackson.annotation.JsonProperty("start_line")
		private Integer startLine;

		@com.fasterxml.jackson.annotation.JsonProperty("end_line")
		private Integer endLine;

		public TextFileInput() {
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

		public String getContent() {
			return content;
		}

		public void setContent(String content) {
			this.content = content;
		}

		public String getSourceText() {
			return sourceText;
		}

		public void setSourceText(String sourceText) {
			this.sourceText = sourceText;
		}

		public String getTargetText() {
			return targetText;
		}

		public void setTargetText(String targetText) {
			this.targetText = targetText;
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

	private final TextFileService textFileService;

	private final SmartContentSavingService innerStorageService;

	public TextFileOperator(TextFileService textFileService, SmartContentSavingService innerStorageService) {
		this.textFileService = textFileService;
		this.innerStorageService = innerStorageService;
	}

	private final String PARAMETERS = """
			{
			    "oneOf": [
			        {
			            "type": "object",
			            "properties": {
			                "action": {
			                    "type": "string",
			                    "const": "replace"
			                },
			                "file_path": {
			                    "type": "string",
			                    "description": "要操作的文件路径"
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
			            "required": ["action", "file_path", "source_text", "target_text"],
			            "additionalProperties": false
			        },		        {
			           "type": "object",
			           "properties": {
			               "action": {
			                   "type": "string",
			                   "const": "get_text"
			               },
			               "file_path": {
			                   "type": "string",
			                   "description": "要读取的文件路径"
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
			           "required": ["action", "file_path", "start_line", "end_line"],
			           "additionalProperties": false
			       },
			       {
			           "type": "object",
			           "properties": {
			               "action": {
			                   "type": "string",
			                   "const": "get_all_text"
			               },
			               "file_path": {
			                   "type": "string",
			                   "description": "要读取全部内容的文件路径。注意：如果文件过长，内容将存储在临时文件中并返回文件路径"
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
			                    "const": "append"
			                },
			                "file_path": {
			                    "type": "string",
			                    "description": "要追加内容的文件路径"
			                },
			                "content": {
			                    "type": "string",
			                    "description": "要追加的内容"
			                }
			            },
			            "required": ["action", "file_path", "content"],
			            "additionalProperties": false
			        },
			        {
			            "type": "object",
			            "properties": {
			                "action": {
			                    "type": "string",
			                    "const": "count_words"
			                },
			                "file_path": {
			                    "type": "string",
			                    "description": "要统计单词数的文件路径"
			                }
			            },
			            "required": ["action", "file_path"],
			            "additionalProperties": false
			        }
			    ]
			}
			""";

	private static final String TOOL_NAME = "text_file_operator";

	private final String TOOL_DESCRIPTION = """
			对文本文件（包括 md、html、css、java 等）执行各种操作。
			支持的操作：
			- replace: 替换文件中的特定文本，需要提供 source_text 和 target_text 参数
			- get_text: 获取文件指定行号范围的内容，需要提供 start_line 和 end_line 参数
			  限制：单次最多返回500行内容，如需更多内容请多次调用
			- get_all_text: 获取文件的全部内容
			  注意：如果文件内容过长，将自动存储到临时文件中并返回文件路径
			- append: 向文件追加内容，需要提供 content 参数
			- count_words: 统计当前文件中的单词数量

			支持的文件类型包括：
			- 文本文件 (.txt)
			- Markdown 文件 (.md, .markdown)
			- 网页文件 (.html, .css, .scss, .sass, .less)
			- 编程文件 (.java, .py, .js, .ts, .jsx, .tsx)
			- 配置文件 (.xml, .json, .yaml, .yml, .properties)
			- 脚本文件 (.sh, .bat, .cmd)
			- 日志文件 (.log)
			- 以及更多基于文本的文件类型

			注意：文件操作会自动处理文件的打开和保存，用户无需手动执行这些操作。
			每个操作都有严格的参数要求，确保操作的准确性和安全性。
			""";

	public OpenAiApi.FunctionTool getToolDefinition() {
		OpenAiApi.FunctionTool.Function function = new OpenAiApi.FunctionTool.Function(TOOL_DESCRIPTION, TOOL_NAME,
				PARAMETERS);
		OpenAiApi.FunctionTool functionTool = new OpenAiApi.FunctionTool(function);
		return functionTool;
	}

	public ToolExecuteResult run(String toolInput) {
		log.info("TextFileOperator toolInput:{}", toolInput);
		try {
			Map<String, Object> toolInputMap = new ObjectMapper().readValue(toolInput,
					new TypeReference<Map<String, Object>>() {
					});
			String planId = this.currentPlanId;

			String action = (String) toolInputMap.get("action");
			String filePath = (String) toolInputMap.get("file_path");

			// Basic parameter validation
			if (action == null) {
				return new ToolExecuteResult("错误：action参数是必需的");
			}
			if (filePath == null) {
				return new ToolExecuteResult("错误：file_path参数是必需的");
			}

			return switch (action) {
				case "replace" -> {
					String sourceText = (String) toolInputMap.get("source_text");
					String targetText = (String) toolInputMap.get("target_text");

					if (sourceText == null || targetText == null) {
						yield new ToolExecuteResult("错误：replace操作需要source_text和target_text参数");
					}

					yield replaceText(planId, filePath, sourceText, targetText);
				}
				case "get_text" -> {
					Integer startLine = (Integer) toolInputMap.get("start_line");
					Integer endLine = (Integer) toolInputMap.get("end_line");

					if (startLine == null || endLine == null) {
						yield new ToolExecuteResult("错误：get_text操作需要start_line和end_line参数");
					}

					yield getTextByLines(planId, filePath, startLine, endLine);
				}
				case "get_all_text" -> getAllText(planId, filePath);
				case "append" -> {
					String appendContent = (String) toolInputMap.get("content");

					if (appendContent == null) {
						yield new ToolExecuteResult("错误：append操作需要content参数");
					}

					yield appendToFile(planId, filePath, appendContent);
				}
				case "count_words" -> countWords(planId, filePath);
				default -> {
					textFileService.updateFileState(planId, filePath, "Error: Unknown action");
					yield new ToolExecuteResult(
							"未知操作: " + action + "。支持的操作: replace, get_text, get_all_text, append, count_words");
				}
			};
		}
		catch (Exception e) {
			String planId = this.currentPlanId;
			textFileService.updateFileState(planId, textFileService.getCurrentFilePath(planId),
					"Error: " + e.getMessage());
			return new ToolExecuteResult("工具执行失败: " + e.getMessage());
		}
	}

	/**
	 * 执行文本文件操作，接受强类型输入对象
	 */
	@Override
	public ToolExecuteResult run(TextFileInput input) {
		log.info("TextFileOperator input: action={}, filePath={}", input.getAction(), input.getFilePath());
		try {
			String planId = this.currentPlanId;
			String action = input.getAction();
			String filePath = input.getFilePath();

			// Basic parameter validation
			if (action == null) {
				return new ToolExecuteResult("错误：action参数是必需的");
			}
			if (filePath == null) {
				return new ToolExecuteResult("错误：file_path参数是必需的");
			}

			return switch (action) {
				case "replace" -> {
					String sourceText = input.getSourceText();
					String targetText = input.getTargetText();

					if (sourceText == null || targetText == null) {
						yield new ToolExecuteResult("错误：replace操作需要source_text和target_text参数");
					}

					yield replaceText(planId, filePath, sourceText, targetText);
				}
				case "get_text" -> {
					Integer startLine = input.getStartLine();
					Integer endLine = input.getEndLine();

					if (startLine == null || endLine == null) {
						yield new ToolExecuteResult("错误：get_text操作需要start_line和end_line参数");
					}

					yield getTextByLines(planId, filePath, startLine, endLine);
				}
				case "get_all_text" -> getAllText(planId, filePath);
				case "append" -> {
					String appendContent = input.getContent();

					if (appendContent == null) {
						yield new ToolExecuteResult("错误：append操作需要content参数");
					}

					yield appendToFile(planId, filePath, appendContent);
				}
				case "count_words" -> countWords(planId, filePath);
				default -> {
					textFileService.updateFileState(planId, filePath, "Error: Unknown action");
					yield new ToolExecuteResult(
							"未知操作: " + action + "。支持的操作: replace, get_text, get_all_text, append, count_words");
				}
			};
		}
		catch (Exception e) {
			String planId = this.currentPlanId;
			textFileService.updateFileState(planId, textFileService.getCurrentFilePath(planId),
					"Error: " + e.getMessage());
			return new ToolExecuteResult("工具执行失败: " + e.getMessage());
		}
	}

	/**
	 * 确保文件被打开，如果不存在则创建
	 */
	private ToolExecuteResult ensureFileOpen(String planId, String filePath) {
		try {
			// Check file type
			if (!textFileService.isSupportedFileType(filePath)) {
				textFileService.updateFileState(planId, filePath, "Error: Unsupported file type");
				return new ToolExecuteResult("Unsupported file type. Only text-based files are supported.");
			}

			// Use TextFileService to validate and get the absolute path
			Path absolutePath = textFileService.validateFilePath(planId, filePath);

			// If file doesn't exist, create parent directory first
			if (!Files.exists(absolutePath)) {
				try {
					Files.createDirectories(absolutePath.getParent());
					Files.createFile(absolutePath);
					textFileService.updateFileState(planId, filePath, "Success: New file created");
					return new ToolExecuteResult("New file created successfully: " + absolutePath);
				}
				catch (IOException e) {
					textFileService.updateFileState(planId, filePath,
							"Error: Failed to create file: " + e.getMessage());
					return new ToolExecuteResult("Failed to create file: " + e.getMessage());
				}
			}

			textFileService.updateFileState(planId, filePath, "Success: File opened");
			return new ToolExecuteResult("File opened successfully: " + absolutePath);
		}
		catch (IOException e) {
			textFileService.updateFileState(planId, filePath, "Error: " + e.getMessage());
			return new ToolExecuteResult("Error opening file: " + e.getMessage());
		}
	}

	private ToolExecuteResult replaceText(String planId, String filePath, String sourceText, String targetText) {
		try {
			// Automatically open file
			ToolExecuteResult openResult = ensureFileOpen(planId, filePath);
			if (!openResult.getOutput().toLowerCase().contains("success")) {
				return openResult;
			}

			Path absolutePath = textFileService.validateFilePath(planId, filePath);
			String content = Files.readString(absolutePath);
			String newContent = content.replace(sourceText, targetText);
			Files.writeString(absolutePath, newContent);

			// Automatically save file
			try (FileChannel channel = FileChannel.open(absolutePath, StandardOpenOption.WRITE)) {
				channel.force(true);
			}

			textFileService.updateFileState(planId, filePath, "Success: Text replaced and saved");
			return new ToolExecuteResult("Text replaced and saved successfully");
		}
		catch (IOException e) {
			textFileService.updateFileState(planId, filePath, "Error: " + e.getMessage());
			return new ToolExecuteResult("Error replacing text: " + e.getMessage());
		}
	}

	private ToolExecuteResult getTextByLines(String planId, String filePath, Integer startLine, Integer endLine) {
		try {
			// Parameter validation
			if (startLine < 1 || endLine < 1) {
				return new ToolExecuteResult("错误：行号必须从1开始");
			}
			if (startLine > endLine) {
				return new ToolExecuteResult("错误：起始行号不能大于结束行号");
			}

			// Check 500-line limit
			int requestedLines = endLine - startLine + 1;
			if (requestedLines > 500) {
				return new ToolExecuteResult("错误：单次最多返回500行内容。请调整行号范围或分多次调用。当前请求行数：" + requestedLines);
			}

			// Automatically open file
			ToolExecuteResult openResult = ensureFileOpen(planId, filePath);
			if (!openResult.getOutput().toLowerCase().contains("success")) {
				return openResult;
			}

			Path absolutePath = textFileService.validateFilePath(planId, filePath);
			java.util.List<String> lines = Files.readAllLines(absolutePath);

			if (lines.isEmpty()) {
				textFileService.updateFileState(planId, filePath, "Success: File is empty");
				return new ToolExecuteResult("文件为空");
			}

			// Validate line number range
			if (startLine > lines.size()) {
				return new ToolExecuteResult("错误：起始行号超出文件范围（文件共" + lines.size() + "行）");
			}

			// Adjust end line number (not exceeding total file lines)
			int actualEndLine = Math.min(endLine, lines.size());

			StringBuilder result = new StringBuilder();
			result.append(String.format("文件: %s (第%d-%d行，共%d行)\n", filePath, startLine, actualEndLine, lines.size()));
			result.append("=".repeat(50)).append("\n");

			for (int i = startLine - 1; i < actualEndLine; i++) {
				result.append(String.format("%4d: %s\n", i + 1, lines.get(i)));
			}

			// If file has more content, prompt user
			if (actualEndLine < lines.size()) {
				result.append("\n提示：文件还有更多内容（第")
					.append(actualEndLine + 1)
					.append("-")
					.append(lines.size())
					.append("行），可继续调用get_text获取。");
			}

			textFileService.updateFileState(planId, filePath, "Success: Retrieved text lines");
			return new ToolExecuteResult(result.toString());
		}
		catch (IOException e) {
			textFileService.updateFileState(planId, filePath, "Error: " + e.getMessage());
			return new ToolExecuteResult("Error retrieving text lines: " + e.getMessage());
		}
	}

	private ToolExecuteResult getAllText(String planId, String filePath) {
		try {
			// Automatically open file
			ToolExecuteResult openResult = ensureFileOpen(planId, filePath);
			if (!openResult.getOutput().toLowerCase().contains("success")) {
				return openResult;
			}

			// Read file content
			Path absolutePath = textFileService.validateFilePath(planId, filePath);
			String content = Files.readString(absolutePath);

			// Force flush to disk to ensure data consistency
			try (FileChannel channel = FileChannel.open(absolutePath, StandardOpenOption.READ)) {
				channel.force(true);
			}

			textFileService.updateFileState(planId, filePath, "Success: Retrieved all text");

			// Use InnerStorageService to intelligently process content
			SmartContentSavingService.SmartProcessResult processedResult = innerStorageService.processContent(planId,
					content, "get_all_text");

			return new ToolExecuteResult(processedResult.getSummary());
		}
		catch (IOException e) {
			textFileService.updateFileState(planId, filePath, "Error: " + e.getMessage());
			return new ToolExecuteResult("Error retrieving all text: " + e.getMessage());
		}
	}

	private ToolExecuteResult appendToFile(String planId, String filePath, String content) {
		try {
			if (content == null || content.isEmpty()) {
				textFileService.updateFileState(planId, filePath, "Error: No content to append");
				return new ToolExecuteResult("Error: No content to append");
			}

			// Automatically open file
			ToolExecuteResult openResult = ensureFileOpen(planId, filePath);
			if (!openResult.getOutput().toLowerCase().contains("success")) {
				return openResult;
			}

			Path absolutePath = textFileService.validateFilePath(planId, filePath);
			Files.writeString(absolutePath, "\n" + content, StandardOpenOption.APPEND, StandardOpenOption.CREATE);

			// Automatically save file
			try (FileChannel channel = FileChannel.open(absolutePath, StandardOpenOption.WRITE)) {
				channel.force(true);
			}

			textFileService.updateFileState(planId, filePath, "Success: Content appended and saved");
			return new ToolExecuteResult("Content appended and saved successfully");
		}
		catch (IOException e) {
			textFileService.updateFileState(planId, filePath, "Error: " + e.getMessage());
			return new ToolExecuteResult("Error appending to file: " + e.getMessage());
		}
	}

	private ToolExecuteResult countWords(String planId, String filePath) {
		try {
			// Automatically open file
			ToolExecuteResult openResult = ensureFileOpen(planId, filePath);
			if (!openResult.getOutput().toLowerCase().contains("success")) {
				return openResult;
			}

			Path absolutePath = textFileService.validateFilePath(planId, filePath);
			String content = Files.readString(absolutePath);
			int wordCount = content.isEmpty() ? 0 : content.split("\\s+").length;

			textFileService.updateFileState(planId, filePath, "Success: Counted words");
			return new ToolExecuteResult(String.format("Total word count (including Markdown symbols): %d", wordCount));
		}
		catch (IOException e) {
			textFileService.updateFileState(planId, filePath, "Error: " + e.getMessage());
			return new ToolExecuteResult("Error counting words: " + e.getMessage());
		}
	}

	@Override
	public String getCurrentToolStateString() {
		String planId = this.currentPlanId;
		try {
			Path workingDir = textFileService.getAbsolutePath(planId, "");
			return String.format(
					"""
							Current Text File Operation State:
							- working Directory:
							%s

							- Operations are automatically handled (no manual file opening/closing required)
							- All file operations (open, save) are performed automatically
							- Supported file types: txt, md, html, css, java, py, js, ts, xml, json, yaml, properties, sh, bat, log, etc.

							- Last Operation Result:
							%s
							""",
					workingDir.toString(), textFileService.getLastOperationResult(planId).isEmpty()
							? "No operation performed yet" : textFileService.getLastOperationResult(planId));
		}
		catch (Exception e) {
			return String.format("""
					Current Text File Operation State:
					- Error getting working directory: %s

					- Last Operation Result:
					%s
					""", e.getMessage(), textFileService.getLastOperationResult(planId).isEmpty()
					? "No operation performed yet" : textFileService.getLastOperationResult(planId));
		}
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
	public Class<TextFileInput> getInputType() {
		return TextFileInput.class;
	}

	@Override
	public void cleanup(String planId) {
		if (planId != null) {
			log.info("Cleaning up text file resources for plan: {}", planId);
			textFileService.cleanupPlanDirectory(planId);
		}
	}

	// @Override
	// public FileState getInstance(String planId) {
	// if (planId == null) {
	// throw new IllegalArgumentException("planId cannot be null");
	// }
	// return textFileService.getFileState(planId);
	// }

	@Override
	public String getServiceGroup() {
		return "default-service-group";
	}

}
