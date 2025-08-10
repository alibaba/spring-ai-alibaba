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

package com.alibaba.cloud.ai.service.generator.workflow.sections;

import com.alibaba.cloud.ai.model.VariableSelector;
import com.alibaba.cloud.ai.model.workflow.Edge;
import com.alibaba.cloud.ai.model.workflow.Node;
import com.alibaba.cloud.ai.model.workflow.NodeType;
import com.alibaba.cloud.ai.model.workflow.nodedata.QuestionClassifierNodeData;
import com.alibaba.cloud.ai.service.generator.workflow.NodeSection;
import com.google.common.base.Strings;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class QuestionClassifierNodeSection implements NodeSection<QuestionClassifierNodeData> {

	@Override
	public boolean support(NodeType nodeType) {
		return NodeType.QUESTION_CLASSIFIER.equals(nodeType);
	}

	@Override
	public String render(Node node, String varName) {
		QuestionClassifierNodeData data = (QuestionClassifierNodeData) node.getData();
		String id = node.getId();

		StringBuilder sb = new StringBuilder();
		sb.append(String.format("// —— QuestionClassifierNode [%s] ——%n", id));
		sb.append(String.format("QuestionClassifierNode %s = QuestionClassifierNode.builder()%n", varName));

		sb.append(".chatClient(chatClient)\n");

		List<VariableSelector> inputs = data.getInputs();
		if (inputs != null && !inputs.isEmpty()) {
			String key = inputs.get(0).getNameInCode();
			sb.append(String.format(".inputTextKey(\"%s\")%n", escape(key)));
		}
		else {
			sb.append(".inputTextKey(\"input\")\n");
		}

		List<String> categoryIds = data.getClasses()
			.stream()
			.map(QuestionClassifierNodeData.ClassConfig::getText)
			.toList();
		if (!categoryIds.isEmpty()) {
			String joined = categoryIds.stream()
				.map(this::escape)
				.map(s -> "\"" + s + "\"")
				.collect(Collectors.joining(", "));
			sb.append(String.format(".categories(List.of(%s))%n", joined));
		}

		String outputKey = data.getOutputKey();
		if (!Strings.isNullOrEmpty(outputKey)) {
			sb.append(String.format(".outputKey(\"%s\")%n", escape(outputKey)));
		}

		String instr = data.getInstruction();
		if (instr != null && !instr.isBlank()) {
			sb.append(String.format(".classificationInstructions(List.of(\"%s\"))%n", escape(instr)));
		}
		else {
			sb.append(".classificationInstructions(List.of(\"请根据输入内容选择对应分类\"))\n");
		}

		sb.append(".build();\n");
		sb.append(String.format("stateGraph.addNode(\"%s\", AsyncNodeAction.node_async(%s));%n%n", varName, varName));

		return sb.toString();
	}

	private String resolveConditionKey(QuestionClassifierNodeData classifier, String handleId) {
		return classifier.getClasses()
			.stream()
			.filter(c -> c.getId().equals(handleId))
			.map(QuestionClassifierNodeData.ClassConfig::getText)
			.findFirst()
			.orElse(handleId);
	}

	@Override
	public String renderConditionalEdges(QuestionClassifierNodeData nodeData, Map<String, Node> nodeMap,
			Map.Entry<String, List<Edge>> entry, Map<String, String> varNames) {
		String sourceId = entry.getKey();
		List<Edge> condEdges = entry.getValue();
		List<String> conditions = new ArrayList<>();
		List<String> mappings = new ArrayList<>();
		String srcVar = varNames.get(sourceId);
		StringBuilder sb = new StringBuilder();

		List<String> endNodes = new ArrayList<>();

		// 如果输出的都不是预定分类，则使用最后一个分类
		String lastConditionKey = "unknown";

		for (Edge e : condEdges) {
			Map<String, Object> data = e.getData();
			String targetType = data != null ? (String) data.get("targetType") : null;
			String conditionKey = resolveConditionKey(nodeData, e.getSourceHandle());
			String tgtVar2 = varNames.get(e.getTarget());
			lastConditionKey = conditionKey;
			conditions.add(String.format("if (value.contains(\"%s\")) return \"%s\";", conditionKey, conditionKey));
			mappings.add(String.format("\"%s\", \"%s\"", conditionKey, tgtVar2));
			if ("end".equalsIgnoreCase(targetType)) {
				endNodes.add(tgtVar2);
			}
		}

		String lambdaContent = String.join("\n", conditions);
		String mapContent = String.join(", ", mappings);

		sb.append(String.format(
				"stateGraph.addConditionalEdges(\"%s\",%n" + "            edge_async(state -> {%n"
						+ "String value = state.value(\"%s_class_name\", String.class).orElse(\"\");%n" + "%s%n"
						+ "return \"%s\";%n" + "            }),%n" + "            Map.of(%s)%n" + ");%n",
				srcVar, srcVar, lambdaContent, lastConditionKey, mapContent));

		// 补充end节点与END的边，如果有
		if (!endNodes.isEmpty()) {
			sb.append("stateGraph");
			endNodes.forEach(end -> sb.append(String.format("%n.addEdge(\"%s\", END)", end)));
			sb.append(String.format(";%n"));
		}

		return sb.toString();
	}

}
