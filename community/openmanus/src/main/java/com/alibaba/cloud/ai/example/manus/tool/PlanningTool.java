/**
 * Copyright (C) 2024 AIDC-AI
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.example.manus.tool;

import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;

import org.springframework.ai.openai.api.OpenAiApi.FunctionTool;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.metadata.ToolMetadata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;

public class PlanningTool implements Function<String, ToolExecuteResult> {

	private static final Logger log = LoggerFactory.getLogger(PlanningTool.class);

	private static final String PARAMETERS = """
			{
			    "type": "object",
			    "properties": {
			        "command": {
			            "description": "The command to execute. Available commands: create, update, list, get, set_active, mark_step, delete.",
			            "enum": [
			                "create",
			                "update",
			                "list",
			                "get",
			                "set_active",
			                "mark_step",
			                "delete"
			            ],
			            "type": "string"
			        },
			        "plan_id": {
			            "description": "Unique identifier for the plan. Required for create, update, set_active, and delete commands. Optional for get and mark_step (uses active plan if not specified).",
			            "type": "string"
			        },
			        "title": {
			            "description": "Title for the plan. Required for create command, optional for update command.",
			            "type": "string"
			        },
			        "steps": {
			            "description": "List of plan steps. Required for create command, optional for update command.",
			            "type": "array",
			            "items": {
			                "type": "string"
			            }
			        },
			        "step_index": {
			            "description": "Index of the step to update (0-based). Required for mark_step command.",
			            "type": "integer"
			        },
			        "step_status": {
			            "description": "Status to set for a step. Used with mark_step command.",
			            "enum": ["not_started", "in_progress", "completed", "blocked"],
			            "type": "string"
			        },
			        "step_notes": {
			            "description": "Additional notes for a step. Optional for mark_step command.",
			            "type": "string"
			        }
			    },
			    "required": ["command"],
			    "additionalProperties": false
			}
			""";

	private static final String name = "planning";

	private static final String description = """
			A planning tool that allows the agent to create and manage plans for solving complex tasks.
			The tool provides functionality for creating plans, updating plan steps, and tracking progress.
			""";

	public static FunctionTool getToolDefinition() {
		FunctionTool.Function function = new FunctionTool.Function(description, name, PARAMETERS);
		FunctionTool functionTool = new FunctionTool(function);
		return functionTool;
	}

	public static FunctionToolCallback getFunctionToolCallback() {
		return FunctionToolCallback.builder(name, PlanningTool.INSTANCE)
			.description(description)
			.inputSchema(PARAMETERS)
			.inputType(String.class)
			.toolMetadata(ToolMetadata.builder().returnDirect(true).build())
			.build();
	}

	public static final PlanningTool INSTANCE = new PlanningTool();

	private Map<String, Map<String, Object>> plans = new HashMap<>();

	private String currentPlanId;

	public ToolExecuteResult run(String toolInput) {
		try {
			log.info("PlanningTool toolInput:" + toolInput);
			Map<String, Object> toolInputMap = JSON.parseObject(toolInput, new TypeReference<Map<String, Object>>() {
			});

			String command = null;
			if (toolInputMap.get("command") != null) {
				command = (String) toolInputMap.get("command");
			}
			String planId = null;
			if (toolInputMap.get("plan_id") != null) {
				planId = (String) toolInputMap.get("plan_id");
			}
			String title = null;
			if (toolInputMap.get("title") != null) {
				title = (String) toolInputMap.get("title");
			}
			List<String> steps = null;
			if (toolInputMap.get("steps") != null) {
				steps = (List<String>) toolInputMap.get("steps");
			}
			Integer stepIndex = null;
			if (toolInputMap.get("step_index") != null) {
				stepIndex = (Integer) toolInputMap.get("step_index");
			}
			String stepStatus = null;
			if (toolInputMap.get("step_status") != null) {
				stepStatus = (String) toolInputMap.get("step_status");
			}
			String stepNotes = null;
			if (toolInputMap.get("step_notes") != null) {
				stepNotes = (String) toolInputMap.get("step_notes");
			}

			switch (command) {
				case "create":
					return createPlan(planId, title, steps);
				case "update":
					return updatePlan(planId, title, steps);
				case "list":
					return listPlans();
				case "get":
					return getPlan(planId);
				case "set_active":
					return setActivePlan(planId);
				case "mark_step":
					return markStep(planId, stepIndex, stepStatus, stepNotes);
				case "delete":
					return deletePlan(planId);
				default:
					throw new RuntimeException("Unrecognized command: " + command
							+ ". Allowed commands are: create, update, list, get, set_active, mark_step, delete");
			}
		}
		catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	public ToolExecuteResult createPlan(String planId, String title, List<String> steps) {
		if (planId == null || planId.isEmpty()) {
			throw new RuntimeException("Parameter `plan_id` is required for command: create");
		}

		if (plans.containsKey(planId)) {
			throw new RuntimeException(
					"A plan with ID '" + planId + "' already exists. Use 'update' to modify existing plans.");
		}

		if (title == null || title.isEmpty()) {
			throw new RuntimeException("Parameter `title` is required for command: create");
		}

		if (steps == null || steps.isEmpty() || !steps.stream().allMatch(step -> step instanceof String)) {
			throw new RuntimeException("Parameter `steps` must be a non-empty list of strings for command: create");
		}

		Map<String, Object> plan = new HashMap<>();
		plan.put("plan_id", planId);
		plan.put("title", title);
		plan.put("steps", steps);
		plan.put("step_statuses", new ArrayList<>(Collections.nCopies(steps.size(), "not_started")));
		plan.put("step_notes", new ArrayList<>(Collections.nCopies(steps.size(), "")));

		plans.put(planId, plan);
		this.currentPlanId = planId; // Set as active plan

		return new ToolExecuteResult("Plan created successfully with ID: " + planId + "\n\n" + formatPlan(plan));
	}

	public ToolExecuteResult updatePlan(String planId, String title, List<String> steps) {
		if (planId == null || planId.isEmpty()) {
			throw new RuntimeException("Parameter `plan_id` is required for command: update");
		}

		if (!plans.containsKey(planId)) {
			throw new RuntimeException("No plan found with ID: " + planId);
		}

		Map<String, Object> plan = plans.get(planId);

		if (title != null && !title.isEmpty()) {
			plan.put("title", title);
		}

		if (steps != null) {
			if (!steps.stream().allMatch(step -> step instanceof String)) {
				throw new RuntimeException("Parameter `steps` must be a list of strings for command: update");
			}

			List<String> oldSteps = (List<String>) plan.get("steps");
			List<String> oldStatuses = (List<String>) plan.get("step_statuses");
			List<String> oldNotes = (List<String>) plan.get("step_notes");

			List<String> newStatuses = new ArrayList<>();
			List<String> newNotes = new ArrayList<>();

			for (int i = 0; i < steps.size(); i++) {
				String step = steps.get(i);
				if (i < oldSteps.size() && step.equals(oldSteps.get(i))) {
					newStatuses.add(oldStatuses.get(i));
					newNotes.add(oldNotes.get(i));
				}
				else {
					newStatuses.add("not_started");
					newNotes.add("");
				}
			}

			plan.put("steps", steps);
			plan.put("step_statuses", newStatuses);
			plan.put("step_notes", newNotes);
		}

		return new ToolExecuteResult("Plan updated successfully: " + planId + "\n\n" + formatPlan(plan));
	}

	public ToolExecuteResult listPlans() {
		if (plans.isEmpty()) {
			return new ToolExecuteResult("No plans available. Create a plan with the 'create' command.");
		}

		StringBuilder output = new StringBuilder("Available plans:\n");
		for (String planId : plans.keySet()) {
			Map<String, Object> plan = plans.get(planId);
			String currentMarker = planId.equals(currentPlanId) ? " (active)" : "";
			long completed = ((List<String>) plan.get("step_statuses")).stream()
				.filter(status -> "completed".equals(status))
				.count();
			int total = ((List<String>) plan.get("steps")).size();
			String progress = completed + "/" + total + " steps completed";
			output.append("• ")
				.append(planId)
				.append(currentMarker)
				.append(": ")
				.append(plan.get("title"))
				.append(" - ")
				.append(progress)
				.append("\n");
		}

		return new ToolExecuteResult(output.toString());
	}

	public ToolExecuteResult getPlan(String planId) {
		if (planId == null || planId.isEmpty()) {
			if (currentPlanId == null) {
				throw new RuntimeException("No active plan. Please specify a plan_id or set an active plan.");
			}
			planId = currentPlanId;
		}

		if (!plans.containsKey(planId)) {
			throw new RuntimeException("No plan found with ID: " + planId);
		}

		Map<String, Object> plan = plans.get(planId);
		return new ToolExecuteResult(formatPlan(plan));
	}

	public ToolExecuteResult setActivePlan(String planId) {
		if (planId == null || planId.isEmpty()) {
			throw new RuntimeException("Parameter `plan_id` is required for command: set_active");
		}

		if (!plans.containsKey(planId)) {
			throw new RuntimeException("No plan found with ID: " + planId);
		}

		currentPlanId = planId;
		return new ToolExecuteResult(
				"Plan '" + planId + "' is now the active plan.\n\n" + formatPlan(plans.get(planId)));
	}

	public ToolExecuteResult markStep(String planId, Integer stepIndex, String stepStatus, String stepNotes) {
		if (planId == null || planId.isEmpty()) {
			if (currentPlanId == null) {
				throw new RuntimeException("No active plan. Please specify a plan_id or set an active plan.");
			}
			planId = currentPlanId;
		}

		if (!plans.containsKey(planId)) {
			throw new RuntimeException("No plan found with ID: " + planId);
		}

		if (stepIndex == null) {
			throw new RuntimeException("Parameter `step_index` is required for command: mark_step");
		}

		Map<String, Object> plan = plans.get(planId);
		List<String> steps = (List<String>) plan.get("steps");

		if (stepIndex < 0 || stepIndex >= steps.size()) {
			throw new RuntimeException(
					"Invalid step_index: " + stepIndex + ". Valid indices range from 0 to " + (steps.size() - 1) + ".");
		}

		List<String> stepStatuses = (List<String>) plan.get("step_statuses");
		List<String> stepNotesList = (List<String>) plan.get("step_notes");

		if (stepStatus != null
				&& !Arrays.asList("not_started", "in_progress", "completed", "blocked").contains(stepStatus)) {
			throw new RuntimeException("Invalid step_status: " + stepStatus
					+ ". Valid statuses are: not_started, in_progress, completed, blocked");
		}

		if (stepStatus != null) {
			stepStatuses.set(stepIndex, stepStatus);
		}

		if (stepNotes != null) {
			stepNotesList.set(stepIndex, stepNotes);
		}

		String result = "Step " + stepIndex + " updated in plan '" + planId + "'.\n\n" + formatPlan(plan);
		log.info(result);
		return new ToolExecuteResult(result);
	}

	public ToolExecuteResult deletePlan(String planId) {
		if (planId == null || planId.isEmpty()) {
			throw new RuntimeException("Parameter `plan_id` is required for command: delete");
		}

		if (!plans.containsKey(planId)) {
			throw new RuntimeException("No plan found with ID: " + planId);
		}

		plans.remove(planId);

		if (planId.equals(currentPlanId)) {
			currentPlanId = null;
		}

		return new ToolExecuteResult("Plan '" + planId + "' has been deleted.");
	}

	private String formatPlan(Map<String, Object> plan) {
		StringBuilder output = new StringBuilder();
		String planTitle = (String) plan.get("title");
		String planId = (String) plan.get("plan_id");

		output.append("Plan: ").append(planTitle).append(" (ID: ").append(planId).append(")\n");
		output.append(repeatString("=", output.length())).append("\n\n");

		// Calculate progress statistics
		List<String> steps = (List<String>) plan.get("steps");
		List<String> stepStatuses = (List<String>) plan.get("step_statuses");
		List<String> stepNotes = (List<String>) plan.get("step_notes");

		int totalSteps = steps.size();
		long completed = stepStatuses.stream().filter(status -> "completed".equals(status)).count();
		long inProgress = stepStatuses.stream().filter(status -> "in_progress".equals(status)).count();
		long blocked = stepStatuses.stream().filter(status -> "blocked".equals(status)).count();
		long notStarted = stepStatuses.stream().filter(status -> "not_started".equals(status)).count();

		output.append("Progress: ").append(completed).append("/").append(totalSteps).append(" steps completed ");
		if (totalSteps > 0) {
			double percentage = (completed / (double) totalSteps) * 100;
			output.append(String.format("(%.1f%%)\n", percentage));
		}
		else {
			output.append("(0%)\n");
		}

		output.append("Status: ")
			.append(completed)
			.append(" completed, ")
			.append(inProgress)
			.append(" in progress, ")
			.append(blocked)
			.append(" blocked, ")
			.append(notStarted)
			.append(" not started\n\n");
		output.append("Steps:\n");

		// Add each step with its status and notes
		for (int i = 0; i < totalSteps; i++) {
			String step = steps.get(i);
			String status = stepStatuses.get(i);
			String notes = stepNotes.get(i);

			String statusSymbol;
			switch (status) {
				case "in_progress":
					statusSymbol = "[→]";
					break;
				case "completed":
					statusSymbol = "[✓]";
					break;
				case "blocked":
					statusSymbol = "[!]";
					break;
				default:
					statusSymbol = "[ ]";
			}

			output.append(i).append(". ").append(statusSymbol).append(" ").append(step).append("\n");
			if (notes != null && !notes.isEmpty()) {
				output.append("   Notes: ").append(notes).append("\n");
			}
		}

		return output.toString();
	}

	private String repeatString(String str, int times) {
		StringBuilder repeated = new StringBuilder();
		for (int i = 0; i < times; i++) {
			repeated.append(str);
		}
		return repeated.toString();
	}

	public Map<String, Map<String, Object>> getPlans() {
		return plans;
	}

	public void setPlans(Map<String, Map<String, Object>> plans) {
		this.plans = plans;
	}

	public String getCurrentPlanId() {
		return currentPlanId;
	}

	public void setCurrentPlanId(String currentPlanId) {
		this.currentPlanId = currentPlanId;
	}

	@Override
	public ToolExecuteResult apply(String s) {
		return run(s);
	}

}
