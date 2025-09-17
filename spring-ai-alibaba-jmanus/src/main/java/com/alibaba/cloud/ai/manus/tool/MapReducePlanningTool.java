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

import com.alibaba.cloud.ai.manus.runtime.entity.vo.ExecutionStep;
import com.alibaba.cloud.ai.manus.runtime.entity.vo.mapreduce.MapReduceExecutionPlan;
import com.alibaba.cloud.ai.manus.runtime.entity.vo.mapreduce.MapReduceNode;
import com.alibaba.cloud.ai.manus.runtime.entity.vo.mapreduce.SequentialNode;
import com.alibaba.cloud.ai.manus.tool.code.ToolExecuteResult;
import org.springframework.ai.openai.api.OpenAiApi.FunctionTool;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.function.Function;

public class MapReducePlanningTool
		implements Function<MapReducePlanningTool.MapReducePlanningInput, ToolExecuteResult>, PlanningToolInterface {

	private static final Logger log = LoggerFactory.getLogger(MapReducePlanningTool.class);

	private MapReduceExecutionPlan currentPlan;

	public String getCurrentPlanId() {
		return currentPlan != null ? currentPlan.getCurrentPlanId() : null;
	}

	public MapReduceExecutionPlan getCurrentPlan() {
		return currentPlan;
	}

	private static final String PARAMETERS = """
			{
			    "type": "object",
			    "properties": {
			        "command": {
			            "description": "MapReducePlanningTool accepts JSON format input to create execution plans. Required parameters include command (fixed as 'create'), planId as unique identifier, title as plan title, steps array containing execution nodes. Each node has a type field specifying either sequential (sequential execution) or mapreduce (distributed processing). Sequential nodes contain steps array, mapreduce nodes contain dataPreparedSteps (data preparation phase, serial execution), mapSteps (Map phase, parallel execution) and reduceSteps (Reduce phase, serial execution) arrays. Each step object contains stepRequirement describing specific task content, suggest using square brackets to indicate agent type. The tool automatically adds type prefixes to steps, supports combining multiple node types in the same plan to form complex workflows. All JSON keys must be in proper English format.",
			            "enum": [
			                "create"
			            ],
			            "type": "string"
			        },
			        "planId": {
			            "description": "Unique identifier for the plan",
			            "type": "string"
			        },
			        "title": {
			            "description": "Title of the plan",
			            "type": "string"
			        },
			        "steps": {
			            "description": "List of plan steps, supporting both sequential and MapReduce node types",
			            "type": "array",
			            "items": {
			                "type": "object",
			                "properties": {
			                    "type": {
			                        "description": "Node type: sequential (sequential execution) or mapreduce (MapReduce processing)",
			                        "enum": ["sequential", "mapreduce"],
			                        "type": "string"
			                    },
			                    "steps": {
			                        "description": "Step list for sequential nodes",
			                        "type": "array",
			                        "items": {
			                            "type": "object",
			                            "properties": {
			                                "stepRequirement": {
			                                    "description": "Step requirement description",
			                                    "type": "string"
			                                }
			                            },
			                            "required": ["stepRequirement"]
			                        }
			                    },
			                    "dataPreparedSteps": {
			                        "description": "Data preparation phase step list for MapReduce nodes, executed serially before Map phase, used for data preprocessing, splitting, cleaning and other preparation work",
			                        "type": "array",
			                        "items": {
			                            "type": "object",
			                            "properties": {
			                                "stepRequirement": {
			                                    "description": "Data preparation step requirement description",
			                                    "type": "string"
			                                }
			                            },
			                            "required": ["stepRequirement"]
			                        }
			                    },
			                    "mapSteps": {
			                        "description": "Map phase step list for MapReduce nodes, executed in parallel after data preparation phase",
			                        "type": "array",
			                        "items": {
			                            "type": "object",
			                            "properties": {
			                                "stepRequirement": {
			                                    "description": "Map step requirement description",
			                                    "type": "string"
			                                }
			                            },
			                            "required": ["stepRequirement"]
			                        }
			                    },
			                    "reduceSteps": {
			                        "description": "Reduce phase step list for MapReduce nodes",
			                        "type": "array",
			                        "items": {
			                            "type": "object",
			                            "properties": {
			                                "stepRequirement": {
			                                    "description": "Reduce step requirement description",
			                                    "type": "string"
			                                }
			                            },
			                            "required": ["stepRequirement"]
			                        }
			                    }
			                },
			                "required": ["type"]
			            }
			        }
			    },
			    "required": [
			        "command",
			        "planId",
			        "title",
			        "steps"
			    ]
			}
			""";

	private static final String name = "mapreduce_planning";

	private static final String description = "MapReduce planning tool for managing complex task execution plans supporting both sequential and MapReduce modes, with MapReduce mode including three phases: Data Preparation, Map, and Reduce";

	public FunctionTool getToolDefinition() {
		return new FunctionTool(new FunctionTool.Function(description, name, PARAMETERS));
	}

	public FunctionToolCallback<MapReducePlanningInput, ToolExecuteResult> getFunctionToolCallback() {
		return buildFunctionToolCallback(this);
	}

	/**
	 * Build FunctionToolCallback with common configuration
	 * @param toolInstance The tool instance to use
	 * @return Configured FunctionToolCallback
	 */
	private FunctionToolCallback<MapReducePlanningInput, ToolExecuteResult> buildFunctionToolCallback(
			PlanningToolInterface toolInstance) {
		return FunctionToolCallback
			.<MapReducePlanningInput, ToolExecuteResult>builder(name, (MapReducePlanningInput input) -> {
				if (toolInstance instanceof MapReducePlanningTool) {
					return ((MapReducePlanningTool) toolInstance).run(input);
				}
				throw new UnsupportedOperationException("Tool instance type not supported");
			})
			.description(description)
			.inputSchema(PARAMETERS)
			.inputType(MapReducePlanningInput.class)
			.toolMetadata(ToolMetadata.builder().returnDirect(true).build())
			.build();
	}

	@Override
	public FunctionToolCallback<?, ToolExecuteResult> getFunctionToolCallback(
			PlanningToolInterface planningToolInterface) {
		return buildFunctionToolCallback(planningToolInterface);
	}

	public MapReducePlanningTool() {
	}

	public ToolExecuteResult run(MapReducePlanningInput toolInput) {

		String command = toolInput.getCommand();
		String planId = toolInput.getPlanId();
		String title = toolInput.getTitle();
		List<Map<String, Object>> steps = toolInput.getSteps();

		return switch (command) {
			case "create" -> createMapReducePlan(planId, title, steps);
			default -> {
				log.info("Received invalid command: {}", command);
				throw new IllegalArgumentException("Invalid command: " + command);
			}
		};
	}

	/**
	 * Create MapReduce execution plan
	 * @param planId Plan ID
	 * @param title Plan title
	 * @param steps Step list
	 * @return Tool execution result
	 */
	public ToolExecuteResult createMapReducePlan(String planId, String title, List<Map<String, Object>> steps) {
		if (planId == null || title == null || steps == null || steps.isEmpty()) {
			log.info("Missing required parameters when creating MapReduce plan: planId={}, title={}, steps={}", planId,
					title, steps);
			return new ToolExecuteResult("Required parameters missing");
		}

		MapReduceExecutionPlan plan = new MapReduceExecutionPlan(planId, planId, title);

		for (Map<String, Object> stepNode : steps) {
			String nodeType = (String) stepNode.get("type");

			switch (nodeType) {
				case "sequential" -> processSequentialNode(plan, stepNode);
				case "mapreduce" -> processMapReduceNode(plan, stepNode);
				default -> {
					log.warn("Unknown node type: {}", nodeType);
					return new ToolExecuteResult("Unknown node type: " + nodeType);
				}
			}
		}

		this.currentPlan = plan;
		return new ToolExecuteResult(
				"MapReduce Plan created: " + planId + "\n" + plan.getPlanExecutionStateStringFormat(false));
	}

	/**
	 * Process sequential node
	 * @param plan Execution plan
	 * @param stepNode Step node
	 */
	private void processSequentialNode(MapReduceExecutionPlan plan, Map<String, Object> stepNode) {
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> sequentialSteps = (List<Map<String, Object>>) stepNode.get("steps");
		if (sequentialSteps == null)
			return;

		SequentialNode node = new SequentialNode();
		for (Map<String, Object> step : sequentialSteps) {
			ExecutionStep executionStep = createExecutionStepFromMap(step);
			node.addStep(executionStep);
		}

		plan.addSequentialNode(node);
	}

	/**
	 * Process MapReduce node
	 * @param plan Execution plan
	 * @param stepNode Step node
	 */
	private void processMapReduceNode(MapReduceExecutionPlan plan, Map<String, Object> stepNode) {
		MapReduceNode node = new MapReduceNode();

		// Process data preparation steps
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> dataPreparedSteps = (List<Map<String, Object>>) stepNode.get("dataPreparedSteps");
		if (dataPreparedSteps != null) {
			for (Map<String, Object> step : dataPreparedSteps) {
				ExecutionStep executionStep = createExecutionStepFromMap(step);
				node.addDataPreparedStep(executionStep);
			}
		}

		// Process Map steps
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> mapSteps = (List<Map<String, Object>>) stepNode.get("mapSteps");
		if (mapSteps != null) {
			for (Map<String, Object> step : mapSteps) {
				ExecutionStep executionStep = createExecutionStepFromMap(step);
				node.addMapStep(executionStep);
			}
		}

		// Process Reduce steps
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> reduceSteps = (List<Map<String, Object>>) stepNode.get("reduceSteps");
		if (reduceSteps != null) {
			for (Map<String, Object> step : reduceSteps) {
				ExecutionStep executionStep = createExecutionStepFromMap(step);
				node.addReduceStep(executionStep);
			}
		}

		// Process post-processing steps
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> postProcessSteps = (List<Map<String, Object>>) stepNode.get("postProcessSteps");
		if (postProcessSteps != null) {
			for (Map<String, Object> step : postProcessSteps) {
				ExecutionStep executionStep = createExecutionStepFromMap(step);
				node.addPostProcessStep(executionStep);
			}
		}

		plan.addMapReduceNode(node);
	}

	/**
	 * Create ExecutionStep from Map
	 * @param stepMap Step Map
	 * @return Created ExecutionStep instance
	 */
	private ExecutionStep createExecutionStepFromMap(Map<String, Object> stepMap) {
		String stepRequirement = (String) stepMap.get("stepRequirement");

		// Validate and sanitize stepRequirement to handle Chinese characters properly
		if (stepRequirement != null) {
			// Ensure the string is properly encoded and doesn't contain problematic
			// characters
			stepRequirement = sanitizeStepRequirement(stepRequirement);
		}

		ExecutionStep executionStep = new ExecutionStep();
		executionStep.setStepRequirement(stepRequirement);

		return executionStep;
	}

	/**
	 * Sanitize step requirement string to handle Chinese characters and special
	 * characters properly
	 * @param stepRequirement Original step requirement string
	 * @return Sanitized step requirement string
	 */
	private String sanitizeStepRequirement(String stepRequirement) {
		if (stepRequirement == null) {
			return null;
		}

		try {
			// Ensure proper UTF-8 encoding
			byte[] bytes = stepRequirement.getBytes("UTF-8");
			String sanitized = new String(bytes, "UTF-8");

			// Log if we detect Chinese characters for debugging
			if (containsChineseCharacters(sanitized)) {
				log.debug("Detected Chinese characters in stepRequirement: {}", sanitized);
			}

			return sanitized;
		}
		catch (Exception e) {
			log.warn("Failed to sanitize stepRequirement, using original: {}", e.getMessage());
			return stepRequirement;
		}
	}

	/**
	 * Check if string contains Chinese characters
	 * @param text Input text
	 * @return true if contains Chinese characters
	 */
	private boolean containsChineseCharacters(String text) {
		if (text == null) {
			return false;
		}

		for (char c : text.toCharArray()) {
			// Check for Chinese character ranges
			if ((c >= 0x4E00 && c <= 0x9FFF) || // CJK Unified Ideographs
					(c >= 0x3400 && c <= 0x4DBF) || // CJK Unified Ideographs Extension A
					(c >= 0x20000 && c <= 0x2A6DF)) { // CJK Unified Ideographs Extension
														// B
				return true;
			}
		}
		return false;
	}

	@Override
	public ToolExecuteResult apply(MapReducePlanningInput input) {
		return run(input);
	}

	public static class MapReducePlanningInput {

		private String command;

		private String planId;

		private String title;

		private List<Map<String, Object>> steps;

		public MapReducePlanningInput() {
		}

		public MapReducePlanningInput(String command, String planId, String title, List<Map<String, Object>> steps) {
			this.command = command;
			this.planId = planId;
			this.title = title;
			this.steps = steps;
		}

		// Getters and Setters
		public String getCommand() {
			return command;
		}

		public void setCommand(String command) {
			this.command = command;
		}

		public String getPlanId() {
			return planId;
		}

		public void setPlanId(String planId) {
			this.planId = planId;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public List<Map<String, Object>> getSteps() {
			return steps;
		}

		public void setSteps(List<Map<String, Object>> steps) {
			this.steps = steps;
		}

	}

}
