---
title: 流式输出
description: 使用 Spring AI Alibaba Graph 框架实现工作流的流式输出，支持节点级别和 LLM 级别的流式处理
keywords: [Spring AI Alibaba, Graph, 流式输出, AsyncGenerator, Streaming, 节点流式]
---

# 流式输出

Spring AI Alibaba Graph 内置了对流式处理的原生支持，框架统一是使用 Flux 来在框架中定义和传递流，与 Spring 生态的流式处理保持一致。以下是从 Graph 运行中流式返回输出的不同方式。

## 调用 Graph 的流式输出

`.stream()` 是一个用于从图运行中流式返回输出的方法。它返回一个 Flux，请记住由于 Flux 流式的特性，流返回后并不会立即出触发图引擎的执行，你需要执行类似 Flux.subscribe() 的操作才能真正启动流引擎。

目前 Flux 返回的是 `NodeOutput` 类的实例，该类基本上报告执行的**节点名称**和结果**状态**。

### 流的组合（嵌入和组合）

Flux 支持多个流的合并、转换、组合等操作，具备非常强大的能力，这在处理图中多个流式节点时会非常有用。具体使用方式可搜索 Spring Reactor 学习。

## 理解

## 在节点操作中整合流式输出

在 Spring AI Alibaba Graph 中，您可以在节点操作中直接返回 `Flux` 对象，框架会自动处理流式输出。

### 流式节点实现

<Code
  language="java"
  title="流式节点实现" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/core/StreamingExample.java"
>
{`import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;

import java.util.Map;

import reactor.core.publisher.Flux;

/**
 * 流式节点实现 - 使用 GraphFluxGenerator 处理流式响应
 */
public static class StreamingNode implements NodeAction {

    private final ChatClient chatClient;
    private final String nodeId;

    public StreamingNode(ChatClient.Builder chatClientBuilder, String nodeId) {
        this.chatClient = chatClientBuilder.build();
        this.nodeId = nodeId;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        String query = (String) state.value("query").orElse("");

        // 获取流式响应
        Flux<ChatResponse> chatResponseFlux = chatClient.prompt()
                .user(query)
                .stream()
                .chatResponse();

        // 将流式响应存储在状态中
        return Map.of("messages", chatResponseFlux);
    }
}`}
</Code>

### 处理流式输出的节点

<Code
  language="java"
  title="处理流式输出的节点" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/core/StreamingExample.java"
>
{`import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;

import java.util.Map;

/**
 * 处理流式输出的节点 - 接收并处理流式响应
 */
public static class ProcessStreamingNode implements NodeAction {

    @Override
    public Map<String, Object> apply(OverAllState state) {
         // 请注意，虽然上一个节点返回的是Flux对象，但是在引擎运行到当前节点时，
		// 框架已经完成了对上一个节点Flux对象的自动订阅与消费，并将最终的结果汇总后添加到了 messages key 中（基于 AppendStrategy）
		Object messages = state.value("messages").orElse("");
		String result = "流式响应已处理完成: " + messages;
		return Map.of("result", result);
    }
}`}
</Code>

### 完整示例：使用流式输出的图

<Code
  language="java"
  title="使用流式输出的完整示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/core/StreamingExample.java"
