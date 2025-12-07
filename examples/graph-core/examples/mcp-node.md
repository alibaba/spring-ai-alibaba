---
title: 分配MCP工具给指定节点
keywords: [Spring AI, 通义千问, 百炼, 智能体应用, MCP, MCP工具, Node节点, 工具分配]
description: "在构建agent中，Graph中特定节点需要额外增加MCP提供的能力，需要分配指定的MCP给指定的node节点"
---

> 分配指定的 MCP 给指定的 node 节点

实战代码可见：[spring-ai-alibaba-examples](https://github.com/springaialibaba/spring-ai-alibaba-examples) 下的 graph 目录，本章代码为其 mcp-node 模块

### pom.xml

```xml

<dependencies>

    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-autoconfigure-model-openai</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-autoconfigure-model-chat-client</artifactId>
    </dependency>

    <dependency>
        <groupId>com.alibaba.cloud.ai</groupId>
        <artifactId>spring-ai-alibaba-graph-core</artifactId>
        <version>${spring-ai-alibaba.version}</version>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
</dependencies>
```

### application.yml

注意 spring.ai.graph.nodes 下的配置，node 配置对应 mcp 服务的映射

```yaml
server:
  port: 8080
spring:
  application:
    name: mcp-node
  ai:
    openai:
      api-key: ${AIDASHSCOPEAPIKEY}
      base-url: https://dashscope.aliyuncs.com/compatible-mode
      chat:
        options:
          model: qwen-max
    mcp:
      client:
        enabled: true
        name: my-mcp-client
        version: 1.0.0
        request-timeout: 30s
        type: ASYNC  # or ASYNC for reactive applications
        sse:
          connections:
            server1:
              url: http://localhost:19000

    graph:
      nodes:
        node2servers:
          mcp-node:
            - server1
```

### config

#### McpNodeProperties

node 配置对应 mcp 服务的映射类

<Code
  language="java"
  title="McpNodeProperties" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/examples/McpNodeExample.java"
>
{`package com.spring.ai.tutorial.graph.mcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;
import java.util.Set;

@ConfigurationProperties(prefix = McpNodeProperties.PREFIX)
public class McpNodeProperties {

    public static final String PREFIX = "spring.ai.graph.nodes";

    private Map<String, Set<String>> node2servers;

    public Map<String, Set<String>> getNode2servers() {
        return node2servers;
    }

    public void setNode2servers(Map<String, Set<String>> node2servers) {
        this.node2servers = node2servers;
    }
}`}
</Code>

#### McpGaphConfiguration

注入 McpClientToolCallbackProvider，提供给 McpNode

<Code
  language="java"
  title="McpGaphConfiguration" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/examples/McpNodeExample.java"
>
{`package com.spring.ai.tutorial.graph.mcp.config;

import com.alibaba.cloud.ai.graph.GraphRepresentation;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.spring.ai.tutorial.graph.mcp.node.McpNode;
import com.spring.ai.tutorial.graph.mcp.tool.McpClientToolCallbackProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;

import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.nodeasync;

@Configuration
@EnableConfigurationProperties({ McpNodeProperties.class })
public class McpGaphConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(McpGaphConfiguration.class);

    @Autowired
    private McpClientToolCallbackProvider mcpClientToolCallbackProvider;

    @Bean
    public StateGraph mcpGraph(ChatClient.Builder chatClientBuilder) throws GraphStateException {
        KeyStrategyFactory keyStrategyFactory = () -> {
            HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();

            // 用户输入
            keyStrategyHashMap.put("query", new ReplaceStrategy());
            keyStrategyHashMap.put("mcpcontent", new ReplaceStrategy());
            return keyStrategyHashMap;
        };

        StateGraph stateGraph = new StateGraph(keyStrategyFactory)
                .addNode("mcp", nodeasync(new McpNode(chatClientBuilder, mcpClientToolCallbackProvider)))

                .addEdge(StateGraph.START, "mcp")
                .addEdge("mcp", StateGraph.END);

        // 添加 PlantUML 打印
        GraphRepresentation representation = stateGraph.getGraph(GraphRepresentation.Type.PLANTUML,
                "mcp flow");
        logger.info("\n=== mcp UML Flow ===");
        logger.info(representation.content());
        logger.info("==================================\n");

        return stateGraph;
    }
}`}
</Code>

### tool

#### McpClientToolCallbackProvider

根据节点名称，匹配对应的 MCP 提供的 ToolCallback

<Code
  language="java"
  title="McpClientToolCallbackProvider" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/examples/McpNodeExample.java"
>
{`package com.spring.ai.tutorial.graph.mcp.tool;

import com.spring.ai.tutorial.graph.mcp.config.McpNodeProperties;
import org.apache.commons.compress.utils.Lists;
import org.glassfish.jersey.internal.guava.Sets;
import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.mcp.client.autoconfigure.properties.McpClientCommonProperties;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class McpClientToolCallbackProvider {

    private final ToolCallbackProvider toolCallbackProvider;

    private final McpClientCommonProperties commonProperties;

    private final McpNodeProperties mcpNodeProperties;

    public McpClientToolCallbackProvider(ToolCallbackProvider toolCallbackProvider,
                                         McpClientCommonProperties commonProperties, McpNodeProperties mcpNodeProperties) {
        this.toolCallbackProvider = toolCallbackProvider;
        this.commonProperties = commonProperties;
        this.mcpNodeProperties = mcpNodeProperties;
    }

    public Set<ToolCallback> findToolCallbacks(String nodeName) {
        Set<ToolCallback> defineCallback = Sets.newHashSet();
        Set<String> mcpClients = mcpNodeProperties.getNode2servers().get(nodeName);
        if (mcpClients == null || mcpClients.isEmpty()) {
            return defineCallback;
        }

        List<String> exceptMcpClientNames = Lists.newArrayList();
        for (String mcpClient : mcpClients) {
            // my-mcp-client
            String name = commonProperties.getName();
            // mymcpclientserver1
            String prefixedMcpClientName = McpToolUtils.prefixedToolName(name, mcpClient);
            exceptMcpClientNames.add(prefixedMcpClientName);
        }

        ToolCallback[] toolCallbacks = toolCallbackProvider.getToolCallbacks();
        for (ToolCallback toolCallback : toolCallbacks) {
            ToolDefinition toolDefinition = toolCallback.getToolDefinition();
            // mymcpclientserver1getCityTimeMethod
            String name = toolDefinition.name();
            for (String exceptMcpClientName : exceptMcpClientNames) {
                if (name.startsWith(exceptMcpClientName)) {
                    defineCallback.add(toolCallback);
                }
            }
        }
        return defineCallback;
    }
}`}
</Code>

### node

#### McpNode

通过 ToolCallbacks 为节点配置 MCP 工具

<Code
  language="java"
  title="McpNode" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/examples/McpNodeExample.java"
>
{`import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import reactor.core.publisher.Flux;

/**
 * MCP 节点实现
 */
public static class McpNode implements NodeAction {

    private static final String NODENAME = "mcp-node";

    private final ChatClient chatClient;

    public McpNode(ChatClient.Builder chatClientBuilder, Set<ToolCallback> toolCallbacks) {
        // 为节点配置 MCP 工具
        this.chatClient = chatClientBuilder
                .defaultToolCallbacks(toolCallbacks.toArray(ToolCallback[]::new))
                .build();
    }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        String query = state.value("query", "");
        Flux<String> streamResult = chatClient.prompt(query).stream().content();
        String result = streamResult.reduce("", (acc, item) -> acc + item).block();

        HashMap<String, Object> resultMap = new HashMap<>();
        resultMap.put("mcpcontent", result);

        return resultMap;
    }
}`}
</Code>

#### 配置 MCP 节点

<Code
  language="java"
  title="配置 MCP 节点" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/examples/McpNodeExample.java"
>
{`import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;

import java.util.Set;

/**
 * 配置 MCP 节点
 */
public static void configureMcpNode(ChatClient.Builder chatClientBuilder, Set<ToolCallback> toolCallbacks) {
    McpNode mcpNode = new McpNode(chatClientBuilder, toolCallbacks);
    System.out.println("MCP node configured successfully");
}`}
</Code>

### controller

#### McpController

<Code
  language="java"
  title="McpController" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/examples/McpNodeExample.java"
>
{`package com.spring.ai.tutorial.graph.mcp.controller;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/graph/mcp")
public class McpController {

    private static final Logger logger = LoggerFactory.getLogger(McpController.class);

    private final CompiledGraph compiledGraph;

    public McpController(@Qualifier("mcpGraph") StateGraph stateGraph) throws GraphStateException {
        this.compiledGraph = stateGraph.compile();
    }

    @GetMapping("/call")
    public Map<String, Object> call(@RequestParam(value = "query", defaultValue = "北京时间现在几点钟", required = false) String query,
                                      @RequestParam(value = "threadid", defaultValue = "yingzi", required = false) String threadId){
        RunnableConfig runnableConfig = RunnableConfig.builder().threadId(threadId).build();
        Map<String, Object> objectMap = new HashMap<>();
        objectMap.put("query", query);
        Optional<OverAllState> invoke = this.compiledGraph.invoke(objectMap, runnableConfig);
        return invoke.map(OverAllState::data).orElse(new HashMap<>());
    }

}`}
</Code>

### MCP Server 服务提供

提供一个 MCP Server 服务，远端 or 本地都可以

这里本地启动一个MCP Server，提供一个时间服务

### 效果

启动本地 MCP Server，提供时间服务
![](/img/user/ai/tutorials/graph/NivBbKqXooFsgexeeRrcIV0rnEf.png)

调用接口，触发本地端 MCP Server 提供的时间服务
![](/img/user/ai/tutorials/graph/LMPkbZ5heoHs2Qx604kcdiplnlc.png)
