---
title: 时光旅行 - 状态历史回溯
description: 学习如何在Spring AI Alibaba Graph中实现时光旅行，回溯和恢复历史状态
keywords: [时光旅行, Time Travel, 状态历史, 回溯, Checkpoint, StateHistory]
---

# 时光旅行 - 状态历史回溯

Spring AI Alibaba Graph 支持时光旅行功能，允许您查看和恢复 Graph 执行的历史状态。

## 配置 Checkpoint

要启用时光旅行，需要配置 Checkpointer：

```java
import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;

/**
 * 配置 Checkpoint
 */
public static CompiledGraph configureCheckpoint(StateGraph stateGraph) throws GraphStateException {
// 创建 Checkpointer
var checkpointer = new MemorySaver();

// 配置持久化
var compileConfig = CompileConfig.builder()
            .saverConfig(SaverConfig.builder()
                    .register(checkpointer)
                    .build())
    .build();

    return stateGraph.compile(compileConfig);
}
```

## 执行 Graph 并生成历史

```java
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.RunnableConfig;

import java.util.Map;

/**
 * 执行 Graph 并生成历史
 */
public static void executeGraphAndGenerateHistory(CompiledGraph graph) {
// 配置线程 ID
var config = RunnableConfig.builder()
    .threadId("conversation-1")
    .build();

// 执行 Graph
Map<String, Object> input = Map.of("query", "Hello");
    graph.invoke(input, config);

// 再次执行
graph.invoke(Map.of("query", "Follow-up question"), config);
}
```

## 查看状态历史

```java
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.state.StateSnapshot;

import java.util.List;

/**
 * 查看状态历史
 */
public static void viewStateHistory(CompiledGraph graph) {
    var config = RunnableConfig.builder()
            .threadId("conversation-1")
            .build();

// 获取所有历史状态
    List<StateSnapshot> history = (List<StateSnapshot>) graph.getStateHistory(config);

System.out.println("State history:");
for (int i = 0; i < history.size(); i++) {
    StateSnapshot snapshot = history.get(i);
    System.out.printf("Step %d: %s\n", i, snapshot.state());
        System.out.printf("  Checkpoint ID: %s\n", snapshot.config().checkPointId().orElse("N/A"));
    System.out.printf("  Node: %s\n", snapshot.node());
    }
}
```

**输出示例**:
```
State history:
Step 0: {query=Follow-up question, answer=Response to follow-up}
  Checkpoint ID: abc123
  Node: answer_node
Step 1: {query=Hello, answer=Initial response}
  Checkpoint ID: def456
  Node: answer_node
Step 2: {query=Hello}
  Checkpoint ID: ghi789
  Node: __START__
```

## 回溯到历史状态

```java
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.state.StateSnapshot;

import java.util.List;
import java.util.Map;

/**
 * 回溯到历史状态
 */
public static void travelBackToHistory(CompiledGraph graph) {
    var config = RunnableConfig.builder()
            .threadId("conversation-1")
            .build();

    // 获取所有历史状态
    List<StateSnapshot> history = (List<StateSnapshot>) graph.getStateHistory(config);

// 获取特定的历史状态
StateSnapshot historicalSnapshot = history.get(1);

// 使用历史状态的 checkpoint ID 创建新配置
var historicalConfig = RunnableConfig.builder()
    .threadId("conversation-1")
            .checkPointId(historicalSnapshot.config().checkPointId().orElse(null))
    .build();

// 从历史状态继续执行
graph.invoke(
    Map.of("query", "New question from historical state"),
    historicalConfig
);
}
```

## 分支创建

基于历史状态创建新的执行分支：

```java
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.state.StateSnapshot;

import java.util.List;
import java.util.Map;

/**
 * 分支创建
 */
public static void createBranch(CompiledGraph graph) {
    var config = RunnableConfig.builder()
            .threadId("conversation-1")
            .build();

    // 获取所有历史状态
    List<StateSnapshot> history = (List<StateSnapshot>) graph.getStateHistory(config);

    // 获取特定的历史状态
    StateSnapshot historicalSnapshot = history.get(1);

// 从历史状态创建新分支
var branchConfig = RunnableConfig.builder()
    .threadId("conversation-1-branch")  // 新的线程 ID
            .checkPointId(historicalSnapshot.config().checkPointId().orElse(null))
    .build();

// 在新分支上执行
    graph.invoke(
    Map.of("query", "Alternative path"),
    branchConfig
);
}
```

## 应用场景

1. **A/B 测试**: 从相同起点测试不同路径
2. **错误恢复**: 回溯到错误发生前的状态
3. **调试分析**: 检查每个步骤的状态变化
4. **用户撤销**: 允许用户撤销操作
5. **实验对比**: 比较不同决策的结果

## 状态快照信息

`StateSnapshot` 包含以下信息：

```java
public interface StateSnapshot {
    // 状态数据
    Map<String, Object> state();

    // 检查点配置
    RunnableConfig config();

    // 执行的节点名称
    String node();

    // 下一个要执行的节点
    String getNext();
}
```

## 完整示例

```java
import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.StateSnapshot;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * 完整示例
 */
public static void completeExample() throws GraphStateException {
// 构建 Graph
KeyStrategyFactory keyStrategyFactory = () -> {
    HashMap<String, KeyStrategy> strategies = new HashMap<>();
    strategies.put("messages", new AppendStrategy());
    strategies.put("step", new ReplaceStrategy());
    return strategies;
};

StateGraph builder = new StateGraph(keyStrategyFactory)
            .addNode("step1", node_async(state ->
        Map.of("messages", "Step 1", "step", 1)))
            .addNode("step2", node_async(state ->
        Map.of("messages", "Step 2", "step", 2)))
            .addNode("step3", node_async(state ->
        Map.of("messages", "Step 3", "step", 3)))
            .addEdge(START, "step1")
    .addEdge("step1", "step2")
    .addEdge("step2", "step3")
            .addEdge("step3", END);

// 配置持久化
var checkpointer = new MemorySaver();
var compileConfig = CompileConfig.builder()
            .saverConfig(SaverConfig.builder()
                    .register(checkpointer)
                    .build())
    .build();

CompiledGraph graph = builder.compile(compileConfig);

// 执行
var config = RunnableConfig.builder()
    .threadId("demo")
    .build();

graph.invoke(Map.of(), config);

// 查看历史
    List<StateSnapshot> history = (List<StateSnapshot>) graph.getStateHistory(config);
history.forEach(snapshot -> {
    System.out.println("State: " + snapshot.state());
    System.out.println("Node: " + snapshot.node());
    System.out.println("---");
});

// 回溯到 step1
StateSnapshot step1Snapshot = history.stream()
    .filter(s -> "step1".equals(s.node()))
    .findFirst()
    .orElseThrow();

var replayConfig = RunnableConfig.builder()
    .threadId("demo")
            .checkPointId(step1Snapshot.config().checkPointId().orElse(null))
    .build();

// 从 step1 重新执行
graph.invoke(Map.of(), replayConfig);
}
```

## 注意事项

1. **性能考虑**: 历史记录会占用内存，生产环境建议使用持久化存储
2. **数据一致性**: 确保状态数据可序列化
3. **版本兼容**: Graph 结构改变时历史状态可能不兼容
4. **清理策略**: 定期清理旧的历史记录

## 相关文档

- [持久化](./persistence) - 状态持久化
- [等待用户输入](./human-in-the-loop) - 中断和恢复
- [Checkpoint 机制](../examples/checkpoint-redis) - 检查点详解
- [快速入门](../quick-start) - Graph 基础使用

