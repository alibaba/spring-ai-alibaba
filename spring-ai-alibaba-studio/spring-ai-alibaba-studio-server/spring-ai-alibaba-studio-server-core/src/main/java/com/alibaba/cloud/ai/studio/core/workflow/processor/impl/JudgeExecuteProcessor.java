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
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.NodeStatusEnum;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.NodeTypeEnum;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.ValueFromEnum;
import com.alibaba.cloud.ai.studio.runtime.utils.JsonUtils;
import com.alibaba.cloud.ai.studio.core.config.CommonConfig;
import com.alibaba.cloud.ai.studio.core.base.manager.RedisManager;
import com.alibaba.cloud.ai.studio.core.workflow.JudgeOperator;
import com.alibaba.cloud.ai.studio.runtime.enums.ParameterTypeEnum;
import com.alibaba.cloud.ai.studio.core.workflow.WorkflowContext;
import com.alibaba.cloud.ai.studio.core.utils.common.VariableUtils;
import com.alibaba.cloud.ai.studio.core.workflow.WorkflowInnerService;
import com.alibaba.cloud.ai.studio.core.workflow.processor.AbstractExecuteProcessor;
import com.google.common.collect.Lists;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 判断节点
 */
@Slf4j
@Component("JudgeExecuteProcessor")
public class JudgeExecuteProcessor extends AbstractExecuteProcessor {

	public JudgeExecuteProcessor(RedisManager redisManager, WorkflowInnerService workflowInnerService,
			ChatMemory conversationChatMemory, CommonConfig commonConfig) {
		super(redisManager, workflowInnerService, conversationChatMemory, commonConfig);
	}

	@Override
	public String getNodeType() {
		return NodeTypeEnum.JUDGE.getCode();
	}

	@Override
	public String getNodeDescription() {
		return NodeTypeEnum.JUDGE.getDesc();
	}

	@Override
	public NodeResult innerExecute(DirectedAcyclicGraph<String, Edge> graph, Node node, WorkflowContext context) {
		NodeResult nodeResult = initNodeResultAndRefreshContext(node, context);
		NodeParam config = JsonUtils.fromMap(node.getConfig().getNodeParam(), NodeParam.class);
		Optional<Branch> any = config.getBranches().stream().filter(branch -> {
			String id = branch.getId();
			return conditionHit(id, branch, context);
		}).findFirst();
		if (any.isPresent()) {
			Branch branch = any.get();
			List<String> targetIds = null;
			Set<Edge> edges = graph.outgoingEdgesOf(node.getId());
			if (CollectionUtils.isNotEmpty(edges)) {
				targetIds = edges.stream()
					.filter(edge -> edge.getSourceHandle().equals(edge.getSource() + "_" + branch.getId()))
					.map(Edge::getTarget)
					.collect(Collectors.toList());
			}
			nodeResult.setInput(constructInput(config.getBranches(), context));
			Map<String, Object> outputObj = new HashMap<>();
			if (CollectionUtils.isNotEmpty(targetIds)) {
				String targetIdString = targetIds.stream().collect(Collectors.joining(","));
				outputObj.put(OUTPUT_DECORATE_PARAM_KEY, "Hit branch target node: " + targetIdString);
				nodeResult.setNodeStatus(NodeStatusEnum.SUCCESS.getCode());
			}
			else {
				outputObj.put(OUTPUT_DECORATE_PARAM_KEY, "Hit branch target node: empty");
				nodeResult.setErrorInfo("Hit branch target node: empty");
				nodeResult.setNodeStatus(NodeStatusEnum.FAIL.getCode());
			}

			nodeResult.setOutput(JsonUtils.toJson(outputObj));
			nodeResult.setMultiBranch(true);
			NodeResult.MultiBranchReference branchReference = new NodeResult.MultiBranchReference();
			branchReference.setConditionId(branch.getId());
			branchReference.setTargetIds(targetIds);
			nodeResult.setMultiBranchResults(Lists.newArrayList(branchReference));
		}
		else {
			Optional<Branch> defaultOptional = config.getBranches()
				.stream()
				.filter(branch -> "default".equals(branch.getId()))
				.findFirst();
			if (defaultOptional.isPresent()) {
				Branch branch = defaultOptional.get();
				List<String> targetIds = null;
				Set<Edge> edges = graph.outgoingEdgesOf(node.getId());
				if (CollectionUtils.isNotEmpty(edges)) {
					targetIds = edges.stream()
						.filter(edge -> edge.getSourceHandle().equals(edge.getSource() + "_default"))
						.map(Edge::getTarget)
						.collect(Collectors.toList());
				}
				Map<String, Object> outputObj = new HashMap<>();
				if (CollectionUtils.isNotEmpty(targetIds)) {
					String targetIdString = targetIds.stream().collect(Collectors.joining(","));
					outputObj.put(OUTPUT_DECORATE_PARAM_KEY, "Hit branch target node: " + targetIdString);
				}
				else {
					outputObj.put(OUTPUT_DECORATE_PARAM_KEY, "Default branch target node: empty");
					nodeResult.setErrorInfo("Default branch target node: empty");
					nodeResult.setNodeStatus(NodeStatusEnum.FAIL.getCode());
				}
				nodeResult.setOutput(JsonUtils.toJson(outputObj));
				nodeResult.setMultiBranch(true);
				NodeResult.MultiBranchReference branchReference = new NodeResult.MultiBranchReference();
				branchReference.setConditionId(branch.getId());
				branchReference.setTargetIds(targetIds);
				nodeResult.setMultiBranchResults(Lists.newArrayList(branchReference));
			}
			else {
				nodeResult.setNodeStatus(NodeStatusEnum.FAIL.getCode());
				nodeResult.setErrorInfo("No condition branch hit");
			}

		}
		return nodeResult;
	}

