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
import java.util.List;

/**
 * Tool registration management interface
 */
public interface ToolRegistry {
    
    /**
     * Register a tool
     * @param tool Tool to register
     * @return true if successful, false otherwise
     */
    boolean register(CoordinatorTool tool);
    
    /**
     * Unregister a tool
     * @param toolName Tool name
     * @param endpoint Endpoint address
     * @return true if successful, false otherwise
     */
    boolean unregister(String toolName, String endpoint);
    
    /**
     * Get tools for a specific endpoint
     * @param endpoint Endpoint address
     * @return List of tools
     */
    List<CoordinatorTool> getTools(String endpoint);
    
    /**
     * Get all registered tools grouped by endpoint
     * @return Map of tools grouped by endpoint
     */
    java.util.Map<String, List<CoordinatorTool>> getAllTools();
    
    /**
     * Refresh a specific tool
     * @param toolName Tool name
     * @param updatedTool Updated tool
     * @return true if successful, false otherwise
     */
    boolean refreshTool(String toolName, CoordinatorTool updatedTool);
}
