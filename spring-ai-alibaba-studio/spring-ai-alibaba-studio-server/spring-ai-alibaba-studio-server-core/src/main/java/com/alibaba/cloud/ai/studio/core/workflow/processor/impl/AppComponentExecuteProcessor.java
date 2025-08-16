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

import com.alibaba.cloud.ai.studio.runtime.enums.AppComponentTypeEnum;
import com.alibaba.cloud.ai.studio.runtime.domain.agent.AgentResponse;
import com.alibaba.cloud.ai.studio.runtime.domain.chat.ChatMessage;
import com.alibaba.cloud.ai.studio.runtime.domain.chat.MessageRole;
import com.alibaba.cloud.ai.studio.runtime.domain.component.AppComponentRequest;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.Edge;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.Node;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.NodeResult;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.NodeStatusEnum;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.NodeTypeEnum;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.ValueFromEnum;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.debug.WorkflowResponse;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.inner.ShortTermMemory;
import com.alibaba.cloud.ai.studio.runtime.utils.JsonUtils;
import com.alibaba.cloud.ai.studio.core.config.CommonConfig;
import com.alibaba.cloud.ai.studio.core.base.manager.AppComponentManager;
import com.alibaba.cloud.ai.studio.core.base.manager.RedisManager;
import com.alibaba.cloud.ai.studio.core.workflow.WorkflowContext;
import com.alibaba.cloud.ai.studio.core.utils.common.VariableUtils;
import com.alibaba.cloud.ai.studio.core.workflow.WorkflowInnerService;
import com.alibaba.cloud.ai.studio.core.workflow.processor.AbstractExecuteProcessor;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode.APP_COMPONENT_NOT_FOUND_ERROR;

/**
 * Application Component Node Processor
 * <p>
 * This processor is responsible for executing application components within workflows,
 * supporting both agent and workflow component types with streaming and non-streaming
 * execution modes.
 * <p>
 * Key Features: 1. Supports multiple component types (Agent and Workflow) 2. Handles
 * streaming and non-streaming execution modes 3. Manages component input/output parameter
 * mapping 4. Supports short-term memory for context-aware execution 5. Provides real-time
 * execution status updates 6. Handles component execution errors and validation 7.
 * Supports JSON and text output formats 8. Manages component execution context and state
 *
 * @author guning.lt
 * @version 1.0.0-M1
 */
@Slf4j
@Component("AppComponentExecuteProcessor")
public class AppComponentExecuteProcessor extends AbstractExecuteProcessor {

	private final AppComponentManager appComponentManager;

	public AppComponentExecuteProcessor(RedisManager redisManager, WorkflowInnerService workflowInnerService,
			ChatMemory conversationChatMemory, CommonConfig commonConfig, AppComponentManager appComponentManager) {
		super(redisManager, workflowInnerService, conversationChatMemory, commonConfig);
		this.appComponentManager = appComponentManager;
	}

	@Override
	public String getNodeType() {
		return NodeTypeEnum.COMPONENT.getCode();
	}

	@Override
	public String getNodeDescription() {
		return NodeTypeEnum.COMPONENT.getDesc();
	}

