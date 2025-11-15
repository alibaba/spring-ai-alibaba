---
title: 人工介入（Human-in-the-Loop）
description: 学习如何使用人工介入Hook为Agent工具调用添加人工监督，支持批准、编辑和拒绝操作，实现安全的AI应用
keywords:
  [
    人工介入,
    HITL,
    Human-in-the-Loop,
    Agent监督,
    工具审批,
    中断恢复,
    Hook,
    人工审批,
  ]
---

# 人工介入（Human-in-the-Loop）

人工介入（HITL）Hook 允许你为 Agent 工具调用添加人工监督。当模型提出需要审查的操作时——例如写入文件或执行 SQL——Hook 可以暂停执行并等待人工决策。

它通过检查每个工具调用并与可配置的策略进行比对来实现。如果需要人工干预，Hook 会发出中断（interrupt）来暂停执行。图的状态会通过 Spring AI Alibaba 的检查点机制保存，因此执行可以安全暂停并在之后恢复。

人工决策决定接下来发生什么：操作可以被原样批准（`approve`）、修改后运行（`edit`）或拒绝并提供反馈（`reject`）。

## 中断决策类型

Hook 定义了三种人工响应中断的内置方式：

| 决策类型     | 描述                               | 使用场景示例                   |
| ------------ | ---------------------------------- | ------------------------------ |
| ✅ `approve` | 操作被原样批准并执行，不做任何更改 | 完全按照写好的内容发送电子邮件 |
| ✏️ `edit`    | 工具调用将被修改后执行             | 在发送电子邮件之前更改收件人   |
| ❌ `reject`  | 工具调用被拒绝，并向对话中添加解释 | 拒绝电子邮件草稿并解释如何重写 |

每个工具可用的决策类型取决于你在 `approvalOn` 中配置的策略。当多个工具调用同时暂停时，每个操作都需要单独的决策。

<!-- <Tip>
  当**编辑**工具参数时，请保守地进行更改。对原始参数的重大修改可能会导致模型重新评估其方法，并可能多次执行工具或采取意外操作。
</Tip> -->

## 配置中断

要使用 HITL，在创建 Agent 时将 Hook 添加到 Agent 的 `hooks` 列表中。

你可以配置哪些工具需要人工审批，以及为每个工具允许哪些决策类型。

```java
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.hip.HumanInTheLoopHook;
import com.alibaba.cloud.ai.graph.agent.hook.hip.ToolConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;

// 配置检查点保存器（人工介入需要检查点来处理中断）
MemorySaver memorySaver = new MemorySaver();

// 创建人工介入Hook
HumanInTheLoopHook humanInTheLoopHook = HumanInTheLoopHook.builder() // [!code highlight]
    .approvalOn("write_file", ToolConfig.builder() // [!code highlight]
        .description("文件写入操作需要审批") // [!code highlight]
        .build()) // [!code highlight]
    .approvalOn("execute_sql", ToolConfig.builder() // [!code highlight]
        .description("SQL执行操作需要审批") // [!code highlight]
        .build()) // [!code highlight]
    .build(); // [!code highlight]

// 创建Agent
ReactAgent agent = ReactAgent.builder()
    .name("approval_agent")
    .model(chatModel)
    .tools(writeFileTool, executeSqlTool, readDataTool)
    .hooks(List.of(humanInTheLoopHook)) // [!code highlight]
    .saver(memorySaver) // [!code highlight]
    .build();
```

:::info
你必须配置检查点保存器来在中断期间持久化图状态。
在生产环境中，使用持久化的检查点保存器（如基于 Redis 或 PostgreSQL 的实现）。对于测试或原型开发，使用 `MemorySaver`。

调用 Agent 时，传递包含**线程 ID**的 `RunnableConfig` 以将执行与会话线程关联。
:::

## 响应中断

