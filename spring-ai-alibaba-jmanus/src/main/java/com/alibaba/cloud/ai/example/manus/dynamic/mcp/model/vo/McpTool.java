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
import com.alibaba.cloud.ai.example.manus.tool.AbstractBaseTool;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;
import com.alibaba.cloud.ai.example.manus.tool.innerStorage.ISmartContentSavingService;
import com.alibaba.cloud.ai.example.manus.tool.innerStorage.SmartContentSavingService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.tool.ToolCallback;

import java.util.Map;

public class McpTool extends AbstractBaseTool<Map<String, Object>> {

	private final ToolCallback toolCallback;

	private String serviceNameString;

	private McpStateHolderService mcpStateHolderService;

	private ISmartContentSavingService smartContentSavingService;

	public McpTool(ToolCallback toolCallback, String serviceNameString, String planId,
			McpStateHolderService mcpStateHolderService, ISmartContentSavingService smartContentSavingService) {
		this.toolCallback = toolCallback;
		this.serviceNameString = serviceNameString;
		this.currentPlanId = planId;
		this.mcpStateHolderService = mcpStateHolderService;
		this.smartContentSavingService = smartContentSavingService;
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
	public String getCurrentToolStateString() {
		McpState mcpState = mcpStateHolderService.getMcpState(currentPlanId);
		if (mcpState != null) {
			return mcpState.getState();
		}
		return "";
	}

	@Override
	public ToolExecuteResult run(Map<String, Object> inputMap) {
		// Convert Map to JSON string, as ToolCallback expects string input
		ObjectMapper objectMapper = new ObjectMapper();
		String jsonInput;
		try {
			jsonInput = objectMapper.writeValueAsString(inputMap);
		}
		catch (JsonProcessingException e) {
			return new ToolExecuteResult("Error: Failed to serialize input to JSON - " + e.getMessage());
		}

		String result = toolCallback.call(jsonInput, null);
		if (result == null) {
			result = "";
		}

		SmartContentSavingService.SmartProcessResult smartProcessResult = smartContentSavingService
			.processContent(currentPlanId, result, getName());
		result = smartProcessResult.getSummary();
		// Here we can store the result to McpStateHolderService
		McpState mcpState = mcpStateHolderService.getMcpState(currentPlanId);
		if (mcpState == null) {
			mcpState = new McpState();
			mcpStateHolderService.setMcpState(currentPlanId, mcpState);
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
