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
import com.alibaba.cloud.ai.model.workflow.nodedata.ParameterParsingNodeData;
import com.alibaba.cloud.ai.service.generator.workflow.NodeSection;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ParameterParsingNodeSection implements NodeSection {

	@Override
	public boolean support(NodeType nodeType) {
		return NodeType.PARAMETER_PARSING.equals(nodeType);
	}

	@Override
	public String render(Node node, String varName) {
		ParameterParsingNodeData d = (ParameterParsingNodeData) node.getData();
		String id = node.getId();
		StringBuilder sb = new StringBuilder();

		sb.append(String.format("// —— ParameterParsingNode [%s] ——%n", id));
		sb.append(String.format("ParameterParsingNode %s = ParameterParsingNode.builder()%n", varName));

		if (d.getInputTextKey() != null) {
			sb.append(String.format(".inputTextKey(\"%s\")%n", escape(d.getInputTextKey())));
		}

		sb.append(".chatClient(chatClient)\n");

		List<Map<String, String>> params = d.getParameters();
		if (params != null && !params.isEmpty()) {
			String joined = params.stream().map(m -> {
				String entries = m.entrySet()
					.stream()
					.map(e -> String.format("\"%s\", \"%s\"", escape(e.getKey()), escape(e.getValue())))
					.collect(Collectors.joining(", "));
				return String.format("Map.of(%s)", entries);
			}).collect(Collectors.joining(", "));
			sb.append(String.format(".parameters(List.of(%s))%n", joined));
		}

		if (d.getOutputKey() != null) {
			sb.append(String.format(".outputKey(\"%s\")%n", escape(d.getOutputKey())));
		}

		sb.append(".build();\n");
		sb.append(String.format("stateGraph.addNode(\"%s\", AsyncNodeAction.node_async(%s));%n%n", id, varName));

		return sb.toString();
	}

}
