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
import com.alibaba.cloud.ai.studio.runtime.domain.Error;
import com.alibaba.cloud.ai.studio.runtime.domain.file.File;
import com.alibaba.cloud.ai.studio.runtime.domain.agent.AgentResponse;
import com.alibaba.cloud.ai.studio.runtime.domain.chat.ChatMessage;
import com.alibaba.cloud.ai.studio.runtime.domain.chat.MessageRole;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.Edge;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.Node;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.NodeResult;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.NodeStatusEnum;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.NodeTypeEnum;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.inner.ModelConfig;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.inner.RetryConfig;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.inner.ShortTermMemory;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.inner.TryCatchConfig;
import com.alibaba.cloud.ai.studio.runtime.utils.JsonUtils;
import com.alibaba.cloud.ai.studio.core.config.StudioProperties;
import com.alibaba.cloud.ai.studio.core.config.CommonConfig;
import com.alibaba.cloud.ai.studio.core.base.manager.FileManager;
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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.core.io.FileUrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;
import reactor.core.publisher.Flux;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * LLM Node Execution Processor
 * <p>
 * This class is responsible for handling the execution of LLM (Large Language Model)
 * nodes in the workflow. It provides functionality for: 1. Processing LLM node execution
 * with streaming support 2. Managing model configurations and parameters 3. Handling
 * system and user prompts 4. Processing vision capabilities for multimodal inputs 5.
 * Managing short-term memory for conversations 6. Error handling and retry mechanisms 7.
 * Support for model parameter customization 8. Integration with file management for media
 * handling 9. Context-aware prompt processing 10. Response streaming and content
 * aggregation
 *
 * @version 1.0.0-M1
 */
@Component("LLMExecuteProcessor")
@Slf4j
public class LLMExecuteProcessor extends AbstractExecuteProcessor {

	private final StudioProperties studioProperties;

	private final WorkflowInnerService workflowInnerService;

	private final ModelExecuteManager modelExecuteManager;

	private final FileManager fileManager;

	public LLMExecuteProcessor(RedisManager redisManager, WorkflowInnerService workflowInnerService,
			ChatMemory conversationChatMemory, CommonConfig commonConfig, StudioProperties studioProperties,
			ModelExecuteManager modelExecuteManager, FileManager fileManager) {
		super(redisManager, workflowInnerService, conversationChatMemory, commonConfig);
		this.studioProperties = studioProperties;
		this.workflowInnerService = workflowInnerService;
		this.modelExecuteManager = modelExecuteManager;
		this.fileManager = fileManager;
	}

	@Override
	public String getNodeType() {
		return NodeTypeEnum.LLM.getCode();
	}

	@Override
	public String getNodeDescription() {
		return NodeTypeEnum.LLM.getDesc();
	}