当你调用 Agent 时，它会一直运行直到完成或触发中断。当工具调用匹配你在 `approvalOn` 中配置的策略时会触发中断。在这种情况下，调用结果将返回 `InterruptionMetadata`，其中包含需要审查的操作。你可以将这些操作呈现给审查者，并在提供决策后恢复执行。

```java
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;

// 人工介入利用检查点机制。
// 你必须提供线程ID以将执行与会话线程关联，
// 以便可以暂停和恢复对话（人工审查所需）。
String threadId = "user-session-123"; // [!code highlight]
RunnableConfig config = RunnableConfig.builder() // [!code highlight]
    .threadId(threadId) // [!code highlight]
    .build(); // [!code highlight]

// 运行图直到触发中断
Optional<NodeOutput> result = agent.invokeAndGetOutput( // [!code highlight]
    "删除数据库中的旧记录",
    config
);

// 检查是否返回了中断
if (result.isPresent() && result.get() instanceof InterruptionMetadata) { // [!code highlight]
    InterruptionMetadata interruptionMetadata = (InterruptionMetadata) result.get(); // [!code highlight]

    // 中断包含需要审查的工具反馈
    List<InterruptionMetadata.ToolFeedback> toolFeedbacks = // [!code highlight]
        interruptionMetadata.toolFeedbacks(); // [!code highlight]

    for (InterruptionMetadata.ToolFeedback feedback : toolFeedbacks) {
        System.out.println("工具: " + feedback.getName());
        System.out.println("参数: " + feedback.getArguments());
        System.out.println("描述: " + feedback.getDescription());
    }

    // 示例输出:
    // 工具: execute_sql
    // 参数: {"query": "DELETE FROM records WHERE created_at < NOW() - INTERVAL '30 days';"}
    // 描述: SQL执行操作需要审批
}
```

### 决策类型

