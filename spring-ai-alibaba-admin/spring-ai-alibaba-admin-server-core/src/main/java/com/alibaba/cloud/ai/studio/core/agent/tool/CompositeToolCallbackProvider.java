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

import com.alibaba.cloud.ai.studio.runtime.enums.AppComponentTypeEnum;
import com.alibaba.cloud.ai.studio.runtime.domain.app.AgentConfig;
import com.alibaba.cloud.ai.studio.runtime.domain.mcp.McpQuery;
import com.alibaba.cloud.ai.studio.runtime.domain.mcp.McpServerDetail;
import com.alibaba.cloud.ai.studio.runtime.domain.mcp.McpTool;
import com.alibaba.cloud.ai.studio.runtime.domain.plugin.Tool;
import com.alibaba.cloud.ai.studio.runtime.domain.tool.ToolCallSchema;
import com.alibaba.cloud.ai.studio.core.base.service.McpServerService;
import com.alibaba.cloud.ai.studio.core.base.service.PluginService;
import com.alibaba.cloud.ai.studio.core.base.service.ToolExecutionService;
import com.alibaba.cloud.ai.studio.core.base.manager.AppComponentManager;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.support.ToolUtils;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A composite tool callback provider that manages and provides tool callbacks from
 * multiple sources: plugins, MCP servers, and application components.
 *
 * @since 1.0.0.3
 */
@Slf4j
@RequiredArgsConstructor
public class CompositeToolCallbackProvider implements ToolCallbackProvider {

	/** Configuration for the agent */
	private final AgentConfig agentConfig;

	/** Service for managing plugins */
	private final PluginService pluginService;

	/** Service for executing tools */
	private final ToolExecutionService toolExecutionService;

	/** Service for managing MCP servers */
	private final McpServerService mcpServerService;

	/** Manager for application components */
	private final AppComponentManager appComponentManager;

	/** Additional parameters for tool execution */
	@Getter
	private final Map<String, Object> extraParams;

	private List<ToolCallback> toolCallbacks;

	@NotNull
	@Override
	public ToolCallback[] getToolCallbacks() {
		// cache tool callbacks info for performance issue.
		if (toolCallbacks != null) {
			return toolCallbacks.toArray(new ToolCallback[0]);
		}

		toolCallbacks = new ArrayList<>();

		// build plugin tools
		List<AgentConfig.Tool> pluginTools = agentConfig.getTools();
		if (!CollectionUtils.isEmpty(pluginTools)) {
			List<ToolCallback> pluginToolCallbacks = buildPluginToolCallbacks(pluginTools);
			addToolCallbacks(toolCallbacks, pluginToolCallbacks);
		}

		// build mcp tools
		List<AgentConfig.McpServer> mcpServers = agentConfig.getMcpServers();
		if (!CollectionUtils.isEmpty(mcpServers)) {
			List<ToolCallback> mcpToolCallbacks = buildMcpToolCallbacks(mcpServers);
			addToolCallbacks(toolCallbacks, mcpToolCallbacks);
		}

		// build agent components
		List<String> agentComponents = agentConfig.getAgentComponents();
		if (!CollectionUtils.isEmpty(agentComponents)) {
			List<ToolCallback> agentComponentCallbacks = buildAppComponentCallbacks(agentComponents,
					AppComponentTypeEnum.Agent);
			addToolCallbacks(toolCallbacks, agentComponentCallbacks);
		}

		// build workflow components
		List<String> workflowComponents = agentConfig.getWorkflowComponents();
		if (!CollectionUtils.isEmpty(workflowComponents)) {
			List<ToolCallback> workflowComponentCallbacks = buildAppComponentCallbacks(workflowComponents,
					AppComponentTypeEnum.Workflow);
			addToolCallbacks(toolCallbacks, workflowComponentCallbacks);
		}

		return toolCallbacks.toArray(new ToolCallback[0]);
	}

	/**
	 * Validates that there are no duplicate tool names in the provided callbacks.
	 * <p>
	 * This method ensures that each tool has a unique name, which is required for proper
	 * tool resolution and execution.
	 * @param toolCallbacks the tool callbacks to validate
	 * @throws IllegalStateException if duplicate tool names are found
	 */
	private void validateToolCallbacks(ToolCallback[] toolCallbacks) {
		List<String> duplicateToolNames = ToolUtils.getDuplicateToolNames(toolCallbacks);
		if (!duplicateToolNames.isEmpty()) {
			throw new IllegalStateException(
					"Multiple tools with the same name (%s)".formatted(String.join(", ", duplicateToolNames)));
		}
	}

