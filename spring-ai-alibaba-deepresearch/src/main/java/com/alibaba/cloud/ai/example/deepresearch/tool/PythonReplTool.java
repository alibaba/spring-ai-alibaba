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

package com.alibaba.cloud.ai.example.deepresearch.tool;

import lombok.SneakyThrows;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Logger;

@Service
public class PythonReplTool {

	private static final Logger logger = Logger.getLogger(PythonReplTool.class.getName());

	@Value("${spring.ai.alibaba.deepreserch.python-home}")
	private String pythonHome;

	@SneakyThrows
	@Tool(description = "Execute Python code and return the result.")
	public String executePythonCode(@ToolParam(description = "python code") String code) {
		if (code == null || code.trim().isEmpty()) {
			return "Error: Code must be a non-empty string.";
		}

		try {
			// 写入临时文件
			java.nio.file.Path tempScript = java.nio.file.Files.createTempFile("script", ".py");
			java.nio.file.Files.write(tempScript, code.getBytes());

			// 调用 Python 执行
			ProcessBuilder pb = new ProcessBuilder(pythonHome, tempScript.toString());
			Process process = pb.start();

			// 读取标准输出
			BufferedReader stdOutput = new BufferedReader(new InputStreamReader(process.getInputStream()));
			BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));

			StringBuilder output = new StringBuilder();
			String line;

			while ((line = stdOutput.readLine()) != null) {
				output.append(line).append("\n");
			}

			StringBuilder error = new StringBuilder();
			while ((line = stdError.readLine()) != null) {
				error.append(line).append("\n");
			}

			int exitCode = process.waitFor();

			if (exitCode == 0) {
				logger.info("Python code executed successfully.");
				return "Successfully executed:\n```\n" + code + "\n```\nStdout:\n" + output.toString();
			}
			else {
				logger.warning("Python code execution failed.");
				return "Error executing code:\n```\n" + code + "\n```\nError:\n" + error.toString();
			}
		}
		catch (IOException | InterruptedException e) {
			logger.severe("Exception during execution: " + e.getMessage());
			return "Exception occurred while executing code:\n```\n" + code + "\n```\nError:\n" + e.toString();
		}
	}

}
