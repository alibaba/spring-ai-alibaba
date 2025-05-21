package com.alibaba.cloud.ai.mcp.nacos.service;

import com.alibaba.nacos.api.ai.model.mcp.McpServerDetailInfo;

/**
 * @author Sunrisea
 */
public interface NacosMcpSubscriber {
    
    void receive(McpServerDetailInfo mcpServerDetailInfo);

}
