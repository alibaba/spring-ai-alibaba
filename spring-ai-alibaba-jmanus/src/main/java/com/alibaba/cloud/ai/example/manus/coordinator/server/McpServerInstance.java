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

import org.springframework.web.reactive.function.server.RouterFunction;

/**
 * MCP Server instance wrapper
 */
public class McpServerInstance {
    
    private final String endpoint;
    private final Object mcpServer;
    private final RouterFunction<?> routerFunction;
    
    public McpServerInstance(String endpoint, Object mcpServer, RouterFunction<?> routerFunction) {
        this.endpoint = endpoint;
        this.mcpServer = mcpServer;
        this.routerFunction = routerFunction;
    }
    
    public String getEndpoint() {
        return endpoint;
    }
    
    public Object getMcpServer() {
        return mcpServer;
    }
    
    public RouterFunction<?> getRouterFunction() {
        return routerFunction;
    }
    
    public void close() {
        if (mcpServer instanceof AutoCloseable) {
            try {
                ((AutoCloseable) mcpServer).close();
            } catch (Exception e) {
                // Log error but don't throw
            }
        }
    }
}
