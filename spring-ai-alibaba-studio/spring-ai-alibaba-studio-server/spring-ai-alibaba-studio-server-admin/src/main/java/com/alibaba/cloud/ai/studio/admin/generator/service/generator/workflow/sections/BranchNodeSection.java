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
package com.alibaba.cloud.ai.studio.admin.generator.service.generator.workflow.sections;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.Case;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.Edge;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.Node;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.NodeType;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.nodedata.BranchNodeData;
import com.alibaba.cloud.ai.studio.admin.generator.model.VariableSelector;
import com.alibaba.cloud.ai.studio.admin.generator.service.generator.workflow.NodeSection;

import org.springframework.stereotype.Component;

@Component
public class BranchNodeSection implements NodeSection<BranchNodeData> {

	@Override
	public boolean support(NodeType nodeType) {
		return NodeType.BRANCH.equals(nodeType);
	}

	@Override
	public String render(Node node, String varName) {
		String id = node.getId();

		StringBuilder sb = new StringBuilder();
		sb.append(String.format("// —— BranchNode [%s] ——%n", id));
		// 条件判断在条件边上，本节点为空节点
		sb.append(String.format("stateGraph.addNode(\"%s\", AsyncNodeAction.node_async(state -> Map.of()));%n%n",
				varName));

		return sb.toString();
	}

	@Override
	public String renderConditionalEdges(BranchNodeData branchNodeData, Map<String, Node> nodeMap,
			Map.Entry<String, List<Edge>> entry, Map<String, String> varNames) {
		String srcVar = varNames.get(entry.getKey());
		StringBuilder sb = new StringBuilder();
		List<Case> cases = branchNodeData.getCases();

		// 构造EdgeAction.apply函数
		StringBuilder conditionsBuffer = new StringBuilder();
		for (Case c : cases) {
			String logicalOperator = " " + c.getLogicalOperator().getValue() + " ";
			List<String> expressions = c.getConditions().stream().map(condition -> {
				String constValue = condition.getValue();
				if (condition.getVarType().equalsIgnoreCase("String")
						|| condition.getVarType().equalsIgnoreCase("file")) {
					constValue = "\"" + constValue + "\"";
				}

				// 根据变量类型生成安全的访问代码
				String objName = generateSafeVariableAccess(condition, nodeMap);
				return condition.getComparisonOperator().convert(objName, constValue);
			}).toList();
			conditionsBuffer.append("if(");
			// 组合复合条件
			conditionsBuffer.append(String.join(logicalOperator, expressions));
			conditionsBuffer.append(") {\n");
			conditionsBuffer.append(String.format("return \"%s\";", c.getId()));
			conditionsBuffer.append("}\n");
		}
		// 最后需要加上else的结果
		conditionsBuffer.append("return \"false\";");

		// 构建Map
		Map<String, String> edgeCaseMap = entry.getValue()
			.stream()
			.collect(Collectors.toMap(Edge::getSourceHandle, Edge::getTarget));
		String edgeCaseMapStr = "Map.of(" + edgeCaseMap.entrySet()
			.stream()
			.flatMap(e -> Stream.of(e.getKey(), varNames.getOrDefault(e.getValue(), "unknown")))
			.map(v -> String.format("\"%s\"", v))
			.collect(Collectors.joining(", ")) + ")";

		// 构建最终代码
		sb.append("stateGraph.addConditionalEdges(\"")
			.append(srcVar)
			.append("\", edge_async(state -> {\n")
			.append(conditionsBuffer)
			.append("}), ")
			.append(edgeCaseMapStr)
			.append(");\n");

		return sb.toString();
	}

	private String generateSafeVariableAccess(Case.Condition condition, Map<String, Node> nodeMap) {
		String varType = condition.getVarType();
		String variablePath = buildVariablePath(condition, nodeMap);

		switch (varType.toLowerCase()) {
			case "file":
				// 支持从 VariableSelector 中获取属性路径
				VariableSelector selector = condition.getVariableSelector();
				boolean accessExtension = selector != null
						&& (selector.getLabel() != null && selector.getLabel().contains("extension")
								|| selector.getName() != null && selector.getName().contains("extension"));

				if (accessExtension) {
					// 如果是访问扩展名属性，直接访问扩展名字段
					return String.format("state.value(\"%s\", String.class).orElse(\"\")", variablePath);
				}
				else {
					// 从文件对象中提取扩展名
					return String.format(
							"state.value(\"%s\", java.io.File.class).map(file -> { " + "String name = file.getName(); "
									+ "int dotIndex = name.lastIndexOf('.'); "
									+ "return dotIndex > 0 ? name.substring(dotIndex) : \"\"; " + "}).orElse(\"\")",
							variablePath);
				}
			case "string":
				return String.format("state.value(\"%s\", String.class).orElse(\"\")", variablePath);
			case "number":
				return String.format("state.value(\"%s\", Number.class).orElse(0)", variablePath);
			case "boolean":
				return String.format("state.value(\"%s\", Boolean.class).orElse(false)", variablePath);
			case "list":
			case "array":
				return String.format(
						"state.value(\"%s\", java.util.List.class).orElse(java.util.Collections.emptyList())",
						variablePath);
			case "object":
				return String.format("state.value(\"%s\", Object.class).orElse(null)", variablePath);
			default:
				// 使用默认的类型
				return String.format("state.value(\"%s\", Object.class).orElse(null)", variablePath);
		}
	}

	private String buildVariablePath(Case.Condition condition, Map<String, Node> nodeMap) {
		VariableSelector variableSelector = condition.getVariableSelector();
		if (variableSelector == null) {
			return "unknown";
		}

		// 其中第一个是节点ID，第二个是变量名，第三个是属性

		String nodeId = variableSelector.getNamespace();
		String variableName = variableSelector.getName();

		// 如果有节点映射，尝试获取正确的变量名
		if (nodeMap.containsKey(nodeId)) {
			Node inputNode = nodeMap.get(nodeId);
			if (inputNode.getData().getOutputs() != null && !inputNode.getData().getOutputs().isEmpty()) {
				// 使用输出定义中的变量名
				String outputName = inputNode.getData().getOutputs().get(0).getName();
				return outputName != null ? outputName : variableName;
			}
		}

		// 如果无法从节点映射获取，直接使用变量名
		return variableName != null ? variableName : "unknown";
	}

}
