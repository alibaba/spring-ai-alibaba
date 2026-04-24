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
package com.alibaba.cloud.ai.examples.multiagents.handoffs.singleagent.tools;

import com.alibaba.cloud.ai.examples.multiagents.handoffs.singleagent.support.SupportStateConstants;
import com.alibaba.cloud.ai.graph.agent.tools.ToolContextHelper;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.Arrays;
import java.util.List;

/**
 * Tools for the customer support handoffs workflow. Implemented with {@link ToolCallbacks#from(Object...)},
 * which creates {@link org.springframework.ai.tool.method.MethodToolCallback} from @Tool-annotated methods.
 * State-updating tools write to the graph state via {@link ToolContextHelper#getStateForUpdate(ToolContext)}.
 */
public final class SupportTools {

	private static final SupportTools INSTANCE = new SupportTools();

	private static final List<ToolCallback> TOOLS = Arrays.asList(ToolCallbacks.from(INSTANCE));

	private SupportTools() {
	}

	@Tool(name = "record_warranty_status", description = "Record the customer's warranty status and transition to issue classification.")
	public String recordWarrantyStatus(
			@ToolParam(description = "in_warranty or out_of_warranty") String status,
			ToolContext toolContext) {
		ToolContextHelper.getStateForUpdate(toolContext).ifPresent(update -> {
			update.put(SupportStateConstants.WARRANTY_STATUS, status);
			update.put(SupportStateConstants.CURRENT_STEP, SupportStateConstants.STEP_ISSUE_CLASSIFIER);
		});
		return "Warranty status recorded as: " + status;
	}

	@Tool(name = "record_issue_type", description = "Record the type of issue (hardware or software) and transition to resolution specialist.")
	public String recordIssueType(
			@ToolParam(description = "hardware or software") String issueType,
			ToolContext toolContext) {
		ToolContextHelper.getStateForUpdate(toolContext).ifPresent(update -> {
			update.put(SupportStateConstants.ISSUE_TYPE, issueType);
			update.put(SupportStateConstants.CURRENT_STEP, SupportStateConstants.STEP_RESOLUTION_SPECIALIST);
		});
		return "Issue type recorded as: " + issueType;
	}

	@Tool(name = "provide_solution", description = "Provide a solution to the customer's issue.")
	public String provideSolution(
			@ToolParam(description = "The solution to provide") String solution,
			ToolContext toolContext) {
		return "Solution provided: " + solution;
	}

	@Tool(name = "escalate_to_human", description = "Escalate the case to a human support specialist.")
	public String escalateToHuman(
			@ToolParam(description = "Reason for escalation") String reason,
			ToolContext toolContext) {
		return "Escalating to human support. Reason: " + reason;
	}

	private static ToolCallback findByName(String name) {
		return TOOLS.stream()
				.filter(t -> name.equals(t.getToolDefinition().name()))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Unknown tool: " + name));
	}

	/** All support tools as a list (e.g. for agent builder). */
	public static List<ToolCallback> allTools() {
		return TOOLS;
	}

	public static ToolCallback recordWarrantyStatusTool() {
		return findByName("record_warranty_status");
	}

	public static ToolCallback recordIssueTypeTool() {
		return findByName("record_issue_type");
	}

	public static ToolCallback provideSolutionTool() {
		return findByName("provide_solution");
	}

	public static ToolCallback escalateToHumanTool() {
		return findByName("escalate_to_human");
	}
}
