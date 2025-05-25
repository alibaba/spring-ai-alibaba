/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.springframework.util.StringUtils;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MCP Node: 调用 MCP Server
 */
public class McpNode implements NodeAction {
    
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{(.+?)\\}");
    private static final Logger log = LoggerFactory.getLogger(McpNode.class);
    
    private final String url;
    private final String tool;
    private final Map<String, String> headers;
    private final Map<String, Object> params;
    private final String outputKey;
    
    private final HttpClientSseClientTransport transport;
    private final McpSyncClient client;
    
    private McpNode(Builder builder) {
        this.url = builder.url;
        this.tool = builder.tool;
        this.headers = builder.headers;
        this.params = builder.params;
        this.outputKey = builder.outputKey;
        // 构建 transport 和 client
        HttpClientSseClientTransport.Builder transportBuilder = HttpClientSseClientTransport.builder(this.url);
        if (this.headers != null && !this.headers.isEmpty()) {
            transportBuilder.customizeRequest(req -> this.headers.forEach(req::header));
        }
        this.transport = transportBuilder.build();
        this.client = McpClient.sync(this.transport).build();
        this.client.initialize();
    }
    
    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        log.info("[McpNode] 开始执行 apply，原始配置: url={}, tool={}, headers={}, params={}", url, tool, headers, params);
        // 变量替换
        String finalTool = replaceVariables(tool, state);
        Map<String, Object> finalParams = replaceVariablesObj(params, state);
        log.info("[McpNode] 变量替换后: url={}, tool={}, headers={}, params={}", url, finalTool, headers, finalParams);
        
        // 直接使用已初始化的 client
        Object result;
        try {
            McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(finalTool, finalParams);
            log.info("[McpNode] CallToolRequest 构建: {}", request);
            result = client.callTool(request);
            log.info("[McpNode] 工具调用成功，结果: {}", result);
        } catch (Exception e) {
            log.error("[McpNode] MCP 调用异常: {}", e.getMessage(), e);
            throw new McpNodeException("MCP 调用失败: " + e.getMessage(), e);
        }
        
        // 结果处理
        Map<String, Object> updatedState = new HashMap<>();
        updatedState.put("mcp_result", result);
        if (StringUtils.hasLength(this.outputKey)) {
            updatedState.put(this.outputKey, result);
        }
        log.info("[McpNode] 状态更新: {}", updatedState);
        return updatedState;
    }
    
    private String replaceVariables(String template, OverAllState state) {
        if (template == null)
            return null;
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            Object value = state.value(key).orElse("");
            log.info("[McpNode] 替换变量: {} -> {}", key, value);
            matcher.appendReplacement(result, value.toString());
        }
        matcher.appendTail(result);
        return result.toString();
    }
    
    private Map<String, Object> replaceVariablesObj(Map<String, Object> map, OverAllState state) {
        if (map == null)
            return null;
        Map<String, Object> result = new HashMap<>();
        map.forEach((k, v) -> {
            if (v instanceof String) {
                result.put(k, replaceVariables((String) v, state));
            } else {
                result.put(k, v);
            }
        });
        return result;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String url;
        private String tool;
        private Map<String, String> headers = new HashMap<>();
        private Map<String, Object> params = new HashMap<>();
        private String outputKey;
        
        public Builder url(String url) {
            this.url = url;
            return this;
        }
        
        public Builder tool(String tool) {
            this.tool = tool;
            return this;
        }
        
        public Builder header(String name, String value) {
            this.headers.put(name, value);
            return this;
        }
        
        public Builder param(String name, Object value) {
            this.params.put(name, value);
            return this;
        }
        
        public Builder outputKey(String outputKey) {
            this.outputKey = outputKey;
            return this;
        }
        
        public McpNode build() {
            return new McpNode(this);
        }
    }
    
    public static class McpNodeException extends RuntimeException {
        public McpNodeException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}