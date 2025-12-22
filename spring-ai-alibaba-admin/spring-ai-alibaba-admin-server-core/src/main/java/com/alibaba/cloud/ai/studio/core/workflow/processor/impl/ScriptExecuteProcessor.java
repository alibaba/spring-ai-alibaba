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
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.Edge;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.Node;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.NodeResult;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.NodeStatusEnum;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.NodeTypeEnum;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.ValueFromEnum;
import com.alibaba.cloud.ai.studio.runtime.utils.JsonUtils;
import com.alibaba.cloud.ai.studio.core.config.CommonConfig;
import com.alibaba.cloud.ai.studio.core.base.manager.RedisManager;
import com.alibaba.cloud.ai.studio.core.base.manager.SandboxManager;
import com.alibaba.cloud.ai.studio.core.workflow.WorkflowContext;
import com.alibaba.cloud.ai.studio.core.utils.common.VariableUtils;
import com.alibaba.cloud.ai.studio.core.workflow.WorkflowInnerService;
import com.alibaba.cloud.ai.studio.core.workflow.processor.AbstractExecuteProcessor;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Maps;
import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Script Node Processor
 * <p>
 * This processor is responsible for executing script nodes in the workflow, supporting
 * both Python and JavaScript scripts. It provides a secure execution environment through
 * sandbox isolation and handles script input/output parameter management.
 * <p>
 * Key Features: 1. Supports Python and JavaScript script execution 2. Provides sandbox
 * isolation for secure script execution 3. Manages script input/output parameter mapping
 * 4. Handles script execution errors and validation 5. Supports variable injection from
 * workflow context 6. Validates script content and parameters 7. Processes script
 * execution results 8. Manages script execution environment
 *
 * @author Spring AI Alibaba Team
 * @version 1.0.0-M1
 */
@Component("ScriptExecuteProcessor")
public class ScriptExecuteProcessor extends AbstractExecuteProcessor {

	private final SandboxManager sandboxManager;

	public ScriptExecuteProcessor(SandboxManager sandboxManager, RedisManager redisManager,
			WorkflowInnerService workflowInnerService, ChatMemory conversationChatMemory, CommonConfig commonConfig) {
		super(redisManager, workflowInnerService, conversationChatMemory, commonConfig);
		this.sandboxManager = sandboxManager;
	}

	@Override
	public String getNodeType() {
		return NodeTypeEnum.SCRIPT.getCode();
	}

	@Override
	public String getNodeDescription() {
		return NodeTypeEnum.SCRIPT.getDesc();
	}

