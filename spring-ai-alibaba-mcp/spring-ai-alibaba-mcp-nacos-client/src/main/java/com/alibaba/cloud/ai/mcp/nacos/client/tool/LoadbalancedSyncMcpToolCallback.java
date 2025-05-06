package com.alibaba.cloud.ai.mcp.nacos.client.tool;

import com.alibaba.cloud.ai.mcp.nacos.client.transport.LoadbalancedMcpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.Map;

/**
 * @author yingzi
 * @date 2025/5/6:14:15
 */
public class LoadbalancedSyncMcpToolCallback implements ToolCallback {
    private final LoadbalancedMcpSyncClient mcpClient;
    private final McpSchema.Tool tool;

    public LoadbalancedSyncMcpToolCallback(LoadbalancedMcpSyncClient mcpClient, McpSchema.Tool tool) {
        this.mcpClient = mcpClient;
        this.tool = tool;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder().name(McpToolUtils.prefixedToolName(this.mcpClient.getClientInfo().name(), this.tool.name())).description(this.tool.description()).inputSchema(ModelOptionsUtils.toJsonString(this.tool.inputSchema())).build();
    }

    @Override
    public String call(String functionInput) {
        Map<String, Object> arguments = ModelOptionsUtils.jsonToMap(functionInput);
        McpSchema.CallToolResult response = this.mcpClient.callTool(new McpSchema.CallToolRequest(this.tool.name(), arguments));
        if (response.isError() != null && response.isError()) {
            throw new IllegalStateException("Error calling tool: " + String.valueOf(response.content()));
        } else {
            return ModelOptionsUtils.toJsonString(response.content());
        }
    }

    public String call(String toolArguments, ToolContext toolContext) {
        return this.call(toolArguments);
    }
}
