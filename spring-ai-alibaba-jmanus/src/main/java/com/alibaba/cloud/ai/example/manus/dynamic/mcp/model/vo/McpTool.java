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
package com.alibaba.cloud.ai.example.manus.dynamic.mcp.model.vo;

import com.alibaba.cloud.ai.example.manus.dynamic.mcp.service.McpStateHolderService;
import com.alibaba.cloud.ai.example.manus.tool.ToolCallBiFunctionDef;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;

import java.util.Map;

public class McpTool implements ToolCallBiFunctionDef<Map<String, Object>> {

	private final ToolCallback toolCallback;

	private String planId;

	private String serviceNameString;

	private McpStateHolderService mcpStateHolderService;

	public McpTool(ToolCallback toolCallback, String serviceNameString, String planId,
			McpStateHolderService mcpStateHolderService) {
		this.toolCallback = toolCallback;
		this.serviceNameString = serviceNameString;
		this.planId = planId;
		this.mcpStateHolderService = mcpStateHolderService;
	}

	@Override
	public String getName() {
		return toolCallback.getToolDefinition().name();
	}

	@Override
	public String getDescription() {
		return toolCallback.getToolDefinition().description();
	}

	@Override
	public String getParameters() {
		return toolCallback.getToolDefinition().inputSchema();
	}

	@Override
	public Class<Map<String, Object>> getInputType() {
		return (Class<Map<String, Object>>) (Class<?>) Map.class;
	}

	@Override
	public boolean isReturnDirect() {
		return false;
	}

	@Override
	public void setPlanId(String planId) {
		this.planId = planId;
	}

	@Override
	public String getCurrentToolStateString() {
		McpState mcpState = mcpStateHolderService.getMcpState(planId);
		if (mcpState != null) {
			return mcpState.getState();
		}
		return "";
	}

	@Override
	public ToolExecuteResult apply(Map<String, Object> inputMap, ToolContext toolContext) {
		// 将 Map 转换为 JSON 字符串，因为 ToolCallback 期望字符串输入
		ObjectMapper objectMapper = new ObjectMapper();
		String jsonInput;
		try {
			jsonInput = objectMapper.writeValueAsString(inputMap);
		}
		catch (JsonProcessingException e) {
			return new ToolExecuteResult("Error: Failed to serialize input to JSON - " + e.getMessage());
		}

		String result = toolCallback.call(jsonInput, toolContext);
		if (result == null) {
			result = "";
		}
		// 这里可以将结果存储到McpStateHolderService中
		McpState mcpState = mcpStateHolderService.getMcpState(planId);
		if (mcpState == null) {
			mcpState = new McpState();
			mcpStateHolderService.setMcpState(planId, mcpState);
		}
		mcpState.setState(result);

		return new ToolExecuteResult(result);
	}

	@Override
	public void cleanup(String planId) {
		mcpStateHolderService.removeMcpState(planId);
	}

	@Override
	public String getServiceGroup() {
		return serviceNameString;
	}

}
