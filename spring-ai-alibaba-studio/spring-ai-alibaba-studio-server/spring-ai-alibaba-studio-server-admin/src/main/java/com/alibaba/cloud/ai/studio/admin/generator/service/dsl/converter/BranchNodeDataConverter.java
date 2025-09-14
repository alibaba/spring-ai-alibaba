/*
 * Copyright 2024-2025 the original author or authors.
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
package com.alibaba.cloud.ai.studio.admin.generator.service.dsl.converter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.alibaba.cloud.ai.studio.admin.generator.model.Variable;
import com.alibaba.cloud.ai.studio.admin.generator.model.VariableSelector;
import com.alibaba.cloud.ai.studio.admin.generator.model.VariableType;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.Case;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.ComparisonOperatorType;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.LogicalOperatorType;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.NodeType;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.nodedata.BranchNodeData;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.AbstractNodeDataConverter;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.DSLDialectType;

import com.alibaba.cloud.ai.studio.admin.generator.utils.MapReadUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.stereotype.Component;

@Component
public class BranchNodeDataConverter extends AbstractNodeDataConverter<BranchNodeData> {

	@Override
	public Boolean supportNodeType(NodeType nodeType) {
		return NodeType.BRANCH.equals(nodeType);
	}

	@Override
	protected List<DialectConverter<BranchNodeData>> getDialectConverters() {
		return Stream.of(BranchNodeDialectConverter.values())
			.map(BranchNodeDialectConverter::dialectConverter)
			.collect(Collectors.toList());
	}

	private enum BranchNodeDialectConverter {

		DIFY(new DialectConverter<>() {
			@Override
			public Boolean supportDialect(DSLDialectType dialectType) {
				return DSLDialectType.DIFY.equals(dialectType);
			}

			@Override
			public BranchNodeData parse(Map<String, Object> data) {
				@SuppressWarnings("unchecked")
				List<Map<String, Object>> casesMap = (List<Map<String, Object>>) data.get("cases");
				List<Case> cases = new ArrayList<>();
				if (casesMap != null) {
					for (Map<String, Object> caseData : casesMap) {
						// convert cases
						@SuppressWarnings("unchecked")
						List<Map<String, Object>> conditionMaps = (List<Map<String, Object>>) caseData
							.get("conditions");
						List<Case.Condition> conditions = conditionMaps.stream().map(conditionMap -> {
							@SuppressWarnings("unchecked")
							List<String> selectors = (List<String>) conditionMap.get("variable_selector");
							String difyVarType = (String) conditionMap.get("varType");
							VariableType variableType = VariableType.fromDifyValue(difyVarType)
								.orElse(VariableType.OBJECT);
							return new Case.Condition().setReferenceValue((String) conditionMap.get("value"))
								.setVarType(variableType)
								.setComparisonOperator(ComparisonOperatorType.fromDslValue(DSLDialectType.DIFY,
										(String) conditionMap.get("comparison_operator"), variableType))
								.setTargetSelector(new VariableSelector(selectors.get(0), selectors.get(1)));
						}).collect(Collectors.toList());
						cases.add(new Case().setId((String) caseData.get("id"))
							.setLogicalOperator(
									LogicalOperatorType.fromValue((String) caseData.get("logical_operator")))
							.setConditions(conditions));
					}
				}

				return new BranchNodeData().setCases(cases).setDefaultCase("false");
			}

			@Override
			public Map<String, Object> dump(BranchNodeData nodeData) {
				throw new UnsupportedOperationException();
			}
		}),

		STUDIO(new DialectConverter<>() {
			@Override
			public Boolean supportDialect(DSLDialectType dialectType) {
				return DSLDialectType.STUDIO.equals(dialectType);
			}

			@Override
			public BranchNodeData parse(Map<String, Object> data) throws JsonProcessingException {
				BranchNodeData nodeData = new BranchNodeData();

				// 获取条件信息
				List<Map<String, Object>> caseList = Optional
					.ofNullable(MapReadUtil.safeCastToListWithMap(
							MapReadUtil.getMapDeepValue(data, List.class, "config", "node_param", "branches")))
					.orElse(List.of())
					.stream()
					.filter(caseMap -> caseMap.containsKey("id"))
					.toList();
				String defaultCase = caseList.stream()
					.filter(map -> !map.containsKey("conditions"))
					.map(map -> map.get("id").toString())
					.findFirst()
					.orElse("default");
				List<Case> cases = caseList.stream().filter(map -> map.containsKey("conditions")).map(map -> {
					String id = MapReadUtil.getMapDeepValue(map, String.class, "id");
					LogicalOperatorType logicalOperatorType = LogicalOperatorType
						.fromValue(Optional.ofNullable(MapReadUtil.getMapDeepValue(map, String.class, "logic"))
							.orElse(LogicalOperatorType.AND.getValue()));

					// 提取Conditions
					List<Map<String, Object>> conditionMap = Optional
						.ofNullable(MapReadUtil
							.safeCastToListWithMap(MapReadUtil.getMapDeepValue(map, List.class, "conditions")))
						.orElse(List.of())
						.stream()
						.filter(mp -> mp.containsKey("left") && mp.containsKey("right") && mp.containsKey("operator"))
						.toList();
					List<Case.Condition> conditions = conditionMap.stream().map(mp -> {
						String rightFrom = MapReadUtil.getMapDeepValue(mp, String.class, "right", "value_from");
						String leftValue = MapReadUtil.getMapDeepValue(mp, String.class, "left", "value");
						String rightValue = MapReadUtil.getMapDeepValue(mp, String.class, "right", "value");

						VariableType variableType = VariableType
							.fromStudioValue(
									Optional.ofNullable(MapReadUtil.getMapDeepValue(mp, String.class, "left", "type"))
										.orElse(VariableType.OBJECT.studioValue()))
							.orElseThrow();
						VariableType referenceType = VariableType
							.fromStudioValue(
									Optional.ofNullable(MapReadUtil.getMapDeepValue(mp, String.class, "right", "type"))
										.orElse(VariableType.OBJECT.studioValue()))
							.orElseThrow();

						ComparisonOperatorType comparisonOperatorType = ComparisonOperatorType.fromDslValue(
								DSLDialectType.STUDIO, MapReadUtil.getMapDeepValue(mp, String.class, "operator"),
								variableType);

						VariableSelector targetSelector = this.varTemplateToSelector(DSLDialectType.STUDIO, leftValue);
						Case.Condition condition = new Case.Condition().setVarType(variableType)
							.setReferenceType(referenceType)
							.setTargetSelector(targetSelector)
							.setComparisonOperator(comparisonOperatorType);

						if ("refer".equalsIgnoreCase(rightFrom)) {
							VariableSelector referenceSelector = this.varTemplateToSelector(DSLDialectType.STUDIO,
									rightValue);
							condition.setReferenceSelector(referenceSelector);
						}
						else {
							condition.setReferenceValue(rightValue);
						}

						return condition;
					}).toList();

					return new Case().setId(id).setLogicalOperator(logicalOperatorType).setConditions(conditions);
				}).toList();

				// 设置基本信息
				nodeData.setCases(cases);
				nodeData.setDefaultCase(defaultCase);
				return nodeData;
			}

			@Override
			public Map<String, Object> dump(BranchNodeData nodeData) {
				throw new UnsupportedOperationException();
			}
		}),

		CUSTOM(AbstractNodeDataConverter.defaultCustomDialectConverter(BranchNodeData.class));

		private final DialectConverter<BranchNodeData> dialectConverter;

		public DialectConverter<BranchNodeData> dialectConverter() {
			return dialectConverter;
		}

		BranchNodeDialectConverter(DialectConverter<BranchNodeData> dialectConverter) {
			this.dialectConverter = dialectConverter;
		}

	}

	@Override
	public String generateVarName(int count) {
		return "branchNode" + count;
	}

	@Override
	public Stream<Variable> extractWorkflowVars(BranchNodeData data) {
		return Stream.empty();
	}

	@Override
	public BiConsumer<BranchNodeData, Map<String, String>> postProcessConsumer(DSLDialectType dialectType) {
		BiConsumer<BranchNodeData, Map<String, String>> consumer = super.postProcessConsumer(dialectType)
			.andThen((nodeData, idToVarName) -> {
				// 处理条件里的VariableSelector
				nodeData.getCases().forEach(c -> {
					c.getConditions().forEach(condition -> {
						VariableSelector selector = condition.getTargetSelector();
						selector
							.setNameInCode(idToVarName.getOrDefault(selector.getNamespace(), selector.getNamespace())
									+ "_" + selector.getName());
						VariableSelector referenceSelector = condition.getReferenceSelector();
						if (referenceSelector != null) {
							referenceSelector.setNameInCode(idToVarName.getOrDefault(referenceSelector.getNamespace(),
									referenceSelector.getNamespace()) + "_" + referenceSelector.getName());
						}
					});
				});
			});

		return switch (dialectType) {
			case DIFY -> consumer;
			case STUDIO -> consumer.andThen((nodeData, idToVarName) -> {
				// 将Case的caseId里添加nodeId（为了与Edge里的sourceHandle保持一致）
				String varName = nodeData.getVarName();
				String prefix = idToVarName.entrySet()
					.stream()
					.filter(entry -> entry.getValue().equals(varName))
					.map(Map.Entry::getKey)
					.findFirst()
					.orElseThrow() + "_";
				nodeData.getCases().forEach(c -> c.setId(prefix + c.getId()));
				nodeData.setDefaultCase(prefix + nodeData.getDefaultCase());
			});
			default -> super.postProcessConsumer(dialectType);
		};
	}

}
