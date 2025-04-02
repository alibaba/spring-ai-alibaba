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
package com.alibaba.cloud.ai.example.manus.tool;

import java.io.*;
import java.nio.file.*;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import com.alibaba.cloud.ai.example.manus.agent.BaseAgent;
import com.alibaba.cloud.ai.example.manus.tool.support.ToolExecuteResult;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.function.FunctionToolCallback;

public class TextFileOperator implements ToolCallBiFunctionDef {

	private static final Logger log = LoggerFactory.getLogger(TextFileOperator.class);

	/**
	 * 文本文件操作的工作目录
	 */
	private final String workingDirectoryPath;

	/**
	 * 支持的文本文件扩展名集合
	 */
	private static final Set<String> SUPPORTED_EXTENSIONS = new HashSet<>(Set.of(".txt", ".md", ".markdown", // 普通文本和Markdown
			".java", ".py", ".js", ".ts", ".jsx", ".tsx", // 常见编程语言
			".html", ".htm", ".css", ".scss", ".sass", ".less", // Web相关
			".xml", ".json", ".yaml", ".yml", ".properties", // 配置文件
			".sql", ".sh", ".bat", ".cmd", // 脚本和数据库
			".log", ".conf", ".ini", // 日志和配置
			".gradle", ".pom", ".mvn" // 构建工具
	));

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
			Operate on text files (including md, html, css, java, etc) with various actions:
			- open: Open and read a text file
			- replace: Replace specific text in the file
			- get_text: Get current content of the file
			- save: Save and close the file
			- append: Append content to the file
			- count_words: Count words in the current file

