package com.alibaba.cloud.ai.mcp.nacos.client.tool;

import com.alibaba.cloud.ai.mcp.nacos.client.transport.LoadbalancedMcpSyncClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.util.ToolUtils;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

/**
 * @author yingzi
 * @date 2025/5/6:13:47
 */
public class LoadbalancedSyncMcpToolCallbackProvider implements ToolCallbackProvider {

    private final List<LoadbalancedMcpSyncClient> mcpClients;
    private final BiPredicate<McpSyncClient, McpSchema.Tool> toolFilter;

    public LoadbalancedSyncMcpToolCallbackProvider(BiPredicate<McpSyncClient, McpSchema.Tool> toolFilter, List<LoadbalancedMcpSyncClient> mcpClients) {
        Assert.notNull(mcpClients, "MCP clients must not be null");
        Assert.notNull(toolFilter, "Tool filter must not be null");
        this.mcpClients = mcpClients;
        this.toolFilter = toolFilter;
    }

    public LoadbalancedSyncMcpToolCallbackProvider(List<LoadbalancedMcpSyncClient> mcpClients) {
        this((mcpClient, tool) -> {
            return true;
        }, mcpClients);
    }

    @Override
    public ToolCallback[] getToolCallbacks() {
        ArrayList<Object> toolCallbacks = new ArrayList();
        this.mcpClients.stream().forEach((mcpClient) -> {
            toolCallbacks.addAll(mcpClient.listTools().tools().stream().filter((tool) -> {
                return this.toolFilter.test(mcpClient.getMcpSyncClient(), tool);
            }).map((tool) -> {
                return new LoadbalancedSyncMcpToolCallback(mcpClient, tool);
            }).toList());
        });
        ToolCallback[] array = (ToolCallback[])toolCallbacks.toArray(new ToolCallback[0]);
        this.validateToolCallbacks(array);
        return array;
    }

    private void validateToolCallbacks(ToolCallback[] toolCallbacks) {
        List<String> duplicateToolNames = ToolUtils.getDuplicateToolNames(toolCallbacks);
        if (!duplicateToolNames.isEmpty()) {
            throw new IllegalStateException("Multiple tools with the same name (%s)".formatted(String.join(", ", duplicateToolNames)));
        }
    }
}
