---
title: 人类反馈
keywords: [Spring AI, 通义千问, 百炼, 智能体应用, 人类反馈, Human-in-the-Loop, 中断, 人工介入, InterruptionMetadata, interruptBefore]
description: "在构建agent工作流中，人类反馈是很常见的场景，本期介绍如何利用Spring Ai Alibaba Graph构建工作流时，中断 -> 人类反馈介入 -> 无缝衔接剩下流程"
---

# 人类反馈（Human-in-the-Loop）

在实际业务场景中，经常会遇到人类介入的场景，人类的不同操作将影响工作流不同的走向。Spring AI Alibaba Graph 提供了两种方式来实现人类反馈：

1. **InterruptionMetadata 模式**：可以在任意节点随时中断，通过实现 `InterruptableAction` 接口来控制中断时机
2. **interruptBefore 模式**：需要提前在编译配置中定义中断点，在指定节点执行前中断

## 模式一：InterruptionMetadata 模式

InterruptionMetadata 模式允许节点在运行时动态决定是否需要中断，提供了最大的灵活性。节点通过实现 `InterruptableAction` 接口，可以在任意时刻返回 `InterruptionMetadata` 来中断执行。

### 优势

- **灵活性强**：可以在任意节点根据运行时状态决定是否中断
- **动态控制**：中断逻辑由节点自身控制，不需要提前配置
- **状态感知**：可以根据当前状态动态决定是否需要等待用户输入

### 定义带中断的 Graph

<Code
  language="java"
  title="定义带中断的 Graph" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/examples/HumanInTheLoopExample.java"
>
{`import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.action.InterruptableAction;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * 定义带中断的 Graph
 * 使用 InterruptableAction 实现中断，不需要 interruptBefore 配置
 */
public static CompiledGraph createGraphWithInterrupt() throws GraphStateException {
    // 定义普通节点
    var step1 = node_async(state -> {
        return Map.of("messages", "Step 1");
    });

    // 定义可中断节点（实现 InterruptableAction）
    var humanFeedback = new InterruptableNodeAction("human_feedback", "等待用户输入");

    var step3 = node_async(state -> {
        return Map.of("messages", "Step 3");
    });

    // 定义条件边：根据 human_feedback 的值决定路由
    var evalHumanFeedback = edge_async(state -> {
        var feedback = (String) state.value("human_feedback").orElse("unknown");
        return (feedback.equals("next") || feedback.equals("back")) ? feedback : "unknown";
    });

    // 配置 KeyStrategyFactory
    KeyStrategyFactory keyStrategyFactory = () -> {
        HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
        keyStrategyHashMap.put("messages", new AppendStrategy());
        keyStrategyHashMap.put("human_feedback", new ReplaceStrategy());
        return keyStrategyHashMap;
    };

    // 构建 Graph
    StateGraph builder = new StateGraph(keyStrategyFactory)
            .addNode("step_1", step1)
            .addNode("human_feedback", humanFeedback)  // 使用可中断节点
            .addNode("step_3", step3)
            .addEdge(START, "step_1")
            .addEdge("step_1", "human_feedback")
            .addConditionalEdges("human_feedback", evalHumanFeedback,
                    Map.of("back", "step_1", "next", "step_3", "unknown", "human_feedback"))
            .addEdge("step_3", END);

    // 配置内存保存器（用于状态持久化）
    var saver = new MemorySaver();

    var compileConfig = CompileConfig.builder()
            .saverConfig(SaverConfig.builder()
                    .register(saver)
                    .build())
            // 不再需要 interruptBefore 配置，中断由 InterruptableAction 控制
            .build();

    return builder.compile(compileConfig);
}`}
</Code>

### 实现 InterruptableNodeAction

<Code
  language="java"
  title="实现 InterruptableNodeAction" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/examples/HumanInTheLoopExample.java"
>
{`/**
 * 可中断的节点动作
 * 实现 InterruptableAction 接口，可以在任意节点中断执行
 */
public static class InterruptableNodeAction implements AsyncNodeActionWithConfig, InterruptableAction {
    private final String nodeId;
    private final String message;

    public InterruptableNodeAction(String nodeId, String message) {
        this.nodeId = nodeId;
        this.message = message;
    }

    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig config) {
        // 正常节点逻辑：更新状态
        return CompletableFuture.completedFuture(Map.of("messages", message));
    }

    @Override
    public Optional<InterruptionMetadata> interrupt(String nodeId, OverAllState state, RunnableConfig config) {
        // 检查是否需要中断
        // 如果状态中没有 human_feedback，则中断等待用户输入
        Optional<Object> humanFeedback = state.value("human_feedback");

        if (humanFeedback.isEmpty()) {
            // 返回 InterruptionMetadata 来中断执行
            InterruptionMetadata interruption = InterruptionMetadata.builder(nodeId, state)
                    .addMetadata("message", "等待用户输入...")
                    .addMetadata("node", nodeId)
                    .build();

            return Optional.of(interruption);
        }

        // 如果已经有 human_feedback，继续执行
        return Optional.empty();
    }
}`}
</Code>

