---
title: 子图作为 StateGraph
description: 学习如何在Spring AI Alibaba中将StateGraph组合使用，构建层次化的工作流
keywords: [子图, Subgraph, StateGraph, 组合, 工作流嵌套, Graph模块化]
---

# 子图作为 StateGraph

在 Spring AI Alibaba 中，可以将一个 StateGraph 嵌入到另一个 StateGraph 中，实现复杂工作流的模块化设计。

## 基本概念

将 StateGraph 作为子图使用有以下优势：

- **复用性**: 相同的子图可以在多个父图中复用
- **可维护性**: 独立开发和测试每个子图
- **清晰性**: 层次化结构使工作流更易理解

## 定义子图

<Code
  language="java"
  title="定义子图" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/examples/SubgraphAsStateGraphExample.java"
>
{`import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import java.util.HashMap;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * 定义子图
 */
public static StateGraph createProcessingSubGraph() throws GraphStateException {
    KeyStrategyFactory keyFactory = () -> {
        HashMap<String, KeyStrategy> strategies = new HashMap<>();
        strategies.put("input", new ReplaceStrategy());
        strategies.put("output", new ReplaceStrategy());
        strategies.put("valid", new ReplaceStrategy());
        return strategies;
    };

    return new StateGraph(keyFactory)
            .addNode("validate", node_async(state -> {
            String input = (String) state.value("input").orElse("");
            boolean isValid = input != null && !input.isEmpty();
            return Map.of("valid", isValid);
        }))
            .addNode("transform", node_async(state -> {
            String input = (String) state.value("input").orElse("");
            String transformed = input.toUpperCase();
            return Map.of("output", transformed);
        }))
            .addEdge(START, "validate")
        .addConditionalEdges("validate",
                    edge_async(state -> {
                Boolean valid = (Boolean) state.value("valid").orElse(false);
                return valid ? "valid" : "invalid";
            }),
            Map.of(
                "valid", "transform",
                            "invalid", END
            ))
            .addEdge("transform", END);
}`}
</Code>

## 在父图中集成子图

### 方式 1: 直接嵌入

<Code
  language="java"
  title="直接嵌入子图" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/examples/SubgraphAsStateGraphExample.java"
>
{`/**
 * 在父图中集成子图 - 方式 1: 直接嵌入
 */
public static StateGraph createParentGraphWithDirectEmbedding() throws GraphStateException {
    KeyStrategyFactory keyFactory = () -> {
        HashMap<String, KeyStrategy> strategies = new HashMap<>();
        strategies.put("data", new ReplaceStrategy());
        strategies.put("output", new ReplaceStrategy());
        strategies.put("result", new ReplaceStrategy());
        return strategies;
    };

    StateGraph subGraph = createProcessingSubGraph();

    return new StateGraph(keyFactory)
            .addNode("prepare", node_async(state -> {
            return Map.of("data", "hello world");
        }))
        // 将子图作为节点添加
        .addNode("process", subGraph)
            .addNode("finalize", node_async(state -> {
            String output = (String) state.value("output").orElse("");
            return Map.of("result", "Final: " + output);
        }))
            .addEdge(START, "prepare")
        .addEdge("prepare", "process")
        .addEdge("process", "finalize")
            .addEdge("finalize", END);
}`}
</Code>

### 方式 2: 使用编译后的子图

<Code
  language="java"
  title="使用编译后的子图" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/examples/SubgraphAsStateGraphExample.java"
