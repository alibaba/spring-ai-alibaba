package com.alibaba.cloud.ai.service.dsl;

import com.alibaba.cloud.ai.model.workflow.NodeData;

import java.util.Map;

/**
 * NodeDataConverter defined the mutual conversion between specific DSL data and
 * {@link NodeData}
 */
public interface NodeDataConverter<T extends NodeData> {

	/**
	 * Judge if this converter support this node type
	 * @param nodeType {@link com.alibaba.cloud.ai.model.workflow.NodeType} value
	 * @return true if support
	 */
	Boolean supportType(String nodeType);

	/**
	 * Parse DSL data to NodeData
	 * @param data DSL data
	 * @return converted {@link NodeData}
	 */
	T parseDifyData(Map<String, Object> data);

	/**
	 * Dump NodeData to DSL data
	 * @param nodeData {@link NodeData}
	 * @return converted DSL data
	 */
	Map<String, Object> dumpDifyData(T nodeData);

}
