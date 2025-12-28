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

import com.alibaba.cloud.ai.studio.runtime.exception.BizException;
import com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.Edge;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.Node;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.NodeResult;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.NodeStatusEnum;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.NodeTypeEnum;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.ValueFromEnum;
import com.alibaba.cloud.ai.studio.runtime.utils.JsonUtils;
import com.alibaba.cloud.ai.studio.core.config.CommonConfig;
import com.alibaba.cloud.ai.studio.core.base.manager.RedisManager;
import com.alibaba.cloud.ai.studio.core.workflow.WorkflowContext;
import com.alibaba.cloud.ai.studio.core.utils.common.VariableUtils;
import com.alibaba.cloud.ai.studio.core.workflow.WorkflowInnerService;
import com.alibaba.cloud.ai.studio.core.workflow.processor.AbstractExecuteProcessor;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.studio.core.utils.common.VariableUtils.getValueFromPayload;

/**
 * Processor for variable assignment operations in workflows Handles the assignment of
 * values between variables
 *
 * @version 1.0.0-beta
 */
@Slf4j
@Component("VariableAssignExecuteProcessor")
public class VariableAssignExecuteProcessor extends AbstractExecuteProcessor {

	public VariableAssignExecuteProcessor(RedisManager redisManager, WorkflowInnerService workflowInnerService,
			ChatMemory conversationChatMemory, CommonConfig commonConfig) {
		super(redisManager, workflowInnerService, conversationChatMemory, commonConfig);
	}

	/**
	 * Executes the variable assignment operation Processes pairs of variables (left and
	 * right) and assigns values accordingly
	 * @param graph The directed acyclic graph representing the workflow
	 * @param node The current node to be executed
	 * @param context The workflow context containing variables and state
	 * @return NodeResult containing the execution result
	 */
	@Override
	public NodeResult innerExecute(DirectedAcyclicGraph<String, Edge> graph, Node node, WorkflowContext context) {

		// init node result with executing status and refresh context
		NodeResult nodeResult = initNodeResultAndRefreshContext(node, context);

		try {
			NodeParam config = JsonUtils.fromMap(node.getConfig().getNodeParam(), NodeParam.class);
			List<Pair> inputs = config.getInputs();
			List<Object> inputList = new ArrayList<>();
			// Collect input information
			for (Pair pair : inputs) {
				Node.InputParam left = pair.getLeft();
				Node.InputParam right = pair.getRight();
				if (left.getValue() == null || right.getValue() == null) {
					// Skip if expression is null, no modification needed
					continue;
				}
				Map<String, Object> object = new HashMap<>();
				String leftExpression = VariableUtils.getExpressionFromBracket((String) left.getValue());
				object.put("left", getValueFromPayload(leftExpression, context.getVariablesMap()) == null ? ""
						: getValueFromPayload(leftExpression, context.getVariablesMap()));
				if (right.getValueFrom().equals(ValueFromEnum.refer.name())) {
					String rightExpression = VariableUtils.getExpressionFromBracket((String) right.getValue());
					object.put("right", getValueFromPayload(rightExpression, context.getVariablesMap()) == null ? ""
							: getValueFromPayload(rightExpression, context.getVariablesMap()));
				}
				else if (right.getValueFrom().equals(ValueFromEnum.clear.name())) {
					// Clear the variable value
					object.put("right", null);
				}
				else {
					object.put("right", right.getValue());
				}
				inputList.add(object);
			}
			nodeResult.setInput(JsonUtils.toJson(inputList));

			// Process variable assignments
			for (Pair pair : inputs) {
				Node.InputParam left = pair.getLeft();
				Node.InputParam right = pair.getRight();
				String leftExpression = VariableUtils.getExpressionFromBracket((String) left.getValue());
				if (left.getValue() == null
						|| (right.getValue() == null && !right.getValueFrom().equals(ValueFromEnum.clear.name()))) {
					// Skip if expression is null, no modification needed
					continue;
				}
				Object valueFromPayload;
				if (right.getValueFrom().equals(ValueFromEnum.refer.name())) {
					// Get value from referenced variable
					String rightExpression = VariableUtils.getExpressionFromBracket((String) right.getValue());
					valueFromPayload = getValueFromPayload(rightExpression, context.getVariablesMap());
				}
				else if (right.getValueFrom().equals(ValueFromEnum.clear.name())) {
					// Clear the variable value
					valueFromPayload = null;
				}
				else {
					// Use direct input value
					valueFromPayload = right.getValue();
				}
				VariableUtils.setValueForPayload(leftExpression, context.getVariablesMap(), valueFromPayload);
			}
		}
		catch (Exception e) {
			// Handle execution errors
			log.error("VariableNode execute fail ,requestId:{}", context.getRequestId(), e);
			nodeResult.setNodeStatus(NodeStatusEnum.FAIL.getCode());
			HashMap<String, String> resultMap = new HashMap<>();
			resultMap.put("result", "Setup failed");
			nodeResult.setOutput(JsonUtils.toJson(resultMap));
			if (e instanceof BizException) {
				nodeResult.setErrorInfo(((BizException) e).getError().getMessage());
				nodeResult.setError(((BizException) e).getError());
			}
			else {
				nodeResult.setErrorInfo("Variable assignment error:" + e.getMessage());
				nodeResult
					.setError(ErrorCode.WORKFLOW_EXECUTE_ERROR.toError("Variable assignment error:" + e.getMessage()));
			}
			return nodeResult;
		}

		HashMap<String, String> resultMap = new HashMap<>();
		resultMap.put("result", "Assignment successful");
		nodeResult.setOutput(JsonUtils.toJson(resultMap));

		return nodeResult;
	}

	@Override
	public CheckNodeParamResult checkNodeParam(DirectedAcyclicGraph<String, Edge> graph, Node node) {
		CheckNodeParamResult result = super.checkNodeParam(graph, node);
		NodeParam nodeParam = JsonUtils.fromMap(node.getConfig().getNodeParam(), NodeParam.class);
		List<Pair> inputs = nodeParam.getInputs();
		if (inputs.isEmpty()) {
			result.setSuccess(false);
			result.getErrorInfos().add("No variables added");
		}
		for (Pair pair : inputs) {
			Node.InputParam left = pair.getLeft();
			if (left.getValue() == null) {
				result.setSuccess(false);
				result.getErrorInfos().add("[" + left.getKey() + "]" + " is empty");
			}
		}

		return result;
	}

	/**
	 * Configuration class for variable assignments
	 */
	@Data
	public static class NodeParam {

		@JsonProperty("inputs")
		private List<Pair> inputs;

	}

	/**
	 * Represents a pair of input parameters for variable assignment Left is the target
	 * variable, right is the source value
	 */
	@Data
	public static class Pair {

		@JsonProperty("left")
		private Node.InputParam left;

		@JsonProperty("right")
		private Node.InputParam right;

	}

	/**
	 * Returns the node type code
	 * @return The node type code for variable assignment
	 */
	@Override
	public String getNodeType() {
		return NodeTypeEnum.VARIABLE_ASSIGN.getCode();
	}

	/**
	 * Returns the node description
	 * @return The description of the variable assignment node
	 */
	@Override
	public String getNodeDescription() {
		return NodeTypeEnum.VARIABLE_ASSIGN.getDesc();
	}

}
