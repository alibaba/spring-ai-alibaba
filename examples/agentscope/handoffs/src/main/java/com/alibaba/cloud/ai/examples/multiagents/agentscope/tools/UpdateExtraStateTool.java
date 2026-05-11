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
package com.alibaba.cloud.ai.examples.multiagents.agentscope.tools;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.tools.ToolContextHelper;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.springframework.ai.chat.model.ToolContext;

import java.util.Map;

/**
 * Example AgentScope tool that demonstrates how to read graph state and update extraState.
 * <p>
 * Key patterns:
 * <ul>
 *   <li><b>Read state:</b> {@link ToolContextHelper#getState(ToolContext)} returns current graph state</li>
 *   <li><b>Update state:</b> {@link ToolContextHelper#getStateForUpdate(ToolContext)} returns a map;
 *       put keys (e.g. {@code extraState}) that get merged into graph state when the node completes</li>
 *   <li><b>ToolContext:</b> Auto-injected when the tool is invoked from AgentScopeAgent (no @ToolParam)</li>
 * </ul>
 * <p>
 * The graph must declare {@code extraState} in its key strategies (e.g. ReplaceStrategy) for updates to apply.
 */
public final class UpdateExtraStateTool {

	public static final String TOOL_NAME = "update_extra_state";

	private UpdateExtraStateTool() {
	}

	@Tool(
			name = TOOL_NAME,
			description = "Update the extra state with an observation. Use this tool to record what you observe from the current graph state.")
	public String updateExtraState(
			@ToolParam(name = "observation", description = "A string describing what you observe from the current state") String observation,
			ToolContext toolContext) {
		// 1. Read graph state
		OverAllState state = ToolContextHelper.getState(toolContext).orElse(null);
		String stateSummary = state != null ? summarizeState(state) : "no_state";

		// 2. Update extraState via stateForUpdate (merged when node completes)
		ToolContextHelper.getStateForUpdate(toolContext).ifPresent(update ->
				update.put("extraState", Map.of(
						"observation", observation != null ? observation : "",
						"stateSummary", stateSummary,
						"updatedByTool", true)));

		return "Updated extraState with observation: " + observation + ", stateSummary: " + stateSummary;
	}

	private static String summarizeState(OverAllState state) {
		Map<String, Object> data = state.data();
		if (data == null || data.isEmpty()) {
			return "empty";
		}
		return "keys=" + data.keySet();
	}

	public static UpdateExtraStateTool create() {
		return new UpdateExtraStateTool();
	}
}
