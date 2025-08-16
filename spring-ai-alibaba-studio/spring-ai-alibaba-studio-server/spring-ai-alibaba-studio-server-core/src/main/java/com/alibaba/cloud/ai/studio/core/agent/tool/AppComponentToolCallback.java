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
import com.alibaba.cloud.ai.studio.runtime.domain.agent.AgentResponse;
import com.alibaba.cloud.ai.studio.runtime.domain.chat.ToolCallType;
import com.alibaba.cloud.ai.studio.runtime.domain.component.AppComponentRequest;
import com.alibaba.cloud.ai.studio.runtime.domain.tool.ToolCallSchema;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.debug.WorkflowResponse;
import com.alibaba.cloud.ai.studio.runtime.utils.JsonUtils;
import com.alibaba.cloud.ai.studio.core.base.manager.AppComponentManager;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

import java.util.Map;

/**
 * Tool callback implementation for app components. Handles the execution of agent and
 * workflow components.
 *
 * @since 1.0.0.3
 */
@RequiredArgsConstructor
public class AppComponentToolCallback implements AgentToolCallback {

	/** Manager for handling app component operations */
	private final AppComponentManager appComponentManager;

	/** Unique identifier for the app component */
	private final String appComponentId;

	/** Schema defining the tool's call structure */
	private final ToolCallSchema toolCallSchema;

	/** Additional parameters for tool execution */
	private final Map<String, Object> extraParams;

	/** Type of the app component (Agent or Workflow) */
	private final AppComponentTypeEnum componentType;

	/**
	 * Returns the tool definition based on the tool call schema.
	 */
	@NotNull
	@Override
	public ToolDefinition getToolDefinition() {
		return ToolDefinition.builder()
			.name(toolCallSchema.getName())
			.description(toolCallSchema.getDescription())
			.inputSchema(JsonUtils.toJson(toolCallSchema.getInputSchema()))
			.build();
	}

	/**
	 * Executes the tool with the given input. Merges extra parameters if available and
	 * processes based on component type.
	 */
	@NotNull
	@Override
	public String call(@NotNull String toolInput) {
		Map<String, Object> arguments = ToolArgumentsHelper.mergeToolArguments(toolInput, extraParams, appComponentId);

		AppComponentRequest request = new AppComponentRequest();
		request.setBizVars(arguments);
		request.setCode(appComponentId);
		request.setStreamMode(false);
		request.setType(componentType.getValue());

		if (componentType == AppComponentTypeEnum.Agent) {
			AgentResponse response = appComponentManager.executeAgentComponent(request);
			if (!response.isSuccess()) {
				return JsonUtils.toJson(response.getError());
			}

			return String.valueOf(response.getMessage().getContent());
		}
		else if (componentType == AppComponentTypeEnum.Workflow) {
			WorkflowResponse response = appComponentManager.executeWorkflowComponent(request);
			if (!response.isSuccess()) {
				return JsonUtils.toJson(response.getError());
			}

			return String.valueOf(response.getMessage().getContent());
		}
		else {
			throw new IllegalArgumentException("unknown component type: " + componentType.getValue());
		}
	}

	/**
	 * Returns tool metadata indicating direct return behavior.
	 */
	@NotNull
	@Override
	public ToolMetadata getToolMetadata() {
		return ToolMetadata.builder().returnDirect(true).build();
	}

	@Override
	public String getId() {
		return appComponentId;
	}

	@Override
	public ToolCallType getToolCallType() {
		return ToolCallType.COMPONENT_TOOL_CALL;
	}

}
