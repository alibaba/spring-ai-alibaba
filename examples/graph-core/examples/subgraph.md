---
title: 子图 Subgraphs
description: 学习如何使用子图实现多智能体系统和组件复用，实现团队协作开发和模块化设计
keywords: [Spring AI Alibaba, Subgraphs, 子图, 多智能体, Multi-agent, 组件复用, 模块化, Graph封装]
---

# 子图（Subgraphs）

子图是在另一个图中用作节点的图，Spring AI Alibaba Graph 支持多种不同的模式来使用子图，不同的使用方式会决定图之间是否共享上下文、。

## 使用子图的原因

使用子图的一些原因包括：

* **构建多智能体系统**：每个智能体可以是一个独立的子图
* **组件复用**：当您想在多个图中重用一组节点时，这些节点可能共享某些状态，您可以在子图中定义一次，然后在多个父图中使用它们
* **团队协作**：当您希望不同的团队独立地在图的不同部分上工作时，您可以将每个部分定义为子图，只要遵守子图接口（输入和输出 schema），就可以在不了解子图任何细节的情况下构建父图

## 如何添加子图

有三种方法可以将子图添加到父图中：

### 1. 直接添加编译的子图作为节点

当父图和子图共享状态时，可以直接将编译的子图作为节点添加到父图中。这是最简单的方式。

<Code
  language="java"
  title="直接添加编译的子图作为节点" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/examples/SubgraphExample.java"
>
{`import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import java.util.HashMap;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

// 创建并编译子图
KeyStrategyFactory subKeyFactory = () -> {
    HashMap<String, KeyStrategy> strategies = new HashMap<>();
    strategies.put("input", new ReplaceStrategy());
    strategies.put("output", new ReplaceStrategy());
    return strategies;
};

StateGraph subGraphDef = new StateGraph(subKeyFactory)
        .addNode("process", node_async(state -> {
            String input = (String) state.value("input").orElse("");
            String output = "Processed: " + input.toUpperCase();
            return Map.of("output", output);
        }))
        .addEdge(START, "process")
        .addEdge("process", END);

CompiledGraph compiledSubGraph = subGraphDef.compile();

// 在父图中直接使用编译的子图作为节点
KeyStrategyFactory parentKeyFactory = () -> {
    HashMap<String, KeyStrategy> strategies = new HashMap<>();
    strategies.put("data", new ReplaceStrategy());
    strategies.put("result", new ReplaceStrategy());
    return strategies;
};

StateGraph parentGraph = new StateGraph(parentKeyFactory)
        .addNode("prepare", node_async(state ->
                Map.of("data", "hello world")))
        .addNode("subgraph", compiledSubGraph)  // 直接使用编译的子图
        .addNode("finalize", node_async(state -> {
            String result = (String) state.value("result").orElse("");
            return Map.of("final", "Done: " + result);
        }))
        .addEdge(START, "prepare")
        .addEdge("prepare", "subgraph")
        .addEdge("subgraph", "finalize")
        .addEdge("finalize", END);

CompiledGraph compiledParent = parentGraph.compile();`}
</Code>

### 2. 在节点操作中调用子图

当父图和子图具有不同的状态 schema，需要在调用子图之前或之后转换状态时，可以在节点操作中手动调用子图。

<Code
  language="java"
  title="在节点操作中调用子图" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/examples/SubgraphExample.java"
