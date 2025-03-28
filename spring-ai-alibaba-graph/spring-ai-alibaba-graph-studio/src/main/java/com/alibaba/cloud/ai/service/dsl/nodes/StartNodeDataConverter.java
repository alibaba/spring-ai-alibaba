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
import com.alibaba.cloud.ai.model.workflow.nodedata.StartNodeData;
import com.alibaba.cloud.ai.service.dsl.AbstractNodeDataConverter;
import com.alibaba.cloud.ai.service.dsl.DSLDialectType;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Stream;

@Component
public class StartNodeDataConverter extends AbstractNodeDataConverter<StartNodeData> {

	@Override
	public Boolean supportNodeType(NodeType nodeType) {
		return NodeType.START.equals(nodeType);
	}

	@Override
	protected List<DialectConverter<StartNodeData>> getDialectConverters() {
		return Stream.of(StartNodeDialectConverter.values()).map(StartNodeDialectConverter::dialectConverter).toList();
	}

	private enum StartNodeDialectConverter {

		DIFY(new DialectConverter<>() {
			@Override
			public Boolean supportDialect(DSLDialectType dialectType) {
				return DSLDialectType.DIFY.equals(dialectType);
			}

			@Override
			public StartNodeData parse(Map<String, Object> data) {
				List<Map<String, Object>> inputMap = (List<Map<String, Object>>) data.get("variables");
				List<StartNodeData.StartInput> startInputs = new ArrayList<>();
				List<Variable> outputs = new ArrayList<>();
				List<VariableSelector> inputs = new ArrayList<>();
				ObjectMapper objectMapper = new ObjectMapper();
				objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
				objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
				for (Map<String, Object> variable : inputMap) {
					StartNodeData.StartInput startInput = objectMapper.convertValue(variable,
							StartNodeData.StartInput.class);
					String inputType = startInput.getType();
					String varType = VariableType.fromDifyValue(inputType).orElse(VariableType.OBJECT).value();
					outputs.add(new Variable(startInput.getVariable(), varType));
					startInputs.add(startInput);
					inputs.add(new VariableSelector("", startInput.getVariable(), startInput.getLabel()));
				}
				return new StartNodeData(inputs, outputs).setStartInputs(startInputs);
			}

			@Override
			public Map<String, Object> dump(StartNodeData nodeData) {
				ObjectMapper objectMapper = new ObjectMapper();
				objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
				objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
				Map<String, Object> data = new HashMap<>();
				data.put("variables", objectMapper.convertValue(nodeData.getStartInputs(), List.class));
				return data;
			}
		}), CUSTOM(AbstractNodeDataConverter.defaultCustomDialectConverter(StartNodeData.class));

		private final DialectConverter<StartNodeData> dialectConverter;

		public DialectConverter<StartNodeData> dialectConverter() {
			return dialectConverter;
		}

		StartNodeDialectConverter(DialectConverter<StartNodeData> dialectConverter) {
			this.dialectConverter = dialectConverter;
		}

	}

}
