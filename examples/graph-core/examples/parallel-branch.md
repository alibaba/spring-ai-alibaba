---
title: 并行节点定义
description: 了解如何在 Spring AI Alibaba Graph 中定义并行节点以加速图执行，使用 Executor 进行并发管理
keywords: [Spring AI Alibaba, 并行节点, Graph并发, Executor, RunnableConfig, AsyncNodeAction, 并发执行]
---

# 并行节点定义

Spring AI Alibaba Graph 允许您定义并行节点以加速总图执行。

## 图管理的并发执行

要实现并发执行，您必须使用 `RunnableConfig` 为特定的并行节点提供一个 `Executor`：

<Code
  language="java"
  title="配置并行节点 Executor" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/examples/ParallelBranchExample.java"
>
{`import com.alibaba.cloud.ai.graph.RunnableConfig;
import java.util.concurrent.ForkJoinPool;

RunnableConfig runnableConfig = RunnableConfig.builder()
    .addParallelNodeExecutor("<parallel node id>", ForkJoinPool.commonPool())
    .build();`}
</Code>

**注意：**
> 如果未指定 `Executor`，并行节点将被**顺序调度**，要并发运行它们，您必须依赖 `CompletableFuture` 的异步功能，使用 `AsyncNodeAction`

## 并行节点的限制 ⚠️

当前并行节点实现执行存在一些整体**限制**：

* 仅支持 **Fork-Join** 模型

```
          ┌─┐
          │A│
          └─┘
           |
     ┌-----------┐
     |     |     |
   ┌──┐  ┌──┐  ┌──┐
   │A1│  │A2│  │A3│
   └──┘  └──┘  └──┘
     |     |     |
     └-----------┘
           |
          ┌─┐
          │B│
          └─┘
```

* 仅允许**一个并行步骤** ⚠️
```
          ┌─┐
          │A│
          └─┘
           |
     ┌-----------┐
     |     |     |
   ┌──┐  ┌──┐  ┌──┐
   │A1│  │A2│  │A3│
   └──┘  └──┘  └──┘
     |     |     |
   ┌──┐    |     |
   │A4│ ❌ 不允许
   └──┘    |     |
     |     |     |
     └-----------┘
           |
          ┌─┐
          │B│
          └─┘
```

* 不允许使用**条件边** ⚠️

以下是一些示例，展示如何创建分支数据流。

## 定义带并行节点的图

只需将多个节点关联到同一条边即可。

### 示例 - 定义并行节点

<Code
  language="java"
  title="定义并行节点" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/examples/ParallelBranchExample.java"
>
{`import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

// 定义状态策略
KeyStrategyFactory keyStrategyFactory = () -> {
    HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
    keyStrategyHashMap.put("messages", new AppendStrategy());
    return keyStrategyHashMap;
};

// 创建节点的辅助方法
private static AsyncNodeAction makeNode(String message) {
    return node_async(state -> Map.of("messages", List.of(message)));
}

// 构建并行图
StateGraph workflow = new StateGraph(keyStrategyFactory)
        .addNode("A", makeNode("A"))
        .addNode("A1", makeNode("A1"))
        .addNode("A2", makeNode("A2"))
        .addNode("A3", makeNode("A3"))
        .addNode("B", makeNode("B"))
        .addNode("C", makeNode("C"))
        .addEdge("A", "A1")   // A 到 A1
        .addEdge("A", "A2")   // A 到 A2 (并行)
        .addEdge("A", "A3")   // A 到 A3 (并行)
        .addEdge("A1", "B")   // A1 到 B
        .addEdge("A2", "B")   // A2 到 B (汇聚)
        .addEdge("A3", "B")   // A3 到 B (汇聚)
        .addEdge("B", "C")
        .addEdge(START, "A")
        .addEdge("C", END);

CompiledGraph graph = workflow.compile();`}
</Code>

### 执行并行 Graph

<Code
  language="java"
  title="执行并行 Graph" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/examples/ParallelBranchExample.java"
>
{`// 执行 Graph
compiledGraph.stream(Map.of())
        .doOnNext(step -> System.out.println(step))
        .doOnError(error -> System.err.println("流错误: " + error.getMessage()))
        .doOnComplete(() -> System.out.println("流完成"))
        .blockLast();`}
</Code>

**图示：**

