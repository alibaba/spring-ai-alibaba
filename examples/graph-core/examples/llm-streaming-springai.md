---
title: Spring AI Alibaba LLM 流式集成
description: 学习如何在 Spring AI Alibaba Graph 中使用 LLM 流式输出功能
keywords: [LLM Streaming, 流式输出, Spring AI Alibaba, Graph, 流式响应, 实时响应]
---

# Spring AI Alibaba Graph 中的 LLM 流式输出


## 使用流式 ChatClient

Spring AI Alibaba 支持通过 `ChatClient` 进行流式输出。

<Code
  language="java"
  title="使用流式 ChatClient" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/examples/LlmStreamingSpringAiExample.java"
>
{`import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import reactor.core.publisher.Flux;

// 使用流式输出
Flux<ChatResponse> flux = chatClient.prompt()
        .user("tell me a joke")
        .stream()
        .chatResponse();

// 订阅流式响应
flux.subscribe(
        response -> {
            String content = response.getResult().getOutput().getText();
            System.out.print(content);
        },
        error -> System.err.println("Error: " + error.getMessage()),
        () -> System.out.println("\nStream completed")
);`}
</Code>

### 使用 Reactor 的阻塞式处理

<Code
  language="java"
  title="使用 Reactor 的阻塞式处理" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/examples/LlmStreamingSpringAiExample.java"
>
{`Flux<ChatResponse> flux = chatClient.prompt()
        .user("tell me a joke")
        .stream()
        .chatResponse();

// 使用 Reactor 的阻塞式处理
flux.collectList().block().forEach(response -> {
    System.out.println("Received: " + response.getResult().getOutput().getText());
});`}
</Code>

**输出示例**:
```
Sure, here's a joke for you:

Why don't scientists trust atoms?

Because they make up everything!
Stream completed
```

## 在 Graph 节点中使用流式输出
### 创建带流式输出的 Graph 节点

参考 [节点流式输出文档](../core/streaming) 获取完整示例。

<Code
  language="java"
  title="创建带流式输出的 Graph 节点" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/examples/LlmStreamingSpringAiExample.java"
>
{`import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.springframework.ai.chat.client.ChatClient;
import reactor.core.publisher.Flux;

import java.util.Map;

public class StreamingAgentNode implements NodeAction {

    private final ChatClient chatClient;

    public StreamingAgentNode(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        String userMessage = (String) state.value("query").orElse("Hello");

        // 使用流式输出
        Flux<String> contentFlux = chatClient.prompt()
                .user(userMessage)
                .stream()
                .content();

        return Map.of("answer", contentFlux);
    }
}`}
</Code>

### 配置和运行

<Code
  language="java"
  title="配置和运行流式 Graph" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/examples/LlmStreamingSpringAiExample.java"
>
{`import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import org.springframework.ai.chat.client.ChatClient;

// 配置 Graph
StateGraph graph = new StateGraph(keyStrategyFactory)
    .addNode("agent", new StreamingAgentNode(chatClientBuilder))
    .addEdge(StateGraph.START, "agent")
    .addEdge("agent", StateGraph.END);

CompiledGraph compiledGraph = graph.compile();

// 执行
Map<String, Object> input = Map.of("query", "Hello");
OverAllState result = compiledGraph.invoke(input);

System.out.println("Final result: " + result.value("answer").orElse(""));`}
</Code>

## 相关文档

- [节点流式输出](../core/streaming) - 完整的流式输出示例
- [快速入门](../quick-start) - Graph 基础使用
- [Spring AI 文档](https://docs.spring.io/spring-ai/reference/) - Spring AI 官方文档
