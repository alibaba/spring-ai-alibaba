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

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.metadata.ToolMetadata;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

public class TerminateTool implements ToolCallBiFunctionDef {

	private static final Logger log = LoggerFactory.getLogger(TerminateTool.class);

	private static String PARAMETERS = """
			{
			  "type" : "object",
			  "properties" : {
			    "message" : {
			      "type" : "string",
			      "description" : "终结当前步骤的信息，你需要在这个终结信息里尽可能多的包含所有相关的事实和数据，详细描述执行结果和状态，包含所有收集到的相关事实和数据，关键发现和观察。这个终结信息将作为当前步骤的最终输出，并且应该足够全面，以便为后续步骤或其他代理提供完整的上下文与关键事实。如果没有定义outputColumns，则使用此字段作为默认输出。"
			    },
			    "tableData" : {
			      "type" : "array",
			      "description" : "当定义了outputColumns时，使用此字段输出结构化的表格数据。数组中的每个对象表示一行数据，对象的键必须严格匹配outputColumns中定义的列名。",
			      "items" : {
			        "type" : "object",
			        "description" : "表格中的一行数据，键值对的键必须严格对应outputColumns中定义的列名",
			        "additionalProperties" : {
			          "type" : "string"
			        }
			      }
			    }
			  },
			  "required" : [ "message" ]
			}
			""";

	public static final String name = "terminate";

	private static final String description = """

			终结当前执行步骤，提供全面的总结消息。

			**输出字段说明：**

			1. **message（始终必需）：** 提供全面的总结，包含详细的执行结果、关键发现和上下文信息。

			2. **tableData（当定义了outputColumns时）：** 额外提供与预定义列匹配的结构化表格数据。

			**当定义了outputColumns时：**
			- 优先使用，'tableData'（用于结构化数据），message 则可输出为空字符串 : ""
			- 'tableData' 应该是一个对象数组，每个对象代表表格中的一行数据
			- 对象的键必须完全匹配outputColumns中定义的列名
			- 不允许额外的列，所有定义的列都必须存在
			- 示例：如果outputColumns = "col1,col2"，那么tableData = [{"col1":"value1","col2":"value2"},{"col1":"value3","col2":"value4"}]

			**当没有定义outputColumns时：**
			- 只使用 'message' 字段进行所有输出

			**message字段的要求：**
			- 始终包含详细的执行结果和状态
			- 包含所有收集到的相关事实和数据
			- 提供关键发现和观察结果
			- 添加重要的见解和结论
			- 包含任何可操作的建议

			输出应该足够全面，为后续步骤或其他代理提供完整的上下文。

			""";

	public static OpenAiApi.FunctionTool getToolDefinition() {
		OpenAiApi.FunctionTool.Function function = new OpenAiApi.FunctionTool.Function(description, name, PARAMETERS);
		return new OpenAiApi.FunctionTool(function);
	}

	public static FunctionToolCallback<String, ToolExecuteResult> getFunctionToolCallback(String planId) {
		return FunctionToolCallback.builder(name, new TerminateTool(planId))
			.description(description)
			.inputSchema(PARAMETERS)
			.inputType(String.class)
			.toolMetadata(ToolMetadata.builder().returnDirect(true).build())
			.build();
	}

	public static FunctionToolCallback<String, ToolExecuteResult> getFunctionToolCallback(String planId,
			String outputColumns) {
		return FunctionToolCallback.builder(name, new TerminateTool(planId, outputColumns))
			.description(description)
			.inputSchema(PARAMETERS)
			.inputType(String.class)
			.toolMetadata(ToolMetadata.builder().returnDirect(true).build())
			.build();
	}

	private String planId;

	private String lastTerminationMessage = "";

	private boolean isTerminated = false;

	private String terminationTimestamp = "";

	private List<Map<String, String>> lastTableData = null;

	private String outputColumns;

	private static final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public String getCurrentToolStateString() {
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("""
				Termination Tool Status:
				- Current State: %s
				- Last Termination: %s
				- Termination Message: %s
				- Timestamp: %s
				""", isTerminated ? "🛑 Terminated" : "⚡ Active",
				isTerminated ? "Process was terminated" : "No termination recorded",
				lastTerminationMessage.isEmpty() ? "N/A" : lastTerminationMessage,
				terminationTimestamp.isEmpty() ? "N/A" : terminationTimestamp));

		// 添加列输出信息
		if (outputColumns != null && !outputColumns.trim().isEmpty()) {
			sb.append("- Output Columns: ").append(outputColumns).append("\n");
		}
		else {
			sb.append("- Output Columns: N/A\n");
		}

