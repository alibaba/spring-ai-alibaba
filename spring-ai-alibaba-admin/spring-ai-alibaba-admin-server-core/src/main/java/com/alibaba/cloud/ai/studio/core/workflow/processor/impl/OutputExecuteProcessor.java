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
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.ParamSourceEnum;
import com.alibaba.cloud.ai.studio.runtime.utils.JsonUtils;
import com.alibaba.cloud.ai.studio.core.config.CommonConfig;
import com.alibaba.cloud.ai.studio.core.base.manager.RedisManager;
import com.alibaba.cloud.ai.studio.core.workflow.WorkflowContext;
import com.alibaba.cloud.ai.studio.core.utils.common.VariableUtils;
import com.alibaba.cloud.ai.studio.core.workflow.WorkflowInnerService;
import com.alibaba.cloud.ai.studio.core.workflow.processor.AbstractExecuteProcessor;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Sets;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Output Node Processor
 * <p>
 * This processor is responsible for handling the output node in the workflow. It
 * processes text templates with variable substitution and supports streaming output.
 * <p>
 * Features: 1. Variable substitution in output templates 2. Support for streaming output
 * 3. Integration with system and conversation variables 4. Dynamic content generation
 * based on node dependencies
 *
 * @version 1.0.0
 */
@Slf4j
@Component("OutputExecuteProcessor")
public class OutputExecuteProcessor extends AbstractExecuteProcessor {

	public OutputExecuteProcessor(RedisManager redisManager, WorkflowInnerService workflowInnerService,
			ChatMemory conversationChatMemory, CommonConfig commonConfig) {
		super(redisManager, workflowInnerService, conversationChatMemory, commonConfig);
	}

	@Override
	public NodeResult innerExecute(DirectedAcyclicGraph<String, Edge> graph, Node node, WorkflowContext context) {
		NodeResult nodeResult = initNodeResultAndRefreshContext(node, context);
		NodeParam config = JsonUtils.fromMap(node.getConfig().getNodeParam(), NodeParam.class);

		String textTemplate = config.getOutput();
		boolean streamSwitch = config.getStreamSwitch() != null && config.getStreamSwitch();
		handleTextTemplate(graph, textTemplate, streamSwitch, nodeResult, context);
		return nodeResult;
	}