### 执行 Graph 直到中断

<Code
  language="java"
  title="执行 Graph 直到中断" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/examples/HumanInTheLoopExample.java"
>
{`import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import reactor.core.publisher.Flux;

/**
 * 执行 Graph 直到中断
 * 检查流式输出中的 InterruptionMetadata
 */
public static InterruptionMetadata executeUntilInterrupt(CompiledGraph graph) {
    // 初始输入
    Map<String, Object> initialInput = Map.of("messages", "Step 0");

    // 配置线程 ID
    var invokeConfig = RunnableConfig.builder()
            .threadId("Thread1")
            .build();

    // 用于保存最后一个输出
    AtomicReference<NodeOutput> lastOutputRef = new AtomicReference<>();

    // 运行 Graph 直到第一个中断点
    graph.stream(initialInput, invokeConfig)
            .doOnNext(event -> {
                System.out.println("节点输出: " + event);
                lastOutputRef.set(event);
            })
            .doOnError(error -> System.err.println("流错误: " + error.getMessage()))
            .doOnComplete(() -> System.out.println("流完成"))
            .blockLast();

    // 检查最后一个输出是否是 InterruptionMetadata
    NodeOutput lastOutput = lastOutputRef.get();
    if (lastOutput instanceof InterruptionMetadata) {
        System.out.println("\n检测到中断: " + lastOutput);
        return (InterruptionMetadata) lastOutput;
    }

    return null;
}`}
</Code>

**输出**:
```
节点输出: NodeOutput{node=__START__, state={messages=[Step 0]}}
节点输出: NodeOutput{node=step_1, state={messages=[Step 0, Step 1]}}

检测到中断: InterruptionMetadata{node=human_feedback, state={messages=[Step 0, Step 1]}, metadata={message=等待用户输入..., node=human_feedback}}
```

### 等待用户输入并更新状态

<Code
  language="java"
  title="等待用户输入并更新状态" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/examples/HumanInTheLoopExample.java"
>
{`/**
 * 等待用户输入并更新状态
 */
public static RunnableConfig waitUserInputAndUpdateState(CompiledGraph graph, InterruptionMetadata interruption) throws Exception {
    var invokeConfig = RunnableConfig.builder()
            .threadId("Thread1")
            .build();

    // 检查当前状态
    System.out.printf("\n--State before update--\n%s\n", graph.getState(invokeConfig));

    // 模拟用户输入
    var userInput = "back"; // "back" 表示返回上一个节点
    System.out.printf("\n--User Input--\n用户选择: '%s'\n\n", userInput);

    // 更新状态：添加 human_feedback
    // 使用 updateState 更新状态，传入中断时的节点 ID
    var updatedConfig = graph.updateState(invokeConfig, Map.of("human_feedback", userInput), interruption.node());

    // 检查更新后的状态
    System.out.printf("--State after update--\n%s\n", graph.getState(updatedConfig));

    return updatedConfig;
}`}
</Code>

**输出**:
```
--State before update--
StateSnapshot{node=step_1, state={messages=[Step 0, Step 1]}, config=RunnableConfig{ threadId=Thread1, nextNode=human_feedback }}

--User Input--
用户选择: 'back'

--State after update--
StateSnapshot{node=step_1, state={messages=[Step 0, Step 1], human_feedback=back}, config=RunnableConfig{ threadId=Thread1, nextNode=human_feedback }}
```

### 继续执行 Graph

<Code
  language="java"
  title="继续执行 Graph" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/examples/HumanInTheLoopExample.java"
>
{`/**
 * 继续执行 Graph
 * 使用 HUMAN_FEEDBACK_METADATA_KEY 来恢复执行
 */
public static void continueExecution(CompiledGraph graph, RunnableConfig updatedConfig) {
    // 创建恢复配置，添加 HUMAN_FEEDBACK_METADATA_KEY
    RunnableConfig resumeConfig = RunnableConfig.builder(updatedConfig)
            .addMetadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY, "placeholder")
            .build();

    System.out.println("\n--继续执行 Graph--");

    // 继续执行 Graph（input 为 null，使用之前的状态）
    graph.stream(null, resumeConfig)
            .doOnNext(event -> System.out.println("节点输出: " + event))
            .doOnError(error -> System.err.println("流错误: " + error.getMessage()))
            .doOnComplete(() -> System.out.println("流完成"))
            .blockLast();
}`}
</Code>

