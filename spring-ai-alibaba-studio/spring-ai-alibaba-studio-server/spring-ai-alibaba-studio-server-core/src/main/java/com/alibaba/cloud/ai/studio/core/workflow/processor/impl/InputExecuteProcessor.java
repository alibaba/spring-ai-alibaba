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

import com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode;
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
import lombok.extern.slf4j.Slf4j;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.alibaba.cloud.ai.studio.core.base.constants.CacheConstants.WORKFLOW_TASK_CONTEXT_PREFIX;

/**
 * Input Node Processor
 * <p>
 * This processor is responsible for handling input nodes in the workflow. It manages user
 * input collection and timeout handling for workflow execution.
 * <p>
 * Features: 1. Manages input node state transitions 2. Handles input timeout scenarios 3.
 * Supports asynchronous input collection 4. Maintains workflow context during input
 * waiting
 *
 * @version 1.0.0-M1
 */
@Slf4j
@Component("InputExecuteProcessor")
public class InputExecuteProcessor extends AbstractExecuteProcessor {

	/**
	 * Constructor for InputExecuteProcessor
	 * @param redisManager Redis manager for caching
	 * @param workflowInnerService Workflow inner service for context management
	 * @param conversationChatMemory Chat memory for conversation context
	 * @param commonConfig Common configuration settings
	 */
	public InputExecuteProcessor(RedisManager redisManager, WorkflowInnerService workflowInnerService,
			ChatMemory conversationChatMemory, CommonConfig commonConfig) {
		super(redisManager, workflowInnerService, conversationChatMemory, commonConfig);
	}

	/**
	 * Execute the input node processing This method handles the input collection process,
	 * including: 1. Setting up the node in pause state 2. Waiting for user input 3.
	 * Handling timeout scenarios 4. Managing state transitions
	 * @param graph The workflow graph
	 * @param node The current node to execute
	 * @param context The workflow context
	 * @return NodeResult containing the input processing status and results
	 */
	@Override
	public NodeResult innerExecute(DirectedAcyclicGraph<String, Edge> graph, Node node, WorkflowContext context) {
		// Set to pause state
		NodeResult nodeResult = new NodeResult();
		nodeResult.setNodeId(node.getId());
		nodeResult.setNodeName(node.getName());
		nodeResult.setNodeType(node.getType());
		nodeResult.setUsages(null);
		List<Node.OutputParam> outputParams = node.getConfig().getOutputParams();
		nodeResult.setInput(JsonUtils.toJson(outputParams));
		nodeResult.setNodeStatus(NodeStatusEnum.PAUSE.getCode());
		context.setTaskStatus(NodeStatusEnum.PAUSE.getCode());
		context.getNodeResultMap().put(node.getId(), nodeResult);
		// Force refresh context
		// workflowInnerService.forceRefreshContextCache(context);

		long startTime = System.currentTimeMillis();
		long timeout = commonConfig.getInputTimeout(); // 5 minutes timeout

		while (NodeStatusEnum.PAUSE.getCode().equals(nodeResult.getNodeStatus())) {
			// Re-fetch node result
			WorkflowContext wfContext = redisManager
				.get(WORKFLOW_TASK_CONTEXT_PREFIX + context.getWorkspaceId() + "_" + context.getTaskId());
			nodeResult = wfContext.getNodeResultMap().get(node.getId());
			try {
				// Avoid CPU spinning, wait 500ms each time
				Thread.sleep(500);
			}
			catch (InterruptedException e) {
				log.error("Interrupted while waiting for input node result", e);
				Thread.currentThread().interrupt();
			}

			// Check for timeout
			if (System.currentTimeMillis() - startTime > timeout) {
				nodeResult.setNodeStatus(NodeStatusEnum.FAIL.getCode());
				nodeResult.setErrorInfo("Input node waiting timeout");
				nodeResult.setError(ErrorCode.WORKFLOW_EXECUTE_ERROR.toError("input node waiting timeout"));
				return nodeResult;
			}
		}

		// Restore workflow context
		nodeResult.setNodeStatus(NodeStatusEnum.EXECUTING.getCode());
		context.setTaskStatus(NodeStatusEnum.EXECUTING.getCode());
		context.getNodeResultMap().put(node.getId(), nodeResult);

		return nodeResult;
	}

	@Override
	public String getNodeType() {
		return NodeTypeEnum.INPUT.getCode();
	}

	@Override
	public String getNodeDescription() {
		return NodeTypeEnum.INPUT.getDesc();
	}

}
