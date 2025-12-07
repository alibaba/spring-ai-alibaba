---
title: 并行流式输出
description: 使用 Spring AI Alibaba Graph 实现并行节点的流式输出，每个并行节点可以独立产生流式输出并保持各自的节点 ID
keywords: [Spring AI Alibaba, Graph, 并行流式, Parallel Streaming, GraphFlux, 节点流式]
---

# 并行流式输出

并行流式输出允许在并行分支中使用 `GraphFlux` 实现流式输出。每个并行节点可以独立产生流式输出，并保持各自的节点 ID，便于区分不同节点的输出。

## 核心概念

在并行流式输出中：

- **GraphFlux**：用于将 Reactor Flux 转换为图流式输出的工具类
- **节点 ID 保持**：每个并行节点的流式输出会保持各自的节点 ID
- **独立流式处理**：每个并行节点可以独立产生和处理流式数据

## 实现示例

### 示例 1: 并行节点流式输出

<Code
  language="java"
  title="并行节点流式输出示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/examples/ParallelStreamingExample.java"
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
import com.alibaba.cloud.ai.graph.streaming.GraphFlux;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import reactor.core.publisher.Flux;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;

public class ParallelStreamingExample {

    /**
     * 并行节点流式输出 - 每个节点保持独立的节点 ID
     */
    public static void parallelStreamingWithNodeIdPreservation() throws GraphStateException {
        // 定义状态策略
        KeyStrategyFactory keyStrategyFactory = () -> {
            Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
            keyStrategyMap.put("messages", new AppendStrategy());
            keyStrategyMap.put("parallel_results", new AppendStrategy());
            return keyStrategyMap;
        };

        // 并行节点 1 - 返回 GraphFlux 流式输出
        AsyncNodeAction node1 = state -> {
            // 创建流式数据
            Flux<String> stream1 = Flux.just("节点1-块1", "节点1-块2", "节点1-块3")
                    .delayElements(Duration.ofMillis(50));

            // 定义最终结果映射函数
            Function<String, String> mapResult1 = lastChunk ->
                    "节点1完成，最后块: " + lastChunk;

            // 定义块结果提取函数
            Function<String, String> chunkResult1 = chunk -> chunk;

            // 创建 GraphFlux，指定节点 ID 为 "parallel_node_1"
            GraphFlux<String> graphFlux1 = GraphFlux.of(
                    "parallel_node_1",  // 节点 ID
                    "stream1",          // 输出键
                    stream1,            // 流式数据
                    mapResult1,         // 最终结果映射
                    chunkResult1        // 块结果提取
            );

            return CompletableFuture.completedFuture(Map.of("stream1", graphFlux1));
        };

        // 并行节点 2 - 返回 GraphFlux 流式输出
        AsyncNodeAction node2 = state -> {
            // 创建流式数据（延迟时间不同，模拟不同的处理速度）
            Flux<String> stream2 = Flux.just("节点2-块1", "节点2-块2", "节点2-块3")
                    .delayElements(Duration.ofMillis(75));

            // 定义最终结果映射函数
            Function<String, String> mapResult2 = lastChunk ->
                    "节点2完成，最后块: " + lastChunk;

            // 定义块结果提取函数
            Function<String, String> chunkResult2 = chunk -> chunk;

            // 创建 GraphFlux，指定节点 ID 为 "parallel_node_2"
            GraphFlux<String> graphFlux2 = GraphFlux.of(
                    "parallel_node_2",  // 节点 ID
                    "stream2",          // 输出键
                    stream2,            // 流式数据
                    mapResult2,         // 最终结果映射
                    chunkResult2        // 块结果提取
            );

            return CompletableFuture.completedFuture(Map.of("stream2", graphFlux2));
        };

        // 合并节点 - 接收并行节点的结果
        AsyncNodeAction mergeNode = state -> {
            System.out.println("\n合并节点接收到状态: " + state.data());
            return CompletableFuture.completedFuture(
                    Map.of("messages", "所有并行节点已完成，结果已合并")
            );
        };

        // 构建图：两个并行节点从 START 开始，都汇聚到 merge 节点
        StateGraph stateGraph = new StateGraph(keyStrategyFactory)
                .addNode("node1", node1)
                .addNode("node2", node2)
                .addNode("merge", mergeNode)
                .addEdge(START, "node1")      // 并行分支 1
                .addEdge(START, "node2")      // 并行分支 2
                .addEdge("node1", "merge")    // 汇聚到合并节点
                .addEdge("node2", "merge")    // 汇聚到合并节点
                .addEdge("merge", END);

        // 编译图
        CompiledGraph graph = stateGraph.compile(
                CompileConfig.builder()
                        .build()
        );

        // 创建配置
        RunnableConfig config = RunnableConfig.builder()
                .threadId("parallel_streaming_thread")
                .build();

        // 跟踪每个节点产生的流式输出数量
        Map<String, Integer> nodeStreamCounts = new HashMap<>();
        AtomicInteger totalChunks = new AtomicInteger(0);

        System.out.println("开始并行流式输出...\n");

        // 执行流式图并处理输出
        graph.stream(Map.of("input", "test"), config)
                .doOnNext(output -> {
                    if (output instanceof StreamingOutput<?> streamingOutput) {
                        // 处理流式输出
                        String nodeId = streamingOutput.node();
                        String chunk = streamingOutput.chunk();

                        // 统计每个节点的流式输出
                        nodeStreamCounts.merge(nodeId, 1, Integer::sum);
                        totalChunks.incrementAndGet();

                        // 实时打印流式内容，显示节点 ID
                        System.out.println("[流式输出] 节点: " + nodeId +
                                ", 内容: " + chunk);
                    }
                    else {
                        // 处理普通节点输出
                        String nodeId = output.node();
                        Map<String, Object> state = output.state().data();
                        System.out.println("\n[节点完成] " + nodeId +
                                ", 状态: " + state);
                    }
                })
                .doOnComplete(() -> {
                    System.out.println("\n=== 并行流式输出完成 ===");
                    System.out.println("总流式块数: " + totalChunks.get());
                    System.out.println("各节点流式输出统计: " + nodeStreamCounts);
                })
                .doOnError(error -> {
                    System.err.println("流式输出错误: " + error.getMessage());
                    error.printStackTrace();
                })
                .blockLast(); // 阻塞等待流完成
    }
}`}
</Code>

### 示例 2: 单个节点的流式输出

<Code
  language="java"
  title="单个节点的流式输出示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/examples/ParallelStreamingExample.java"
>
{`/**
 * 单个节点的流式输出
 */
