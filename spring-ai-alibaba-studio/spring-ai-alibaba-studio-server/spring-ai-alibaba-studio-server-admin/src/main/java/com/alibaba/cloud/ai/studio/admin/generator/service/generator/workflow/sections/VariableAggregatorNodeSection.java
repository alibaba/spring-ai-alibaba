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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.alibaba.cloud.ai.studio.admin.generator.model.VariableSelector;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.Node;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.NodeType;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.nodedata.VariableAggregatorNodeData;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.nodedata.VariableAggregatorNodeData.Groups;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.DSLDialectType;
import com.alibaba.cloud.ai.studio.admin.generator.service.generator.workflow.NodeSection;

import org.springframework.stereotype.Component;

@Component
public class VariableAggregatorNodeSection implements NodeSection<VariableAggregatorNodeData> {

	@Override
	public boolean support(NodeType nodeType) {
		return NodeType.AGGREGATOR.equals(nodeType);
	}

	@Override
	public String render(Node node, String varName) {
		VariableAggregatorNodeData data = (VariableAggregatorNodeData) node.getData();
		StringBuilder sb = new StringBuilder();

		String outputKey = data.getOutputKey();
		VariableAggregatorNodeData.AdvancedSettings advancedSettings = data.getAdvancedSettings();
		List<Groups> groups = advancedSettings.getGroups();
		boolean hasGroup = advancedSettings != null && groups != null && !groups.isEmpty()
				&& advancedSettings.isGroupEnabled();
		// build advancedSettings and group
		if (hasGroup) {
			AtomicInteger idx = new AtomicInteger(1);
			sb.append("// - Build advancedSettings and group \n");
			// build group
			groups.forEach(group -> {
				sb.append(String.format("VariableAggregatorNode.Group group%s = new VariableAggregatorNode.Group();\n",
						idx));
				sb.append(String.format("    group%s.setGroupName(\"%s\");\n", idx, group.getGroupName()));
				sb.append(String.format("    group%s.setVariables(List.of(%s));\n", idx,
						renderVariables(group.getVariableSelectors())));
				sb.append(String.format("    group%s.setGroupId(\"%s\");\n", idx, group.getGroupId()));
				sb.append(String.format("    group%s.setOutputType(\"%s\");\n", idx, group.getOutputType()));
				idx.getAndIncrement();
			});
			// build advancedSettings
			sb.append(
					"VariableAggregatorNode.AdvancedSettings advancedSettings = new VariableAggregatorNode.AdvancedSettings();\n");
			sb.append("advancedSettings.setGroupEnabled(true);\n");
			sb.append(String.format("advancedSettings.setGroups(List.of(%s));%n",
					IntStream.range(1, idx.get())
						.boxed()
						.map(i -> String.format("group%s", i))
						.collect(Collectors.joining(", "))));

		}

		String id = node.getId();

		sb.append(String.format("// —— VariableAggregatorNode [%s] ——%n", id));
		sb.append(String.format("VariableAggregatorNode %s = VariableAggregatorNode.builder()\n", varName));

		// .variables
		sb.append(String.format("    .variables(List.of(%s))\n", renderVariables(data.getInputs())));

		// .outputKey
		sb.append(String.format("    .outputKey(\"%s\")\n", outputKey));

		// .outputType(...) if present
		sb.append("    .outputType(\"list\")\n");

		// .advancedSettings(...) if present
		if (hasGroup) {
			sb.append("    .advancedSettings(advancedSettings)\n");
		}

		sb.append("    .build();\n");

		// 辅助节点，将节点输出转为定义的格式
		String assistNodeCode;
		if (hasGroup) {
			assistNodeCode = String.format("wrapperAggregatorNodeAction(%s, \"%s\", \"%s\", %s)", varName, varName,
					varName + "_output", true);
		}
		else {
			assistNodeCode = String.format("wrapperAggregatorNodeAction(%s, \"%s\", \"%s\", %s)", varName, varName,
					data.getOutputKey(), false);
		}

		sb.append(String.format("stateGraph.addNode(\"%s\", AsyncNodeAction.node_async(%s));\n\n", varName,
				assistNodeCode));

		return sb.toString();
	}

	// build variables list
	private String renderVariables(List<VariableSelector> variables) {
		return variables.stream()
			.map(VariableSelector::getNameInCode)
			.map(name -> String.format("List.of(\"%s\")", name))
			.collect(Collectors.joining(", "));
	}

	@Override
	public String assistMethodCode(DSLDialectType dialectType) {
		return switch (dialectType) {
			case DIFY ->
				"""
						private NodeAction wrapperAggregatorNodeAction(NodeAction nodeAction, String nodeName, String key, boolean hasGroup) {
						    if (hasGroup) {
						        return (state) -> {
						            Map<String, Object> result = nodeAction.apply(state);
						            Object object = result.get(key);
						            if ((object instanceof Map<?, ?> map)) {
						                return map.entrySet()
						                        .stream()
						                        .collect(Collectors.toMap(k -> nodeName + "_" + k.getKey().toString(), v -> {
						                            if (v.getValue() instanceof List<?> list) {
						                                return list.isEmpty() ? "unknown" : list.get(0);
						                            }
						                            else {
						                                return v.getValue() == null ? "unknown" : v.getValue().toString();
						                            }
						                        }));
						            }
						            else if (object instanceof List<?> list) {
						                return Map.of(key, list.isEmpty() ? "unknown" : list.get(0));
						            }
						            else {
						                return Map.of(key, object == null ? "unknown" : object.toString());
						            }
						        };
						    } else {
						        return (state) -> {
						            Map<String, Object> result = nodeAction.apply(state);
						            Object object = result.get(key);
						            if (object instanceof List<?> list) {
						                return Map.of(key, list.isEmpty() ? "unknown" : list.get(0));
						            } else {
						                return Map.of(key, object == null ? "unknown" : object.toString());
						            }
						        };
						    }
						}
						""";
			default -> "";
		};
	}

	@Override
	public List<String> getImports() {
		return List.of("com.alibaba.cloud.ai.graph.node.VariableAggregatorNode", "java.util.stream.Collectors");
	}

}