>
{`import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;

import java.util.Map;

/**
 * 在父图中集成子图 - 方式 2: 使用编译后的子图
 */
public static StateGraph createParentGraphWithCompiledSubGraph() throws GraphStateException {
    KeyStrategyFactory keyFactory = () -> {
        HashMap<String, KeyStrategy> strategies = new HashMap<>();
        strategies.put("data", new ReplaceStrategy());
        strategies.put("output", new ReplaceStrategy());
        strategies.put("processed", new ReplaceStrategy());
        return strategies;
    };

// 先编译子图
CompiledGraph compiledSubGraph = createProcessingSubGraph().compile();

// 在父图中使用
    return new StateGraph(keyFactory)
            .addNode("prepare", node_async(state -> {
        return Map.of("data", "input");
    }))
            .addNode("process", node_async(state -> {
        // 手动调用子图
        Map<String, Object> subInput = Map.of(
            "input", state.value("data").orElse("")
        );
                OverAllState subResult = compiledSubGraph.invoke(subInput, RunnableConfig.builder().build()).orElseThrow();
        return Map.of("processed", subResult.value("output").orElse(""));
    }))
            .addEdge(START, "prepare")
    .addEdge("prepare", "process")
            .addEdge("process", END);
}`}
</Code>

## 状态共享与隔离

### 共享状态

父子图共享相同的状态键：

<Code
  language="java"
  title="共享状态" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/examples/SubgraphAsStateGraphExample.java"
>
{`// 父子图使用相同的 KeyStrategyFactory
KeyStrategyFactory sharedKeyFactory = () -> {
    HashMap<String, KeyStrategy> strategies = new HashMap<>();
    strategies.put("data", new ReplaceStrategy());
    strategies.put("result", new ReplaceStrategy());
    return strategies;
};

StateGraph subGraph = new StateGraph(sharedKeyFactory)
    .addNode("process", nodeasync(state -> {
        String data = (String) state.value("data").orElse("");
        return Map.of("result", "Processed: " + data);
    }))
    .addEdge(StateGraph.START, "process")
    .addEdge("process", StateGraph.END);

StateGraph parentGraph = new StateGraph(sharedKeyFactory)
    .addNode("prepare", nodeasync(state ->
        Map.of("data", "Input")))
    .addNode("sub", subGraph)  // 子图直接访问 "data" 并写入 "result"
    .addEdge(StateGraph.START, "prepare")
    .addEdge("prepare", "sub")
    .addEdge("sub", StateGraph.END);`}
</Code>

### 状态隔离

子图使用独立的状态空间：

<Code
  language="java"
  title="状态隔离" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/examples/SubgraphAsStateGraphExample.java"
>
{`public class IsolatedSubGraphNode implements NodeAction {
    private final CompiledGraph subGraph;

    public IsolatedSubGraphNode(StateGraph subGraphDef) {
        this.subGraph = subGraphDef.compile();
    }

    @Override
    public Map<String, Object> apply(OverAllState parentState) {
        // 提取父状态数据
        String input = (String) parentState.value("input").orElse("");

        // 创建子图独立状态
        Map<String, Object> subState = Map.of("subInput", input);

        // 执行子图
        OverAllState subResult = subGraph.invoke(subState);

        // 将子图结果映射回父状态
        String output = (String) subResult.value("subOutput").orElse("");
        return Map.of("output", output);
    }
}`}
</Code>

## 递归子图

子图可以包含其他子图：

<Code
  language="java"
  title="递归子图" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/examples/SubgraphAsStateGraphExample.java"
>
{`// Level 2 子图
StateGraph level2SubGraph = new StateGraph(keyFactory)
    .addNode("level2Node", nodeasync(state ->
        Map.of("level2Result", "Level 2")))
    .addEdge(StateGraph.START, "level2Node")
    .addEdge("level2Node", StateGraph.END);

// Level 1 子图，包含 Level 2
StateGraph level1SubGraph = new StateGraph(keyFactory)
    .addNode("level1Node", nodeasync(state ->
        Map.of("level1Result", "Level 1")))
    .addNode("level2", level2SubGraph)
    .addEdge(StateGraph.START, "level1Node")
    .addEdge("level1Node", "level2")
    .addEdge("level2", StateGraph.END);

// 父图，包含 Level 1
StateGraph parentGraph = new StateGraph(keyFactory)
    .addNode("parentNode", nodeasync(state ->
        Map.of("parentResult", "Parent")))
    .addNode("level1", level1SubGraph)
    .addEdge(StateGraph.START, "parentNode")
    .addEdge("parentNode", "level1")
    .addEdge("level1", StateGraph.END);`}
