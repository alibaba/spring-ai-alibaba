---
title: 为图添加持久化（记忆）
description: 学习如何使用Checkpointer为StateGraph提供持久化记忆，跨多个交互共享上下文
keywords: [持久化, Persistence, 记忆, Checkpointer, MemorySaver, StateGraph, 状态持久化]
---

# 为图添加持久化（"记忆"）

许多 AI 应用程序需要记忆来跨多个交互共享上下文。在 Spring AI Alibaba 中，通过 [`Checkpointer`] 为任何 [`StateGraph`] 提供记忆。

## 核心概念

在创建任何 Spring AI Alibaba 工作流时，可以通过以下方式设置持久化：

1. 创建一个 [`Checkpointer`]，例如 [`MemorySaver`]
2. 在编译图时通过 [`CompileConfig`] 传递 Checkpointer
3. 使用 `threadId` 来标识不同的会话

[`StateGraph`]: https://github.com/alibaba/spring-ai-alibaba/blob/main/spring-ai-alibaba-graph-core/src/main/java/com/alibaba/cloud/ai/graph/StateGraph.java
[`Checkpointer`]: https://github.com/alibaba/spring-ai-alibaba/blob/main/spring-ai-alibaba-graph-core/src/main/java/com/alibaba/cloud/ai/graph/checkpoint/Checkpoint.java
[`MemorySaver`]: https://github.com/alibaba/spring-ai-alibaba/blob/main/spring-ai-alibaba-graph-core/src/main/java/com/alibaba/cloud/ai/graph/checkpoint/savers/MemorySaver.java
[`CompileConfig`]: https://github.com/alibaba/spring-ai-alibaba/blob/main/spring-ai-alibaba-graph-core/src/main/java/com/alibaba/cloud/ai/graph/CompileConfig.java

## 初始化配置

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;

private static final Logger log = LoggerFactory.getLogger("Persistence");
```

## 定义状态和策略

状态是在图中所有节点之间共享的数据结构。Spring AI Alibaba 使用 `KeyStrategyFactory` 来定义状态键的行为。

```java
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

// 自定义状态类
public class ConversationState extends OverAllState {

    public ConversationState(Map<String, Object> initData) {
        super(initData);
    }

    public Optional<List<String>> messages() {
        return value("messages");
    }

    public Optional<String> userName() {
        return value("user_name");
    }
}

// 配置状态键策略
KeyStrategyFactory keyStrategyFactory = () -> {
    HashMap<String, KeyStrategy> strategies = new HashMap<>();
    strategies.put("messages", new AppendStrategy());      // 消息追加
    strategies.put("user_name", new ReplaceStrategy());    // 用户名替换
    strategies.put("context", new ReplaceStrategy());       // 上下文替换
    return strategies;
};
```

## 创建带工具的 Agent 节点

我们将创建一个简单的搜索工具来演示如何在持久化环境中使用工具。

### 定义工具函数

```java
import java.util.function.Function;

// 搜索工具
public class SearchTool implements Function<SearchTool.Request, String> {

    public record Request(String query) {}

    @Override
    public String apply(Request request) {
        log.info("Executing search for: {}", request.query());
        // 模拟搜索结果
        return "Search result: The weather is cold with a low of 13 degrees";
    }
}
```

### 创建 Agent 节点

```java
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.model.function.FunctionCallbackWrapper;

class AgentNode implements NodeAction {

    private final ChatClient chatClient;

    public AgentNode(ChatClient.Builder chatClientBuilder, SearchTool searchTool) {
        // 配置工具
        FunctionCallback searchCallback = FunctionCallbackWrapper.builder(searchTool)
            .withName("search")
            .withDescription("Search for information, check weather, and retrieve data")
            .build();

        this.chatClient = chatClientBuilder
            .defaultFunctions(searchCallback)
            .build();
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        ConversationState convState = (ConversationState) state;

        // 获取最后一条消息
        List<String> messages = convState.messages().orElse(List.of());
        String lastMessage = messages.isEmpty() ? "" : messages.get(messages.size() - 1);

        log.info("Processing message: {}", lastMessage);

        // 调用 LLM（会自动处理工具调用）
        String response = chatClient.prompt()
            .user(lastMessage)
            .call()
            .content();

        return Map.of("messages", response);
    }
}
```

### 定义路由逻辑

```java
import com.alibaba.cloud.ai.graph.action.EdgeAction;

class RouteMessage implements EdgeAction {

    @Override
    public String apply(OverAllState state) throws Exception {
        ConversationState convState = (ConversationState) state;

        // 获取消息列表
        List<String> messages = convState.messages().orElse(List.of());

        if (messages.isEmpty()) {
            return "exit";
        }

        // 简单的路由逻辑：检查最后一条消息是否需要工具调用
        String lastMessage = messages.get(messages.size() - 1);

        // 如果消息包含工具调用相关内容，继续；否则结束
        if (lastMessage.contains("search") || lastMessage.contains("weather")) {
            return "continue";
        }

        return "exit";
    }
}
```

## 构建带持久化的 Graph

### 不使用 Checkpointer

首先，让我们看看不使用持久化时的行为：

```java
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.nodeasync;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edgeasync;

