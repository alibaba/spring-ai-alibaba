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
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.NodeTypeEnum;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.ValueFromEnum;
import com.alibaba.cloud.ai.studio.runtime.utils.JsonUtils;
import com.alibaba.cloud.ai.studio.core.config.CommonConfig;
import com.alibaba.cloud.ai.studio.core.base.manager.RedisManager;
import com.alibaba.cloud.ai.studio.core.workflow.WorkflowContext;
import com.alibaba.cloud.ai.studio.core.utils.common.VariableUtils;
import com.alibaba.cloud.ai.studio.core.workflow.WorkflowInnerService;
import com.alibaba.cloud.ai.studio.core.workflow.processor.AbstractExecuteProcessor;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * End Node Processor
 * <p>
 * This processor is responsible for handling the end node in the workflow. It supports
 * both text and JSON output, and integrates with the output node processor for text
 * template rendering.
 * <p>
 * Features: 1. Supports text and JSON output types 2. Integrates with
 * OutputExecuteProcessor for text template rendering 3. Handles output parameter
 * construction and validation 4. Manages node execution timing and error handling
 *
 * @version 1.0.0-M1
 */
@Slf4j
@Component("EndExecuteProcessor")
public class EndExecuteProcessor extends AbstractExecuteProcessor {

	private final OutputExecuteProcessor outputExecuteProcessor;

	/**
	 * Constructor for EndExecuteProcessor
	 * @param redisManager Redis manager for caching
	 * @param workflowInnerService Workflow inner service for context management
	 * @param conversationChatMemory Chat memory for conversation context
	 * @param commonConfig Common configuration settings
	 * @param outputExecuteProcessor Output node processor for text template rendering
	 */
	public EndExecuteProcessor(RedisManager redisManager, WorkflowInnerService workflowInnerService,
			ChatMemory conversationChatMemory, CommonConfig commonConfig,
			OutputExecuteProcessor outputExecuteProcessor) {
		super(redisManager, workflowInnerService, conversationChatMemory, commonConfig);
		this.outputExecuteProcessor = outputExecuteProcessor;
	}

	/**
	 * Output type enumeration
	 */
	@Getter
	public enum OutputType {

		TEXT("text"), JSON("json");

		private final String value;

		OutputType(String value) {
			this.value = value;
		}

		public static OutputType fromString(String value) {
			if (StringUtils.isBlank(value)) {
				return TEXT;
			}
			for (OutputType type : values()) {
				if (type.value.equalsIgnoreCase(value)) {
					return type;
				}
			}
			return TEXT;
		}

	}

	@Override
	public String getNodeType() {
		return NodeTypeEnum.END.getCode();
	}

	@Override
	public String getNodeDescription() {
		return NodeTypeEnum.END.getDesc();
	}

	/**
	 * Execute the end node processing Handles both text and JSON output types, integrates
	 * with OutputExecuteProcessor for text templates.
	 * @param graph The workflow graph
	 * @param node The current node to execute
	 * @param context The workflow context
	 * @return NodeResult containing the end node execution status and results
	 */
	@Override
	public NodeResult innerExecute(DirectedAcyclicGraph<String, Edge> graph, Node node, WorkflowContext context) {
		long start = System.currentTimeMillis();
		try {
			NodeResult endNodeResult = initNodeResultAndRefreshContext(node, context);
			NodeParam nodeParam = JsonUtils.fromMap(node.getConfig().getNodeParam(), NodeParam.class);
			String outputType = nodeParam.getOutputType();
			OutputType type = OutputType.fromString(outputType);
			endNodeResult.setOutputType(type.getValue());
			String result;
			if (OutputType.TEXT.equals(type)) {
				outputExecuteProcessor.handleTextTemplate(graph, nodeParam.getTextTemplate(),
						BooleanUtils.isTrue(nodeParam.getStreamSwitch()), endNodeResult, context);
			}
			else {
				// JSON output type
				result = JsonUtils.toJson(constructJsonResult(nodeParam.getJsonParams(), context));
				endNodeResult.setOutput(result);
			}
			endNodeResult.setNodeExecTime((System.currentTimeMillis() - start) + "ms");
			endNodeResult.setUsages(null);
			return endNodeResult;
		}
		catch (Exception e) {
			log.error("innerExecute error:{}", JsonUtils.toJson(node), e);
			// Node execution error, save result and record error info
			return NodeResult.error(node, e.getMessage());
		}
	}

