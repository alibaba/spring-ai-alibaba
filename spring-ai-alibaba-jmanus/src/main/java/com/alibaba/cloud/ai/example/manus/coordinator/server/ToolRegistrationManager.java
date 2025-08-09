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
import com.alibaba.cloud.ai.example.manus.config.CoordinatorToolProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tool registration manager implementation
 */
@Component
public class ToolRegistrationManager implements ToolRegistry {
    
    private static final Logger log = LoggerFactory.getLogger(ToolRegistrationManager.class);
    
    @Autowired
    private CoordinatorToolProperties coordinatorToolProperties;
    
    // Store registered tools, grouped by endpoint
    private final Map<String, List<CoordinatorTool>> registeredTools = new ConcurrentHashMap<>();
    
    @Override
    public boolean register(CoordinatorTool tool) {
        // Check if CoordinatorTool feature is enabled
        if (!coordinatorToolProperties.isEnabled()) {
            log.info("CoordinatorTool feature is disabled, skipping tool registration");
            return false;
        }

        if (tool == null) {
            log.warn("CoordinatorTool is null, cannot register");
            return false;
        }

        String endpoint = tool.getEndpoint();
        if (endpoint == null || endpoint.trim().isEmpty()) {
            log.warn("CoordinatorTool's endpoint is empty, cannot register");
            return false;
        }

        try {
            log.info("Starting to register CoordinatorTool: {} to endpoint: {}", tool.getToolName(), endpoint);

            // Get or create the tool list for this endpoint
            List<CoordinatorTool> toolsForEndpoint = registeredTools.computeIfAbsent(endpoint, k -> new ArrayList<>());

            // Check if tool is already registered, and remove old version if it is
            boolean alreadyRegistered = toolsForEndpoint.stream()
                .anyMatch(existingTool -> existingTool.getToolName().equals(tool.getToolName()));

            if (alreadyRegistered) {
                log.info("CoordinatorTool: {} is already registered to endpoint: {}, will update to new service registration", tool.getToolName(), endpoint);
                // Remove old tool version
                toolsForEndpoint.removeIf(existingTool -> existingTool.getToolName().equals(tool.getToolName()));
            }

            // Add new tool to the list (whether it's new or updated)
            toolsForEndpoint.add(tool);
            log.info("Successfully added CoordinatorTool: {} to the tool list for endpoint: {}", tool.getToolName(), endpoint);

            log.info("Successfully registered CoordinatorTool: {} to endpoint: {}", tool.getToolName(), endpoint);
            return true;

        }
        catch (Exception e) {
            log.error("Exception occurred while registering CoordinatorTool: {}", e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public boolean unregister(String toolName, String endpoint) {
        // Check if CoordinatorTool feature is enabled
        if (!coordinatorToolProperties.isEnabled()) {
            log.info("CoordinatorTool feature is disabled, skipping tool unregistration");
            return false;
        }

        if (toolName == null || toolName.trim().isEmpty()) {
            log.warn("Tool name is empty, cannot unregister");
            return false;
        }

        if (endpoint == null || endpoint.trim().isEmpty()) {
            log.warn("Endpoint is empty, cannot unregister");
            return false;
        }

        try {
            log.info("Starting to unregister CoordinatorTool: {} from endpoint: {}", toolName, endpoint);

            // Get tool list for this endpoint
            List<CoordinatorTool> toolsForEndpoint = registeredTools.get(endpoint);
            if (toolsForEndpoint == null) {
                log.warn("No tools found for endpoint: {}", endpoint);
                return false;
            }

            // Remove tool from the list
            boolean removed = toolsForEndpoint.removeIf(tool -> tool.getToolName().equals(toolName));

            if (!removed) {
                log.warn("Tool: {} not found in endpoint: {}", toolName, endpoint);
                return false;
            }

            log.info("Successfully removed tool: {} from endpoint: {}", toolName, endpoint);

            // If no tools left for this endpoint, remove the endpoint entirely
            if (toolsForEndpoint.isEmpty()) {
                registeredTools.remove(endpoint);
                log.info("Removed empty endpoint: {}", endpoint);
            }

            log.info("Successfully unregistered CoordinatorTool: {} from endpoint: {}", toolName, endpoint);
            return true;

        }
        catch (Exception e) {
            log.error("Exception occurred while unregistering CoordinatorTool: {}", e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public List<CoordinatorTool> getTools(String endpoint) {
        return registeredTools.getOrDefault(endpoint, new ArrayList<>());
    }
    
    @Override
    public Map<String, List<CoordinatorTool>> getAllTools() {
        return new ConcurrentHashMap<>(registeredTools);
    }
    
    @Override
    public boolean refreshTool(String toolName, CoordinatorTool updatedTool) {
        // Check if CoordinatorTool feature is enabled
        if (!coordinatorToolProperties.isEnabled()) {
            log.info("CoordinatorTool feature is disabled, skipping tool refresh");
            return false;
        }

        if (updatedTool == null || toolName == null) {
            log.warn("Tool or tool name is empty, cannot refresh");
            return false;
        }

        String endpoint = updatedTool.getEndpoint();
        if (endpoint == null || endpoint.trim().isEmpty()) {
            log.warn("Tool's endpoint is empty, cannot refresh");
            return false;
        }

        try {
            log.info("Starting to force refresh tool: {} in endpoint: {}", toolName, endpoint);

            // Get tool list for this endpoint
            List<CoordinatorTool> toolsForEndpoint = registeredTools.get(endpoint);
            if (toolsForEndpoint == null) {
                log.warn("Tool list not found for endpoint: {}", endpoint);
                return false;
            }

            // Find and replace tool
            boolean found = false;
            for (int i = 0; i < toolsForEndpoint.size(); i++) {
                if (toolsForEndpoint.get(i).getToolName().equals(toolName)) {
                    toolsForEndpoint.set(i, updatedTool);
                    found = true;
                    log.info("Tool found and replaced: {}", toolName);
                    break;
                }
            }

            if (!found) {
                log.warn("Tool: {} not found in endpoint: {}", endpoint, toolName);
                return false;
            }

            log.info("Successfully refreshed tool: {} in endpoint: {}", toolName, endpoint);
            return true;

        }
        catch (Exception e) {
            log.error("Exception occurred while refreshing tool: {}", e.getMessage(), e);
            return false;
        }
    }
}
