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

import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.Edge;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.Node;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.NodeType;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.nodedata.StartNodeData;
import com.alibaba.cloud.ai.studio.admin.generator.service.generator.workflow.NodeSection;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StartNodeSection implements NodeSection<StartNodeData> {

	@Override
	public boolean support(NodeType nodeType) {
		return NodeType.START.equals(nodeType);
	}

	// use static constant of stateGraph
	@Override
	public String render(Node node, String varName) {
		return "";
	}

	@Override
	public String renderEdges(StartNodeData nodeData, List<Edge> edges) {
		// 开始节点的Source应为StateGraph.START
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("// Edges For [START]%n"));
		if (edges.isEmpty()) {
			return "";
		}
		sb.append(String.format("stateGraph%n"));
		edges.forEach(edge -> sb.append(String.format(".addEdge(START, \"%s\")%n", edge.getTarget())));
		sb.append(String.format(";%n%n"));
		return sb.toString();
	}

	@Override
	public List<String> getImports() {
		return List.of();
	}

}
