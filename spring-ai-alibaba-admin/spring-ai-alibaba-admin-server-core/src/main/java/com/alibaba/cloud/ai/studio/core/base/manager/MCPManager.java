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

package com.alibaba.cloud.ai.studio.core.base.manager;

import com.alibaba.cloud.ai.studio.runtime.domain.mcp.Content;
import com.alibaba.cloud.ai.studio.runtime.domain.mcp.McpServerCallToolRequest;
import com.alibaba.cloud.ai.studio.runtime.domain.mcp.McpServerCallToolResponse;
import com.alibaba.cloud.ai.studio.runtime.domain.mcp.McpServerDeployConfig;
import com.alibaba.cloud.ai.studio.runtime.domain.mcp.McpTool;
import com.alibaba.cloud.ai.studio.runtime.domain.mcp.TextContent;
import com.alibaba.cloud.ai.studio.runtime.enums.McpInstallTypeEnum;
import com.alibaba.cloud.ai.studio.runtime.enums.McpServerStatusEnum;
import com.alibaba.cloud.ai.studio.runtime.domain.Result;
import com.alibaba.cloud.ai.studio.runtime.domain.tool.InputSchema;
import com.alibaba.cloud.ai.studio.runtime.utils.JsonUtils;
import com.alibaba.cloud.ai.studio.core.base.constants.CacheConstants;
import com.alibaba.cloud.ai.studio.core.base.entity.McpServerEntity;
import com.alibaba.cloud.ai.studio.core.utils.LogUtils;
import com.alibaba.cloud.ai.studio.core.utils.concurrent.ThreadPoolUtils;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode.MCP_PARSE_CONFIG_ERROR;
import static com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode.MCP_PARSE_URL_ERROR;
import static com.alibaba.cloud.ai.studio.core.utils.LogUtils.FAIL;
import static com.alibaba.cloud.ai.studio.core.utils.LogUtils.SUCCESS;

/**
 * Manager class for handling Model Context Protocol (MCP) operations. Provides
 * functionality for client creation, tool management, and server configuration.
 */
@Slf4j
@Component
public class MCPManager {

    private static final String SSE_DEFAULT_ENDPOINT = "/sse";
    private static final String STREAMABLE_HTTP_DEFAULT_ENDPOINT = "/mcp";
    private static final String STREAMABLE_HTTP_TYPE = "streamable_http";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String JSON_CONTENT_TYPE = "application/json";
    private static final int REQUEST_TIMEOUT_SECONDS = 3;
    private static final int INITIALIZATION_TIMEOUT_SECONDS = 3;
    private static final String MCP_SERVERS_KEY = "mcpServers";
    private static final String HEADERS_KEY = "headers";
    private static final String URL_KEY = "url";

    /** Redis manager for caching operations */
    private final RedisManager redisManager;

    public MCPManager(RedisManager redisManager) {
        this.redisManager = redisManager;
    }

    /**
     * Creates a synchronous MCP client for the given server entity.
     * @param entity Server entity containing deployment configuration
     * @return Synchronous MCP client instance
     */
    public McpSyncClient getMcpSyncClient(McpServerEntity entity) {
        McpServerDeployConfig deployConfig = JsonUtils.fromJson(entity.getDeployConfig(), McpServerDeployConfig.class);
        String remoteEndpoint = getRemoteEndpointOrDefault(deployConfig, entity.getInstallType());
        String host = entity.getHost();
        LogUtils.error("Client host", host, entity.getServerCode());
        Map<String, String> headers = deployConfig.getRemoteHeader();
        // 创建基础HTTP请求构建器
        HttpRequest.Builder requestBuilder = createHttpRequestBuilder(headers);
        McpClientTransport transport = createTransport(host, remoteEndpoint, entity.getInstallType(), requestBuilder);
        McpClient.SyncSpec builder = McpClient.sync(transport)
                .requestTimeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
                .initializationTimeout(Duration.ofSeconds(INITIALIZATION_TIMEOUT_SECONDS))
                .capabilities(McpSchema.ClientCapabilities.builder().roots(true).build());
        return builder.build();
    }

    /**
     * Gets remote endpoint from deploy config or returns default value based on server type
     * @param deployConfig Deployment configuration
     * @param serverType Type of server (streamable_http or other)
     * @return Remote endpoint
     */
    private String getRemoteEndpointOrDefault(McpServerDeployConfig deployConfig, String serverType) {
        String remoteEndpoint = deployConfig.getRemoteEndpoint();
        if (StringUtils.isBlank(remoteEndpoint)) {
            // Return appropriate default based on server type
            if (STREAMABLE_HTTP_TYPE.equalsIgnoreCase(serverType)) {
                return STREAMABLE_HTTP_DEFAULT_ENDPOINT;
            } else {
                return SSE_DEFAULT_ENDPOINT;
            }
        }
        return remoteEndpoint;
    }

