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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.Node;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.NodeType;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.nodedata.ParameterParsingNodeData;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.DSLDialectType;
import com.alibaba.cloud.ai.studio.admin.generator.service.generator.workflow.NodeSection;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
public class ParameterParsingNodeSection implements NodeSection<ParameterParsingNodeData> {

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

		List<Map<String, Object>> params = d.getParameters();
		if (!CollectionUtils.isEmpty(params)) {
			String joined = params.stream().map(m -> {
				String mapCode = Stream
					.of("name", m.getOrDefault("name", "unknown").toString(), "type",
							m.getOrDefault("type", "string").toString(), "description",
							m.getOrDefault("description", "").toString())
					.map(s -> "\"" + s + "\"")
					.collect(Collectors.joining(", "));
				return String.format("Map.of(%s)", mapCode);
			}).collect(Collectors.joining(", "));
			sb.append(String.format(".parameters(List.of(%s))%n", joined));
		}

		if (d.getOutputKey() != null) {
			sb.append(String.format(".outputKey(\"%s\")%n", escape(d.getOutputKey())));
		}

		sb.append(".build();\n");

		// 辅助节点
		String assistNodeCode = String.format("wrapperParameterNodeAction(%s, \"%s\", \"%s\")", varName, varName,
				d.getOutputKey());

		sb.append(String.format("stateGraph.addNode(\"%s\", AsyncNodeAction.node_async(%s));%n%n", varName,
				assistNodeCode));

		return sb.toString();
	}

	@Override
	public String assistMethodCode(DSLDialectType dialectType) {
		return switch (dialectType) {
			case DIFY ->
				"""
						private NodeAction wrapperParameterNodeAction(NodeAction nodeAction, String nodeName, String key) {
						    return (state) -> {
						        Map<String, Object> result = nodeAction.apply(state);
						        Object object = result.get(key);
						        if(!(object instanceof Map<?,?> map)) {
						            return Map.of();
						        }
						        return map.entrySet().stream().collect(Collectors.toMap(e -> nodeName + "_" + e.getKey(), Map.Entry::getValue));
						    };
						}
						""";
			default -> "";
		};
	}

	@Override
	public List<String> getImports() {
		return List.of("com.alibaba.cloud.ai.graph.node.ParameterParsingNode", "java.util.stream.Collectors");
	}

}