// 配置 ChatClient
ChatClient.Builder chatClientBuilder = ChatClient.builder(chatModel);

// 创建工具和节点
SearchTool searchTool = new SearchTool();
var agentNode = nodeasync(new AgentNode(chatClientBuilder, searchTool));
var routeMessage = edgeasync(new RouteMessage());

// 构建 Graph（不使用 checkpointer）
StateGraph workflow = new StateGraph(keyStrategyFactory)
    .addNode("agent", agentNode)
    .addEdge(StateGraph.START, "agent")
    .addConditionalEdges("agent", routeMessage,
        Map.of(
            "continue", "agent",
            "exit", StateGraph.END
        ));

CompiledGraph graph = workflow.compile();
```

### 测试不带持久化的 Graph

```java
// 第一次调用 - 介绍自己
log.info("=== First call - Introduction ===");
var result1 = graph.invoke(Map.of("messages", "Hi, I'm Alice, nice to meet you"));

List<String> messages1 = (List<String>) result1.data().get("messages");
log.info("Response: {}", messages1.get(messages1.size() - 1));

// 第二次调用 - 询问名字（没有持久化，无法记住）
log.info("=== Second call - Ask name ===");
var result2 = graph.invoke(Map.of("messages", "What's my name?"));

List<String> messages2 = (List<String>) result2.data().get("messages");
log.info("Response: {}", messages2.get(messages2.size() - 1));
```

**输出**（不带持久化）:
```
=== First call - Introduction ===
Response: Hello Alice, nice to meet you too!

=== Second call - Ask name ===
Response: I don't have information about your name. Could you please tell me?
```

可以看到，没有持久化时，Graph 无法记住之前的对话内容。

## 添加持久化（记忆）

现在让我们添加 `MemorySaver` 来实现持久化：

```java
import com.alibaba.cloud.ai.graph.checkpoint.MemorySaver;
import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.RunnableConfig;

// 创建 Checkpointer
var checkpointer = new MemorySaver();

// 配置持久化
var compileConfig = CompileConfig.builder()
    .checkpointSaver(checkpointer)
    .build();

// 编译带持久化的 Graph
CompiledGraph persistentGraph = workflow.compile(compileConfig);
```

### 测试带持久化的 Graph

```java
// 创建运行配置（使用 threadId 标识会话）
var config = RunnableConfig.builder()
    .threadId("user-alice-session")
    .build();

// 第一次调用 - 介绍自己
log.info("=== First call with persistence - Introduction ===");
var result1 = persistentGraph.invoke(
    Map.of("messages", "Hi, I'm Alice, nice to meet you"),
    config
);

List<String> messages1 = (List<String>) result1.data().get("messages");
log.info("Response: {}", messages1.get(messages1.size() - 1));

// 第二次调用 - 询问名字（有持久化，可以记住）
log.info("=== Second call with persistence - Ask name ===");
var result2 = persistentGraph.invoke(
    Map.of("messages", "What's my name?"),
    config
);

List<String> messages2 = (List<String>) result2.data().get("messages");
log.info("Response: {}", messages2.get(messages2.size() - 1));

// 第三次调用 - 继续对话
log.info("=== Third call - Continue conversation ===");
var result3 = persistentGraph.invoke(
    Map.of("messages", "What did I say in my first message?"),
    config
);

List<String> messages3 = (List<String>) result3.data().get("messages");
log.info("Response: {}", messages3.get(messages3.size() - 1));
```

**输出**（带持久化）:
```
=== First call with persistence - Introduction ===
Response: Hello Alice, nice to meet you too! How can I help you today?

=== Second call with persistence - Ask name ===
Response: Your name is Alice!

=== Third call - Continue conversation ===
Response: You said "Hi, I'm Alice, nice to meet you"
```

## 多会话隔离

使用不同的 `threadId` 可以创建完全独立的会话：

```java
// Alice 的会话
var aliceConfig = RunnableConfig.builder()
    .threadId("user-alice")
    .build();

persistentGraph.invoke(Map.of("messages", "Hi, I'm Alice"), aliceConfig);

// Bob 的会话
var bobConfig = RunnableConfig.builder()
    .threadId("user-bob")
    .build();

persistentGraph.invoke(Map.of("messages", "Hi, I'm Bob"), bobConfig);

// Alice 询问名字 - 能记住
var aliceResult = persistentGraph.invoke(
    Map.of("messages", "What's my name?"),
    aliceConfig
);
log.info("Alice: {}", aliceResult.data().get("messages"));
// 输出: Your name is Alice