	private String constructInput(List<Branch> originBranches, WorkflowContext context) {
		List<Branch> branches = originBranches.stream()
			.filter(branch -> !branch.getId().equals("default"))
			.collect(Collectors.toList());
		List<Map<String, Object>> inputList = Lists.newArrayList();
		for (int i = 0; i < branches.size(); i++) {
			Branch branch = branches.get(i);
			Map<String, Object> branchObj = new HashMap<>();
			List<Condition> conditions = branch.getConditions();
			List<Map<String, Object>> subBranches = Lists.newArrayList();
			conditions.stream().forEach(condition -> {
				Map<String, Object> subBranchObj = new HashMap<>();
				Object leftValueObj = VariableUtils.getValueFromContext(condition.getLeft(), context);
				Object rightValueObj = VariableUtils.getValueFromContext(condition.getRight(), context);
				String leftValue;
				if (leftValueObj instanceof Map || leftValueObj instanceof List) {
					leftValue = JsonUtils.toJson(leftValueObj);
				}
				else {
					leftValue = String.valueOf(leftValueObj);
				}
				String rightValue;
				if (rightValueObj instanceof Map || rightValueObj instanceof List) {
					rightValue = JsonUtils.toJson(rightValueObj);
				}
				else {
					rightValue = String.valueOf(rightValueObj);
				}
				subBranchObj.put("leftKey", condition.getLeft().getValue());
				subBranchObj.put("leftValue", leftValue);
				subBranchObj.put("operator", condition.getOperator());
				String rightValueFrom = condition.getRight().getValueFrom();
				if (ValueFromEnum.refer.name().equals(rightValueFrom)) {
					subBranchObj.put("rightKey", condition.getRight().getValue());
				}
				subBranchObj.put("rightValue", rightValue);
				subBranches.add(subBranchObj);
			});
			branchObj.put("conditionId", branch.getId());
			branchObj.put("subBranches", subBranches);
			branchObj.put("logic", branch.getLogic());
			branchObj.put("label", "Condition " + (i + 1));
			inputList.add(branchObj);
		}
		return JsonUtils.toJson(inputList);
	}

