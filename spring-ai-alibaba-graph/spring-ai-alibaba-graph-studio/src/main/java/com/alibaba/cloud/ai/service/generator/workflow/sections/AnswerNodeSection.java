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
import com.alibaba.cloud.ai.model.workflow.nodedata.AnswerNodeData;
import com.alibaba.cloud.ai.service.generator.workflow.NodeSection;
import org.springframework.stereotype.Component;

@Component
public class AnswerNodeSection implements NodeSection {

	@Override
	public boolean support(NodeType nodeType) {
		return NodeType.ANSWER.equals(nodeType);
	}

	@Override
	public String render(Node node, String varName) {
		AnswerNodeData d = (AnswerNodeData) node.getData();
		String id = node.getId();
		StringBuilder sb = new StringBuilder();

		sb.append("// —— AnswerNode [").append(id).append("] ——\n");
		sb.append("AnswerNode ").append(varName).append(" = AnswerNode.builder()\n");

		if (d.getAnswer() != null) {
			sb.append(".answer(\"").append(escape(d.getAnswer())).append("\")\n");
		}

		sb.append(".build();\n");
		sb.append("stateGraph.addNode(\"")
			.append(id)
			.append("\", AsyncNodeAction.node_async(")
			.append(varName)
			.append("));\n\n");

		return sb.toString();
	}

}
