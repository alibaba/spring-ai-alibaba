/*
 * Copyright 2025-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.graph.agent.agentscope;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.tools.ToolContextHelper;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.springframework.ai.chat.model.ToolContext;

import java.util.Map;

/**
 * AgentScope tool that reads graph state via ToolContextHelper and updates extraState.
 * Used to verify ToolContext integration in Graph + AgentScopeAgent scenarios.
 */
public class UpdateExtraStateTool {

	public static final String TOOL_NAME = "update_extra_state";

	@Tool(
			name = TOOL_NAME,
			description = "Update the extra state with an observation. Use this tool to record what you observe from the current graph state. Call this tool with a string describing your observation.")
	public String updateExtraState(
			@ToolParam(name = "observation", description = "A string describing what you observe from the current state") String observation,
			ToolContext toolContext) {
		// 1. Verify tool can read graph state
		OverAllState state = ToolContextHelper.getState(toolContext).orElse(null);
		String stateSummary = state != null ? summarizeState(state) : "no_state";

		// 2. Update extraState via stateForUpdate
		ToolContextHelper.getStateForUpdate(toolContext).ifPresent(update -> {
			update.put("extraState", Map.of(
					"observation", observation != null ? observation : "",
					"stateSummary", stateSummary,
					"updatedByTool", true));
		});

		return "Updated extraState with observation: " + observation + ", stateSummary: " + stateSummary;
	}

	private static String summarizeState(OverAllState state) {
		Map<String, Object> data = state.data();
		if (data == null || data.isEmpty()) {
			return "empty";
		}
		return "keys=" + data.keySet();
	}
}
