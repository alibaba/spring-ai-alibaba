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

import com.alibaba.cloud.ai.model.VariableSelector;
import com.alibaba.cloud.ai.model.workflow.Node;
import com.alibaba.cloud.ai.model.workflow.NodeType;
import com.alibaba.cloud.ai.model.workflow.nodedata.EndNodeData;
import com.alibaba.cloud.ai.service.generator.workflow.NodeSection;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class EndNodeSection implements NodeSection<EndNodeData> {

	@Override
	public boolean support(NodeType nodeType) {
		return NodeType.END.equals(nodeType);
	}

	@Override
	public String render(Node node, String varName) {
		EndNodeData data = (EndNodeData) node.getData();
		String outputKey = data.getOutputKey();
		List<VariableSelector> selector = data.getInputs();
		String id = node.getId();
		StringBuilder sb = new StringBuilder();
		sb.append("// EndNode [ ").append(id).append(" ]\n");
		// 最终节点用于输出用户选中的变量
		sb.append("stateGraph.addNode(\"")
			.append(varName)
			.append("\", AsyncNodeAction.node_async(")
			.append(String.format("""
					state -> Map.of("%s", Map.of(%s))
					""", outputKey,
					selector.stream()
						.flatMap(v -> Stream.of(String.format("\"%s\"", v.getLabel()),
								String.format("state.value(\"%s\").orElse(\"\")", v.getNameInCode())))
						.collect(Collectors.joining(", "))))
			.append("));");
		return sb.toString();
	}

}
