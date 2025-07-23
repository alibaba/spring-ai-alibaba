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
import com.alibaba.cloud.ai.model.workflow.nodedata.IterationNodeData;
import com.alibaba.cloud.ai.service.generator.workflow.NodeSection;
import org.springframework.stereotype.Component;

/**
 * @author vlsmb
 * @since 2025/7/23
 */
@Component
public class IterationNodeSection implements NodeSection {

	@Override
	public boolean support(NodeType nodeType) {
		return nodeType.equals(NodeType.ITERATION);
	}

	@Override
	public String render(Node node, String varName) {
		// 构建Iteration.Start -> Iteration -> Iteration.End节点
		IterationNodeData data = (IterationNodeData) node.getData();
		StringBuilder sb = new StringBuilder();

		// 获取输入输出的泛型
		String inputType = "Map<String, Object>";
		String outputType = "Map<String, Object>";
		if (data.getInputType().equalsIgnoreCase("string")) {
			inputType = "String";
		}
		else if (data.getInputType().equalsIgnoreCase("number")) {
			inputType = "Number";
		}
		if (data.getOutputType().equalsIgnoreCase("string")) {
			outputType = "String";
		}
		if (data.getOutputType().equalsIgnoreCase("number")) {
			outputType = "Number";
		}

		// 获取节点ID、迭代起始ID、迭代终止ID
		String id = data.getId();

		sb.append("// —— IterationNode [").append(id).append("] ——\n\n");
		sb.append("// IterationNode.Start \n");
		sb.append("stateGraph.addNode(\"")
			.append(id)
			.append("\", AsyncNodeAction.node_async(\n")
			.append("IterationNode.<")
			.append(inputType)
			.append(">start()\n")
			.append(".inputArrayJsonKey(")
			.append("\"?\"")
			.append(")\n")
			.append(".inputArrayKey(")
			.append("\"?\"")
			.append(")\n")
			.append(".outputItemKey(")
			.append("\"?\"")
			.append(")\n")
			.append(".outputStartIterationKey(")
			.append("\"?\"")
			.append(")\n")
			.append(".build()))\n")
			.append("// IterationNode.End \n")
			.append(".addNode(\"")
			.append(id)
			.append("_end")
			.append("\", AsyncNodeAction.node_async(\n")
			.append("IterationNode.<")
			.append(inputType)
			.append(", ")
			.append(outputType)
			.append(">end()\n")
			.append(".inputArrayKey(")
			.append("\"?\"")
			.append(")\n")
			.append(".inputResultKey(")
			.append("\"?\"")
			.append(")\n")
			.append(".outputArrayKey(")
			.append("\"?\"")
			.append(")\n")
			.append(".outputContinueIterationKey(")
			.append("\"?\"")
			.append(")\n")
			.append(".build()))\n")
			.append("// IterationNode.OutNode \n")
			.append(".addNode(\"")
			.append(id)
			.append("_out")
			.append("\", AsyncNodeAction.node_async((OverAllState state) -> Map.of()))\n")
			.append("// Start Conditional Edge \n")
			.append(".addConditionalEdges(\"")
			.append(id)
			.append("\", \n")
			.append("AsyncEdgeAction.edge_async(\n")
			.append("(OverAllState state) -> state.value(")
			.append("\"?st_flag\"")
			.append(", Boolean.class).orElse(false) ? \"true\" : \"false\"),\n")
			.append("Map.of(\"true\", \"iteration\", \"false\", \"")
			.append(id)
			.append("_end")
			.append("\"))")
			.append("// End Conditional Edge \n")
			.append(".addConditionalEdges(\"")
			.append(id)
			.append("_end")
			.append("\", \n")
			.append("AsyncEdgeAction.edge_async(\n")
			.append("(OverAllState state) -> state.value(")
			.append("\"?end_flag\"")
			.append(", Boolean.class).orElse(false) ? \"true\" : \"false\"),\n")
			.append("Map.of(\"true\", \"")
			.append(id)
			.append("\", \"false\", \"")
			.append(id)
			.append("_end")
			.append("\"));\n\n");
		return sb.toString();
	}

}