**输出**:
```
--继续执行 Graph--
节点输出: NodeOutput{node=human_feedback, state={messages=[Step 0, Step 1], human_feedback=back}}
节点输出: NodeOutput{node=step_1, state={messages=[Step 0, Step 1], human_feedback=back}}
流完成
```

## 模式二：interruptBefore 模式

interruptBefore 模式需要在编译 Graph 时提前指定中断点，在指定节点执行前自动中断。这种方式适合已知的中断点，配置简单直接。

### 优势

- **配置简单**：只需在编译配置中指定中断点
- **无需修改节点**：普通节点即可，不需要实现特殊接口
- **明确的中断点**：中断位置在编译时确定，易于理解和维护

### 定义带中断的 Graph

<Code
  language="java"
  title="定义带中断的 Graph (interruptBefore 模式)" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/examples/HumanInTheLoopExample.java"
>
{`import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import java.util.HashMap;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * 定义带中断的 Graph
 * 使用 interruptBefore 配置在指定节点前中断
 */
public static CompiledGraph createGraphWithInterrupt() throws GraphStateException {
    // 定义节点
    var step1 = node_async(state -> {
        return Map.of("messages", "Step 1");
    });

    var humanFeedback = node_async(state -> {
        return Map.of(); // 等待用户输入，不修改状态
    });

    var step3 = node_async(state -> {
        return Map.of("messages", "Step 3");
    });

    // 定义条件边
    var evalHumanFeedback = edge_async(state -> {
        var feedback = (String) state.value("human_feedback").orElse("unknown");
        return (feedback.equals("next") || feedback.equals("back")) ? feedback : "unknown";
    });

    // 配置 KeyStrategyFactory
    KeyStrategyFactory keyStrategyFactory = () -> {
        HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
        keyStrategyHashMap.put("messages", new AppendStrategy());
        keyStrategyHashMap.put("human_feedback", new ReplaceStrategy());
        return keyStrategyHashMap;
    };

    // 构建 Graph
    StateGraph builder = new StateGraph(keyStrategyFactory)
            .addNode("step_1", step1)
            .addNode("human_feedback", humanFeedback)
            .addNode("step_3", step3)
            .addEdge(START, "step_1")
            .addEdge("step_1", "human_feedback")
            .addConditionalEdges("human_feedback", evalHumanFeedback,
                    Map.of("back", "step_1", "next", "step_3", "unknown", "human_feedback"))
            .addEdge("step_3", END);

    // 配置内存保存器和中断点
    var saver = new MemorySaver();

    var compileConfig = CompileConfig.builder()
            .saverConfig(SaverConfig.builder()
                    .register(saver)
                    .build())
            .interruptBefore("human_feedback") // 在 human_feedback 节点前中断
            .build();

    return builder.compile(compileConfig);
}`}
</Code>

### 执行 Graph 直到中断

<Code
  language="java"
  title="执行 Graph 直到中断 (interruptBefore 模式)" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/examples/HumanInTheLoopExample.java"
>
{`import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.RunnableConfig;

import java.util.Map;

import reactor.core.publisher.Flux;

/**
 * 执行 Graph 直到中断
 */
public static void executeUntilInterrupt(CompiledGraph graph) {
    // 初始输入
    Map<String, Object> initialInput = Map.of("messages", "Step 0");

    // 配置线程 ID
    var invokeConfig = RunnableConfig.builder()
            .threadId("Thread1")
            .build();

    // 运行 Graph 直到第一个中断点
    graph.stream(initialInput, invokeConfig)
            .doOnNext(event -> System.out.println(event))
            .doOnError(error -> System.err.println("流错误: " + error.getMessage()))
            .doOnComplete(() -> System.out.println("流完成"))
            .blockLast();
}`}
</Code>

**输出**:
```
NodeOutput{node=__START__, state={messages=[Step 0]}}
NodeOutput{node=step_1, state={messages=[Step 0, Step 1]}}
流完成
```

### 等待用户输入并更新状态

<Code
  language="java"
  title="等待用户输入并更新状态 (interruptBefore 模式)" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/examples/HumanInTheLoopExample.java"
