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
package com.alibaba.cloud.ai.service.dsl.nodes;

import com.alibaba.cloud.ai.model.Variable;
import com.alibaba.cloud.ai.model.VariableSelector;
import com.alibaba.cloud.ai.model.VariableType;
import com.alibaba.cloud.ai.model.workflow.NodeType;
import com.alibaba.cloud.ai.model.workflow.nodedata.EndNodeData;
import com.alibaba.cloud.ai.service.dsl.AbstractNodeDataConverter;
import com.alibaba.cloud.ai.service.dsl.DSLDialectType;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Component
public class EndNodeDataConverter extends AbstractNodeDataConverter<EndNodeData> {

	@Override
	public Boolean supportNodeType(NodeType nodeType) {
		return NodeType.END.equals(nodeType);
	}

	@Override
	protected List<DialectConverter<EndNodeData>> getDialectConverters() {
		return Stream.of(EndNodeDialectConverter.values()).map(EndNodeDialectConverter::dialectConverter).toList();
	}

	private enum EndNodeDialectConverter {

		DIFY(new DialectConverter<>() {
			@Override
			public Boolean supportDialect(DSLDialectType dialectType) {
				return DSLDialectType.DIFY.equals(dialectType);
			}

			@Override
			public EndNodeData parse(Map<String, Object> data) {
				List<Map<String, Object>> outputsMap = (List<Map<String, Object>>) data.get("outputs");
				List<VariableSelector> inputs = outputsMap.stream().map(output -> {
					List<String> valueSelector = (List<String>) output.get("value_selector");
					String variable = (String) output.get("variable");
					return new VariableSelector(valueSelector.get(0), valueSelector.get(1)).setLabel(variable);
				}).toList();
				List<Variable> outputs = inputs.stream()
					.map(input -> new Variable(input.getLabel(), VariableType.OBJECT.value()))
					.toList();
				return new EndNodeData(inputs, outputs);
			}

			@Override
			public Map<String, Object> dump(EndNodeData nodeData) {
				Map<String, Object> data = new HashMap<>();
				List<Map<String, Object>> outputsMap = nodeData.getInputs()
					.stream()
					.map(input -> Map.of("value_selector", List.of(input.getNamespace(), input.getName()), "variable",
							input.getLabel()))
					.toList();
				data.put("outputs", outputsMap);
				return data;
			}
		}), CUSTOM(AbstractNodeDataConverter.defaultCustomDialectConverter(EndNodeData.class));

		private final DialectConverter<EndNodeData> dialectConverter;

		public DialectConverter<EndNodeData> dialectConverter() {
			return dialectConverter;
		}

		EndNodeDialectConverter(DialectConverter<EndNodeData> converter) {
			this.dialectConverter = converter;
		}

	}

}
