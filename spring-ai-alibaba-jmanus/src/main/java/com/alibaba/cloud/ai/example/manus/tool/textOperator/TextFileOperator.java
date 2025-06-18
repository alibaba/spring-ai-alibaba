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
import com.alibaba.cloud.ai.example.manus.tool.innerStorage.InnerStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.function.FunctionToolCallback;

public class TextFileOperator extends AbstractSmartFileOperator implements ToolCallBiFunctionDef {

	private static final Logger log = LoggerFactory.getLogger(TextFileOperator.class);

	private final String workingDirectoryPath;

	private final TextFileService textFileService;

	private String planId;

	public TextFileOperator(TextFileService textFileService) {
		this.textFileService = textFileService;
		ManusProperties manusProperties = textFileService.getManusProperties();
		workingDirectoryPath = CodeUtils.getWorkingDirectory(manusProperties.getBaseDir());
	}

	@Override
	protected InnerStorageService getInnerStorageService() {
		return textFileService.getInnerStorageService();
	}

	private final String PARAMETERS = """
			{
			    "type": "object",
			    "properties": {
			        "action": {
			            "type": "string",
			            "description": "(required) The action to perform: 'replace', 'get_text', 'append', 'count_words', 'get_description'"
			        },
			        "file_path": {
			            "type": "string",
			            "description": "(required for file operations) The path where the text file is located or should be saved"
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
			    "required": ["action"]
			}
			""";

	private final String TOOL_NAME = "text_file_operator";

	private final String TOOL_DESCRIPTION = """
			对文本文件（包括 md、html、css、java 等）执行各种操作：
			- replace: 替换文件中的特定文本
			- get_text: 获取文件的当前内容
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

			注意：文件操作会自动处理文件的打开和保存，用户无需手动执行这些操作。
			""";

	public OpenAiApi.FunctionTool getToolDefinition() {
		OpenAiApi.FunctionTool.Function function = new OpenAiApi.FunctionTool.Function(TOOL_DESCRIPTION, TOOL_NAME,
				PARAMETERS);
		OpenAiApi.FunctionTool functionTool = new OpenAiApi.FunctionTool(function);
		return functionTool;
	}