	private boolean conditionHit(String id, Branch branch, WorkflowContext context) {
		// The default logic does not participate in hit
		if (id.equals("default")) {
			return false;
		}
		String logic = branch.getLogic();
		// The default logic is AND
		if (StringUtils.isBlank(logic)) {
			logic = Logic.and.name();
		}
		List<Condition> conditions = branch.getConditions();
		if (CollectionUtils.isEmpty(conditions)) {
			// If no conditions are configured, go directly to the next step, considered
			// as not hit
			return false;
		}
		return checkCondition(logic, conditions, context);
	}

	private boolean checkCondition(String logic, List<Condition> conditions, WorkflowContext context) {
		Logic logicEnum = Logic.valueOf(logic);
		switch (logicEnum) {
			case and:
				for (Condition condition : conditions) {
					Node.InputParam left = condition.getLeft();
					String operator = condition.getOperator();
					Node.InputParam right = condition.getRight();
					String leftValue = VariableUtils.getValueStringFromContext(left, context);
					String rightValue = VariableUtils.getValueStringFromContext(right, context);
					if (!leftOperateRight(left, leftValue, right, rightValue, operator)) {
						return false;
					}
				}
				return true;
			case or:
				for (Condition condition : conditions) {
					Node.InputParam left = condition.getLeft();
					String operator = condition.getOperator();
					Node.InputParam right = condition.getRight();
					String leftValue = VariableUtils.getValueStringFromContext(left, context);
					String rightValue = VariableUtils.getValueStringFromContext(right, context);
					if (leftOperateRight(left, leftValue, right, rightValue, operator)) {
						return true;
					}
				}
				return false;
			default:
				return false;
		}
	}

