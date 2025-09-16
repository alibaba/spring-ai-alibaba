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

package com.alibaba.cloud.ai.studio.admin.generator.service.generator.workflow.sections;

import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.Node;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.NodeType;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.nodedata.AssignerNodeData;
import com.alibaba.cloud.ai.studio.admin.generator.service.generator.workflow.NodeSection;

import com.alibaba.cloud.ai.studio.admin.generator.utils.ObjectToCodeUtil;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AssignerNodeSection implements NodeSection<AssignerNodeData> {

	@Override
	public boolean support(NodeType nodeType) {
		return NodeType.ASSIGNER.equals(nodeType);
	}

	@Override
	public String render(Node node, String varName) {
		AssignerNodeData nodeData = ((AssignerNodeData) node.getData());
		return String.format("""
				// —— AssignerNode [%s] ——
				AssignerNode %s = AssignerNode.builder()
				                    .setItems(%s)
				                    .build();
				stateGraph.addNode("%s", AsyncNodeAction.node_async(%s));

				""", node.getId(), varName, ObjectToCodeUtil.toCode(nodeData.getItems()), varName, varName);
	}

	@Override
	public List<String> getImports() {
		return List.of("com.alibaba.cloud.ai.graph.node.AssignerNode");
	}

}