>
{`import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import java.util.HashMap;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

// 定义子图
KeyStrategyFactory childKeyFactory = () -> {
    HashMap<String, KeyStrategy> strategies = new HashMap<>();
    strategies.put("input", new ReplaceStrategy());
    strategies.put("result", new ReplaceStrategy());
    return strategies;
};

StateGraph subGraph = new StateGraph(childKeyFactory)
        .addNode("substep1", node_async(state -> {
            String input = (String) state.value("input").orElse("");
            return Map.of("result", "SubStep1:" + input);
        }))
        .addNode("substep2", node_async(state -> {
            String prev = (String) state.value("result").orElse("");
            return Map.of("result", prev + "->SubStep2");
        }))
        .addEdge(START, "substep1")
        .addEdge("substep1", "substep2")
        .addEdge("substep2", END);

CompiledGraph compiledSubGraph = subGraph.compile();

// 将子图包装为 NodeAction
public static class SubGraphNode implements NodeAction {
    private final CompiledGraph subGraph;

    public SubGraphNode(CompiledGraph subGraph) {
        this.subGraph = subGraph;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        // 从父状态提取子图需要的数据
        String input = (String) state.value("data").orElse("");

        // 执行子图
        Map<String, Object> subInput = Map.of("input", input);
        OverAllState subResult = subGraph.invoke(subInput).orElseThrow();

        // 返回结果给父图
        String result = (String) subResult.value("result").orElse("");
        return Map.of("processed", result);
    }
}

// 在父图中使用
KeyStrategyFactory parentKeyFactory = () -> {
    HashMap<String, KeyStrategy> strategies = new HashMap<>();
    strategies.put("data", new ReplaceStrategy());
    strategies.put("processed", new ReplaceStrategy());
    strategies.put("final", new ReplaceStrategy());
    return strategies;
};

SubGraphNode subGraphNode = new SubGraphNode(compiledSubGraph);

StateGraph parentGraph = new StateGraph(parentKeyFactory)
        .addNode("prepare", node_async(state -> {
            return Map.of("data", "Input Data");
        }))
        .addNode("process", node_async(subGraphNode))  // 使用子图作为节点
        .addNode("finalize", node_async(state -> {
            String processed = (String) state.value("processed").orElse("");
            return Map.of("final", "Final:" + processed);
        }))
        .addEdge(START, "prepare")
        .addEdge("prepare", "process")
        .addEdge("process", "finalize")
        .addEdge("finalize", END);

CompiledGraph compiledParent = parentGraph.compile();`}
</Code>

### 3. 直接嵌入 StateGraph

当父图和子图紧密相关，共享状态时，可以直接将 StateGraph 作为节点嵌入到父图中。

<Code
  language="java"
  title="直接嵌入 StateGraph" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/examples/SubgraphExample.java"
>
{`import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import java.util.HashMap;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

// 定义子图（返回 StateGraph）
KeyStrategyFactory keyFactory = () -> {
    HashMap<String, KeyStrategy> strategies = new HashMap<>();
    strategies.put("input", new ReplaceStrategy());
    strategies.put("output", new ReplaceStrategy());
    strategies.put("valid", new ReplaceStrategy());
    return strategies;
};

StateGraph processingSubGraph = new StateGraph(keyFactory)
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

// 在父图中直接嵌入 StateGraph
KeyStrategyFactory parentKeyFactory = () -> {
    HashMap<String, KeyStrategy> strategies = new HashMap<>();
    strategies.put("data", new ReplaceStrategy());
    strategies.put("output", new ReplaceStrategy());
    strategies.put("result", new ReplaceStrategy());
    return strategies;
};

StateGraph parentGraph = new StateGraph(parentKeyFactory)
        .addNode("prepare", node_async(state -> {
            return Map.of("data", "hello world");
        }))
        .addNode("process", processingSubGraph)  // 直接嵌入 StateGraph
        .addNode("finalize", node_async(state -> {
            String output = (String) state.value("output").orElse("");
            return Map.of("result", "Final: " + output);
        }))
        .addEdge(START, "prepare")
        .addEdge("prepare", "process")
        .addEdge("process", "finalize")
        .addEdge("finalize", END);

CompiledGraph compiledParent = parentGraph.compile();`}
</Code>

## 作为编译图使用

创建子图节点的最简单方法是直接使用编译的子图。这样做时，**父图和子图状态 schema 至少要共享一个键，它们可以使用该键进行通信**，这一点很重要。如果您的图和子图不共享任何键，您应该编写一个调用子图的操作（见上文）。

### 注意事项

* 如果您向子图节点传递额外的键（即，除了共享键之外），子图节点将忽略它们。同样，如果您从子图返回额外的键，父图将忽略它们。
* 支持中断（通过返回 InterruptionMetadata）

### 示例：共享状态的子图

<Code
  language="java"
  title="共享状态的子图" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/examples/SubgraphExample.java"
