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
import com.alibaba.cloud.ai.model.workflow.nodedata.EmptyNodeData;
import com.alibaba.cloud.ai.service.generator.workflow.NodeSection;
import org.springframework.stereotype.Component;

/**
 * @author vlsmb
 * @since 2025/7/23
 */
@Component
public class EmptyNodeSection implements NodeSection {

	@Override
	public boolean support(NodeType nodeType) {
		return nodeType.equals(NodeType.DIFY_ITERATION_START);
	}

	@Override
	public String render(Node node, String varName) {
		EmptyNodeData data = (EmptyNodeData) node.getData();
		StringBuilder sb = new StringBuilder();
		String id = data.getId();
		sb.append("// —— Empty Node [").append(id).append("] ——\n");
		sb.append("stateGraph.addNode(\"")
			.append(varName)
			.append("\", AsyncNodeAction.node_async((OverAllState state) -> Map.of()));\n\n");
		return sb.toString();
	}

}