	/**
	 * Construct JSON result from input parameters
	 * @param jsonParams List of input parameters
	 * @param context The workflow context
	 * @return Map containing constructed JSON result
	 */
	private Map<String, Object> constructJsonResult(List<Node.InputParam> jsonParams, WorkflowContext context) {
		Map<String, Object> result = new HashMap<>();
		jsonParams.forEach(inputParam -> {
			String valueFrom = inputParam.getValueFrom();
			Object value = inputParam.getValue();
			if (valueFrom.equals(ValueFromEnum.refer.name())) {
				if (value == null) {
					return;
				}
				String expression = VariableUtils.getExpressionFromBracket((String) value);
				if (expression == null) {
					return;
				}
				Object finalValue = VariableUtils.getValueFromContext(inputParam, context);
				if (finalValue == null) {
					return;
				}
				result.put(inputParam.getKey(), finalValue);
			}
			else {
				if (value == null) {
					return;
				}
				result.put(inputParam.getKey(), value);
			}
		});
		return result;
	}

	/**
	 * Get text template result (not used in main logic)
	 * @param nodeParam Node parameter
	 * @param context The workflow context
	 * @return String result after template replacement
	 */
	private String getTextTemplateResult(NodeParam nodeParam, WorkflowContext context) {
		// Template mode
		String textTemplate = nodeParam.getTextTemplate();
		return replaceTemplateContent(textTemplate, context);
	}

	@Data
	public static class NodeParam implements Serializable {

		/**
		 * Output type (text or json)
		 */
		@JsonProperty("output_type")
		private String outputType;

		/**
		 * Text template for output
		 */
		@JsonProperty("text_template")
		private String textTemplate;

		/**
		 * JSON parameters for output
		 */
		@JsonProperty("json_params")
		private List<Node.InputParam> jsonParams;

		/**
		 * Streaming switch
		 */
		@JsonProperty("stream_switch")
		private Boolean streamSwitch;

	}

	/**
	 * End node does not need to handle variable values
	 */
	@Override
	public void handleVariables(DirectedAcyclicGraph<String, Edge> graph, Node node, WorkflowContext context,
			NodeResult nodeResult) {
		// End node does not need to handle variable values
	}

	/**
	 * Check node parameter validity
	 * @param graph The workflow graph
	 * @param node The current node
	 * @return CheckNodeParamResult with validation status
	 */
	@Override
	public CheckNodeParamResult checkNodeParam(DirectedAcyclicGraph<String, Edge> graph, Node node) {
		CheckNodeParamResult result = super.checkNodeParam(graph, node);
		NodeParam nodeParam = JsonUtils.fromMap(node.getConfig().getNodeParam(), NodeParam.class);
		if (nodeParam == null) {
			result.setSuccess(false);
			result.getErrorInfos().add("[nodeParam] is null");
			return result;
		}
		String outputType = nodeParam.getOutputType();
		OutputType type = OutputType.fromString(outputType);
		if (OutputType.TEXT.equals(type) && StringUtils.isBlank(nodeParam.getTextTemplate())) {
			result.setSuccess(false);
			result.getErrorInfos().add("[output] is empty");
		}
		else if (OutputType.JSON.equals(type) && CollectionUtils.isEmpty(nodeParam.getJsonParams())) {
			result.setSuccess(false);
			result.getErrorInfos().add("[output] is empty");
		}
		return result;
	}

}
