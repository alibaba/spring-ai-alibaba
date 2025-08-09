/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.example.manus.coordinator.server;

import com.alibaba.cloud.ai.example.manus.coordinator.tool.CoordinatorTool;
import org.springframework.web.reactive.function.server.RouterFunction;
import java.util.List;

/**
 * MCP Server factory interface
 */
public interface McpServerFactory {
    
    /**
     * Create MCP server instance
     * @param endpoint Endpoint address
     * @param tools List of tools
     * @return MCP server instance
     */
    McpServerInstance createServer(String endpoint, List<CoordinatorTool> tools);
    
    /**
     * Get router function for an endpoint
     * @param endpoint Endpoint address
     * @param tools List of tools
     * @return Router function
     */
    RouterFunction<?> createRouter(String endpoint, List<CoordinatorTool> tools);
}
