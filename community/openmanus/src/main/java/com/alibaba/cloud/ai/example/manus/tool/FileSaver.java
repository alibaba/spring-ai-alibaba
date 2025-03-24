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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.function.Function;

import com.alibaba.cloud.ai.example.manus.tool.support.ToolExecuteResult;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.function.FunctionToolCallback;

public class FileSaver implements Function<String, ToolExecuteResult> {

	private static final Logger log = LoggerFactory.getLogger(FileSaver.class);

	private static String PARAMETERS = """
			{
			    "type": "object",
			    "properties": {
			        "content": {
			            "type": "string",
			            "description": "(required) The content to save to the file."
			        },
			        "file_path": {
			            "type": "string",
			            "description": "(required) The path where the file should be saved, including filename and extension."
			        }
			    },
			    "required": ["content", "file_path"]
			}
			""";

	private static final String name = "file_saver";

	private static final String description = """
			Save content to a local file at a specified path.
			Use this tool when you need to save text, code, or generated content to a file on the local filesystem.
			The tool accepts content and a file path, and saves the content to that location.
			""";

	public static OpenAiApi.FunctionTool getToolDefinition() {
		OpenAiApi.FunctionTool.Function function = new OpenAiApi.FunctionTool.Function(description, name, PARAMETERS);
		OpenAiApi.FunctionTool functionTool = new OpenAiApi.FunctionTool(function);
		return functionTool;
	}

	public static FunctionToolCallback getFunctionToolCallback() {
		return FunctionToolCallback.builder(name, new FileSaver())
			.description(description)
			.inputSchema(PARAMETERS)
			.inputType(String.class)
			.build();
	}

	public ToolExecuteResult run(String toolInput) {
		log.info("FileSaver toolInput:" + toolInput);
		try {
			Map<String, Object> toolInputMap = JSON.parseObject(toolInput, new TypeReference<Map<String, Object>>() {
			});
			String content = (String) toolInputMap.get("content");
			String filePath = (String) toolInputMap.get("file_path");
			File file = new File(filePath);
			File directory = file.getParentFile();
			if (directory != null && !directory.exists()) {
				directory.mkdirs();
			}

			try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
				writer.write(content);
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}

			return new ToolExecuteResult("Content successfully saved to " + filePath);
		}
		catch (Throwable e) {
			return new ToolExecuteResult("Error saving file: " + e.getMessage());
		}
	}

	@Override
	public ToolExecuteResult apply(String s) {
		return run(s);
	}

}