>
{`import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;

import org.springframework.ai.chat.client.ChatClient;

import java.util.HashMap;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;

/**
 * 使用 StateGraph 实现流式输出的完整示例
 */
public static void streamLLMTokens(ChatClient.Builder chatClientBuilder) throws GraphStateException {
    // 定义状态策略
    KeyStrategyFactory keyStrategyFactory = () -> {
        Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
        keyStrategyMap.put("query", new AppendStrategy());
        keyStrategyMap.put("messages", new AppendStrategy());
        keyStrategyMap.put("result", new AppendStrategy());
        return keyStrategyMap;
    };

    // 创建流式节点
    StreamingNode streamingNode = new StreamingNode(chatClientBuilder, "streaming_node");

    // 创建处理节点
    ProcessStreamingNode processNode = new ProcessStreamingNode();

    // 构建图
    StateGraph stateGraph = new StateGraph(keyStrategyFactory)
            .addNode("streaming_node", AsyncNodeAction.node_async(streamingNode))
            .addNode("process_node", AsyncNodeAction.node_async(processNode))
            .addEdge(START, "streaming_node")
            .addEdge("streaming_node", "process_node")
            .addEdge("process_node", END);

    // 编译图
    CompiledGraph graph = stateGraph.compile(
            CompileConfig.builder()
                    .build()
    );

    // 创建配置
    RunnableConfig config = RunnableConfig.builder()
            .threadId("streaming_thread")
            .build();

    // 使用流式方式执行图
    System.out.println("开始流式输出...\n");

    graph.stream(Map.of("query", "请用一句话介绍 Spring AI"), config)
            .doOnNext(output -> {
                // 处理流式输出
                if (output instanceof StreamingOutput<?> streamingOutput) {
                    // 流式输出块
                    String chunk = streamingOutput.chunk();
                    if (chunk != null && !chunk.isEmpty()) {
                        System.out.print(chunk); // 实时打印流式内容
                    }
                }
                else {
                    // 普通节点输出
                    String nodeId = output.node();
                    Map<String, Object> state = output.state().data();
                    System.out.println("\n节点 '" + nodeId + "' 执行完成");
                    if (state.containsKey("result")) {
                        System.out.println("最终结果: " + state.get("result"));
                    }
                }
            })
            .doOnComplete(() -> {
                System.out.println("\n\n流式输出完成");
            })
            .doOnError(error -> {
                System.err.println("流式输出错误: " + error.getMessage());
            })
            .blockLast(); // 阻塞等待流完成
}`}
</Code>

## 流式 LLM tokens

假设我们有一个调用 LLM 的流式节点：

<Code
  language="java"
  title="流式 LLM tokens" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/core/StreamingExample.java"
>
{`import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;

import reactor.core.publisher.Flux;

ChatClient chatClient = chatClientBuilder.build();

Flux<ChatResponse> chatResponseFlux = chatClient.prompt()
        .user("给我讲一个笑话")
        .stream()
        .chatResponse();

chatResponseFlux.subscribe(
        response -> {
            // 处理每个 token
            String content = response.getResult().getOutput().getText();
            System.out.print(content);
        },
        error -> {
            // 处理错误
            System.err.println("错误: " + error.getMessage());
        },
        () -> {
            // 完成处理
            System.out.println("\n流式处理完成");
        }
);`}
</Code>

## 理解 Graph 中的流

在 Spring AI Alibaba Graph 中，流式输出通过以下方式工作：

### 流式输出的层次结构

```
┌─────────────────────────────────────────────────────────────────┐
│                     图级别流式输出 (Graph Level)                  │
│                                                                 │
│  ┌──────────────┐      ┌──────────────┐      ┌──────────────┐  │
│  │   Node A     │ ───> │   Node B     │ ───> │   Node C     │  │
│  │ (普通节点)    │      │ (流式LLM节点) │      │ (普通节点)    │  │
│  └──────────────┘      └──────────────┘      └──────────────┘  │
│         │                    │                      │          │
│    NodeOutput          ┌─────┴─────┐          NodeOutput      │
│                        │           │                          │
│                  StreamingOutput  StreamingOutput            │
│                  (Token 1)        (Token 2)                    │
│                                                                 │
│  获取方式：                                                      │
│  • graph.stream() → Flux<NodeOutput>                           │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                   节点级别流式输出 (Node Level)                   │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │              StreamingNode (LLM 节点)                     │  │
│  │                                                            │  │
│  │  chatClient.prompt()                                      │  │
│  │    .user(query)                                           │  │
│  │    .stream()                                              │  │
│  │    .chatResponse()                                        │  │
│  │                                                            │  │
│  │  ┌──────┐  ┌──────┐  ┌──────┐  ┌──────┐  ┌──────┐      │  │
│  │  │Token1│→ │Token2│→ │Token3│→ │Token4│→ │Token5│→ ... │  │
│  │  └──────┘  └──────┘  └──────┘  └──────┘  └──────┘      │  │
│  │                                                            │  │
│  │  这些 Token 会作为整个图流的一部分输出                      │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                   流输出数据类型层次 (Data Types)                  │
│                                                                 │
│                          NodeOutput                            │
│                         (基类/接口)                             │
│                              │                                  │
│        ┌─────────────────────┼─────────────────────┐          │
│        │                     │                     │          │
│   ┌────▼────┐        ┌───────▼───────┐      ┌─────▼─────┐   │
│   │NodeOutput│        │StreamingOutput│      │CustomOutput│   │
│   │(普通节点) │        │  (LLM流式节点) │      │ (用户自定义) │   │
│   └─────────┘        └───────────────┘      └───────────┘   │
│                                                                 │
│  包含内容：                                                      │
│  • OverallState (全局状态)                                      │
│  • Message (节点消息)                                           │
│  • Node ID (节点标识)                                           │
└─────────────────────────────────────────────────────────────────┘
```

