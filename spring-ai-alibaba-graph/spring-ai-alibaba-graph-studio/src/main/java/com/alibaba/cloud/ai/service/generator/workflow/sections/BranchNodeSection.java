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
import com.alibaba.cloud.ai.model.workflow.nodedata.BranchNodeData;
import com.alibaba.cloud.ai.service.generator.workflow.NodeSection;
import org.springframework.stereotype.Component;

@Component
public class BranchNodeSection implements NodeSection {

	@Override
	public boolean support(NodeType nodeType) {
		return NodeType.BRANCH.equals(nodeType);
	}

	@Override
	public String render(Node node, String varName) {
		BranchNodeData branchNode = (BranchNodeData) node.getData();
		String id = node.getId();

		String ouputKey = id + "_output";

		String inputKey = "";
		if (!branchNode.getCases().isEmpty()) {
			inputKey = branchNode.getCases().get(0).getConditions().get(0).getVariableSelector().getName();
		}

		StringBuilder sb = new StringBuilder();
		sb.append(String.format("// —— BranchNode [%s] ——%n", id));
		sb.append(String.format("BranchNode %s = BranchNode.builder()\n", varName));

		sb.append(String.format("    .inputKey(\"%s\")\n", inputKey));
		sb.append(String.format("    .outputKey(\"%s\")\n", ouputKey));
		sb.append(String.format("    .build();\n"));

		sb.append(String.format("stateGraph.addNode(\"%s\", AsyncNodeAction.node_async(%s));%n%n", id, varName));

		return sb.toString();
	}

}
