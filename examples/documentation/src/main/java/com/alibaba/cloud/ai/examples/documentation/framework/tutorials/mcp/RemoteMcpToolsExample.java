/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.examples.documentation.framework.tutorials.mcp;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.Builder;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.alibaba.fastjson2.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 远程工具调用示例
 *
 * @author NGshiyu
 */
@Service
public class RemoteMcpToolsExample {

    private static final Logger log = LoggerFactory.getLogger(RemoteMcpToolsExample.class);
    @Autowired
    private final ToolCallbackProvider toolCallbackProvider;

    public RemoteMcpToolsExample(ToolCallbackProvider toolCallbackProvider) {this.toolCallbackProvider = toolCallbackProvider;}

    private static @NonNull ChatModel getChatModel() {
        DashScopeApi dashScopeApi = DashScopeApi.builder()
                .apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
                .build();

        return DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .build();
    }


    /**
     * 示例17：基于 Spring Boot 使用远端MCP工具 -- React Agent
     */
    public void remoteMcpToolsReactWithSpringBootExample() throws GraphRunnerException {
        ChatModel chatModel = getChatModel();

        //Get Tools From Spring AI ToolCallbackProvider which the tools config in application.yml
        ToolCallback[] toolCallbacks = toolCallbackProvider.getToolCallbacks();
        System.out.printf("""
                        ==============================Find the tools from spring ToolCallbackProvider==============================
                        %s
                        """,
                JSON.toJSONString(toolCallbacks));

        executeAgent(chatModel, "You are a helpful assistant with travel route planning and train ticket search.", "travel_planning_session", """
                I plan to travel from Shanghai to Beijing tomorrow.
                1. Please check at which stations I can alight (i.e., the available arrival/drop-off stations) for my journey.
                2. Please check the available train numbers and their departure times.
                3. Please also check Beijing’s weather forecast for tomorrow.
                """, toolCallbackProvider);
    }


    /**
     * 示例18：解耦 Spring Boot 使用远端MCP工具 -- React Agent
     *
     * <p>不使用 Spring 依赖注入的情况下，直接使用 MCP 客户端
     * 获取远程工具，并将其转换为 Spring AI 的 ToolCallback，最后在 ReactAgent 中使用。</p>
     *
     * <p>关键步骤：</p>
     * <ol>
     *   <li>创建 MCP 客户端传输层 (HttpClientSseClientTransport)</li>
     *   <li>构建并初始化 MCP 同步客户端</li>
     *   <li>调用 listTools() 获取远程服务器的工具列表</li>
     *   <li>将 MCP 工具转换为 Spring AI 的 ToolCallback</li>
     * </ol>
     */
    public void remoteMcpToolsReactWithoutSpringBootExample() throws GraphRunnerException {
        ChatModel chatModel = getChatModel();

        // Get Remote MCP Server Endpoint Configuration
        String modelScope12306BaseUrlSse = System.getenv("MODEL_SCOPE_12306_BASE_URL");
        String modelScopeAmapBaseUrlSse = System.getenv("MODEL_SCOPE_AMAP_BASE_URL");

        // Create HTTP Client Builder (Reusable)
        HttpClient.Builder httpBuilder = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(60));

        List<ToolCallback> toolCallbacks = new ArrayList<>();
        List<McpSyncClient> clientsToClose = new ArrayList<>();