<!-- ```
<Tabs>
  <Tab title="✅ approve - 批准">
    使用 `approve` 批准工具调用原样执行，不做任何更改。

```
```java
    // 构建批准反馈
    InterruptionMetadata.Builder feedbackBuilder = InterruptionMetadata.builder()
        .nodeId(interruptionMetadata.node())
        .state(interruptionMetadata.state());

    // 对每个工具调用设置批准决策
    interruptionMetadata.toolFeedbacks().forEach(toolFeedback -> {
        InterruptionMetadata.ToolFeedback approvedFeedback =
            InterruptionMetadata.ToolFeedback.builder(toolFeedback) // [!code highlight]
                .result(InterruptionMetadata.ToolFeedback.FeedbackResult.APPROVED) // [!code highlight]
                .build(); // [!code highlight]
        feedbackBuilder.addToolFeedback(approvedFeedback);
    });

    InterruptionMetadata approvalMetadata = feedbackBuilder.build();

    // 使用批准决策恢复执行
    RunnableConfig resumeConfig = RunnableConfig.builder()
        .threadId(threadId) // 相同的线程ID以恢复暂停的对话
        .addMetadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY, approvalMetadata) // [!code highlight]
        .build();

    // 第二次调用以恢复执行
    Optional<NodeOutput> finalResult = agent.invokeAndGetOutput("", resumeConfig);
    ```
  </Tab>

  <Tab title="✏️ edit - 编辑">
    使用 `edit` 在执行前修改工具调用。
    提供编辑后的操作，包括新的工具参数。

    ```java
    // 构建编辑反馈
    InterruptionMetadata.Builder feedbackBuilder = InterruptionMetadata.builder()
        .nodeId(interruptionMetadata.node())
        .state(interruptionMetadata.state());

    interruptionMetadata.toolFeedbacks().forEach(toolFeedback -> {
        // 修改工具参数
        String editedArguments = toolFeedback.getArguments()
            .replace("DELETE FROM records", "DELETE FROM old_records"); // [!code highlight]

        InterruptionMetadata.ToolFeedback editedFeedback =
            InterruptionMetadata.ToolFeedback.builder(toolFeedback)
                .arguments(editedArguments) // [!code highlight]
                .result(InterruptionMetadata.ToolFeedback.FeedbackResult.EDITED) // [!code highlight]
                .build();
        feedbackBuilder.addToolFeedback(editedFeedback);
    });

    InterruptionMetadata editMetadata = feedbackBuilder.build();

    // 使用编辑决策恢复执行
    RunnableConfig resumeConfig = RunnableConfig.builder()
        .threadId(threadId)
        .addMetadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY, editMetadata)
        .build();

    Optional<NodeOutput> finalResult = agent.invokeAndGetOutput("", resumeConfig);
    ```

    <Tip>
      当**编辑**工具参数时，请保守地进行更改。对原始参数的重大修改可能会导致模型重新评估其方法，并可能多次执行工具或采取意外操作。
    </Tip>
  </Tab>

  <Tab title="❌ reject - 拒绝">
    使用 `reject` 拒绝工具调用并提供反馈而不是执行。

    ```java
    // 构建拒绝反馈
    InterruptionMetadata.Builder feedbackBuilder = InterruptionMetadata.builder()
        .nodeId(interruptionMetadata.node())
        .state(interruptionMetadata.state());

    interruptionMetadata.toolFeedbacks().forEach(toolFeedback -> {
        InterruptionMetadata.ToolFeedback rejectedFeedback =
            InterruptionMetadata.ToolFeedback.builder(toolFeedback)
                .result(InterruptionMetadata.ToolFeedback.FeedbackResult.REJECTED) // [!code highlight]
                .description("不允许删除操作，请使用归档功能代替。") // [!code highlight]
                .build();
        feedbackBuilder.addToolFeedback(rejectedFeedback);
    });

    InterruptionMetadata rejectMetadata = feedbackBuilder.build();

    // 使用拒绝决策恢复执行
    RunnableConfig resumeConfig = RunnableConfig.builder()
        .threadId(threadId)
        .addMetadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY, rejectMetadata)
        .build();

    Optional<NodeOutput> finalResult = agent.invokeAndGetOutput("", resumeConfig);
    ```

    `description` 消息将被添加到对话中作为反馈，帮助Agent理解为什么操作被拒绝以及应该做什么。

    ---

    ### 多个决策

    当多个操作需要审查时，为每个操作提供决策：

    ```java
    InterruptionMetadata.Builder feedbackBuilder = InterruptionMetadata.builder()
        .nodeId(interruptionMetadata.node())
        .state(interruptionMetadata.state());

    List<InterruptionMetadata.ToolFeedback> feedbacks = interruptionMetadata.toolFeedbacks();

    // 第一个工具：批准
    feedbackBuilder.addToolFeedback(
        InterruptionMetadata.ToolFeedback.builder(feedbacks.get(0))
            .result(InterruptionMetadata.ToolFeedback.FeedbackResult.APPROVED)
            .build()
    );

    // 第二个工具：编辑
    feedbackBuilder.addToolFeedback(
        InterruptionMetadata.ToolFeedback.builder(feedbacks.get(1))
            .arguments("{\"param\": \"new_value\"}")
            .result(InterruptionMetadata.ToolFeedback.FeedbackResult.EDITED)
            .build()
    );

    // 第三个工具：拒绝
    feedbackBuilder.addToolFeedback(
        InterruptionMetadata.ToolFeedback.builder(feedbacks.get(2))
            .result(InterruptionMetadata.ToolFeedback.FeedbackResult.REJECTED)
            .description("不允许此操作")
            .build()
    );
    ```
  </Tab>
</Tabs>
``` -->

## 执行生命周期

Hook 定义了一个在模型生成响应后但在执行任何工具调用之前运行的 `afterModel` 钩子：

1. Agent 调用模型生成响应。
2. Hook 检查响应中的工具调用。
3. 如果任何调用需要人工输入，Hook 会构建包含工具反馈信息的 `InterruptionMetadata` 并触发中断。
4. Agent 等待人工决策。
5. 基于 `InterruptionMetadata` 中的决策，Hook 执行批准或编辑的调用，为拒绝的调用合成工具响应消息，并恢复执行。

## 完整示例

```java
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.hip.HumanInTheLoopHook;
import com.alibaba.cloud.ai.graph.agent.hook.hip.ToolConfig;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;

