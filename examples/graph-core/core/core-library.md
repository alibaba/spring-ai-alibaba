---
title: 核心概念
description: 深入了解 Spring AI Alibaba Graph 核心概念，包括状态管理、节点和边的定义
keywords: [Spring AI Alibaba, Graph, 核心概念, State, Nodes, Edges, 状态管理, 工作流]
---

# 核心库：概念指南

## 图（Graphs）

Spring AI Alibaba Graph 将智能体工作流建模为图。您可以使用三个关键组件来定义智能体的行为：

1. [State](#state状态)：共享的数据结构，表示应用程序的当前快照。它由 `OverAllState` 对象表示。

2. [Nodes](#节点nodes)：一个**函数式接口** (`AsyncNodeAction`)，编码智能体的逻辑。它们接收当前的 `State` 作为输入，执行一些计算或副作用，并返回更新后的 `State`。或者使用 `AsyncNodeActionWithConfig`，它可以额外接收 `RunnableConfig` 用于传递上下文。

3. [Edges](#边edges)：一个**函数式接口** (`AsyncEdgeAction`)，根据当前的 `State` 确定接下来执行哪个 `Node`。它们可以是条件分支或固定转换。或者使用 `AsyncEdgeActionWithConfig`，它可以额外接收 `RunnableConfig` 用于传递上下文。

通过组合 `Nodes` 和 `Edges`，您可以创建复杂的循环工作流，工作流在工作过程中持续更新 `State`，Spring AI Alibaba 会管理好 `State`，并确保 `State` 在工作流中传递并持久化。

在 Graph 中，`Nodes` 和 `Edges` 就像函数一样 - 它们可以包含 LLM 调用或只是普通的 Java 代码。

简而言之：_节点完成工作，边决定下一步做什么_。

### StateGraph

`StateGraph` 类 Spring AI Alibaba Graph 中的核心定义，它通过用户定义的状态策略进行参数化。

### 编译图

要构建您的图，首先定义 [state](#state状态)，然后添加 [nodes](#节点nodes) 和 [edges](#边edges)，最后编译它。编译图是什么意思？为什么需要编译？

编译是一个非常简单的步骤，它提供了对图结构的一些基本检查（没有孤立节点等），这也是您可以指定运行时参数（如检查点器和中断点）的地方。

编译本身并没有什么额外复杂的操作，它只是帮你做图编排的检查、预设置一些 config 参数而已。调用 `.compile()` 方法来编译图：

<Code
  language="java"
  title="编译图" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/core/CoreLibraryExample.java"
>
{`import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.CompiledGraph;

// 编译您的图
CompiledGraph graph = stateGraph.compile();`}
</Code>

在使用图之前，您**必须**编译它。

### Schema

## OverAllState（状态）

定义图时首先要做的是定义图的 `State`。`State` 由图的 `Key` 以及 `KeyStrategy 函数` 组成，`KeyStrategy 函数` 用于多个节点更新同一个 `key` 时应该如何处理多个值（比如合并或覆盖）。`State` 的 key 将是图中所有 `Nodes` 和 `Edges` 的输入 schema。所有 `Nodes` 将发出对 `State` 的更新，通过返回一个包含一系列 key-value 对的 Map，然后图引擎会使用指定的 `KeyStrategy` 函数应用这些更新到 `State`。

### KeyStrategy

KeyStrategy 是理解如何将节点的更新应用到 `State` 的关键。`State` 中的每个键都有自己独立的 Strategy 策略。如果没有显式指定 Strategy 策略，则默认使用 AppendStrategy，即假定该键的所有更新都应覆盖它。

让我们看几个例子来更好地理解它们。

**示例 A:**

<Code
  language="java"
  title="创建 KeyStrategyFactory" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/core/CoreLibraryExample.java"
>
{`import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import java.util.HashMap;
import java.util.Map;

public static KeyStrategyFactory createKeyStrategyFactory() {
    return () -> {
        Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
        keyStrategyMap.put("messages", new AppendStrategy());
        return keyStrategyMap;
    };
}

var graphBuilder = new StateGraph(createKeyStrategyFactory());`}
</Code>

### ReplaceStrategy（覆盖策略）

`ReplaceStrategy` 会用新值完全替换旧值。当多个节点返回同一个 key 的 Map 时，后执行的节点会覆盖先执行节点的值。

**示例：演示 ReplaceStrategy 的替换效果**

<Code
  language="java"
  title="ReplaceStrategy 示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/core/CoreLibraryExample.java"
>
{`// 定义状态策略，使用 ReplaceStrategy
KeyStrategyFactory keyStrategyFactory = () -> {
    Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
    keyStrategyMap.put("value", new ReplaceStrategy());  // 使用替换策略
    return keyStrategyMap;
};

// 节点 A：返回 value = "初始值"
var nodeA = node_async(state -> {
    return Map.of("value", "初始值");
});

// 节点 B：返回 value = "更新后的值"（会覆盖节点 A 的值）
var nodeB = node_async(state -> {
    return Map.of("value", "更新后的值");
});

// 构建图
StateGraph stateGraph = new StateGraph(keyStrategyFactory)
        .addNode("node_a", nodeA)
        .addNode("node_b", nodeB)
        .addEdge(START, "node_a")
        .addEdge("node_a", "node_b")
        .addEdge("node_b", END);

// 编译并执行
CompiledGraph graph = stateGraph.compile();

RunnableConfig config = RunnableConfig.builder()
        .threadId("replace-strategy-demo")
        .build();

// 执行图
Optional<OverAllStaste> stateOptional = graph.invoke(Map.of(), config);

// 获取最终状态
System.out.println("最终状态中的 value: " + (String)stateOptional.get().value("value"));
// 输出: 最终状态中的 value: 更新后的值
// 注意：节点 A 的值 "初始值" 已被节点 B 的值 "更新后的值" 完全替换`}
</Code>

**执行流程说明：**

1. **节点 A 执行**：状态中 `value = "初始值"`
2. **节点 B 执行**：由于使用 `ReplaceStrategy`，`value` 被替换为 `"更新后的值"`
3. **最终状态**：`value = "更新后的值"`（节点 A 的值已被完全覆盖）

### AppendStrategy（追加策略）

`AppendStrategy` 会将新值追加到旧值中。当多个节点返回同一个 key 的 Map 时，后执行的节点的值会被追加到先执行节点的值之后。

**示例：演示 AppendStrategy 的追加效果**

<Code
  language="java"
  title="AppendStrategy 示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/core/CoreLibraryExample.java"
>
{`// 定义状态策略，使用 AppendStrategy
KeyStrategyFactory keyStrategyFactory = () -> {
    Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
    keyStrategyMap.put("messages", new AppendStrategy());  // 使用追加策略
    return keyStrategyMap;
};

// 节点 A：返回 messages = "消息1"
var nodeA = node_async(state -> {
    return Map.of("messages", "消息1");
});

// 节点 B：返回 messages = "消息2"（会追加到节点 A 的值之后）
var nodeB = node_async(state -> {
    return Map.of("messages", "消息2");
});

// 节点 C：返回 messages = "消息3"（会追加到之前的消息之后）
var nodeC = node_async(state -> {
    return Map.of("messages", "消息3");
});

// 构建图
StateGraph stateGraph = new StateGraph(keyStrategyFactory)
        .addNode("node_a", nodeA)
        .addNode("node_b", nodeB)
        .addNode("node_c", nodeC)
        .addEdge(START, "node_a")
        .addEdge("node_a", "node_b")
        .addEdge("node_b", "node_c")
        .addEdge("node_c", END);

// 编译并执行
CompiledGraph graph = stateGraph.compile();

RunnableConfig config = RunnableConfig.builder()
        .threadId("append-strategy-demo")
        .build();

// 执行图
Optional<OverAllState> stateOptional = graph.invoke(Map.of(), config);

// 获取最终状态
List<String> messages = (List<String>) stateOptional.get().value("messages").orElse(List.of());
System.out.println("最终状态中的 messages: " + messages);
// 输出: 最终状态中的 messages: [消息1, 消息2, 消息3]
// 注意：所有节点的值都被追加到列表中，而不是被替换`}
</Code>

**执行流程说明：**

1. **节点 A 执行**：状态中 `messages = ["消息1"]`
2. **节点 B 执行**：由于使用 `AppendStrategy`，`messages` 变为 `["消息1", "消息2"]`
3. **节点 C 执行**：继续追加，`messages` 变为 `["消息1", "消息2", "消息3"]`
4. **最终状态**：`messages = ["消息1", "消息2", "消息3"]`（所有节点的值都被保留并追加）

#### 如何在 AppendStrategy 策略的删除消息

[AppendStrategy] 支持通过 [RemoveByHash] 删除消息。

Spring AI Alibaba 提供了内置的 [RemoveByHash]，允许通过比较其 `hashCode` 来删除消息，下面是其用法示例：

<Code
  language="java"
  title="RemoveByHash 使用示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/core/CoreLibraryExample.java"
>
{`import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import com.alibaba.cloud.ai.graph.state.RemoveByHash;

var workflow = new StateGraph(createKeyStrategyFactory())
        .addNode("agent_1", node_async(state ->
                Map.of("messages", "message1")))
        .addNode("agent_2", node_async(state ->
                Map.of("messages", "message2.1")))
        .addNode("agent_3", node_async(state ->
                Map.of("messages", RemoveByHash.of("message2.1")))) // 从消息值中删除 "message2.1"
        .addEdge(START, "agent_1")
        .addEdge("agent_1", "agent_2")
        .addEdge("agent_2", "agent_3")
        .addEdge("agent_3", END);`}
</Code>

### 自定义 KeyStrategy

您也可以为特定的状态属性指定自定义的 Strategy

## 序列化器（Serializer）

在图执行期间，状态需要被序列化（主要用于克隆目的），同时也为跨不同执行持久化状态提供能力。Spring AI Alibaba 目前提供了 Jackson、JDK 两种序列化策略实现。默认使用 Jackson 实现。

在序列化过程中，重点关注如下内容：

1. 不依赖不安全的标准序列化框架
2. 允许为第三方（非可序列化）类实现序列化
3. 尽可能避免类加载问题
4. 在序列化过程中管理可空值

### 自定义序列化器

默认情况下，Graph 使用 Jackson 作为序列化器，并且对几个主流模型厂商的 Message 类型做了兼容。但是对于用户自定义的消息类型，可能无法做到完全兼容，这里有三种常见做法：

#### 1. 为自定义数据类增加 Jackson 注解

为自定义数据类添加 Jackson 注解，提升序列化兼容性。这是最简单直接的方式：

<Code
  language="java"
  title="为自定义数据类增加 Jackson 注解" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/core/CoreLibraryExample.java"
>
{`import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// 使用 @JsonIgnoreProperties 忽略未知属性
@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomMessage {
    private String content;
    private String type;
    
    // 使用 @JsonCreator 和 @JsonProperty 指定构造函数参数映射
    @JsonCreator
    public CustomMessage(
            @JsonProperty("content") String content,
            @JsonProperty("type") String type) {
        this.content = content;
        this.type = type;
    }
    
    // Getter 和 Setter 方法
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
}`}
</Code>

#### 2. 定制 StateGraph 中的默认 Serializer

通过 `StateGraph.getStateSerializer()` 获取序列化器，转换为 `JacksonSerializer` 类型，然后获取 `ObjectMapper` 进行定制：

<Code
  language="java"
  title="定制 StateGraph 中的默认 Serializer" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/core/CoreLibraryExample.java"
>
{`import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.serializer.StateSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;

// 创建 StateGraph（使用默认的 JacksonSerializer）
StateGraph graph = new StateGraph(keyStrategyFactory);

// 获取序列化器并转换为 JacksonSerializer
StateSerializer stateSerializer = graph.getStateSerializer();
if (stateSerializer instanceof StateGraph.JacksonSerializer) {
    StateGraph.JacksonSerializer jacksonSerializer = 
            (StateGraph.JacksonSerializer) stateSerializer;
    
    // 获取 ObjectMapper
    ObjectMapper objectMapper = jacksonSerializer.getObjectMapper();
    
    // 定制 ObjectMapper，例如：
    // 1. 配置序列化选项
    objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    
    // 2. 注册自定义模块
    var module = new SimpleModule();
    // module.addSerializer(CustomMessage.class, new CustomMessageSerializer());
    // module.addDeserializer(CustomMessage.class, new CustomMessageDeserializer());
    objectMapper.registerModule(module);
}`}
</Code>

**说明**：
- `StateGraph` 默认使用 `JacksonSerializer`（继承自 `SpringAIJacksonStateSerializer`）
- 通过 `getStateSerializer()` 获取序列化器后，可以转换为 `JacksonSerializer` 类型
- 使用 `getObjectMapper()` 方法获取 `ObjectMapper` 进行定制
- 这种方式适合在创建图之后需要动态定制序列化器的场景

#### 3. 为 Graph 提供自定义序列化器（推荐）

可以为 StateGraph 指定任意序列化器，如框架已经提供的 JDK 原生类型 ObjectStateSerializer 或自定义的 Gson 序列化器实现等。

<Code
  language="java"
  title="为 Graph 提供自定义序列化器" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/core/CoreLibraryExample.java"
>
{`import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.serializer.plain_text.jackson.SpringAIJacksonStateSerializer;
import com.alibaba.cloud.ai.graph.state.AgentStateFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.alibaba.cloud.ai.graph.StateGraph;

import java.io.IOException;

// 自定义消息类型
public class CustomMessage {
    private String content;
    private String type;
    
    // 构造函数、Getter、Setter...
    public CustomMessage(String content, String type) {
        this.content = content;
        this.type = type;
    }
    
    public String getContent() { return content; }
    public String getType() { return type; }
}

// 自定义序列化器
public class CustomMessageSerializer extends JsonSerializer<CustomMessage> {
    @Override
    public void serialize(CustomMessage value, JsonGenerator gen, SerializerProvider serializers) 
            throws IOException {
        gen.writeStartObject();
        gen.writeStringField("content", value.getContent());
        gen.writeStringField("type", value.getType());
        gen.writeEndObject();
    }
}

// 自定义反序列化器
public class CustomMessageDeserializer extends JsonDeserializer<CustomMessage> {
    @Override
    public CustomMessage deserialize(JsonParser p, DeserializationContext ctxt) 
            throws IOException {
        String content = null;
        String type = null;
        
        p.nextToken(); // 跳过 START_OBJECT
        while (p.nextToken() != com.fasterxml.jackson.core.JsonToken.END_OBJECT) {
            String fieldName = p.getCurrentName();
            p.nextToken();
            
            if ("content".equals(fieldName)) {
                content = p.getText();
            } else if ("type".equals(fieldName)) {
                type = p.getText();
            }
        }
        
        return new CustomMessage(content, type);
    }
}

// 创建自定义序列化器类
class CustomizedSerializer extends SpringAIJacksonStateSerializer {
    
    public CustomizedSerializer() {
        super(OverAllState::new);
        
        // 创建 SimpleModule 并注册自定义序列化器/反序列化器
        var module = new SimpleModule();
        module.addSerializer(CustomMessage.class, new CustomMessageSerializer());
        module.addDeserializer(CustomMessage.class, new CustomMessageDeserializer());
        
        // 注册模块到 ObjectMapper
        objectMapper.registerModule(module);
        
        // 可以继续定制 ObjectMapper，例如：
        // objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
    }
}

// 使用自定义序列化器创建 StateGraph
KeyStrategyFactory keyStrategyFactory = () -> new HashMap<>();
StateGraph graph = new StateGraph("demo", keyStrategyFactory, new CustomizedSerializer());`}
</Code>

**说明**：
- `SpringAIJacksonStateSerializer` 在构造函数中已经注册了 Spring AI 相关的消息类型序列化器
- 继承后可以在构造函数中继续注册自定义类型的序列化器/反序列化器
- 可以通过 `objectMapper` 字段（继承自父类）访问和定制 `ObjectMapper`
- 这种方式可以完全控制序列化行为，适合复杂的自定义类型

## 节点（Nodes）

在 Spring AI Alibaba 中，节点通常是
 * 一个**函数式接口** ([AsyncNodeAction])，其入参是 [OverAllState](#state)，您可以使用 [addNode] 方法将这些节点添加到图中。
 * 一个**函数式接口** ([AsyncNodeActionWithConfig])，其入参是 [OverAllState](#state) 和 RunnableConfig，您可以使用 [addNode] 方法将这些节点添加到图中。

由于 [AsyncNodeAction] 设计用于与 [CompletableFuture] 一起工作，您可以使用 `node_async` 静态方法将其适配为更简单的同步场景。


### `START` 节点
// add a normal edge
`START` 节点是一个特殊节点，表示将用户输入发送到图的节点。引用此节点的主要目的是确定首先应该调用哪些节点。

<Code
  language="java"
  title="使用 START 节点" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/core/CoreLibraryExample.java"
>
{`import static com.alibaba.cloud.ai.graph.StateGraph.START;`}
</Code>

### `END` 节点

`END` 节点是一个特殊节点，表示终端节点。当您想要表示哪些边在完成后没有任何操作时，会引用此节点。

## 边（Edges）

边定义了逻辑如何路由以及图如何决定停止。这是智能体工作方式和不同节点之间如何通信的重要部分。有几种关键类型的边：

- **普通边（Normal Edges）**：
  > 直接从一个节点到下一个节点。
- **条件边（Conditional Edges）**：
  > 调用函数来确定接下来要去哪个节点。
- **入口点（Entry Point）**：
  > 当用户输入到达时首先调用哪个节点。
- **条件入口点（Conditional Entry Point）**：
  > 调用函数来确定当用户输入到达时首先调用哪个节点。

### 普通边

如果您**总是**想从节点 A 到节点 B，可以直接使用 [addEdge] 方法。

<Code
  language="java"
  title="添加条件边" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/core/CoreLibraryExample.java"
>
{`import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;

// 添加条件边
graph.addConditionalEdges("nodeA", edge_async(state -> "nodeB"),
        Map.of("nodeB", "nodeB", "nodeC", "nodeC"));`}
</Code>

您必须提供一个对象，将 `routingFunction` 的输出映射到下一个节点的名称。

### Conditional Edges

若需**可选地**路由至一个或多个 Node 节点，可使用 addConditionalEdges 方法。该方法接收节点名称及一个**函数式接口** AsyncEdgeAction 或 AsyncEdgeActionWithConfig，该接口将作为“路由函数”在节点执行后调用，用来决策下一个节点应该走向哪里。

### 为同一个节点设置出边

一个节点可以拥有多个出边，这样就形成了节点间的并行关系。如果一个节点有多个出边，那么所有这些目标节点将作为下一个并行执行。关于并行节点，可参考示例目录中的详细文档描述。


## 会话（Threads）

会话支持对多个不同运行进行检查点，这对于多租户聊天应用程序和其他需要维护独立状态的场景至关重要。会话是分配给 checkpointer 保存的一系列检查点的唯一 ID。使用 checkpointer 时，必须在运行图时指定 `thread_id`。

<Code
  language="java"
  title="使用会话 ID" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/core/CoreLibraryExample.java"
>
{`// 指定会话 ID
RunnableConfig config = RunnableConfig.builder().threadId("unique-id-1").build();

// 调用 Graph 时传进去

Flux<NodeOutput> stream = graph.stream(Map.of("input", "你好"), config);

//可以在多次调用间传递同一个会话 ID
RunnableConfig config2 = RunnableConfig.builder().threadId("unique-id-1").build();
Flux<NodeOutput> stream2 = graph.stream(Map.of("input", "你好"), config2);`}
</Code>

### Checkpointer（检查点）

Spring AI Alibaba 具有内置的持久化层，通过 Checkpointers 实现。当您将 checkpointer 与图一起使用时，可以与图的状态进行交互。checkpointer 在每一步保存图状态的_检查点_，实现几个强大的功能：

首先，checkpointers 通过允许人类检查、中断和批准步骤来促进**人机协作工作流**。这些工作流需要 checkpointers，因为人类必须能够在任何时间点查看图的状态，并且图必须能够在人类对状态进行任何更新后恢复执行。

其次，它允许在交互之间保持"记忆"。您可以使用 checkpointers 创建会话并在图执行后保存会话的状态。在重复的人类交互（如对话）的情况下，任何后续消息都可以发送到该检查点，它将保留之前的记忆。

每条 Checkpoint 中记录了如下内容，它们可以作为检视和恢复图的基础：

- **state**：这是此时的状态值。
- **nextNodeId**：这是图中接下来要执行的节点的标识符。

