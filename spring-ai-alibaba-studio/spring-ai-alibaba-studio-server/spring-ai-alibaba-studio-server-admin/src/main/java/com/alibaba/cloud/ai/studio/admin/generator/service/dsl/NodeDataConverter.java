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
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.alibaba.cloud.ai.studio.admin.generator.model.Variable;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.NodeData;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.NodeType;
import com.google.common.base.Strings;

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
	 * After parseMapData is complete and varName is injected, call: Used to override the
	 * default outputKey based on varName and refresh the list of outputs
	 * @param nodeData {@link NodeData}
	 * @param varName nodeVarName
	 */
	default void postProcessOutput(T nodeData, String varName) {
		// 将所有的输出变量的名称统一为"nodeVarName_varName"的格式
		Optional.ofNullable(nodeData.getOutputs())
			.ifPresentOrElse((outputs) -> nodeData.setOutputs(outputs.stream().peek(v -> {
				String name = v.getName();
				v.setName(varName.concat("_").concat(name));
			}).toList()), () -> nodeData.setOutputs(List.of()));
	}

	/**
	 * 生成用于处理inputKey和inputSelector的Consumer
	 * @return 一个BiConsumer，接受参数：T nodeData和Map idToVarName
	 */
	default BiConsumer<T, Map<String, String>> postProcessConsumer(DSLDialectType dialectType) {
		return switch (dialectType) {
			case DIFY -> (nodeData, idToVarName) -> {
				// 将所有的输入变量的nodeId转化为nodeName
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
			case CUSTOM -> (nodeData, idToVarName) -> {
			};
			case AGENT -> (nodeData, idToVarName) -> {
				// agent DSL 不走 workflow node 流程，这里 no-op 以满足枚举覆盖
			};
		};
	}

	/**
	 * 将文本中变量占位符进行转化，比如Dify DSL的"你好，{{#123.query#}}"转化为"你好，{nodeName1.query}"
	 * @param dialectType dsl语言
	 * @return BiFunction，输入待转换的字符串和nodeId与nodeName的映射，输出转换结果
	 */
	static BiFunction<String, Map<String, String>, String> convertVarReserveFunction(DSLDialectType dialectType) {
		return switch (dialectType) {
			case DIFY -> (str, idToVarName) -> {
				// todo: 模板支持上下文
				if (Strings.isNullOrEmpty(str)) {
					return str;
				}
				StringBuilder result = new StringBuilder();
				Pattern pattern = Pattern.compile("\\{\\{#(\\w+)\\.(\\w+)#}}");
				Matcher matcher = pattern.matcher(str);
				while (matcher.find()) {
					String nodeId = matcher.group(1);
					String varName = matcher.group(2);
					String res = "{"
							+ idToVarName.getOrDefault(nodeId, StringUtils.hasText(nodeId) ? nodeId : "unknown") + "_"
							+ varName + "}";
					matcher.appendReplacement(result, Matcher.quoteReplacement(res));
				}
				matcher.appendTail(result);
				return result.toString();
			};
			case CUSTOM -> (str, idToVarName) -> str;
			case AGENT -> (str, idToVarName) -> str;
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