public static void singleNodeStreaming() throws GraphStateException {
    // 定义状态策略
    KeyStrategyFactory keyStrategyFactory = () -> {
        Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
        keyStrategyMap.put("messages", new AppendStrategy());
        keyStrategyMap.put("stream_result", new AppendStrategy());
        return keyStrategyMap;
    };

    // 单个流式节点
    AsyncNodeAction streamingNode = state -> {
        // 创建流式数据
        Flux<String> dataStream = Flux.just("块1", "块2", "块3", "块4", "块5")
                .delayElements(Duration.ofMillis(100));

        // 定义最终结果映射函数
        Function<String, Map<String, Object>> mapResult = lastChunk ->
                Map.of("final_result", "所有块处理完成，最后块: " + lastChunk);

        // 定义块结果提取函数
        Function<String, String> chunkResult = chunk -> chunk;

        // 创建 GraphFlux
        GraphFlux<String> graphFlux = GraphFlux.of(
                "streaming_node",  // 节点 ID
                "stream_output",   // 输出键
                dataStream,         // 流式数据
                mapResult,          // 最终结果映射
                chunkResult         // 块结果提取
        );

        return CompletableFuture.completedFuture(Map.of("stream_output", graphFlux));
    };

    // 构建图
    StateGraph stateGraph = new StateGraph(keyStrategyFactory)
            .addNode("streaming_node", streamingNode)
            .addEdge(START, "streaming_node")
            .addEdge("streaming_node", END);

    // 编译图
    CompiledGraph graph = stateGraph.compile(
            CompileConfig.builder()
                    .build()
    );

    // 创建配置
    RunnableConfig config = RunnableConfig.builder()
            .threadId("single_streaming_thread")
            .build();

    System.out.println("开始单节点流式输出...\n");

    AtomicInteger streamCount = new AtomicInteger(0);
    String[] lastNodeId = new String[1];

    // 执行流式图
    graph.stream(Map.of("input", "test"), config)
            .filter(output -> output instanceof StreamingOutput)
            .map(output -> (StreamingOutput<?>) output)
            .doOnNext(streamingOutput -> {
                streamCount.incrementAndGet();
                lastNodeId[0] = streamingOutput.node();
                System.out.println("[流式输出] 节点: " + streamingOutput.node() +
                        ", 内容: " + streamingOutput.chunk());
            })
            .doOnComplete(() -> {
                System.out.println("\n=== 单节点流式输出完成 ===");
                System.out.println("节点 ID: " + lastNodeId[0]);
                System.out.println("流式块数: " + streamCount.get());
            })
            .doOnError(error -> {
                System.err.println("流式输出错误: " + error.getMessage());
            })
            .blockLast();
}`}
</Code>

## GraphFlux API

`GraphFlux` 提供了以下静态方法来创建流式输出：

<Code
  language="java"
  title="GraphFlux API" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/examples/ParallelStreamingExample.java"
>
{`GraphFlux<T> GraphFlux.of(
    String nodeId,              // 节点 ID
    String outputKey,           // 输出键
    Flux<T> dataStream,        // 流式数据
    Function<T, Map<String, Object>> mapResult,  // 最终结果映射
    Function<T, Object> chunkResult              // 块结果提取
);`}
</Code>

### 参数说明

- **nodeId**：节点标识符，用于在流式输出中标识来源节点
- **outputKey**：输出键，用于在状态中存储流式结果
- **dataStream**：Reactor Flux 流式数据源
- **mapResult**：将最后一个块映射为最终状态更新的函数
- **chunkResult**：从每个块中提取结果的函数

## 关键特性

1. **节点 ID 保持**：每个并行节点的流式输出会保持各自的节点 ID，便于区分
2. **独立流式处理**：每个并行节点可以独立产生和处理流式数据
3. **结果统计**：可以统计每个节点产生的流式输出数量
4. **实时输出**：流式输出可以实时打印，提供良好的用户体验

## 使用场景

- **并行数据处理**：多个节点同时处理不同的数据流
- **实时反馈**：需要实时向用户展示处理进度的场景
- **多源数据聚合**：从多个数据源并行获取数据并聚合
- **流式 AI 响应**：多个 AI 节点并行生成响应

## 最佳实践

1. **节点 ID 命名**：为每个并行节点使用清晰、有意义的节点 ID
2. **延迟控制**：合理设置流式数据的延迟，避免过快或过慢
3. **错误处理**：在流式处理中添加适当的错误处理逻辑
4. **结果统计**：使用统计机制跟踪流式输出的进度和数量

通过并行流式输出，您可以构建高效、实时的并行处理系统，提供良好的用户体验。