```
    START
      |
      A
    / | \
   A1 A2 A3  (并行执行)
    \ | /
      B
      |
      C
      |
     END
```

### 条件返回到并行节点

您也可以在所有并行执行结束后有条件地返回到特定并行节点：

<Code
  language="java"
  title="条件返回到并行节点" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/examples/ParallelBranchExample.java"
>
{`import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.StateGraph;
import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;

StateGraph workflow = new StateGraph(keyStrategyFactory)
        .addNode("A", makeNode("A"))
        .addNode("A1", makeNode("A1"))
        .addNode("A2", makeNode("A2"))
        .addNode("A3", makeNode("A3"))
        .addNode("B", makeNode("B"))
        .addNode("C", makeNode("C"))
        .addEdge("A", "A1")
        .addEdge("A", "A2")
        .addEdge("A", "A3")
        .addEdge("A1", "B")
        .addEdge("A2", "B")
        .addEdge("A3", "B")
        // 条件边：根据状态决定是继续还是返回
        .addConditionalEdges("B",
                edge_async(state -> {
                    // 检查上一个节点
                    List<String> messages = (List<String>) state.value("messages").orElse(List.of());
                    String lastMessage = messages.isEmpty() ? "" : messages.get(messages.size() - 1);
                    return lastMessage.equals("A3") ? "continue" : "back";
                }),
                Map.of(
                        "back", "A1",
                        "continue", "C"
                )
        )
        .addEdge(START, "A")
        .addEdge("C", END);

CompiledGraph graph = workflow.compile();`}
</Code>

## 使用编译的子图作为并行节点

为了克服并行分支中仅支持单步的问题，我们可以使用子图。

### 示例 - 混合节点和子图

<Code
  language="java"
  title="混合节点和子图" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/examples/ParallelBranchExample.java"
>
{`import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.StateGraph;
import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

// 创建子图 A3
StateGraph subgraphA3Builder = new StateGraph(keyStrategyFactory)
        .addNode("A3.1", makeNode("A3.1"))
        .addNode("A3.2", makeNode("A3.2"))
        .addEdge(START, "A3.1")
        .addEdge("A3.1", "A3.2")
        .addEdge("A3.2", END);

CompiledGraph subgraphA3 = subgraphA3Builder.compile();

// 创建子图 A1
StateGraph subgraphA1Builder = new StateGraph(keyStrategyFactory)
        .addNode("A1.1", makeNode("A1.1"))
        .addNode("A1.2", makeNode("A1.2"))
        .addEdge(START, "A1.1")
        .addEdge("A1.1", "A1.2")
        .addEdge("A1.2", END);

CompiledGraph subgraphA1 = subgraphA1Builder.compile();

// 主图：混合使用节点和子图
StateGraph workflow = new StateGraph(keyStrategyFactory)
        .addNode("A", makeNode("A"))
        .addNode("A1", node_async(state -> subgraphA1.invoke(state.data()).orElseThrow().data()))
        .addNode("A2", makeNode("A2"))
        .addNode("A3", node_async(state -> subgraphA3.invoke(state.data()).orElseThrow().data()))
        .addNode("B", makeNode("B"))
        .addEdge("A", "A1")
        .addEdge("A", "A2")
        .addEdge("A", "A3")
        .addEdge("A1", "B")
        .addEdge("A2", "B")
        .addEdge("A3", "B")
        .addEdge(START, "A")
        .addEdge("B", END);

CompiledGraph graph = workflow.compile();`}
</Code>

**图示：**

```
        START
          |
          A
        / | \
    ┌────┐ ┌────┐ ┌────┐
    │ A1 │ │ A2 │ │ A3 │
    │子图│ │节点│ │子图│
    └────┘ └────┘ └────┘
        \ | /
          B
          |
         END
```

### 示例 - 仅使用子图

<Code
  language="java"
  title="仅使用子图" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/examples/ParallelBranchExample.java"
