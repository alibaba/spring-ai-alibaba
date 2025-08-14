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
import com.alibaba.cloud.ai.studio.runtime.exception.BizException;
import com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode;
import com.alibaba.cloud.ai.studio.runtime.utils.JsonUtils;
import com.alibaba.cloud.ai.studio.core.config.CommonConfig;
import com.alibaba.cloud.ai.studio.core.base.manager.RedisManager;
import com.alibaba.cloud.ai.studio.core.workflow.WorkflowConfig;
import com.alibaba.cloud.ai.studio.core.workflow.WorkflowContext;
import com.alibaba.cloud.ai.studio.core.utils.concurrent.ThreadPoolUtils;
import com.alibaba.cloud.ai.studio.core.utils.common.VariableUtils;
import com.alibaba.cloud.ai.studio.core.workflow.WorkflowInnerService;
import com.alibaba.cloud.ai.studio.core.workflow.processor.AbstractExecuteProcessor;
import com.alibaba.cloud.ai.studio.core.workflow.runtime.WorkflowExecuteManager;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static com.alibaba.cloud.ai.studio.core.utils.common.VariableUtils.getValueFromPayload;

/**
 * Parallel Execution Node Processor
 * <p>
 * This processor is responsible for executing multiple workflow tasks in parallel,
 * supporting concurrent execution of sub-workflows with configurable batch size and
 * concurrency limits.
 * <p>
 * Key Features: 1. Parallel execution of sub-workflows with configurable concurrency 2.
 * Batch processing with size limits and monitoring 3. Dynamic task scheduling and
 * resource management 4. Real-time progress monitoring and status tracking 5. Error
 * handling and task failure management 6. Context isolation for parallel tasks 7. Result
 * aggregation and output formatting 8. Redis-based state management for distributed
 * execution
 *
 * @author guning.lt
 * @version 1.0.0-M1
 */
@Slf4j
@Component("ParallelExecuteProcessor")
public class ParallelExecuteProcessor extends AbstractExecuteProcessor {

	/** Maximum allowed batch size for parallel execution */
	private static final int MAX_BATCH_SIZE = 200;

	/** Manager for workflow execution */
	private final WorkflowExecuteManager workflowExecuteManager;

	/**
	 * Constructor for ParallelExecuteProcessor
	 * @param workflowExecuteManager Manager for workflow execution
	 * @param redisManager Redis manager for state management
	 * @param workflowInnerService Inner workflow service
	 * @param conversationChatMemory Chat memory for conversations
	 * @param commonConfig Common configuration settings
	 */
	public ParallelExecuteProcessor(WorkflowExecuteManager workflowExecuteManager, RedisManager redisManager,
			WorkflowInnerService workflowInnerService, ChatMemory conversationChatMemory, CommonConfig commonConfig) {
		super(redisManager, workflowInnerService, conversationChatMemory, commonConfig);
		this.workflowExecuteManager = workflowExecuteManager;
	}