>
{`import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.RunnableConfig;

import java.util.Map;

/**
 * 等待用户输入并更新状态
 */
public static RunnableConfig waitUserInputAndUpdateState(CompiledGraph graph) throws Exception {
    var invokeConfig = RunnableConfig.builder()
            .threadId("Thread1")
            .build();

    // 检查当前状态
    System.out.printf("--State before update--\n%s\n", graph.getState(invokeConfig));

    // 模拟用户输入
    var userInput = "back"; // "back" 表示返回上一个节点
    System.out.printf("\n--User Input--\n用户选择: '%s'\n\n", userInput);

    // 更新状态（模拟 human_feedback 节点的输出）
    // 注意：interruptBefore 模式下，传入 null 作为节点 ID
    var updateConfig = graph.updateState(invokeConfig, Map.of("human_feedback", userInput), null);

    // 检查更新后的状态
    System.out.printf("--State after update--\n%s\n", graph.getState(updateConfig));

    return updateConfig;
}`}
</Code>

**输出**:
```
--State before update--
StateSnapshot{node=step_1, state={messages=[Step 0, Step 1]}, config=RunnableConfig{ threadId=Thread1, nextNode=human_feedback }}

--User Input--
用户选择: 'back'

--State after update--
StateSnapshot{node=step_1, state={messages=[Step 0, Step 1], human_feedback=back}, config=RunnableConfig{ threadId=Thread1, nextNode=human_feedback }}
```

### 继续执行 Graph

<Code
  language="java"
  title="继续执行 Graph (interruptBefore 模式)" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/examples/HumanInTheLoopExample.java"
>
{`/**
 * 继续执行 Graph
 */
public static void continueExecution(CompiledGraph graph, RunnableConfig updateConfig) {
    // 继续执行 Graph（input 为 null，使用之前的状态）
    graph.stream(null, updateConfig)
            .doOnNext(event -> System.out.println(event))
            .doOnError(error -> System.err.println("流错误: " + error.getMessage()))
            .doOnComplete(() -> System.out.println("流完成"))
            .blockLast();
}`}
</Code>

**输出**:
```
NodeOutput{node=human_feedback, state={messages=[Step 0, Step 1], human_feedback=back}}
NodeOutput{node=step_1, state={messages=[Step 0, Step 1], human_feedback=back}}
流完成
```

### 第二次等待用户输入

<Code
  language="java"
  title="第二次等待用户输入" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/examples/HumanInTheLoopExample.java"
>
{`var userInput = "next"; // "next" 表示继续下一个节点
System.out.printf("\n--User Input--\n用户选择: '%s'\n", userInput);

// 更新状态
var updateConfig = graph.updateState(invokeConfig, Map.of("human_feedback", userInput), null);

System.out.printf("\ngetNext()\n\twith invokeConfig:[%s]\n\twith updateConfig:[%s]\n",
    graph.getState(invokeConfig).getNext(),
    graph.getState(updateConfig).getNext());`}
</Code>

### 继续执行直到完成

<Code
  language="java"
  title="继续执行直到完成" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/examples/HumanInTheLoopExample.java"
>
{`// 继续执行 Graph
graph.stream(null, updateConfig)
        .doOnNext(event -> System.out.println(event))
        .doOnError(error -> System.err.println("流错误: " + error.getMessage()))
        .doOnComplete(() -> System.out.println("流完成"))
        .blockLast();`}
</Code>

**输出**:
```
NodeOutput{node=human_feedback, state={messages=[Step 0, Step 1], human_feedback=next}}
NodeOutput{node=step_3, state={messages=[Step 0, Step 1, Step 3], human_feedback=next}}
NodeOutput{node=__END__, state={messages=[Step 0, Step 1, Step 3], human_feedback=next}}
流完成
```

## 两种模式对比

| 特性 | InterruptionMetadata 模式 | interruptBefore 模式 |
|------|-------------------------|---------------------|
| **中断时机** | 运行时动态决定 | 编译时预先定义 |
| **节点要求** | 需要实现 `InterruptableAction` 接口 | 普通节点即可 |
| **灵活性** | 高，可根据状态动态中断 | 中，需要在编译时确定 |
| **配置复杂度** | 需要实现接口方法 | 只需配置节点名称 |
| **适用场景** | 需要根据运行时状态决定是否中断 | 已知的固定中断点 |

## 应用场景

- **需要人工审核的审批流程**：在关键节点等待人工审核
- **需要用户确认的关键操作**：在执行重要操作前等待用户确认
- **交互式对话系统**：在对话过程中等待用户输入
- **多步骤表单填写**：在表单的每个步骤之间等待用户输入
- **动态工作流**：根据用户反馈动态调整工作流路径

## 完整示例：扩展-反馈-翻译流程

以下是一个完整的示例，展示如何在扩展节点、人类反馈节点和翻译节点之间实现中断和恢复：

### 节点定义