	/**
	 * Executes the application component node in the workflow
	 * @param graph The workflow graph
	 * @param node The component node to execute
	 * @param context The workflow context
	 * @return NodeResult containing execution status and component output
	 */
	@Override
	public NodeResult innerExecute(DirectedAcyclicGraph<String, Edge> graph, Node node, WorkflowContext context) {

		NodeParam nodeParam = JsonUtils.fromMap(node.getConfig().getNodeParam(), NodeParam.class);
		AppComponentRequest request = new AppComponentRequest();
		List<Node.InputParam> inputParams = node.getConfig().getInputParams();
		HashMap<String, Object> bizVars = convertToComponentInput(inputParams, context);
		request.setBizVars(bizVars);
		request.setCode(nodeParam.getCode());
		request.setStreamMode(nodeParam.getStreamSwitch());
		request.setType(nodeParam.getType());
		ShortTermMemory shortMemory = nodeParam.getShortMemory();
		List<Message> shortTermMemories = constructShortTermMemory(node, shortMemory, context);
		request.setMessages(convertToChatMessage(shortTermMemories));
		try {

			NodeResult nodeResult = new NodeResult();
			nodeResult.setNodeId(node.getId());
			nodeResult.setNodeName(node.getName());
			nodeResult.setNodeType(node.getType());
			nodeResult.setNodeStatus(NodeStatusEnum.EXECUTING.getCode());
			nodeResult.setInput(JsonUtils.toJson(convertToComponentInput(inputParams, context)));
			Map<String, Object> outputMap = Maps.newHashMap();
			StringBuilder builder = new StringBuilder();

			if (nodeParam.getType().equals(AppComponentTypeEnum.Agent.getValue())) {
				// Handle agent component execution
				Flux<AgentResponse> stream = appComponentManager.executeAgentComponentStream(request);
				if (stream == null) {
					return NodeResult.error(node, APP_COMPONENT_NOT_FOUND_ERROR.getMessage());
				}
				stream.doOnNext((modelResponse) -> handleResponse(modelResponse, node, context, builder)).blockLast();
				outputMap.put(OUTPUT_DECORATE_PARAM_KEY, builder.toString());

			}
			else if (nodeParam.getType().equals(AppComponentTypeEnum.Workflow.getValue())) {
				// Handle workflow component execution
				Flux<WorkflowResponse> stream = appComponentManager.executeWorkflowComponentStream(request);
				if (stream == null) {
					return NodeResult.error(node, APP_COMPONENT_NOT_FOUND_ERROR.getMessage());
				}
				StringBuilder result = new StringBuilder();
				stream.doOnNext((modelResponse) -> handleResponse(modelResponse, node, context, builder, result))
					.blockLast();
				if (nodeParam.getOutputType().equals("json")) {
					outputMap.put("outputStream", builder.toString());
					if (!result.isEmpty()) {
						Map<String, Object> objectObjectMap = JsonUtils.fromJsonToMap(result.toString());
						outputMap.putAll(objectObjectMap);
					}
				}
				else {
					outputMap.put(OUTPUT_DECORATE_PARAM_KEY, builder.toString());
				}
			}

			nodeResult.setOutput(JsonUtils.toJson(outputMap));
			context.getNodeResultMap().put(node.getId(), nodeResult);

		}
		catch (Exception e) {
			log.error("Component node execution failed, requestId:{}", context.getRequestId(), e);
			return NodeResult.error(node, e.getMessage());
		}

		return context.getNodeResultMap().get(node.getId());
	}

	/**
	 * Converts input parameters into a map for component execution
	 * @param inputParams List of input parameters
	 * @param context The workflow context
	 * @return Map of component input parameters
	 */
	public HashMap<String, Object> convertToComponentInput(List<Node.InputParam> inputParams, WorkflowContext context) {
		HashMap<String, Object> inputJson = new HashMap<>();
		inputParams.forEach(inputParam -> {
			if (inputParam.getValueFrom() != null && inputParam.getKey() != null && inputParam.getValue() != null) {
				if (ValueFromEnum.input.name().equals(inputParam.getValueFrom())) {
					inputJson.put(inputParam.getKey(), inputParam.getValue());
				}
				else {
					inputJson.put(inputParam.getKey(),
							VariableUtils.getValueFromPayload(
									VariableUtils.getExpressionFromBracket((String) inputParam.getValue()),
									context.getVariablesMap()));
				}
			}
		});
		return inputJson;
	}

	/**
	 * Handles agent component response
	 * @param agentResponse The agent component response
	 * @param node The component node
	 * @param context The workflow context
	 * @param builder StringBuilder for accumulating response content
	 */
	public void handleResponse(AgentResponse agentResponse, Node node, WorkflowContext context, StringBuilder builder) {
		NodeParam nodeParam = JsonUtils.fromMap(node.getConfig().getNodeParam(), NodeParam.class);

		// Process agent component response
		if (!agentResponse.isSuccess()) {
			String content = fetchContent(agentResponse);
			// Save execution result and record error
			context.getNodeResultMap().put(node.getId(), NodeResult.error(node, "Node execution failed: " + content));
		}
		else {
			String responseText;
			Map<String, Object> outputMap = Maps.newHashMap();
			responseText = fetchContent(agentResponse);
			if (responseText != null) {
				builder.append(responseText);
			}
			if (nodeParam.getStreamSwitch()) {
				NodeResult nodeResult = new NodeResult();
				nodeResult.setNodeId(node.getId());
				nodeResult.setNodeName(node.getName());
				nodeResult.setNodeType(node.getType());
				nodeResult.setNodeStatus(NodeStatusEnum.EXECUTING.getCode());
				List<Node.InputParam> inputParams = node.getConfig().getInputParams();
				nodeResult.setInput(JsonUtils.toJson(convertToComponentInput(inputParams, context)));
				outputMap.put(OUTPUT_DECORATE_PARAM_KEY, builder.toString());
				nodeResult.setOutput(JsonUtils.toJson(outputMap));
				context.getNodeResultMap().put(node.getId(), nodeResult);
			}
		}
		// workflowInnerService.refreshContextCache(context);

	}

