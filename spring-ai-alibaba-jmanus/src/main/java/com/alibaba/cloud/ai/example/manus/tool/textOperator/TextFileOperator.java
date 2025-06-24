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

import com.alibaba.cloud.ai.example.manus.config.ManusProperties;
import com.alibaba.cloud.ai.example.manus.tool.ToolCallBiFunctionDef;
import com.alibaba.cloud.ai.example.manus.tool.code.CodeUtils;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.openai.api.OpenAiApi;

public class TextFileOperator implements ToolCallBiFunctionDef<TextFileOperator.TextFileInput> {

	private static final Logger log = LoggerFactory.getLogger(TextFileOperator.class);

	/**
	 * 内部输入类，用于定义文本文件操作工具的输入参数
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

	}

	private final String workingDirectoryPath;

	private final TextFileService textFileService;

	private String planId;

	public TextFileOperator(TextFileService textFileService) {
		this.textFileService = textFileService;
		ManusProperties manusProperties = textFileService.getManusProperties();
		workingDirectoryPath = CodeUtils.getWorkingDirectory(manusProperties.getBaseDir());
	}

	private static final String PARAMETERS = """
			{
			    "type": "object",
			    "properties": {
			        "action": {
			            "type": "string",
			            "description": "(required) The action to perform: 'open', 'replace', 'get_text', 'save', 'append', 'count_words'"
			        },
			        "file_path": {
			            "type": "string",
			            "description": "(required) The path where the text file is located or should be saved"
			        },
			        "content": {
			            "type": "string",
			            "description": "(optional) The content to write or append to the file"
			        },
			        "source_text": {
			            "type": "string",
			            "description": "(optional) The text to be replaced when using 'replace' action"
			        },
			        "target_text": {
			            "type": "string",
			            "description": "(optional) The text to replace with when using 'replace' action"
			        }
			    },
			    "required": ["action", "file_path"]
			}
			""";

	private static final String TOOL_NAME = "text_file_operator";

	private static final String TOOL_DESCRIPTION = """
			对文本文件（包括 md、html、css、java 等）执行各种操作：
			- open: 打开并读取文本文件，您必须先打开文件！
			- replace: 替换文件中的特定文本
			- get_text: 获取文件的当前内容
			- save: 保存并关闭文件
			- append: 向文件追加内容
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
			String planId = this.planId;

			String action = (String) toolInputMap.get("action");
			String filePath = (String) toolInputMap.get("file_path");

			return switch (action) {
				case "open" -> openFile(planId, filePath);
				case "replace" -> {
					String sourceText = (String) toolInputMap.get("source_text");
					String targetText = (String) toolInputMap.get("target_text");
					yield replaceText(planId, sourceText, targetText);
				}
				case "get_text" -> getCurrentText(planId);
				case "save" -> {
					String content = (String) toolInputMap.get("content");
					yield saveAndClose(planId, content);
				}
				case "append" -> {
					String appendContent = (String) toolInputMap.get("content");
					yield appendToFile(planId, appendContent);
				}
				case "count_words" -> countWords(planId);
				default -> {
					textFileService.updateFileState(planId, textFileService.getCurrentFilePath(planId),
							"Error: Unknown action");
					yield new ToolExecuteResult("Unknown action: " + action);
				}
			};
		}
		catch (Exception e) {
			String planId = this.planId;
			textFileService.updateFileState(planId, textFileService.getCurrentFilePath(planId),
					"Error: " + e.getMessage());
			return new ToolExecuteResult("Error: " + e.getMessage());
		}
	}

	public ToolExecuteResult runTyped(TextFileInput input) {
		log.info("TextFileOperator typed input: action={}, filePath={}", input.getAction(), input.getFilePath());
		try {
			String planId = this.planId;
			String action = input.getAction();
			String filePath = input.getFilePath();

			return switch (action) {
				case "open" -> openFile(planId, filePath);
				case "replace" -> {
					String sourceText = input.getSourceText();
					String targetText = input.getTargetText();
					yield replaceText(planId, sourceText, targetText);
				}
				case "get_text" -> getCurrentText(planId);
				case "save" -> {
					String content = input.getContent();
					yield saveAndClose(planId, content);
				}
				case "append" -> {
					String appendContent = input.getContent();
					yield appendToFile(planId, appendContent);
				}
				case "count_words" -> countWords(planId);
				default -> {
					textFileService.updateFileState(planId, textFileService.getCurrentFilePath(planId),
							"Error: Unknown action");
					yield new ToolExecuteResult("Unknown action: " + action);
				}
			};
		}
		catch (Exception e) {
			String planId = this.planId;
			textFileService.updateFileState(planId, textFileService.getCurrentFilePath(planId),
					"Error: " + e.getMessage());
			return new ToolExecuteResult("Error: " + e.getMessage());
		}
	}

	private ToolExecuteResult openFile(String planId, String filePath) {
		try {
			// 检查文件类型
			if (!textFileService.isSupportedFileType(filePath)) {
				textFileService.updateFileState(planId, filePath, "Error: Unsupported file type");
				return new ToolExecuteResult("Unsupported file type. Only text-based files are supported.");
			}

			textFileService.validateAndGetAbsolutePath(workingDirectoryPath, filePath);

			// 如果文件不存在，先创建父目录
			Path absolutePath = Paths.get(workingDirectoryPath).resolve(filePath);
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

	private ToolExecuteResult replaceText(String planId, String sourceText, String targetText) {
		try {
			String currentFilePath = textFileService.getCurrentFilePath(planId);
			if (currentFilePath.isEmpty()) {
				textFileService.updateFileState(planId, "", "Error: No file is currently open");
				return new ToolExecuteResult("Error: No file is currently open");
			}

			Path absolutePath = Paths.get(workingDirectoryPath).resolve(currentFilePath);
			String content = Files.readString(absolutePath);
			String newContent = content.replace(sourceText, targetText);
			Files.writeString(absolutePath, newContent);

			textFileService.updateFileState(planId, currentFilePath, "Success: Text replaced");
			return new ToolExecuteResult("Text replaced successfully");
		}
		catch (IOException e) {
			textFileService.updateFileState(planId, textFileService.getCurrentFilePath(planId),
					"Error: " + e.getMessage());
			return new ToolExecuteResult("Error replacing text: " + e.getMessage());
		}
	}

	private ToolExecuteResult getCurrentText(String planId) {
		try {
			String currentFilePath = textFileService.getCurrentFilePath(planId);
			if (currentFilePath.isEmpty()) {
				textFileService.updateFileState(planId, "", "Error: No file is currently open");
				return new ToolExecuteResult("Error: No file is currently open");
			}

			Path absolutePath = Paths.get(workingDirectoryPath).resolve(currentFilePath);
			String content = Files.readString(absolutePath);

			textFileService.updateFileState(planId, currentFilePath, "Success: Retrieved current text");
			return new ToolExecuteResult(content);
		}
		catch (IOException e) {
			textFileService.updateFileState(planId, textFileService.getCurrentFilePath(planId),
					"Error: " + e.getMessage());
			return new ToolExecuteResult("Error retrieving text: " + e.getMessage());
		}
	}

	private ToolExecuteResult saveAndClose(String planId, String content) {
		try {
			String currentFilePath = textFileService.getCurrentFilePath(planId);
			if (currentFilePath.isEmpty()) {
				textFileService.updateFileState(planId, "", "Error: No file is currently open");
				return new ToolExecuteResult("Error: No file is currently open");
			}
			Path absolutePath = Paths.get(workingDirectoryPath).resolve(currentFilePath);

			if (content != null) {
				Files.writeString(absolutePath, content);
			}

			// 强制刷新到磁盘
			try (FileChannel channel = FileChannel.open(absolutePath, StandardOpenOption.WRITE)) {
				channel.force(true);
			}

			textFileService.updateFileState(planId, "", "Success: File saved and closed");
			textFileService.closeFileForPlan(planId);
			return new ToolExecuteResult("File saved and closed successfully: " + absolutePath);
		}
		catch (IOException e) {
			textFileService.updateFileState(planId, textFileService.getCurrentFilePath(planId),
					"Error: " + e.getMessage());
			return new ToolExecuteResult("Error saving file: " + e.getMessage());
		}
	}

	private ToolExecuteResult appendToFile(String planId, String content) {
		try {
			if (content == null || content.isEmpty()) {
				textFileService.updateFileState(planId, textFileService.getCurrentFilePath(planId),
						"Error: No content to append");
				return new ToolExecuteResult("Error: No content to append");
			}

			String currentFilePath = textFileService.getCurrentFilePath(planId);
			if (currentFilePath.isEmpty()) {
				textFileService.updateFileState(planId, "", "Error: No file is currently open");
				return new ToolExecuteResult("Error: No file is currently open");
			}

			Path absolutePath = Paths.get(workingDirectoryPath).resolve(currentFilePath);
			Files.writeString(absolutePath, "\n" + content, StandardOpenOption.APPEND, StandardOpenOption.CREATE);

			textFileService.updateFileState(planId, currentFilePath, "Success: Content appended");
			return new ToolExecuteResult("Content appended successfully");
		}
		catch (IOException e) {
			textFileService.updateFileState(planId, textFileService.getCurrentFilePath(planId),
					"Error: " + e.getMessage());
			return new ToolExecuteResult("Error appending to file: " + e.getMessage());
		}
	}

	private ToolExecuteResult countWords(String planId) {
		try {
			String currentFilePath = textFileService.getCurrentFilePath(planId);
			if (currentFilePath.isEmpty()) {
				textFileService.updateFileState(planId, "", "Error: No file is currently open");
				return new ToolExecuteResult("Error: No file is currently open");
			}

			Path absolutePath = Paths.get(workingDirectoryPath).resolve(currentFilePath);
			String content = Files.readString(absolutePath);
			int wordCount = content.isEmpty() ? 0 : content.split("\\s+").length;

			textFileService.updateFileState(planId, currentFilePath, "Success: Counted words");
			return new ToolExecuteResult(String.format("Total word count (including Markdown symbols): %d", wordCount));
		}
		catch (IOException e) {
			textFileService.updateFileState(planId, textFileService.getCurrentFilePath(planId),
					"Error: " + e.getMessage());
			return new ToolExecuteResult("Error counting words: " + e.getMessage());
		}
	}

	@Override
	public void setPlanId(String planId) {
		this.planId = planId;
	}

	@Override
	public String getCurrentToolStateString() {
		String planId = this.planId;
		return String.format("""
				Current Text File Operation State:
				- Working Directory:
				%s

				- Current File:
				%s
				- File Type: %s

				- Last Operation Result:
				%s
				""", workingDirectoryPath,
				textFileService.getCurrentFilePath(planId).isEmpty() ? "No file open"
						: textFileService.getCurrentFilePath(planId),
				textFileService.getCurrentFilePath(planId).isEmpty() ? "N/A"
						: textFileService.getFileExtension(textFileService.getCurrentFilePath(planId)),
				textFileService.getLastOperationResult(planId).isEmpty() ? "No operation performed yet"
						: textFileService.getLastOperationResult(planId));
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
	public boolean isReturnDirect() {
		return false;
	}

	@Override
	public ToolExecuteResult apply(TextFileInput input, ToolContext toolContext) {
		return runTyped(input);
	}

	@Override
	public void cleanup(String planId) {
		if (planId != null) {
			log.info("Cleaning up text file resources for plan: {}", planId);
			textFileService.closeFileForPlan(planId);
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