	private boolean leftOperateRight(Node.InputParam left, String leftValue, Node.InputParam right, String rightValue,
			String operator) {

		String leftType = left.getType();
		String rightType = right.getType();
		// The left value can only be referenced, not filled in, so the type of left
		// cannot be empty
		if (StringUtils.isBlank(leftType) || StringUtils.isBlank(operator)) {
			return false;
		}

		// If the right value is a reference but the type is unknown, return false
		// directly
		if (ValueFromEnum.refer.name().equals(right.getValueFrom()) && StringUtils.isBlank(rightType)) {
			if (!operator.equals(JudgeOperator.IS_NULL.getCode())
					&& !operator.equals(JudgeOperator.IS_NOT_NULL.getCode())
					&& !operator.equals(JudgeOperator.IS_TRUE.getCode())
					&& !operator.equals(JudgeOperator.IS_FALSE.getCode())) {
				// is_null, is_not_null, is_true, is_false do not require a right value
				return false;
			}
		}

		Set<String> operatorScopeSet = JudgeOperator.getAllCodes();
		Set<String> typeSet = ParameterTypeEnum.getAllCodes();
		// Return false for illegal operators or illegal types
		if (!operatorScopeSet.contains(operator) || !typeSet.contains(leftType)) {
			return false;
		}
		// Return false if the operator does not match the operand
		if (!JudgeOperator.getOperator(operator).getScopeSet().contains(leftType)) {
			return false;
		}
		try {
			switch (operator) {
				case "equals":
					if ("string".equalsIgnoreCase(leftType)) {
						return leftValue.equals(rightValue);
					}
					else if ("number".equalsIgnoreCase(leftType)) {
						return compareDigits(leftValue, rightValue) == 0;
					}
					else if ("boolean".equalsIgnoreCase(leftType)) {
						return Boolean.parseBoolean(leftValue) == Boolean.parseBoolean(rightValue);
					}
					return false;

				case "notEquals":
					if ("string".equalsIgnoreCase(leftType)) {
						return !leftValue.equals(rightValue);
					}
					else if ("number".equalsIgnoreCase(leftType)) {
						return compareDigits(leftValue, rightValue) != 0;
					}
					else if ("boolean".equalsIgnoreCase(leftType)) {
						return (Boolean.parseBoolean(leftValue) ^ Boolean.parseBoolean(rightValue));
					}
					return false;

				case "isNull":
					if ("string".equalsIgnoreCase(leftType)) {
						return StringUtils.isEmpty(leftValue);
					}
					else if ("number".equalsIgnoreCase(leftType)) {
						return StringUtils.isEmpty(leftValue);
					}
					else if ("boolean".equalsIgnoreCase(leftType)) {
						return StringUtils.isEmpty(leftValue);
					}
					else if (leftType.toLowerCase().startsWith("array")) {
						List<Object> objects = JsonUtils.fromJsonToList(leftValue, Object.class);
						return CollectionUtils.isEmpty(objects);
					}
					else if ("object".equalsIgnoreCase(leftType)) {
						return Objects.isNull(JsonUtils.fromJsonToMap(leftValue));
					}
					return false;

				case "isNotNull":
					if ("string".equalsIgnoreCase(leftType)) {
						return !StringUtils.isEmpty(leftValue);
					}
					else if ("number".equalsIgnoreCase(leftType)) {
						return true;
					}
					else if ("boolean".equalsIgnoreCase(leftType)) {
						return true;
					}
					else if (leftType.toLowerCase().startsWith("array")) {
						List<Object> objects = JsonUtils.fromJsonToList(leftValue, Object.class);
						return !CollectionUtils.isEmpty(objects);
					}
					else if ("object".equalsIgnoreCase(leftType)) {
						return !Objects.isNull(JsonUtils.fromJsonToMap(leftValue));
					}
					return false;

				case "greater":
					if ("number".equalsIgnoreCase(leftType)) {
						return compareDigits(leftValue, rightValue) > 0;
					}
					return false;

				case "greaterAndEqual":
					if ("number".equalsIgnoreCase(leftType)) {
						return compareDigits(leftValue, rightValue) >= 0;
					}
					return false;

				case "less":
					if ("number".equalsIgnoreCase(leftType)) {
						return compareDigits(leftValue, rightValue) < 0;
					}
					return false;

				case "lessAndEqual":
					if ("number".equalsIgnoreCase(leftType)) {
						return compareDigits(leftValue, rightValue) <= 0;
					}
					return false;

				case "isTrue":
					if ("boolean".equalsIgnoreCase(leftType)) {
						return Boolean.parseBoolean(leftValue);
					}
					return false;

				case "isFalse":
					if ("boolean".equalsIgnoreCase(leftType)) {
						return !Boolean.parseBoolean(leftValue);
					}
					return false;

				case "lengthEquals":
					if ("string".equalsIgnoreCase(leftType)) {
						if (NumberUtils.isCreatable(rightValue)) {
							// 如果右值是数字，则直接用左值的长度和这个数字进行比较
							return compareDigits(leftValue.length() + "", rightValue) == 0;
						}
						else {
							return leftValue.length() == rightValue.length();
						}
					}
					else if (leftType.toLowerCase().startsWith("array")) {
						List<Object> leftArrays = JsonUtils.fromJsonToList(leftValue, Object.class);
						// List<Object> rightArrays =
						// JsonUtils.fromJsonToList(rightValue);
						if (NumberUtils.isCreatable(rightValue)) {
							// 如果右值是数字，则直接用左值的长度和这个数字进行比较
							return compareDigits(leftArrays.size() + "", rightValue) == 0;
						}
						else {
							boolean isArray = JsonUtils.isJsonArray(rightValue) && rightValue.trim().startsWith("[");
							int leftSize = CollectionUtils.isEmpty(leftArrays) ? 0 : leftArrays.size();
							int rightSize = isArray ? JsonUtils.fromJsonToList(rightValue, Object.class).size()
									: rightValue.length();
							return leftSize == rightSize;
						}
					}
					return false;
				case "lengthGreater":
					if ("string".equalsIgnoreCase(leftType)) {
						if (NumberUtils.isCreatable(rightValue)) {
							// 如果右值是数字，则直接用左值的长度和这个数字进行比较
							return compareDigits(leftValue.length() + "", rightValue) > 0;
						}
						else {
							return leftValue.length() > rightValue.length();
						}
					}
					else if (leftType.toLowerCase().startsWith("array")) {
						List<Object> leftArrays = JsonUtils.fromJsonToList(leftValue, Object.class);
						// List<Object> rightArrays =
						// JsonUtils.fromJsonToList(rightValue);
						if (NumberUtils.isCreatable(rightValue)) {
							// 如果右值是数字，则直接用左值的长度和这个数字进行比较
							return compareDigits(leftArrays.size() + "", rightValue) > 0;
						}
						else {
							boolean isArray = JsonUtils.isJsonArray(rightValue) && rightValue.trim().startsWith("[");
							int leftSize = CollectionUtils.isEmpty(leftArrays) ? 0 : leftArrays.size();
							int rightSize = isArray ? JsonUtils.fromJsonToList(rightValue, Object.class).size()
									: rightValue.length();
							return leftSize > rightSize;
						}
					}
					return false;

				case "lengthGreaterAndEqual":
					if ("string".equalsIgnoreCase(leftType)) {
						if (NumberUtils.isCreatable(rightValue)) {
							// 如果右值是数字，则直接用左值的长度和这个数字进行比较
							return compareDigits(leftValue.length() + "", rightValue) >= 0;
						}
						else {
							return leftValue.length() >= rightValue.length();
						}
					}
					else if (leftType.toLowerCase().startsWith("array")) {
						List<Object> leftArrays = JsonUtils.fromJsonToList(leftValue, Object.class);
						// List<Object> rightArrays =
						// JsonUtils.fromJsonToList(rightValue);
						if (NumberUtils.isCreatable(rightValue)) {
							// 如果右值是数字，则直接用左值的长度和这个数字进行比较
							return compareDigits(leftArrays.size() + "", rightValue) >= 0;
						}
						else {
							boolean isArray = JsonUtils.isJsonArray(rightValue);
							int leftSize = CollectionUtils.isEmpty(leftArrays) ? 0 : leftArrays.size();
							int rightSize = isArray ? JsonUtils.fromJsonToList(rightValue, Object.class).size()
									: rightValue.length();
							return leftSize >= rightSize;
						}
					}
					return false;

				case "lengthLess":
					if ("string".equalsIgnoreCase(leftType)) {
						if (NumberUtils.isCreatable(rightValue)) {
							// 如果右值是数字，则直接用左值的长度和这个数字进行比较
							return compareDigits(leftValue.length() + "", rightValue) < 0;
						}
						else {
							return leftValue.length() < rightValue.length();
						}
					}
					else if (leftType.toLowerCase().startsWith("array")) {
						List<Object> leftArrays = JsonUtils.fromJsonToList(leftValue, Object.class);
						// List<Object> rightArrays =
						// JsonUtils.fromJsonToList(rightValue);
						if (NumberUtils.isCreatable(rightValue)) {
							// 如果右值是数字，则直接用左值的长度和这个数字进行比较
							return compareDigits(leftArrays.size() + "", rightValue) < 0;
						}
						else {
							boolean isArray = JsonUtils.isJsonArray(rightValue);
							int leftSize = CollectionUtils.isEmpty(leftArrays) ? 0 : leftArrays.size();
							int rightSize = isArray ? JsonUtils.fromJsonToList(rightValue, Object.class).size()
									: rightValue.length();
							return leftSize < rightSize;
						}
					}
					return false;

				case "lengthLessAndEqual":
					if ("string".equalsIgnoreCase(leftType)) {
						if (NumberUtils.isCreatable(rightValue)) {
							// 如果右值是数字，则直接用左值的长度和这个数字进行比较
							return compareDigits(leftValue.length() + "", rightValue) <= 0;
						}
						else {
							return leftValue.length() <= rightValue.length();
						}
					}
					else if (leftType.toLowerCase().startsWith("array")) {
						List<Object> leftArrays = JsonUtils.fromJsonToList(leftValue, Object.class);
						// List<Object> rightArrays =
						// JsonUtils.fromJsonToList(rightValue);
						if (NumberUtils.isCreatable(rightValue)) {
							// 如果右值是数字，则直接用左值的长度和这个数字进行比较
							return compareDigits(leftArrays.size() + "", rightValue) <= 0;
						}
						else {
							boolean isArray = JsonUtils.isJsonArray(rightValue) && rightValue.trim().startsWith("[");
							int leftSize = CollectionUtils.isEmpty(leftArrays) ? 0 : leftArrays.size();
							int rightSize = isArray ? JsonUtils.fromJsonToList(rightValue, Object.class).size()
									: rightValue.length();
							return leftSize <= rightSize;
						}
					}
					return false;

				case "contains":
					if ("string".equalsIgnoreCase(leftType)) {
						return leftValue.contains(rightValue);
					}
					else if (leftType.toLowerCase().startsWith("array")) {
						List<String> strings = JsonUtils.fromJsonToList(leftValue, String.class);
						if (CollectionUtils.isEmpty(strings)) {
							return false;
						}
						return strings.contains(rightValue);
					}
					else if ("object".equalsIgnoreCase(leftType)) {
						Map<String, Object> jsonObject = JsonUtils.fromJsonToMap(leftValue);
						if (jsonObject != null && jsonObject.containsKey(rightValue)) {
							return true;
						}
						return false;
					}
					return false;

				case "notContains":
					if ("string".equalsIgnoreCase(leftType)) {
						return !leftValue.contains(rightValue);
					}
					else if (leftType.toLowerCase().startsWith("array")) {
						List<String> strings = JsonUtils.fromJsonToList(leftValue, String.class);
						if (CollectionUtils.isEmpty(strings)) {
							return true;
						}
						return !strings.contains(rightValue);
					}
					else if ("object".equalsIgnoreCase(leftType)) {
						Map<String, Object> jsonObject = JsonUtils.fromJsonToMap(leftValue);
						if (jsonObject != null && jsonObject.containsKey(rightValue)) {
							return false;
						}
						return true;
					}
					return false;

				default:
					break;

			}
		}
		catch (Exception e) {
			log.error("Error occurred while evaluating condition: {}", e.getMessage(), e);
			return false;
		}

		return false;
	}