	/**
	 * Handles workflow component response
	 * @param workflowResponse The workflow component response
	 * @param node The component node
	 * @param context The workflow context
	 * @param builder StringBuilder for accumulating response content
	 * @param result StringBuilder for accumulating final result
	 */
	public void handleResponse(WorkflowResponse workflowResponse, Node node, WorkflowContext context,
			StringBuilder builder, StringBuilder result) {
		NodeParam nodeParam = JsonUtils.fromMap(node.getConfig().getNodeParam(), NodeParam.class);

		if (!workflowResponse.isSuccess()) {
			String errorMsg = workflowResponse.getError().toString();
			// Save execution result and record error
			context.getNodeResultMap().put(node.getId(), NodeResult.error(node, "Execution failed: " + errorMsg));
		}
		else {
			HashMap<String, Object> outputMap = new HashMap<>();
			ChatMessage message = workflowResponse.getMessage();
			if (message == null) {
				return;
			}
			if (NodeTypeEnum.END.getCode().equals(workflowResponse.getNodeType())) {
				builder.append(message.getContent());
				result.append(message.getContent());
			}
			else {
				builder.append(message.getContent());
			}

			if (nodeParam.getStreamSwitch()) {
				String outputType = nodeParam.getOutputType();
				if ("json".equals(outputType)) {
					outputMap.put("outputStream", builder.toString());
					if (StringUtils.isNotBlank(result.toString())) {
						Map<String, Object> objectObjectMap = JsonUtils.fromJsonToMap(result.toString());
						outputMap.putAll(objectObjectMap);
					}

				}
				else {
					outputMap.put(OUTPUT_DECORATE_PARAM_KEY, builder.toString());
				}
				NodeResult nodeResult = new NodeResult();
				nodeResult.setNodeId(node.getId());
				nodeResult.setNodeName(node.getName());
				nodeResult.setNodeType(node.getType());
				nodeResult.setNodeStatus(NodeStatusEnum.EXECUTING.getCode());
				List<Node.InputParam> inputParams = node.getConfig().getInputParams();
				nodeResult.setInput(JsonUtils.toJson(convertToComponentInput(inputParams, context)));
				nodeResult.setOutput(JsonUtils.toJson(outputMap));
				context.getNodeResultMap().put(node.getId(), nodeResult);
			}
		}
		// workflowInnerService.refreshContextCache(context);

	}

	/**
	 * Updates short-term memory based on current round of interaction.
	 */
	public void handleCurrentRound4SelfShortTermMemory(Node node, Integer round, WorkflowContext context,
			NodeResult nodeResult) {
		List<ChatMessage> chatMessages = Lists.newArrayList();
		List<Node.InputParam> inputParams = node.getConfig().getInputParams();
		HashMap<String, Object> bizVars = convertToComponentInput(inputParams, context);
		if (bizVars.containsKey("query")) {
			chatMessages.add(new ChatMessage(MessageRole.USER, bizVars.get("query")));
		}
		Map<String, Object> outputMap = JsonUtils.fromJsonToMap(nodeResult.getOutput());
		chatMessages.add(new ChatMessage(MessageRole.ASSISTANT, outputMap.get(OUTPUT_DECORATE_PARAM_KEY)));
		NodeResult.ShortMemory shortMemory = new NodeResult.ShortMemory();
		shortMemory.setRound(round);
		shortMemory.setCurrentSelfChatMessages(chatMessages);
		nodeResult.setShortMemory(shortMemory);
	}

	/**
	 * Extracts content from agent response.
	 */
	public String fetchContent(AgentResponse agentResponse) {
		return agentResponse.getMessage().getContent().toString();
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
	 * Configuration parameters for application component node
	 */
	@Data
	public static class NodeParam {

		@JsonProperty("code")
		private String code;

		@JsonProperty("stream_switch")
		private Boolean streamSwitch = false;

		@JsonProperty("output_type")
		private String outputType;

		/**
		 * @see AppComponentTypeEnum
		 */
		private String type;

		@JsonProperty("short_memory")
		private ShortTermMemory shortMemory;

	}

}
