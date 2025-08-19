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

package com.alibaba.cloud.ai.studio.core.workflow.processor.impl;

import com.alibaba.cloud.ai.studio.runtime.domain.workflow.Edge;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.InvokeSourceEnum;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.Node;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.NodeResult;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.NodeStatusEnum;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.NodeTypeEnum;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.ValueFromEnum;
import com.alibaba.cloud.ai.studio.runtime.exception.BizException;
import com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode;
import com.alibaba.cloud.ai.studio.runtime.enums.IteratorType;
import com.alibaba.cloud.ai.studio.runtime.utils.JsonUtils;
import com.alibaba.cloud.ai.studio.core.config.CommonConfig;
import com.alibaba.cloud.ai.studio.core.base.manager.RedisManager;
import com.alibaba.cloud.ai.studio.core.workflow.JudgeOperator;
import com.alibaba.cloud.ai.studio.runtime.enums.ParameterTypeEnum;
import com.alibaba.cloud.ai.studio.core.workflow.WorkflowConfig;
import com.alibaba.cloud.ai.studio.core.workflow.WorkflowContext;
import com.alibaba.cloud.ai.studio.core.utils.concurrent.ThreadPoolUtils;
import com.alibaba.cloud.ai.studio.core.utils.common.VariableUtils;
import com.alibaba.cloud.ai.studio.core.workflow.WorkflowInnerService;
import com.alibaba.cloud.ai.studio.core.workflow.processor.AbstractExecuteProcessor;
import com.alibaba.cloud.ai.studio.core.workflow.runtime.WorkflowExecuteManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static com.alibaba.cloud.ai.studio.core.base.constants.CacheConstants.WORKFLOW_TASK_CONTEXT_PREFIX;

/**
 * Iterator Node Processor
 * <p>
 * This class is responsible for handling the execution of iterator nodes in the workflow.
 * It provides functionality for: 1. Array-based iteration with configurable item mapping
 * 2. Count-based iteration with termination conditions 3. Parallel execution of iteration
 * blocks 4. Variable management across iterations 5. Termination condition evaluation 6.
 * Sub-workflow execution and result aggregation 7. Timeout handling and error management
 * 8. Real-time result tracking and state management
 *
 * @author guning.lt
 * @version 1.0.0-M1
 */
@Slf4j
@Component("IteratorExecuteProcessor")
public class IteratorExecuteProcessor extends AbstractExecuteProcessor {

	private static final int DEFAULT_MAX_ITERATIONS = Integer.MAX_VALUE;

	private static final int MAX_ITERATION_LIMIT = 500;

	private final WorkflowExecuteManager workflowExecuteManager;

	public IteratorExecuteProcessor(WorkflowExecuteManager workflowExecuteManager, RedisManager redisManager,
			WorkflowInnerService workflowInnerService, ChatMemory conversationChatMemory, CommonConfig commonConfig) {
		super(redisManager, workflowInnerService, conversationChatMemory, commonConfig);
		this.workflowExecuteManager = workflowExecuteManager;
	}

