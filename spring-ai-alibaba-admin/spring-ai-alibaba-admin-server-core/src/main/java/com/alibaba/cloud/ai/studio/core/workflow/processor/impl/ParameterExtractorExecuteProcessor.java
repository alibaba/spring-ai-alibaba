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

import com.alibaba.cloud.ai.studio.runtime.domain.agent.AgentResponse;
import com.alibaba.cloud.ai.studio.runtime.domain.chat.ChatMessage;
import com.alibaba.cloud.ai.studio.runtime.domain.chat.MessageRole;
import com.alibaba.cloud.ai.studio.runtime.domain.chat.Usage;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.Edge;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.Node;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.NodeResult;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.NodeStatusEnum;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.NodeTypeEnum;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.inner.ModelConfig;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.inner.ShortTermMemory;
import com.alibaba.cloud.ai.studio.runtime.utils.JsonUtils;
import com.alibaba.cloud.ai.studio.core.config.CommonConfig;
import com.alibaba.cloud.ai.studio.core.base.manager.ModelExecuteManager;
import com.alibaba.cloud.ai.studio.core.base.manager.RedisManager;
import com.alibaba.cloud.ai.studio.core.workflow.WorkflowContext;
import com.alibaba.cloud.ai.studio.core.utils.common.VariableUtils;
import com.alibaba.cloud.ai.studio.core.workflow.WorkflowInnerService;
import com.alibaba.cloud.ai.studio.core.workflow.processor.AbstractExecuteProcessor;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.Data;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * Parameter Extractor Node Processor
 * <p>
 * This processor is responsible for extracting structured parameters from unstructured
 * text input using LLM models. It provides a flexible and accurate way to parse user
 * input into predefined parameter formats.
 * <p>
 * Key Features: 1. Extracts parameters from natural language input using LLM 2. Supports
 * custom parameter definitions with types and descriptions 3. Handles parameter
 * validation and completion status 4. Provides example-based learning for better
 * extraction 5. Supports short-term memory for context-aware extraction 6. Configurable
 * model parameters and instructions 7. JSON-formatted output with completion status 8.
 * Error handling and parameter validation
 *
 * @version 1.0.0-M1
 */
@Component("ParameterExtractorExecuteProcessor")
public class ParameterExtractorExecuteProcessor extends AbstractExecuteProcessor {

	private static final String SYSTEM_PROMPT = "You are a professional parameter extraction assistant. Your task is to accurately extract specified parameters from user input. Please carefully analyze the input content to ensure accurate parameter extraction. If certain parameters cannot be extracted, set _is_completed to false and explain the reason in _reason.";

	private static final String INSTRUCTION_PROMPT = "Please note the following content, which is crucial for parameter extraction: %s";

	private static final String PARAMETER_LIST_TEMPLATE = "Parameters to extract are in the format - {parameter_name}:{parameter_type}:{parameter_description}:{is_required}. The list of parameters to extract is:\n%s\n";

	private static final String USER_INPUT_TEMPLATE = "User input:\n%s\n\n";

	private static final String JSON_TEMPLATE = "Please return the extracted parameters in JSON format as follows:\n%s\n";

	private static final String EXAMPLES_TEMPLATE = "\nExamples:\n"
			+ "1. Input: \"I want to travel to Beijing on March 15, 2024\"\n" + "   Parameters: {\n"
			+ "     \"_is_completed\": true,\n" + "     \"_reason\": \"\",\n" + "     \"city\": \"Beijing\",\n"
			+ "     \"date\": \"2024-03-15\"\n" + "   }\n\n" + "2. Input: \"Help me book a flight to Shanghai\"\n"
			+ "   Parameters: {\n" + "     \"_is_completed\": false,\n"
			+ "     \"_reason\": \"Missing date information\",\n" + "     \"city\": \"Shanghai\"\n" + "   }\n\n"
			+ "3. Input: \"Going to Hangzhou next Monday\"\n" + "   Parameters: {\n" + "     \"_is_completed\": true,\n"
			+ "     \"_reason\": \"\",\n" + "     \"city\": \"Hangzhou\",\n" + "     \"date\": \"2024-03-18\"\n"
			+ "   }\n";

	private final ModelExecuteManager modelExecuteManager;

