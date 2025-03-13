/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.service.dsl.nodes;

import com.alibaba.cloud.ai.model.VariableSelector;
import com.alibaba.cloud.ai.model.VariableType;
import com.alibaba.cloud.ai.model.workflow.Case;
import com.alibaba.cloud.ai.model.workflow.NodeType;
import com.alibaba.cloud.ai.model.workflow.nodedata.BranchNodeData;
import com.alibaba.cloud.ai.service.dsl.AbstractNodeDataConverter;
import com.alibaba.cloud.ai.service.dsl.DSLDialectType;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
				List<Map<String, Object>> casesMap = (List<Map<String, Object>>) data.get("cases");
				List<Case> cases = new ArrayList<>();
				if (casesMap != null) {
					for (Map<String, Object> caseData : casesMap) {
						// convert cases
						List<Map<String, Object>> conditionMaps = (List<Map<String, Object>>) caseData
							.get("conditions");
						List<Case.Condition> conditions = conditionMaps.stream().map(conditionMap -> {
							List<String> selectors = (List<String>) conditionMap.get("variable_selector");
							String difyVarType = (String) conditionMap.get("varType");
							VariableType variableType = VariableType.fromDifyValue(difyVarType)
								.orElse(VariableType.OBJECT);
							return new Case.Condition().setValue((String) conditionMap.get("value"))
								.setVarType(variableType.value())
								.setComparisonOperator((String) conditionMap.get("comparison_operator"))
								.setVariableSelector(new VariableSelector(selectors.get(0), selectors.get(1)));
						}).collect(Collectors.toList());
						cases.add(new Case().setId((String) caseData.get("id"))
							.setLogicalOperator((String) caseData.get("logical_operator"))
							.setConditions(conditions));
					}
				}
				return new BranchNodeData(List.of(), List.of()).setCases(cases);
			}

			@Override
			public Map<String, Object> dump(BranchNodeData nodeData) {
				Map<String, Object> data = new HashMap<>();
				List<Map<String, Object>> caseMaps = nodeData.getCases().stream().map(c -> {
					List<Map<String, Object>> conditions = c.getConditions()
						.stream()
						.map(condition -> Map.of("comparison_operator", condition.getComparisonOperator(), "value",
								condition.getValue(), "varType", condition.getVarType(), "variable_selector",
								List.of(condition.getVariableSelector().getNamespace(),
										condition.getVariableSelector().getName())))
						.toList();
					return Map.of("id", c.getId(), "case_id", c.getId(), "conditions", conditions, "logical_operator",
							c.getLogicalOperator());
				}).toList();
				data.put("cases", caseMaps);
				return data;
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

}