	public FunctionToolCallback<String, ToolExecuteResult> getFunctionToolCallback(TextFileService textFileService) {
		return FunctionToolCallback.builder(TOOL_NAME, new TextFileOperator(textFileService))
			.description(TOOL_DESCRIPTION)
			.inputSchema(PARAMETERS)
			.inputType(String.class)
			.build();
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
				case "replace" -> {
					String sourceText = (String) toolInputMap.get("source_text");
					String targetText = (String) toolInputMap.get("target_text");
					yield processResult(replaceText(planId, filePath, sourceText, targetText), "replace", filePath);
				}
				case "get_text" -> processResult(getCurrentText(planId, filePath), "get_text", filePath);
				case "append" -> {
					String appendContent = (String) toolInputMap.get("content");
					yield processResult(appendToFile(planId, filePath, appendContent), "append", filePath);
				}
				case "count_words" -> processResult(countWords(planId, filePath), "count_words", filePath);
				case "get_description" -> getSavedDescriptionContent();
				default -> {
					textFileService.updateFileState(planId, filePath, "Error: Unknown action");
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

	/**
	 * 确保文件被打开，如果不存在则创建
	 */
	private ToolExecuteResult ensureFileOpen(String planId, String filePath) {
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

	private ToolExecuteResult replaceText(String planId, String filePath, String sourceText, String targetText) {
		try {
			// 自动打开文件
			ToolExecuteResult openResult = ensureFileOpen(planId, filePath);
			if (!openResult.getOutput().toLowerCase().contains("success")) {
				return openResult;
			}

			Path absolutePath = Paths.get(workingDirectoryPath).resolve(filePath);
			String content = Files.readString(absolutePath);
			String newContent = content.replace(sourceText, targetText);
			Files.writeString(absolutePath, newContent);

			// 自动保存文件
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

	private ToolExecuteResult getCurrentText(String planId, String filePath) {
		try {
			// 自动打开文件
			ToolExecuteResult openResult = ensureFileOpen(planId, filePath);
			if (!openResult.getOutput().toLowerCase().contains("success")) {
				return openResult;
			}

			Path absolutePath = Paths.get(workingDirectoryPath).resolve(filePath);
			String content = Files.readString(absolutePath);

			textFileService.updateFileState(planId, filePath, "Success: Retrieved current text");
			return new ToolExecuteResult(content);
		}
		catch (IOException e) {
			textFileService.updateFileState(planId, filePath, "Error: " + e.getMessage());
			return new ToolExecuteResult("Error retrieving text: " + e.getMessage());
		}
	}

	private ToolExecuteResult appendToFile(String planId, String filePath, String content) {
		try {
			if (content == null || content.isEmpty()) {
				textFileService.updateFileState(planId, filePath, "Error: No content to append");
				return new ToolExecuteResult("Error: No content to append");
			}

			// 自动打开文件
			ToolExecuteResult openResult = ensureFileOpen(planId, filePath);
			if (!openResult.getOutput().toLowerCase().contains("success")) {
				return openResult;
			}

			Path absolutePath = Paths.get(workingDirectoryPath).resolve(filePath);
			Files.writeString(absolutePath, "\n" + content, StandardOpenOption.APPEND, StandardOpenOption.CREATE);

			// 自动保存文件
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
			// 自动打开文件
			ToolExecuteResult openResult = ensureFileOpen(planId, filePath);
			if (!openResult.getOutput().toLowerCase().contains("success")) {
				return openResult;
			}

			Path absolutePath = Paths.get(workingDirectoryPath).resolve(filePath);
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

	/**
	 * 获取保存的描述内容
	 */
	private ToolExecuteResult getSavedDescriptionContent() {
		String planId = this.planId;
		if (planId == null) {
			return new ToolExecuteResult("Error: No plan ID set");
		}

		// 使用 InnerStorageService 获取自动存储的内容作为描述
		try {
			InnerStorageService storageService = getInnerStorageService();
			java.util.List<InnerStorageService.FileInfo> autoStoredFiles = storageService
				.searchAutoStoredFiles(getWorkingDirectoryPath(), planId, "");

			if (autoStoredFiles.isEmpty()) {
				return new ToolExecuteResult("No auto-stored content found for the current plan");
			}

			StringBuilder description = new StringBuilder();
			description.append("Auto-stored content summary for plan ").append(planId).append(":\n\n");
			for (InnerStorageService.FileInfo file : autoStoredFiles) {
				description.append("📄 ")
					.append(file.getRelativePath())
					.append(" (")
					.append(file.getSize())
					.append(" bytes)\n");
				try {
					String content = storageService.readFileContent(getWorkingDirectoryPath(), planId,
							file.getRelativePath());
					description.append(content).append("\n\n");
				}
				catch (java.io.IOException e) {
					description.append("  Error reading file: ").append(e.getMessage()).append("\n\n");
				}
			}

			return new ToolExecuteResult(description.toString());
		}
		catch (Exception e) {
			return new ToolExecuteResult("Error retrieving auto-stored content: " + e.getMessage());
		}
	}

	@Override
	public void setPlanId(String planId) {
		this.planId = planId;
	}

	@Override
	public String getCurrentToolStateString() {
		String planId = this.planId;
		return String.format(
				"""
						Current Text File Operation State:
						- Working Directory:
						%s

						- Operations are automatically handled (no manual file opening/closing required)
						- All file operations (open, save) are performed automatically
						- Supported file types: txt, md, html, css, java, py, js, ts, xml, json, yaml, properties, sh, bat, log, etc.

						- Last Operation Result:
						%s
						""",
				workingDirectoryPath, textFileService.getLastOperationResult(planId).isEmpty()
						? "No operation performed yet" : textFileService.getLastOperationResult(planId));
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
	public ToolExecuteResult apply(String s, ToolContext toolContext) {
		return run(s);
	}

	@Override
	public void cleanup(String planId) {
		if (planId != null) {
			log.info("Cleaning up text file resources for plan: {}", planId);
			textFileService.closeFileForPlan(planId);
			// 使用父类的清理方法来清理 InnerStorage 相关资源
			cleanupPlan(planId);
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

	@Override
	protected String getWorkingDirectoryPath() {
		return workingDirectoryPath;
	}

	@Override
	protected String getCurrentPlanId() {
		return planId;
	}

}