	public ParameterExtractorExecuteProcessor(RedisManager redisManager, WorkflowInnerService workflowInnerService,
			ChatMemory conversationChatMemory, CommonConfig commonConfig, ModelExecuteManager modelExecuteManager) {
		super(redisManager, workflowInnerService, conversationChatMemory, commonConfig);
		this.modelExecuteManager = modelExecuteManager;
	}

	/**
	 * Executes the parameter extraction node in the workflow
	 * @param graph The workflow graph
	 * @param node The parameter extraction node to execute
	 * @param context The workflow context
	 * @return NodeResult containing extraction status and parameters
	 */
	@Override
	public NodeResult innerExecute(DirectedAcyclicGraph<String, Edge> graph, Node node, WorkflowContext context) {
		// Initialize node result with executing status and refresh context
		NodeResult nodeResult = initNodeResultAndRefreshContext(node, context);

		// Parse configuration
		NodeParam config = JsonUtils.fromMap(node.getConfig().getNodeParam(), NodeParam.class);
		if (config == null || config.getModelConfig() == null || config.getExtractParams() == null) {
			nodeResult.setNodeStatus(NodeStatusEnum.FAIL.getCode());
			nodeResult.setErrorInfo("Configuration error: Missing required configuration information");
			return nodeResult;
		}

		// Get input
		Map<String, Object> inputMap = constructInputParamsMap(node, context);
		String query = (String) inputMap.get(INPUT_DECORATE_PARAM_KEY);
		if (StringUtils.isBlank(query)) {
			nodeResult.setNodeStatus(NodeStatusEnum.FAIL.getCode());
			nodeResult.setErrorInfo("Input error: Missing query content");
			return nodeResult;
		}

		// Build prompt
		StringBuilder promptBuilder = new StringBuilder();
		if (StringUtils.isNotBlank(config.getInstruction())) {
			promptBuilder.append(replaceTemplateContent(config.getInstruction(), context)).append("\n\n");
		}
		promptBuilder.append(String.format(PARAMETER_LIST_TEMPLATE, buildParameterList(config.getExtractParams())));
		promptBuilder.append(EXAMPLES_TEMPLATE);
		promptBuilder.append(String.format(USER_INPUT_TEMPLATE, query));
		promptBuilder.append(String.format(JSON_TEMPLATE, buildJsonTemplate(config.getExtractParams())));

		// Call LLM
		List<Message> messages = Lists.newArrayList();

		String instruction = replaceTemplateContent(config.getInstruction(), context);
		if (StringUtils.isNotBlank(instruction)) {
			messages.add(new SystemMessage(SYSTEM_PROMPT + "\n" + String.format(INSTRUCTION_PROMPT, instruction)));
		}
		else {
			messages.add(new SystemMessage(SYSTEM_PROMPT));
		}

		// Add short-term memory
		List<Message> shortTermMemories = constructShortTermMemory(node, config.getShortMemory(), context);
		if (CollectionUtils.isNotEmpty(shortTermMemories)) {
			messages.addAll(shortTermMemories);
		}

		messages.add(new UserMessage(promptBuilder.toString()));

		// Build model parameters
		Map<String, Object> paramMap = Maps.newHashMap();
		List<ModelConfig.ModelParam> params = config.getModelConfig().getParams();
		if (CollectionUtils.isNotEmpty(params)) {
			params.stream().forEach(modelParam -> {
				if (BooleanUtils.isTrue(modelParam.getEnable())) {
					paramMap.put(modelParam.getKey(), modelParam.getValue());
				}
			});
		}

		// Prepare input parameters
		Map<String, Object> inputObj = Maps.newHashMap();
		inputObj.put("messages", messages);
		nodeResult.setInput(JsonUtils.toJson(inputObj));
		workflowInnerService.refreshContextCache(context);

		// Call model
		Flux<AgentResponse> stream = modelExecuteManager.stream(config.getModelConfig().getProvider(),
				config.getModelConfig().getModelId(), paramMap, messages);

		// Process response
		StringBuilder responseBuilder = new StringBuilder();
		Usage usage = new Usage();
		stream.doOnNext(response -> {
			if (response.getMessage() != null && response.getMessage().getContent() != null) {
				responseBuilder.append(response.getMessage().getContent());
			}
			if (response.getUsage() != null) {
				usage.setPromptTokens(response.getUsage().getPromptTokens());
				usage.setCompletionTokens(response.getUsage().getCompletionTokens());
				usage.setTotalTokens(response.getUsage().getTotalTokens());
			}
		}).doOnError(error -> {
			nodeResult.setNodeStatus(NodeStatusEnum.FAIL.getCode());
			nodeResult.setErrorInfo("Model call failed: " + error.getMessage());
		}).doOnComplete(() -> {
			try {
				// Parse JSON response
				Map<String, Object> extractedParams = JsonUtils.fromJsonToMap(responseBuilder.toString());
				nodeResult.setOutput(JsonUtils.toJson(extractedParams));
				nodeResult.setNodeStatus(NodeStatusEnum.SUCCESS.getCode());
			}
			catch (Exception e) {
				nodeResult.setNodeStatus(NodeStatusEnum.FAIL.getCode());
				nodeResult.setErrorInfo("Parameter parsing failed: " + e.getMessage());
			}
		}).blockLast();
		if (usage != null) {
			nodeResult.setUsages(Lists.newArrayList(usage));
		}
		return nodeResult;
	}