	private int compareDigits(String leftValue, String rightValue) {
		return new BigDecimal(leftValue).compareTo(new BigDecimal(rightValue));
	}

	private enum Logic {

		and, or

	}

	@Data
	public static class NodeParam {

		private List<Branch> branches;

	}

	@Data
	public static class Branch {

		private String id;

		private String label;

		private String logic;

		private List<Condition> conditions;

	}

	@Data
	public static class Condition {

		private String operator;

		private Node.InputParam left;

		private Node.InputParam right;

	}

	@Override
	public void handleVariables(DirectedAcyclicGraph<String, Edge> graph, Node node, WorkflowContext context,
			NodeResult nodeResult) {
		// No need to handle variables for judge node
	}

	@Override
	public CheckNodeParamResult checkNodeParam(DirectedAcyclicGraph<String, Edge> graph, Node node) {
		CheckNodeParamResult result = super.checkNodeParam(graph, node);
		CheckNodeParamResult inputParamsResult = checkInputParams(node.getConfig().getInputParams());
		if (!inputParamsResult.isSuccess()) {
			result.setSuccess(false);
			result.getErrorInfos().addAll(inputParamsResult.getErrorInfos());
		}
		NodeParam config = JsonUtils.fromMap(node.getConfig().getNodeParam(), NodeParam.class);
		List<Branch> notDefaultList = config.getBranches()
			.stream()
			.filter(branch -> !branch.getId().equals("default"))
			.collect(Collectors.toList());
		StringBuilder stringBuilder = new StringBuilder();
		if (CollectionUtils.isNotEmpty(notDefaultList)) {
			for (int i = 0; i < notDefaultList.size(); i++) {
				boolean needPrefix = false;
				Branch branch = notDefaultList.get(i);
				List<String> targetIds = null;
				Set<Edge> edges = graph.outgoingEdgesOf(node.getId());
				if (CollectionUtils.isNotEmpty(edges)) {
					targetIds = edges.stream()
						.filter(edge -> edge.getSourceHandle().equals(edge.getSource() + "_" + branch.getId()))
						.map(Edge::getTarget)
						.collect(Collectors.toList());
				}
				if (CollectionUtils.isEmpty(targetIds)) {
					needPrefix = true;
					stringBuilder.append("There are one or more conditional branches with no subsequent nodes;\n");
				}
				List<Condition> conditions = branch.getConditions();
				if (CollectionUtils.isEmpty(conditions)) {
					needPrefix = true;
					stringBuilder.append("No conditions selected;\n");
				}
				else {
					StringBuilder subStringBuilder = new StringBuilder();
					boolean needSubPrefix = false;
					for (int j = 0; j < conditions.size(); j++) {
						Condition condition = conditions.get(j);
						Node.InputParam left = condition.getLeft();
						if (left == null || left.getValue() == null) {
							needSubPrefix = true;
							subStringBuilder.append("The left value is empty;\n");
						}
						String operator = condition.getOperator();
						if (StringUtils.isBlank(operator)) {
							needSubPrefix = true;
							subStringBuilder.append("Operator not selected;\n");
						}
						Node.InputParam right = condition.getRight();
						if (right == null || left.getValue() == null) {
							if (StringUtils.isNotBlank(operator)
									&& !JudgeOperator.IS_FALSE.getCode().equalsIgnoreCase(operator)
									&& !JudgeOperator.IS_TRUE.getCode().equalsIgnoreCase(operator)
									&& !JudgeOperator.IS_NULL.getCode().equalsIgnoreCase(operator)
									&& !JudgeOperator.IS_NOT_NULL.getCode().equalsIgnoreCase(operator)) {
								needSubPrefix = true;
								subStringBuilder.append("The left value is empty;\n");
							}
						}
						if (needSubPrefix) {
							subStringBuilder.insert(0, "No." + (j + 1) + "Condition configuration error：\n");
						}
					}
					String subStr = subStringBuilder.toString();
					if (StringUtils.isNotBlank(subStr)) {
						stringBuilder.append(subStr);
					}
				}
				if (needPrefix) {
					stringBuilder.insert(0, "No." + (i + 1) + "Branch configuration error：\n");
				}
			}
		}
		Optional<Branch> defaultBranchOptional = config.getBranches()
			.stream()
			.filter(branch -> branch.getId().equals("default"))
			.findFirst();
		if (defaultBranchOptional.isPresent()) {
			List<String> targetIds = null;
			Set<Edge> edges = graph.outgoingEdgesOf(node.getId());
			if (CollectionUtils.isNotEmpty(edges)) {
				targetIds = edges.stream()
					.filter(edge -> edge.getSourceHandle().equals(edge.getSource() + "_default"))
					.map(Edge::getTarget)
					.collect(Collectors.toList());
			}
			if (CollectionUtils.isEmpty(targetIds)) {
				stringBuilder.append("[default] The conditional branch does not have a subsequent node;");
			}
		}
		String errorInfo = stringBuilder.toString();
		if (StringUtils.isNotBlank(errorInfo)) {
			result.setSuccess(false);
			result.getErrorInfos().add(errorInfo);
		}
		return result;
	}

}
