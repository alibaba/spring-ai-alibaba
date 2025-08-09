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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;

import java.util.Map;

/**
 * Router builder for coordinator server
 */
@Component
public class RouterBuilder {
    
    private static final Logger log = LoggerFactory.getLogger(RouterBuilder.class);
    
    @Autowired
    private McpServerFactory mcpServerFactory;
    
    /**
     * Build combined router functions
     * @param toolsByEndpoint Tools grouped by endpoint
     * @return Combined router function
     */
    public RouterFunction<?> buildCombinedRouter(Map<String, java.util.List<CoordinatorTool>> toolsByEndpoint) {
        log.info("Starting to create combined router functions, {} endpoints", toolsByEndpoint.size());

        RouterFunction<?> combinedRouter = null;

        // Create independent transport providers and servers for each coordinator endpoint
        for (Map.Entry<String, java.util.List<CoordinatorTool>> entry : toolsByEndpoint.entrySet()) {
            String endpoint = entry.getKey();
            java.util.List<CoordinatorTool> tools = entry.getValue();

            if (tools.isEmpty()) {
                continue;
            }

            // Create MCP server and router function
            RouterFunction<?> routerFunction = mcpServerFactory.createRouter(endpoint, tools);
            if (routerFunction != null) {
                if (combinedRouter == null) {
                    combinedRouter = routerFunction;
                }
                else {
                    combinedRouter = combinedRouter.andOther(routerFunction);
                }
            }

            log.info("Created MCP server for endpoint {}, containing {} tools", endpoint, tools.size());
        }

        return combinedRouter;
    }
}