	@Override
	public NodeResult innerExecute(DirectedAcyclicGraph<String, Edge> graph, Node node, WorkflowContext context) {
		NodeResult nodeResult = initNodeResultAndRefreshContext(node, context);

		NodeParam nodeParam = JsonUtils.fromMap(node.getConfig().getNodeParam(), NodeParam.class);
		// Get model related content
		ModelConfig modelConfig = nodeParam.getModelConfig();
		//
		List<Message> requestMessages = Lists.newArrayList();
		String sysPrompt = nodeParam.getSysPromptContent();
		if (StringUtils.isNotBlank(sysPrompt)) {
			requestMessages.add(new SystemMessage(replaceTemplateContent(sysPrompt, context)));
		}

		ShortTermMemory shortMemory = nodeParam.getShortMemory();
		List<Message> shortTermMemories = constructShortTermMemory(node, shortMemory, context);
		if (CollectionUtils.isNotEmpty(shortTermMemories)) {
			requestMessages.addAll(shortTermMemories);
		}

		String userPrompt = nodeParam.getPromptContent();
		if (StringUtils.isNotBlank(userPrompt)) {
			requestMessages.add(constructUserMessage(node, modelConfig, userPrompt, context));
		}

		// Build model parameters
		Map<String, Object> paramMap = Maps.newHashMap();
		List<ModelConfig.ModelParam> params = modelConfig.getParams();
		if (CollectionUtils.isNotEmpty(params)) {
			params.forEach(modelParam -> {
				if (BooleanUtils.isTrue(modelParam.getEnable())) {
					paramMap.put(modelParam.getKey(), modelParam.getValue());
				}
			});
		}
		log.info(
				context.getRequestId() + " LLMExecuteProcessor requestMessages : " + JsonUtils.toJson(requestMessages));
		ModelTmpResponseContent tmpResponseContent = new ModelTmpResponseContent();
		Flux<AgentResponse> stream = modelExecuteManager.stream(modelConfig.getProvider(), modelConfig.getModelId(),
				paramMap, requestMessages);
		AgentResponse agentResponse = stream
			.doOnNext((response) -> handleResponse(response, requestMessages, paramMap, modelConfig, node, context,
					tmpResponseContent))
			.blockLast();
		if (agentResponse.isSuccess()) {
			String responseText = tmpResponseContent.getTemporaryContent().toString();
			String reasoningContent = tmpResponseContent.getTemporaryReasoningContent().toString();
			if (StringUtils.isBlank(responseText)) {
				nodeResult.setNodeStatus(NodeStatusEnum.FAIL.getCode());
				nodeResult.setErrorInfo("Model service did not return any response");
				nodeResult
					.setError(ErrorCode.WORKFLOW_EXECUTE_ERROR.toError("Model service did not return any response"));
			}
			else {
				Map<String, Object> inputObj = Maps.newHashMap();
				inputObj.put("provider", modelConfig.getProvider());
				inputObj.put("modelId", modelConfig.getModelId());
				inputObj.put("messages", convertToChatMessage(requestMessages));
				inputObj.put("params", paramMap);
				nodeResult.setInput(JsonUtils.toJson(decorateInput(inputObj)));
				Map<String, Object> outputMap = Maps.newHashMap();
				outputMap.put(OUTPUT_DECORATE_PARAM_KEY, responseText);
				if (StringUtils.isNotBlank(reasoningContent)) {
					outputMap.put(REASONING_DECORATE_PARAM_KEY, reasoningContent);
				}
				nodeResult.setOutput(JsonUtils.toJson(outputMap));
				nodeResult.setUsages(Lists.newArrayList(agentResponse.getUsage()));
			}
		}
		else {
			nodeResult.setNodeStatus(NodeStatusEnum.FAIL.getCode());
			nodeResult.setErrorInfo(agentResponse.getError().getMessage());
			nodeResult.setError(agentResponse.getError());
		}
		return nodeResult;
	}

	/**
	 * Handles the streaming response from the model and updates the workflow context
	 * @param response The agent response from the model
	 * @param requestMessages The list of request messages
	 * @param paramMap The parameter map for the model
	 * @param modelConfig The model configuration
	 * @param node The workflow node
	 * @param context The workflow context
	 * @param tmpResponseContent Temporary storage for response content
	 */
	private void handleResponse(AgentResponse response, List<Message> requestMessages, Map<String, Object> paramMap,
			ModelConfig modelConfig, Node node, WorkflowContext context, ModelTmpResponseContent tmpResponseContent) {
		if (!response.isSuccess()) {
			Error error = response.getError();
			// Node execution error, save execution result and record error information
			context.getNodeResultMap()
				.put(node.getId(), NodeResult.error(node, "Workflow execution error: " + error.getMessage()));
		}
		else {
			NodeResult nodeResult = new NodeResult();
			nodeResult.setNodeId(node.getId());
			nodeResult.setNodeName(node.getName());
			nodeResult.setNodeType(node.getType());
			nodeResult.setNodeStatus(NodeStatusEnum.EXECUTING.getCode());
			Map<String, Object> inputObj = Maps.newHashMap();
			inputObj.put("provider", modelConfig.getProvider());
			inputObj.put("modelId", modelConfig.getModelId());
			inputObj.put("messages", convertToChatMessage(requestMessages));
			inputObj.put("params", paramMap);
			nodeResult.setInput(JsonUtils.toJson(decorateInput(inputObj)));
			String content = fetchContent(response);
			String reasoningContent = fetchReasoningContent(response);
			tmpResponseContent.getTemporaryContent().append(content == null ? "" : content);
			tmpResponseContent.getTemporaryReasoningContent().append(reasoningContent == null ? "" : reasoningContent);
			Map<String, Object> tmpMap = decorateOutput(tmpResponseContent.getTemporaryContent().toString());
			String tmpReasoningContent = tmpResponseContent.getTemporaryReasoningContent().toString();
			if (StringUtils.isNotBlank(tmpReasoningContent)) {
				tmpMap.put(REASONING_DECORATE_PARAM_KEY, tmpReasoningContent);
			}
			nodeResult.setOutput(JsonUtils.toJson(tmpMap));
			nodeResult.setUsages(Lists.newArrayList(response.getUsage()));
			context.getNodeResultMap().put(node.getId(), nodeResult);
		}
		// workflowInnerService.refreshContextCache(context);
	}

