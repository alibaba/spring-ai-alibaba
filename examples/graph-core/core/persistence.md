---
title: 持久化
description: 使用 Spring AI Alibaba Graph 框架持久化和管理工作流状态，实现跨执行的状态保持
keywords: [Spring AI Alibaba, Checkpoint, 检查点, 持久化, 状态管理, 工作流状态]
---

# 持久化

Spring AI Alibaba Graph 具有内置的持久化层，通过检查点（Checkpointers）实现。当您使用检查点编译图时，检查点会在每个超级步骤（super-step）保存图状态的`检查点`。这些检查点保存到一个`会话`（thread）中，可以在图执行后访问。

由于`会话`允许在执行后访问图的状态，因此几个强大的功能都成为可能，包括人在回路中（human-in-the-loop）、内存、时间旅行和容错能力。下面，我们将详细讨论这些概念。

## 会话

会话是分配给检查点器保存的每个检查点的唯一 ID 或会话标识符。它包含一系列运行的累积状态。当执行运行时，图的底层状态将被持久化到会话。

当使用检查点调用图时，您**必须**在配置的 `RunnableConfig` 中指定一个 `threadId`。

<Code
  language="java"
  title="指定 threadId" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/core/PersistenceExample.java"
>
{`RunnableConfig config = RunnableConfig.builder()
    .threadId("1")
    .build();`}
</Code>

可以检索会话的当前和历史状态。要持久化状态，必须在执行运行之前创建会话。

## 检查点（Checkpoints）

会话在特定时间点的状态称为检查点。检查点是在每个超级步骤保存的图状态快照，由 `StateSnapshot` 对象表示，具有以下关键属性：

* `config`: 与此检查点关联的配置。
* `metadata`: 与此检查点关联的元数据。
* `values`: 此时状态通道的值。
* `next`: 图中下一个要执行的节点名称元组。
* `tasks`: 包含有关下一个要执行的任务的信息的 `PregelTask` 对象元组。

检查点是持久化的，可以用于在稍后的时间恢复会话的状态。

让我们看看当一个简单的图被调用时保存了哪些检查点：

<Code
  language="java"
  title="检查点示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/core/PersistenceExample.java"
>
{`import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

// 定义状态策略
KeyStrategyFactory keyStrategyFactory = () -> {
    Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
    keyStrategyMap.put("foo", new ReplaceStrategy());
    keyStrategyMap.put("bar", new AppendStrategy());
    return keyStrategyMap;
};

// 定义节点操作
var nodeA = node_async(state -> {
    return Map.of("foo", "a", "bar", List.of("a"));
});

var nodeB = node_async(state -> {
    return Map.of("foo", "b", "bar", List.of("b"));
});

// 创建图
StateGraph stateGraph = new StateGraph(keyStrategyFactory)
    .addNode("node_a", nodeA)
    .addNode("node_b", nodeB)
    .addEdge(START, "node_a")
    .addEdge("node_a", "node_b")
    .addEdge("node_b", END);

// 配置检查点
SaverConfig saverConfig = SaverConfig.builder()
        .register(new MemorySaver())
    .build();

// 编译图
CompiledGraph graph = stateGraph.compile(
    CompileConfig.builder()
        .saverConfig(saverConfig)
        .build()
);

// 运行图
RunnableConfig config = RunnableConfig.builder()
    .threadId("1")
    .build();

Map<String, Object> input = new HashMap<>();
input.put("foo", "");

graph.invoke(input, config);`}
</Code>

运行图后，我们期望看到恰好 4 个检查点：

* 空检查点，`START` 作为下一个要执行的节点
* 带有用户输入 `{'foo': '', 'bar': []}` 和 `node_a` 作为下一个要执行的节点的检查点
* 带有 `node_a` 的输出 `{'foo': 'a', 'bar': ['a']}` 和 `node_b` 作为下一个要执行的节点的检查点
* 带有 `node_b` 的输出 `{'foo': 'b', 'bar': ['a', 'b']}` 且没有下一个要执行的节点的检查点

请注意，`bar` 通道值包含两个节点的输出，因为我们对 `bar` 通道使用了追加策略（AppendStrategy）。

