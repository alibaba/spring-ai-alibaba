package com.alibaba.cloud.ai.mcp.nacos.service.model;

import com.alibaba.nacos.api.ai.model.mcp.McpEndpointInfo;

import java.util.List;

public class NacosMcpServerEndpoint {
    
    private List<McpEndpointInfo> mcpEndpointInfoList;
    
    private String exportPath;
    
    private String protocol;
    
    private String version;
    
    public NacosMcpServerEndpoint(List<McpEndpointInfo> mcpEndpointInfoList, String exportPath, String protocol,
            String version) {
        this.mcpEndpointInfoList = mcpEndpointInfoList;
        this.exportPath = exportPath;
        this.protocol = protocol;
        this.version = version;
    }
    
    public List<McpEndpointInfo> getMcpEndpointInfoList() {
        return mcpEndpointInfoList;
    }
    
    public void setMcpEndpointInfoList(List<McpEndpointInfo> mcpEndpointInfoList) {
        this.mcpEndpointInfoList = mcpEndpointInfoList;
    }
    
    public String getExportPath() {
        return exportPath;
    }
    
    public void setExportPath(String exportPath) {
        this.exportPath = exportPath;
    }
    
    public String getProtocol() {
        return protocol;
    }
    
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
}