### 详细说明

#### 1. 节点级别流式输出

单独看图中一个具体的 Node 节点，它可能会产生流式输出，比如调用模型得到流式 token 输出，这些 token 会作为整个流输出的一部分。

**示例流程：**
```
LLM 节点内部：
  Query → LLM API → Token1 → Token2 → Token3 → ... → TokenN
         (流式响应)
         
这些 Token 会被包装成 StreamingOutput，成为图流的一部分
```

#### 2. 图级别流式输出

站在整个图的视角，图有多个节点且每个节点都会有输出，那么执行图的多个节点自然就形成一个流式过程。有两种方法可以获取整个图的执行流（注意，这包含节点的流式输出内容）。

**方法对比：**

| 方法 | 返回类型 | 使用场景 |
|------|---------|---------|
| `graph.stream()` | `Flux<NodeOutput>` | 普通图执行，直接获取节点输出流 |
| `graph.graphResponseStream()` | `Flux<GraphResponse<NodeOutput>>` | 嵌套子图场景，需要 GraphResponse 包装 |

**图流执行示例：**
```
graph.stream() 返回的流：

NodeOutput(node="A", state={...})           ← 节点 A 输出
  ↓
StreamingOutput(chunk="Hello")             ← LLM 节点流式 Token 1
  ↓
StreamingOutput(chunk=" World")            ← LLM 节点流式 Token 2
  ↓
StreamingOutput(chunk="!")                 ← LLM 节点流式 Token 3
  ↓
NodeOutput(node="C", state={...})          ← 节点 C 输出
  ↓
NodeOutput(node="__END__", state={...})    ← 图执行完成
```

#### 3. 流输出数据类型

流式输出的核心数据类型是 `NodeOutput`，代表节点的输出，NodeOutput 中包含整个图的当前全局 OverallState 状态、当前节点的 Message 输出等，不同的节点可能返回不同子类型：

**类型层次：**

<Code
  language="java"
  title="流输出数据类型层次" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/core/StreamingExample.java"
>
{`// 基类：所有节点输出的基础类型
NodeOutput {
    - node: String              // 节点 ID
    - state: OverallState      // 全局状态
    - message: Object          // 节点消息
}

// 子类型 1：LLM 流式输出（框架内置）
StreamingOutput extends NodeOutput {
    - chunk: String            // 流式 Token 内容
}

// 子类型 2：普通节点输出（框架默认）
NodeOutput (普通实例)

// 子类型 3：用户自定义（可扩展）
CustomOutput extends NodeOutput {
    - customField: Object      // 用户自定义字段
}`}
</Code>

**使用场景：**

- **StreamingOutput**：框架自动为 LLM 流式节点创建，标识流式 Token 输出块
- **NodeOutput**：普通节点的标准输出类型
- **自定义类型**：用户可以基于 `NodeOutput` 扩展任意类型，在自定义节点中返回

## 并行节点的流式输出

如果你有多个并行节点（普通节点或者嵌套子图），可以参考 [并行节点的流式处理](../examples/parallel-streaming) 来了解详情。

## 最佳实践

1. **使用适当的订阅方式**：根据需求选择 `subscribe()`、`blockLast()` 或其他 Reactor 操作符
2. **错误处理**：始终使用 `doOnError()` 处理流式输出中的错误
3. **资源清理**：确保在流完成或取消时正确清理资源
4. **性能考虑**：对于大量数据，使用背压（backpressure）机制控制流的速度

## 相关文档

- [快速入门](../quick-start) - Graph 基础使用
- [Spring Reactor 文档](https://projectreactor.io/docs/core/release/reference/) - Reactor 流式处理参考
