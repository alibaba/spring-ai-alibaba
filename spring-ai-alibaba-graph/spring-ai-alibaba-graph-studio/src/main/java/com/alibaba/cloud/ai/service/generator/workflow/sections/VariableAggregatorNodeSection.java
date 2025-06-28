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

import com.alibaba.cloud.ai.model.workflow.Node;
import com.alibaba.cloud.ai.model.workflow.NodeType;
import com.alibaba.cloud.ai.model.workflow.nodedata.VariableAggregatorNodeData;
import com.alibaba.cloud.ai.model.workflow.nodedata.VariableAggregatorNodeData.Groups;
import com.alibaba.cloud.ai.service.generator.workflow.NodeSection;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class VariableAggregatorNodeSection implements NodeSection {

	private final Map<String, String> varNames;

	public VariableAggregatorNodeSection(Map<String, String> varNames) {
		this.varNames = varNames;
	}

	@Override
	public boolean support(NodeType nodeType) {
		return NodeType.AGGREGATOR.equals(nodeType);
	}

	@Override
	public String render(Node node, String varName) {
		VariableAggregatorNodeData data = (VariableAggregatorNodeData) node.getData();
		StringBuilder sb = new StringBuilder();

		String outputKey = data.getOutputKey();
		String outputType = data.getOutputType();
		VariableAggregatorNodeData.AdvancedSettings advancedSettings = data.getAdvancedSettings();
		List<Groups> groups = advancedSettings.getGroups();
		// build advancedSettings and group
		if (advancedSettings != null && groups != null && !groups.isEmpty()) {
			AtomicInteger idx = new AtomicInteger(1);
			sb.append("// - Build advancedSettings and group \n");
			// build group
			groups.forEach(group -> {
				sb.append(String.format("VariableAggregatorNode.Group group%s = new VariableAggregatorNode.Group();\n",
						idx));
				sb.append(String.format("    group%s.setGroupName(\"%s\");\n", idx, group.getGroupName()));
				sb.append(String.format("    group%s.setVariables(List.of(%s));\n", idx,
						renderVariables(group.getVariables())));
				sb.append(String.format("    group%s.setGroupId(\"%s\");\n", idx, group.getGroupId()));
				sb.append(String.format("    group%s.setOutputType(\"%s\");\n", idx, group.getOutputType()));
				idx.getAndIncrement();
			});
			// build advancedSettings
			sb.append(String.format(
					"VariableAggregatorNode.AdvancedSettings advancedSettings = new VariableAggregatorNode.AdvancedSettings();\n"));
			sb.append(String.format("advancedSettings.setGroupEnabled(true);\n"));
			sb.append(String.format("advancedSettings.setGroups(groups);\n"));

		}

		String id = node.getId();

		sb.append(String.format("// —— VariableAggregatorNode [%s] ——%n", id));
		sb.append(String.format("VariableAggregatorNode %s = VariableAggregatorNode.builder()\n", varName));

		// .variables
		List<List<String>> variables = data.getVariables();
		sb.append(String.format("    .variables(List.of(%s))\n", renderVariables(variables)));

		// .outputKey
		sb.append(String.format("    .outputKey(\"%s\")\n", outputKey));

		// .outputType(...) if present
		if (outputType != null && !outputType.isEmpty()) {
			sb.append(String.format("    .outputType(\"%s\")\n", outputType));
		}

		// .advancedSettings(...) if present
		if (advancedSettings != null && groups != null && !groups.isEmpty()) {
			sb.append(String.format("    .advancedSettings(advancedSettings)\n"));
		}

		sb.append("    .build();\n");
		sb.append(String.format("stateGraph.addNode(\"%s\", AsyncNodeAction.node_async(%s));\n\n", id, varName));

		return sb.toString();
	}

	// build variables list
	private String renderVariables(List<List<String>> variables) {
		if (variables == null || variables.isEmpty()) {
			return "";
		}

		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < variables.size(); i++) {
			List<String> path = variables.get(i);
			String listStr = "        List.of("
					+ path.stream().map(s -> "\"" + s.replace("\"", "\\\"") + "\"").collect(Collectors.joining(", "))
					+ ")";
			sb.append(listStr);

			if (i < variables.size() - 1) {
				sb.append(",\n");
			}
			else {
				sb.append("\n");
			}
		}
		return sb.toString();
	}

}
