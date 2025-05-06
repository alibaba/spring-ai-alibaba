package com.alibaba.cloud.ai.mcp.nacos.client.tool;

import com.alibaba.cloud.ai.mcp.nacos.client.transport.LoadbalancedMcpAsyncClient;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.util.ToolUtils;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiPredicate;

/**
 * @author yingzi
 * @date 2025/5/6:14:27
 */
public class LoadbalancedAsyncMcpToolCallbackProvider implements ToolCallbackProvider {

    private final List<LoadbalancedMcpAsyncClient> mcpClients;
    private final BiPredicate<McpAsyncClient, McpSchema.Tool> toolFilter;

    public LoadbalancedAsyncMcpToolCallbackProvider(BiPredicate<McpAsyncClient, McpSchema.Tool> toolFilter, List<LoadbalancedMcpAsyncClient> mcpClients) {
        Assert.notNull(mcpClients, "MCP clients must not be null");
        Assert.notNull(toolFilter, "Tool filter must not be null");
        this.mcpClients = mcpClients;
        this.toolFilter = toolFilter;
    }

    public LoadbalancedAsyncMcpToolCallbackProvider(List<LoadbalancedMcpAsyncClient> mcpClients) {
        this((mcpClient, tool) -> {
            return true;
        }, mcpClients);
    }

    @Override
    public ToolCallback[] getToolCallbacks() {
        List<ToolCallback> toolCallbackList = new ArrayList();
        Iterator var2 = this.mcpClients.iterator();

        while(var2.hasNext()) {
            LoadbalancedMcpAsyncClient mcpClient = (LoadbalancedMcpAsyncClient)var2.next();
            ToolCallback[] toolCallbacks = (ToolCallback[])mcpClient.listTools().map((response) -> {
                return (ToolCallback[])response.tools().stream().filter((tool) -> {
                    return this.toolFilter.test(mcpClient.getMcpAsyncClient(), tool);
                }).map((tool) -> {
                    return new LoadbalancedAsyncMcpToolCallback(mcpClient, tool);
                }).toArray((x$0) -> {
                    return new ToolCallback[x$0];
                });
            }).block();
            this.validateToolCallbacks(toolCallbacks);
            toolCallbackList.addAll(List.of(toolCallbacks));
        }

        return (ToolCallback[])toolCallbackList.toArray(new ToolCallback[0]);
    }

    private void validateToolCallbacks(ToolCallback[] toolCallbacks) {
        List<String> duplicateToolNames = ToolUtils.getDuplicateToolNames(toolCallbacks);
        if (!duplicateToolNames.isEmpty()) {
            throw new IllegalStateException("Multiple tools with the same name (%s)".formatted(String.join(", ", duplicateToolNames)));
        }
    }
}
