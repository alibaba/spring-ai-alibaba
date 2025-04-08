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
package com.alibaba.cloud.ai.example.manus.tool.code;

import com.alibaba.cloud.ai.example.manus.agent.BaseAgent;
import com.alibaba.cloud.ai.example.manus.flow.PlanningFlow;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.cloud.ai.example.manus.tool.ToolCallBiFunctionDef;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.function.FunctionToolCallback;

public class PythonExecute implements ToolCallBiFunctionDef {

	private static final Logger log = LoggerFactory.getLogger(PythonExecute.class);

	private Boolean arm64 = true;

	public static final String LLMMATH_PYTHON_CODE = """
			import sys
			import math
			import numpy as np
			import numexpr as ne
			input = '%s'
			res = ne.evaluate(input)
			print(res)
			""";

	private static String PARAMETERS = """
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

	private static final String name = "python_execute";

	private static final String description = """
			Executes Python code string. Note: Only print outputs are visible, function return values are not captured. Use print statements to see results.
			""";

	public static OpenAiApi.FunctionTool getToolDefinition() {
		OpenAiApi.FunctionTool.Function function = new OpenAiApi.FunctionTool.Function(description, name, PARAMETERS);
		OpenAiApi.FunctionTool functionTool = new OpenAiApi.FunctionTool(function);
		return functionTool;
	}

	public static FunctionToolCallback getFunctionToolCallback() {
		return FunctionToolCallback.builder(name, new PythonExecute())
			.description(description)
			.inputSchema(PARAMETERS)
			.inputType(String.class)
			.build();
	}

	private InMemoryChatMemory chatMemory;

	private PlanningFlow planningFlow;

	private String lastCode = "";

	private String lastExecutionResult = "";

	private String lastExecutionLogId = "";

	private String lastError = "";

	private boolean hasError = false;

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
		log.info("PythonExecute toolInput:" + toolInput);
		Map<String, Object> toolInputMap = JSON.parseObject(toolInput, new TypeReference<Map<String, Object>>() {
		});
		String code = (String) toolInputMap.get("code");
		this.lastCode = code;
		this.lastExecutionLogId = "tmp_" + LogIdGenerator.generateUniqueId();

		try {
			CodeExecutionResult codeExecutionResult = CodeUtils.executeCode(code, "python", lastExecutionLogId + ".py",
					arm64, new HashMap<>());
			String result = codeExecutionResult.getLogs();
			this.lastExecutionResult = result;

			// 检查执行结果中是否包含 Python 错误信息
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

	private String extractErrorMessage(String output) {
		// 从 Python 错误输出中提取错误信息
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
		return description;
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

	@Override
	public void cleanup(String planId) {
		// do nothing
	}

}
