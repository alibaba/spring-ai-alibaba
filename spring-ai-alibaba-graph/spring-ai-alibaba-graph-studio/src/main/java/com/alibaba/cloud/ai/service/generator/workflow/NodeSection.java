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
package com.alibaba.cloud.ai.service.generator.workflow;

import com.alibaba.cloud.ai.model.workflow.Edge;
import com.alibaba.cloud.ai.model.workflow.Node;
import com.alibaba.cloud.ai.model.workflow.NodeData;
import com.alibaba.cloud.ai.model.workflow.NodeType;

import java.util.List;
import java.util.Map;

/**
 * Render a node data
 *
 * @author robocanic
 * @since 2025/5/23
 */
public interface NodeSection<T extends NodeData> {

	boolean support(NodeType nodeType);

	String render(Node node, String varName);

	default String escape(String input) {
		if (input == null) {
			return "";
		}
		return input.replace("\\", "\\\\")
			.replace("\"", "\\\"")
			.replace("\n", "\\n")
			.replace("\r", "\\r")
			.replace("\t", "\\t");
	}

	// todo: 完善条件边的EdgeAction
	/**
	 * 生成条件边的SAA代码
	 * @param nodeData 节点数据
	 * @param nodeMap nodeId与node的映射
	 * @param entry 包含当前节点ID与当前节点出发的条件边List
	 * @param varNames nodeId与nodeVarName的映射
	 * @return 条件边代码
	 */
	default String renderConditionalEdges(T nodeData, Map<String, Node> nodeMap, Map.Entry<String, List<Edge>> entry,
			Map<String, String> varNames) {
		System.err.println("Unsupported Conditional Edges!");
		return "";
	}

}