	/**
	 * Builds the parameter list prompt
	 * @param params List of parameters to extract
	 * @return Formatted parameter list prompt
	 */
	private String buildParameterList(List<Node.InputParam> params) {
		StringBuilder builder = new StringBuilder();
		for (Node.InputParam param : params) {
			builder.append("- ")
				.append(param.getKey())
				.append(": ")
				.append(param.getType())
				.append(": ")
				.append(param.getDesc())
				.append(": ")
				.append(param.getRequired() == null ? true : param.getRequired())
				.append("\n");
		}
		return builder.toString();
	}

	/**
	 * Builds the JSON template prompt
	 * @param params List of parameters to extract
	 * @return Formatted JSON template prompt
	 */
	private String buildJsonTemplate(List<Node.InputParam> params) {
		StringBuilder builder = new StringBuilder();
		builder.append("{\n");
		builder.append("  \"_is_completed\": true,\n");
		builder.append("  \"_reason\": \"\",\n");
		for (Node.InputParam param : params) {
			builder.append("  \"").append(param.getKey()).append("\": \"\",\n");
		}
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Configuration parameters for parameter extraction node
	 */
	@Data
	public static class NodeParam {

		@JsonProperty("model_config")
		private ModelConfig modelConfig;

		@JsonProperty("instruction")
		private String instruction;

		@JsonProperty("extract_params")
		private List<Node.InputParam> extractParams;

		@JsonProperty("short_memory")
		private ShortTermMemory shortMemory;

	}

	@Override
	public String getNodeType() {
		return NodeTypeEnum.PARAMETER_EXTRACTOR.getCode();
	}

	@Override
	public String getNodeDescription() {
		return NodeTypeEnum.PARAMETER_EXTRACTOR.getDesc();
	}

	@Override
	public void handleCurrentRound4SelfShortTermMemory(Node node, Integer round, WorkflowContext context,
			NodeResult nodeResult) {
		List<ChatMessage> chatMessages = Lists.newArrayList();
		chatMessages.add(new ChatMessage(MessageRole.USER,
				VariableUtils.getValueFromContext(node.getConfig().getInputParams().get(0), context)));
		Map<Object, Object> outputMap = JsonUtils.fromJsonToMap(nodeResult.getOutput());
		chatMessages.add(new ChatMessage(MessageRole.ASSISTANT, JsonUtils.toJson(outputMap)));
		NodeResult.ShortMemory shortMemory = new NodeResult.ShortMemory();
		shortMemory.setRound(round);
		shortMemory.setCurrentSelfChatMessages(chatMessages);
		nodeResult.setShortMemory(shortMemory);
	}

	@Override
	public CheckNodeParamResult checkNodeParam(DirectedAcyclicGraph<String, Edge> graph, Node node) {
		CheckNodeParamResult result = super.checkNodeParam(graph, node);
		NodeParam nodeParam = JsonUtils.fromMap(node.getConfig().getNodeParam(), NodeParam.class);
		ModelConfig modelConfig = nodeParam.getModelConfig();
		if (modelConfig == null || StringUtils.isBlank(modelConfig.getModelId())) {
			result.setSuccess(false);
			result.getErrorInfos().add("[modelConfig] is missing");
		}
		if (StringUtils.isBlank(nodeParam.getInstruction())) {
			result.setSuccess(false);
			result.getErrorInfos().add("[instruction] is empty");
		}
		return result;
	}

}