    /**
     * Creates HTTP request builder with default configuration and headers
     * @param headers Map of headers to add to the request
     * @return Configured HttpRequest.Builder
     */
    private HttpRequest.Builder createHttpRequestBuilder(Map<String, String> headers) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                .timeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS));

        // 添加配置中的headers
        if (headers != null) {
            for (Map.Entry<String, String> headerEntry : headers.entrySet()) {
                builder.header(headerEntry.getKey(), headerEntry.getValue());
            }
        }
        return builder;
    }

    /**
     * Creates appropriate transport based on server type
     * @param host Server host
     * @param remoteEndpoint Remote endpoint
     * @param serverType Type of server (streamable_http or other)
     * @param requestBuilder HTTP request builder
     * @return MCP client transport
     */
    private McpClientTransport createTransport(String host, String remoteEndpoint, String serverType, HttpRequest.Builder requestBuilder) {
        // 根据type选择适当的传输实现
        if (STREAMABLE_HTTP_TYPE.equalsIgnoreCase(serverType)) {
            // 使用HttpClientStreamableHttpTransport
            return HttpClientStreamableHttpTransport.builder(host)
                    .requestBuilder(requestBuilder)
                    .endpoint(remoteEndpoint)
                    .build();
        } else {
            // 默认使用HttpClientSseClientTransport（包括"SSE"类型）
            return HttpClientSseClientTransport.builder(host)
                    .requestBuilder(requestBuilder)
                    .sseEndpoint(remoteEndpoint)
                    .build();
        }
    }

    /**
     * Retrieves available tools from the specified MCP server. Implements caching
     * mechanism for non-SSE installations.
     * @param entity Server entity to query tools from
     * @return List of available tools
     */
    public List<McpTool> getTools(McpServerEntity entity) {
        long start = System.currentTimeMillis();
        if (entity == null || !entity.getStatus().equals(McpServerStatusEnum.Normal.getCode())) {
            return new ArrayList<>();
        }
        String cacheKey = "mcp_tools_cache_" + entity.getServerCode();
        boolean needCache = !"SSE".equals(entity.getInstallType());
        if (needCache) {
            List<McpTool> toolList = redisManager.get(cacheKey);
            if (toolList != null) {
                return toolList;
            }
        }
        Future<Object> future = ThreadPoolUtils.TOOL_TASK_EXECUTOR.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                McpSyncClient client = getMcpSyncClient(entity);
                try {
                    client.initialize();
                    McpSchema.ListToolsResult toolsResult = client.listTools();
                    List<McpSchema.Tool> toolsList = toolsResult.tools();
                    List<McpTool> tools = new ArrayList<>();
                    if (CollectionUtils.isEmpty(toolsList)) {
                        return tools;
                    }
                    toolsList.forEach(e -> {
                        McpTool tool = new McpTool();
                        tool.setName(e.name());
                        tool.setDescription(e.description());
                        McpSchema.JsonSchema jsonSchema = e.inputSchema();
                        InputSchema inputSchema = new InputSchema();
                        inputSchema.setType(jsonSchema.type());
                        inputSchema.setProperties(jsonSchema.properties());
                        inputSchema.setRequired(jsonSchema.required());
                        inputSchema.setAdditionalProperties(jsonSchema.additionalProperties());
                        tool.setInputSchema(inputSchema);
                        tools.add(tool);
                    });
                    LogUtils.error("FromServer GetTool", entity.getServerCode(), System.currentTimeMillis() - start,
                            tools.size());
                    return tools;
                }
                catch (Exception ex) {
                    LogUtils.error("getTools error", ex, entity.getServerCode(), entity.getHost());
                    return new ArrayList<>();
                }
                finally {
                    if (client != null) {
                        try {
                            client.close();
                        } catch (Exception closeEx) {
                            LogUtils.error("Error closing client", closeEx, entity.getServerCode());
                        }
                    }
                }
            }
        });
        try {
            List<McpTool> result = (List<McpTool>) future.get(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (result != null && !result.isEmpty() && needCache) {
                redisManager.put(cacheKey, result, CacheConstants.CACHE_EMPTY_TTL);
            }
            LogUtils.error("FromServer Last", entity.getServerCode(), System.currentTimeMillis() - start, result);
            return result;
        }
        catch (Exception ex) {
            LogUtils.error("toolsGetFuture Exception", ex, entity.getServerCode());
            return new ArrayList<>();
        }
    }

    /**
     * Executes a specific tool on the MCP server.
     * @param request Tool execution request parameters
     * @param entity Server entity where the tool resides
     * @return Response from the tool execution
     */
    public McpServerCallToolResponse callTool(McpServerCallToolRequest request, McpServerEntity entity) {
        Long start = System.currentTimeMillis();
        McpSyncClient client = getMcpSyncClient(entity);
        McpServerCallToolResponse response = new McpServerCallToolResponse();
        try {
            client.initialize();
            McpSchema.CallToolRequest callToolRequest = new McpSchema.CallToolRequest(request.getToolName(),
                    request.getToolParams());
            McpSchema.CallToolResult callToolResult = client.callTool(callToolRequest);
            response.setIsError(callToolResult.isError());
            List<McpSchema.Content> contentList = callToolResult.content();
            List<Content> content = new ArrayList<>();
            contentList.forEach(e -> {
                if (e.type().equals("text")) {
                    TextContent textContent = new TextContent();
                    textContent.setType(e.type());
                    McpSchema.TextContent tContent = (McpSchema.TextContent) e;
                    textContent.setText(tContent.text());
                    content.add(textContent);
                }
            });
            response.setContent(content);
        }
        catch (Exception ex) {
            LogUtils.monitor("McpService", "callTool", start, FAIL, request, ex.getMessage(), ex);
            LogUtils.error("McpServerManager callTool exception", ex);
            throw ex;
        }
        finally {
            if (client != null) {
                try {
                    client.close();
                } catch (Exception closeEx) {
                    LogUtils.error("Error closing client", closeEx, entity.getServerCode());
                }
            }
        }
        LogUtils.monitor("McpService", "callTool", start, SUCCESS, request, response);
        return response;
    }

    /**
     * Processes and validates installation configuration. Handles URL validation and
     * configuration transformation based on installation type.
     * @param originDeployConfig Original deployment configuration string
     * @param installType Installation type (e.g., SSE)
     * @return Result containing processed configuration or error
     */
    public Result<String> processInstallConfig(String originDeployConfig, String installType) {
        try {
            McpInstallTypeEnum installTypeEnum = McpInstallTypeEnum.of(installType);
            HashMap<String, Object> targetDeployConfig = new HashMap<>();
            Map<String, Object> originConfig = JsonUtils.fromJsonToMap(originDeployConfig);
            Map<String, Object> mcpServers = JsonUtils.fromJsonToMap(JsonUtils.toJson(originConfig.get(MCP_SERVERS_KEY)));

            if (mcpServers == null || mcpServers.keySet().size() != 1) {
                LogUtils.error("ParseConfigError", "mcpServers must be configured and only one is supported");
                return Result.error(MCP_PARSE_CONFIG_ERROR);
            }
            targetDeployConfig.put("install_config", originDeployConfig);
            if (installTypeEnum == McpInstallTypeEnum.SSE || installTypeEnum == McpInstallTypeEnum.STREAMABLE_HTTP) {
                // check request header and host address
                for (String singleServer : mcpServers.keySet()) {
                    Map<String, Object> singleServerConfig = JsonUtils
                            .fromJsonToMap(JsonUtils.toJson(mcpServers.get(singleServer)));
                    String url = (String) singleServerConfig.get(URL_KEY);
                    try {
                        URL urlObj = new URL(url);
//						if (!urlObj.getPath().endsWith("/sse")) {
//							return Result.error(MCP_PARSE_URL_ERROR);
//						}

                        String remoteAddress = urlObj.getProtocol() + "://" + urlObj.getHost();
                        if (urlObj.getPort() != -1) {
                            remoteAddress += ":" + urlObj.getPort();
                        }
                        targetDeployConfig.put("remote_address", remoteAddress);
                        targetDeployConfig.put("remote_endpoint", urlObj.getPath());
                        String query = urlObj.getQuery();
                        if (StringUtils.isNotBlank(query)) {
                            targetDeployConfig.put("remote_endpoint", urlObj.getPath() + "?" + query);
                        }
                        Map<Object, Object> headers = JsonUtils
                                .fromJsonToMap(JsonUtils.toJson(singleServerConfig.get(HEADERS_KEY)));
                        targetDeployConfig.put("remote_header", headers);
                    }
                    catch (Exception urlCheckEx) {
                        LogUtils.error("processInstallConfig", url, urlCheckEx);
                        return Result.error(MCP_PARSE_URL_ERROR);
                    }
                    break;
                }
            }
            return Result.success(JsonUtils.toJson(targetDeployConfig));
        }
        catch (Exception ex) {
            LogUtils.error("processInstallConfig exception", ex, originDeployConfig);
            throw ex;
        }
    }
}