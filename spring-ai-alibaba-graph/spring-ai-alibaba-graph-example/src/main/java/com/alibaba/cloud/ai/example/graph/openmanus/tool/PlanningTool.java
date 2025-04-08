/*
 * Copyright 2024-2025 the original author or authors.
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
package com.alibaba.cloud.ai.example.graph.openmanus.tool;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlanningTool {

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

	private static final Plan plan = new Plan("1", List.of("step1", "step2", "step3"));

	private Map<String, Plan> plans;

	public PlanningTool(Map<String, Plan> plans) {
		this.plans = plans;
	}

	public void createPlan() {
		// create plan
	}

	public void updatePlan() {
		// update plan
	}

	public Plan getPlans(String id) {
		return plan;
		// return plans.get(id);
	}

}
