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

import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.openai.api.OpenAiApi;

import java.util.List;
import java.util.Map;

public class TerminateTool extends AbstractBaseTool<Map<String, Object>> implements TerminableTool {

	private static final Logger log = LoggerFactory.getLogger(TerminateTool.class);

	public static final String name = "terminate";

	private final List<String> columns;

	private String lastTerminationMessage = "";

	private boolean isTerminated = false;

	private String terminationTimestamp = "";

	public static OpenAiApi.FunctionTool getToolDefinition(List<String> columns) {
		String parameters = generateParametersJson(columns);
		String description = getDescriptions(columns);
		OpenAiApi.FunctionTool.Function function = new OpenAiApi.FunctionTool.Function(description, name, parameters);
		return new OpenAiApi.FunctionTool(function);
	}

	private static String getDescriptions(List<String> columns) {
		// Simple description to avoid generating overly long content
		return "Terminate the current execution step with structured data. "
				+ "Provide data in JSON format with 'columns' array and 'data' array containing rows of values.";
	}

	private static String generateParametersJson(List<String> columns) {
		String template = """
				{
				  "type": "object",
				  "properties": {
				    "columns": {
				      "type": "array",
				      "items": {"type": "string"},
				      "description": "Column names"
				    },
				    "data": {
				      "type": "array",
				      "items": {
				        "type": "array",
				        "items": {"type": "string"}
				      },
				      "description": "Data rows"
				    }
				  },
				  "required": ["columns", "data"]
				}
				""";

		return template;
	}

	@Override
	public String getCurrentToolStateString() {
		return String.format("""
				Termination Tool Status:
				- Current State: %s
				- Last Termination: %s
				- Termination Message: %s
				- Timestamp: %s
				- Plan ID: %s
				- Columns: %s
				""", isTerminated ? "🛑 Terminated" : "⚡ Active",
				isTerminated ? "Process was terminated" : "No termination recorded",
				lastTerminationMessage.isEmpty() ? "N/A" : lastTerminationMessage,
				terminationTimestamp.isEmpty() ? "N/A" : terminationTimestamp,
				currentPlanId != null ? currentPlanId : "N/A", columns != null ? String.join(", ", columns) : "N/A");
	}

	public TerminateTool(String planId, List<String> columns) {
		this.currentPlanId = planId;
		// If columns is null or empty, use "message" as default column
		this.columns = (columns == null || columns.isEmpty()) ? List.of("message") : columns;
	}

	@Override
	public ToolExecuteResult run(Map<String, Object> input) {
		log.info("Terminate with input: {}", input);

		// Extract message from the structured data
		String message = formatStructuredData(input);
		this.lastTerminationMessage = message;
		this.isTerminated = true;
		this.terminationTimestamp = java.time.LocalDateTime.now().toString();

		return new ToolExecuteResult(message);
	}

	private String formatStructuredData(Map<String, Object> input) {
		StringBuilder sb = new StringBuilder();
		sb.append("Structured termination data:\n");

		if (input.containsKey("columns") && input.containsKey("data")) {
			@SuppressWarnings("unchecked")
			List<String> inputColumns = (List<String>) input.get("columns");
			@SuppressWarnings("unchecked")
			List<List<Object>> inputData = (List<List<Object>>) input.get("data");

			sb.append("Columns: ").append(inputColumns).append("\n");
			sb.append("Data:\n");
			for (List<Object> row : inputData) {
				sb.append("  ").append(row).append("\n");
			}
		}
		else {
			sb.append(input.toString());
		}

		return sb.toString();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getDescription() {
		return getDescriptions(this.columns);
	}

	@Override
	public String getParameters() {
		return generateParametersJson(this.columns);
	}

	@Override
	public Class<Map<String, Object>> getInputType() {
		@SuppressWarnings("unchecked")
		Class<Map<String, Object>> clazz = (Class<Map<String, Object>>) (Class<?>) Map.class;
		return clazz;
	}

	@Override
	public boolean isReturnDirect() {
		return true;
	}

	@Override
	public void cleanup(String planId) {
		// do nothing
	}

	@Override
	public String getServiceGroup() {
		return "default-service-group";
	}

	// ==================== TerminableTool interface implementation ====================

	@Override
	public boolean canTerminate() {
		// TerminateTool can always be terminated as its purpose is to terminate execution
		return true;
	}

}
