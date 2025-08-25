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
import com.alibaba.cloud.ai.studio.runtime.domain.Result;
import com.alibaba.cloud.ai.studio.runtime.domain.mcp.McpServerCallToolRequest;
import com.alibaba.cloud.ai.studio.runtime.domain.mcp.McpServerCallToolResponse;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.Edge;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.Node;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.NodeResult;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.NodeStatusEnum;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.NodeTypeEnum;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.ValueFromEnum;
import com.alibaba.cloud.ai.studio.runtime.utils.JsonUtils;
import com.alibaba.cloud.ai.studio.core.base.service.McpServerService;
import com.alibaba.cloud.ai.studio.core.config.CommonConfig;
import com.alibaba.cloud.ai.studio.core.base.manager.RedisManager;
import com.alibaba.cloud.ai.studio.core.workflow.WorkflowContext;
import com.alibaba.cloud.ai.studio.core.utils.common.VariableUtils;
import com.alibaba.cloud.ai.studio.core.workflow.WorkflowInnerService;
import com.alibaba.cloud.ai.studio.core.workflow.processor.AbstractExecuteProcessor;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Maps;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP (Model Control Protocol) Node Processor
 * <p>
 * This processor is responsible for handling MCP node execution in the workflow. It
 * manages the interaction with MCP servers and tools, handling requests and responses.
 * <p>
 * Features: 1. MCP server tool execution 2. Parameter conversion and validation 3.
 * Response handling and error management 4. Integration with workflow context
 *
 * @author guning.lt
 * @version 1.0.0-M1
 */
@Component("MCPExecuteProcessor")
@Slf4j
public class MCPExecuteProcessor extends AbstractExecuteProcessor {

	private final McpServerService mcpServerService;

	/**
	 * Constructor for MCPExecuteProcessor
	 * @param redisManager Redis manager for caching
	 * @param workflowInnerService Workflow inner service for context management
	 * @param conversationChatMemory Chat memory for conversation context
	 * @param commonConfig Common configuration settings
	 * @param mcpServerService MCP server service for tool execution
	 */
	public MCPExecuteProcessor(RedisManager redisManager, WorkflowInnerService workflowInnerService,
			ChatMemory conversationChatMemory, CommonConfig commonConfig, McpServerService mcpServerService) {
		super(redisManager, workflowInnerService, conversationChatMemory, commonConfig);
		this.mcpServerService = mcpServerService;
	}

	/**
	 * Execute the MCP node processing This method handles the MCP tool execution process,
	 * including: 1. Request construction 2. Tool execution 3. Response handling 4. Error
	 * management
	 * @param graph The workflow graph
	 * @param node The current node to execute
	 * @param context The workflow context
	 * @return NodeResult containing the MCP execution status and results
	 */
	@Override
	public NodeResult innerExecute(DirectedAcyclicGraph<String, Edge> graph, Node node, WorkflowContext context) {
		NodeResult nodeResult = initNodeResultAndRefreshContext(node, context);
		McpServerCallToolRequest request = constructMcpServerCallToolRequest(node, context);

		Result<McpServerCallToolResponse> responseResult = mcpServerService.callTool(request);
		if (responseResult != null && responseResult.isSuccess()) {
			McpServerCallToolResponse callToolResponse = responseResult.getData();
			nodeResult.setNodeStatus(NodeStatusEnum.SUCCESS.getCode());
			nodeResult.setInput(JsonUtils.toJson(request.getToolParams()));
			Map<String, Object> outputMap = Maps.newHashMap();
			outputMap.put(OUTPUT_DECORATE_PARAM_KEY, callToolResponse);
			nodeResult.setOutput(JsonUtils.toJson(outputMap));
		}
		else {
			nodeResult.setNodeStatus(NodeStatusEnum.FAIL.getCode());
			String errorMsg = "MCP Node execute fail, requestId:" + responseResult.getRequestId() + ", " + "errorMsg:"
					+ responseResult.getCode() + "-" + responseResult.getMessage();
			nodeResult.setError(ErrorCode.WORKFLOW_EXECUTE_ERROR.toError(errorMsg));
			nodeResult.setErrorInfo(errorMsg);
		}
		return nodeResult;
	}

	/**
	 * Construct MCP server call tool request
	 * @param node The current node
	 * @param context The workflow context
	 * @return McpServerCallToolRequest with configured parameters
	 */
	private McpServerCallToolRequest constructMcpServerCallToolRequest(Node node, WorkflowContext context) {
		McpServerCallToolRequest request = new McpServerCallToolRequest();
		NodeParam nodeParam = JsonUtils.fromMap(node.getConfig().getNodeParam(), NodeParam.class);
		request.setServerCode(nodeParam.getServerCode());
		request.setToolName(nodeParam.getToolName());
		request.setWorkspaceId(context.getWorkspaceId());
		request.setToolParams(convertToolParams(node.getConfig().getInputParams(), context));
		request.setRequestId(context.getRequestId());
		return request;
	}

	/**
	 * Convert tool parameters from input parameters
	 * @param inputParams List of input parameters
	 * @param context The workflow context
	 * @return HashMap containing converted tool parameters
	 */
	public HashMap<String, Object> convertToolParams(List<Node.InputParam> inputParams, WorkflowContext context) {
		HashMap<String, Object> inputJsonObj = new HashMap<>();
		inputParams.forEach(inputParam -> {
			if (inputParam.getValueFrom() != null && inputParam.getKey() != null && inputParam.getValue() != null) {
				if (ValueFromEnum.input.name().equals(inputParam.getValueFrom())) {
					inputJsonObj.put(inputParam.getKey(), inputParam.getValue());
				}
				else {
					inputJsonObj.put(inputParam.getKey(),
							VariableUtils.getValueFromPayload(
									VariableUtils.getExpressionFromBracket((String) inputParam.getValue()),
									context.getVariablesMap()));
				}
			}
		});
		return inputJsonObj;
	}

	@Override
	public CheckNodeParamResult checkNodeParam(DirectedAcyclicGraph<String, Edge> graph, Node node) {
		CheckNodeParamResult result = super.checkNodeParam(graph, node);
		CheckNodeParamResult checkParamsResult = checkInputParams(node.getConfig().getInputParams());

		if (checkParamsResult != null && !checkParamsResult.isSuccess()) {
			result.setSuccess(false);
			result.getErrorInfos().addAll(checkParamsResult.getErrorInfos());
		}

		return result;
	}

	@Override
	public String getNodeType() {
		return NodeTypeEnum.MCP.getCode();
	}

	@Override
	public String getNodeDescription() {
		return NodeTypeEnum.MCP.getDesc();
	}

	@Data
	public static class NodeParam {

		/**
		 * Server code for MCP service
		 */
		@JsonProperty("server_code")
		private String serverCode;

		/**
		 * Server name for MCP service
		 */
		@JsonProperty("server_name")
		private String serverName;

		/**
		 * Tool name to be executed
		 */
		@JsonProperty("tool_name")
		private String toolName;

	}

}