>
{`// 创建三个子图
CompiledGraph subgraphA1 = new StateGraph(keyStrategyFactory)
    .addNode("A1.1", makeNode("A1.1"))
    .addNode("A1.2", makeNode("A1.2"))
    .addEdge(START, "A1.1")
    .addEdge("A1.1", "A1.2")
    .addEdge("A1.2", END)
    .compile();

CompiledGraph subgraphA2 = new StateGraph(keyStrategyFactory)
    .addNode("A2.1", makeNode("A2.1"))
    .addNode("A2.2", makeNode("A2.2"))
    .addEdge(START, "A2.1")
    .addEdge("A2.1", "A2.2")
    .addEdge("A2.2", END)
    .compile();

CompiledGraph subgraphA3 = new StateGraph(keyStrategyFactory)
    .addNode("A3.1", makeNode("A3.1"))
    .addNode("A3.2", makeNode("A3.2"))
    .addEdge(START, "A3.1")
    .addEdge("A3.1", "A3.2")
    .addEdge("A3.2", END)
    .compile();

// 主图：仅使用子图
StateGraph workflow = new StateGraph(keyStrategyFactory)
    .addNode("A", makeNode("A"))
    .addNode("A1", node_async(state -> subgraphA1.invoke(state.data()).orElseThrow().data()))
    .addNode("A2", node_async(state -> subgraphA2.invoke(state.data()).orElseThrow().data()))
    .addNode("A3", node_async(state -> subgraphA3.invoke(state.data()).orElseThrow().data()))
    .addNode("B", makeNode("B"))
    .addEdge("A", "A1")
    .addEdge("A", "A2")
    .addEdge("A", "A3")
    .addEdge("A1", "B")
    .addEdge("A2", "B")
    .addEdge("A3", "B")
    .addEdge(START, "A")
    .addEdge("B", END);

CompiledGraph graph = workflow.compile();`}
</Code>

## 完整示例：并行数据处理

<Code
  language="java"
  title="完整示例：并行数据处理" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/examples/ParallelBranchExample.java"
>
{`import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import java.util.concurrent.ForkJoinPool;

// 定义状态
KeyStrategyFactory keyStrategyFactory = () -> {
    Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
    keyStrategyMap.put("data", new ReplaceStrategy());
    keyStrategyMap.put("results", new AppendStrategy());
    return keyStrategyMap;
};

// 数据处理节点
var processTypeA = node_async(state -> {
    String data = (String) state.value("data").orElse("");
    String result = "Type A processed: " + data;
    return Map.of("results", List.of(result));
});

var processTypeB = node_async(state -> {
    String data = (String) state.value("data").orElse("");
    String result = "Type B processed: " + data;
    return Map.of("results", List.of(result));
});

var processTypeC = node_async(state -> {
    String data = (String) state.value("data").orElse("");
    String result = "Type C processed: " + data;
    return Map.of("results", List.of(result));
});

// 聚合结果
var aggregateResults = node_async(state -> {
    List<String> results = (List<String>) state.value("results").orElse(List.of());
    String aggregated = String.join(", ", results);
    return Map.of("final_result", aggregated);
});

// 构建图
StateGraph stateGraph = new StateGraph(keyStrategyFactory)
    .addNode("process_a", processTypeA)
    .addNode("process_b", processTypeB)
    .addNode("process_c", processTypeC)
    .addNode("aggregate", aggregateResults)
    .addEdge(START, "process_a")
    .addEdge(START, "process_b")
    .addEdge(START, "process_c")
    .addEdge("process_a", "aggregate")
    .addEdge("process_b", "aggregate")
    .addEdge("process_c", "aggregate")
    .addEdge("aggregate", END);

CompiledGraph graph = stateGraph.compile();

// 配置并行执行器
RunnableConfig config = RunnableConfig.builder()
    .addParallelNodeExecutor("process_a", ForkJoinPool.commonPool())
    .addParallelNodeExecutor("process_b", ForkJoinPool.commonPool())
    .addParallelNodeExecutor("process_c", ForkJoinPool.commonPool())
    .build();

// 执行
Map<String, Object> result = graph.invoke(
    Map.of("data", "Sample Data"),
    config
);`}
</Code>

## 最佳实践

1. **合理使用并行**：仅在节点之间没有数据依赖时使用并行执行。
2. **线程池管理**：使用适当大小的线程池，避免过度并发。
3. **错误处理**：确保并行节点都有适当的错误处理。
4. **状态同步**：使用适当的状态策略（如 `AppendStrategy`）来合并并行结果。
5. **性能监控**：监控并行执行的性能收益，确保它确实提高了性能。

通过并行节点，您可以显著提高工作流的执行效率，特别是在处理独立任务时。

更多详细示例，请参阅 [parallel-node 示例](https://github.com/springaialibaba/spring-ai-alibaba-examples/tree/main/graph/parallel-node)。