	/**
	 * Executes the parallel node in the workflow
	 * @param graph The workflow graph containing node relationships
	 * @param node The parallel node to execute
	 * @param context The workflow execution context
	 * @return NodeResult containing execution status and aggregated results
	 */
	@Override
	public NodeResult innerExecute(DirectedAcyclicGraph<String, Edge> graph, Node node, WorkflowContext context) {

		// Build result node
		NodeResult nodeResult = new NodeResult();
		nodeResult.setInput(JsonUtils.toJson(constructInputParamsMap(node, context)));
		nodeResult.setNodeId(node.getId());
		nodeResult.setNodeType(node.getType());
		nodeResult.setNodeName(node.getName());
		nodeResult.setNodeStatus(NodeStatusEnum.EXECUTING.getCode());
		context.getNodeResultMap().put(node.getId(), nodeResult);
		workflowInnerService.refreshContextCache(context);
		log.info("ParallelExecuteProcessor is executing requestId:{}", context.getRequestId());

		NodeParam nodeParam = JsonUtils.fromMap(node.getConfig().getNodeParam(), NodeParam.class);
		// Get input List mapping table
		Map<String, List<Object>> itemListMap = buildItemMap(node.getConfig().getInputParams(), context);

		// Determine batch count
		int maxIndex = Integer.MAX_VALUE;
		if (itemListMap.isEmpty()) {
			maxIndex = 0;
		}
		else {
			for (String key : itemListMap.keySet()) {
				List<Object> list = itemListMap.get(key);
				maxIndex = Math.min(maxIndex, list.size());
			}
			// Batch processing limit is 100
			maxIndex = Math.min(maxIndex, MAX_BATCH_SIZE);
		}

		// Record nodes in sub-canvas
		List<String> nodeIds = nodeParam.getBlock().getNodes().stream().map(Node::getId).collect(Collectors.toList());

		// Batch processing node sub-canvas configuration
		WorkflowConfig appOrchestraConfig = nodeParam.getBlock();

		log.info(
				"ParallelExecuteProcessor is executing requestId:{} ,itemListMap:{} ,parallelNodeIds:{} ,WorkflowConfig:{} ,maxIndex:{}",
				context.getRequestId(), JsonUtils.toJson(itemListMap), nodeIds, JsonUtils.toJson(appOrchestraConfig),
				maxIndex);

		// Copy context, loop body streaming output will affect context, copy
		// finalCloneContext each time
		WorkflowContext finalCloneContext = WorkflowContext.deepCopy(context);

		// Batch processing node concurrency
		int concurrentSize = nodeParam.getConcurrentSize() > 0 & nodeParam.getConcurrentSize() <= 10
				? nodeParam.getConcurrentSize() : 1;
		Semaphore semaphore = new Semaphore(concurrentSize);
		CountDownLatch latch = new CountDownLatch(maxIndex);
		// Save sub-task results
		ArrayList<WorkflowContext> contextList = new ArrayList<>();
		// Current node status flag
		AtomicBoolean subTaskStatus = new AtomicBoolean(true);

		// Record successful nodes in sub-tasks
		List<HashSet<String>> nodeSetList = new ArrayList<>();
		for (int i = 0; i < maxIndex; i++) {
			nodeSetList.add(new HashSet<>());
		}

		// Start monitoring thread for console streaming output
		if (workflowInnerService.checkRedisNecessity(context)) {
			int finalMaxIndex = maxIndex;
			ThreadPoolUtils.taskExecutorService.submit(() -> {
				try {
					executeMonitorThread(context, contextList, nodeIds, node, subTaskStatus, finalMaxIndex,
							nodeSetList);
				}
				catch (Exception e) {
					log.error("ParallelProcessor task Schedule execute,requestId:{} error:{}", context.getRequestId(),
							e.getMessage());
				}
			});
		}

		for (int index = 0; index < maxIndex; index++) {

			// If sub-task execution fails, end batch processing
			if (!subTaskStatus.get()) {
				break;
			}

			// Update context loop node configuration for sub-canvas reference
			Map<String, Object> object = new HashMap<>();
			for (String key : itemListMap.keySet()) {
				object.put(key, itemListMap.get(key).get(index));
			}
			object.put("index", index + 1);
			finalCloneContext.getVariablesMap().put(node.getId(), object);

			// Configure sub-workflow information, add sub-task id
			WorkflowContext cloneContext = WorkflowContext.deepCopy(finalCloneContext);
			String newTaskId = finalCloneContext.getTaskId() + "-" + node.getId() + "-" + index;
			cloneContext.setLock(new ReentrantLock());
			cloneContext.setExecuteOrderList(new CopyOnWriteArrayList<>());
			cloneContext.setTaskId(newTaskId);
			context.getSubTaskIdSet().add(newTaskId);

			// Execute sub-task
			int finalIndex = index;
			try {
				// Store sub-task results
				contextList.add(cloneContext);
				if (semaphore.tryAcquire(150, TimeUnit.SECONDS)) {
					ThreadPoolUtils.taskExecutorService.submit(() -> {
						// Initialize batch node batch span
						try {
							// Execute sub-task
							workflowExecuteManager.syncExecute(appOrchestraConfig, cloneContext);
						}
						catch (Exception e) {
							// Handle task execution exceptions
							log.error("Task error batchIndex:{},RequestID:{}:", finalIndex, context.getRequestId(), e);
						}
						finally {
							// Sub-task execution fails, set current node status flag
							if (cloneContext.getTaskStatus().equals(NodeStatusEnum.FAIL.getCode())) {
								context.setErrorInfo(cloneContext.getErrorInfo());
								context.setErrorCode(cloneContext.getErrorCode());
								subTaskStatus.set(false);
							}
							// Release permit
							semaphore.release();
							latch.countDown();
						}
					});
				}
			}
			catch (Exception e) {
				// Handle task execution exceptions
				log.error("Task error batchIndex:{},RequestID:{}:", finalIndex, context.getRequestId(), e);
			}

		}

		// Wait for all sub-tasks to complete, or task failure
		while (true) {
			// Sub-task execution fails or task times out
			if (!subTaskStatus.get()) {
				extraSubTaskResult(context, contextList, nodeIds, node, nodeSetList);
				NodeResult nodeResultFail = new NodeResult();
				nodeResultFail.setNodeId(node.getId());
				nodeResultFail.setNodeName(node.getName());
				nodeResultFail.setNodeType(node.getType());
				nodeResultFail.setInput(JsonUtils.toJson(constructInputParamsMap(node, context)));
				HashMap<String, Object> stringObjectHashMap = new HashMap<>();
				nodeResultFail.setNodeStatus(NodeStatusEnum.FAIL.getCode());
				nodeResultFail.setErrorInfo(context.getErrorInfo());
				nodeResultFail.setErrorCode(context.getErrorCode());
				nodeResultFail.setOutput(JsonUtils.toJson(stringObjectHashMap));
				log.info("ParallelExecuteProcessor is fail requestId:{} ,nodeResultFail:{} ", context.getRequestId(),
						JsonUtils.toJson(nodeResultFail));
				return nodeResultFail;
			}
			else {
				// Wait for all sub-tasks to execute successfully
				if (latch.getCount() == 0 && subTaskStatus.get()) {
					break;
				}
			}
			try {
				Thread.sleep(100);
			}
			catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}

		// Construct loop node results
		extraSubTaskResult(context, contextList, nodeIds, node, nodeSetList);

		// Create Comparator using lambda expression, sort by Index in ascending order
		Comparator<NodeResult> byIndex = (p1, p2) -> Integer.compare(p1.getIndex(), p2.getIndex());
		for (String nodId : nodeIds) {
			if (context.getNodeResultMap().containsKey(nodId)) {
				context.getNodeResultMap().get(nodId).getBatches().sort(byIndex);
			}
		}
		HashMap<String, Object> stringObjectHashMap = constructOutput(node, context, nodeIds);
		nodeResult.setOutput(JsonUtils.toJson(stringObjectHashMap));
		return nodeResult;
	}

