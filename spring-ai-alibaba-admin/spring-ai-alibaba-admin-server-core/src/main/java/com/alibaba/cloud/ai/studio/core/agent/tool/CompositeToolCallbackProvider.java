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

import com.alibaba.cloud.ai.studio.core.agent.skill.WorkspaceSkillRegistry;
import com.alibaba.cloud.ai.studio.core.base.manager.AppComponentManager;
import com.alibaba.cloud.ai.studio.core.base.service.McpServerService;
import com.alibaba.cloud.ai.studio.core.base.service.PluginService;
import com.alibaba.cloud.ai.studio.core.base.service.SkillService;
import com.alibaba.cloud.ai.studio.core.base.service.ToolExecutionService;
import com.alibaba.cloud.ai.studio.core.config.StudioProperties;
import com.alibaba.cloud.ai.studio.runtime.domain.app.AgentConfig;
import com.alibaba.cloud.ai.studio.runtime.domain.mcp.McpQuery;
import com.alibaba.cloud.ai.studio.runtime.domain.mcp.McpServerDetail;
import com.alibaba.cloud.ai.studio.runtime.domain.mcp.McpTool;
import com.alibaba.cloud.ai.studio.runtime.domain.plugin.Tool;
import com.alibaba.cloud.ai.studio.runtime.domain.skill.Skill;
import com.alibaba.cloud.ai.studio.runtime.domain.tool.ToolCallSchema;
import com.alibaba.cloud.ai.studio.runtime.enums.AppComponentTypeEnum;
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
 * Composite tool callback provider for plugins, MCP, app components and skills.
 *
 * @since 1.0.0.3
 */
@Slf4j
@RequiredArgsConstructor
public class CompositeToolCallbackProvider implements ToolCallbackProvider {

	private final AgentConfig agentConfig;

	private final PluginService pluginService;

	private final ToolExecutionService toolExecutionService;

	private final McpServerService mcpServerService;

	private final AppComponentManager appComponentManager;

	private final SkillService skillService;

	private final StudioProperties studioProperties;

	@Getter
	private final Map<String, Object> extraParams;

	private List<ToolCallback> toolCallbacks;

	private WorkspaceSkillRegistry skillRegistry;

	@NotNull
	@Override
	public ToolCallback[] getToolCallbacks() {
		if (toolCallbacks != null) {
			return toolCallbacks.toArray(new ToolCallback[0]);
		}

		toolCallbacks = new ArrayList<>();

		List<AgentConfig.Tool> pluginTools = agentConfig.getTools();
		if (!CollectionUtils.isEmpty(pluginTools)) {
			addToolCallbacks(toolCallbacks, buildPluginToolCallbacks(pluginTools));
		}

		List<AgentConfig.McpServer> mcpServers = agentConfig.getMcpServers();
		if (!CollectionUtils.isEmpty(mcpServers)) {
			addToolCallbacks(toolCallbacks, buildMcpToolCallbacks(mcpServers));
		}

		List<String> agentComponents = agentConfig.getAgentComponents();
		if (!CollectionUtils.isEmpty(agentComponents)) {
			addToolCallbacks(toolCallbacks, buildAppComponentCallbacks(agentComponents, AppComponentTypeEnum.Agent));
		}

		List<String> workflowComponents = agentConfig.getWorkflowComponents();
		if (!CollectionUtils.isEmpty(workflowComponents)) {
			addToolCallbacks(toolCallbacks,
					buildAppComponentCallbacks(workflowComponents, AppComponentTypeEnum.Workflow));
		}

		List<AgentConfig.SkillRef> skills = agentConfig.getSkills();
		if (!CollectionUtils.isEmpty(skills)) {
			addToolCallbacks(toolCallbacks, buildSkillToolCallbacks(skills));
		}

		return toolCallbacks.toArray(new ToolCallback[0]);
	}

	public WorkspaceSkillRegistry getSkillRegistry() {
		if (skillRegistry == null && !CollectionUtils.isEmpty(agentConfig.getSkills())) {
			getToolCallbacks();
		}
		return skillRegistry;
	}

	private void validateToolCallbacks(ToolCallback[] toolCallbacks) {
		List<String> duplicateToolNames = ToolUtils.getDuplicateToolNames(toolCallbacks);
		if (!duplicateToolNames.isEmpty()) {
			throw new IllegalStateException(
					"Multiple tools with the same name (%s)".formatted(String.join(", ", duplicateToolNames)));
		}
	}

	public static List<ToolCallback> toolCallbacks(AgentConfig config, PluginService pluginService,
			ToolExecutionService toolExecutionService, McpServerService mcpServerService,
			AppComponentManager appComponentManager, SkillService skillService, StudioProperties studioProperties,
			Map<String, Object> extraParams) {
		CompositeToolCallbackProvider provider = new CompositeToolCallbackProvider(config, pluginService,
				toolExecutionService, mcpServerService, appComponentManager, skillService, studioProperties,
				extraParams);
		ToolCallback[] toolCallbacks = provider.getToolCallbacks();
		if (ArrayUtils.isEmpty(toolCallbacks)) {
			return List.of();
		}
		return List.of(toolCallbacks);
	}

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

	private List<ToolCallback> buildPluginToolCallbacks(List<AgentConfig.Tool> pluginTools) {
		List<String> toolIds = pluginTools.stream().map(AgentConfig.Tool::getId).toList();
		if (CollectionUtils.isEmpty(toolIds)) {
			return List.of();
		}

		List<Tool> tools = pluginService.getTools(toolIds);
		if (CollectionUtils.isEmpty(tools)) {
			return List.of();
		}

		List<ToolCallback> callbacks = new ArrayList<>();
		tools.forEach(tool -> callbacks.add(new PluginToolCallback(toolExecutionService, tool, extraParams)));
		return callbacks;
	}

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

		List<ToolCallback> callbacks = new ArrayList<>();
		for (McpServerDetail mcpServerDetail : mcpServerDetails) {
			if (!CollectionUtils.isEmpty(mcpServerDetail.getTools())) {
				for (McpTool tool : mcpServerDetail.getTools()) {
					callbacks.add(new McpToolCallback(mcpServerService, mcpServerDetail, tool, extraParams));
				}
			}
		}
		return callbacks;
	}

	private List<ToolCallback> buildAppComponentCallbacks(List<String> agentComponents,
			AppComponentTypeEnum componentType) {
		if (CollectionUtils.isEmpty(agentComponents)) {
			return List.of();
		}

		Map<String, ToolCallSchema> toolCallSchemaMap = appComponentManager.getToolCallSchema(agentComponents);
		if (CollectionUtils.isEmpty(toolCallSchemaMap)) {
			return List.of();
		}

		List<ToolCallback> callbacks = new ArrayList<>();
		toolCallSchemaMap.forEach((key, value) -> callbacks
			.add(new AppComponentToolCallback(appComponentManager, key, value, extraParams, componentType)));
		return callbacks;
	}

	private List<ToolCallback> buildSkillToolCallbacks(List<AgentConfig.SkillRef> skillRefs) {
		List<String> skillIds = skillRefs.stream().map(AgentConfig.SkillRef::getId).filter(Objects::nonNull).toList();
		if (CollectionUtils.isEmpty(skillIds)) {
			return List.of();
		}
		List<Skill> skills = skillService.getSkills(skillIds);
		this.skillRegistry = new WorkspaceSkillRegistry(studioProperties, skills);
		if (skillRegistry.isEmpty()) {
			return List.of();
		}
		return List.of(new SkillToolCallback(skillRegistry), new SkillResourceToolCallback(skillRegistry));
	}

}