	/**
	 * Creates a list of tool callbacks based on the provided configuration and services.
	 */
	public static List<ToolCallback> toolCallbacks(AgentConfig config, PluginService pluginService,
			ToolExecutionService toolExecutionService, McpServerService mcpServerService,
			AppComponentManager appComponentManager, Map<String, Object> extraParams) {
		CompositeToolCallbackProvider provider = new CompositeToolCallbackProvider(config, pluginService,
				toolExecutionService, mcpServerService, appComponentManager, extraParams);
		ToolCallback[] toolCallbacks = provider.getToolCallbacks();
		if (ArrayUtils.isEmpty(toolCallbacks)) {
			return List.of();
		}

		return List.of(toolCallbacks);
	}

	/**
	 * Adds new tool callbacks to the list, skipping any duplicates.
	 */
	private void addToolCallbacks(List<ToolCallback> toolCallbacks, List<ToolCallback> newToolCallbacks) {
		Set<String> existingNames = toolCallbacks.stream()
			.map(callback -> callback.getToolDefinition().name())
			.collect(Collectors.toSet());

		newToolCallbacks.stream().filter(toolCallback -> {
			String toolName = toolCallback.getToolDefinition().name();
			if (existingNames.contains(toolName)) {
				log.warn("Duplicate tool name found: {}, skipping...", toolName);
				return false;
			}
			existingNames.add(toolName);
			return true;
		}).forEach(toolCallbacks::add);
	}

	/**
	 * Builds tool callbacks from plugin tools.
	 */
	private List<ToolCallback> buildPluginToolCallbacks(List<AgentConfig.Tool> pluginTools) {
		List<String> toolIds = pluginTools.stream().map(AgentConfig.Tool::getId).toList();
		if (CollectionUtils.isEmpty(toolIds)) {
			return List.of();
		}

		List<Tool> tools = pluginService.getTools(toolIds);
		if (CollectionUtils.isEmpty(tools)) {
			return List.of();
		}

		List<ToolCallback> toolCallbacks = new ArrayList<>();
		tools.forEach(tool -> {
			toolCallbacks.add(new PluginToolCallback(toolExecutionService, tool, extraParams));
		});

		return toolCallbacks;
	}

	/**
	 * Builds tool callbacks from MCP server tools.
	 */
	private List<ToolCallback> buildMcpToolCallbacks(List<AgentConfig.McpServer> mcpServers) {
		if (CollectionUtils.isEmpty(mcpServers)) {
			return List.of();
		}

		List<String> serverCodes = mcpServers.stream().map(AgentConfig.McpServer::getId).toList();
		List<McpServerDetail> mcpServerDetails = mcpServerService
			.listByCodes(McpQuery.builder().needTools(true).serverCodes(serverCodes).build());

		if (CollectionUtils.isEmpty(mcpServerDetails)) {
			return List.of();
		}

		List<ToolCallback> toolCallbacks = new ArrayList<>();
		for (McpServerDetail mcpServerDetail : mcpServerDetails) {
			if (!CollectionUtils.isEmpty(mcpServerDetail.getTools())) {
				for (McpTool tool : mcpServerDetail.getTools()) {
					toolCallbacks.add(new McpToolCallback(mcpServerService, mcpServerDetail, tool, extraParams));
				}
			}
		}

		return toolCallbacks;
	}

	/**
	 * Builds tool callbacks from application components.
	 */
	private List<ToolCallback> buildAppComponentCallbacks(List<String> agentComponents,
			AppComponentTypeEnum componentType) {
		if (CollectionUtils.isEmpty(agentComponents)) {
			return List.of();
		}

		Map<String, ToolCallSchema> toolCallSchemaMap = appComponentManager.getToolCallSchema(agentComponents);
		if (CollectionUtils.isEmpty(toolCallSchemaMap)) {
			return List.of();
		}

		List<ToolCallback> toolCallbacks = new ArrayList<>();
		toolCallSchemaMap.forEach((key, value) -> {
			toolCallbacks
				.add(new AppComponentToolCallback(appComponentManager, key, value, extraParams, componentType));
		});

		return toolCallbacks;
	}

}
