package com.alibaba.cloud.ai.service.generator;

import com.alibaba.cloud.ai.model.workflow.NodeType;

import java.util.Map;

/**
 * CodeGenerator abstracts the code generation of a specific node
 */
public interface CodeGenerator {

	/**
	 * whether the node type is supported
	 * @param nodeType {@link NodeType}
	 * @return true if supported
	 */
	Boolean supportNodeType(String nodeType);

	/**
	 * generate code
	 * @param nodeData node properties
	 * @return code string
	 */
	String generateCode(Map<String, Object> nodeData);

}
