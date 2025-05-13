---
title: MCP 协议重大升级，Spring AI Alibaba 联合 Higress 发布业界首个 Stramable HTTP 实现方案
keywords: [Model Context Protocol, MCP, Spring Ai, Claude, OpenManus]
description: MCP协议引入StreamableHTTP传输层，从根本改变原有HTTP+SSE机制设计。文章详解其设计思想、技术细节及Spring AI Alibaba开源框架提供的 Streamable HTTP Client 实现，提供Spring AI Alibaba+Higress完整示例。
author: 刘军
date: "2025-04-20"
category: article
---

## 文章摘要
MCP 官方引入了全新的 `[**Streamable HTTP**](https://modelcontextprotocol.io/specification/2025-03-26/basic/transports#streamable-http)` 传输层，对原有 HTTP+SSE 传输机制有重大改进。

本文将：
1. 详细解析这个协议的设计思想、技术细节以及实际应用。
2. 详解 Spring AI Alibaba 开源框架提供的 Stramable HTTP Java 实现 ，文后包含 Spring AI Alibaba + Higress 的 Streamable HTTP 示例讲解。


相关项目链接如下：

+ 完整可运行示例： [https://github.com/springaialibaba/spring-ai-alibaba-examples](https://github.com/springaialibaba/spring-ai-alibaba-examples/tree/main/spring-ai-alibaba-mcp-example/starter-example/client/starter-streamable-client)
+ Spring AI Alibaba 官网博客文章：[https://java2ai.com/](https://java2ai.com/)
+ Spring AI Alibaba 开源项目地址：[https://github.com/alibaba/spring-ai-alibaba](https://github.com/alibaba/spring-ai-alibaba)
+ Higress 官网地址：[https://higress.ai/](https://higress.ai/)

## <font style="color:rgb(2, 8, 23);">HTTP+SSE 原理及缺陷</font>

![MCP HTTP+SSE 工作原理](/img/blog/mcp-streamable-http/img.png)

<font style="color:rgb(2, 8, 23);">在原有的 MCP 实现中，客户端和服务器通过两个主要通道通信：</font>

+ **<font style="color:rgb(2, 8, 23);">HTTP 请求/响应</font>**<font style="color:rgb(2, 8, 23);">：客户端通过标准 HTTP 请求发送消息到服务器</font>
+ **<font style="color:rgb(2, 8, 23);">服务器发送事件(SSE)</font>**<font style="color:rgb(2, 8, 23);">：服务器通过专门的 </font>**<font style="color:rgb(2, 8, 23);background-color:#f7f7f7;">/sse</font>**<font style="color:rgb(2, 8, 23);"> 端点向客户端推送消息</font>

### **<font style="color:rgb(2, 8, 23);">主要问题</font>**
<font style="color:rgb(2, 8, 23);">这种设计虽然简单直观，但存在几个关键问题：</font>

**<font style="color:rgb(2, 8, 23);">不支持断线重连/恢复</font>**

<font style="color:rgb(2, 8, 23);">当 SSE 连接断开时，所有会话状态丢失，客户端必须重新建立连接并初始化整个会话。例如，正在执行的大型文档分析任务会因 WiFi 不稳定而完全中断，迫使用户重新开始整个过程。</font>

**<font style="color:rgb(2, 8, 23);">服务器需维护长连接</font>**

<font style="color:rgb(2, 8, 23);">服务器必须为每个客户端维护一个长时间的 SSE 连接，大量并发用户会导致资源消耗剧增。当服务器需要重启或扩容时，所有连接都会中断，影响用户体验和系统可靠性。</font>

**<font style="color:rgb(2, 8, 23);">服务器消息只能通过 SSE 传递</font>**

<font style="color:rgb(2, 8, 23);">即使是简单的请求-响应交互，服务器也必须通过 SSE 通道返回信息，造成不必要的复杂性和开销。对于某些环境（如云函数）不适合长时间保持 SSE 连接。</font>

**<font style="color:rgb(2, 8, 23);">基础设施兼容性限制</font>**

<font style="color:rgb(2, 8, 23);">许多现有的 Web 基础设施如 CDN、负载均衡器、API 网关等可能不能正确处理长时间的 SSE 连接，企业防火墙可能会强制关闭超时连接，导致服务不可靠。</font>

## **<font style="color:rgb(2, 8, 23);">Streamable HTTP 原理与改进</font>**
### **<font style="color:rgb(2, 8, 23);">Streamable 关键改进</font>**
<font style="color:rgb(2, 8, 23);">相比原有 HTTP+SSE 机制，Streamable HTTP 引入了几项关键改进：</font>

1. **<font style="color:rgb(2, 8, 23);">统一 Endoint</font>**<font style="color:rgb(2, 8, 23);">：移除专门的 </font>**<font style="color:rgb(2, 8, 23);background-color:#f7f7f7;">/sse</font>**<font style="color:rgb(2, 8, 23);"> 端点，所有通信通过单一端点（当前官方 sdk 实现为 </font>**<font style="color:rgb(2, 8, 23);background-color:#f7f7f7;">/mcp</font>**<font style="color:rgb(2, 8, 23);">）进行</font>
2. **<font style="color:rgb(2, 8, 23);">按需流式传输</font>**<font style="color:rgb(2, 8, 23);">：服务器可灵活选择是返回普通 HTTP 响应还是升级为 SSE 流</font>
3. **<font style="color:rgb(2, 8, 23);">会话标识</font>**<font style="color:rgb(2, 8, 23);">：引入会话 ID 机制，支持状态管理和恢复</font>
4. **<font style="color:rgb(2, 8, 23);">灵活初始化</font>**<font style="color:rgb(2, 8, 23);">：客户端可通过空 GET 请求主动初始化 SSE 流</font>

### **<font style="color:rgb(2, 8, 23);">Streamable </font>**<font style="color:rgb(2, 8, 23);">工作原理</font>
<font style="color:rgb(2, 8, 23);">Streamable HTTP 的工作流程如下：</font>

1. **<font style="color:rgb(2, 8, 23);">会话初始化（非强制，适用于有状态实现场景）</font>**<font style="color:rgb(2, 8, 23);">：</font>
    - <font style="color:rgb(2, 8, 23);">客户端发送初始化请求到 </font>**<font style="color:rgb(2, 8, 23);background-color:#f7f7f7;">/mcp</font>**<font style="color:rgb(2, 8, 23);"> 端点</font>
    - <font style="color:rgb(2, 8, 23);">服务器可选择生成会话 ID 返回给客户端</font>
    - <font style="color:rgb(2, 8, 23);">会话 ID 用于后续请求中标识会话</font>
2. **<font style="color:rgb(2, 8, 23);">客户端向服务器通信</font>**<font style="color:rgb(2, 8, 23);">：</font>
    - <font style="color:rgb(2, 8, 23);">所有消息通过 HTTP POST 请求发送到 </font>**<font style="color:rgb(2, 8, 23);background-color:#f7f7f7;">/mcp</font>**<font style="color:rgb(2, 8, 23);"> 端点</font>
    - <font style="color:rgb(2, 8, 23);">如果有会话 ID，则包含在请求中</font>
3. **<font style="color:rgb(2, 8, 23);">服务器响应方式</font>**<font style="color:rgb(2, 8, 23);">：</font>
    - **<font style="color:rgb(2, 8, 23);">普通响应</font>**<font style="color:rgb(2, 8, 23);">：直接返回 HTTP 响应，适合简单交互</font>
    - **<font style="color:rgb(2, 8, 23);">流式响应</font>**<font style="color:rgb(2, 8, 23);">：升级连接为 SSE，发送一系列事件后关闭</font>
    - **<font style="color:rgb(2, 8, 23);">长连接</font>**<font style="color:rgb(2, 8, 23);">：维持 SSE 连接持续发送事件</font>
4. **<font style="color:rgb(2, 8, 23);">主动建立 SSE 流</font>**<font style="color:rgb(2, 8, 23);">：</font>
    - <font style="color:rgb(2, 8, 23);">客户端可发送 GET 请求到 </font>**<font style="color:rgb(2, 8, 23);background-color:#f7f7f7;">/mcp</font>**<font style="color:rgb(2, 8, 23);"> 端点主动建立 SSE 流</font>
    - <font style="color:rgb(2, 8, 23);">服务器可通过该流推送通知或请求</font>
5. **<font style="color:rgb(2, 8, 23);">连接恢复</font>**<font style="color:rgb(2, 8, 23);">：</font>
    - <font style="color:rgb(2, 8, 23);">连接中断时，客户端可使用之前的会话 ID 重新连接</font>
    - <font style="color:rgb(2, 8, 23);">服务器可恢复会话状态继续之前的交互</font>

### <font style="color:rgb(2, 8, 23);">Streamable 请求示例</font>
### **<font style="color:rgb(2, 8, 23);">无状态服务器模式</font>**
**<font style="color:rgb(2, 8, 23);">场景</font>**<font style="color:rgb(2, 8, 23);">：简单工具 API 服务，如数学计算、文本处理等。</font>

**<font style="color:rgb(2, 8, 23);">实现</font>**<font style="color:rgb(2, 8, 23);">：</font>

```plain
客户端                                 服务器
   |                                    |
   |-- POST /message (计算请求) -------->|
   |                                    |-- 执行计算
   |<------- HTTP 200 (计算结果) -------|
   |                                    |
```

**<font style="color:rgb(2, 8, 23);">优势</font>**<font style="color:rgb(2, 8, 23);">：极简部署，无需状态管理，适合无服务器架构和微服务。</font>

### **<font style="color:rgb(2, 8, 23);">流式进度反馈模式</font>**
**<font style="color:rgb(2, 8, 23);">场景</font>**<font style="color:rgb(2, 8, 23);">：长时间运行的任务，如大文件处理、复杂 AI 生成等。</font>

**<font style="color:rgb(2, 8, 23);">实现</font>**<font style="color:rgb(2, 8, 23);">：</font>

```plain
客户端                                 服务器
   |                                    |
   |-- POST /message (处理请求) -------->|
   |                                    |-- 启动处理任务
   |<------- HTTP 200 (SSE开始) --------|
   |                                    |
   |<------- SSE: 进度10% ---------------|
   |<------- SSE: 进度30% ---------------|
   |<------- SSE: 进度70% ---------------|
   |<------- SSE: 完成 + 结果 ------------|
   |                                    |
```

**<font style="color:rgb(2, 8, 23);">优势</font>**<font style="color:rgb(2, 8, 23);">：提供实时反馈，但不需要永久保持连接状态。</font>

### **<font style="color:rgb(2, 8, 23);">复杂 AI 会话模式</font>**
**<font style="color:rgb(2, 8, 23);">场景</font>**<font style="color:rgb(2, 8, 23);">：多轮对话 AI 助手，需要维护上下文。</font>

**<font style="color:rgb(2, 8, 23);">实现</font>**<font style="color:rgb(2, 8, 23);">：</font>

```plain
客户端                                 服务器
   |                                    |
   |-- POST /message (初始化) ---------->|
   |<-- HTTP 200 (会话ID: abc123) ------|
   |                                    |
   |-- GET /message (会话ID: abc123) --->|
   |<------- SSE流建立 -----------------|
   |                                    |
   |-- POST /message (问题1, abc123) --->|
   |<------- SSE: 思考中... -------------|
   |<------- SSE: 回答1 ----------------|
   |                                    |
   |-- POST /message (问题2, abc123) --->|
   |<------- SSE: 思考中... -------------|
   |<------- SSE: 回答2 ----------------|
```

**<font style="color:rgb(2, 8, 23);">优势</font>**<font style="color:rgb(2, 8, 23);">：维护会话上下文，支持复杂交互，同时允许水平扩展。</font>

### **<font style="color:rgb(2, 8, 23);">断线恢复模式</font>**
**<font style="color:rgb(2, 8, 23);">场景</font>**<font style="color:rgb(2, 8, 23);">：不稳定网络环境下的 AI 应用使用。</font>

**<font style="color:rgb(2, 8, 23);">实现</font>**<font style="color:rgb(2, 8, 23);">：</font>

```plain
客户端                                 服务器
   |                                    |
   |-- POST /message (初始化) ---------->|
   |<-- HTTP 200 (会话ID: xyz789) ------|
   |                                    |
   |-- GET /message (会话ID: xyz789) --->|
   |<------- SSE流建立 -----------------|
   |                                    |
   |-- POST /message (长任务, xyz789) -->|
   |<------- SSE: 进度30% ---------------|
   |                                    |
   |     [网络中断]                      |
   |                                    |
   |-- GET /message (会话ID: xyz789) --->|
   |<------- SSE流重新建立 --------------|
   |<------- SSE: 进度60% ---------------|
   |<------- SSE: 完成 ------------------|
```

**<font style="color:rgb(2, 8, 23);">优势</font>**<font style="color:rgb(2, 8, 23);">：提高弱网环境下的可靠性，改善用户体验。</font>

## Spring AI Alibaba 社区的 Streamable HTTP 实现
在前文中，我们从个理论上列举了 `HTTP+SSE`与 `Streamable`两种模式各自的优劣。在实际落地应用中，由于`HTTP+SSE`模式割裂的请求与响应模式，导致它在架构实现与扩展性上存在一个非常棘手的问题：它强制要求在 client 与 server 之间维持粘性会话连接（Sticky Session），即使只是维持无状态 Stateless 通信，我们也需要维护一个 session id，并确保具备相同 session id 的请求发送到相同的服务端机器，这对于 client、server 端实现都是非常重的负担。

对于 Streamable 模式，如果只是要维持 Stateless 通信的话，是完全不需要维护 sticky session 的。而考虑到 90% 以上的 MCP 服务可能都是无状态的，这对于整体架构的可扩展性是一个非常大的提升。

当然，如果要实现 Stateful 通信的话，Streamable HTTP 模式同样也还是需要维护 session id 的。

### Streamable HTTP Java 版实现方案
当前 MCP 和 Spring AI 官方并没有给出Streamable 目前我们只给出了 Stream HTTP Client实现，且只支持 Stateless 模式，可以调通官方的 Typescript server 实现、Higress 社区的 server 实现。



> 完整可运行示例可参考： [https://github.com/springaialibaba/spring-ai-alibaba-examples](https://github.com/springaialibaba/spring-ai-alibaba-examples/tree/main/spring-ai-alibaba-mcp-example/starter-example/client/starter-streamable-client)
>
> <font style="color:rgb(31, 35, 40);">
</font><font style="color:rgb(31, 35, 40);">由于 </font>[<font style="color:rgb(9, 105, 218);">streamable http</font>](https://modelcontextprotocol.io/specification/2025-03-26/basic/transports#streamable-http)<font style="color:rgb(31, 35, 40);"> 方案的 MCP java sdk 实现还在开发中，因此该示例仓库中包含如下两个仓库的定制源码：</font>
>
> 1. [<font style="color:rgb(9, 105, 218);">MCP java-sdk</font>](https://github.com/modelcontextprotocol/java-sdk/)<font style="color:rgb(31, 35, 40);">，在项目</font><font style="color:rgb(31, 35, 40);"> </font>`<font style="color:rgb(31, 35, 40);">io.modelcontextprotocol</font>`<font style="color:rgb(31, 35, 40);"> </font><font style="color:rgb(31, 35, 40);">包。</font>
> 2. [<font style="color:rgb(9, 105, 218);">Spring AI</font>](https://github.com/spring-projects/spring-ai/)<font style="color:rgb(31, 35, 40);">，在项目</font><font style="color:rgb(31, 35, 40);"> </font>`<font style="color:rgb(31, 35, 40);">org.springframework.ai.mcp.client.autoconfigure</font>`<font style="color:rgb(31, 35, 40);"> </font><font style="color:rgb(31, 35, 40);">包。</font>
>
> <font style="color:rgb(31, 35, 40);">示例集成了支持 MCP Streamable HTTP 协议实现的 Higress 网关，该实现还有很多限制，如不支持 GET 请求、不支持 session-id 管理等。</font>
>

### 新增 StreamHttpClientTransport
### GET 请求（空请求体），主动建立 SSE 连接
客户端可以通过主动发送 GET 请求到 /mcp 端点建立 SSE 连接，并用做后续请求响应通道。

```java
return Mono.defer(() -> Mono.fromFuture(() -> {
        final HttpRequest.Builder builder = requestBuilder.copy().GET().uri(uri);
        final String lastId = lastEventId.get();
        if (lastId != null) {
            builder.header("Last-Event-ID", lastId);
        }
        return httpClient.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofInputStream());
    }).flatMap(response -> {
        if (response.statusCode() == 405 || response.statusCode() == 404) {
            // .....
        }
        return handleStreamingResponse(response, handler);
    })
    .retryWhen(Retry.backoff(3, Duration.ofSeconds(3)).filter(err -> err instanceof IllegalStateException))
    .doOnSuccess(v -> state.set(TransportState.CONNECTED))
    .doOnTerminate(() -> state.set(TransportState.CLOSED))
    .onErrorResume(e -> {
        System.out.println("Ignore GET connection error.");
        LOGGER.error("Streamable transport connection error", e);
        state.set(TransportState.CONNECTED);
        return Mono.just("Streamable transport connection error").then();
    }));
```



### POST 请求，服务端可以普通 response 响应或者升级 SSE 响应
对等的示例 http 请求，listTool 与 callTool 是类似的请求。

```shell
curl -X POST -H "Content-Type: application/json" -H "Accept: application/json" -H "Accept: text/event-stream"     -d '{
  "jsonrpc" : "2.0",
  "method" : "initialize",
  "id" : "9afdedcc-0",
  "params" : {
    "protocolVersion" : "2024-11-05",
    "capabilities" : {
      "roots" : {
        "listChanged" : true
      }
    },
    "clientInfo" : {
      "name" : "Java SDK MCP Client",
      "version" : "1.0.0"
    }
  }
}' -i http://localhost:3000/mcp
```



> 可以启动并使用官方 [typescript-sdk](https://github.com/modelcontextprotocol/typescript-sdk/tree/main/src/examples) 提供的 Streamable Server 配合当前 client 实现进行测试。
>



Java 代码实现

```java
// 发送 POST 请求到 /mcp，包括
public Mono<Void> sendMessage(final McpSchema.JSONRPCMessage message,
			final Function<Mono<McpSchema.JSONRPCMessage>, Mono<McpSchema.JSONRPCMessage>> handler) {
    // ...
    return sentPost(message, handler).onErrorResume(e -> {
        LOGGER.error("Streamable transport sendMessage error", e);
        return Mono.error(e);
    });
}

// 实际发送 POST 请求并处理响应
private Mono<Void> sentPost(final Object msg,
        final Function<Mono<McpSchema.JSONRPCMessage>, Mono<McpSchema.JSONRPCMessage>> handler) {
    return serializeJson(msg).flatMap(json -> {
        final HttpRequest request = requestBuilder.copy()
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .uri(uri)
            .build();
        return Mono.fromFuture(httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream()))
            .flatMap(response -> {
                // If the response is 202 Accepted, there's no body to process
                if (response.statusCode() == 202) {
                    return Mono.empty();
                }

                if (response.statusCode() == 405 || response.statusCode() == 404) {
                 // ...
                }

                if (response.statusCode() >= 400) {
                 // ...
                }

                return handleStreamingResponse(response, handler);
            });
    });

}

// 处理服务端可能发回的不同类型响应
private Mono<Void> handleStreamingResponse(final HttpResponse<InputStream> response,
			final Function<Mono<McpSchema.JSONRPCMessage>, Mono<McpSchema.JSONRPCMessage>> handler) {
    final String contentType = response.headers().firstValue("Content-Type").orElse("");
    if (contentType.contains("application/json-seq")) {
        return handleJsonStream(response, handler);
    }
    else if (contentType.contains("text/event-stream")) {
        return handleSseStream(response, handler);
    }
    else if (contentType.contains("application/json")) {
        return handleSingleJson(response, handler);
    }
    else {
        return Mono.error(new UnsupportedOperationException("Unsupported Content-Type: " + contentType));
    }
}
```



### 集成到 Spring AI 框架
```java
@AutoConfiguration
@ConditionalOnClass({ McpSchema.class, McpSyncClient.class })
@EnableConfigurationProperties({ McpStreamableClientProperties.class, McpClientCommonProperties.class })
@ConditionalOnProperty(prefix = McpClientCommonProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true",
		matchIfMissing = true)
public class StreamableHttpClientTransportAutoConfiguration {
	@Bean
	public List<NamedClientMcpTransport> mcpHttpClientTransports(McpStreamableClientProperties streamableProperties,
			ObjectProvider<ObjectMapper> objectMapperProvider) {

		ObjectMapper objectMapper = objectMapperProvider.getIfAvailable(ObjectMapper::new);

		List<NamedClientMcpTransport> sseTransports = new ArrayList<>();

		for (Map.Entry<String, McpStreamableClientProperties.StreamableParameters> serverParameters : streamableProperties.getConnections().entrySet()) {

			var transport = StreamableHttpClientTransport.builder(serverParameters.getValue().url()).withObjectMapper(objectMapper).build();
			sseTransports.add(new NamedClientMcpTransport(serverParameters.getKey(), transport));
		}

		return sseTransports;
	}

}
```



### 完整 Spring AI Alibaba + Higress Streamable HTTP 示例
通过配置如下，可以开启 Streamable HTTP Transport。配置如下 Higress 提供的 MCP Server 地址（支持有限的 Streamable HTTP Server 实现）。

```yaml
spring:
  ai:
    mcp:
      client:
        toolcallback:
          enabled: true
        streamable:
          connections:
            server1:
              url: http://env-cvpjbjem1hkjat42sk4g-ap-southeast-1.alicloudapi.com/mcp-quark
```



```java
@SpringBootApplication(exclude = {
        org.springframework.ai.mcp.client.autoconfigure.SseHttpClientTransportAutoConfiguration.class,
})
@ComponentScan(basePackages = "org.springframework.ai.mcp.client")
public class Application {
    @Bean
    public CommandLineRunner predefinedQuestions(ChatClient.Builder chatClientBuilder, ToolCallbackProvider tools,
            ConfigurableApplicationContext context) {
        return args -> {
            var chatClient = chatClientBuilder
                    .defaultTools(tools)
                    .build();

            System.out.println("\n>>> QUESTION: " + "阿里巴巴西溪园区");
            System.out.println("\n>>> ASSISTANT: " + chatClient.prompt("阿里巴巴西溪园区").call().content());

            System.out.println("\n>>> QUESTION: " + "黄金价格走势");
            System.out.println("\n>>> ASSISTANT: " + chatClient.prompt("黄金价格走势").call().content());

        };
    }
}
```



运行示例后，看到成功连接 MCP Server 并执行 list tool，Higress 示例内置了两个 tool。

```json
{
	"jsonrpc": "2.0",
	"id": "32124bd9-1",
	"result": {
		"nextCursor": "",
		"tools": [{
			"description": "Performs a web search using the Quark Search API, ideal for general queries, news, articles, and online content.\nUse this for broad information gathering, recent events, or when you need diverse web sources.\nBecause Quark search performs poorly for English searches, please use Chinese for the query parameters.",
			"inputSchema": {
				"additionalProperties": false,
				"properties": {
					"contentMode": {
						"default": "summary",
						"description": "Return the level of content detail, choose to use summary or full text",
						"enum": ["full", "summary"],
						"type": "string"
					},
					"number": {
						"default": 5,
						"description": "Number of results",
						"type": "integer"
					},
					"query": {
						"description": "Search query, please use Chinese",
						"examples": ["黄金价格走势"],
						"type": "string"
					}
				},
				"required": ["query"],
				"type": "object"
			},
			"name": "web_search"
		}]
	}
}
```



示例发起 chat 会话，模型会引导智能体调用 `web_search` tool 并返回结果。

## 当前实现限制
当前只是在官方 java sdk 的基础上新增了 Streamable HTTP 模式的 McpClientTransport 实现，但其实这种改造方法没办法很好的完全支持 Streamable HTTP，因为它的工作流程在很多地方和 HTTP+SSE 不一致，而之前的 java sdk 很多流程是强绑定 HTTP+SSE 模式设计的，导致当前的 sdk 实现上需要做一些结构化修改。

比如，有如下几点当前的实现就是受限的：

1. Initialization 过程在 Streamable HTTP 中并不是必须的，只有实现有状态管理时才需要，并且Initializated之后，后续的所有请求都需要带上 mcp-session-id。而当前的 java-sdk 设计是强制检查Initialization状态，并且在initiialization后没有管理mcp-session-id
2. /mcp GET 请求在协议中是被约束为 client 主动发起 SSE 请求时使用，而当前实现是在每次connect时发起 GET 请求并建立 SSE 会话，后续的 POST 请求通过也依赖这里发回响应，这可以通过 pendingResponses 属性的操作看出来。

## 官方实现与参考资料
1. Spring AI Alibaba 官方网站：[https://java2ai.com/](https://java2ai.com/)
2. Spring AI Alibaba 开源项目源码仓库：[https://github.com/alibaba/spring-ai-alibaba](https://github.com/alibaba/spring-ai-alibaba)
3. [https://www.claudemcp.com/blog/mcp-streamable-http](https://www.claudemcp.com/blog/mcp-streamable-http)
4. [https://github.com/modelcontextprotocol/java-sdk](https://github.com/modelcontextprotocol/java-sdk)




