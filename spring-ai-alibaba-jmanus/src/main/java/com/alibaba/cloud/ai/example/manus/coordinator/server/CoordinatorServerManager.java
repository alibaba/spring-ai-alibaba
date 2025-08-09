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
import com.alibaba.cloud.ai.example.manus.config.CoordinatorToolProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Coordinator server manager implementation
 */
@Component
public class CoordinatorServerManager implements ServerLifecycle {
    
    private static final Logger log = LoggerFactory.getLogger(CoordinatorServerManager.class);
    
    @Autowired
    private CoordinatorToolProperties coordinatorToolProperties;
    
    @Autowired
    private CoordinatorService coordinatorService;
    
    @Autowired
    private ToolRegistry toolRegistry;
    
    @Autowired
    private McpServerFactory mcpServerFactory;
    
    @Autowired
    private HttpServerManager httpServerManager;
    
    @Autowired
    private RouterBuilder routerBuilder;
    
    // Store registered MCP servers, grouped by endpoint
    private final Map<String, McpServerInstance> registeredMcpServers = new ConcurrentHashMap<>();
    
    private boolean running = false;
    
    @Override
    public void start() {
        // Check if CoordinatorTool feature is enabled
        if (!coordinatorToolProperties.isEnabled()) {
            log.info("CoordinatorTool feature is disabled, skipping coordinator server startup");
            return;
        }

        try {
            log.info("==========================================");
            log.info("JManus Multi EndPoint Streamable Http Server");
            log.info("==========================================");
            log.info("Starting JManus Multi EndPoint Streamable Http Server...");

            log.info("Server Information:");
            log.info("  Full Address: http://{}:{}", EndPointUtils.SERVICE_HOST, EndPointUtils.SERVICE_PORT);

            // Load coordinator tools
            Map<String, List<CoordinatorTool>> coordinatorToolsByEndpoint = coordinatorService.loadCoordinatorTools();

            // Combine all router functions
            RouterFunction<?> combinedRouter = routerBuilder.buildCombinedRouter(coordinatorToolsByEndpoint);

            if (combinedRouter == null) {
                log.warn("No router functions created, server may not function normally");
            }
            else {
                log.info("Successfully created combined router functions");
            }

            // Start HTTP server
            httpServerManager.startHttpServer(combinedRouter);

            log.info("JManus Multi EndPoint Streamable Http Server started successfully!");
            log.info("==========================================");
            log.info("Coordinator Service List:");
            log.info("==========================================");

            // Output all coordinator service information
            if (!coordinatorToolsByEndpoint.isEmpty()) {
                int serviceIndex = 1;
                for (Map.Entry<String, List<CoordinatorTool>> entry : coordinatorToolsByEndpoint.entrySet()) {
                    String endpoint = entry.getKey();
                    List<CoordinatorTool> tools = entry.getValue();
                    log.info("  Full URL: {}", EndPointUtils.getUrl(endpoint));
                    log.info("  Tool Count: {}", tools.size());

                    // Output all tools for this endpoint
                    for (int i = 0; i < tools.size(); i++) {
                        CoordinatorTool tool = tools.get(i);
                        log.info("    Tool #{}: {} - {}", i + 1, tool.getToolName(), tool.getToolDescription());
                    }
                    log.info("  ----------------------------------------");
                }
            }
            else {
                log.info("No coordinator services found");
            }

            log.info("==========================================");
            log.info("Coordinator service startup complete, {} endpoints", coordinatorToolsByEndpoint.size());
            log.info("==========================================");

            running = true;

        }
        catch (Exception e) {
            log.error("Error starting server: {}", e.getMessage(), e);
        }
    }
    
    @Override
    public void stop() {
        try {
            log.info("Stopping coordinator server...");
            
            // Stop HTTP server
            httpServerManager.stopHttpServer();
            
            // Close all MCP servers
            for (McpServerInstance serverInstance : registeredMcpServers.values()) {
                serverInstance.close();
            }
            registeredMcpServers.clear();
            
            running = false;
            log.info("Coordinator server stopped");
            
        } catch (Exception e) {
            log.error("Error stopping server: {}", e.getMessage(), e);
        }
    }
    
    @Override
    public boolean isRunning() {
        return running && httpServerManager.isRunning();
    }
    
    /**
     * Register a tool and update server
     * @param tool Tool to register
     * @return true if successful
     */
    public boolean registerTool(CoordinatorTool tool) {
        boolean success = toolRegistry.register(tool);
        if (success) {
            updateServerForEndpoint(tool.getEndpoint());
        }
        return success;
    }
    
    /**
     * Unregister a tool and update server
     * @param toolName Tool name
     * @param endpoint Endpoint
     * @return true if successful
     */
    public boolean unregisterTool(String toolName, String endpoint) {
        boolean success = toolRegistry.unregister(toolName, endpoint);
        if (success) {
            updateServerForEndpoint(endpoint);
        }
        return success;
    }
    
    /**
     * Refresh a tool and update server
     * @param toolName Tool name
     * @param updatedTool Updated tool
     * @return true if successful
     */
    public boolean refreshTool(String toolName, CoordinatorTool updatedTool) {
        boolean success = toolRegistry.refreshTool(toolName, updatedTool);
        if (success) {
            updateServerForEndpoint(updatedTool.getEndpoint());
        }
        return success;
    }
    
    /**
     * Update server for a specific endpoint
     * @param endpoint Endpoint to update
     */
    private void updateServerForEndpoint(String endpoint) {
        try {
            List<CoordinatorTool> tools = toolRegistry.getTools(endpoint);
            
            if (tools.isEmpty()) {
                // Remove MCP server if no tools left
                McpServerInstance serverInstance = registeredMcpServers.remove(endpoint);
                if (serverInstance != null) {
                    serverInstance.close();
                    log.info("Removed MCP server for empty endpoint: {}", endpoint);
                }
            } else {
                // Create or update MCP server
                McpServerInstance serverInstance = mcpServerFactory.createServer(endpoint, tools);
                if (serverInstance != null) {
                    registeredMcpServers.put(endpoint, serverInstance);
                    log.info("Updated MCP server for endpoint: {}", endpoint);
                }
            }
            
            // Recreate HTTP server to update routes
            RouterFunction<?> combinedRouter = routerBuilder.buildCombinedRouter(toolRegistry.getAllTools());
            if (combinedRouter != null) {
                httpServerManager.recreateHttpServer(combinedRouter);
            }
            
        } catch (Exception e) {
            log.error("Error updating server for endpoint {}: {}", endpoint, e.getMessage(), e);
        }
    }
}
