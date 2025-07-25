/*
 * Copyright 2024-2026 the original author or authors.
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
import com.alibaba.cloud.ai.model.workflow.nodedata.AssignerNodeData;
import com.alibaba.cloud.ai.service.generator.workflow.NodeSection;
import org.springframework.stereotype.Component;

@Component
public class AssignerNodeSection implements NodeSection {

	@Override
	public boolean support(NodeType nodeType) {
		return NodeType.ASSIGNER.equals(nodeType);
	}

	@Override
	public String render(Node node, String varName) {
		AssignerNodeData data = (AssignerNodeData) node.getData();
		String id = node.getId();

		StringBuilder sb = new StringBuilder();
		sb.append(String.format("// —— AssignerNode [%s] ——%n", id));
		sb.append(String.format("AssignerNode %s = AssignerNode.builder()%n", varName));
		for (AssignerNodeData.AssignerItem item : data.getItems()) {
			String targetKey = item.getVariableSelector() != null && !item.getVariableSelector().isEmpty()
					? item.getVariableSelector().get(item.getVariableSelector().size() - 1) : "target";
			String inputKey = (item.getValue() != null && item.getValue().size() > 1) ? item.getValue().get(1) : null;
			String writeMode = item.getWriteMode() != null ? item.getWriteMode().toUpperCase().replace("-", "_")
					: "OVER_WRITE";
			sb.append(String.format(".addItem(\"%s\", %s, AssignerNode.WriteMode.%s)%n", targetKey,
					inputKey == null ? "null" : "\"" + inputKey + "\"", writeMode));
		}
		sb.append(".build();\n");
		sb.append(String.format("stateGraph.addNode(\"%s\", AsyncNodeAction.node_async(%s));%n%n", varName, varName));
		return sb.toString();
	}

}
