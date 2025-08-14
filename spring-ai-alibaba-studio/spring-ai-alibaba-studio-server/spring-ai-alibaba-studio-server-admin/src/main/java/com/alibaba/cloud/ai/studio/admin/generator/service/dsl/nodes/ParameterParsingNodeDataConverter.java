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

package com.alibaba.cloud.ai.studio.admin.generator.service.dsl.nodes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.alibaba.cloud.ai.studio.admin.generator.model.Variable;
import com.alibaba.cloud.ai.studio.admin.generator.model.VariableSelector;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.NodeType;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.nodedata.ParameterParsingNodeData;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.AbstractNodeDataConverter;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.DSLDialectType;

import org.springframework.stereotype.Component;

/**
 * Convert the ParameterParsing node configuration in the Dify DSL to and from the
 * ParameterParsingNodeData object.
 */
@Component
public class ParameterParsingNodeDataConverter extends AbstractNodeDataConverter<ParameterParsingNodeData> {

	@Override
	public Boolean supportNodeType(NodeType nodeType) {
		return NodeType.PARAMETER_PARSING.equals(nodeType);
	}

	@Override
	protected List<DialectConverter<ParameterParsingNodeData>> getDialectConverters() {
		return Stream.of(ParameterParsingNodeConverter.values())
			.map(ParameterParsingNodeConverter::dialectConverter)
			.collect(Collectors.toList());
	}

	private enum ParameterParsingNodeConverter {

		DIFY(new DialectConverter<>() {
			@SuppressWarnings("unchecked")
			@Override
			public ParameterParsingNodeData parse(Map<String, Object> data) {
				// 获取指令
				String instruction = data.getOrDefault("instruction", "unknown").toString();
				Object query = data.get("query");
				// 获取输入
				VariableSelector input;
				if (query instanceof List<?> queryList && queryList.size() >= 2) {
					input = new VariableSelector(queryList.get(0).toString(), queryList.get(1).toString());
				}
				else {
					input = new VariableSelector("unknown", "unknown");
				}
				// 获取输出参数
				List<Map<String, Object>> parametersList = (List<Map<String, Object>>) data.getOrDefault("parameters",
						List.of());
				return new ParameterParsingNodeData("input", parametersList, instruction, "output", input);
			}

			@Override
			public Map<String, Object> dump(ParameterParsingNodeData nd) {
				Map<String, Object> m = new LinkedHashMap<>();

				if (nd.getInstruction() != null) {
					m.put("instruction", nd.getInstruction());
				}

				if (nd.getParameters() != null) {
					m.put("parameters", nd.getParameters());
				}

				if (nd.getInputs() != null && !nd.getInputs().isEmpty()) {
					VariableSelector selector = nd.getInputs().get(0);
					m.put("query", List.of(selector.getNamespace(), selector.getName()));
				}

				return m;
			}

			@Override
			public Boolean supportDialect(DSLDialectType dialect) {
				return DSLDialectType.DIFY.equals(dialect);
			}
		}),

		CUSTOM(defaultCustomDialectConverter(ParameterParsingNodeData.class));

		private final DialectConverter<ParameterParsingNodeData> converter;

		ParameterParsingNodeConverter(DialectConverter<ParameterParsingNodeData> converter) {
			this.converter = converter;
		}

		public DialectConverter<ParameterParsingNodeData> dialectConverter() {
			return converter;
		}

	}

	@Override
	public String generateVarName(int count) {
		return "parameterParsingNode" + count;
	}

	@Override
	public void postProcessOutput(ParameterParsingNodeData nodeData, String varName) {
		nodeData.setOutputKey(varName + "_output");
		List<Variable> variableList = nodeData.getParameters()
			.stream()
			.map(mp -> new Variable(mp.getOrDefault("name", "unknown").toString(),
					mp.getOrDefault("type", "string").toString())
				.setDescription(mp.getOrDefault("description", "").toString()))
			.toList();
		nodeData.setOutputs(variableList);
		super.postProcessOutput(nodeData, varName);
	}

	@Override
	public BiConsumer<ParameterParsingNodeData, Map<String, String>> postProcessConsumer(DSLDialectType dialectType) {
		return switch (dialectType) {
			case DIFY -> super.postProcessConsumer(dialectType).andThen((nodeData, varName) -> {
				nodeData.setInputTextKey(nodeData.getInputs().get(0).getNameInCode());
			});
			case CUSTOM -> super.postProcessConsumer(dialectType);
		};
	}

}