		// 添加表格数据信息
		if (lastTableData != null && !lastTableData.isEmpty()) {
			sb.append("- Table Data (").append(lastTableData.size()).append(" rows):\n");
			for (int i = 0; i < Math.min(3, lastTableData.size()); i++) {
				Map<String, String> row = lastTableData.get(i);
				sb.append("  Row ").append(i + 1).append(": ").append(row).append("\n");
			}
			if (lastTableData.size() > 3) {
				sb.append("  ... and ").append(lastTableData.size() - 3).append(" more rows\n");
			}
		}
		else {
			sb.append("- Table Data: N/A\n");
		}

		return sb.toString();
	}

	public TerminateTool(String planId) {
		this.planId = planId;
	}

	public TerminateTool(String planId, String outputColumns) {
		this.planId = planId;
		this.outputColumns = outputColumns;
	}

	public void setOutputColumns(String outputColumns) {
		this.outputColumns = outputColumns;
	}

	public String getOutputColumns() {
		return outputColumns;
	}

	public ToolExecuteResult run(String toolInput) {
		log.info("Terminate toolInput: {}", toolInput);

		try {
			// 尝试解析 JSON 输入
			Map<String, Object> inputMap = objectMapper.readValue(toolInput, new TypeReference<Map<String, Object>>() {
			});

			String message = (String) inputMap.get("message");
			if (message == null) {
				message = toolInput; // 如果不是 JSON 格式，直接使用原始输入
			}

			// 处理表格数据
			@SuppressWarnings("unchecked")
			List<Map<String, String>> tableData = (List<Map<String, String>>) inputMap.get("tableData");

			this.lastTerminationMessage = message;
			this.lastTableData = tableData;
			this.isTerminated = true;
			this.terminationTimestamp = java.time.LocalDateTime.now().toString();

			// 构建返回消息
			StringBuilder resultMessage = new StringBuilder();

			// 如果有 outputColumns 定义且有表格数据，则格式化表格输出
			if (outputColumns != null && !outputColumns.trim().isEmpty() && tableData != null && !tableData.isEmpty()) {
				resultMessage.append("Structured Output:\n");

				// 解析列名
				List<String> columnNames = Arrays.asList(outputColumns.split(","));
				for (int i = 0; i < columnNames.size(); i++) {
					columnNames.set(i, columnNames.get(i).trim());
				}

				// 验证表格数据是否符合列定义
				boolean isValidTable = validateTableData(tableData, columnNames);
				if (!isValidTable) {
					resultMessage.append("Warning: Table data does not match the defined columns.\n");
				}

				// 格式化表格
				resultMessage.append(formatTableOutput(tableData, columnNames));

				// 添加原始消息作为补充
				if (message != null && !message.trim().isEmpty()) {
					resultMessage.append("\n\nAdditional Information:\n").append(message);
				}
			}
			else {
				// 没有 outputColumns 或表格数据，使用默认的 message 输出
				resultMessage.append(message);
			}

			return new ToolExecuteResult(resultMessage.toString());

		}
		catch (Exception e) {
			// 如果 JSON 解析失败，按原来的方式处理
			log.warn("Failed to parse JSON input, treating as plain text: {}", e.getMessage());
			this.lastTerminationMessage = toolInput;
			this.lastTableData = null;
			this.isTerminated = true;
			this.terminationTimestamp = java.time.LocalDateTime.now().toString();
			return new ToolExecuteResult(toolInput);
		}
	}

	/**
	 * 验证表格数据是否符合列定义
	 */
	private boolean validateTableData(List<Map<String, String>> tableData, List<String> columnNames) {
		for (Map<String, String> row : tableData) {
			// 检查行是否包含所有必需的列
			if (!row.keySet().containsAll(columnNames)) {
				return false;
			}
			// 检查行是否只包含定义的列（不允许额外的列）
			if (!columnNames.containsAll(row.keySet())) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 格式化表格输出
	 */
	private String formatTableOutput(List<Map<String, String>> tableData, List<String> columnNames) {
		StringBuilder sb = new StringBuilder();

		// 表头
		sb.append(String.join(" | ", columnNames)).append("\n");
		sb.append(String.join(" | ",
				columnNames.stream().map(name -> "-".repeat(Math.max(name.length(), 3))).toArray(String[]::new)))
			.append("\n");

		// 数据行
		for (Map<String, String> row : tableData) {
			List<String> values = new ArrayList<>();
			for (String columnName : columnNames) {
				String value = row.getOrDefault(columnName, "");
				values.add(value);
			}
			sb.append(String.join(" | ", values)).append("\n");
		}

		return sb.toString();
	}

	@Override
	public ToolExecuteResult apply(String s, ToolContext toolContext) {
		return run(s);
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
		return true;
	}

	@Override
	public void setPlanId(String planId) {
		this.planId = planId;
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