	/**
	 * Extracts the main content from the agent response
	 * @param gptResponse The agent response containing the message
	 * @return The extracted content as string, or null if not available
	 */
	private static String fetchContent(AgentResponse gptResponse) {
		Object content = gptResponse.getMessage().getContent();
		if (content != null) {
			return String.valueOf(content);
		}
		else {
			return "";
		}
	}

	/**
	 * Extracts the reasoning content from the agent response
	 * @param gptResponse The agent response containing the message
	 * @return The extracted reasoning content as string, or null if not available
	 */
	private static String fetchReasoningContent(AgentResponse gptResponse) {
		String reasoningContent = gptResponse.getMessage().getReasoningContent();
		if (StringUtils.isNotBlank(reasoningContent)) {
			return reasoningContent;
		}
		else {
			return "";
		}
	}

	/**
	 * Constructs a user message with support for media content
	 * @param node The workflow node
	 * @param modelConfig The model configuration
	 * @param userPrompt The user prompt content
	 * @param context The workflow context
	 * @return A constructed UserMessage with optional media content
	 */
	private UserMessage constructUserMessage(Node node, ModelConfig modelConfig, String userPrompt,
			WorkflowContext context) {
		userPrompt = replaceTemplateContent(userPrompt, context);
		// 构造视觉理解
		ModelConfig.SkillConfig visionConfig = modelConfig.getVisionConfig();
		if (visionConfig != null && BooleanUtils.isTrue(visionConfig.getEnable())) {
			List<Node.InputParam> visionParams = visionConfig.getParams();
			if (CollectionUtils.isNotEmpty(visionParams)) {
				Object value = VariableUtils.getValueFromContext(visionParams.get(0), context);
				if (value == null) {
					return new UserMessage(userPrompt);
				}
				else if (value instanceof File) {
					Media media = constructMedia(value);
					if (media == null) {
						return new UserMessage(userPrompt);
					}
					return UserMessage.builder().text(userPrompt).media(media).build();
				}
				else if (value instanceof List) {
					List<Media> mediaList = ((List<?>) value).stream()
						.map(this::constructMedia)
						.filter(Objects::nonNull)
						.collect(Collectors.toList());
					if (CollectionUtils.isEmpty(mediaList)) {
						return new UserMessage(userPrompt);
					}
					return UserMessage.builder().text(userPrompt).media(mediaList).build();
				}
				else {
					throw new BizException(ErrorCode.WORKFLOW_CONFIG_INVALID
						.toError(node.getName() + " vision param is not File or List<File>"));
				}
			}
		}
		return new UserMessage(userPrompt);
	}

