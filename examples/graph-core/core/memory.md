---
title: 内存管理
description: 使用 Spring AI Alibaba Graph 框架实现短期和长期内存管理，支持多轮对话和跨会话数据存储
keywords: [Spring AI Alibaba, 内存管理, 短期内存, 长期内存, 检查点, 会话管理]
---

# 内存管理

AI 应用程序需要支持在同一轮会话的多条消息间共享上下文，或者在不同的会话场景先共享上下文。在 Spring AI Alibaba Graph 中，您可以添加两种类型的内存：

* [添加短期内存](#添加短期内存)作为智能体状态的一部分，支持与智能体进行多轮聊天对话。
* [添加长期内存](#添加长期内存)是指跨会话存储的用户特定或应用程序级别的数据。

## 添加短期内存

**短期内存**（会话级持久化）使智能体能够跟踪多轮对话。要添加短期内存：

<Code
  language="java"
  title="添加短期内存" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/core/MemoryExample.java"
>
{`// 创建内存检查点器
MemorySaver checkpointer = new MemorySaver();

SaverConfig saverConfig = SaverConfig.builder()
    .register(checkpointer)
    .build();

// 定义状态策略
KeyStrategyFactory keyStrategyFactory = () -> {
    Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
    keyStrategyMap.put("messages", new AppendStrategy());
    return keyStrategyMap;
};

// 构建图
StateGraph stateGraph = new StateGraph(keyStrategyFactory)
    .addNode("agent", agentNode)
    .addEdge(START, "agent")
    .addEdge("agent", END);

// 使用检查点器编译图
CompiledGraph graph = stateGraph.compile(
    CompileConfig.builder()
        .saverConfig(saverConfig)
        .build()
);

// 使用会话 ID 调用图
RunnableConfig config = RunnableConfig.builder()
    .threadId("user-session-1")
    .build();

Map<String, Object> input = Map.of(
    "messages", List.of(
        Map.of("role", "user", "content", "你好！我是 Bob")
    )
);

graph.invoke(input, config);`}
</Code>

### 生产环境使用

在生产环境中，使用由数据库支持的检查点器：

#### Redis 检查点器

<Code
  language="java"
  title="Redis 检查点器配置" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/core/MemoryExample.java"
>
{`import com.alibaba.cloud.ai.graph.checkpoint.savers.RedisSaver;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.constant.SaverConstant;

// Redis 配置
String redisHost = "localhost";
int redisPort = 6379;

RedisSaver redisSaver = new RedisSaver(redisHost, redisPort);

SaverConfig saverConfig = SaverConfig.builder()
    .register(SaverConstant.REDIS, redisSaver)
    .build();

CompiledGraph graph = stateGraph.compile(
    CompileConfig.builder()
        .saverConfig(saverConfig)
        .build()
);`}
</Code>

### 完整示例：使用短期内存的多轮对话

<Code
  language="java"
  title="使用短期内存的多轮对话示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/core/MemoryExample.java"
>
{`import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;

import org.springframework.ai.chat.client.ChatClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

// 定义状态策略
KeyStrategyFactory keyStrategyFactory = () -> {
    Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
    keyStrategyMap.put("messages", new AppendStrategy());
    return keyStrategyMap;
};

// 创建聊天节点
var chatNode = node_async(state -> {
    List<Map<String, String>> messages =
        (List<Map<String, String>>) state.value("messages").orElse(List.of());

    // 使用 ChatClient 调用 AI 模型
    ChatClient chatClient = chatClientBuilder.build();
    String response = chatClient.prompt()
        .user(messages.get(messages.size() - 1).get("content"))
        .call()
        .content();

    return Map.of("messages", List.of(
        Map.of("role", "assistant", "content", response)
    ));
});

// 构建图
StateGraph stateGraph = new StateGraph(keyStrategyFactory)
    .addNode("chat", chatNode)
    .addEdge(START, "chat")
    .addEdge("chat", END);

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

// 第一轮对话
RunnableConfig config = RunnableConfig.builder()
    .threadId("conversation-1")
    .build();

graph.invoke(Map.of("messages", List.of(
    Map.of("role", "user", "content", "你好！我是 Bob")
)), config);

// 第二轮对话（使用相同的 threadId）
graph.invoke(Map.of("messages", List.of(
    Map.of("role", "user", "content", "我的名字是什么？")
)), config);
// AI 将能够记住之前的对话，回答 "Bob"`}
</Code>

### 在子图中使用

如果您的图包含子图，您只需在编译父图时提供检查点器。Spring AI Alibaba Graph 将自动将检查点器传播到子图。

<Code
  language="java"
  title="在子图中使用检查点器" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/core/MemoryExample.java"
>
{`import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import static com.alibaba.cloud.ai.graph.StateGraph.START;

// 定义状态
KeyStrategyFactory keyStrategyFactory = () -> {
    Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
    keyStrategyMap.put("foo", new ReplaceStrategy());
    return keyStrategyMap;
};

// 子图
var subgraphNode = node_async(state -> {
    String foo = (String) state.value("foo").orElse("");
    return Map.of("foo", foo + "bar");
});

StateGraph subgraphBuilder = new StateGraph(keyStrategyFactory)
    .addNode("subgraph_node_1", subgraphNode)
    .addEdge(START, "subgraph_node_1");

// 子图不需要检查点器
CompiledGraph subgraph = subgraphBuilder.compile();

// 父图
StateGraph parentBuilder = new StateGraph(keyStrategyFactory)
    .addNode("node_1", state -> {
        // 调用子图
        return subgraph.invoke(state.data(),
            RunnableConfig.builder().build());
    })
    .addEdge(START, "node_1");

// 只在父图编译时提供检查点器
SaverConfig saverConfig = SaverConfig.builder()
    .register(SaverConstant.MEMORY, new MemorySaver())
    .build();

CompiledGraph graph = parentBuilder.compile(
    CompileConfig.builder()
        .saverConfig(saverConfig)
        .build()
);`}
</Code>

如果您希望子图拥有自己的内存，可以使用适当的检查点器选项编译它。这在多智能体系统中很有用，如果您希望智能体跟踪其内部消息历史。

## 添加长期内存

使用长期内存跨对话存储用户特定或应用程序特定的数据。

Spring AI Alibaba 借助 Store 组件来实现记忆的写入或读取管理。Store 是一个抽象接口，可以有不同的实现（如 `MemoryStore`、`RedisStore` 等），用于持久化存储跨会话的数据。

### 使用 Store 存储用户信息

在节点中使用 Store 存储和检索用户特定的长期数据：

<Code
  language="java"
  title="使用 Store 存储用户信息" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/core/MemoryExample.java"
>
{`import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.alibaba.cloud.ai.graph.store.Store;
import com.alibaba.cloud.ai.graph.store.StoreItem;
import com.alibaba.cloud.ai.graph.store.stores.MemoryStore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig.node_async;

// 在节点中使用 Store 存储用户信息
var userProfileNode = node_async((state, config) -> {
    String userId = (String) state.value("userId").orElse("");

    if (userId.isEmpty()) {
        return Map.of("userProfile", Map.of("name", "Unknown", "preferences", "default"));
    }

    // 从 Store 获取用户配置
    Store store = config.store();
    if (store != null) {
        Optional<StoreItem> itemOpt = store.getItem(List.of("user_profiles"), userId);
        if (itemOpt.isPresent()) {
            Map<String, Object> userProfile = itemOpt.get().getValue();
            return Map.of("userProfile", userProfile);
        }
    }

    // 如果未找到，返回默认值
    Map<String, Object> userProfile = Map.of("name", "User", "preferences", "default");
    return Map.of("userProfile", userProfile);
});

// 创建图
KeyStrategyFactory keyStrategyFactory = () -> {
    Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
    keyStrategyMap.put("userId", new ReplaceStrategy());
    keyStrategyMap.put("userProfile", new ReplaceStrategy());
    return keyStrategyMap;
};

StateGraph stateGraph = new StateGraph(keyStrategyFactory)
        .addNode("load_profile", userProfileNode)
        .addEdge(START, "load_profile")
        .addEdge("load_profile", END);

CompiledGraph graph = stateGraph.compile(CompileConfig.builder().build());

// 创建长期记忆存储并预填充数据
MemoryStore memoryStore = new MemoryStore();
Map<String, Object> profileData = new HashMap<>();
profileData.put("name", "张三");
profileData.put("preferences", "喜欢编程");
StoreItem profileItem = StoreItem.of(List.of("user_profiles"), "user_001", profileData);
memoryStore.putItem(profileItem);

// 运行图
RunnableConfig config = RunnableConfig.builder()
        .threadId("profile_thread")
        .store(memoryStore)
        .build();

Optional<OverAllState> stateOptional = graph.invoke(Map.of("userId", "user_001"), config);
Map<String, Object> result = stateOptional.get().data();
System.out.println("加载的用户配置: " + result.get("userProfile"));`}
</Code>

**说明**：
- 使用 `AsyncNodeActionWithConfig.node_async` 来访问 `RunnableConfig`，从而获取 `Store` 实例
- 通过 `config.store()` 获取 Store，可能为 `null`，需要做空值检查
- 使用 `store.getItem(namespace, key)` 从 Store 中获取数据，返回 `Optional<StoreItem>`
- 使用 `StoreItem.of(namespace, key, value)` 创建 StoreItem，然后通过 `store.putItem(item)` 存储数据
- 在 `RunnableConfig` 中通过 `.store(memoryStore)` 指定要使用的 Store 实例

### 使用 Store 实现缓存

使用 Store 实现缓存机制，避免重复执行耗时操作：

<Code
  language="java"
  title="使用 Store 实现缓存" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/core/MemoryExample.java"
>
{`import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.alibaba.cloud.ai.graph.store.Store;
import com.alibaba.cloud.ai.graph.store.StoreItem;
import com.alibaba.cloud.ai.graph.store.stores.MemoryStore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig.node_async;

var cacheNode = node_async((state, config) -> {
    String key = (String) state.value("cacheKey").orElse("");

    if (key.isEmpty()) {
        return Map.of("result", "no_key");
    }

    // 从 Store 获取缓存数据
    Store store = config.store();
    if (store != null) {
        Optional<StoreItem> itemOpt = store.getItem(List.of("cache"), key);
        if (itemOpt.isPresent()) {
            // 缓存命中
            Map<String, Object> cachedData = itemOpt.get().getValue();
            return Map.of("result", cachedData.get("value"));
        }
    }

    // 缓存未命中，执行计算或查询
    Object computedData = performExpensiveOperation(key);

    // 存储到 Store
    if (store != null) {
        Map<String, Object> cacheValue = new HashMap<>();
        cacheValue.put("value", computedData);
        StoreItem cacheItem = StoreItem.of(List.of("cache"), key, cacheValue);
        store.putItem(cacheItem);
    }

    return Map.of("result", computedData);
});

// 创建图
KeyStrategyFactory keyStrategyFactory = () -> {
    Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
    keyStrategyMap.put("cacheKey", new ReplaceStrategy());
    keyStrategyMap.put("result", new ReplaceStrategy());
    return keyStrategyMap;
};

StateGraph stateGraph = new StateGraph(keyStrategyFactory)
        .addNode("cache", cacheNode)
        .addEdge(START, "cache")
        .addEdge("cache", END);

CompiledGraph graph = stateGraph.compile(CompileConfig.builder().build());

// 创建长期记忆存储
MemoryStore memoryStore = new MemoryStore();

// 第一次调用（缓存未命中）
RunnableConfig config = RunnableConfig.builder()
        .threadId("cache_thread")
        .store(memoryStore)
        .build();

Optional<OverAllState> stateOptional = graph.invoke(Map.of("cacheKey", "expensive_key"), config);
Map<String, Object> result1 = stateOptional.get().data();
System.out.println("第一次调用结果: " + result1.get("result"));

// 第二次调用（缓存命中）
Optional<OverAllState> stateOptional2 = graph.invoke(Map.of("cacheKey", "expensive_key"), config);
Map<String, Object> result2 = stateOptional2.get().data();
System.out.println("第二次调用结果（从缓存）: " + result2.get("result"));`}
</Code>

**辅助方法**（在实际代码中定义）：

<Code
  language="java"
  title="模拟耗时操作" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/core/MemoryExample.java"
>
{`// 模拟耗时操作
private static Object performExpensiveOperation(String key) {
    // 模拟耗时计算
    return "computed_result_for_" + key;
}`}
</Code>

**说明**：
- 缓存逻辑：首先检查 Store 中是否存在缓存数据，如果存在则直接返回（缓存命中），否则执行耗时操作并将结果存储到 Store（缓存未命中）
- 使用 `List.of("cache")` 作为命名空间来组织缓存数据
- 同一个 `RunnableConfig` 和 `Store` 实例可以在多次调用中复用，实现跨调用的缓存持久化

## 完整示例：结合短期和长期内存

<Code
  language="java"
  title="结合短期和长期内存的完整示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/core/MemoryExample.java"
>
{`import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.alibaba.cloud.ai.graph.store.Store;
import com.alibaba.cloud.ai.graph.store.StoreItem;
import com.alibaba.cloud.ai.graph.store.stores.MemoryStore;

import org.springframework.ai.chat.client.ChatClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig.node_async;

// 定义状态
KeyStrategyFactory keyStrategyFactory = () -> {
    Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
    keyStrategyMap.put("userId", new ReplaceStrategy());
    keyStrategyMap.put("messages", new AppendStrategy());
    keyStrategyMap.put("userPreferences", new ReplaceStrategy());
    return keyStrategyMap;
};

// 加载用户偏好（长期内存）
var loadUserPreferences = node_async((state, config) -> {
    String userId = (String) state.value("userId").orElse("");

    if (userId.isEmpty()) {
        return Map.of("userPreferences", Map.of("theme", "default", "language", "zh"));
    }

    // 从 Store 加载用户偏好
    Store store = config.store();
    if (store != null) {
        Optional<StoreItem> itemOpt = store.getItem(List.of("user_preferences"), userId);
        if (itemOpt.isPresent()) {
            Map<String, Object> preferences = itemOpt.get().getValue();
            return Map.of("userPreferences", preferences);
        }
    }

    // 如果未找到，返回默认偏好
    Map<String, Object> preferences = Map.of("theme", "dark", "language", "zh");
    return Map.of("userPreferences", preferences);
});

// 聊天节点（使用短期和长期内存）
var chatNode = node_async(state -> {
    List<Map<String, String>> messages =
        (List<Map<String, String>>) state.value("messages").orElse(List.of());
    Map<String, Object> preferences =
        (Map<String, Object>) state.value("userPreferences").orElse(Map.of());

    // 构建包含用户偏好的提示
    String userPrompt = messages.get(messages.size() - 1).get("content");
    String enhancedPrompt = "用户偏好: " + preferences + "\n用户问题: " + userPrompt;

    // 调用 AI
    ChatClient chatClient = chatClientBuilder.build();
    String response = chatClient.prompt()
        .user(enhancedPrompt)
        .call()
        .content();

    return Map.of("messages", List.of(
        Map.of("role", "assistant", "content", response)
    ));
});

// 构建图
StateGraph stateGraph = new StateGraph(keyStrategyFactory)
    .addNode("load_preferences", loadUserPreferences)
    .addNode("chat", chatNode)
    .addEdge(START, "load_preferences")
    .addEdge("load_preferences", "chat")
    .addEdge("chat", END);

// 配置检查点（短期内存）
SaverConfig saverConfig = SaverConfig.builder()
        .register(new MemorySaver())
    .build();

// 编译图
CompiledGraph graph = stateGraph.compile(
    CompileConfig.builder()
        .saverConfig(saverConfig)
        .build()
);

// 创建长期记忆存储并预填充用户偏好
MemoryStore memoryStore = new MemoryStore();
Map<String, Object> preferencesData = new HashMap<>();
preferencesData.put("theme", "dark");
preferencesData.put("language", "zh");
preferencesData.put("timezone", "Asia/Shanghai");
StoreItem preferencesItem = StoreItem.of(List.of("user_preferences"), "user_002", preferencesData);
memoryStore.putItem(preferencesItem);

// 运行图
RunnableConfig config = RunnableConfig.builder()
        .threadId("combined_thread")
        .store(memoryStore)
    .build();

// 第一轮对话（加载偏好并开始对话）
graph.invoke(Map.of(
        "userId", "user_002",
        "messages", List.of(Map.of("role", "user", "content", "你好"))
), config);

// 第二轮对话（使用短期和长期记忆）
graph.invoke(Map.of(
        "userId", "user_002",
        "messages", List.of(Map.of("role", "user", "content", "根据我的偏好给我一些建议"))
), config);`}
</Code>

通过这种方式，您的应用程序可以同时利用短期内存（对话历史）和长期内存（用户偏好），提供更个性化和上下文感知的体验。