        try {
            // get MCP Tools
            List<ToolCallback> tools12306 = fetchMcpTools(
                    modelScope12306BaseUrlSse,
                    "sse",
                    httpBuilder,
                    clientsToClose,
                    "12306", false
            );
            toolCallbacks.addAll(tools12306);

            List<ToolCallback> toolsAmap = fetchMcpTools(
                    modelScopeAmapBaseUrlSse,
                    "mcp",
                    httpBuilder,
                    clientsToClose,
                    "amap", true
            );
            toolCallbacks.addAll(toolsAmap);
            //also you can build these toolCallback into a ToolCallbackProvider
            System.out.printf("""
                            ==============================Find the tools from MCP Servers==============================
                            Found %d Tools From MCP Servers
                            """,
                    toolCallbacks.size());
            // Build React Agent With MCP Tools
            executeAgent(chatModel, "You are a helpful assistant with travel route planning, train ticket search, and map services.", "travel_planning_session_no_spring", """
                    I plan to travel from Shanghai to Beijing tomorrow.
                    1. Please check at which stations I can alight (i.e., the available arrival/drop-off stations) for my journey.
                    2. Please check the available train numbers and their departure times.
                    3. Please also check Beijing's weather forecast for tomorrow.
                    4. Please help me find the route from Beijing South Station to Tiananmen Square.
                    """, null, toolCallbacks.toArray(new ToolCallback[0]));

        } catch (Exception e) {
            log.error("execute MCP Agent error", e);
        } finally {
            //close all MCP client
            for (McpSyncClient client : clientsToClose) {
                try {
                    if (client != null) {
                        client.close();
                        log.info("MCP Client Is Closed");
                    }
                } catch (Exception e) {
                    log.warn("Close MCP Client Error", e);
                }
            }
        }
    }

    /**
     * 从指定的 MCP 服务器获取工具并转换为 ToolCallback 列表
     *
     * @param baseUrl        MCP 服务器的基础 URL
     * @param endpoint       SSE 端点路径（如 "sse"）
     * @param httpBuilder    HTTP 客户端构建器
     * @param clientsToClose 需要关闭的客户端列表（用于资源管理）
     * @param serverName     服务器名称（用于日志和工具名称前缀）
     *
     * @return ToolCallback 列表
     */
    private List<ToolCallback> fetchMcpTools(
            String baseUrl,
            String endpoint,
            HttpClient.Builder httpBuilder,
            List<McpSyncClient> clientsToClose,
            String serverName,
            boolean isStreamable) {

        List<ToolCallback> toolCallbacks = new ArrayList<>();
        McpSyncClient mcpClient;

        try {
            //create MCP Transport
            McpClientTransport mcpClientTransport;
            if (isStreamable) {
                ObjectMapper objectMapper = new ObjectMapper();
                mcpClientTransport = HttpClientStreamableHttpTransport
                        .builder(baseUrl)
                        .endpoint(endpoint)
                        .clientBuilder(HttpClient.newBuilder())
                        .jsonMapper(new JacksonMcpJsonMapper(objectMapper)).build();

            }
            else {
                mcpClientTransport = HttpClientSseClientTransport.builder(baseUrl)
                        .clientBuilder(httpBuilder)
                        .sseEndpoint(endpoint)
                        .build();

            }
            //build mcp client
            mcpClient = McpClient.sync(mcpClientTransport)
                    .requestTimeout(Duration.ofSeconds(60))
                    .initializationTimeout(Duration.ofSeconds(60))
                    .capabilities(McpSchema.ClientCapabilities.builder().roots(true).build())
                    .build();

            clientsToClose.add(mcpClient);

            //initialize MCP client
            log.info("[{}] Initialize MCP Client...", serverName);
            McpSchema.InitializeResult initResult = mcpClient.initialize();
            log.info("[{}] MCP Client Initialize Successful: serverInfo={}, capabilities={}",
                    serverName, initResult.serverInfo(), initResult.capabilities());

            //get tools
            log.info("[{}] Get Tools...", serverName);
            McpSchema.ListToolsResult toolsResult = mcpClient.listTools();
            List<McpSchema.Tool> mcpTools = toolsResult.tools();

            log.info("[{}] Found {} Tools From MCP Server", serverName, mcpTools.size());

            //register to ToolCallback
            for (McpSchema.Tool mcpTool : mcpTools) {
                log.info("[{}] Register MCP Tool: name={}, description={}",
                        serverName, mcpTool.name(), mcpTool.description());
                ToolCallback toolCallback = createToolCallback(mcpTool, mcpClient, serverName);
                toolCallbacks.add(toolCallback);
            }
        } catch (Exception e) {
            log.error("[{}] Fetch MCP Tools Error", serverName, e);
        }
        return toolCallbacks;
    }

    /**
     * Convert MCP Tools to ToolCallback
     */
    private ToolCallback createToolCallback(McpSchema.Tool mcpTool, McpSyncClient mcpClient, String serverName) {
        return FunctionToolCallback.builder(
                        mcpTool.name(),
                        (Map<String, Object> functionInput) -> {
                            try {
                                //build tools request
                                log.debug("[{}] Call MCP Tool: {} With Input: {}",
                                        serverName, mcpTool.name(), functionInput);
                                McpSchema.CallToolRequest callRequest = new McpSchema.CallToolRequest(
                                        mcpTool.name(), functionInput);

                                // Call Tool
                                McpSchema.CallToolResult callResult = mcpClient.callTool(callRequest);

                                // get return
                                StringBuilder resultBuilder = new StringBuilder();
                                for (McpSchema.Content content : callResult.content()) {
                                    if (content instanceof McpSchema.TextContent textContent) {
                                        resultBuilder.append(textContent.text());
                                    }
                                }

                                String result = resultBuilder.toString();
                                log.debug("[{}] MCP Tool Return: {}", serverName, result);
                                return result;

                            } catch (Exception e) {
                                log.error("[{}] Call MCP Tool Failed: {}",
                                        serverName, mcpTool.name(), e);
                                return "{\"error\": \"" + e.getMessage() + "\"}";
                            }
                        })
                .description(mcpTool.description())
                .inputType(Map.class)
                .build();
    }


    private static void executeAgent(ChatModel chatModel, String instruction, String travel_planning_session, String message, ToolCallbackProvider toolCallbackProvider,
                                     ToolCallback... toolCallback) throws GraphRunnerException {
        //Run React Agent With MCP Tools
        Builder builder = ReactAgent.builder()
                .name("travel_planning_assistant")
                .model(chatModel)
                .description("Your Travel Assistant")
                .instruction(instruction)
                .saver(new MemorySaver());
        if (toolCallbackProvider != null) {
            builder.toolCallbackProviders(toolCallbackProvider);
        }
        else {
            builder.tools(toolCallback);
        }
        ReactAgent agent = builder.build();

        RunnableConfig config = RunnableConfig.builder()
                .threadId(travel_planning_session)
                .build();

        //stream
        Flux<NodeOutput> stream = agent.stream(message, config);
        StringBuffer answerString = new StringBuffer();
        stream.doOnNext(output -> {
                    if (output.node().equals("_AGENT_MODEL_")) {
                        answerString.append(((StreamingOutput<?>) output).message().getText());
                    }
                    else if (output.node().equals("_AGENT_TOOL_")) {
                        answerString.append("\nTool Call:").append(((ToolResponseMessage) ((StreamingOutput<?>) output).message()).getResponses().get(0)).append("\n");
                    }
                })
                .doOnComplete(() -> System.out.println(answerString))
                .doOnError(e -> System.err.println("Stream Processing Error: " + e.getMessage()))
                .blockLast();
    }


    /**
     * 示例19：基于 Spring Boot 使用远端MCP工具 -- Only ChatClient
     */
    public void remoteMcpToolsWithChatCliAndSpringBootExample() {
        ChatModel chatModel = getChatModel();
        ChatClient chatClient = ChatClient.builder(chatModel)
                .build();
        //Get Tools From Spring AI ToolCallbackProvider which the tools config in application.yml
        ToolCallback[] toolCallbacks = toolCallbackProvider.getToolCallbacks();
        System.out.printf("""
                        ==============================Find the tools from spring ToolCallbackProvider==============================
                        %s
                        """,
                JSON.toJSONString(toolCallbacks));
        ChatClient.ChatClientRequestSpec doChat =
                chatClient.prompt("You are a helpful assistant with travel route planning and train ticket search.")
                        .user("""
                                I plan to travel from Shanghai to Beijing tomorrow.
                                1. Please check at which stations I can alight (i.e., the available arrival/drop-off stations) for my journey.
                                2. Please check the available train numbers and their departure times.
                                3. Please also check Beijing’s weather forecast for tomorrow.
                                """)
                        .toolCallbacks(toolCallbackProvider);
        //check the logs from DefaultToolCallingManager
        String text = doChat.call().chatResponse().getResult().getOutput().getText();
        System.out.println(text);
    }


}
