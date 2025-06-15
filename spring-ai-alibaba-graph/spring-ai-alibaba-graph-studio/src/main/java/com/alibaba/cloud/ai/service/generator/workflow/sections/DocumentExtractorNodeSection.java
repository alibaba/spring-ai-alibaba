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
import com.alibaba.cloud.ai.model.workflow.nodedata.DocumentExtractorNodeData;
import com.alibaba.cloud.ai.service.generator.workflow.NodeSection;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class DocumentExtractorNodeSection implements NodeSection {

	@Override
	public boolean support(NodeType nodeType) {
		return NodeType.DOC_EXTRACTOR.equals(nodeType);
	}

	@Override
	public String render(Node node, String varName) {
		DocumentExtractorNodeData data = (DocumentExtractorNodeData) node.getData();
		String id = node.getId();

		StringBuilder sb = new StringBuilder();
		sb.append(String.format("// —— DocumentExtractorNode [%s] ——%n", id));
		sb.append(String.format("DocumentExtractorNode %s = DocumentExtractorNode.builder()%n", varName));

		List<String> fileList = data.getFileList();
		if (fileList != null && !fileList.isEmpty()) {
			String joined = fileList.stream().map(f -> "\"" + escape(f) + "\"").collect(Collectors.joining(", "));
			sb.append(String.format(".fileList(List.of(%s))%n", joined));
		}

		List<com.alibaba.cloud.ai.model.VariableSelector> inputs = data.getInputs();
		if (inputs != null && !inputs.isEmpty()) {
			// Take the name of the first VariableSelector as the paramsKey
			String key = inputs.get(0).getName();
			sb.append(String.format(".paramsKey(\"%s\")%n", escape(key)));
		}

		String outputKey = data.getOutputKey();
		sb.append(String.format(".outputKey(\"%s\")%n", escape(outputKey)));

		sb.append(".build();\n");
		sb.append(String.format("stateGraph.addNode(\"%s\", AsyncNodeAction.node_async(%s));%n%n", id, varName));

		return sb.toString();
	}

}
