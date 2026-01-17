/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.sandbox.tools.mcp;

import com.alibaba.cloud.ai.sandbox.BaseSandboxAwareTool;
import com.alibaba.cloud.ai.sandbox.RuntimeFunctionToolCallback;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.runtime.sandbox.manager.SandboxService;
import io.agentscope.runtime.sandbox.tools.MCPTool;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.metadata.ToolMetadata;

import java.util.Map;

public class SaaMCPTool extends BaseSandboxAwareTool<MCPTool, Map<String, Object>, SaaMCPTool.MCPToolResponse> {
    public SaaMCPTool(String name, String toolType, String description,
                      Map<String, Object> schema, Map<String, Object> serverConfigs,
                      String sandboxType, SandboxService sandboxManager) {
        super(new MCPTool(
                name,
                toolType,
                description,
                schema,
                serverConfigs,
                sandboxType,
                sandboxManager
        ));
    }

    public SaaMCPTool(MCPTool mcpTool) {
        super(mcpTool);
    }

    @Override
    public MCPToolResponse apply(Map<String, Object> arguments, ToolContext toolContext) {
        try {
            String result = sandboxTool.executeMCPTool(arguments);
            return new MCPToolResponse(result, "MCP tool execution completed");
        } catch (Exception e) {
            logger.error("MCP tool execution error: {}", e.getMessage());
            return new MCPToolResponse(
                    "Error",
                    "MCP tool execution error: " + e.getMessage()
            );
        }
    }

    public record MCPToolResponse(String result, String message) {
        public MCPToolResponse(String result, String message) {
            this.result = result;
            this.message = message;
        }
    }

    public RuntimeFunctionToolCallback<?, ?> buildTool() {
        ObjectMapper mapper = new ObjectMapper();
        String inputSchema = "";
        try {
            inputSchema = mapper.writeValueAsString(sandboxTool.getSchema());
        } catch (Exception e) {
            logger.error("Failed to serialize schema: {}", e.getMessage());
        }

        return RuntimeFunctionToolCallback
                .builder(
                        sandboxTool.getName(),
                        this
                )
                .description(sandboxTool.getDescription())
                .inputSchema(inputSchema)
                .inputType(Map.class)
                .toolMetadata(ToolMetadata.builder().returnDirect(false).build())
                .build();
    }

}