public class HumanInTheLoopExample {

    public static void main(String[] args) throws Exception {
        // 1. 配置检查点
        MemorySaver memorySaver = new MemorySaver();

        // 2. 创建人工介入Hook
        HumanInTheLoopHook humanInTheLoopHook = HumanInTheLoopHook.builder()
            .approvalOn("poem", ToolConfig.builder()
                .description("请确认诗歌创作操作")
                .build())
            .build();

        // 3. 创建Agent
        ReactAgent agent = ReactAgent.builder()
            .name("poet_agent")
            .model(chatModel)
            .tools(List.of(poetToolCallback))
            .hooks(List.of(humanInTheLoopHook))
            .saver(memorySaver)
            .build();

        String threadId = "user-session-001";
        RunnableConfig config = RunnableConfig.builder()
            .threadId(threadId)
            .build();

        // 4. 第一次调用 - 触发中断
        System.out.println("=== 第一次调用：期望中断 ===");
        Optional<NodeOutput> result = agent.invokeAndGetOutput(
            "帮我写一首100字左右的诗",
            config
        );

        // 5. 检查中断并处理
        if (result.isPresent() && result.get() instanceof InterruptionMetadata) {
            InterruptionMetadata interruptionMetadata = (InterruptionMetadata) result.get();

            System.out.println("检测到中断，需要人工审批");
            List<InterruptionMetadata.ToolFeedback> toolFeedbacks =
                interruptionMetadata.toolFeedbacks();

            for (InterruptionMetadata.ToolFeedback feedback : toolFeedbacks) {
                System.out.println("工具: " + feedback.getName());
                System.out.println("参数: " + feedback.getArguments());
                System.out.println("描述: " + feedback.getDescription());
            }

            // 6. 模拟人工决策（这里选择批准）
            InterruptionMetadata.Builder feedbackBuilder = InterruptionMetadata.builder()
                .nodeId(interruptionMetadata.node())
                .state(interruptionMetadata.state());

            toolFeedbacks.forEach(toolFeedback -> {
                InterruptionMetadata.ToolFeedback approvedFeedback =
                    InterruptionMetadata.ToolFeedback.builder(toolFeedback)
                        .result(InterruptionMetadata.ToolFeedback.FeedbackResult.APPROVED)
                        .build();
                feedbackBuilder.addToolFeedback(approvedFeedback);
            });

            InterruptionMetadata approvalMetadata = feedbackBuilder.build();

            // 7. 第二次调用 - 使用人工反馈恢复执行
            System.out.println("\n=== 第二次调用：使用批准决策恢复 ===");
            RunnableConfig resumeConfig = RunnableConfig.builder()
                .threadId(threadId)
                .addMetadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY, approvalMetadata)
                .build();

            Optional<NodeOutput> finalResult = agent.invokeAndGetOutput("", resumeConfig);

            if (finalResult.isPresent()) {
                System.out.println("执行完成");
                System.out.println("最终结果: " + finalResult.get());
            }
        }
    }
}
```

## 实用工具方法

为了简化人工介入的处理，你可以创建实用方法：

```java
public class HITLHelper {

    /**
     * 批准所有工具调用
     */
    public static InterruptionMetadata approveAll(InterruptionMetadata interruptionMetadata) {
        InterruptionMetadata.Builder builder = InterruptionMetadata.builder()
            .nodeId(interruptionMetadata.node())
            .state(interruptionMetadata.state());

        interruptionMetadata.toolFeedbacks().forEach(toolFeedback -> {
            builder.addToolFeedback(
                InterruptionMetadata.ToolFeedback.builder(toolFeedback)
                    .result(InterruptionMetadata.ToolFeedback.FeedbackResult.APPROVED)
                    .build()
            );
        });

        return builder.build();
    }

