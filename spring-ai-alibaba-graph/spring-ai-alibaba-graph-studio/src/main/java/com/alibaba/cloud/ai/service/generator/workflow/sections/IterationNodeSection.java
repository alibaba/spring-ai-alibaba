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

		sb.append("// —— IterationNode [").append(data.getId()).append("] ——\n");
		sb.append("IterationNode.<")
			.append(inputType)
			.append(", ")
			.append(outputType)
			.append(">converter()\n")
			.append(".subGraphStartNodeName(\"")
			.append(data.getStartNodeName())
			.append("\")\n")
			.append(".subGraphEndNodeName(\"")
			.append(data.getEndNodeName())
			.append("\")\n")
			.append(".tempArrayKey(\"")
			.append(data.getInnerArrayKey())
			.append("\")\n")
			.append(".tempStartFlagKey(\"")
			.append(data.getInnerStartFlagKey())
			.append("\")\n")
			.append(".tempEndFlagKey(\"")
			.append(data.getInnerEndFlagKey())
			.append("\")\n")
			.append(".tempIndexKey(\"")
			.append(data.getInnerIndexKey())
			.append("\")\n")
			.append(".iteratorItemKey(\"")
			.append(data.getInnerItemKey())
			.append("\")\n")
			.append(".iteratorResultKey(\"")
			.append(data.getInnerItemResultKey())
			.append("\")\n")
			.append(".inputArrayJsonKey(\"")
			.append(data.getInputKey())
			.append("\")\n")
			.append(".outputArrayJsonKey(\"")
			.append(data.getOutputKey())
			.append("\")\n")
			.append(".appendToStateGraph(stateGraph, \"")
			.append(varName)
			.append("\", \"")
			.append(varName)
			.append("_out\");\n\n");
		return sb.toString();
	}

}