	/**
	 * Process the text template by replacing variables with their actual values
	 * @param graph The workflow graph
	 * @param outputTemplate The template containing variables to be replaced
	 * @param streamSwitch Whether to enable streaming output
	 * @param nodeResult The node result to store the processed output
	 * @param context The workflow context
	 */
	public void handleTextTemplate(DirectedAcyclicGraph<String, Edge> graph, String outputTemplate,
			boolean streamSwitch, NodeResult nodeResult, WorkflowContext context) {
		List<String> keys = VariableUtils.identifyVariableListFromText(outputTemplate);
		// Extract variable list from output content
		StringBuilder contentStringBuilder = new StringBuilder();
		if (!keys.isEmpty()) {
			int lastIndex = 0;
			String lastKey = null;

			for (String key : keys) {
				// Key source
				String keyFrom = key.contains(".") ? key.substring(0, key.indexOf(".")) : key;
				// Get variable position in text
				int keyStartIndex = outputTemplate.indexOf("${" + key + "}", lastIndex);

				if (keyStartIndex >= 0) {
					if (lastKey == null) {
						// If it's the first variable, add text before the variable
						contentStringBuilder.append(outputTemplate.substring(0, keyStartIndex));
						if (streamSwitch) {
							nodeResult.setOutput(contentStringBuilder.toString());
							workflowInnerService.refreshContextCache(context);
						}
					}
					else if (lastIndex > 0) {
						// If it's not the first variable, add text between variables
						contentStringBuilder.append(outputTemplate.substring(lastIndex, keyStartIndex));
						if (streamSwitch) {
							nodeResult.setOutput(contentStringBuilder.toString());
							workflowInnerService.refreshContextCache(context);
						}
					}

					if (Sets.newHashSet(ParamSourceEnum.sys.name(), ParamSourceEnum.conversation.name())
						.contains(keyFrom)) {
						// No need to wait
						contentStringBuilder
							.append(VariableUtils.getValueStringFromPayload(key, context.getVariablesMap()));
						if (streamSwitch) {
							nodeResult.setOutput(contentStringBuilder.toString());
							workflowInnerService.refreshContextCache(context);
						}
					}
					else {
						boolean nodeContain = graph.containsVertex(keyFrom);
						if (nodeContain) {
							String tmpContent = contentStringBuilder.toString();
							String subPath = key.contains(".") ? key.substring(key.indexOf(".") + 1) : key;
							do {
								if (streamSwitch) {
									NodeResult keyFromNodeResult = context.getNodeResultMap().get(keyFrom);
									String keyFromOutput = keyFromNodeResult.getOutput();
									if (StringUtils.isNotBlank(keyFromOutput)) {
										String value;
										if (keyFromNodeResult.getNodeType().equals(NodeTypeEnum.START.getCode())) {
											// Start node value can be obtained at once,
											// other nodes get through nodeResult output
											value = VariableUtils.getValueStringFromPayload(key,
													context.getVariablesMap());
										}
										else {
											Map<String, Object> keyFromMap = JsonUtils.fromJsonToMap(keyFromOutput);
											value = VariableUtils.getValueStringFromPayload(subPath, keyFromMap);
										}
										if (value != null) {
											nodeResult.setOutput(tmpContent + value);
											workflowInnerService.refreshContextCache(context);
										}
									}
								}
							}
							while (!isFinished(graph, context, keyFrom));
							// Re-insert after completion
							NodeResult keyFromNodeResult = context.getNodeResultMap().get(keyFrom);
							if (StringUtils.isNotBlank(keyFromNodeResult.getOutput())) {
								// Start node uses complete mode
								if (keyFromNodeResult.getNodeType().equals(NodeTypeEnum.START.getCode())) {
									contentStringBuilder.append(
											VariableUtils.getValueStringFromPayload(key, context.getVariablesMap()));
								}
								else {
									Map<String, Object> keyFromMap = JsonUtils
										.fromJsonToMap(context.getNodeResultMap().get(keyFrom).getOutput());
									// Extract subpath from complete expression, e.g.,
									// "xxx.yyy" from "LLM_sss.xxx.yyy"
									contentStringBuilder
										.append(VariableUtils.getValueStringFromPayload(subPath, keyFromMap));
								}
							}
							if (streamSwitch) {
								nodeResult.setOutput(contentStringBuilder.toString());
								workflowInnerService.refreshContextCache(context);
							}
						}
					}
					lastIndex = keyStartIndex + key.length() + 3; // +3 for "${" and "}"
					// length
					lastKey = key;
				}
			}

			// Add text after the last variable
			if (lastIndex < outputTemplate.length()) {
				contentStringBuilder.append(outputTemplate.substring(lastIndex));
				if (streamSwitch) {
					nodeResult.setOutput(contentStringBuilder.toString());
					workflowInnerService.refreshContextCache(context);
				}
			}
			// Set processed content to result
			nodeResult.setOutput(contentStringBuilder.toString());
			workflowInnerService.refreshContextCache(context);
		}
		else {
			// If no variables, use original text directly
			nodeResult.setOutput(outputTemplate);
			workflowInnerService.refreshContextCache(context);
		}
		nodeResult.setOutputType("text");
	}

	/**
	 * Check if a node's execution is finished
	 * @param graph The workflow graph
	 * @param context The workflow context
	 * @param nodeId The ID of the node to check
	 * @return true if the node execution is finished, false otherwise
	 */
	private boolean isFinished(DirectedAcyclicGraph<String, Edge> graph, WorkflowContext context, String nodeId) {
		boolean nodeContain = graph.containsVertex(nodeId);
		if (!nodeContain) {
			return true;
		}
		NodeResult nodeResult = context.getNodeResultMap().get(nodeId);
		if (nodeResult == null) {
			return false;
		}
		if (context.getTaskStatus().equals(NodeStatusEnum.FAIL.getCode())
				|| nodeResult.getNodeStatus().equals(NodeStatusEnum.FAIL.getCode())) {
			throw new BizException(ErrorCode.WORKFLOW_EXECUTE_ERROR.toError(context.getError().getMessage()));
		}
		if (nodeResult.getNodeStatus().equals(NodeStatusEnum.SUCCESS.getCode())
				|| nodeResult.getNodeStatus().equals(NodeStatusEnum.SKIP.getCode())) {
			return true;
		}
		return false;
	}

	@Override
	public CheckNodeParamResult checkNodeParam(DirectedAcyclicGraph<String, Edge> graph, Node node) {
		CheckNodeParamResult result = super.checkNodeParam(graph, node);
		NodeParam nodeParam = JsonUtils.fromMap(node.getConfig().getNodeParam(), NodeParam.class);
		if (nodeParam == null) {
			result.setSuccess(false);
			result.getErrorInfos().add("[nodeParam] is null");
			return result;
		}
		if (StringUtils.isBlank(nodeParam.getOutput())) {
			result.setSuccess(false);
			result.getErrorInfos().add("[output] is empty");
		}
		return result;
	}

	@Override
	public String getNodeType() {
		return NodeTypeEnum.OUTPUT.getCode();
	}

	@Override
	public String getNodeDescription() {
		return NodeTypeEnum.OUTPUT.getDesc();
	}

	@Data
	public static class NodeParam {

		// Output content
		private String output;

		// Streaming switch
		@JsonProperty("stream_switch")
		private Boolean streamSwitch;

	}

}
