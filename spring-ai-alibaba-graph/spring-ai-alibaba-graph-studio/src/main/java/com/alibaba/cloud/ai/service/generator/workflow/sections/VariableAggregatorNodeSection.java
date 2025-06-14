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
import com.alibaba.cloud.ai.service.generator.workflow.NodeSection;
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
		var data = (VariableAggregatorNodeData) node.getData();
		List<String> inputKeys = data.getVariables().stream().map(pair -> {
			String fromNodeId = pair.get(0);
			return varNames.getOrDefault(fromNodeId, fromNodeId + "_output");
		}).toList();
		String outputKey = data.getOutputKey();
		;

		String id = node.getId();
		String keysLit = inputKeys.stream().map(k -> "\"" + k + "\"").collect(Collectors.joining(", "));
		return String.format(
				"        // —— VariableAggregatorNode [%s] ——%n"
						+ "        VariableAggregatorNode %s = VariableAggregatorNode.builder()%n"
						+ "                .inputKeys(List.of(%s))%n" + "                .outputKey(\"%s\")%n"
						+ "                .build();%n"
						+ "        stateGraph.addNode(\"%s\", AsyncNodeAction.node_async(%s));%n%n",
				id, varName, keysLit, outputKey, id, varName);
	}

}