</Code>

## 完整示例

<Code
  language="java"
  title="完整示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/examples/SubgraphAsStateGraphExample.java"
>
{`import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.state.strategy.*;
import java.util.*;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.nodeasync;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edgeasync;

// 数据验证子图
public StateGraph createValidationSubGraph() {
    KeyStrategyFactory keyFactory = () -> {
        HashMap<String, KeyStrategy> strategies = new HashMap<>();
        strategies.put("data", new ReplaceStrategy());
        strategies.put("isValid", new ReplaceStrategy());
        strategies.put("error", new ReplaceStrategy());
        return strategies;
    };

    return new StateGraph(keyFactory)
        .addNode("checkFormat", nodeasync(state -> {
            String data = (String) state.value("data").orElse("");
            boolean valid = data.matches("^[A-Za-z0-9]+$");
            return Map.of(
                "isValid", valid,
                "error", valid ? "" : "Invalid format"
            );
        }))
        .addEdge(StateGraph.START, "checkFormat")
        .addEdge("checkFormat", StateGraph.END);
}

// 数据转换子图
public StateGraph createTransformSubGraph() {
    KeyStrategyFactory keyFactory = () -> {
        HashMap<String, KeyStrategy> strategies = new HashMap<>();
        strategies.put("data", new ReplaceStrategy());
        strategies.put("transformed", new ReplaceStrategy());
        return strategies;
    };

    return new StateGraph(keyFactory)
        .addNode("uppercase", nodeasync(state -> {
            String data = (String) state.value("data").orElse("");
            return Map.of("transformed", data.toUpperCase());
        }))
        .addNode("addPrefix", nodeasync(state -> {
            String data = (String) state.value("transformed").orElse("");
            return Map.of("transformed", "PROC_" + data);
        }))
        .addEdge(StateGraph.START, "uppercase")
        .addEdge("uppercase", "addPrefix")
        .addEdge("addPrefix", StateGraph.END);
}

// 主处理流程
public StateGraph createMainGraph() {
    KeyStrategyFactory keyFactory = () -> {
        HashMap<String, KeyStrategy> strategies = new HashMap<>();
        strategies.put("data", new ReplaceStrategy());
        strategies.put("isValid", new ReplaceStrategy());
        strategies.put("transformed", new ReplaceStrategy());
        strategies.put("result", new ReplaceStrategy());
        return strategies;
    };

    StateGraph validationSub = createValidationSubGraph();
    StateGraph transformSub = createTransformSubGraph();

    return new StateGraph(keyFactory)
        .addNode("validate", validationSub)
        .addNode("transform", transformSub)
        .addNode("finalize", nodeasync(state -> {
            String result = (String) state.value("transformed").orElse("No result");
            return Map.of("result", "Final: " + result);
        }))
        .addEdge(StateGraph.START, "validate")
        .addConditionalEdges("validate",
            edgeasync(state -> {
                Boolean valid = (Boolean) state.value("isValid").orElse(false);
                return valid ? "continue" : "error";
            }),
            Map.of(
                "continue", "transform",
                "error", StateGraph.END
            ))
        .addEdge("transform", "finalize")
        .addEdge("finalize", StateGraph.END);
}

// 使用
CompiledGraph graph = createMainGraph().compile();
OverAllState result = graph.invoke(Map.of("data", "test123"));
System.out.println("Result: " + result.value("result").orElse(""));
// 输出: Result: Final: PROC_TEST123`}
</Code>

## 附图
![png](/img/graph/examples/subgraph-as-stategraph_files/subgraph-as-stategraph_15_0.png)


![png](/img/graph/examples/subgraph-as-stategraph_files/subgraph-as-stategraph_17_0.png)


## 相关文档

- [子图作为 NodeAction](./subgraph-as-nodeaction) - 另一种集成方式
- [子图作为 CompiledGraph](./subgraph-as-compiledgraph) - 编译后使用
- [快速入门](../quick-start) - Graph 基础

