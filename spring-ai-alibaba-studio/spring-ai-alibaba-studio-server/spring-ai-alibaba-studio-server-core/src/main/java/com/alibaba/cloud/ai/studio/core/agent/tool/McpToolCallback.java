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

package com.alibaba.cloud.ai.studio.core.agent.tool;

import com.alibaba.cloud.ai.studio.runtime.domain.Result;
import com.alibaba.cloud.ai.studio.runtime.domain.chat.ToolCallType;
import com.alibaba.cloud.ai.studio.runtime.domain.mcp.McpServerCallToolRequest;
import com.alibaba.cloud.ai.studio.runtime.domain.mcp.McpServerCallToolResponse;
import com.alibaba.cloud.ai.studio.runtime.domain.mcp.McpServerDetail;
import com.alibaba.cloud.ai.studio.runtime.domain.mcp.McpTool;
import com.alibaba.cloud.ai.studio.runtime.utils.JsonUtils;
import com.alibaba.cloud.ai.studio.core.base.service.McpServerService;
import com.alibaba.cloud.ai.studio.core.context.RequestContextHolder;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

import java.util.Map;

/**
 * MCP tool callback implementation. Handles the execution of MCP tools and manages
 * tool-related operations.
 *
 * @since 1.0.0.3
 */
@RequiredArgsConstructor
public class McpToolCallback implements AgentToolCallback {

	/** Service for handling MCP server operations */
	private final McpServerService mcpServerService;

	/** Details of the MCP server */
	private final McpServerDetail mcpServerDetail;

	/** The MCP tool to be executed */
	private final McpTool tool;

	/** Additional parameters for tool execution */
	private final Map<String, Object> extraParams;

	/**
	 * Gets the tool definition including name, description and input schema
	 */
	@NotNull
	@Override
	public ToolDefinition getToolDefinition() {
		return ToolDefinition.builder()
			.name(tool.getName())
			.description(tool.getDescription())
			.inputSchema(JsonUtils.toJson(tool.getInputSchema()))
			.build();
	}

	/**
	 * Executes the tool with the given input parameters
	 * @param functionInput JSON string containing the input parameters
	 * @return JSON string containing the execution result
	 */
	@NotNull
	@Override
	public String call(@NotNull String functionInput) {
		String mcpServerId = mcpServerDetail.getServerCode();
		Map<String, Object> arguments = ToolArgumentsHelper.mergeToolArguments(functionInput, extraParams, mcpServerId);

		McpServerCallToolRequest request = new McpServerCallToolRequest();
		request.setServerCode(this.mcpServerDetail.getServerCode());
		request.setToolName(this.tool.getName());
		request.setWorkspaceId(RequestContextHolder.getRequestContext().getWorkspaceId());
		request.setToolParams(arguments);
		request.setRequestId(RequestContextHolder.getRequestContext().getRequestId());

		Result<McpServerCallToolResponse> result = mcpServerService.callTool(request);

		if (!result.isSuccess()) {
			return JsonUtils.toJson(result);
		}

		return JsonUtils.toJson(result.getData());
	}

	/**
	 * Gets the tool metadata configuration
	 */
	@NotNull
	@Override
	public ToolMetadata getToolMetadata() {
		return ToolMetadata.builder().returnDirect(true).build();
	}

	@Override
	public String getId() {
		return mcpServerDetail.getServerCode();
	}

	@Override
	public ToolCallType getToolCallType() {
		return ToolCallType.MCP_TOOL_CALL;
	}

}
