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
import com.alibaba.cloud.ai.model.workflow.nodedata.ToolNodeData;
import com.alibaba.cloud.ai.service.generator.workflow.NodeSection;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ToolNodeSection implements NodeSection {

	@Override
	public boolean support(NodeType nodeType) {
		return NodeType.TOOL.equals(nodeType);
	}

	@Override
	public String render(Node node, String varName) {
		ToolNodeData d = (ToolNodeData) node.getData();
		String id = node.getId();
		StringBuilder sb = new StringBuilder();

		sb.append(String.format("// —— ToolNode [%s] ——%n", id));
		sb.append(String.format("ToolNode %s = ToolNode.builder()%n", varName));

		if (d.getLlmResponseKey() != null) {
			sb.append(String.format(".llmResponseKey(\"%s\")%n", escape(d.getLlmResponseKey())));
		}

		if (d.getOutputKey() != null) {
			sb.append(String.format(".outputKey(\"%s\")%n", escape(d.getOutputKey())));
		}

		List<String> names = d.getToolNames();
		if (names != null && !names.isEmpty()) {
			String joined = names.stream()
				.map(this::escape)
				.map(s -> "\"" + s + "\"")
				.collect(Collectors.joining(", "));
			sb.append(String.format(".toolNames(List.of(%s))%n", joined));
		}

		List<String> callbacks = d.getToolCallbacks();
		if (callbacks != null && !callbacks.isEmpty()) {
			String joined = callbacks.stream()
				.map(this::escape)
				.map(s -> "\"" + s + "\"")
				.collect(Collectors.joining(", "));
			sb.append(String.format(".toolCallbacks(List.of(%s))%n", joined));
		}

		sb.append(".toolCallbackResolver(toolCallbackResolver)\n");

		sb.append(".build();\n");
		sb.append(String.format("stateGraph.addNode(\"%s\", AsyncNodeAction.node_async(%s));%n%n", id, varName));

		return sb.toString();
	}

}