// Bob 询问名字 - 也能记住
var bobResult = persistentGraph.invoke(
    Map.of("messages", "What's my name?"),
    bobConfig
);
log.info("Bob: {}", bobResult.data().get("messages"));
// 输出: Your name is Bob
```

## 检查和管理状态

### 获取当前状态

```java
import com.alibaba.cloud.ai.graph.StateSnapshot;

// 获取当前状态快照
StateSnapshot snapshot = persistentGraph.getState(config);

log.info("Current node: {}", snapshot.node());
log.info("Current state: {}", snapshot.state());
log.info("Next node: {}", snapshot.getNext());
log.info("Checkpoint ID: {}", snapshot.config().checkpointId());
```

### 获取状态历史

```java
import java.util.List;

// 获取所有历史状态
List<StateSnapshot> history = persistentGraph.getStateHistory(config);

log.info("=== State History ===");
for (int i = 0; i < history.size(); i++) {
    StateSnapshot h = history.get(i);
    log.info("Step {}: node={}, messages count={}",
        i,
        h.node(),
        ((List<?>) h.state().get("messages")).size()
    );
}
```

### 清除特定会话的状态

```java
// 删除特定线程的所有 checkpoint
checkpointer.delete("user-alice");

// 或者删除特定的 checkpoint
String checkpointId = snapshot.config().checkpointId();
checkpointer.delete(checkpointId);
```

## 完整示例：带工具调用的持久化对话

```java
import com.alibaba.cloud.ai.graph.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.function.FunctionCallbackWrapper;

public class PersistenceExample {

    private static final Logger log = LoggerFactory.getLogger(PersistenceExample.class);

    public static void main(String[] args) {
        // 1. 配置状态策略
        KeyStrategyFactory keyStrategyFactory = () -> {
            HashMap<String, KeyStrategy> strategies = new HashMap<>();
            strategies.put("messages", new AppendStrategy());
            strategies.put("user_name", new ReplaceStrategy());
            return strategies;
        };

        // 2. 创建工具和节点
        SearchTool searchTool = new SearchTool();
        ChatClient.Builder builder = ChatClient.builder(chatModel);
        var agentNode = nodeasync(new AgentNode(builder, searchTool));

        // 3. 构建 Graph
        StateGraph workflow = new StateGraph(keyStrategyFactory)
            .addNode("agent", agentNode)
            .addEdge(StateGraph.START, "agent")
            .addEdge("agent", StateGraph.END);

        // 4. 配置持久化
        var checkpointer = new MemorySaver();
        var compileConfig = CompileConfig.builder()
            .checkpointSaver(checkpointer)
            .build();

        CompiledGraph graph = workflow.compile(compileConfig);

        // 5. 测试持久化对话
        var config = RunnableConfig.builder()
            .threadId("demo-session")
            .build();

        // 第一轮对话
        graph.invoke(Map.of("messages", "Hi, I'm Charlie"), config);

        // 第二轮对话 - 能记住名字
        var result = graph.invoke(Map.of("messages", "What's my name?"), config);
        log.info("Response: {}", result.data().get("messages"));

        // 第三轮对话 - 使用工具
        result = graph.invoke(Map.of("messages", "What's the weather like?"), config);
        log.info("Response: {}", result.data().get("messages"));

        // 查看状态历史
        List<StateSnapshot> history = graph.getStateHistory(config);
        log.info("Total conversation steps: {}", history.size());
    }
}
```

## 关键特性总结

| 特性 | 说明 |
|------|------|
| **会话隔离** | 使用不同的 `threadId` 创建独立的会话 |
| **状态恢复** | 相同 `threadId` 可以恢复之前的状态 |
| **历史追踪** | 可以查看状态的历史版本 |
| **工具调用记忆** | 持久化包括工具调用的历史 |
| **内存高效** | `MemorySaver` 适合开发和测试 |
| **可扩展** | 可以实现自定义 `Checkpointer` 用于持久化存储 |

## 应用场景

1. **多轮对话系统**: 记住用户的上下文和偏好
2. **客服机器人**: 跨会话跟踪客户问题
3. **工作流状态恢复**: 长时间运行的任务可以中断和恢复
4. **A/B 测试**: 比较不同会话的处理结果
5. **审计和调试**: 追踪完整的对话历史

## 与非持久化的对比

| 特性 | 无持久化 | 有持久化 (MemorySaver) |
|------|---------|----------------------|
| 记忆能力 | ❌ 每次调用独立 | ✅ 跨调用记忆 |
| 会话隔离 | N/A | ✅ 通过 threadId |
| 状态恢复 | ❌ 不支持 | ✅ 支持 |
| 历史查询 | ❌ 不支持 | ✅ 支持 |
| 适用场景 | 单次查询 | 多轮对话 |

## 相关文档

- [等待用户输入](./human-in-the-loop) - 中断和恢复示例
- [时光旅行](./time-travel) - 状态历史导航
- [快速入门](../quick-start) - Graph 基础使用