#### ExpanderNode

<Code
  language="java"
  title="ExpanderNode" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/examples/HumanInTheLoopExample.java"
>
{`package com.spring.ai.tutorial.graph.human.node;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.async.AsyncGenerator;
import com.alibaba.cloud.ai.graph.streaming.StreamingChatGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.PromptTemplate;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ExpanderNode implements NodeAction {

    private static final Logger logger = LoggerFactory.getLogger(ExpanderNode.class);

    private static final PromptTemplate DEFAULTPROMPTTEMPLATE = new PromptTemplate(
        "You are an expert at information retrieval and search optimization.\n" +
        "Your task is to generate {number} different versions of the given query.\n\n" +
        "Each variant must cover different perspectives or aspects of the topic,\n" +
        "while maintaining the core intent of the original query. The goal is to\n" +
        "expand the search space and improve the chances of finding relevant information.\n\n" +
        "Do not explain your choices or add any other text.\n" +
        "Provide the query variants separated by newlines.\n\n" +
        "Original query: {query}\n\n" +
        "Query variants:\n"
    );

    private final ChatClient chatClient;
    private final Integer NUMBER = 3;

    public ExpanderNode(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        logger.info("expander node is running.");

        String query = state.value("query", "");
        Integer expanderNumber = state.value("expandernumber", this.NUMBER);

        Flux<ChatResponse> chatResponseFlux = this.chatClient.prompt()
                .user((user) -> user.text(DEFAULTPROMPTTEMPLATE.getTemplate())
                        .param("number", expanderNumber)
                        .param("query", query))
                .stream()
                .chatResponse();

        AsyncGenerator<? extends NodeOutput> generator = StreamingChatGenerator.builder()
                .startingNode("expanderllmstream")
                .startingState(state)
                .mapResult(response -> {
                    String text = response.getResult().getOutput().getText();
                    List<String> queryVariants = Arrays.asList(text.split("\n"));
                    return Map.of("expandercontent", queryVariants);
                })
                .build(chatResponseFlux);

        return Map.of("expandercontent", generator);
    }
}`}
</Code>

#### TranslateNode

<Code
  language="java"
  title="TranslateNode" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/examples/HumanInTheLoopExample.java"
>
{`package com.spring.ai.tutorial.graph.human.node;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.async.AsyncGenerator;
import com.alibaba.cloud.ai.graph.streaming.StreamingChatGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.PromptTemplate;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class TranslateNode implements NodeAction {

    private static final Logger logger = LoggerFactory.getLogger(TranslateNode.class);

    private static final PromptTemplate DEFAULTPROMPTTEMPLATE = new PromptTemplate(
        "Given a user query, translate it to {targetLanguage}.\n" +
        "If the query is already in {targetLanguage}, return it unchanged.\n" +
        "If you don't know the language of the query, return it unchanged.\n" +
        "Do not add explanations nor any other text.\n\n" +
        "Original query: {query}\n\n" +
        "Translated query:\n"
    );

    private final ChatClient chatClient;
    private final String TARGETLANGUAGE = "English";

    public TranslateNode(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        logger.info("translate node is running.");

        String query = state.value("query", "");
        String targetLanguage = state.value("translatelanguage", TARGETLANGUAGE);

        Flux<ChatResponse> chatResponseFlux = this.chatClient.prompt()
                .user((user) -> user.text(DEFAULTPROMPTTEMPLATE.getTemplate())
                        .param("targetLanguage", targetLanguage)
                        .param("query", query))
                .stream()
                .chatResponse();

        AsyncGenerator<? extends NodeOutput> generator = StreamingChatGenerator.builder()
                .startingNode("translatellmstream")
                .startingState(state)
                .mapResult(response -> {
                    String text = response.getResult().getOutput().getText();
                    List<String> queryVariants = Arrays.asList(text.split("\n"));
                    return Map.of("translatecontent", queryVariants);
                })
                .build(chatResponseFlux);

        return Map.of("translatecontent", generator);
    }
}`}
</Code>

### 边定义

<Code
  language="java"
  title="边定义" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/examples/HumanInTheLoopExample.java"
>
{`package com.spring.ai.tutorial.graph.human.dispatcher;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.EdgeAction;

public class HumanFeedbackDispatcher implements EdgeAction {
    @Override
    public String apply(OverAllState state) throws Exception {
        return (String) state.value("humannextnode", StateGraph.END);
    }
}`}
</Code>

## 相关文档

- [Checkpoint 机制](./checkpoint-redis) - 状态持久化
- [快速入门](../quick-start) - Graph 基础使用
- [流式处理](./llm-streaming-springai) - 流式输出
