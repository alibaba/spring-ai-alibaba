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
package com.alibaba.cloud.ai.manus.tool;

import com.alibaba.cloud.ai.manus.runtime.entity.vo.ExecutionPlan;
import com.alibaba.cloud.ai.manus.runtime.entity.vo.ExecutionStep;
import com.alibaba.cloud.ai.manus.tool.code.ToolExecuteResult;

import org.springframework.ai.openai.api.OpenAiApi.FunctionTool;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

public class PlanningTool extends AbstractBaseTool<PlanningTool.PlanningInput> implements PlanningToolInterface {

	private static final Logger log = LoggerFactory.getLogger(PlanningTool.class);

	private ExecutionPlan currentPlan;

	public PlanningTool() {
	}

	/**
	 * Internal input class for defining planning tool input parameters
	 */
	public static class PlanningInput {

		private String command;

		private String title;

		private List<String> steps;

		private String terminateColumns;

		private boolean directResponse = false;

		public PlanningInput() {
		}

		public PlanningInput(String command, String title, List<String> steps, boolean directResponse) {
			this.command = command;
			this.title = title;
			this.steps = steps;
			this.terminateColumns = null;
			this.directResponse = directResponse;
		}

		public String getCommand() {
			return command;
		}

		public void setCommand(String command) {
			this.command = command;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public List<String> getSteps() {
			return steps;
		}

		public void setSteps(List<String> steps) {
			this.steps = steps;
		}

		public String getTerminateColumns() {
			return terminateColumns;
		}

		public void setTerminateColumns(String terminateColumns) {
			this.terminateColumns = terminateColumns;
		}

		public boolean isDirectResponse() {
			return directResponse;
		}

		public void setDirectResponse(boolean directResponse) {
			this.directResponse = directResponse;
		}

	}

	public String getCurrentPlanId() {
		return currentPlan != null ? currentPlan.getCurrentPlanId() : null;
	}

	public void setCurrentPlanId(String planId) {
		if (currentPlan != null) {
			currentPlan.setCurrentPlanId(planId);
		}
	}

	public ExecutionPlan getCurrentPlan() {
		return currentPlan;
	}

	private static final String PARAMETERS = """
			{
			 "type": "object",
			 "properties": {
			  "command": {
			   "description": "create a execution plan , Available commands: create",
			   "enum": [
				   "create"
			   ],
			   "type": "string"
			  },
			  "title": {
			   "description": "Title for the plan",
			   "type": "string"
			  },
			  "steps": {
			   "description": "List of plan steps",
			   "type": "array",
			   "items": {
				   "type": "string"
			   }
			  },
			  "terminateColumns": {
				   "description": "Terminate structure output columns for all steps (optional, will be applied to every step)",
				   "type": "string"
			  },
			  "directResponse": {
				   "description": "Whether to use direct response mode (skip planning and respond directly)",
				   "type": "boolean"
			  }
			 },
			 "required": [
			"command",
			"title",
			"steps"
			 ]
			}
			""";

	private static final String name = "planning";

	private static final String description = "Planning tool for managing tasks ";

	public FunctionTool getToolDefinition() {
		return new FunctionTool(new FunctionTool.Function(description, name, PARAMETERS));
	}

	// Parameterized FunctionToolCallback with appropriate types.
	public static FunctionToolCallback<PlanningInput, ToolExecuteResult> getFunctionToolCallback(
			PlanningTool toolInstance) {
		return FunctionToolCallback.builder(name, toolInstance)
			.description(description)
			.inputSchema(PARAMETERS)
			.inputType(PlanningInput.class)
			.toolMetadata(ToolMetadata.builder().returnDirect(true).build())
			.build();
	}