>
{`import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import java.util.HashMap;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

// 定义共享状态策略
KeyStrategyFactory sharedKeyStrategyFactory = () -> {
    HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
    keyStrategyHashMap.put("sharedData", new ReplaceStrategy());
    keyStrategyHashMap.put("results", new ReplaceStrategy());
    return keyStrategyHashMap;
};

// 创建子图
StateGraph childGraph = new StateGraph(sharedKeyStrategyFactory)
        .addNode("process", node_async(state -> {
            String data = (String) state.value("sharedData").orElse("");
            return Map.of("results", "Child processed: " + data);
        }))
        .addEdge(START, "process")
        .addEdge("process", END);

CompiledGraph compiledChild = childGraph.compile();

// 创建父图，直接使用编译的子图作为节点
StateGraph parentGraph = new StateGraph(sharedKeyStrategyFactory)
        .addNode("prepare", node_async(state -> {
            return Map.of("sharedData", "Parent data");
        }))
        .addNode("subgraph", compiledChild)  // 直接使用编译的子图
        .addEdge(START, "prepare")
        .addEdge("prepare", "subgraph")
        .addEdge("subgraph", END);

CompiledGraph compiledParent = parentGraph.compile();`}
</Code>

### 示例：多个子图复用

同一个编译的子图可以在父图中多次使用，实现组件复用：

<Code
  language="java"
  title="多个子图复用" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/examples/SubgraphExample.java"
>
{`// 创建数据处理子图
KeyStrategyFactory dataProcessorFactory = () -> {
    HashMap<String, KeyStrategy> strategies = new HashMap<>();
    strategies.put("data", new ReplaceStrategy());
    strategies.put("result", new ReplaceStrategy());
    return strategies;
};

StateGraph dataProcessorGraph = new StateGraph(dataProcessorFactory)
        .addNode("process", node_async(state -> {
            String input = (String) state.value("data").orElse("");
            String output = "Processed: " + input.toUpperCase();
            return Map.of("result", output);
        }))
        .addEdge(START, "process")
        .addEdge("process", END);

CompiledGraph dataProcessor = dataProcessorGraph.compile();

// 在多个节点中复用同一个子图
KeyStrategyFactory keyFactory = () -> {
    HashMap<String, KeyStrategy> strategies = new HashMap<>();
    strategies.put("data", new ReplaceStrategy());
    strategies.put("result", new ReplaceStrategy());
    return strategies;
};

StateGraph mainGraph = new StateGraph(keyFactory)
        .addNode("process1", dataProcessor)  // 复用子图
        .addNode("process2", dataProcessor)  // 复用子图
        .addNode("process3", dataProcessor)  // 复用子图
        .addEdge(START, "process1")
        .addEdge("process1", "process2")
        .addEdge("process2", "process3")
        .addEdge("process3", END);

CompiledGraph compiledMain = mainGraph.compile();`}
</Code>

## 作为节点操作使用

您可能想要定义一个具有完全不同 schema 的子图。在这种情况下，您可以创建一个调用子图的节点函数。此函数需要在调用子图之前将输入（父）状态转换为子图状态，并在从节点返回状态更新之前将结果转换回父状态。

### 注意事项

* 中断支持由您自己实现

### 示例：不同状态的子图

<Code
  language="java"
  title="不同状态的子图" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/examples/SubgraphExample.java"
>
{`import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import java.util.HashMap;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

// 父图状态
KeyStrategyFactory parentKeyStrategyFactory = () -> {
    HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
    keyStrategyHashMap.put("parentData", new ReplaceStrategy());
    keyStrategyHashMap.put("processedResult", new ReplaceStrategy());
    return keyStrategyHashMap;
};

// 子图状态（完全不同）
KeyStrategyFactory childKeyStrategyFactory = () -> {
    HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
    keyStrategyHashMap.put("childInput", new ReplaceStrategy());
    keyStrategyHashMap.put("childOutput", new ReplaceStrategy());
    return keyStrategyHashMap;
};

// 创建子图
StateGraph childGraph = new StateGraph(childKeyStrategyFactory)
        .addNode("processor", node_async(state -> {
            String input = (String) state.value("childInput").orElse("");
            String output = "Processed: " + input;
            return Map.of("childOutput", output);
        }))
        .addEdge(START, "processor")
        .addEdge("processor", END);

CompiledGraph compiledChild = childGraph.compile();

// 将子图包装为 NodeAction
public static class CompiledSubGraphNode implements NodeAction {
    private final CompiledGraph compiledGraph;

    public CompiledSubGraphNode(CompiledGraph compiledGraph) {
        this.compiledGraph = compiledGraph;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        // 从父状态提取输入
        String input = (String) state.value("parentData").orElse("");

        // 执行编译好的子图
        Map<String, Object> subInput = Map.of("childInput", input);
        OverAllState subResult = compiledGraph.invoke(subInput).orElseThrow();

        // 提取子图输出
        String output = (String) subResult.value("childOutput").orElse("");
        return Map.of("processedResult", output);
    }
}

// 创建父图
CompiledSubGraphNode subGraphNode = new CompiledSubGraphNode(compiledChild);

StateGraph parentGraph = new StateGraph(parentKeyStrategyFactory)
        .addNode("call_child_with_transform", node_async(subGraphNode))
        .addEdge(START, "call_child_with_transform")
        .addEdge("call_child_with_transform", END);

CompiledGraph compiledParent = parentGraph.compile();`}
</Code>