	/**
	 * Executes the iteration operation
	 * @param graph The workflow graph
	 * @param node The current node to be executed
	 * @param context The workflow context
	 * @return NodeResult containing the iteration results
	 */
	@Override
	public NodeResult innerExecute(DirectedAcyclicGraph<String, Edge> graph, Node node, WorkflowContext context) {

		long start = System.currentTimeMillis();

		// Construct the result of the loop node
		NodeResult nodeResult = new NodeResult();
		nodeResult.setInput(JsonUtils.toJson(constructInputParamsMap(node, context)));
		nodeResult.setNodeId(node.getId());
		nodeResult.setNodeType(node.getType());
		nodeResult.setNodeName(node.getName());
		nodeResult.setNodeStatus(NodeStatusEnum.EXECUTING.getCode());
		context.getNodeResultMap().put(node.getId(), nodeResult);
		// workflowInnerService.refreshContextCache(context);
		log.info("IteratorExecuteProcessor is executing requestId:{}", context.getRequestId());

		NodeParam nodeParam = JsonUtils.fromMap(node.getConfig().getNodeParam(), NodeParam.class);
		// Get the input List mapping table
		Map<String, List<Object>> itemListMap = buildItemMap(node.getConfig().getInputParams(), context);

		// Get the intermediate variable mapping table
		Map<String, Object> variableMap = buildVariableMap(nodeParam.getVariableParameters(), context);

		// Determine the loop count
		int maxIndex = DEFAULT_MAX_ITERATIONS;
		if (nodeParam.getIteratorType().equals(IteratorType.ByArray.getCode())) {
			if (itemListMap.isEmpty()) {
				maxIndex = 0;
			}
			else {
				for (String key : itemListMap.keySet()) {
					List<Object> list = itemListMap.get(key);
					maxIndex = Math.min(maxIndex, list.size());
				}
				// Loop count upper limit 100
				maxIndex = Math.min(maxIndex, 500);
			}
		}
		else {
			maxIndex = nodeParam.getCountLimit();
			maxIndex = Math.min(maxIndex, 500);
			itemListMap = new HashMap<>();
		}

		// Record the execution result of each loop
		HashMap<String, NodeResult> resultMap = new HashMap<>();

		// Record the nodes in the loop body
		List<String> nodeIds = nodeParam.getBlock().getNodes().stream().map(Node::getId).collect(Collectors.toList());

		// Loop body canvas configuration
		WorkflowConfig WorkflowConfig = nodeParam.getBlock();

		log.info(
				"IteratorExecuteProcessor is executing requestId:{} ,itemListMap:{} ,iteratorNodeIds:{} ,WorkflowConfig:{} ,variableMap:{} ,maxIndex:{}",
				context.getRequestId(), JsonUtils.toJson(itemListMap), nodeIds, JsonUtils.toJson(WorkflowConfig),
				JsonUtils.toJson(variableMap), maxIndex);

		// Update the context loop node intermediate variable
		Map<String, Object> jsonObject = new HashMap<>();
		for (String key : variableMap.keySet()) {
			jsonObject.put(key, variableMap.get(key));
		}
		context.getVariablesMap().put(node.getId(), jsonObject);

		// Copy a context, the stream output of the loop body will affect the context,
		// copy finalCloneContext each time
		WorkflowContext finalCloneContext = WorkflowContext.deepCopy(context);
		context.getSubWorkflowContextMap().put(node.getId(), finalCloneContext);

		// Initialize the nodes in the loop body
		initNodeResult(nodeParam, context, resultMap);

		for (int index = 0; index < maxIndex; index++) {

			if (context.getTaskStatus().equals(NodeStatusEnum.FAIL.getCode())) {
				nodeResult.setNodeStatus(NodeStatusEnum.FAIL.getCode());
				nodeResult.setError(ErrorCode.WORKFLOW_EXECUTE_ERROR.toError("主流程已失败，循环节点失败"));
				break;
			}

			if (context.getTaskStatus().equals(NodeStatusEnum.STOP.getCode())) {
				nodeResult.setNodeStatus(NodeStatusEnum.STOP.getCode());
				nodeResult.setError(ErrorCode.WORKFLOW_EXECUTE_ERROR.toError("Manually terminated"));
				break;
			}

			// If timeout, stop the loop
			long seconds = (System.currentTimeMillis() - context.getStartTime()) / 1000;
			// The overall workflow timeout can be adjusted
			if (seconds > InvokeSourceEnum.valueOf(context.getInvokeSource()).getTimeoutSeconds()) {
				NodeResult nodeResultFail = new NodeResult();
				nodeResultFail.setNodeId(node.getId());
				nodeResultFail.setNodeName(node.getName());
				nodeResultFail.setNodeType(node.getType());
				nodeResultFail.setInput(JsonUtils.toJson(constructInputParamsMap(node, context)));
				// Construct the loop node result
				HashMap<String, Object> stringObjectHashMap = constructOutput(node, context, nodeIds);
				nodeResultFail.setNodeStatus(NodeStatusEnum.FAIL.getCode());
				nodeResultFail.setErrorInfo("task timeout");
				nodeResultFail.setOutput(JsonUtils.toJson(stringObjectHashMap));
				log.info("Time out ,IteratorExecuteProcessor is fail requestId:{} ,nodeResultFail:{} ",
						context.getRequestId(), JsonUtils.toJson(nodeResultFail));

				return nodeResultFail;
			}

			// If the loop node execution fails, terminate the loop
			if (context.getNodeResultMap().get(node.getId()).getNodeStatus().equals(NodeStatusEnum.FAIL.getCode())) {
				log.info("subWorkflow terminate ,IteratorExecuteProcessor execute success requestId:{} ",
						context.getRequestId());
				break;
			}

			// Determine whether the termination condition is met
			Optional<Branch> any = nodeParam.getTerminations().stream().filter(branch -> {
				String id = branch.getId();
				return conditionHit(id, branch, context);
			}).findFirst();
			if (any.isPresent()) {
				break;
			}

			// Update the context loop node configuration for sub-canvas reference
			Map<String, Object> object = new HashMap<>();
			for (String key : variableMap.keySet()) {
				object.put(key, variableMap.get(key));
			}
			for (String key : itemListMap.keySet()) {
				if (itemListMap.get(key).get(index) != null) {
					object.put(key, itemListMap.get(key).get(index));
				}

			}
			object.put("index", index + 1);
			finalCloneContext.getVariablesMap().put(node.getId(), object);

			log.info(
					"subWorkflow executing ,IteratorExecuteProcessor is executing requestId:{} ,batch:{},subVariablesMap:{}",
					context.getRequestId(), index, JsonUtils.toJson(finalCloneContext.getVariablesMap()));

			// Configure sub-process information, add the sub-task id to
			WorkflowContext cloneContext = WorkflowContext.deepCopy(finalCloneContext);

			String newTaskId = finalCloneContext.getTaskId() + "-" + node.getId() + "-" + index;
			cloneContext.setLock(new ReentrantLock());
			cloneContext.setExecuteOrderList(new CopyOnWriteArrayList<>());
			cloneContext.setTaskId(newTaskId);
			context.getSubTaskIdSet().add(newTaskId);

			// Update context to cache
			// workflowInnerService.refreshContextCache(context);
			ThreadPoolUtils.nodeExecutorService.submit(() -> {
				try {
					workflowExecuteManager.syncExecute(WorkflowConfig, cloneContext);
				}
				catch (Exception e) {
					log.info("IteratorExecuteProcessor innerExecute submit error:", e);
				}
			});

			// Record successful nodes in the sub-task
			HashSet<String> nodeSet = new HashSet<>();

			// Get the sub-task execution result
			while (true) {

				if (context.getTaskStatus().equals(NodeStatusEnum.FAIL.getCode())
						|| context.getTaskStatus().equals(NodeStatusEnum.STOP.getCode())) {
					break;
				}

				// Get the sub-task information
				String taskStatus = cloneContext.getTaskStatus();
				if (NodeStatusEnum.PAUSE.getCode().equals(taskStatus)) {
					context.setTaskStatus(NodeStatusEnum.PAUSE.getCode());
					ConcurrentHashMap<String, NodeResult> nodeResultMap = cloneContext.getNodeResultMap();
					NodeResult input = new NodeResult();
					for (String key : nodeResultMap.keySet()) {
						NodeResult subNodeResult = nodeResultMap.get(key);
						if (subNodeResult.getNodeStatus().equals(NodeStatusEnum.PAUSE.getCode())) {
							input = subNodeResult;
							break;
						}
					}
					input.setParentNodeId(node.getId());
					if (context.getNodeResultMap().containsKey(input.getNodeId())) {
						input = context.getNodeResultMap().get(input.getNodeId());
						input.setNodeStatus(NodeStatusEnum.PAUSE.getCode());
						input.setIndex(input.getIndex() + 1);
					}
					else {
						input.setIndex(0);
						context.getNodeResultMap().put(input.getNodeId(), input);
					}

					context.getExecuteOrderList().add(input.getNodeId());
					// workflowInnerService.forceRefreshContextCache(context);
					long timeout = commonConfig.getInputTimeout();
					while (NodeStatusEnum.PAUSE.getCode().equals(input.getNodeStatus())) {
						WorkflowContext wfContext = redisManager
							.get(WORKFLOW_TASK_CONTEXT_PREFIX + context.getWorkspaceId() + "_" + context.getTaskId());
						input = wfContext.getNodeResultMap().get(input.getNodeId());
						try {
							// Avoid CPU idle, wait 500ms each time
							Thread.sleep(500);
						}
						catch (InterruptedException e) {
							log.error("Interrupted while waiting for input node result", e);
							Thread.currentThread().interrupt();
						}

						// Check if timeout
						if (System.currentTimeMillis() - context.getStartTime() > timeout) {
							break;
						}
					}
					cloneContext.getNodeResultMap().put(input.getNodeId(), input);
					if (!checkSubTaskPause(cloneContext)) {
						cloneContext.setTaskStatus(NodeStatusEnum.EXECUTING.getCode());
					}
					// workflowInnerService.forceRefreshContextCache(cloneContext);
					context.setTaskStatus(NodeStatusEnum.EXECUTING.getCode());
					// workflowInnerService.forceRefreshContextCache(context);
					continue;
				}
				if (NodeStatusEnum.SUCCESS.getCode().equals(taskStatus)
						|| NodeStatusEnum.FAIL.getCode().equals(taskStatus)) {
					log.info(
							"saveRealTimeNodeResult start , requestId:{} ,batchIndex:{},cloneContextVariableMap:{},cloneContextNodeResultMap:{}",
							context.getRequestId(), index, JsonUtils.toJson(cloneContext.getVariablesMap()),
							JsonUtils.toJson(cloneContext.getNodeResultMap()));
					// Construct real-time sub-canvas node result
					saveRealTimeNodeResult(node, context, cloneContext, nodeSet, nodeIds, index, start);
					if (NodeStatusEnum.FAIL.getCode().equals(taskStatus)) {
						// Sub-task fails, then the loop node execution fails
						NodeResult nodeResultFail = new NodeResult();
						nodeResultFail.setNodeId(node.getId());
						nodeResultFail.setNodeName(node.getName());
						nodeResultFail.setNodeType(node.getType());
						nodeResultFail.setInput(JsonUtils.toJson(constructInputParamsMap(node, context)));
						// Construct the loop node result
						updateVariable(node, context, cloneContext);
						HashMap<String, Object> stringObjectHashMap = constructOutput(node, context, nodeIds);
						nodeResultFail.setNodeStatus(NodeStatusEnum.FAIL.getCode());
						nodeResultFail.setErrorInfo(cloneContext.getErrorInfo());
						nodeResultFail.setOutput(JsonUtils.toJson(stringObjectHashMap));
						log.info(
								"subWorkflow execute fail  ,IteratorExecuteProcessor is fail requestId:{} ,nodeResultFail:{} ",
								context.getRequestId(), JsonUtils.toJson(nodeResultFail));
						return nodeResultFail;
						// context.getNodeResultMap().get(node.getId()).setNodeStatus(NodeStatusEnum.FAIL.getCode());
					}
					break;
				}
				// Construct real-time sub-canvas node result
				saveRealTimeNodeResult(node, context, cloneContext, nodeSet, nodeIds, index, start);

				// Check if the sub-task is timeout, if timeout, do not wait for the
				// result of this loop cycle
				seconds = (System.currentTimeMillis() - context.getStartTime()) / 1000;
				if (seconds > InvokeSourceEnum.valueOf(context.getInvokeSource()).getTimeoutSeconds()) {
					cloneContext.setTaskStatus(NodeStatusEnum.FAIL.getCode());
					cloneContext.setErrorInfo("subTask time out");
					break;
				}

				// Timed get sub-task result, implement sub-task stream output
				try {
					Thread.sleep(200);
				}
				catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
			// Update intermediate variable
			updateVariable(node, context, cloneContext);
			// Synchronize the value of intermediate variables
			for (String key : variableMap.keySet()) {
				Map<Object, Object> variableMapObject = JsonUtils
					.fromJsonToMap(JsonUtils.toJson(context.getVariablesMap().get(node.getId())));
				variableMap.put(key, variableMapObject.get(key));
			}

		}

		// Construct the loop node result
		HashMap<String, Object> stringObjectHashMap = constructOutput(node, context, nodeIds);
		nodeResult.setOutput(JsonUtils.toJson(stringObjectHashMap));
		return nodeResult;
	}

	/**
	 * Checks if a sub-task is in pause state
	 * @param cloneContext The cloned workflow context
	 * @return true if the sub-task is paused, false otherwise
	 */
	private boolean checkSubTaskPause(WorkflowContext cloneContext) {
		ConcurrentHashMap<String, NodeResult> nodeResultMap = cloneContext.getNodeResultMap();
		for (NodeResult nodeResult : nodeResultMap.values()) {
			if (nodeResult.getNodeStatus().equals(NodeStatusEnum.PAUSE.getCode())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Updates variables in the parent context with values from the cloned context
	 * @param node The current node
	 * @param context The parent workflow context
	 * @param cloneContext The cloned workflow context
	 */
	public void updateVariable(Node node, WorkflowContext context, WorkflowContext cloneContext) {
		ConcurrentHashMap<String, Object> subVariablesMap = cloneContext.getVariablesMap();
		context.getVariablesMap().put(node.getId(), subVariablesMap.get(node.getId()));
	}

	/**
	 * Constructs the output map for the iteration node
	 * @param node The current node
	 * @param context The workflow context
	 * @param nodeIds List of node IDs in the iteration block
	 * @return HashMap containing the iteration results
	 */
	public HashMap<String, Object> constructOutput(Node node, WorkflowContext context, List<String> nodeIds) {
		// Loop return result
		// JSONObject jsonObject = new JSONObject();

		// Construct the loop node output variable set that can be referenced, user
		// variable replacement
		HashMap<String, Object> subResultMap = new HashMap<>();

		subResultMap.put(node.getId(), context.getVariablesMap().get(node.getId()));

		ConcurrentHashMap<String, NodeResult> nodeResultMap = context.getNodeResultMap();
		for (String nodeId : nodeIds) {
			if (nodeResultMap.containsKey(nodeId)) {
				NodeResult nodeResult = nodeResultMap.get(nodeId);
				ArrayList<Object> list = new ArrayList<>();
				List<NodeResult> batches = nodeResult.getBatches();
				for (NodeResult batch : batches) {
					String outputJsonString = batch.getOutput();
					list.add(JsonUtils.fromJsonToMap(outputJsonString));
				}
				subResultMap.put(nodeId, list);

			}
		}

		HashMap<String, Object> resultMap = new HashMap<>();
		List<Node.OutputParam> outputParams = node.getConfig().getOutputParams();
		for (Node.OutputParam params : outputParams) {
			String key = params.getKey();
			String expression = VariableUtils.getExpressionFromBracket((String) params.getValue());
			Object valueFromPayload = VariableUtils.getValueFromPayload(expression, subResultMap);
			log.info(
					"log used for debug getValueFromPayload,requestId:{},expression:{},subResultMap:{},valueFromPayload:{},variableMap:{}",
					context.getRequestId(), expression, JsonUtils.toJson(subResultMap), JsonUtils.toJson(subResultMap),
					JsonUtils.toJson(context.getVariablesMap()));
			resultMap.put(key, valueFromPayload);
		}

		return resultMap;
		// Store the constructed output result in output
		// context.getNodeResultMap().get(node.getId()).setOutput(JsonUtils.toJson(resultMap));

	}

	/**
	 * Initializes node results for the iteration block
	 * @param nodeParam The node parameters
	 * @param context The workflow context
	 * @param resultMap Map to store node results
	 */
	private void initNodeResult(NodeParam nodeParam, WorkflowContext context, HashMap<String, NodeResult> resultMap) {
		List<Node> nodes = nodeParam.getBlock().getNodes();
		for (Node node : nodes) {
			NodeResult nodeResult = new NodeResult();
			nodeResult.setBatch(true);
			nodeResult.setNodeId(node.getId());
			nodeResult.setNodeType(node.getType());
			nodeResult.setNodeName(node.getName());
			// nodeResultMap.put(node.getId(), nodeResult);
			resultMap.put(node.getId(), nodeResult);
		}

	}

	/**
	 * Saves real-time node results during iteration
	 * @param node The current node
	 * @param context The parent workflow context
	 * @param cloneContext The cloned workflow context
	 * @param nodeSet Set of successful nodes
	 * @param nodeIds List of node IDs in the iteration block
	 * @param index Current iteration index
	 * @param start Start time of the iteration
	 */
	private void saveRealTimeNodeResult(Node node, WorkflowContext context, WorkflowContext cloneContext,
			HashSet<String> nodeSet, List<String> nodeIds, int index, long start) {
		// log.info("saveRealTimeNodeResult start , requestId:{}
		// ,batch:{},cloneContextVariableMap:{},cloneContextNodeResultMap:{}",
		// context.getRequestId(), index,
		// JsonUtils.toJson(cloneContext.getVariablesMap()),
		// JsonUtils.toJson(cloneContext.getNodeResultMap()));
		ConcurrentHashMap<String, NodeResult> nodeResultMap = context.getNodeResultMap();
		NodeResult iteratorNodeResult = context.getNodeResultMap().get(node.getId());
		iteratorNodeResult.setNodeExecTime((System.currentTimeMillis() - start) + "ms");
		ConcurrentHashMap<String, NodeResult> subNodeResultMap = cloneContext.getNodeResultMap();
		// Check if there are successful nodes in the sub-task
		for (String nodeId : nodeIds) {
			// If there are successful nodes in the sub-task
			if (subNodeResultMap.containsKey(nodeId)) {
				if (!context.getExecuteOrderList().contains(nodeId)) {
					context.getExecuteOrderList().add(nodeId);
				}
				NodeResult childNode = subNodeResultMap.get(nodeId);
				if (Objects.equals(childNode.getNodeStatus(), NodeStatusEnum.SUCCESS.getCode())) {
					if (!nodeSet.contains(nodeId)) {
						// Record successful or failed nodes
						nodeSet.add(nodeId);
						// Set the information and status of the main task corresponding
						// node
						NodeResult nodeResult;
						if (nodeResultMap.containsKey(nodeId)) {
							nodeResult = nodeResultMap.get(nodeId);
						}
						else {
							nodeResult = new NodeResult();
							nodeResult.setBatch(true);
							nodeResult.setNodeId(nodeId);
							nodeResult.setNodeType(childNode.getNodeType());
							nodeResult.setNodeName(childNode.getNodeName());
						}
						// NodeResult nodeResult = nodeResultMap.get(nodeId);
						childNode.setIndex(index);
						nodeResult.getBatches().add(childNode);
						nodeResult.setNodeStatus(childNode.getNodeStatus());
						// Set time
						String subExecutedTime = childNode.getNodeExecTime();
						String executedTime = nodeResult.getNodeExecTime();
						if (StringUtils.isBlank(executedTime)) {
							nodeResult.setNodeExecTime(subExecutedTime);
						}
						else {
							Long executedTimeLong = Long.parseLong(executedTime.replace("ms", ""))
									+ Long.parseLong(subExecutedTime.replace("ms", ""));
							nodeResult.setNodeExecTime(executedTimeLong + "ms");
						}
						// Set usages
						if (childNode.getUsages() != null) {
							if (nodeResult.getUsages() == null) {
								nodeResult.setUsages(childNode.getUsages());
							}
							else {
								nodeResult.getUsages().addAll(childNode.getUsages());
							}
						}

						// Set parent node
						nodeResult.setParentNodeId(node.getId());
						nodeResultMap.put(nodeId, nodeResult);
					}

				}
				else if (Objects.equals(childNode.getNodeStatus(), NodeStatusEnum.EXECUTING.getCode())
						|| Objects.equals(childNode.getNodeStatus(), NodeStatusEnum.SKIP.getCode())) {
					NodeResult nodeResult;
					if (nodeResultMap.containsKey(nodeId)) {
						nodeResult = nodeResultMap.get(nodeId);
					}
					else {
						nodeResult = new NodeResult();
						nodeResult.setBatch(true);
						nodeResult.setNodeId(nodeId);
						nodeResult.setNodeType(childNode.getNodeType());
						nodeResult.setNodeName(childNode.getNodeName());
					}
					// Set parent node
					nodeResult.setParentNodeId(node.getId());
					nodeResult.setNodeStatus(childNode.getNodeStatus());
					nodeResultMap.put(nodeId, nodeResult);

				}
				else if (Objects.equals(childNode.getNodeStatus(), NodeStatusEnum.FAIL.getCode())) {
					if (!nodeSet.contains(nodeId)) {
						// Record successful or failed nodes
						nodeSet.add(nodeId);
						// Set the information and status of the main task corresponding
						// node
						NodeResult nodeResult;
						if (nodeResultMap.containsKey(nodeId)) {
							nodeResult = nodeResultMap.get(nodeId);
						}
						else {
							nodeResult = new NodeResult();
							nodeResult.setBatch(true);
							nodeResult.setNodeId(nodeId);
							nodeResult.setNodeType(childNode.getNodeType());
							nodeResult.setNodeName(childNode.getNodeName());
						}
						childNode.setIndex(index);
						nodeResult.getBatches().add(childNode);
						nodeResult.setNodeStatus(childNode.getNodeStatus());
						nodeResult.setErrorCode(childNode.getErrorCode());
						nodeResult.setErrorInfo(childNode.getErrorInfo());
						// Set time
						String subExecutedTime = childNode.getNodeExecTime();
						String executedTime = nodeResult.getNodeExecTime();
						if (StringUtils.isBlank(executedTime)) {
							nodeResult.setNodeExecTime(subExecutedTime);
						}
						else {
							Long executedTimeLong = Long.parseLong(executedTime.replace("ms", ""))
									+ Long.parseLong(subExecutedTime.replace("ms", ""));
							nodeResult.setNodeExecTime(executedTimeLong + "ms");
						}
						// Set usages
						if (childNode.getUsages() != null) {
							if (nodeResult.getUsages() == null) {
								nodeResult.setUsages(childNode.getUsages());
							}
							else {
								nodeResult.getUsages().addAll(childNode.getUsages());
							}
						}

						// Set parent node
						nodeResult.setParentNodeId(node.getId());
						nodeResultMap.put(nodeId, nodeResult);
					}

				}
			}

		}
		// workflowInnerService.refreshContextCache(context);
	}

	/**
	 * Builds a map of items for iteration
	 * @param paramsArray List of input parameters
	 * @param context The workflow context
	 * @return Map of item lists for iteration
	 */
	private Map<String, List<Object>> buildItemMap(List<Node.InputParam> paramsArray, WorkflowContext context) {
		Map<String, List<Object>> params = Maps.newHashMap();

		if (Objects.isNull(paramsArray)) {
			return params;
		}
		ObjectMapper objectMapper = new ObjectMapper();
		paramsArray.forEach(param -> {
			String valueFromRequestContext = VariableUtils.getValueStringFromContext(param, context);
			// Convert JSON string to Map
			List<Object> list;
			try {
				list = objectMapper.readValue(valueFromRequestContext,
						new com.fasterxml.jackson.core.type.TypeReference<List<Object>>() {
						});
			}
			catch (JsonProcessingException e) {
				throw new BizException(ErrorCode.INVALID_PARAMS.toError("input_params",
						"The parameters of the loop node do not conform to the array format."));
			}
			params.put(param.getKey(), list);
		});
		return params;
	}

	/**
	 * Builds a map of variables for iteration
	 * @param paramsArray List of input parameters
	 * @param context The workflow context
	 * @return Map of variables for iteration
	 */
	private Map<String, Object> buildVariableMap(List<Node.InputParam> paramsArray, WorkflowContext context) {
		Map<String, Object> params = Maps.newHashMap();

		if (Objects.isNull(paramsArray)) {
			return params;
		}
		paramsArray.forEach(param -> {
			Object valueFromRequestContext = VariableUtils.getValueFromContext(param, context);
			params.put(param.getKey(), valueFromRequestContext);

		});
		return params;
	}

	/**
	 * Checks if a termination condition is met
	 * @param id The branch ID
	 * @param branch The branch configuration
	 * @param context The workflow context
	 * @return true if the condition is met, false otherwise
	 */
	private boolean conditionHit(String id, Branch branch, WorkflowContext context) {

		String logic = branch.getLogic();
		// Default to and logic
		if (StringUtils.isBlank(logic)) {
			logic = Logic.and.name();
		}
		List<Condition> conditions = branch.getConditions();
		if (CollectionUtils.isEmpty(conditions)) {
			// If no conditions are configured, go directly down, consider not hitting
			return false;
		}
		return checkCondition(logic, conditions, context);
	}

	/**
	 * Evaluates a logical condition
	 * @param logic The logical operator (AND/OR)
	 * @param conditions List of conditions to evaluate
	 * @param context The workflow context
	 * @return true if the condition is met, false otherwise
	 */
	private boolean checkCondition(String logic, List<Condition> conditions, WorkflowContext context) {
		Logic logicEnum = Logic.valueOf(logic);
		switch (logicEnum) {
			case and:
				for (Condition condition : conditions) {
					Node.InputParam left = condition.getLeft();
					String operator = condition.getOperator();
					Node.InputParam right = condition.getRight();
					String leftValue = VariableUtils.getValueStringFromContext(left, context);
					String rightValue = VariableUtils.getValueStringFromContext(right, context);
					if (!leftOperateRight(left, leftValue, right, rightValue, operator)) {
						return false;
					}
				}
				return true;
			case or:
				for (Condition condition : conditions) {
					Node.InputParam left = condition.getLeft();
					String operator = condition.getOperator();
					Node.InputParam right = condition.getRight();
					String leftValue = VariableUtils.getValueStringFromContext(left, context);
					String rightValue = VariableUtils.getValueStringFromContext(right, context);
					if (leftOperateRight(left, leftValue, right, rightValue, operator)) {
						return true;
					}
				}
				return false;
			default:
				return false;
		}
	}

	/**
	 * Compares left and right operands using the specified operator
	 * @param left The left operand parameter
	 * @param leftValue The left operand value
	 * @param right The right operand parameter
	 * @param rightValue The right operand value
	 * @param operator The comparison operator
	 * @return true if the comparison is successful, false otherwise
	 */
	private boolean leftOperateRight(Node.InputParam left, String leftValue, Node.InputParam right, String rightValue,
			String operator) {

		// // Whether the right value type needs to be checked
		// boolean rightTypeIgnore =
		// ValueFromEnum.input.name().equals(right.getValueFrom());

		String leftType = left.getType() == null ? null : left.getType();
		String rightType = right.getType() == null ? null : right.getType();
		// The left value can only be referenced, not filled in, so the type of left
		// cannot be empty
		if (StringUtils.isBlank(leftType) || StringUtils.isBlank(operator)) {
			return false;
		}

		// operator = operator.toLowerCase();

		// If the right value is a reference but the type is unknown, return false
		// directly
		if (ValueFromEnum.refer.name().equals(right.getValueFrom()) && StringUtils.isBlank(rightType)) {
			if (!operator.equals(JudgeOperator.IS_NULL.getCode())
					&& !operator.equals(JudgeOperator.IS_NOT_NULL.getCode())
					&& !operator.equals(JudgeOperator.IS_TRUE.getCode())
					&& !operator.equals(JudgeOperator.IS_FALSE.getCode())) {
				// is_null、is_not_null、is_true、is_false不需要右值
				return false;
			}
		}

		Set<String> operatorScopeSet = JudgeOperator.getAllCodes();
		Set<String> typeSet = ParameterTypeEnum.getAllCodes();
		// Return false for illegal operators or illegal types
		log.info("operator:{},leftType:{},operatorScopeSet:{},typeSet:{}", operator, leftType, operatorScopeSet,
				typeSet);
		if (!operatorScopeSet.contains(operator) || !typeSet.contains(leftType)) {
			return false;
		}
		// Return false if the operator does not match the operand
		if (!JudgeOperator.getOperator(operator).getScopeSet().contains(leftType)) {
			return false;
		}
		try {
			switch (operator) {
				case "equals":
					if ("string".equalsIgnoreCase(leftType)) {
						return leftValue.equals(rightValue);
					}
					else if ("number".equalsIgnoreCase(leftType)) {
						return compareDigits(leftValue, rightValue) == 0;
					}
					else if ("boolean".equalsIgnoreCase(leftType)) {
						return Boolean.parseBoolean(leftValue) == Boolean.parseBoolean(rightValue);
					}
					return false;

				case "notEquals":
					if ("string".equalsIgnoreCase(leftType)) {
						return !leftValue.equals(rightValue);
					}
					else if ("number".equalsIgnoreCase(leftType)) {
						return compareDigits(leftValue, rightValue) != 0;
					}
					else if ("boolean".equalsIgnoreCase(leftType)) {
						return (Boolean.parseBoolean(leftValue) ^ Boolean.parseBoolean(rightValue));
					}
					return false;

				case "isNull":
					if ("string".equalsIgnoreCase(leftType)) {
						return StringUtils.isEmpty(leftValue);
					}
					else if ("number".equalsIgnoreCase(leftType)) {
						return StringUtils.isEmpty(leftValue);
					}
					else if ("boolean".equalsIgnoreCase(leftType)) {
						return StringUtils.isEmpty(leftValue);
					}
					else if (leftType.toLowerCase().startsWith("array")) {
						return CollectionUtils.isEmpty(JsonUtils.fromJsonToList(leftValue, Object.class));
					}
					else if ("object".equalsIgnoreCase(leftType)) {
						return Objects.isNull(JsonUtils.fromJsonToMap(leftValue));
					}
					return false;

				case "isNotNull":
					if ("string".equalsIgnoreCase(leftType)) {
						return !StringUtils.isEmpty(leftValue);
					}
					else if ("number".equalsIgnoreCase(leftType)) {
						return true;
					}
					else if ("boolean".equalsIgnoreCase(leftType)) {
						return true;
					}
					else if (leftType.toLowerCase().startsWith("array")) {
						return !CollectionUtils.isEmpty(JsonUtils.fromJsonToList(leftValue, Object.class));
					}
					else if ("object".equalsIgnoreCase(leftType)) {
						return !Objects.isNull(JsonUtils.fromJsonToMap(leftValue));
					}
					return false;

				case "greater":
					if ("number".equalsIgnoreCase(leftType)) {
						return compareDigits(leftValue, rightValue) > 0;
					}
					return false;

				case "greaterAndEqual":
					if ("number".equalsIgnoreCase(leftType)) {
						return compareDigits(leftValue, rightValue) >= 0;
					}
					return false;

				case "less":
					if ("number".equalsIgnoreCase(leftType)) {
						return compareDigits(leftValue, rightValue) < 0;
					}
					return false;

				case "lessAndEqual":
					if ("number".equalsIgnoreCase(leftType)) {
						return compareDigits(leftValue, rightValue) <= 0;
					}
					return false;

				case "isTrue":
					if ("boolean".equalsIgnoreCase(leftType)) {
						return Boolean.parseBoolean(leftValue);
					}
					return false;

				case "isFalse":
					if ("boolean".equalsIgnoreCase(leftType)) {
						return !Boolean.parseBoolean(leftValue);
					}
					return false;

				case "lengthEquals":
					if ("string".equalsIgnoreCase(leftType)) {
						if (NumberUtils.isCreatable(rightValue)) {
							// If the right value is a number, directly compare the length
							// of the left value with this number
							return compareDigits(leftValue.length() + "", rightValue) == 0;
						}
						else {
							return leftValue.length() == rightValue.length();
						}
					}
					else if (leftType.toLowerCase().startsWith("array")) {
						List<Object> leftArrays = JsonUtils.fromJsonToList(leftValue, Object.class);
						// JSONArray rightArrays = JSONObject.parseArray(rightValue);
						if (NumberUtils.isCreatable(rightValue)) {
							// If the right value is a number, directly compare the length
							// of the left value with this number
							return compareDigits(leftArrays.size() + "", rightValue) == 0;
						}
						else {
							boolean isArray = JsonUtils.isJsonArray(rightValue);
							int leftSize = CollectionUtils.isEmpty(leftArrays) ? 0 : leftArrays.size();
							int rightSize = isArray ? JsonUtils.fromJsonToList(rightValue, Object.class).size()
									: rightValue.length();
							return leftSize == rightSize;
						}
					}
					return false;
				case "lengthGreater":
					if ("string".equalsIgnoreCase(leftType)) {
						if (NumberUtils.isCreatable(rightValue)) {
							// If the right value is a number, directly compare the length
							// of the left value with this number
							return compareDigits(leftValue.length() + "", rightValue) > 0;
						}
						else {
							return leftValue.length() > rightValue.length();
						}
					}
					else if (leftType.toLowerCase().startsWith("array")) {
						List<Object> leftArrays = JsonUtils.fromJsonToList(leftValue, Object.class);
						if (NumberUtils.isCreatable(rightValue)) {
							// If the right value is a number, directly compare the length
							// of the left value with this number
							return compareDigits(leftArrays.size() + "", rightValue) > 0;
						}
						else {
							boolean isArray = JsonUtils.isJsonArray(rightValue);
							int leftSize = CollectionUtils.isEmpty(leftArrays) ? 0 : leftArrays.size();
							int rightSize = isArray ? JsonUtils.fromJsonToList(rightValue, Object.class).size()
									: rightValue.length();
							return leftSize > rightSize;
						}
					}
					return false;

				case "lengthGreaterAndEqual":
					if ("string".equalsIgnoreCase(leftType)) {
						if (NumberUtils.isCreatable(rightValue)) {
							// If the right value is a number, directly compare the length
							// of the left value with this number
							return compareDigits(leftValue.length() + "", rightValue) >= 0;
						}
						else {
							return leftValue.length() >= rightValue.length();
						}
					}
					else if (leftType.toLowerCase().startsWith("array")) {
						List<Object> leftArrays = JsonUtils.fromJsonToList(leftValue, Object.class);
						if (NumberUtils.isCreatable(rightValue)) {
							// If the right value is a number, directly compare the length
							// of the left value with this number
							return compareDigits(leftArrays.size() + "", rightValue) >= 0;
						}
						else {
							boolean isArray = JsonUtils.isJsonArray(rightValue);
							int leftSize = CollectionUtils.isEmpty(leftArrays) ? 0 : leftArrays.size();
							int rightSize = isArray ? JsonUtils.fromJsonToList(rightValue, Object.class).size()
									: rightValue.length();
							return leftSize >= rightSize;
						}
					}
					return false;

				case "lengthLess":
					if ("string".equalsIgnoreCase(leftType)) {
						if (NumberUtils.isCreatable(rightValue)) {
							// If the right value is a number, directly compare the length
							// of the left value with this number
							return compareDigits(leftValue.length() + "", rightValue) < 0;
						}
						else {
							return leftValue.length() < rightValue.length();
						}
					}
					else if (leftType.toLowerCase().startsWith("array")) {
						List<Object> leftArrays = JsonUtils.fromJsonToList(leftValue, Object.class);
						// JSONArray rightArrays = JSONObject.parseArray(rightValue);
						if (NumberUtils.isCreatable(rightValue)) {
							// If the right value is a number, directly compare the length
							// of the left value with this number
							return compareDigits(leftArrays.size() + "", rightValue) < 0;
						}
						else {
							boolean isArray = JsonUtils.isJsonArray(rightValue);
							int leftSize = CollectionUtils.isEmpty(leftArrays) ? 0 : leftArrays.size();
							int rightSize = isArray ? JsonUtils.fromJsonToList(rightValue, Object.class).size()
									: rightValue.length();
							return leftSize < rightSize;
						}
					}
					return false;

				case "lengthLessAndEqual":
					if ("string".equalsIgnoreCase(leftType)) {
						if (NumberUtils.isCreatable(rightValue)) {
							// If the right value is a number, directly compare the length
							// of the left value with this number
							return compareDigits(leftValue.length() + "", rightValue) <= 0;
						}
						else {
							return leftValue.length() <= rightValue.length();
						}
					}
					else if (leftType.toLowerCase().startsWith("array")) {
						List<Object> leftArrays = JsonUtils.fromJsonToList(leftValue, Object.class);
						if (NumberUtils.isCreatable(rightValue)) {
							// If the right value is a number, directly compare the length
							// of the left value with this number
							return compareDigits(leftArrays.size() + "", rightValue) <= 0;
						}
						else {
							boolean isArray = JsonUtils.isJsonArray(rightValue);
							int leftSize = CollectionUtils.isEmpty(leftArrays) ? 0 : leftArrays.size();
							int rightSize = isArray ? JsonUtils.fromJsonToList(rightValue, Object.class).size()
									: rightValue.length();
							return leftSize <= rightSize;
						}
					}
					return false;

				case "contains":
					if ("string".equalsIgnoreCase(leftType)) {
						return leftValue.contains(rightValue);
					}
					else if (leftType.toLowerCase().startsWith("array")) {
						List<String> strings = JsonUtils.fromJsonToList(leftValue, String.class);
						if (CollectionUtils.isEmpty(strings)) {
							return false;
						}
						return strings.contains(rightValue);
					}
					else if ("object".equalsIgnoreCase(leftType)) {
						Map<String, Object> jsonObject = JsonUtils.fromJsonToMap(leftValue);
						if (jsonObject != null && jsonObject.containsKey(rightValue)) {
							return true;
						}
						return false;
					}
					return false;

				case "notContains":
					if ("string".equalsIgnoreCase(leftType)) {
						return !leftValue.contains(rightValue);
					}
					else if (leftType.toLowerCase().startsWith("array")) {
						List<String> strings = JsonUtils.fromJsonToList(leftValue, String.class);
						if (CollectionUtils.isEmpty(strings)) {
							return true;
						}
						return !strings.contains(rightValue);
					}
					else if ("object".equalsIgnoreCase(leftType)) {
						Map<String, Object> jsonObject = JsonUtils.fromJsonToMap(leftValue);
						if (jsonObject != null && jsonObject.containsKey(rightValue)) {
							return false;
						}
						return true;
					}
					return false;

				default:
					break;

			}
		}
		catch (Exception e) {
			log.error("Error occurred while evaluating condition: {}", e.getMessage(), e);
			return false;
		}

		return false;
	}

	/**
	 * Compares two numeric strings
	 * @param leftValue The left numeric value
	 * @param rightValue The right numeric value
	 * @return Comparison result (-1, 0, or 1)
	 */
	private int compareDigits(String leftValue, String rightValue) {
		return new BigDecimal(leftValue).compareTo(new BigDecimal(rightValue));
	}

	/**
	 * Validates the node parameters including iteration type and termination conditions
	 * @param graph The workflow graph
	 * @param node The node to validate
	 * @return CheckNodeParamResult containing validation results
	 */
	@Override
	public CheckNodeParamResult checkNodeParam(DirectedAcyclicGraph<String, Edge> graph, Node node) {
		CheckNodeParamResult result = super.checkNodeParam(graph, node);
		CheckNodeParamResult inputParamsResult = checkInputParams(node.getConfig().getInputParams());
		if (!inputParamsResult.isSuccess()) {
			result.setSuccess(false);
			result.getErrorInfos().addAll(inputParamsResult.getErrorInfos());
		}
		NodeParam nodeParam = JsonUtils.fromMap(node.getConfig().getNodeParam(), NodeParam.class);

		if (nodeParam.getIteratorType().equals(IteratorType.ByArray.getCode())) {
			if (CollectionUtils.isEmpty(node.getConfig().getInputParams())) {
				result.setSuccess(false);
				result.getErrorInfos().add(("input_params is required"));
			}
		}
		else if (nodeParam.getIteratorType().equals(IteratorType.ByCount.getCode())
				&& nodeParam.getCountLimit() == null) {
			result.setSuccess(false);
			result.getErrorInfos().add(("count_limit is required"));

		}
		return result;
	}

	/**
	 * Configuration parameters for the iterator node
	 */
	@Data
	public static class NodeParam {

		@JsonProperty("iterator_type")
		private String iteratorType = IteratorType.ByArray.getCode();

		@JsonProperty("count_limit")
		private Integer countLimit;

		@JsonProperty("variable_parameters")
		private List<Node.InputParam> variableParameters;

		private List<Branch> terminations;

		private WorkflowConfig block;

	}

	/**
	 * Represents a termination branch in the iteration
	 */
	@Data
	public static class Branch {

		private String id;

		private String label;

		private String logic;

		private List<Condition> conditions;

	}

	/**
	 * Represents a condition for termination
	 */
	@Data
	public static class Condition {

		private String operator;

		private Node.InputParam left;

		private Node.InputParam right;

	}

	/**
	 * Logical operators for condition evaluation
	 */
	private enum Logic {

		and, or

	}

	@Override
	public String getNodeType() {
		return NodeTypeEnum.ITERATOR.getCode();
	}

	@Override
	public String getNodeDescription() {
		return NodeTypeEnum.ITERATOR.getDesc();
	}

}
