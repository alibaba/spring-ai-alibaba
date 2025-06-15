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
import com.alibaba.cloud.ai.model.workflow.nodedata.HumanNodeData;
import com.alibaba.cloud.ai.service.generator.workflow.NodeSection;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class HumanNodeSection implements NodeSection {

	@Override
	public boolean support(NodeType nodeType) {
		return NodeType.HUMAN.equals(nodeType);
	}

	@Override
	public String render(Node node, String varName) {
		HumanNodeData d = (HumanNodeData) node.getData();
		String id = node.getId();
		StringBuilder sb = new StringBuilder();

		sb.append(String.format("// —— HumanNode [%s] ——%n", id));

		String condKey = d.getInterruptConditionKey();
		// If the policy is conditioned, you need to read the boolean value from the
		// state; Otherwise, it is always interrupted
		String condLambda = condKey != null
				? String.format("state -> state.value(\"%s\").map(v -> (Boolean)v).orElse(false)", condKey)
				: "state -> true";

		List<String> keys = d.getStateUpdateKeys();
		String updateLambda;
		if (keys != null && !keys.isEmpty()) {
			String keyListCode = keys.stream()
				.map(k -> "\"" + escape(k) + "\"")
				.collect(Collectors.joining(", ", "List.of(", ")"));
			updateLambda = String.format("state -> { java.util.Map<String, Object> raw = state.humanFeedback().data(); "
					+ "return raw.entrySet().stream()" + ".filter(e -> %s.contains(e.getKey()))"
					+ ".collect(java.util.stream.Collectors.toMap("
					+ "java.util.Map.Entry::getKey, java.util.Map.Entry::getValue)); }", keyListCode);
		}
		else {
			// If don't specify a key to filter, the raw feedback is simply returned
			updateLambda = "state -> state.humanFeedback().data()";
		}

		sb.append("HumanNode ")
			.append(varName)
			.append(" = new HumanNode(")
			.append("\"")
			.append(d.getInterruptStrategy())
			.append("\", ")
			.append(condLambda)
			.append(", ")
			.append(updateLambda)
			.append(");\n");

		sb.append(String.format("stateGraph.addNode(\"%s\", AsyncNodeAction.node_async(%sNode));%n%n", id, varName));

		return sb.toString();
	}

}
