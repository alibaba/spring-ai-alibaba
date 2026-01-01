/*
 * Copyright 2024-2026 the original author or authors.
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
package com.alibaba.cloud.ai.studio.admin.builder.generator.service.generator.workflow.sections;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.alibaba.cloud.ai.studio.admin.builder.generator.model.VariableType;
import com.alibaba.cloud.ai.studio.admin.builder.generator.model.workflow.Case;
import com.alibaba.cloud.ai.studio.admin.builder.generator.model.workflow.Edge;
import com.alibaba.cloud.ai.studio.admin.builder.generator.model.workflow.Node;
import com.alibaba.cloud.ai.studio.admin.builder.generator.model.workflow.NodeType;
import com.alibaba.cloud.ai.studio.admin.builder.generator.model.workflow.nodedata.BranchNodeData;
import com.alibaba.cloud.ai.studio.admin.builder.generator.model.VariableSelector;
import com.alibaba.cloud.ai.studio.admin.builder.generator.service.generator.workflow.NodeSection;

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
		sb.append(String.format("// —�?BranchNode [%s] —�?n", id));
		// 条件判断在条件边上，本节点为空节�?
		sb.append(String.format("stateGraph.addNode(\"%s\", AsyncNodeAction.node_async(state -> Map.of()));%n%n",
				varName));

		return sb.toString();
	}

	@Override
	public String renderEdges(BranchNodeData branchNodeData, List<Edge> edges) {
		// 此处规定Edge的sourceHandle为caseId，前面的转化需要符合这条规�?
		String srcVar = branchNodeData.getVarName();
		StringBuilder sb = new StringBuilder();
		List<Case> cases = branchNodeData.getCases();

		// 维护一个caseId到caseName的映�?
		AtomicInteger count = new AtomicInteger(1);
		Map<String, String> caseIdToName = cases.stream()
			.map(Case::getId)
			.collect(Collectors.toUnmodifiableMap(id -> id, id -> {
				// 如果一些节点的caseId本身就有含义，直接使�?
				if (id.equalsIgnoreCase("default") || id.equalsIgnoreCase("true") || id.equalsIgnoreCase("false")) {
					return id;
				}
				return "case_" + (count.getAndIncrement());
			}));

		// 构造EdgeAction.apply函数
		StringBuilder conditionsBuffer = new StringBuilder();
		for (Case c : cases) {
			String logicalOperator = " " + c.getLogicalOperator().getCodeValue() + " ";
			List<String> expressions = c.getConditions().stream().map(condition -> {
				String constValue = condition.getValue();
				if (condition.getReferenceValue() != null && (VariableType.STRING.equals(condition.getVarType())
						|| VariableType.FILE.equals(condition.getVarType()))) {
					constValue = "\"" + constValue + "\"";
				}

				// 根据变量类型生成安全的访问代�?
				String objName = generateSafeVariableAccess(condition);
				return condition.getComparisonOperator().convert(objName, constValue);
			}).toList();
			conditionsBuffer.append("if(");
			// 组合复合条件
			conditionsBuffer.append(String.join(logicalOperator, expressions));
			conditionsBuffer.append(") {\n");
			conditionsBuffer.append(String.format("return \"%s\";", caseIdToName.get(c.getId())));
			conditionsBuffer.append("}\n");
		}
		// 最后需要加上else的结�?
		conditionsBuffer.append(String.format("return \"%s\";", branchNodeData.getDefaultCase()));

		// 构建Map
		Map<String, String> edgeCaseMap = edges.stream()
			.collect(Collectors.toMap(e -> caseIdToName.getOrDefault(e.getSourceHandle(), e.getSourceHandle()),
					Edge::getTarget));
		String edgeCaseMapStr = "Map.of(" + edgeCaseMap.entrySet()
			.stream()
			.flatMap(e -> Stream.of(e.getKey(), e.getValue()))
			.map(v -> String.format("\"%s\"", v))
			.collect(Collectors.joining(", ")) + ")";

		// 构建最终代�?
		sb.append("stateGraph.addConditionalEdges(\"")
			.append(srcVar)
			.append("\", edge_async(state -> {\n")
			.append(conditionsBuffer)
			.append("}), ")
			.append(edgeCaseMapStr)
			.append(");\n\n");

		return sb.toString();
	}

	private String generateSafeVariableAccess(Case.Condition condition) {
		VariableType varType = condition.getVarType();
		String variablePath = buildVariablePath(condition);

		switch (varType) {
			case FILE:
				// 支持�?VariableSelector 中获取属性路�?
				VariableSelector selector = condition.getTargetSelector();
				boolean accessExtension = selector != null
						&& (selector.getLabel() != null && selector.getLabel().contains("extension")
								|| selector.getName() != null && selector.getName().contains("extension"));

				if (accessExtension) {
					// 如果是访问扩展名属性，直接访问扩展名字�?
					return String.format("state.value(\"%s\", String.class).orElse(null)", variablePath);
				}
				else {
					// 从文件对象中提取扩展�?
					return String.format(
							"state.value(\"%s\", java.io.File.class).map(file -> { " + "String name = file.getName(); "
									+ "int dotIndex = name.lastIndexOf('.'); "
									+ "return dotIndex > 0 ? name.substring(dotIndex) : \"\"; " + "}).orElse(null)",
							variablePath);
				}
				// 默认返回null，避免isNull判断恒为false
			case STRING:
				return String.format("state.value(\"%s\", String.class).orElse(null)", variablePath);
			case NUMBER:
				return String.format("state.value(\"%s\", Number.class).orElse(null)", variablePath);
			case BOOLEAN:
				return String.format("state.value(\"%s\", Boolean.class).orElse(null)", variablePath);
			case ARRAY_FILE:
			case ARRAY_NUMBER:
			case ARRAY_STRING:
			case ARRAY_OBJECT:
			case ARRAY_BOOLEAN:
			case ARRAY:
				return String.format("state.value(\"%s\", List.class).orElse(null)", variablePath);
			case OBJECT:
				return String.format("state.value(\"%s\", Object.class).orElse(null)", variablePath);
			default:
				// 使用默认的类�?
				return String.format("state.value(\"%s\", Object.class).orElse(null)", variablePath);
		}
	}

	private String buildVariablePath(Case.Condition condition) {
		VariableSelector variableSelector = condition.getTargetSelector();
		if (variableSelector == null) {
			return "unknown";
		}
		return Optional.ofNullable(variableSelector.getNameInCode()).orElse("unknown");
	}

	@Override
	public List<String> getImports() {
		return List.of("static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async");
	}

}
