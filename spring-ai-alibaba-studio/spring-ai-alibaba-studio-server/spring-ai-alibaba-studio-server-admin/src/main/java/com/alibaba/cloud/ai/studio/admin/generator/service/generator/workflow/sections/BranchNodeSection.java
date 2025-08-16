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
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.Case;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.Edge;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.Node;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.NodeType;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.nodedata.BranchNodeData;
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
				if (condition.getVarType().equalsIgnoreCase("String")) {
					constValue = "\"" + constValue + "\"";
				}
				String objType = switch (condition.getVarType()) {
					case "string", "String" -> "String.class";
					case "list", "List", "Array", "array" -> "List.class";
					default ->
						condition.getComparisonOperator().getSupportedClassList().get(0).getName().concat(".class");
				};
				String objName = "unknown";
				try {
					if (nodeMap.containsKey(condition.getVariableSelector().getNamespace())) {
						Node inputNode = nodeMap.get(condition.getVariableSelector().getNamespace());
						objName = inputNode.getData().getOutputs().get(0).getName();
					}
				}
				catch (Exception ignore) {
				}
				objName = String.format("state.value(\"%s\", %s).orElseThrow()", objName, objType);
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

}
