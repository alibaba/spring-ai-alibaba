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
package com.alibaba.cloud.ai.service.dsl;

import com.alibaba.cloud.ai.model.Variable;
import com.alibaba.cloud.ai.model.workflow.NodeData;
import com.alibaba.cloud.ai.model.workflow.NodeType;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * NodeDataConverter defined the mutual conversion between specific DSL data and
 * {@link NodeData}
 */
public interface NodeDataConverter<T extends NodeData> {

	/**
	 * Judge if this converter support this node type
	 * @param nodeType {@link NodeType}
	 * @return true if support
	 */
	Boolean supportNodeType(NodeType nodeType);

	/**
	 * Parse DSL data to NodeData
	 * @param data DSL data
	 * @return converted {@link NodeData}
	 */
	T parseMapData(Map<String, Object> data, DSLDialectType dialectType);

	/**
	 * Dump NodeData to DSL map data
	 * @param nodeData {@link NodeData}
	 * @return converted DSL node data <strong>The returned Map must be
	 * modifiable</strong>
	 */
	Map<String, Object> dumpMapData(T nodeData, DSLDialectType dialectType);

	/**
	 * Generate a readable variable name prefix for this node, such as "http1", "llm2",
	 * and so on
	 * @param count
	 * @return friendly varName
	 */
	default String generateVarName(int count) {
		throw new UnsupportedOperationException(getClass().getSimpleName() + " must implement generateVarName");
	}

	/**
	 * After parseMapData is complete and varName is injected, call: Used to override the
	 * default outputKey based on varName and refresh the list of outputs
	 * @param nodeData {@link NodeData}
	 * @param varName
	 */
	default void postProcess(T nodeData, String varName) {
	}

	/**
	 * Fetch the global state variable for this node (usually its list of outputs)
	 * @param nodeData {@link NodeData}
	 * @return Variable stream
	 */
	default Stream<Variable> extractWorkflowVars(T nodeData) {
		List<Variable> outs = nodeData.getOutputs();
		return outs == null ? Stream.empty() : outs.stream();
	}

}
