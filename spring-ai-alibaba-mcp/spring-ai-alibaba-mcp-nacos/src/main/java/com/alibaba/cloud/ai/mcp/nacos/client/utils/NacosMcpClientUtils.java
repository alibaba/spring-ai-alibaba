package com.alibaba.cloud.ai.mcp.nacos.client.utils;

import com.alibaba.nacos.api.ai.model.mcp.McpEndpointInfo;

public class NacosMcpClientUtils {
    
    public static String getMcpEndpointInfoId(McpEndpointInfo mcpEndpointInfo, String exportPath){
        return mcpEndpointInfo.getAddress() + "@@" + mcpEndpointInfo.getPort() + "@@"+ exportPath;
    }
    
}