	/**
	 * Constructs a media object from the provided value
	 * @param value The value to convert into media
	 * @return A Media object if conversion is successful, null otherwise
	 */
	private Media constructMedia(Object value) {
		if (value == null) {
			return null;
		}
		if (!(value instanceof File)) {
			throw new BizException(ErrorCode.WORKFLOW_CONFIG_INVALID.toError("object is not File"));
		}
		File file = (File) value;
		String url = file.getUrl();
		String mimeType = file.getMimeType();
		if (StringUtils.isNotBlank(url)) {
			String source = file.getSource() == null ? File.SourceEnum.localFile.name() : file.getSource();
			try {
				if (File.SourceEnum.localFile.name().equals(source)) {
					String storagePath = studioProperties.getStoragePath();
					return new Media(MimeType.valueOf(mimeType),
							new FileUrlResource(storagePath + java.io.File.separator + url));
				}
				else {
					MediaType mediaType = fileManager.getMediaTypeFromUrl(url);
					return Media.builder().mimeType(MimeType.valueOf(mediaType.toString())).data(new URL(url)).build();
				}
			}
			catch (Exception e) {
				log.error("Error processing local image: {}", url, e);
				throw new BizException(
						ErrorCode.WORKFLOW_EXECUTE_ERROR.toError("Failed to process local image: " + e.getMessage()));
			}
		}
		return null;
	}

	/**
	 * Checks the node parameters for validity
	 * @param graph The workflow graph
	 * @param node The node to check
	 * @return Result of the parameter check
	 */
	@Override
	public CheckNodeParamResult checkNodeParam(DirectedAcyclicGraph<String, Edge> graph, Node node) {
		CheckNodeParamResult result = super.checkNodeParam(graph, node);
		NodeParam nodeParam = JsonUtils.fromMap(node.getConfig().getNodeParam(), NodeParam.class);
		ModelConfig modelConfig = nodeParam.getModelConfig();
		if (modelConfig == null || StringUtils.isBlank(modelConfig.getModelId())) {
			result.setSuccess(false);
			result.getErrorInfos().add("[modelConfig] is missing");
		}
		if (StringUtils.isBlank(nodeParam.getPromptContent()) && StringUtils.isBlank(nodeParam.getSysPromptContent())) {
			result.setSuccess(false);
			result.getErrorInfos().add("[User Prompt] and [System Prompt] are not empty at the same time");
		}
		return result;
	}

	/**
	 * Handles the current round of short-term memory processing
	 * @param node The workflow node
	 * @param round The current round number
	 * @param context The workflow context
	 * @param nodeResult The result of the node execution
	 */
	@Override
	public void handleCurrentRound4SelfShortTermMemory(Node node, Integer round, WorkflowContext context,
			NodeResult nodeResult) {
		List<ChatMessage> chatMessages = Lists.newArrayList();
		NodeParam nodeParam = JsonUtils.fromMap(node.getConfig().getNodeParam(), NodeParam.class);
		String userPrompt = nodeParam.getPromptContent();
		if (StringUtils.isNotBlank(userPrompt)) {
			chatMessages.add(new ChatMessage(MessageRole.USER, replaceTemplateContent(userPrompt, context)));
		}
		Map<String, Object> outputMap = JsonUtils.fromJsonToMap(nodeResult.getOutput());
		chatMessages.add(new ChatMessage(MessageRole.ASSISTANT, outputMap.get(OUTPUT_DECORATE_PARAM_KEY)));
		NodeResult.ShortMemory shortMemory = new NodeResult.ShortMemory();
		shortMemory.setRound(round);
		shortMemory.setCurrentSelfChatMessages(chatMessages);
		nodeResult.setShortMemory(shortMemory);
	}

	/**
	 * Configuration parameters for the LLM node
	 */
	@Data
	public static class NodeParam {

		@JsonProperty("sys_prompt_content")
		private String sysPromptContent;

		@JsonProperty("prompt_content")
		private String promptContent;

		@JsonProperty("model_config")
		private ModelConfig modelConfig;

		@JsonProperty("retry_config")
		private RetryConfig retryConfig;

		@JsonProperty("try_catch_config")
		private TryCatchConfig tryCatchConfig;

		@JsonProperty("short_memory")
		private ShortTermMemory shortMemory;

	}

	/**
	 * Temporary storage for response content during streaming
	 */
	@Data
	private static class ModelTmpResponseContent {

		StringBuilder temporaryContent = new StringBuilder();

		StringBuilder temporaryReasoningContent = new StringBuilder();

	}

}