### 示例：可配置的子图节点

您可以创建一个可配置的子图节点，允许自定义输入和输出键：

<Code
  language="java"
  title="可配置的子图节点" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/examples/SubgraphExample.java"
>
{`public static class ConfigurableSubGraphNode implements NodeAction {
    private final CompiledGraph subGraph;
    private final String inputKey;
    private final String outputKey;

    public ConfigurableSubGraphNode(
            CompiledGraph subGraph,
            String inputKey,
            String outputKey
    ) {
        this.subGraph = subGraph;
        this.inputKey = inputKey;
        this.outputKey = outputKey;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        // 从父状态读取指定键的数据
        Object input = state.value(inputKey).orElse(null);

        // 执行子图
        OverAllState subResult = subGraph.invoke(Map.of("input", input)).orElseThrow();

        // 将结果写入指定键
        Object output = subResult.value("result").orElse(null);
        return Map.of(outputKey, output);
    }
}

// 使用可配置的子图节点
ConfigurableSubGraphNode configurableNode = new ConfigurableSubGraphNode(
        compiledSubGraph,
        "data",      // 从父状态的 "data" 键读取
        "processed"  // 将结果写入父状态的 "processed" 键
);`}
</Code>

## 可视化

能够可视化图是很重要的，特别是当它们变得更加复杂时。Spring AI Alibaba Graph 提供了 `StateGraph.getGraph()` 方法来获取可视化格式（即图即代码表示，如 PlantUML）：

<Code
  language="java"
  title="可视化图" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/examples/SubgraphExample.java"
>
{`import com.alibaba.cloud.ai.graph.GraphRepresentation;

StateGraph stateGraph = new StateGraph(keyStrategyFactory)
    .addNode("node1", node1)
    .addNode("node2", node2)
    .addEdge(START, "node1")
    .addEdge("node1", "node2")
    .addEdge("node2", END);

// 获取 PlantUML 表示
GraphRepresentation representation = stateGraph.getGraph(
    GraphRepresentation.Type.PLANTUML,
    "My Graph"
);

System.out.println(representation.content());`}
</Code>

## 流式处理

在添加编译的子图时，流式处理会自动启用。您可以通过 `stream()` 方法获取子图的流式输出：

<Code
  language="java"
  title="流式处理" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/examples/SubgraphExample.java"
>
{`import com.alibaba.cloud.ai.graph.CompiledGraph;
import reactor.core.publisher.Flux;

// 执行子图并获取流式输出
Flux<?> stream = compiledSubGraph.stream(Map.of("input", "test"));

// 处理流式输出
stream.subscribe(
        step -> System.out.println("Subgraph step: " + step),
        error -> System.err.println("Error: " + error.getMessage()),
        () -> System.out.println("Stream completed")
);`}
</Code>

## 多智能体系统示例

子图在构建多智能体系统时特别有用。每个智能体可以是一个独立的子图：

<Code
  language="java"
  title="多智能体系统示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/examples/SubgraphExample.java"
