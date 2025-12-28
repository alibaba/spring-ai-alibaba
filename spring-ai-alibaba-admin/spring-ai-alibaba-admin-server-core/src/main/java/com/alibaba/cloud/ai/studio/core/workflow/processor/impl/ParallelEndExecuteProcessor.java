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
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.Node;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.NodeResult;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.NodeStatusEnum;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.NodeTypeEnum;
import com.alibaba.cloud.ai.studio.runtime.utils.JsonUtils;
import com.alibaba.cloud.ai.studio.core.config.CommonConfig;
import com.alibaba.cloud.ai.studio.core.base.manager.RedisManager;
import com.alibaba.cloud.ai.studio.core.workflow.WorkflowContext;
import com.alibaba.cloud.ai.studio.core.workflow.WorkflowInnerService;
import com.alibaba.cloud.ai.studio.core.workflow.processor.AbstractExecuteProcessor;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Parallel End Node Processor
 * <p>
 * This processor is responsible for handling the end node in a parallel execution
 * workflow. It manages the finalization of parallel tasks, result aggregation, and task
 * status updates.
 * <p>
 * Features: 1. Finalizes parallel execution 2. Aggregates results and updates task status
 * 3. Handles node result and error propagation 4. Maintains node execution timing
 *
 * @author guning.lt
 * @version 1.0.0-M1
 */
@Slf4j
@Component("ParallelEndExecuteProcessor")
public class ParallelEndExecuteProcessor extends AbstractExecuteProcessor {

	/**
	 * Constructor for ParallelEndExecuteProcessor
	 * @param redisManager Redis manager for caching
	 * @param workflowInnerService Workflow inner service for context management
	 * @param conversationChatMemory Chat memory for conversation context
	 * @param commonConfig Common configuration settings
	 */
	public ParallelEndExecuteProcessor(RedisManager redisManager, WorkflowInnerService workflowInnerService,
			ChatMemory conversationChatMemory, CommonConfig commonConfig) {
		super(redisManager, workflowInnerService, conversationChatMemory, commonConfig);
	}

	/**
	 * Execute the parallel end node processing
	 * @param graph The workflow graph
	 * @param node The current node to execute
	 * @param context The workflow context
	 * @return NodeResult containing the end node execution status and results
	 */
	@Override
	public NodeResult innerExecute(DirectedAcyclicGraph<String, Edge> graph, Node node, WorkflowContext context) {
		long start = System.currentTimeMillis();
		try {
			NodeResult endNodeResult = new NodeResult();
			Map<String, Object> resultMap = Maps.newHashMap();
			resultMap.put("result", "success");
			endNodeResult.setNodeId(node.getId());
			endNodeResult.setNodeName(node.getName());
			endNodeResult.setNodeType(node.getType());
			endNodeResult.setNodeStatus(NodeStatusEnum.EXECUTING.getCode());
			endNodeResult.setNodeExecTime((System.currentTimeMillis() - start) + "ms");
			endNodeResult.setUsages(null);
			endNodeResult.setOutput(JsonUtils.toJson(resultMap));
			return endNodeResult;
		}
		catch (Exception e) {
			log.error("innerExecute error:{}", JsonUtils.toJson(node), e);
			// Node execution error, save result and record error info
			return NodeResult.error(node, e.getMessage());
		}
	}

	/**
	 * Parallel end node does not need to handle variable values
	 */
	@Override
	public void handleVariables(DirectedAcyclicGraph<String, Edge> graph, Node node, WorkflowContext context,
			NodeResult nodeResult) {
		// Parallel end node does not need to handle variable values
	}

	/**
	 * Special handling for end node in parallel batch processing
	 * @param graph The workflow graph
	 * @param node The current node
	 * @param context The workflow context
	 * @param nodeResult The node result
	 * @param startTime The start time of node execution
	 */
	@Override
	public void handleNodeResult(DirectedAcyclicGraph<String, Edge> graph, Node node, WorkflowContext context,
			NodeResult nodeResult, long startTime) {
		if (nodeResult == null) {
			return;
		}
		if (nodeResult.getNodeStatus().equals(NodeStatusEnum.FAIL.getCode())) {
			// If any task fails, set the task status to FAIL directly
			context.setTaskStatus(NodeStatusEnum.FAIL.getCode());
			context.setErrorInfo(nodeResult.getErrorInfo());
			context.setError(nodeResult.getError());
		}
		nodeResult.setNodeExecTime((System.currentTimeMillis() - startTime) + "ms");
		if (!nodeResult.getNodeStatus().equals(NodeStatusEnum.FAIL.getCode())) {
			nodeResult.setNodeStatus(NodeStatusEnum.SUCCESS.getCode());
			// If end node executes successfully, set task status to SUCCESS
			context.setTaskStatus(NodeStatusEnum.SUCCESS.getCode());
			// Set the final task result to the output of the end node
			context.setTaskResult(nodeResult.getOutput());
		}
		// Supplement missing nodeResult properties
		nodeResult.setNodeName(node.getName() == null ? node.getId() : node.getName());
		context.getNodeResultMap().put(node.getId(), nodeResult);
	}

	@Override
	public String getNodeType() {
		return NodeTypeEnum.PARALLEL_END.getCode();
	}

	@Override
	public String getNodeDescription() {
		return NodeTypeEnum.PARALLEL_END.getDesc();
	}

}