	/**
	 * Executes the monitoring thread for real-time progress tracking
	 * @param context The workflow context
	 * @param contextList List of sub-task contexts
	 * @param nodeIds List of node IDs in sub-workflow
	 * @param node The parallel node
	 * @param subTaskStatus Status flag for sub-tasks
	 * @param maxIndex Maximum batch index
	 * @param nodeSetList List of successful node sets
	 */
	private void executeMonitorThread(WorkflowContext context, ArrayList<WorkflowContext> contextList,
			List<String> nodeIds, Node node, AtomicBoolean subTaskStatus, int maxIndex,
			List<HashSet<String>> nodeSetList) {

		while (subTaskStatus.get()) {

			// If timeout, stop loop
			long seconds = (System.currentTimeMillis() - context.getStartTime()) / 1000;
			if (seconds > InvokeSourceEnum.valueOf(context.getInvokeSource()).getTimeoutSeconds()) {
				subTaskStatus.set(false);
				context.setErrorInfo("task timeout");
				return;
			}

			// Check sub-task execution status, if all sub-tasks are successful, stop
			// listening
			String taskStatus = NodeStatusEnum.SUCCESS.getCode();
			if (contextList.size() != maxIndex) {
				taskStatus = NodeStatusEnum.EXECUTING.getCode();
			}
			else {
				for (WorkflowContext WorkflowContext : contextList) {
					if (WorkflowContext.getTaskStatus().equals(NodeStatusEnum.EXECUTING.getCode())) {
						taskStatus = NodeStatusEnum.EXECUTING.getCode();
						break;
					}
				}
			}

			if (NodeStatusEnum.SUCCESS.getCode().equals(taskStatus)) {
				// Batch processing node, console streaming output
				extraSubTaskResult(context, contextList, nodeIds, node, nodeSetList);
				workflowInnerService.refreshContextCache(context);
				return;
			}

			// Batch processing node, console streaming output
			extraSubTaskResult(context, contextList, nodeIds, node, nodeSetList);
			workflowInnerService.refreshContextCache(context);
			try {
				Thread.sleep(commonConfig.getWorkflowAwaitingTime());
			}
			catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}

	}

