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

import com.alibaba.cloud.ai.studio.runtime.domain.workflow.CommonParam;
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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Maps;
import lombok.Data;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Variable Handle Node
 *
 *
 */
@Component("VariableHandleExecuteProcessor")
public class VariableHandleExecuteProcessor extends AbstractExecuteProcessor {

	public VariableHandleExecuteProcessor(RedisManager redisManager, WorkflowInnerService workflowInnerService,
			ChatMemory conversationChatMemory, CommonConfig commonConfig) {
		super(redisManager, workflowInnerService, conversationChatMemory, commonConfig);
	}

	@Override
	public String getNodeType() {
		return NodeTypeEnum.VARIABLE_HANDLE.getCode();
	}

	@Override
	public String getNodeDescription() {
		return NodeTypeEnum.VARIABLE_HANDLE.getDesc();
	}

	/**
	 * Executes the variable handling logic for the node Processes the node based on its
	 * type (template, json, or group)
	 * @param graph The directed acyclic graph representing the workflow
	 * @param node The current node to be executed
	 * @param context The workflow context containing variables and state
	 * @return NodeResult containing the execution result
	 */
	@Override
	public NodeResult innerExecute(DirectedAcyclicGraph<String, Edge> graph, Node node, WorkflowContext context) {

		// init node result with executing status and refresh context
		NodeResult nodeResult = initNodeResultAndRefreshContext(node, context);

		NodeParam nodeParam = JsonUtils.fromMap(node.getConfig().getNodeParam(), NodeParam.class);
		Map<String, Object> localVariableMap = Maps.newHashMap();
		String result;
		if (nodeParam.getType().equals(TextConvertorType.template.name())) {
			// Template mode
			result = replaceTemplateContent(nodeParam.getTemplateContent(), context);
			nodeResult.setOutput(JsonUtils.toJson(decorateOutput(result)));
		}
		else if (nodeParam.getType().equals(TextConvertorType.json.name())) {
			// JSON mode
			nodeResult.setOutput(JsonUtils.toJson(constructJsonResult(nodeParam.getJsonParams(), context)));
		}
		else if (nodeParam.getType().equals(TextConvertorType.group.name())) {
			// Group mode
			nodeResult.setOutput(JsonUtils.toJson(constructGroupResult(nodeParam, context)));
		}
		nodeResult.setInput(JsonUtils.toJson(decorateInput(localVariableMap)));
		nodeResult.setUsages(null);
		return nodeResult;
	}

	/**
	 * Constructs a result map for group-based variable handling Groups variables and
	 * selects values based on the specified strategy
	 * @param nodeParam The node parameters containing group configurations
	 * @param context The workflow context containing variables
	 * @return A map of group names to their respective result values
	 */
	private Map<String, Object> constructGroupResult(NodeParam nodeParam, WorkflowContext context) {
		Map<String, Object> groupVariableMap = Maps.newHashMap();
		// 处理聚合内容
		handleGroupVariables(nodeParam, groupVariableMap, context);
		return groupVariableMap;
	}

	private void handleGroupVariables(NodeParam nodeParam, Map<String, Object> groupVariableMap,
			WorkflowContext context) {
		List<GroupConfig> groups = nodeParam.getGroups();
		if (CollectionUtils.isNotEmpty(groups)) {
			groups.forEach(group -> {
				List<CommonParam> variables = group.getVariables();
				if (CollectionUtils.isNotEmpty(variables)) {
					for (CommonParam variable : variables) {
						if (variable == null) {
							continue;
						}
						Object valueFromContext = VariableUtils.getValueFromContext(variable, context);
						if (valueFromContext != null) {
							groupVariableMap.put(group.getGroupName(), valueFromContext);
							if (nodeParam.getGroupStrategy() == null
									|| nodeParam.getGroupStrategy().equals(GroupStrategyEnum.firstNotNull.name())) {
								break;
							}
						}
					}
				}
			});
		}
	}

	/**
	 * Method to construct JSON result Based on input parameters and context information,
	 * constructs a JSONObject Mainly used to handle values from references or direct
	 * assignments, and integrate them into a JSON object
	 * @param jsonParams Input parameter list, containing parameter keys, values, and
	 * value sources
	 * @param context Application context information, used to resolve reference values
	 * @return Returns a JSONObject containing processed key-value pairs
	 */
	private Map<String, Object> constructJsonResult(List<Node.InputParam> jsonParams, WorkflowContext context) {
		Map<String, Object> result = Maps.newHashMap();
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
	 * Enum defining the types of text conversion operations
	 */
	public enum TextConvertorType {

		json, template, group

	}

	/**
	 * Data class for node parameters
	 */
	@Data
	public static class NodeParam {

		@JsonProperty("type")
		private String type;

		@JsonProperty("template_content")
		private String templateContent;

		@JsonProperty("json_params")
		private List<Node.InputParam> jsonParams;

		@JsonProperty("groups")
		private List<GroupConfig> groups;

		@JsonProperty("group_strategy")
		private String groupStrategy;

	}

	/**
	 * Enum defining strategies for group variable handling
	 */
	enum GroupStrategyEnum {

		// First non-null value in each group
		firstNotNull, lastNotNull

	}

	/**
	 * Configuration class for variable groups
	 */
	@Data
	public static class GroupConfig {

		@JsonProperty("group_id")
		private String groupId;

		@JsonProperty("group_name")
		private String groupName;

		@JsonProperty("output_type")
		private String outputType;

		@JsonProperty("variables")
		private List<CommonParam> variables;

	}

	/**
	 * Validates the node parameters based on the node type
	 * @param node The node to validate
	 * @return Result of the validation check
	 */
	@Override
	public CheckNodeParamResult checkNodeParam(DirectedAcyclicGraph<String, Edge> graph, Node node) {
		CheckNodeParamResult result = super.checkNodeParam(graph, node);
		NodeParam nodeParam = JsonUtils.fromMap(node.getConfig().getNodeParam(), NodeParam.class);
		if (nodeParam.getType().equals(TextConvertorType.template.name())
				&& StringUtils.isBlank(nodeParam.getTemplateContent())) {
			result.setSuccess(false);
			result.getErrorInfos().add("Text content is empty");
		}
		else if (nodeParam.getType().equals(TextConvertorType.json.name()) && nodeParam.getJsonParams().isEmpty()) {
			result.setSuccess(false);
			result.getErrorInfos().add("No variables added");
		}
		else if (nodeParam.getType().equals(TextConvertorType.group.name()) && nodeParam.getGroups().isEmpty()) {
			result.setSuccess(false);
			result.getErrorInfos().add("Group content is empty");
		}

		return result;
	}

}
