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
package com.alibaba.cloud.ai.example.manus.tool;

import com.alibaba.cloud.ai.example.manus.planning.model.vo.mapreduce.MapReduceExecutionPlan;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.mapreduce.SequentialNode;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.mapreduce.MapReduceNode;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionStep;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.ai.openai.api.OpenAiApi.FunctionTool;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.function.Function;

public class MapReducePlanningTool implements Function<String, ToolExecuteResult>, PlanningToolInterface {

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
			            "description": "MapReducePlanningTool 接受JSON格式输入创建执行计划，必需参数包括command固定为create、planId作为唯一标识、title作为计划标题、steps数组包含执行节点，每个节点有type字段指定sequential顺序执行或mapreduce分布式处理两种类型，sequential节点包含steps数组，mapreduce节点包含dataPreparedSteps（数据准备阶段，串行执行）、mapSteps（Map阶段，并行执行）和reduceSteps（Reduce阶段，串行执行）数组，每个步骤对象包含stepRequirement描述具体任务内容建议用方括号标明代理类型，工具会自动为步骤添加类型前缀，支持在同一计划中组合多种节点类型形成复杂工作流。 所有的jsonkey都必须是完全符合要求的英文",
			            "enum": [
			                "create"
			            ],
			            "type": "string"
			        },
			        "planId": {
			            "description": "计划的唯一标识符",
			            "type": "string"
			        },
			        "title": {
			            "description": "计划的标题",
			            "type": "string"
			        },
			        "steps": {
			            "description": "计划步骤列表，支持顺序和MapReduce两种节点类型",
			            "type": "array",
			            "items": {
			                "type": "object",
			                "properties": {
			                    "type": {
			                        "description": "节点类型：sequential（顺序）、mapreduce（MapReduce）",
			                        "enum": ["sequential", "mapreduce"],
			                        "type": "string"
			                    },
			                    "steps": {
			                        "description": "顺序节点的步骤列表",
			                        "type": "array",
			                        "items": {
			                            "type": "object",
			                            "properties": {
			                                "stepRequirement": {
			                                    "description": "步骤要求描述",
			                                    "type": "string"
			                                }
			                            },
			                            "required": ["stepRequirement"]
			                        }
			                    },
			                    "dataPreparedSteps": {
			                        "description": "MapReduce节点的数据准备阶段步骤列表，在Map阶段之前串行执行，用于数据预处理、分割、清洗等准备工作",
			                        "type": "array",
			                        "items": {
			                            "type": "object",
			                            "properties": {
			                                "stepRequirement": {
			                                    "description": "数据准备步骤要求描述",
			                                    "type": "string"
			                                }
			                            },
			                            "required": ["stepRequirement"]
			                        }
			                    },
			                    "mapSteps": {
			                        "description": "MapReduce节点的Map阶段步骤列表，在数据准备阶段之后并行执行",
			                        "type": "array",
			                        "items": {
			                            "type": "object",
			                            "properties": {
			                                "stepRequirement": {
			                                    "description": "Map步骤要求描述",
			                                    "type": "string"
			                                }
			                            },
			                            "required": ["stepRequirement"]
			                        }
			                    },
			                    "reduceSteps": {
			                        "description": "MapReduce节点的Reduce阶段步骤列表",
			                        "type": "array",
			                        "items": {
			                            "type": "object",
			                            "properties": {
			                                "stepRequirement": {
			                                    "description": "Reduce步骤要求描述",
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

	private static final String description = "MapReduce计划工具，用于管理支持顺序和MapReduce模式的复杂任务执行计划，MapReduce模式包含数据准备（DataPrepared）、Map和Reduce三个阶段";

	public FunctionTool getToolDefinition() {
		return new FunctionTool(new FunctionTool.Function(description, name, PARAMETERS));
	}

	public FunctionToolCallback<String, ToolExecuteResult> getFunctionToolCallback() {
		return FunctionToolCallback.builder(name, this)
			.description(description)
			.inputSchema(PARAMETERS)
			.inputType(String.class)
			.toolMetadata(ToolMetadata.builder().returnDirect(true).build())
			.build();
	}

	private static final ObjectMapper objectMapper = new ObjectMapper();

	public ToolExecuteResult run(String toolInput) {
		try {
			Map<String, Object> input = objectMapper.readValue(toolInput, new TypeReference<Map<String, Object>>() {
			});
			String command = (String) input.get("command");
			String planId = (String) input.get("planId");
			String title = (String) input.get("title");
			List<Map<String, Object>> steps = objectMapper.convertValue(input.get("steps"),
					new TypeReference<List<Map<String, Object>>>() {
					});

			return switch (command) {
				case "create" -> createMapReducePlan(planId, title, steps);
				default -> {
					log.info("收到无效的命令: {}", command);
					throw new IllegalArgumentException("Invalid command: " + command);
				}
			};
		}
		catch (JsonProcessingException e) {
			log.info("执行MapReduce计划工具时发生错误", e);
			return new ToolExecuteResult("Error executing mapreduce planning tool: " + e.getMessage());
		}
	}

	/**
	 * 创建MapReduce执行计划
	 * @param planId 计划ID
	 * @param title 计划标题
	 * @param steps 步骤列表
	 * @return 工具执行结果
	 */
	public ToolExecuteResult createMapReducePlan(String planId, String title, List<Map<String, Object>> steps) {
		if (planId == null || title == null || steps == null || steps.isEmpty()) {
			log.info("创建MapReduce计划时缺少必要参数: planId={}, title={}, steps={}", planId, title, steps);
			return new ToolExecuteResult("Required parameters missing");
		}

		MapReduceExecutionPlan plan = new MapReduceExecutionPlan(planId, planId, title);

		for (Map<String, Object> stepNode : steps) {
			String nodeType = (String) stepNode.get("type");

			switch (nodeType) {
				case "sequential" -> processSequentialNode(plan, stepNode);
				case "mapreduce" -> processMapReduceNode(plan, stepNode);
				default -> {
					log.warn("未知的节点类型: {}", nodeType);
					return new ToolExecuteResult("Unknown node type: " + nodeType);
				}
			}
		}

		this.currentPlan = plan;
		return new ToolExecuteResult(
				"MapReduce Plan created: " + planId + "\n" + plan.getPlanExecutionStateStringFormat(false));
	}

	/**
	 * 处理顺序节点
	 * @param plan 执行计划
	 * @param stepNode 步骤节点
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
	 * 处理MapReduce节点
	 * @param plan 执行计划
	 * @param stepNode 步骤节点
	 */
	private void processMapReduceNode(MapReduceExecutionPlan plan, Map<String, Object> stepNode) {
		MapReduceNode node = new MapReduceNode();

		// 处理数据准备步骤
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> dataPreparedSteps = (List<Map<String, Object>>) stepNode.get("dataPreparedSteps");
		if (dataPreparedSteps != null) {
			for (Map<String, Object> step : dataPreparedSteps) {
				ExecutionStep executionStep = createExecutionStepFromMap(step);
				node.addDataPreparedStep(executionStep);
			}
		}

		// 处理Map步骤
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> mapSteps = (List<Map<String, Object>>) stepNode.get("mapSteps");
		if (mapSteps != null) {
			for (Map<String, Object> step : mapSteps) {
				ExecutionStep executionStep = createExecutionStepFromMap(step);
				node.addMapStep(executionStep);
			}
		}

		// 处理Reduce步骤
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> reduceSteps = (List<Map<String, Object>>) stepNode.get("reduceSteps");
		if (reduceSteps != null) {
			for (Map<String, Object> step : reduceSteps) {
				ExecutionStep executionStep = createExecutionStepFromMap(step);
				node.addReduceStep(executionStep);
			}
		}

		// 处理后处理步骤
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
	 * 从Map创建ExecutionStep
	 * @param stepMap 步骤Map
	 * @return 创建的ExecutionStep实例
	 */
	private ExecutionStep createExecutionStepFromMap(Map<String, Object> stepMap) {
		String stepRequirement = (String) stepMap.get("stepRequirement");

		ExecutionStep executionStep = new ExecutionStep();
		executionStep.setStepRequirement(stepRequirement);

		return executionStep;
	}

	@Override
	public ToolExecuteResult apply(String input) {
		return run(input);
	}

}
