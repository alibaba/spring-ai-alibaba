package com.alibaba.cloud.ai.mcp.nacos.service;

import com.alibaba.nacos.api.ai.model.mcp.McpServerDetailInfo;

/**
 * @author Sunrisea
 */
public interface NacosMcpSubscriber {

	/**
	 * Receive McpServerDetailInfo from Nacos server.
	 * @param mcpServerDetailInfo the mcp server detail info
	 */
	void receive(McpServerDetailInfo mcpServerDetailInfo);

}