### 获取状态

当与保存的图状态交互时，您**必须**指定一个[会话标识符](#会话threads)。您可以通过调用 `graph.getState(config)` 来查看图的*最新*状态。这将返回一个 `StateSnapshot` 对象，该对象对应于与配置中提供的会话 ID 关联的最新检查点，或者如果提供了检查点 ID，则对应于该会话的检查点。

<Code
  language="java"
  title="获取状态" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/core/PersistenceExample.java"
>
{`import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.state.StateSnapshot;

// 获取最新的状态快照
RunnableConfig config = RunnableConfig.builder()
    .threadId("1")
    .build();
StateSnapshot stateSnapshot = graph.getState(config);
System.out.println("Current state: " + stateSnapshot.state());
System.out.println("Current node: " + stateSnapshot.node());

// 获取特定 checkpoint_id 的状态快照
RunnableConfig configWithCheckpoint = RunnableConfig.builder()
    .threadId("1")
    .checkPointId("1ef663ba-28fe-6528-8002-5a559208592c")
    .build();

StateSnapshot specificSnapshot = graph.getState(configWithCheckpoint);
System.out.println("Specific checkpoint state: " + specificSnapshot.state());`}
</Code>

### 获取状态历史

您可以通过调用 `graph.getStateHistory(config)` 来获取给定会话的图执行的完整历史记录。这将返回与配置中提供的会话 ID 关联的 `StateSnapshot` 对象列表。重要的是，检查点将按时间顺序排序，最近的检查点/`StateSnapshot` 在列表的第一个位置。

<Code
  language="java"
  title="获取状态历史" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/core/PersistenceExample.java"
>
{`import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.state.StateSnapshot;

import java.util.List;

RunnableConfig config = RunnableConfig.builder()
    .threadId("1")
    .build();

List<StateSnapshot> history = (List<StateSnapshot>) graph.getStateHistory(config);
System.out.println("State history:");
for (int i = 0; i < history.size(); i++) {
    StateSnapshot snapshot = history.get(i);
    System.out.printf("Step %d: %s\n", i, snapshot.state());
    System.out.printf("  Checkpoint ID: %s\n", snapshot.config().checkPointId());
    System.out.printf("  Node: %s\n", snapshot.node());
}`}
</Code>


### 重放（Replay）

也可以重放先前的图执行。如果我们使用 `thread_id` 和 `checkpoint_id` 调用图的 `invoke` 方法，那么我们将*重放*之前执行的步骤（在对应于 `checkpoint_id` 的检查点*之前*），并且只执行检查点*之后*的步骤。

* `thread_id` 是会话的 ID。
* `checkpoint_id` 是指会话内特定检查点的标识符。

当调用图时，您必须将这些作为配置的 `configurable` 部分传递：

<Code
  language="java"
  title="重放图执行" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/core/PersistenceExample.java"
>
{`import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;

// 获取最新的状态快照
RunnableConfig config = RunnableConfig.builder()
    .threadId("1")
    .build();
StateSnapshot stateSnapshot = graph.getState(config);

RunnableConfig replayConfig = RunnableConfig.builder()
    .threadId("1")
     .checkPointId(stateSnapshot.config().checkPointId().orElse(""))
    .build();

graph.invoke(Map.of(), config);
System.out.println("Replay executed");`}
</Code>

重要的是，Spring AI Alibaba Graph 知道某个特定步骤是否之前已执行过。如果已执行，框架只是*重放*图中的该特定步骤，而不重新执行该步骤，但仅适用于提供的 `checkpoint_id` *之前*的步骤。`checkpoint_id` *之后*的所有步骤都将被执行（即新的分支），即使它们之前已被执行。

### 获取状态

您可以通过调用 `graph.getState(config)` 来获取 checkpointer 的状态，配置应包含 `thread_id`，并将为该会话获取状态。

<Code
  language="java"
  title="获取 checkpointer 状态" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/core/PersistenceExample.java"
>
{`RunnableConfig config = RunnableConfig.builder().threadId("unique-id-1").build();
graph.stream(Map.of("input", "你好"), config);

// 调用之后，可以在任意位置查看工作流的当前状态（即最后一条 Checkpoint 的位置）
StateSnapshot lastSnapshot = workflow.getState(config);`}
</Code>

### 获取状态历史

<Code
  language="java"
  title="获取状态历史" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/core/PersistenceExample.java"
>
{`RunnableConfig config = RunnableConfig.builder().threadId("unique-id-1").build();
Optional<NodeOutput> outputOptional = graph.invoke(inputs, config);

// 查看整个工作流的所有节点执行历史（即会返回所有 Checkpoint 记录）
Collection<StateSnapshot> history = workflow.getStateHistory(config);`}
</Code>

您还可以调用 `graph.getStateHistory(config)` 来获取图的历史记录列表。配置应包含 `thread_id`，并将为该会话获取状态历史记录。

### 更新状态

开发者还可以直接更新状态，使用 `graph.updateState(config, values, asNode)` 更新它。这可以让图重新执行之前执行过的节点，实现时光机回溯等能力。

<Code
  language="java"
  title="更新状态" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/core/PersistenceExample.java"
>
{`RunnableConfig config = RunnableConfig.builder().threadId("unique-id-1").build();

var updatedConfig = graph.updateState(config, Map.of("the_key_to", "newValue"), "the-next-node-to-to");`}
</Code>

* 配置应包含指定要更新哪个会话的 `thread_id`。
* `the-next-node-to-to` 可以覆盖当前 Checkpoint 的所在的节点，让图从指定的任何节点开始执行。

> 注意，updateState 并不会直接覆盖 OverAllState 对应 key 的值，而是会根据具体的 key 的更新策略（AppendStrategy 或 AppendStrategy 等）来决策更新动作，可能是直接覆盖，也可能是附加到列表最后。

以下是一个更新状态的示例，假设您使用以下 schema 定义了图的状态：

<Code
  language="java"
  title="定义状态策略" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/core/PersistenceExample.java"
>
{`KeyStrategyFactory keyStrategyFactory = () -> {
    Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
    keyStrategyMap.put("foo", new ReplaceStrategy());  // 替换策略
    keyStrategyMap.put("bar", new AppendStrategy());   // 追加策略
    return keyStrategyMap;
};`}
</Code>

现在假设图的当前状态是

```
{"foo": 1, "bar": ["a"]}
```

如果您按如下方式更新状态：

<Code
  language="java"
  title="更新状态示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/core/PersistenceExample.java"
>
{`import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

KeyStrategyFactory keyStrategyFactory = () -> {
    Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
    keyStrategyMap.put("foo", new ReplaceStrategy());  // 替换策略
    keyStrategyMap.put("bar", new AppendStrategy());   // 追加策略
    return keyStrategyMap;
};

RunnableConfig config = RunnableConfig.builder()
        .threadId("1")
        .build();

Map<String, Object> updates = new HashMap<>();
updates.put("foo", 2);
updates.put("bar", List.of("b"));

graph.updateState(config, updates, null);
System.out.println("State updated successfully");`}
</Code>

那么图的新状态将是：

```
{"foo": 2, "bar": ["a", "b"]}
```

`foo` 键（通道）被完全更改（因为该通道没有指定归约器，所以 `updateState` 覆盖它）。但是，为 `bar` 键指定了归约器（AppendStrategy），因此它将 `"b"` 追加到 `bar` 的状态。

## 检查点器实现

Spring AI Alibaba 提供了多种检查点器实现：

### MemorySaver

内存检查点器，将检查点保存在内存中：

<Code
  language="java"
  title="MemorySaver 配置" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/core/PersistenceExample.java"
>
{`import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.constant.SaverConstant;

SaverConfig saverConfig = SaverConfig.builder()
    .register(SaverConstant.MEMORY, new MemorySaver())
    .build();`}
</Code>

### PostgreSqlSaver

PostgreSQL 数据库检查点器，详见 [PostgreSQL 检查点持久化](../examples/checkpoint-redis)。

### RedisSaver

Redis 检查点器，将检查点保存到 Redis 中。

通过这些检查点器，您可以实现状态的持久化、人在回路中、时间旅行等强大功能。

### MongodbSaver

MongodbSaver 数据库检查点器