package com.alibaba.cloud.ai.service.dsl;

import com.alibaba.cloud.ai.model.workflow.NodeData;
import com.alibaba.cloud.ai.model.workflow.NodeType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

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

}