>
{`import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import java.util.HashMap;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

// 定义智能体状态策略
KeyStrategyFactory agentKeyStrategyFactory = () -> {
    HashMap<String, KeyStrategy> strategies = new HashMap<>();
    strategies.put("topic", new ReplaceStrategy());
    strategies.put("research", new ReplaceStrategy());
    strategies.put("summary", new ReplaceStrategy());
    strategies.put("outline", new ReplaceStrategy());
    strategies.put("content", new ReplaceStrategy());
    return strategies;
};

// 研究智能体子图
StateGraph researcherGraph = new StateGraph(agentKeyStrategyFactory)
    .addNode("research", node_async(state -> {
        String topic = (String) state.value("topic").orElse("");
        return Map.of("research", "Research on: " + topic);
    }))
    .addNode("summarize", node_async(state -> {
        String research = (String) state.value("research").orElse("");
        return Map.of("summary", "Summary: " + research);
    }))
    .addEdge(START, "research")
    .addEdge("research", "summarize")
    .addEdge("summarize", END);

CompiledGraph researcherAgent = researcherGraph.compile();

// 写作智能体子图
StateGraph writerGraph = new StateGraph(agentKeyStrategyFactory)
    .addNode("outline", node_async(state -> {
        String summary = (String) state.value("summary").orElse("");
        return Map.of("outline", "Outline based on: " + summary);
    }))
    .addNode("write", node_async(state -> {
        String outline = (String) state.value("outline").orElse("");
        return Map.of("content", "Content: " + outline);
    }))
    .addEdge(START, "outline")
    .addEdge("outline", "write")
    .addEdge("write", END);

CompiledGraph writerAgent = writerGraph.compile();

// 协调器（父图）
KeyStrategyFactory coordinatorKeyStrategyFactory = () -> {
    HashMap<String, KeyStrategy> strategies = new HashMap<>();
    strategies.put("topic", new ReplaceStrategy());
    strategies.put("research", new ReplaceStrategy());
    strategies.put("summary", new ReplaceStrategy());
    strategies.put("outline", new ReplaceStrategy());
    strategies.put("content", new ReplaceStrategy());
    return strategies;
};

StateGraph coordinatorGraph = new StateGraph(coordinatorKeyStrategyFactory)
    .addNode("researcher", researcherAgent)  // 直接使用编译的子图
    .addNode("writer", writerAgent)           // 直接使用编译的子图
    .addEdge(START, "researcher")
    .addEdge("researcher", "writer")
    .addEdge("writer", END);

CompiledGraph multiAgentSystem = coordinatorGraph.compile();

// 执行多智能体系统
Map<String, Object> result = multiAgentSystem.invoke(
    Map.of("topic", "AI的未来")
).orElseThrow();`}
</Code>

## 状态隔离示例

当子图需要完全独立的状态时，可以使用状态隔离模式：

<Code
  language="java"
  title="状态隔离示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/examples/SubgraphExample.java"
>
{`import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;

import java.util.Map;
import java.util.Optional;

public static class IsolatedSubGraphNode implements NodeAction {
    private final CompiledGraph subGraph;

    public IsolatedSubGraphNode(StateGraph subGraphDef) throws GraphStateException {
        this.subGraph = subGraphDef.compile();
    }

    @Override
    public Map<String, Object> apply(OverAllState parentState) {
        // 提取父状态数据
        String input = (String) parentState.value("input").orElse("");

        // 创建子图独立状态
        Map<String, Object> subState = Map.of("subInput", input);

        // 执行子图
        Optional<OverAllState> subResult = subGraph.invoke(subState);

        // 将子图结果映射回父状态
        String output = (String) subResult.get().value("subOutput").orElse("");
        return Map.of("output", output);
    }
}`}
</Code>

## 最佳实践

1. **明确接口**：定义清晰的输入和输出 schema，便于团队协作。
2. **状态隔离**：当子图需要独立状态时，使用转换节点或状态隔离模式。
3. **错误处理**：在调用子图时添加适当的错误处理，使用 `orElseThrow()` 或 `Optional` 处理。
4. **组件复用**：将常用的处理逻辑封装为子图，在多个地方复用。
5. **文档化**：为每个子图编写清晰的文档，说明其用途和接口。
6. **测试**：独立测试每个子图，然后测试整个系统。
7. **性能考虑**：编译的子图可以复用，避免重复编译。

## 总结

通过子图，您可以：
- **构建多智能体系统**：每个智能体作为独立的子图
- **实现组件复用**：在多个父图中重用相同的子图
- **支持团队协作**：不同团队可以独立开发子图
- **实现模块化设计**：将复杂系统分解为可管理的子图

通过合理使用子图，您可以构建模块化、可维护和可扩展的复杂 AI 工作流系统。