			Supported file types include:
			- Text files (.txt)
			- Markdown files (.md, .markdown)
			- Web files (.html, .css, .scss, .sass, .less)
			- Programming files (.java, .py, .js, .ts, .jsx, .tsx)
			- Configuration files (.xml, .json, .yaml, .yml, .properties)
			- Script files (.sh, .bat, .cmd)
			- Log files (.log)
			- And more text-based file types
			""";

	private String currentFilePath = "";

	private String currentContent = "";

	private String lastOperationResult = "";

	public static OpenAiApi.FunctionTool getToolDefinition() {
		OpenAiApi.FunctionTool.Function function = new OpenAiApi.FunctionTool.Function(TOOL_DESCRIPTION, TOOL_NAME,
				PARAMETERS);
		OpenAiApi.FunctionTool functionTool = new OpenAiApi.FunctionTool(function);
		return functionTool;
	}

	public static FunctionToolCallback getFunctionToolCallback(String workingDirectoryPath) {
		return FunctionToolCallback.builder(TOOL_NAME, new TextFileOperator(workingDirectoryPath))
			.description(TOOL_DESCRIPTION)
			.inputSchema(PARAMETERS)
			.inputType(String.class)
			.build();
	}

	public TextFileOperator(String workingDirectoryPath) {
		this.workingDirectoryPath = workingDirectoryPath;
	}

	public ToolExecuteResult run(String toolInput) {
		log.info("TextFileOperator toolInput:" + toolInput);
		try {
			Map<String, Object> toolInputMap = JSON.parseObject(toolInput, new TypeReference<Map<String, Object>>() {
			});

			String action = (String) toolInputMap.get("action");
			String filePath = (String) toolInputMap.get("file_path");
			this.currentFilePath = filePath;

			return switch (action) {
				case "open" -> openFile(filePath);
				case "replace" -> {
					String sourceText = (String) toolInputMap.get("source_text");
					String targetText = (String) toolInputMap.get("target_text");
					yield replaceText(sourceText, targetText);
				}
				case "get_text" -> getCurrentText();
				case "save" -> {
					String content = (String) toolInputMap.get("content");
					yield saveAndClose(content);
				}
				case "append" -> {
					String appendContent = (String) toolInputMap.get("content");
					yield appendToFile(appendContent);
				}
				case "count_words" -> countWords();
				default -> {
					this.lastOperationResult = "Error: Unknown action";
					yield new ToolExecuteResult("Unknown action: " + action);
				}
			};
		}
		catch (Exception e) {
			this.lastOperationResult = "Error: " + e.getMessage();
			return new ToolExecuteResult("Error: " + e.getMessage());
		}
	}

	private ToolExecuteResult openFile(String filePath) {
		try {
			// 检查文件类型
			String fileExtension = getFileExtension(filePath);
			if (!SUPPORTED_EXTENSIONS.contains(fileExtension.toLowerCase())) {
				this.lastOperationResult = "Error: Unsupported file type: " + fileExtension;
				return new ToolExecuteResult("Unsupported file type. Only text-based files are supported.");
			}

			Path absolutePath = validateAndGetAbsolutePath(filePath);

			// 如果文件不存在，先创建父目录
			if (!Files.exists(absolutePath)) {
				try {
					Files.createDirectories(absolutePath.getParent());
					Files.createFile(absolutePath);
					this.currentContent = "";
					this.lastOperationResult = "Success: New file created";
					return new ToolExecuteResult("New file created successfully: " + absolutePath);
				}
				catch (IOException e) {
					this.lastOperationResult = "Error: Failed to create file: " + e.getMessage();
					return new ToolExecuteResult("Failed to create file: " + e.getMessage());
				}
			}

			// 检查文件大小
			if (Files.size(absolutePath) > 10 * 1024 * 1024) { // 10MB limit
				this.lastOperationResult = "Error: File too large";
				return new ToolExecuteResult(
						"File is too large (>10MB). For safety reasons, please use a smaller file.");
			}

			this.currentContent = Files.readString(absolutePath);
			this.lastOperationResult = "Success: File opened";
			return new ToolExecuteResult("File opened successfully: " + absolutePath);
		}
		catch (IOException e) {
			this.lastOperationResult = "Error: " + e.getMessage();
			return new ToolExecuteResult("Error opening file: " + e.getMessage());
		}
	}

	/**
	 * 获取文件扩展名（包含点号）
	 */
	private String getFileExtension(String filePath) {
		int lastDotIndex = filePath.lastIndexOf('.');
		if (lastDotIndex > 0) {
			return filePath.substring(lastDotIndex).toLowerCase();
		}
		return "";
	}

	private ToolExecuteResult replaceText(String sourceText, String targetText) {
		if (this.currentContent.isEmpty()) {
			this.lastOperationResult = "Error: No file is currently open";
			return new ToolExecuteResult("Error: No file is currently open");
		}
		this.currentContent = this.currentContent.replace(sourceText, targetText);
		this.lastOperationResult = "Success: Text replaced";
		return new ToolExecuteResult("Text replaced successfully");
	}

	private ToolExecuteResult getCurrentText() {
		if (this.currentContent.isEmpty()) {
			this.lastOperationResult = "Error: No file is currently open";
			return new ToolExecuteResult("Error: No file is currently open");
		}
		this.lastOperationResult = "Success: Retrieved current text";
		return new ToolExecuteResult(this.currentContent);
	}

	private ToolExecuteResult saveAndClose(String content) {
		try {
			if (content != null) {
				this.currentContent = content;
			}
			Path absolutePath = validateAndGetAbsolutePath(this.currentFilePath);
			Files.writeString(absolutePath, this.currentContent);
			this.lastOperationResult = "Success: File saved";
			this.currentContent = "";
			return new ToolExecuteResult("File saved successfully: " + absolutePath);
		}
		catch (IOException e) {
			this.lastOperationResult = "Error: " + e.getMessage();
			return new ToolExecuteResult("Error saving file: " + e.getMessage());
		}
	}

	private ToolExecuteResult appendToFile(String content) {
		try {
			Path absolutePath = validateAndGetAbsolutePath(this.currentFilePath);
			if (this.currentContent.isEmpty()) {
				// If no file is open, read it first
				this.currentContent = Files.readString(absolutePath);
			}
			this.currentContent += "\n" + content;
			this.lastOperationResult = "Success: Content appended";
			return new ToolExecuteResult("Content appended successfully");
		}
		catch (IOException e) {
			this.lastOperationResult = "Error: " + e.getMessage();
			return new ToolExecuteResult("Error appending to file: " + e.getMessage());
		}
	}

	private ToolExecuteResult countWords() {
		if (this.currentContent.isEmpty()) {
			this.lastOperationResult = "Error: No file is currently open";
			return new ToolExecuteResult("Error: No file is currently open");
		}

		// 将多个空白字符替换为单个空格
		String processedText = this.currentContent;

		// 按空格分割并计数
		int wordCount = processedText.isEmpty() ? 0 : processedText.split("\\s+").length;

		this.lastOperationResult = "Success: Counted words";
		return new ToolExecuteResult(String.format("Total word count (including Markdown symbols): %d", wordCount));
	}

	/**
	 * 验证并获取文件的绝对路径，确保文件在工作目录范围内
	 */
	private Path validateAndGetAbsolutePath(String filePath) throws IOException {
		Path workingDir = Paths.get(workingDirectoryPath).toAbsolutePath().normalize();
		Path absolutePath = workingDir.resolve(filePath).normalize();

		// 检查文件是否在工作目录范围内
		if (!absolutePath.startsWith(workingDir)) {
			throw new IOException("Access denied: File path must be within working directory");
		}
		return absolutePath;
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

	private BaseAgent agent;

	@Override
	public void setAgent(BaseAgent agent) {
		this.agent = agent;
	}

	public BaseAgent getAgent() {
		return this.agent;
	}

	@Override
	public String getCurrentToolStateString() {
		return String.format("""
				Current Text File Operation State:
				- Working Directory:
				%s

				- Current File:
				%s
				- File Type: %s

				- Last Operation Result:
				%s
				""", workingDirectoryPath, currentFilePath.isEmpty() ? "No file open" : currentFilePath,
				currentFilePath.isEmpty() ? "N/A" : getFileExtension(currentFilePath),
				lastOperationResult.isEmpty() ? "No operation performed yet" : lastOperationResult);
	}

}