	/**
	 * Build FunctionToolCallback with common configuration
	 * @param toolInstance The tool instance to use
	 * @return Configured FunctionToolCallback
	 */
	private FunctionToolCallback<PlanningInput, ToolExecuteResult> buildFunctionToolCallback(
			PlanningToolInterface toolInstance) {
		return FunctionToolCallback.<PlanningInput, ToolExecuteResult>builder(name, (PlanningInput input) -> {
			if (toolInstance instanceof PlanningTool) {
				return ((PlanningTool) toolInstance).run(input);
			}
			throw new UnsupportedOperationException("Tool instance type not supported");
		})
			.description(description)
			.inputSchema(PARAMETERS)
			.inputType(PlanningInput.class)
			.toolMetadata(ToolMetadata.builder().returnDirect(true).build())
			.build();
	}

	@Override
	public ToolExecuteResult run(PlanningInput input) {
		String command = input.getCommand();
		String title = input.getTitle();
		List<String> steps = input.getSteps();
		boolean directResponse = input.isDirectResponse();

		// Support directResponse mode
		if (directResponse) {
			log.info("Direct response mode enabled");
			ExecutionPlan plan = new ExecutionPlan(null, null, title);
			plan.setDirectResponse(true);
			plan.setUserRequest(title); // Here title is the user request content
			this.currentPlan = plan;
			return new ToolExecuteResult("Direct response mode: plan created successfully");
		}

		return switch (command) {
			case "create" -> createPlan(title, steps, input.getTerminateColumns());
			// case "update" -> updatePlan(planId, title, steps);
			// case "get" -> getPlan(planId);
			// case "mark_step" -> markStep(planId, stepIndex, stepStatus, stepNotes);
			// case "delete" -> deletePlan(planId);
			default -> {
				log.info("Received invalid command: {}", command);
				throw new IllegalArgumentException("Invalid command: " + command);
			}
		};
	}

	/**
	 * Create a single execution step
	 * @param step step description
	 * @param index step index
	 * @return created ExecutionStep instance
	 */
	private ExecutionStep createExecutionStep(String step, int index) {
		ExecutionStep executionStep = new ExecutionStep();
		executionStep.setStepRequirement(step);
		return executionStep;
	}

	public ToolExecuteResult createPlan(String title, List<String> steps, String terminateColumns) {
		if (title == null || steps == null || steps.isEmpty()) {
			log.info("Missing required parameters when creating plan: title={}, steps={}", title, steps);
			return new ToolExecuteResult("Required parameters missing");
		}

		ExecutionPlan plan = new ExecutionPlan(null, null, title);

		int index = 0;
		for (String step : steps) {
			ExecutionStep execStep = createExecutionStep(step, index);
			if (terminateColumns != null && !terminateColumns.isEmpty()) {
				execStep.setTerminateColumns(terminateColumns);
			}
			plan.addStep(execStep);
			index++;
		}

		this.currentPlan = plan;
		return new ToolExecuteResult("Plan created successfully\n" + plan.getPlanExecutionStateStringFormat(false));
	}

	// ToolCallBiFunctionDef interface methods
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
	public Class<PlanningInput> getInputType() {
		return PlanningInput.class;
	}

	@Override
	public boolean isReturnDirect() {
		return true;
	}

	@Override
	public String getCurrentToolStateString() {
		if (currentPlan != null) {
			return "Current plan: " + currentPlan.getPlanExecutionStateStringFormat(false);
		}
		return "No active plan";
	}

	@Override
	public void cleanup(String planId) {
		// Implementation can be added if needed
	}

	@Override
	public String getServiceGroup() {
		return "default-service-group";
	}

	// PlanningToolInterface methods
	@Override
	public FunctionToolCallback<PlanningInput, ToolExecuteResult> getFunctionToolCallback() {
		return buildFunctionToolCallback(this);
	}

	@Override
	public FunctionToolCallback<PlanningInput, ToolExecuteResult> getFunctionToolCallback(
			PlanningToolInterface planningToolInterface) {
		return buildFunctionToolCallback(planningToolInterface);
	}

}
