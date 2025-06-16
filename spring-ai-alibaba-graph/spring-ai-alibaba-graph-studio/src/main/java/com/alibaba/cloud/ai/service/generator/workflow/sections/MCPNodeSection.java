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
import com.alibaba.cloud.ai.model.workflow.nodedata.MCPNodeData;
import com.alibaba.cloud.ai.service.generator.workflow.NodeSection;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class MCPNodeSection implements NodeSection {

	@Override
	public boolean support(NodeType nodeType) {
		return NodeType.MCP.equals(nodeType);
	}

	@Override
	public String render(Node node, String varName) {
		MCPNodeData d = (MCPNodeData) node.getData();
		String id = node.getId();
		StringBuilder sb = new StringBuilder();

		sb.append(String.format("// —— McpNode [%s] ——%n", id));
		sb.append(String.format("McpNode %s = McpNode.builder()%n", varName));

		if (d.getUrl() != null) {
			sb.append(String.format(".url(\"%s\")%n", escape(d.getUrl())));
		}

		if (d.getTool() != null) {
			sb.append(String.format(".tool(\"%s\")%n", escape(d.getTool())));
		}

		Map<String, String> headers = d.getHeaders();
		if (headers != null) {
			for (Map.Entry<String, String> entry : headers.entrySet()) {
				sb.append(String.format(".header(\"%s\", \"%s\")%n", escape(entry.getKey()), escape(entry.getValue())));
			}
		}

		Map<String, Object> params = d.getParams();
		if (params != null) {
			for (Map.Entry<String, Object> entry : params.entrySet()) {
				Object val = entry.getValue();
				String valLiteral;
				if (val instanceof String) {
					valLiteral = String.format("\"%s\"", escape((String) val));
				}
				else {
					valLiteral = String.valueOf(val);
				}
				sb.append(String.format(".param(\"%s\", %s)%n", escape(entry.getKey()), valLiteral));
			}
		}

		if (d.getOutputKey() != null) {
			sb.append(String.format(".outputKey(\"%s\")%n", escape(d.getOutputKey())));
		}

		List<String> ipk = d.getInputParamKeys();
		if (ipk != null && !ipk.isEmpty()) {
			String joined = ipk.stream().map(this::escape).map(s -> "\"" + s + "\"").collect(Collectors.joining(", "));
			sb.append(String.format(".inputParamKeys(List.of(%s))%n", joined));
		}

		sb.append(".build();\n");
		sb.append(String.format("stateGraph.addNode(\"%s\", AsyncNodeAction.node_async(%s));%n%n", id, varName));

		return sb.toString();
	}

}
