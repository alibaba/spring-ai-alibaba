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

import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.Node;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.NodeType;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.nodedata.AgentNodeData;
import com.alibaba.cloud.ai.studio.admin.generator.service.generator.workflow.NodeSection;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class AgentNodeSection implements NodeSection<AgentNodeData> {

	@Override
	public boolean support(NodeType nodeType) {
		return NodeType.AGENT.equals(nodeType);
	}

	@Override
	public String render(Node node, String varName) {
		StringBuilder sb = new StringBuilder();
		AgentNodeData nodeData = (AgentNodeData) node.getData();
		sb.append(String.format("// AgentNode [%s] %n", node.getId()));
		sb.append(String.format("AgentNode %s = AgentNode.builder()%n", varName));
		sb.append(String.format(".chatClient(chatClient)%n"));
		sb.append(String.format(".outputKey(\"%s\")%n", nodeData.getOutputs().get(0).getName()));
		if (nodeData.getInstructionPrompt() != null) {
			sb.append(String.format(".systemPrompt(\"%s\")%n", nodeData.getInstructionPrompt()));
		}
		if (nodeData.getQueryPrompt() != null) {
			sb.append(String.format(".userPrompt(\"%s\")%n", nodeData.getQueryPrompt()));
		}
		if (nodeData.getAgentStrategyName() != null) {
			sb.append(String.format(".strategy(AgentNode.Strategy.%s)%n", nodeData.getAgentStrategyName()));
		}
		if (nodeData.getMaxIterations() != null) {
			sb.append(String.format(".maxIterations(%s)%n", nodeData.getMaxIterations()));
		}
		if (nodeData.getToolList() != null && !nodeData.getToolList().isEmpty()) {
			// todo: 对于Dify中与community/tool-call功能类似的工具，可以直接在这里使用而无需用户定义
			String toolCodes = String.format("new ToolCallback[]{%n%s%n}", nodeData.getToolList()
				.stream()
				.map(tool -> String.format(
						"// todo: Implement the ToolCallback for the Dify tool. Tool name: %s, Tool description: %s",
						tool.toolName(), tool.toolDescription()))
				.collect(Collectors.joining(String.format("%n"))));
			sb.append(String.format(".toolCallbacks(%s)%n", toolCodes));
		}
		sb.append(String.format(".build();%n"));
		sb.append(String.format("stateGraph.addNode(\"%s\", AsyncNodeAction.node_async(%s));%n%n", varName, varName));
		return sb.toString();
	}

	@Override
	public List<String> getImports() {
		return List.of("com.alibaba.cloud.ai.graph.node.AgentNode", "org.springframework.ai.tool.ToolCallback");
	}

}
