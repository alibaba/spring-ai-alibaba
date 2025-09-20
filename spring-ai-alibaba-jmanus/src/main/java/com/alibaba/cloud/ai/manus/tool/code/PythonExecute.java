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
package com.alibaba.cloud.ai.manus.tool.code;

import com.alibaba.cloud.ai.manus.tool.AbstractBaseTool;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class PythonExecute extends AbstractBaseTool<PythonExecute.PythonInput> {

	private final ObjectMapper objectMapper;

	private static final Logger log = LoggerFactory.getLogger(PythonExecute.class);

	/**
	 * Internal input class for defining input parameters of Python execution tool
	 */
	public static class PythonInput {

		private String code;

		public PythonInput() {
		}

		public PythonInput(String code) {
			this.code = code;
		}

		public String getCode() {
			return code;
		}

		public void setCode(String code) {
			this.code = code;
		}

	}

	private Boolean arm64 = true;

	private static final String name = "python_execute";

	private String lastCode = "";

	private String lastExecutionResult = "";

	private String lastExecutionLogId = "";

	private String lastError = "";

	private boolean hasError = false;

	public PythonExecute(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public String getCurrentToolStateString() {
		return String.format("""
				Python Executor Status:
				- Runtime Environment: Python3
				(%s)

				- Recent Code Execution:
				%s

				- Execution Status:
				%s

				- Error Details:
				%s

				- Execution Log ID:
				%s

				- Execution Output:
				%s
				""", arm64 ? "ARM64" : "x86_64",
				lastCode.isEmpty() ? "No code executed yet" : String.format("Last executed:\n%s", lastCode),
				hasError ? "❌ Failed with errors" : "✅ Success", hasError ? lastError : "No errors",
				lastExecutionLogId.isEmpty() ? "N/A" : lastExecutionLogId, !lastExecutionResult.isEmpty()
						? lastExecutionResult : (hasError ? "Execution failed due to errors" : "No output available"));
	}

	public ToolExecuteResult run(String toolInput) {
		log.info("PythonExecute toolInput:{}", toolInput);
		try {
			// Add exception handling for JSON deserialization
			Map<String, Object> toolInputMap = objectMapper.readValue(toolInput,
					new TypeReference<Map<String, Object>>() {
					});
			String code = (String) toolInputMap.get("code");
			this.lastCode = code;
			this.lastExecutionLogId = "tmp_" + LogIdGenerator.generateUniqueId();

			try {
				CodeExecutionResult codeExecutionResult = CodeUtils.executeCode(code, "python",
						lastExecutionLogId + ".py", arm64, new HashMap<>());
				String result = codeExecutionResult.getLogs();
				this.lastExecutionResult = result;

				// Check if the execution result contains Python error information
				if (result.contains("SyntaxError") || result.contains("IndentationError")
						|| result.contains("NameError") || result.contains("TypeError") || result.contains("ValueError")
						|| result.contains("ImportError")) {
					this.hasError = true;
					this.lastError = extractErrorMessage(result);
				}
				else {
					this.hasError = false;
					this.lastError = "";
				}

				return new ToolExecuteResult(result);
			}
			catch (Exception e) {
				this.hasError = true;
				this.lastError = e.getMessage();
				this.lastExecutionResult = "Execution failed: " + e.getMessage();
				return new ToolExecuteResult("Execution failed: " + e.getMessage());
			}
		}
		catch (Exception e) {
			log.error("Error deserializing JSON", e);
			return new ToolExecuteResult("Error deserializing JSON: " + e.getMessage());
		}
	}

	private String extractErrorMessage(String output) {
		// Extract error information from Python error output
		String[] lines = output.split("\n");
		StringBuilder errorMsg = new StringBuilder();
		boolean foundError = false;

		for (String line : lines) {
			if (line.contains("Error:") || foundError) {
				foundError = true;
				errorMsg.append(line).append("\n");
			}
		}

		return errorMsg.length() > 0 ? errorMsg.toString().trim() : "Unknown error occurred";
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getDescription() {
		return "Executes Python code string. Note: Only print outputs are visible, function return values are not captured. Use print statements to see results.";
	}

	@Override
	public String getParameters() {
		return """
				{
				    "type": "object",
				    "properties": {
				        "code": {
				            "type": "string",
				            "description": "The Python code to execute."
				        }
				    },
				    "required": ["code"]
				}
				""";
	}

	@Override
	public Class<PythonInput> getInputType() {
		return PythonInput.class;
	}

	@Override
	public ToolExecuteResult run(PythonInput input) {
		String code = input.getCode();
		log.info("PythonExecute code: {}", code);

		this.lastCode = code;
		this.lastExecutionLogId = "tmp_" + LogIdGenerator.generateUniqueId();

		try {
			CodeExecutionResult codeExecutionResult = CodeUtils.executeCode(code, "python", lastExecutionLogId + ".py",
					arm64, new HashMap<>());
			String result = codeExecutionResult.getLogs();
			this.lastExecutionResult = result;

			// Check if the execution result contains Python error information
			if (result.contains("SyntaxError") || result.contains("IndentationError") || result.contains("NameError")
					|| result.contains("TypeError") || result.contains("ValueError")
					|| result.contains("ImportError")) {
				this.hasError = true;
				this.lastError = extractErrorMessage(result);
			}
			else {
				this.hasError = false;
				this.lastError = "";
			}

			return new ToolExecuteResult(result);
		}
		catch (Exception e) {
			this.hasError = true;
			this.lastError = e.getMessage();
			this.lastExecutionResult = "Execution failed: " + e.getMessage();
			return new ToolExecuteResult("Execution failed: " + e.getMessage());
		}
	}

	@Override
	public void cleanup(String planId) {
		// do nothing
	}

	@Override
	public String getServiceGroup() {
		return "default-service-group";
	}

}
