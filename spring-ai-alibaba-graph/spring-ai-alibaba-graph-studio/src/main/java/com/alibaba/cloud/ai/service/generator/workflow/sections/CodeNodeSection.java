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
import com.alibaba.cloud.ai.model.workflow.nodedata.CodeNodeData;
import com.alibaba.cloud.ai.service.generator.workflow.NodeSection;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class CodeNodeSection implements NodeSection {

	@Override
	public boolean support(NodeType nodeType) {
		return NodeType.CODE.equals(nodeType);
	}

	@Override
	public String render(Node node, String varName) {
		CodeNodeData data = (CodeNodeData) node.getData();
		StringBuilder sb = new StringBuilder();
		String id = node.getId();

		sb.append(String.format("// —— CodeNode [%s] ——%n", id));

		sb.append(String.format("CodeExecutorNodeAction %s = CodeExecutorNodeAction.builder()%n", varName));

		sb.append("    .codeExecutor(codeExecutor)  // 注入的 CodeExecutor Bean\n");

		sb.append(String.format("    .codeLanguage(\"%s\")%n", data.getCodeLanguage()));

		String escaped = data.getCode().replace("\\", "\\\\").replace("\"\"\"", "\\\"\\\"\\\"");
		sb.append("    .code(\"\"\"\n").append(escaped).append("\n\"\"\")\n");

		sb.append("    .config(codeExecutionConfig)  // 注入的 CodeExecutionConfig Bean\n");

		if (!data.getInputs().isEmpty()) {
			String params = data.getInputs()
				.stream()
				.map(sel -> String.format("\"%s\", \"%s\"", sel.getLabel(), sel.getName()))
				.collect(Collectors.joining(", "));
			sb.append(String.format("    .params(Map.of(%s))%n", params));
		}
		if (data.getOutputKey() != null) {
			sb.append(String.format(".outputKey(\"%s\")%n", escape(data.getOutputKey())));
		}

		sb.append("    .build();\n");

		sb.append(String.format("stateGraph.addNode(\"%s\", AsyncNodeAction.node_async(%s));%n%n", varName, varName));

		return sb.toString();
	}

}
