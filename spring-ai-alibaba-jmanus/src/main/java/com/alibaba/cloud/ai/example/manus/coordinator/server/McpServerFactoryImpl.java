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

import com.alibaba.cloud.ai.example.manus.coordinator.service.CoordinatorService;
import com.alibaba.cloud.ai.example.manus.coordinator.tool.CoordinatorTool;
import com.alibaba.cloud.ai.example.manus.coordinator.tool.EndPointUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.transport.WebFluxStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;

import java.util.ArrayList;
import java.util.List;

/**
 * MCP Server factory implementation
 */
@Component
public class McpServerFactoryImpl implements McpServerFactory {
    
    private static final Logger log = LoggerFactory.getLogger(McpServerFactoryImpl.class);
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private CoordinatorService coordinatorService;
    
    @Override
    public McpServerInstance createServer(String endpoint, List<CoordinatorTool> tools) {
        try {
            // Build messageEndpoint, add default prefix /mcp
            String messageEndpoint = EndPointUtils.buildMessageEndpoint(endpoint);

            // Create transport provider
            WebFluxStreamableServerTransportProvider transportProvider = WebFluxStreamableServerTransportProvider
                .builder()
                .objectMapper(objectMapper)
                .messageEndpoint(messageEndpoint)
                .build();

            // Create tool specification
            List<McpServerFeatures.SyncToolSpecification> toolSpecs = new ArrayList<>();
            for (CoordinatorTool tool : tools) {
                toolSpecs.add(coordinatorService.createToolSpecification(tool));
            }

            // Create MCP server
            McpServer.SyncSpecification<?> serverSpec = McpServer.sync(transportProvider)
                .serverInfo("jmanus-coordinator-server-" + endpoint, "1.0.0")
                .capabilities(ServerCapabilities.builder().tools(true).logging().build())
                .tools(toolSpecs.toArray(new McpServerFeatures.SyncToolSpecification[0]));

            Object mcpServer = serverSpec.build();
            RouterFunction<?> routerFunction = transportProvider.getRouterFunction();

            log.info("Successfully created MCP server for endpoint: {}, containing {} tools", endpoint, tools.size());

            return new McpServerInstance(endpoint, mcpServer, routerFunction);

        }
        catch (Exception e) {
            log.error("Exception occurred while creating MCP server for endpoint: {}, {}", endpoint, e.getMessage(), e);
            return null;
        }
    }
    
    @Override
    public RouterFunction<?> createRouter(String endpoint, List<CoordinatorTool> tools) {
        try {
            // Build messageEndpoint, add default prefix /mcp
            String messageEndpoint = EndPointUtils.buildMessageEndpoint(endpoint);

            // Create transport provider
            WebFluxStreamableServerTransportProvider transportProvider = WebFluxStreamableServerTransportProvider
                .builder()
                .objectMapper(objectMapper)
                .messageEndpoint(messageEndpoint)
                .build();

            // Create tool specification
            List<McpServerFeatures.SyncToolSpecification> toolSpecs = new ArrayList<>();
            for (CoordinatorTool tool : tools) {
                toolSpecs.add(coordinatorService.createToolSpecification(tool));
            }

            // Create MCP server
            McpServer.SyncSpecification<?> serverSpec = McpServer.sync(transportProvider)
                .serverInfo("jmanus-coordinator-server-" + endpoint, "1.0.0")
                .capabilities(ServerCapabilities.builder().tools(true).logging().build())
                .tools(toolSpecs.toArray(new McpServerFeatures.SyncToolSpecification[0]));

            Object mcpServer = serverSpec.build();
            RouterFunction<?> routerFunction = transportProvider.getRouterFunction();

            log.info("Successfully created router for endpoint: {}, containing {} tools", endpoint, tools.size());

            return routerFunction;

        }
        catch (Exception e) {
            log.error("Exception occurred while creating router for endpoint: {}, {}", endpoint, e.getMessage(), e);
            return null;
        }
    }
}
