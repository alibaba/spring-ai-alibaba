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

package com.alibaba.cloud.ai.example.deepresearch.agents;

import com.alibaba.cloud.ai.example.deepresearch.config.McpJsonProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.WebFluxSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.AsyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP JSON自动配置类
 * 从JSON文件读取MCP配置并创建相应的MCP客户端
 *
 * @author Makoto
 * @since 2025/6/14
 */
@Configuration
@ConditionalOnClass({ McpAsyncClient.class, WebFluxSseClientTransport.class })
@ConditionalOnProperty(prefix = "spring.ai.alibaba.deepresearch.mcp", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties({McpJsonProperties.class})
public class McpJsonAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(McpJsonAutoConfiguration.class);

    private final McpJsonProperties mcpJsonProperties;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;

    public McpJsonAutoConfiguration(McpJsonProperties mcpJsonProperties,
                                    ResourceLoader resourceLoader,
                                    ObjectMapper objectMapper) {
        this.mcpJsonProperties = mcpJsonProperties;
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
    }

    /**
     * 创建基于JSON配置的ToolCallbackProvider Bean
     */
    @Bean
    public JsonBasedMcpToolCallbackProvider jsonBasedMcpToolCallbackProvider() throws IOException {
        Map<String, List<McpAsyncClient>> agentMcpClients = createMcpClientsFromJson();
        return new JsonBasedMcpToolCallbackProvider(agentMcpClients);
    }

    /**
     * 从JSON文件创建按代理分组的MCP客户端
     */
    private Map<String, List<McpAsyncClient>> createMcpClientsFromJson() throws IOException {
        Map<String, List<McpAsyncClient>> agentClients = new HashMap<>();
        

        Resource configResource = resourceLoader.getResource(mcpJsonProperties.getConfigFile());
        if (!configResource.exists()) {
            logger.warn("MCP配置文件不存在: {}", mcpJsonProperties.getConfigFile());
            return agentClients;
        }

        McpJsonProperties.McpJsonConfig config;
        try (InputStream inputStream = configResource.getInputStream()) {
            config = objectMapper.readValue(inputStream, McpJsonProperties.McpJsonConfig.class);
        } catch (Exception e) {
            logger.error("解析JSON配置文件失败", e);
            throw e;
        }


        if (config.getCoder() != null && config.getCoder().getMcpServers() != null) {
            List<McpAsyncClient> coderClients = new ArrayList<>();
            for (McpJsonProperties.McpServerConfig serverConfig : config.getCoder().getMcpServers()) {
                McpAsyncClient client = createMcpClient("coder_" + serverConfig.getUrl().hashCode(), serverConfig);
                if (client != null) {
                    coderClients.add(client);
                }
            }
            agentClients.put("coderAgent", coderClients);
        }

        // 处理researcher agent的MCP服务器
        if (config.getResearcher() != null && config.getResearcher().getMcpServers() != null) {
            List<McpAsyncClient> researcherClients = new ArrayList<>();
            for (McpJsonProperties.McpServerConfig serverConfig : config.getResearcher().getMcpServers()) {
                McpAsyncClient client = createMcpClient("researcher_" + serverConfig.getUrl().hashCode(), serverConfig);
                if (client != null) {
                    researcherClients.add(client);
                }
            }
            agentClients.put("researchAgent", researcherClients);
        }

        logger.info("从JSON配置创建了MCP客户端: coder={}, researcher={}", 
                   agentClients.getOrDefault("coderAgent", new ArrayList<>()).size(),
                   agentClients.getOrDefault("researchAgent", new ArrayList<>()).size());
        return agentClients;
    }

    /**
     * 创建单个MCP客户端
     */
    private McpAsyncClient createMcpClient(String serverName, McpJsonProperties.McpServerConfig serverConfig) {
        try {
            String url = serverConfig.getUrl();
            String baseUrl = url.contains("?") ? url.substring(0, url.indexOf("?")) : url;
            String sseEndpoint = url.contains("?") ? "/sse" + url.substring(url.indexOf("?")) : "/sse";
            
            WebClient.Builder webClientBuilder = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Accept", "text/event-stream")
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("User-Agent", "MCP-Client/1.0.0");

            WebFluxSseClientTransport transport = new WebFluxSseClientTransport(webClientBuilder, objectMapper, sseEndpoint);
            
            McpAsyncClient mcpClient = McpClient.async(transport)
                .clientInfo(new McpSchema.Implementation(serverName, "1.0.0"))
                .build();
            
            mcpClient.initialize().block(Duration.ofMinutes(2));
            logger.info("MCP客户端初始化成功: {} -> {}{}", serverName, baseUrl, sseEndpoint);
            return mcpClient;
            
        } catch (Exception e) {
            logger.error("创建MCP客户端失败: {} -> {}", serverName, serverConfig.getUrl(), e);
            return null;
        }
    }

    /**
     * 支持按代理名称获取工具回调的接口
     */
    public interface AgentSpecificToolCallbackProvider extends ToolCallbackProvider {
        ToolCallback[] getToolCallbacksForAgent(String agentName);
    }

    /**
     * 自定义的ToolCallbackProvider实现
     * 基于JSON配置的MCP客户端，支持按代理名称获取工具回调
     */
    public static class JsonBasedMcpToolCallbackProvider implements AgentSpecificToolCallbackProvider {
        
        private final Map<String, List<AsyncMcpToolCallbackProvider>> agentProviders;
        private static final Logger logger = LoggerFactory.getLogger(JsonBasedMcpToolCallbackProvider.class);
        
        public JsonBasedMcpToolCallbackProvider(Map<String, List<McpAsyncClient>> agentMcpClients) {
            this.agentProviders = new HashMap<>();
            
            // 为每个代理的每个MCP客户端创建AsyncMcpToolCallbackProvider
            for (Map.Entry<String, List<McpAsyncClient>> entry : agentMcpClients.entrySet()) {
                String agentName = entry.getKey();
                List<McpAsyncClient> clients = entry.getValue();
                
                if (!clients.isEmpty()) {
                    List<AsyncMcpToolCallbackProvider> providers = new ArrayList<>();
                    
                    for (McpAsyncClient client : clients) {
                        try {
                            AsyncMcpToolCallbackProvider provider = new AsyncMcpToolCallbackProvider(client);
                            providers.add(provider);
                        } catch (Exception e) {
                            logger.error("为代理 {} 创建AsyncMcpToolCallbackProvider失败", agentName, e);
                        }
                    }
                    
                    if (!providers.isEmpty()) {
                        agentProviders.put(agentName, providers);
                    }
                }
            }
            
            logger.info("JsonBasedMcpToolCallbackProvider创建完成，代理: {}", agentProviders.keySet());
        }
        
        @Override
        public ToolCallback[] getToolCallbacks() {
            List<ToolCallback> allCallbacks = new ArrayList<>();
            for (List<AsyncMcpToolCallbackProvider> providers : agentProviders.values()) {
                for (AsyncMcpToolCallbackProvider provider : providers) {
                    ToolCallback[] callbacks = provider.getToolCallbacks();
                    allCallbacks.addAll(List.of(callbacks));
                }
            }
            return allCallbacks.toArray(new ToolCallback[0]);
        }
        
        @Override
        public ToolCallback[] getToolCallbacksForAgent(String agentName) {
            List<AsyncMcpToolCallbackProvider> providers = agentProviders.get(agentName);
            if (providers != null && !providers.isEmpty()) {
                List<ToolCallback> allCallbacks = new ArrayList<>();
                
                for (AsyncMcpToolCallbackProvider provider : providers) {
                    ToolCallback[] callbacks = provider.getToolCallbacks();
                    allCallbacks.addAll(List.of(callbacks));
                }
                
                return allCallbacks.toArray(new ToolCallback[0]);
            } else {
                return new ToolCallback[0];
            }
        }
    }
} 