    /**
     * 拒绝所有工具调用
     */
    public static InterruptionMetadata rejectAll(
            InterruptionMetadata interruptionMetadata,
            String reason) {
        InterruptionMetadata.Builder builder = InterruptionMetadata.builder()
            .nodeId(interruptionMetadata.node())
            .state(interruptionMetadata.state());

        interruptionMetadata.toolFeedbacks().forEach(toolFeedback -> {
            builder.addToolFeedback(
                InterruptionMetadata.ToolFeedback.builder(toolFeedback)
                    .result(InterruptionMetadata.ToolFeedback.FeedbackResult.REJECTED)
                    .description(reason)
                    .build()
            );
        });

        return builder.build();
    }

    /**
     * 编辑特定工具的参数
     */
    public static InterruptionMetadata editTool(
            InterruptionMetadata interruptionMetadata,
            String toolName,
            String newArguments) {
        InterruptionMetadata.Builder builder = InterruptionMetadata.builder()
            .nodeId(interruptionMetadata.node())
            .state(interruptionMetadata.state());

        interruptionMetadata.toolFeedbacks().forEach(toolFeedback -> {
            if (toolFeedback.getName().equals(toolName)) {
                builder.addToolFeedback(
                    InterruptionMetadata.ToolFeedback.builder(toolFeedback)
                        .arguments(newArguments)
                        .result(InterruptionMetadata.ToolFeedback.FeedbackResult.EDITED)
                        .build()
                );
            } else {
                builder.addToolFeedback(
                    InterruptionMetadata.ToolFeedback.builder(toolFeedback)
                        .result(InterruptionMetadata.ToolFeedback.FeedbackResult.APPROVED)
                        .build()
                );
            }
        });

        return builder.build();
    }
}

// 使用示例
InterruptionMetadata approvalMetadata = HITLHelper.approveAll(interruptionMetadata);
InterruptionMetadata rejectMetadata = HITLHelper.rejectAll(interruptionMetadata, "操作不安全");
InterruptionMetadata editMetadata = HITLHelper.editTool(
    interruptionMetadata,
    "execute_sql",
    "{\"query\": \"SELECT * FROM records LIMIT 10\"}"
);
```

## 最佳实践

1. **始终使用检查点**: 人工介入需要检查点机制来保存和恢复状态
2. **提供清晰的描述**: 在 `ToolConfig` 中提供清晰的描述，帮助审查者理解操作
3. **保守编辑**: 编辑工具参数时，尽量保持最小更改
4. **处理所有工具反馈**: 确保为每个需要审查的工具调用提供决策
5. **使用相同的 threadId**: 恢复执行时必须使用相同的线程 ID
6. **考虑超时**: 实现超时机制以处理长时间未响应的人工审批

## 与 Interceptor 的区别

在 Spring AI Alibaba 中，Hook 和 Interceptor 都可以用于干预 Agent 执行：

| 特性         | Hook                                                 | Interceptor                   |
| ------------ | ---------------------------------------------------- | ----------------------------- |
| **执行位置** | Agent 级别（before/after agent, before/after model） | 模型或工具调用级别            |
| **中断能力** | 支持中断和恢复（如 HumanInTheLoopHook）              | 不支持中断，仅拦截和修改      |
| **使用场景** | 人工审批、Agent 间协调                               | 日志记录、重试、降级          |
| **配置方式** | `.hooks(List.of(...))`                               | `.interceptors(List.of(...))` |

## 相关文档

- [Hooks](../tutorials/hooks.md) - 了解 Hook 机制
- [Interceptors](../tutorials/hooks.md) - 了解 Interceptor 机制
- [Memory](./memory.md) - 检查点和持久化
- [Agents](../tutorials/agents.md) - Agent 基础概念
