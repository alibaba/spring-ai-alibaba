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
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Parallel Start Node Processor
 * <p>
 * This processor is responsible for handling the start node in a parallel execution
 * workflow. It initializes the parallel execution context and prepares for parallel task
 * execution.
 * <p>
 * Features: 1. Initializes parallel execution context 2. Sets up execution status
 * tracking 3. Prepares result map for parallel tasks 4. Manages execution timing
 *
 * @author guning.lt
 * @version 1.0.0-M1
 */
@Component("ParallelStartExecuteProcessor")
public class ParallelStartExecuteProcessor extends AbstractExecuteProcessor {

	/**
	 * Key for storing execution result in the result map
	 */
	private static final String RESULT_KEY = "result";

	/**
	 * Value indicating successful initialization
	 */
	private static final String SUCCESS_VALUE = "success";

	/**
	 * Constructor for ParallelStartExecuteProcessor
	 * @param redisManager Redis manager for caching
	 * @param workflowInnerService Workflow inner service for context management
	 * @param conversationChatMemory Chat memory for conversation context
	 * @param commonConfig Common configuration settings
	 */
	public ParallelStartExecuteProcessor(RedisManager redisManager, WorkflowInnerService workflowInnerService,
			ChatMemory conversationChatMemory, CommonConfig commonConfig) {
		super(redisManager, workflowInnerService, conversationChatMemory, commonConfig);
	}

	/**
	 * Execute the parallel start node initialization
	 * @param graph The workflow graph
	 * @param node The current node to execute
	 * @param context The workflow context
	 * @return NodeResult containing the initialization status and timing information
	 */
	@Override
	public NodeResult innerExecute(DirectedAcyclicGraph<String, Edge> graph, Node node, WorkflowContext context) {
		long start = System.currentTimeMillis();
		NodeResult startNodeResult = new NodeResult();
		Map<String, Object> resultMap = Maps.newHashMap();
		resultMap.put(RESULT_KEY, SUCCESS_VALUE);
		startNodeResult.setNodeId(node.getId());
		startNodeResult.setNodeName(node.getName());
		startNodeResult.setNodeType(node.getType());
		startNodeResult.setNodeStatus(NodeStatusEnum.EXECUTING.getCode());
		startNodeResult.setInput(JsonUtils.toJson(resultMap));
		startNodeResult.setOutput(JsonUtils.toJson(resultMap));
		startNodeResult.setNodeExecTime((System.currentTimeMillis() - start) + "ms");
		startNodeResult.setUsages(null);
		return startNodeResult;
	}

	@Override
	public String getNodeType() {
		return NodeTypeEnum.PARALLEL_START.getCode();
	}

	@Override
	public String getNodeDescription() {
		return NodeTypeEnum.PARALLEL_START.getDesc();
	}

}
