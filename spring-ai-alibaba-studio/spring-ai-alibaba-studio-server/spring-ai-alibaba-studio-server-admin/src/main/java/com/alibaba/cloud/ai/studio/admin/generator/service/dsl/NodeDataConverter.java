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
package com.alibaba.cloud.ai.studio.admin.generator.service.dsl;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import com.alibaba.cloud.ai.studio.admin.generator.model.Variable;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.NodeData;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.NodeType;

import org.springframework.util.StringUtils;

/**
 * NodeDataConverter defined the mutual conversion between specific DSL data and
 * {@link NodeData}
 */
public interface NodeDataConverter<T extends NodeData> {

	/**
	 * Judge if this converter support this node type
	 * @param nodeType {@link NodeType}
	 * @return true if support
	 */
	Boolean supportNodeType(NodeType nodeType);

	/**
	 * Parse DSL data to NodeData
	 * @param data DSL data
	 * @return converted {@link NodeData}
	 */
	T parseMapData(Map<String, Object> data, DSLDialectType dialectType);

	/**
	 * Dump NodeData to DSL map data
	 * @param nodeData {@link NodeData}
	 * @return converted DSL node data <strong>The returned Map must be
	 * modifiable</strong>
	 */
	Map<String, Object> dumpMapData(T nodeData, DSLDialectType dialectType);

	/**
	 * Generate a readable variable name prefix for this node, such as "http1", "llm2",
	 * and so on
	 * @param count
	 * @return friendly varName
	 */
	default String generateVarName(int count) {
		throw new UnsupportedOperationException(getClass().getSimpleName() + " must implement generateVarName");
	}

	/**
	 * 统一处理节点的输入输出变量名称，生成用于处理outputKey、inputKey、inputSelector 以及其他需要后置处理操作的Consumer
	 * @return 一个BiConsumer，接受参数：T nodeData和Map idToVarName
	 */
	default BiConsumer<T, Map<String, String>> postProcessConsumer(DSLDialectType dialectType) {
		return (nodeData, idToVarName) -> {
			// 将所有的输出变量的名称统一为"nodeVarName_varName"的格式
			Optional.ofNullable(nodeData.getOutputs())
				.ifPresentOrElse((outputs) -> nodeData.setOutputs(outputs.stream().peek(v -> {
					String name = v.getName();
					v.setName(nodeData.getVarName().concat("_").concat(name));
				}).toList()), () -> nodeData.setOutputs(List.of()));

			// 将所有的输入变量的nodeId转化为nodeName，并保存到nameInCode字段中
			nodeData.setInputs(
					Optional.ofNullable(nodeData.getInputs()).orElse(List.of()).stream().peek(variableSelector -> {
						String nodeId = variableSelector.getNamespace();
						String nodeName = idToVarName.get(nodeId);
						if (StringUtils.hasText(nodeName)) {
							variableSelector.setNamespace(nodeName);
						}
						variableSelector
							.setNameInCode(variableSelector.getNamespace() + "_" + variableSelector.getName());
					}).toList());
		};
	}

	/**
	 * Fetch the global state variable for this node (usually its list of outputs)
	 * @param nodeData {@link NodeData}
	 * @return Variable stream
	 */
	default Stream<Variable> extractWorkflowVars(T nodeData) {
		List<Variable> outs = nodeData.getOutputs();
		return outs == null ? Stream.empty() : outs.stream();
	}

}