	/**
	 * Extracts and processes sub-task results
	 * @param context The workflow context
	 * @param contextList List of sub-task contexts
	 * @param nodeIds List of node IDs in sub-workflow
	 * @param node The parallel node
	 * @param nodeSetList List of successful node sets
	 */
	private void extraSubTaskResult(WorkflowContext context, ArrayList<WorkflowContext> contextList,
			List<String> nodeIds, Node node, List<HashSet<String>> nodeSetList) {
		ConcurrentHashMap<String, NodeResult> nodeResultMap = context.getNodeResultMap();
		for (int index = 0; index < contextList.size(); index++) {

			// Iterate through each sub-task
			WorkflowContext subContext = contextList.get(index);
			ConcurrentHashMap<String, NodeResult> subNodeResultMap = subContext.getNodeResultMap();
			for (String nodeId : nodeIds) {

				// Iterate through each node in sub-task
				if (!context.getExecuteOrderList().contains(nodeId)) {
					context.getExecuteOrderList().add(nodeId);
				}
				// Get result of corresponding node in sub-task
				if (!subNodeResultMap.containsKey(nodeId)) {
					continue;
				}
				NodeResult childNode = subNodeResultMap.get(nodeId);
				if (Objects.equals(childNode.getNodeStatus(), NodeStatusEnum.SUCCESS.getCode())) {
					if (!nodeSetList.get(index).contains(nodeId)) {
						// Record successful or failed node
						nodeSetList.get(index).add(nodeId);
						// Set main task, corresponding node information and status
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
							// Set parent node
							nodeResult.setParentNodeId(node.getId());
							nodeResultMap.put(nodeId, nodeResult);
						}
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
							Long executedTimeLong = Math.max(Long.parseLong(executedTime.replace("ms", "")),
									Long.parseLong(subExecutedTime.replace("ms", "")));
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
						// Set parent node
						nodeResult.setParentNodeId(node.getId());
						nodeResultMap.put(nodeId, nodeResult);
					}
					nodeResult.setNodeStatus(childNode.getNodeStatus());

				}
				else if (Objects.equals(childNode.getNodeStatus(), NodeStatusEnum.FAIL.getCode())) {
					if (!nodeSetList.get(index).contains(nodeId)) {
						// Record successful or failed node
						nodeSetList.get(index).add(nodeId);
						// Set main task, corresponding node information and status
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
							// Set parent node
							nodeResult.setParentNodeId(node.getId());
							nodeResultMap.put(nodeId, nodeResult);
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
							Long executedTimeLong = Math.max(Long.parseLong(executedTime.replace("ms", "")),
									Long.parseLong(subExecutedTime.replace("ms", "")));
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
					}

				}

			}

		}

	}

	/**
	 * Constructs the output map for the parallel node
	 * @param node The parallel node
	 * @param context The workflow context
	 * @param nodeIds List of node IDs in sub-workflow
	 * @return Map containing aggregated results
	 */
	public HashMap<String, Object> constructOutput(Node node, WorkflowContext context, List<String> nodeIds) {

		// Construct loop node output reusable variable set, used for variable replacement
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
			Object valueFromPayload = getValueFromPayload(expression, subResultMap);
			log.info(
					"log used for debug getValueFromPayload,requestId:{},expression:{},subResultMap:{},valueFromPayload:{},variableMap:{}",
					context.getRequestId(), expression, JsonUtils.toJson(subResultMap), JsonUtils.toJson(subResultMap),
					JsonUtils.toJson(context.getVariablesMap()));
			resultMap.put(key, valueFromPayload);

		}

		return resultMap;
	}

	/**
	 * Builds the item map from input parameters
	 * @param paramsArray List of input parameters
	 * @param context The workflow context
	 * @return Map of parameter lists
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
				throw new BizException(ErrorCode.WORKFLOW_CONFIG_INVALID.toError());
			}
			params.put(param.getKey(), list);
		});
		return params;
	}

	@Override
	public CheckNodeParamResult checkNodeParam(DirectedAcyclicGraph<String, Edge> graph, Node node) {
		CheckNodeParamResult result = super.checkNodeParam(graph, node);
		CheckNodeParamResult inputParamsResult = checkInputParams(node.getConfig().getInputParams());
		if (!inputParamsResult.isSuccess()) {
			result.setSuccess(false);
			result.getErrorInfos().addAll(inputParamsResult.getErrorInfos());
		}
		return result;
	}

	/**
	 * Configuration parameters for parallel execution node
	 */
	@Data
	public static class NodeParam {

		/** Number of concurrent tasks allowed */
		@JsonProperty("concurrent_size")
		private Integer concurrentSize;

		/** Size of each batch for processing */
		@JsonProperty("batch_size")
		private Integer batchSize;

		/** Workflow configuration block */
		@JsonProperty("block")
		private WorkflowConfig block;

	}

	/**
	 * Gets the node type
	 * @return String representing the node type
	 */
	@Override
	public String getNodeType() {
		return NodeTypeEnum.PARALLEL.getCode();
	}

	/**
	 * Gets the node description
	 * @return String containing the node description
	 */
	@Override
	public String getNodeDescription() {
		return NodeTypeEnum.PARALLEL.getDesc();
	}

}
