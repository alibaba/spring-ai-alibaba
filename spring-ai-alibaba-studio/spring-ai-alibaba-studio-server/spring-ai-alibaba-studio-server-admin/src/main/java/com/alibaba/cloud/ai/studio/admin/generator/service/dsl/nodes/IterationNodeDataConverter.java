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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.alibaba.cloud.ai.studio.admin.generator.model.Variable;
import com.alibaba.cloud.ai.studio.admin.generator.model.VariableSelector;
import com.alibaba.cloud.ai.studio.admin.generator.model.VariableType;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.NodeType;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.nodedata.IterationNodeData;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.AbstractNodeDataConverter;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.DSLDialectType;
import com.fasterxml.jackson.core.JsonProcessingException;

import org.springframework.stereotype.Component;

/**
 * @author vlsmb
 * @since 2025/7/22
 */
@Component
public class IterationNodeDataConverter extends AbstractNodeDataConverter<IterationNodeData> {

	private enum IterationNodeDialectConverter {

		DIFY(new DialectConverter<IterationNodeData>() {
			@Override
			public Boolean supportDialect(DSLDialectType dialectType) {
				return DSLDialectType.DIFY.equals(dialectType);
			}

			@Override
			public IterationNodeData parse(Map<String, Object> data) throws JsonProcessingException {
				// 获取输入输出的类型，从 array[xxx] 中提取xxx
				Pattern typePattern = Pattern.compile("array\\[(.*?)]");
				String inputType = "object";
				String outputType = "object";
				Matcher inputTypeMatcher = typePattern
					.matcher((String) data.getOrDefault("iterator_input_type", "object"));
				Matcher outputTypeMatcher = typePattern.matcher((String) data.getOrDefault("output_type", "object"));
				if (inputTypeMatcher.find()) {
					inputType = inputTypeMatcher.group(1);
				}
				if (outputTypeMatcher.find()) {
					outputType = outputTypeMatcher.group(1);
				}
				List<String> inputSelector = (List<String>) data.get("iterator_selector");
				List<String> outputSelector = (List<String>) data.get("output_selector");
				String startNodeId = (String) data.get("start_node_id");
				String id = (String) data.get("id");
				// 规定输出结果的节点为最后一个节点
				String endNodeId = outputSelector.get(0);
				// 返回
				return IterationNodeData.builder()
					.id(id)
					.inputType(inputType)
					.outputType(outputType)
					.inputSelector(new VariableSelector(inputSelector.get(0), inputSelector.get(1), ""))
					.outputSelector(new VariableSelector(outputSelector.get(0), outputSelector.get(1), ""))
					.startNodeId(startNodeId)
					.endNodeId(endNodeId)
					.inputKey(id + "_input")
					.outputKey(id + "_output")
					.build();
			}

			@Override
			public Map<String, Object> dump(IterationNodeData nodeData) {
				return Map.of();
			}
		}), CUSTOM(AbstractNodeDataConverter.defaultCustomDialectConverter(IterationNodeData.class));

		private final DialectConverter<IterationNodeData> dialectConverter;

		public DialectConverter<IterationNodeData> dialectConverter() {
			return dialectConverter;
		}

		IterationNodeDialectConverter(DialectConverter<IterationNodeData> dialectConverter) {
			this.dialectConverter = dialectConverter;
		}

	}

	@Override
	protected List<DialectConverter<IterationNodeData>> getDialectConverters() {
		return Stream.of(IterationNodeDialectConverter.values())
			.map(IterationNodeDialectConverter::dialectConverter)
			.toList();
	}

	@Override
	public Boolean supportNodeType(NodeType nodeType) {
		return nodeType.equals(NodeType.ITERATION);
	}

	@Override
	public String generateVarName(int count) {
		return "iteration" + count;
	}

	@Override
	public void postProcessOutput(IterationNodeData nodeData, String varName) {
		nodeData.setOutputKey(varName + "_" + IterationNodeData.getDefaultOutputSchema().getName());
		nodeData.setOutputs(List.of(IterationNodeData.getDefaultOutputSchema()));
		nodeData.setOutput(IterationNodeData.getDefaultOutputSchema());
		super.postProcessOutput(nodeData, varName);
	}

	@Override
	public BiConsumer<IterationNodeData, Map<String, String>> postProcessConsumer(DSLDialectType dialectType) {
		return switch (dialectType) {
			case DIFY -> super.postProcessConsumer(dialectType).andThen((iterationNodeData, varNames) -> {
				// 等待所有的节点都生成了变量名后，补充迭代节点的起始名称
				iterationNodeData
					.setStartNodeName(varNames.getOrDefault(iterationNodeData.getStartNodeId(), "unknown"));
				iterationNodeData.setEndNodeName(varNames.getOrDefault(iterationNodeData.getEndNodeId(), "unknown"));

				// 更新迭代节点的输入Key
				VariableSelector inputSelector = iterationNodeData.getInputs().get(0);
				iterationNodeData.setInputKey(inputSelector.getNameInCode());

				// 更新迭代节点的ResultKey
				VariableSelector outputSelector = iterationNodeData.getOutputSelector();
				iterationNodeData.setInnerItemResultKey(
						Optional.ofNullable(varNames.get(outputSelector.getNamespace())).orElse("unknown") + "_"
								+ outputSelector.getName());
			});
			case CUSTOM -> super.postProcessConsumer(dialectType);
            default -> super.postProcessConsumer(dialectType);
        };
	}

	@Override
	public Stream<Variable> extractWorkflowVars(IterationNodeData nodeData) {
		return Stream.concat(nodeData.getOutputs().stream(),
				Stream.of(new Variable(nodeData.getInnerArrayKey(), "string"),
						new Variable(nodeData.getInnerStartFlagKey(), VariableType.STRING.value()),
						new Variable(nodeData.getInnerEndFlagKey(), VariableType.STRING.value()),
						new Variable(nodeData.getInnerItemKey(), nodeData.getInputType()),
						new Variable(nodeData.getInnerIndexKey(), VariableType.NUMBER.value()),
						new Variable(nodeData.getInnerItemResultKey(), nodeData.getOutputType())));
	}

}
