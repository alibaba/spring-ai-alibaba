---
title: 多智能体（Multi-agent）
description: 了解如何在Spring AI Alibaba中实现Multi-agent协作，包括工具调用和交接模式
keywords: [Multi-agent, Multi-Agent, 工具调用, Tool Calling, Handoffs, Agent协作, 子Agent]
---

**Multi-agent** 将复杂的应用程序分解为多个协同工作的专业化Agent。与依赖单个Agent处理所有步骤不同，**Multi-agent架构**允许你将更小、更专注的Agent组合成协调的工作流。

Multi-agent系统在以下情况下很有用：

* 单个Agent拥有太多工具，难以做出正确的工具选择决策
* 上下文或记忆增长过大，单个Agent难以有效跟踪
* 任务需要**专业化**（例如：规划器、研究员、数学专家）

## Multi-agent模式

Spring AI Alibaba支持以下Multi-agent模式：

| 模式 | 工作原理 | 控制流 | 使用场景 |
| ---- | -------- | ------ | -------- |
| [**Tool Calling**](#tool-calling) | Supervisor Agent将其他Agent作为*工具*调用。"工具"Agent不直接与用户对话——它们只执行任务并返回结果。 | 集中式：所有路由都通过调用Agent。 | 任务编排、结构化工作流。 |
| [**Handoffs**](#Handoffs) | 当前的Agent决定将控制权转移给另一个Agent。活动Agent随之变更，用户可以继续与新的Agent直接交互。 | 去中心化：Agent可以改变当前由谁来担当活跃Agent。 | 跨领域对话、专家接管。 |


## 选择模式

| 问题 | 工具调用 | 交接（Handoffs） |
| --- | --- | --- |
| 需要集中控制工作流程？ | ✅ 是 | ❌ 否 |
| 希望Agent直接与用户交互？ | ❌ 否 | ✅ 是 |
| 专家之间复杂的、类人对话？ | ❌ 有限 | ✅ 强 |

> 你可以混合使用两种模式——使用**交接**进行Agent切换，并让每个Agent**将子Agent作为工具调用**来执行专门任务。

## 自定义Agent上下文

Multi-agent设计的核心是**上下文工程**——决定每个Agent看到什么信息。Spring AI Alibaba 为你提供细粒度的控制：

* 将对话或状态的哪些部分传递给每个Agent
* 为子Agent定制专门的提示
* 包含/排除中间推理
* 为每个Agent自定义输入/输出格式

系统的质量**在很大程度上取决于**上下文工程。目标是确保每个Agent都能访问执行任务所需的正确数据，无论它是作为工具还是作为活动Agent。

## 交接（Handoffs）

在**交接**模式中，Agent可以直接将控制权传递给彼此。"活动"Agent会发生变化，用户与当前拥有控制权的Agent进行交互。

流程：

1. **当前Agent**决定它需要另一个Agent的帮助
2. 它将控制权（和状态）传递给**下一个Agent**
3. **新Agent**直接与用户交互，直到它决定再次交接或完成

### 顺序执行（Sequential Agent）

在**顺序执行**模式中，多个Agent按预定义的顺序依次执行。每个Agent的输出成为下一个Agent的输入。

流程：

1. **Agent A**处理初始输入
2. **Agent A**的输出传递给**Agent B**
3. **Agent B**处理并传递给**Agent C**
4. 最后一个Agent返回最终结果

![Spring AI Alibaba SequentialAgent](/img/agent/multi-agent/sequential.png)

#### 实现

```java
import com.alibaba.cloud.ai.graph.agent.flow.agent.SequentialAgent;
import com.alibaba.cloud.ai.graph.OverAllState;

// 创建专业化的子Agent
ReactAgent writerAgent = ReactAgent.builder()
    .name("writer_agent")
    .model(chatModel)
    .description("专业写作Agent")
    .instruction("你是一个知名的作家，擅长写作和创作。请根据用户的提问进行回答。")
    .outputKey("article") // [!code highlight]
    .build();

ReactAgent reviewerAgent = ReactAgent.builder()
    .name("reviewer_agent")
    .model(chatModel)
    .description("专业评审Agent")
    .instruction("你是一个知名的评论家，擅长对文章进行评论和修改。" +
                 "对于散文类文章，请确保文章中必须包含对于西湖风景的描述。" +
                 "最终只返回修改后的文章，不要包含任何评论信息。")
    .outputKey("reviewed_article") // [!code highlight]
    .build();

// 创建顺序Agent
SequentialAgent blogAgent = SequentialAgent.builder() // [!code highlight]
    .name("blog_agent")
    .description("根据用户给定的主题写一篇文章，然后将文章交给评论员进行评论")
    .subAgents(List.of(writerAgent, reviewerAgent)) // [!code highlight]
    .build();

// 使用
Optional<OverAllState> result = blogAgent.invoke("帮我写一个100字左右的散文");

if (result.isPresent()) {
    OverAllState state = result.get();

    // 访问第一个Agent的输出
    AssistantMessage article = (AssistantMessage) state.value("article").get();
    System.out.println("原始文章: " + article.getText());

    // 访问第二个Agent的输出
    AssistantMessage reviewedArticle = (AssistantMessage) state.value("reviewed_article").get();
    System.out.println("评审后文章: " + reviewedArticle.getText());
}
```

#### 关键特性

1. **按顺序执行**：Agent按照 `subAgents` 列表中定义的顺序执行
2. **状态传递**：每个Agent的输出通过 `outputKey` 存储在状态中，可被后续Agent访问
3. **消息历史**：默认情况下，所有Agent共享消息历史
4. **推理内容控制**：使用 `returnReasoningContents` 控制是否在消息历史中包含中间推理

#### 控制推理内容

```java
ReactAgent writerAgent = ReactAgent.builder()
    .name("writer_agent")
    .model(chatModel)
    .returnReasoningContents(true) // [!code highlight]
    .tools(List.of(poetToolCallback))
    .outputKey("article")
    .build();

ReactAgent reviewerAgent = ReactAgent.builder()
    .name("reviewer_agent")
    .model(chatModel)
    .returnReasoningContents(true) // [!code highlight]
    .tools(List.of(reviewerToolCallback))
    .outputKey("reviewed_article")
    .build();

SequentialAgent blogAgent = SequentialAgent.builder()
    .name("blog_agent")
    .subAgents(List.of(writerAgent, reviewerAgent))
    .build();

Optional<OverAllState> result = blogAgent.invoke("帮我写一个100字左右的散文");

// 消息历史将包含所有工具调用和推理过程
List<Message> messages = (List<Message>) result.get().value("messages").get();
System.out.println("消息数量: " + messages.size()); // 包含所有中间步骤
```

### 并行执行（Parallel Agent）

在**并行执行**模式中，多个Agent同时处理相同的输入。它们的结果被收集并合并。

流程：

1. 输入同时发送给**所有Agent**
2. 所有Agent**并行**处理
3. 结果被**合并**成单一输出

![Spring AI Alibaba ParallelAgent](/img/agent/multi-agent/parallel.png)

#### 实现

```java
import com.alibaba.cloud.ai.graph.agent.flow.agent.ParallelAgent;

// 创建多个专业化Agent
ReactAgent proseWriterAgent = ReactAgent.builder()
    .name("prose_writer_agent")
    .model(chatModel)
    .description("专门写散文的AI助手")
    .instruction("你是一个知名的散文作家，擅长写优美的散文。" +
                 "用户会给你一个主题，你只需要创作一篇100字左右的散文。")
    .outputKey("prose_result") // [!code highlight]
    .build();

ReactAgent poemWriterAgent = ReactAgent.builder()
    .name("poem_writer_agent")
    .model(chatModel)
    .description("专门写现代诗的AI助手")
    .instruction("你是一个知名的现代诗人，擅长写现代诗。" +
                 "用户会给你一个主题，你只需要创作一首现代诗。")
    .outputKey("poem_result") // [!code highlight]
    .build();

ReactAgent summaryAgent = ReactAgent.builder()
    .name("summary_agent")
    .model(chatModel)
    .description("专门做内容总结的AI助手")
    .instruction("你是一个专业的内容分析师，擅长对主题进行总结和提炼。" +
                 "用户会给你一个主题，你只需要对这个主题进行简要总结。")
    .outputKey("summary_result") // [!code highlight]
    .build();

// 创建并行Agent
ParallelAgent parallelAgent = ParallelAgent.builder() // [!code highlight]
    .name("parallel_creative_agent")
    .description("并行执行多个创作任务，包括写散文、写诗和做总结")
    .mergeOutputKey("merged_results") // [!code highlight]
    .subAgents(List.of(proseWriterAgent, poemWriterAgent, summaryAgent)) // [!code highlight]
    .mergeStrategy(new ParallelAgent.DefaultMergeStrategy()) // [!code highlight]
    .build();

// 使用
Optional<OverAllState> result = parallelAgent.invoke("以'西湖'为主题");

if (result.isPresent()) {
    OverAllState state = result.get();

    // 访问各个Agent的输出
    String proseResult = (String) state.value("prose_result").get();
    String poemResult = (String) state.value("poem_result").get();
    String summaryResult = (String) state.value("summary_result").get();

    System.out.println("散文: " + proseResult);
    System.out.println("诗歌: " + poemResult);
    System.out.println("总结: " + summaryResult);

    // 访问合并后的结果
    Object mergedResults = state.value("merged_results").get();
    System.out.println("合并结果: " + mergedResults);
}
```

#### 自定义合并策略

你可以实现自定义的合并策略来控制如何组合多个Agent的输出：

```java
public class CustomMergeStrategy implements ParallelAgent.MergeStrategy {

    @Override
    public Map<String, Object> merge(List<OverAllState> results) {
        Map<String, Object> mergedState = new HashMap<>();

        // 收集所有输出
        List<String> allOutputs = new ArrayList<>();
        for (OverAllState state : results) {
            // 从每个Agent的状态中提取输出
            state.data().forEach((key, value) -> {
                if (key.endsWith("_result")) {
                    allOutputs.add(value.toString());
                }
            });
        }

        // 创建合并后的输出
        String combined = String.join("\n\n---\n\n", allOutputs);
        mergedState.put("merged_results", combined);

        return mergedState;
    }
}

// 使用自定义合并策略
ParallelAgent parallelAgent = ParallelAgent.builder()
    .name("parallel_agent")
    .subAgents(List.of(agent1, agent2, agent3))
    .mergeStrategy(new CustomMergeStrategy()) // [!code highlight]
    .build();
```

### 路由（LlmRoutingAgent）

在**路由模式**中，使用大语言模型（LLM）动态决定将请求路由到哪个子Agent。这种模式非常适合需要智能选择不同专家Agent的场景。

流程：

1. **路由Agent**接收用户输入
2. **LLM**分析输入并决定最合适的子Agent
3. **选中的子Agent**处理请求
4. 结果返回给用户

![Spring AI Alibaba LlmRoutingAgent](/img/agent/multi-agent/routing.png)

#### 实现

```java
import com.alibaba.cloud.ai.graph.agent.flow.agent.LlmRoutingAgent;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;

// 创建专业化的子Agent
ReactAgent writerAgent = ReactAgent.builder()
    .name("writer_agent")
    .model(chatModel)
    .description("擅长创作各类文章，包括散文、诗歌等文学作品")
    .instruction("你是一个知名的作家，擅长写作和创作。请根据用户的提问进行回答。")
    .outputKey("writer_output")
    .build();

ReactAgent reviewerAgent = ReactAgent.builder()
    .name("reviewer_agent")
    .model(chatModel)
    .description("擅长对文章进行评论、修改和润色")
    .instruction("你是一个知名的评论家，擅长对文章进行评论和修改。" +
                 "对于散文类文章，请确保文章中必须包含对于西湖风景的描述。")
    .outputKey("reviewer_output")
    .build();

ReactAgent translatorAgent = ReactAgent.builder()
    .name("translator_agent")
    .model(chatModel)
    .description("擅长将文章翻译成各种语言")
    .instruction("你是一个专业的翻译家，能够准确地将文章翻译成目标语言。")
    .outputKey("translator_output")
    .build();

// 创建路由Agent
LlmRoutingAgent routingAgent = LlmRoutingAgent.builder()
    .name("content_routing_agent")
    .description("根据用户需求智能路由到合适的专家Agent")
    .chatModel(chatModel) // [!code highlight]
    .subAgents(List.of(writerAgent, reviewerAgent, translatorAgent)) // [!code highlight]
    .build();

// 使用 - LLM会自动选择最合适的Agent
Optional<OverAllState> result1 = routingAgent.invoke("帮我写一篇关于春天的散文");
// LLM会路由到 writerAgent

Optional<OverAllState> result2 = routingAgent.invoke("请帮我修改这篇文章：春天来了，花开了。");
// LLM会路由到 reviewerAgent

Optional<OverAllState> result3 = routingAgent.invoke("请将以下内容翻译成英文：春暖花开");
// LLM会路由到 translatorAgent
```

#### 关键特性

1. **智能路由**：LLM根据输入内容和子Agent的描述自动选择最合适的Agent
2. **灵活扩展**：可以轻松添加新的专家Agent，LLM会自动识别并路由
3. **描述驱动**：子Agent的 `description` 非常重要，它告诉LLM何时应该选择该Agent
4. **单次执行**：每次请求只路由到一个Agent执行

#### 优化路由准确性

为了提高路由的准确性，需要注意以下几点：

```java
// 1. 提供清晰明确的Agent描述
ReactAgent codeAgent = ReactAgent.builder()
    .name("code_agent")
    .model(chatModel)
    .description("专门处理编程相关问题，包括代码编写、调试、重构和优化。" + // [!code highlight]
                 "擅长Java、Python、JavaScript等主流编程语言。") // [!code highlight]
    .instruction("你是一个资深的软件工程师...")
    .build();

// 2. 明确Agent的职责边界
ReactAgent businessAgent = ReactAgent.builder()
    .name("business_agent")
    .model(chatModel)
    .description("专门处理商业分析、市场研究和战略规划问题。" + // [!code highlight]
                 "不处理技术实现细节。") // [!code highlight]
    .instruction("你是一个资深的商业分析师...")
    .build();

// 3. 使用不同领域的Agent避免重叠
LlmRoutingAgent routingAgent = LlmRoutingAgent.builder()
    .name("multi_domain_router")
    .chatModel(chatModel)
    .subAgents(List.of(codeAgent, businessAgent, writerAgent))
    .build();
```

### 自定义（Customized）

Spring AI Alibaba 提供了 `FlowAgent` 抽象类，允许你创建自定义的Agent工作流模式。通过继承 `FlowAgent` 并实现特定的图构建逻辑，你可以实现任何复杂的多Agent协作模式。

#### FlowAgent 架构

`FlowAgent` 是所有流程型Agent（如 `SequentialAgent`、`ParallelAgent`、`LlmRoutingAgent`）的基类，它提供了以下核心能力：

```java
public abstract class FlowAgent extends Agent {

    protected List<Agent> subAgents;  // 子Agent列表
    protected CompileConfig compileConfig;  // 编译配置

    // 核心抽象方法：子类必须实现具体的图构建逻辑
    protected abstract StateGraph buildSpecificGraph(
        FlowGraphBuilder.FlowGraphConfig config
    ) throws GraphStateException;

    // 提供给子类使用的工具方法
    public List<Agent> subAgents() { return this.subAgents; }
    public CompileConfig compileConfig() { return compileConfig; }
}
```

#### 实现自定义FlowAgent

下面展示如何创建一个自定义的 `ConditionalAgent`，它根据条件选择不同的Agent分支：

```java
import com.alibaba.cloud.ai.graph.agent.flow.agent.FlowAgent;
import com.alibaba.cloud.ai.graph.agent.flow.builder.FlowAgentBuilder;
import com.alibaba.cloud.ai.graph.agent.flow.builder.FlowGraphBuilder;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.agent.Agent;

import java.util.List;
import java.util.function.Predicate;

/**
 * 条件路由Agent：根据条件函数选择不同的Agent分支
 */
public class ConditionalAgent extends FlowAgent {

    private final Predicate<Map<String, Object>> condition;
    private final Agent trueAgent;
    private final Agent falseAgent;

    protected ConditionalAgent(ConditionalAgentBuilder builder) throws GraphStateException {
        super(builder.name, builder.description, builder.compileConfig,
              List.of(builder.trueAgent, builder.falseAgent));
        this.condition = builder.condition;
        this.trueAgent = builder.trueAgent;
        this.falseAgent = builder.falseAgent;
    }

    @Override
    protected StateGraph buildSpecificGraph(FlowGraphBuilder.FlowGraphConfig config)
            throws GraphStateException {
        // 使用 FlowGraphBuilder 构建自定义图结构
        return FlowGraphBuilder.buildConditionalGraph(
            config,
            this.condition,
            this.trueAgent,
            this.falseAgent
        );
    }

    public static ConditionalAgentBuilder builder() {
        return new ConditionalAgentBuilder();
    }

    /**
     * Builder for ConditionalAgent
     */
    public static class ConditionalAgentBuilder
            extends FlowAgentBuilder<ConditionalAgent, ConditionalAgentBuilder> {

        private Predicate<Map<String, Object>> condition;
        private Agent trueAgent;
        private Agent falseAgent;

        public ConditionalAgentBuilder condition(Predicate<Map<String, Object>> condition) {
            this.condition = condition;
            return this;
        }

        public ConditionalAgentBuilder trueAgent(Agent trueAgent) {
            this.trueAgent = trueAgent;
            return this;
        }

        public ConditionalAgentBuilder falseAgent(Agent falseAgent) {
            this.falseAgent = falseAgent;
            return this;
        }

        @Override
        public ConditionalAgent build() throws GraphStateException {
            if (condition == null || trueAgent == null || falseAgent == null) {
                throw new IllegalStateException(
                    "Condition, trueAgent and falseAgent must be set");
            }
            return new ConditionalAgent(this);
        }

        @Override
        protected ConditionalAgentBuilder self() {
            return this;
        }
    }
}
```

#### 使用自定义Agent

```java
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import java.util.Map;

// 创建两个分支Agent
ReactAgent urgentAgent = ReactAgent.builder()
    .name("urgent_handler")
    .model(chatModel)
    .description("处理紧急请求")
    .instruction("你需要快速响应紧急情况...")
    .outputKey("urgent_result")
    .build();

ReactAgent normalAgent = ReactAgent.builder()
    .name("normal_handler")
    .model(chatModel)
    .description("处理常规请求")
    .instruction("你可以详细分析和处理常规请求...")
    .outputKey("normal_result")
    .build();

// 定义条件：检查输入是否包含"紧急"关键字
Predicate<Map<String, Object>> isUrgent = state -> {
    Object input = state.get("input");
    if (input instanceof String) {
        return ((String) input).contains("紧急") || ((String) input).contains("urgent");
    }
    return false;
};

// 创建条件路由Agent
ConditionalAgent conditionalAgent = ConditionalAgent.builder()
    .name("priority_router")
    .description("根据紧急程度路由请求")
    .condition(isUrgent) // [!code highlight]
    .trueAgent(urgentAgent) // [!code highlight]
    .falseAgent(normalAgent) // [!code highlight]
    .build();

// 使用
Optional<OverAllState> result1 = conditionalAgent.invoke("这是一个紧急问题需要立即处理");
// 会路由到 urgentAgent

Optional<OverAllState> result2 = conditionalAgent.invoke("请帮我分析一下这个问题");
// 会路由到 normalAgent
```

#### 实现复杂的循环Agent

你还可以创建更复杂的自定义Agent，例如带有循环逻辑的 `LoopAgent`：

```java
/**
 * 循环Agent：重复执行直到满足退出条件
 */
public class CustomLoopAgent extends FlowAgent {

    private final Predicate<Map<String, Object>> exitCondition;
    private final int maxIterations;

    protected CustomLoopAgent(CustomLoopAgentBuilder builder)
            throws GraphStateException {
        super(builder.name, builder.description, builder.compileConfig, builder.subAgents);
        this.exitCondition = builder.exitCondition;
        this.maxIterations = builder.maxIterations;
    }

    @Override
    protected StateGraph buildSpecificGraph(FlowGraphBuilder.FlowGraphConfig config)
            throws GraphStateException {
        // 构建带有循环逻辑的图
        return FlowGraphBuilder.buildLoopGraph(
            config,
            this.exitCondition,
            this.maxIterations
        );
    }

    // Builder implementation...
}

// 使用示例
CustomLoopAgent refinementAgent = CustomLoopAgent.builder()
    .name("iterative_refinement")
    .subAgents(List.of(drafterAgent, reviewerAgent))
    .exitCondition(state -> {
        // 当质量分数 >= 8 时退出循环
        Object score = state.get("quality_score");
        return score != null && (int) score >= 8;
    })
    .maxIterations(5) // 最多循环5次
    .build();
```

#### 关键要点

扩展 `FlowAgent` 时需要注意：

1. **实现 buildSpecificGraph**：这是核心方法，定义了Agent的工作流逻辑
2. **使用 FlowGraphBuilder**：提供了构建图的工具方法
3. **继承 FlowAgentBuilder**：保持一致的构建器模式
4. **管理子Agent**：通过 `subAgents` 列表管理所有子Agent
5. **状态传递**：通过 `StateGraph` 控制状态在Agent之间的流动

通过自定义 `FlowAgent`，你可以实现任意复杂的多Agent协作模式，满足各种业务场景需求。

### 混合模式示例

你可以组合不同的模式创建复杂的工作流：

```java
// 1. 创建研究Agent（作为工具）
ReactAgent researchAgent = ReactAgent.builder()
    .name("research_agent")
    .model(chatModel)
    .description("进行背景研究")
    .outputKey("research_result")
    .build();

// 2. 创建多个并行创作Agent
ReactAgent proseAgent = ReactAgent.builder()
    .name("prose_agent")
    .model(chatModel)
    .outputKey("prose")
    .build();

ReactAgent poemAgent = ReactAgent.builder()
    .name("poem_agent")
    .model(chatModel)
    .outputKey("poem")
    .build();

ParallelAgent creativeAgent = ParallelAgent.builder()
    .name("creative_agent")
    .subAgents(List.of(proseAgent, poemAgent))
    .mergeOutputKey("creative_outputs")
    .build();

// 3. 创建评审Agent
ReactAgent reviewAgent = ReactAgent.builder()
    .name("review_agent")
    .model(chatModel)
    .outputKey("final_review")
    .build();

// 4. 组合成顺序工作流
SequentialAgent complexWorkflow = SequentialAgent.builder()
    .name("complex_workflow")
    .description("研究 -> 并行创作 -> 评审")
    .subAgents(List.of(
        researchAgent,      // 步骤1：研究
        creativeAgent,      // 步骤2：并行创作
        reviewAgent         // 步骤3：评审
    ))
    .build();

// 使用
Optional<OverAllState> result = complexWorkflow.invoke("创作关于'人工智能'的内容");
```

## 相关文档

- [Agents](../tutorials/agents.md) - Agent基础概念
- [Tools](../tutorials/tools.md) - 工具的创建和使用
- [Hooks](../tutorials/hooks.md) - Hook机制
- [Memory](./memory.md) - 状态和记忆管理