	/**
	 * Executes the script node in the workflow
	 * @param graph The workflow graph
	 * @param node The script node to execute
	 * @param context The workflow context
	 * @return NodeResult containing execution status and output
	 */
	@Override
	public NodeResult innerExecute(DirectedAcyclicGraph<String, Edge> graph, Node node, WorkflowContext context) {
		NodeResult nodeResult = new NodeResult();
		nodeResult.setNodeId(node.getId());
		nodeResult.setNodeName(node.getName());
		nodeResult.setNodeType(node.getType());
		nodeResult.setNodeStatus(NodeStatusEnum.SUCCESS.getCode());

		NodeParam nodeParam = JsonUtils.fromMap(node.getConfig().getNodeParam(), NodeParam.class);

		// Get script type and content
		String scriptType = nodeParam.getScriptType();
		String scriptContent = nodeParam.getScriptContent();

		// Validate script type
		if (!ScriptType.python.name().equals(scriptType) && !ScriptType.javascript.name().equals(scriptType)) {
			nodeResult.setNodeStatus(NodeStatusEnum.FAIL.getCode());
			nodeResult.setErrorCode(ErrorCode.WORKFLOW_CONFIG_INVALID.getCode());
			nodeResult.setErrorInfo(
					"Unsupported script type: " + scriptType + ", currently only python3 and javascript are supported");
			return nodeResult;
		}

		// Build script variable mapping
		Map<String, Object> localVariableMap = constructScriptVariableMap(node, context);
		Map<String, Object> variableMap = Maps.newHashMap();
		variableMap.put("params", localVariableMap);

		// Execute script in sandbox for container isolation
		Result<String> executeResult;
		if (ScriptType.python.name().equals(scriptType)) {
			// Execute Python script using GraalVM Python
			scriptContent += "\nmain()";
			executeResult = sandboxManager.executePython3Script(scriptContent, variableMap, context.getRequestId());
		}
		else {
			scriptContent += "\nmain()";
			// Execute JavaScript script using traditional method
			executeResult = sandboxManager.executeScript(scriptContent, variableMap, context.getRequestId());
		}

		if (executeResult != null && executeResult.isSuccess()) {
			ScriptExecutionResponse scriptRes = JsonUtils.fromJson(executeResult.getData(),
					ScriptExecutionResponse.class);
			if (scriptRes.getSuccess()) {
				// Script execution successful
				Map<String, Object> outputParamsMap;
				if (scriptRes.getData() instanceof Map) {
					// Process output parameters
					outputParamsMap = constructOutputParamsMap(node, scriptRes.getData(), context);
					nodeResult.setInput(JsonUtils.toJson(decorateInput(localVariableMap)));
					nodeResult.setOutput(JsonUtils.toJson(outputParamsMap));
					nodeResult.setUsages(null);
				}
				else {
					// Output format does not meet requirements
					nodeResult.setNodeStatus(NodeStatusEnum.FAIL.getCode());
					nodeResult.setErrorCode(ErrorCode.WORKFLOW_CONFIG_INVALID.getCode());
					nodeResult
						.setErrorInfo("Output format does not match configuration, raw data: " + scriptRes.getData());
				}
			}
			else {
				// Script execution failed
				nodeResult.setNodeStatus(NodeStatusEnum.FAIL.getCode());
				nodeResult.setErrorCode(ErrorCode.WORKFLOW_CONFIG_INVALID.getCode());
				nodeResult.setErrorInfo("Script node execution failed: " + scriptRes.getMessage());
			}
		}
		else {
			// Sandbox execution failed
			nodeResult.setNodeStatus(NodeStatusEnum.FAIL.getCode());
			nodeResult.setErrorCode(ErrorCode.WORKFLOW_CONFIG_INVALID.getCode());
			String errorMsg = executeResult != null && !executeResult.isSuccess() ? executeResult.getMessage()
					: "Unknown error";
			nodeResult.setErrorInfo("Script node execution failed in sandbox: " + errorMsg);
		}

		// Set input parameters
		nodeResult.setInput(JsonUtils.toJson(decorateInput(localVariableMap)));
		return nodeResult;
	}

	/**
	 * Constructs a variable map for script execution from the workflow context
	 * @param node The script node
	 * @param context The workflow context
	 * @return Map of variables for script execution
	 */
	private Map<String, Object> constructScriptVariableMap(Node node, WorkflowContext context) {
		Map<String, Object> map = Maps.newHashMap();
		List<Node.InputParam> inputParams = node.getConfig().getInputParams();
		if (CollectionUtils.isEmpty(inputParams)) {
			return map;
		}
		inputParams.forEach(scriptParam -> {
			String valueFrom = scriptParam.getValueFrom();
			if (valueFrom.equals(ValueFromEnum.refer.name())) {
				Object value = VariableUtils.getValueFromContext(scriptParam, context);
				map.put(scriptParam.getKey(), value);
			}
			else {
				Object value = VariableUtils.convertValueByType(scriptParam.getKey(), scriptParam.getType(),
						scriptParam.getValue());
				if (value == null) {
					return;
				}
				map.put(scriptParam.getKey(), value);
			}
		});
		return map;
	}

	/**
	 * Supported script types for execution
	 */
	public enum ScriptType {

		python, javascript

	}

	/**
	 * Configuration parameters for script node
	 */
	@Data
	public static class NodeParam {

		@JsonProperty("type")
		private String type;

		@JsonProperty("script_type")
		private String scriptType;

		@JsonProperty("script_content")
		private String scriptContent;

		@JsonProperty("script_params")
		private List<Node.InputParam> scriptParams;

	}

	/**
	 * Validates the script node parameters
	 * @param graph The workflow graph
	 * @param node The script node to validate
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
		String scriptContent = nodeParam.getScriptContent();
		if (StringUtils.isBlank(scriptContent)) {
			result.setSuccess(false);
			result.getErrorInfos().add("[Script] is empty");
		}
		return result;
	}

	/**
	 * Response model for script execution results
	 */
	@Data
	@Accessors(chain = true)
	public static class ScriptExecutionResponse {

		@JsonProperty("data")
		private Object data;

		@JsonProperty("success")
		private Boolean success;

		@JsonProperty("message")
		private String message;

		@JsonProperty("code")
		private String code;

	}

}
