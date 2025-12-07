---
title: Hooks 和 Interceptors
description: 学习使用Hooks和Interceptors在每个步骤精细控制和自定义Agent执行，实现监控、修改、控制和强制执行
keywords: [Hooks, Interceptors, Agent控制, 监控, 重试, 回退, 日志, 护栏, PII检测]
---

# Hooks 和 Interceptors

> 让开发者在每个步骤控制和自定义 Agent 执行

Hooks 和 Interceptors 提供了一种更精细控制 Agent 内部行为的方式。

核心 Agent 循环涉及调用模型、让其选择要执行的工具，直到不需要调用工具时完成。

<img src="/img/agent/agents/reactagent.png" alt="reactagent" width="360" />

Hooks 和 Interceptors 在这些步骤的前后暴露了钩子点，允许你：

<img src="/img/agent/hooks/reactagent-hook.png" alt="reactagent" width="450" />

- **监控**: 通过日志、分析和调试跟踪 Agent 行为
- **修改**: 转换提示、工具选择和输出格式
- **控制**: 添加重试、回退和提前终止逻辑
- **强制执行**: 应用速率限制、护栏和 PII 检测

通过将它们传递给 `ReactAgent.builder()` 来添加 Hooks 和 Interceptors：

<Code
  language="java"
  title="添加 Hooks 和 Interceptors 到 ReactAgent" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/HooksExample.java"
>
{`import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.*;
import com.alibaba.cloud.ai.graph.agent.interceptor.*;

ReactAgent agent = ReactAgent.builder()
    .name("my_agent")
    .model(chatModel)
    .tools(tools)
    .hooks(loggingHook, messageTrimmingHook)
    .interceptors(guardrailInterceptor, retryInterceptor)
    .build();`}
</Code>

## Hooks 和 Interceptors 能做什么？

* 监控。使用日志、分析和调试跟踪 Agent 行为。

* 修改。转换提示、工具选择和输出格式。

* 控制。添加重试、回退和提前终止逻辑。

* 强制执行。应用速率限制、护栏和 PII 检测。

## 内置实现

Spring AI Alibaba 为常见用例提供了预构建的 Hooks 和 Interceptors 实现：

### 消息压缩（Summarization）

当接近 token 限制时自动压缩对话历史。

**适用场景**：
* 超出上下文窗口的长期对话
* 具有大量历史记录的多轮对话
* 需要保留完整对话上下文的应用程序

<Code
  language="java"
  title="SummarizationHook 消息压缩示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/HooksExample.java"
>
{`import com.alibaba.cloud.ai.graph.agent.hook.summarization.SummarizationHook;

// 创建消息压缩 Hook
SummarizationHook summarizationHook = SummarizationHook.builder()
    .model(chatModel)
    .maxTokensBeforeSummary(4000)
    .messagesToKeep(20)
    .build();

// 使用
ReactAgent agent = ReactAgent.builder()
    .name("my_agent")
    .model(chatModel)
    .hooks(summarizationHook)
    .build();`}
</Code>

**配置选项**：
- `model`: 用于生成摘要的 ChatModel
- `maxTokensBeforeSummary`: 触发摘要之前的最大 token 数
- `messagesToKeep`: 摘要后保留的最新消息数

### Human-in-the-Loop（人机协同）

暂停 Agent 执行以获得人工批准、编辑或拒绝工具调用。

**适用场景**：
* 需要人工批准的高风险操作（数据库写入、金融交易）
* 人工监督是强制性的合规工作流程
* 长期对话，使用人工反馈引导 Agent

<Code
  language="java"
  title="HumanInTheLoopHook 人机协同示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/HooksExample.java"
>
{`import com.alibaba.cloud.ai.graph.agent.hook.hip.HumanInTheLoopHook;
import com.alibaba.cloud.ai.graph.agent.hook.hip.ToolConfig;

// 创建 Human-in-the-Loop Hook
HumanInTheLoopHook humanReviewHook = HumanInTheLoopHook.builder()
    .approvalOn("sendEmailTool", ToolConfig.builder().description("Please confirm sending the email.").build())
    .approvalOn("deleteDataTool")
    .build();

ReactAgent agent = ReactAgent.builder()
    .name("supervised_agent")
    .model(chatModel)
    .tools(sendEmailTool, deleteDataTool)
    .hooks(humanReviewHook)
    .saver(new RedisSaver())
    .build();`}
</Code>

**重要提示**：Human-in-the-loop Hook 需要 checkpointer 来维护跨中断的状态。示例中我们演示用了 `RedisSaver`。

### 模型调用限制（Model Call Limit）

限制模型调用次数以防止无限循环或过度成本。

**适用场景**：
* 防止失控的 Agent 进行太多 API 调用
* 在生产部署中强制执行成本控制
* 在特定调用预算内测试 Agent 行为

<Code
  language="java"
  title="ModelCallLimitHook 模型调用限制示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/HooksExample.java"
>
{`ReactAgent agent = ReactAgent.builder()
    .name("my_agent")
    .model(chatModel)
    .hooks(ModelCallLimitHook.builder().runLimit(5).build())  // 限制模型调用次数为5次
    .saver(new MemorySaver())
    .build();`}
</Code>

### PII 检测（Personally Identifiable Information）

检测和处理对话中的个人身份信息。

**适用场景**：
* 具有合规要求的医疗保健和金融应用
* 需要清理日志的客户服务 Agent
* 任何处理敏感用户数据的应用程序

<Code
  language="java"
  title="PIIDetectionHook PII 检测示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/HooksExample.java"
>
{`import com.alibaba.cloud.ai.graph.agent.hook.pii.PIIDetectionHook;
import com.alibaba.cloud.ai.graph.agent.hook.pii.PIIType;
import com.alibaba.cloud.ai.graph.agent.hook.pii.RedactionStrategy;

PIIDetectionHook pii = PIIDetectionHook.builder()
    .piiType(PIIType.EMAIL)
    .strategy(RedactionStrategy.REDACT)
    .applyToInput(true)
    .build();

// 使用
ReactAgent agent = ReactAgent.builder()
    .name("secure_agent")
    .model(chatModel)
    .hooks(pii)
    .build();`}
</Code>

### 工具重试（Tool Retry）

自动重试失败的工具调用，具有可配置的指数退避。

**适用场景**：
* 处理外部 API 调用中的瞬态故障
* 提高依赖网络的工具的可靠性
* 构建优雅处理临时错误的弹性 Agent

<Code
  language="java"
  title="ToolRetryInterceptor 工具重试示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/HooksExample.java"
>
{`import com.alibaba.cloud.ai.graph.agent.interceptor.toolretry.ToolRetryInterceptor;

// 使用
ReactAgent agent = ReactAgent.builder()
    .name("resilient_agent")
    .model(chatModel)
    .tools(searchTool, databaseTool)
    .interceptors(ToolRetryInterceptor.builder()
        .maxRetries(2)
        .onFailure(ToolRetryInterceptor.OnFailureBehavior.RETURN_MESSAGE)
        .build())
    .build();`}
</Code>


### Planning（规划）

在执行工具之前强制执行一个规划步骤，以概述 Agent 将要采取的步骤。

**适用场景**：
*   需要执行复杂、多步骤任务的 Agent
*   通过在执行前显示 Agent 的计划来提高透明度
*   通过检查建议的计划来调试错误

<Code
  language="java"
  title="TodoListInterceptor 规划示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/HooksExample.java"
>
{`import com.alibaba.cloud.ai.graph.agent.interceptor.todolist.TodoListInterceptor;

// 使用
ReactAgent agent = ReactAgent.builder()
    .name("planning_agent")
    .model(chatModel)
    .tools(myTool)
    .interceptors(TodoListInterceptor.builder().build())
    .build();`}
</Code>

### LLM Tool Selector（LLM 工具选择器）

使用一个 LLM 来决定在多个可用工具之间选择哪个工具。

**适用场景**：
*   当多个工具可以实现相似目标时
*   需要根据细微的上下文差异进行工具选择
*   动态选择最适合特定输入的工具

<Code
  language="java"
  title="ToolSelectionInterceptor LLM 工具选择器示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/HooksExample.java"
>
{`import com.alibaba.cloud.ai.graph.agent.interceptor.toolselection.ToolSelectionInterceptor;

// 使用
ReactAgent agent = ReactAgent.builder()
    .name("smart_selector_agent")
    .model(chatModel)
    .tools(tool1, tool2)
    .interceptors(ToolSelectionInterceptor.builder().build())
    .build();`}
</Code>

### LLM Tool Emulator（LLM 工具模拟器）

在没有实际执行工具的情况下，使用 LLM 模拟工具的输出。

**适用场景**：
*   在演示或测试期间模拟 API
*   在开发过程中为工具提供占位符行为
*   在不产生实际成本或副作用的情况下测试 Agent 逻辑

<Code
  language="java"
  title="ToolEmulatorInterceptor LLM 工具模拟器示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/HooksExample.java"
>
{`import com.alibaba.cloud.ai.graph.agent.interceptor.toolemulator.ToolEmulatorInterceptor;

// 使用
ReactAgent agent = ReactAgent.builder()
    .name("emulator_agent")
    .model(chatModel)
    .tools(simulatedTool)
    .interceptors(ToolEmulatorInterceptor.builder().model(chatModel).build())
    .build();`}
</Code>

### Context Editing（上下文编辑）

在将上下文发送给 LLM 之前对其进行修改，以注入、删除或修改信息。

**适用场景**：
*   向 LLM 提供额外的上下文或指令
*   从对话历史中删除不相关或冗余的信息
*   动态修改上下文以引导 Agent 的行为

<Code
  language="java"
  title="ContextEditingInterceptor 上下文编辑示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/HooksExample.java"
>
{`import com.alibaba.cloud.ai.graph.agent.interceptor.contextediting.ContextEditingInterceptor;

// 使用
ReactAgent agent = ReactAgent.builder()
    .name("context_aware_agent")
    .model(chatModel)
    .interceptors(ContextEditingInterceptor.builder().trigger(120000).clearAtLeast(60000).build())
    .build();`}
</Code>

## 自定义 Hooks 和 Interceptors

通过实现在 Agent 执行流程中特定点运行的钩子来构建自定义功能。

你可以通过以下方式创建自定义功能：

1. **MessagesModelHook** - 在模型调用前后执行，专注于消息操作（推荐）
2. **ModelHook** - 在模型调用前后执行，可访问完整状态
3. **AgentHook** - 在 Agent 开始和结束时执行
4. **ModelInterceptor** - 拦截和修改模型请求/响应
5. **ToolInterceptor** - 拦截和修改工具调用

### MessagesModelHook

`MessagesModelHook` 是一个专门用于操作消息列表的 Hook，**使用更简单，更推荐**。它直接接收和返回消息列表，无需处理复杂的 `OverAllState`。

**适用场景**：
- 消息修剪、过滤或转换
- 添加系统提示或上下文消息
- 消息压缩和摘要
- 简单的消息操作需求

<Code
  language="java"
  title="MessageTrimmingHook 消息修剪示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/HooksExample.java"
>
{`import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.messages.AgentCommand;
import com.alibaba.cloud.ai.graph.agent.hook.messages.UpdatePolicy;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import org.springframework.ai.chat.messages.Message;

@HookPositions({HookPosition.BEFORE_MODEL})
public class MessageTrimmingHook extends MessagesModelHook {
    private static final int MAX_MESSAGES = 10;

    @Override
    public String getName() {
        return "message_trimming";
    }

    @Override
    public AgentCommand beforeModel(List<Message> previousMessages, RunnableConfig config) {
        // 如果消息数量超过限制，只保留最后 MAX_MESSAGES 条消息
        if (previousMessages.size() > MAX_MESSAGES) {
            List<Message> trimmedMessages = previousMessages.subList(
                previousMessages.size() - MAX_MESSAGES,
                previousMessages.size()
            );
            // 使用 REPLACE 策略替换所有消息
            return new AgentCommand(trimmedMessages, UpdatePolicy.REPLACE);
        }
        // 如果消息数量未超过限制，返回原始消息（不进行修改）
        return new AgentCommand(previousMessages);
    }
}`}
</Code>

**AgentCommand 和 UpdatePolicy**：

`MessagesModelHook` 通过 `AgentCommand` 返回操作结果：

- **REPLACE 策略**：替换所有现有消息
- **APPEND 策略**：将新消息追加到现有消息列表

<Code
  language="java"
  title="使用不同策略的 MessagesModelHook 示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/HooksExample.java"
>
{`import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.messages.AgentCommand;
import com.alibaba.cloud.ai.graph.agent.hook.messages.UpdatePolicy;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import java.util.ArrayList;
import java.util.List;

@HookPositions({HookPosition.BEFORE_MODEL})
public class ContextEnhancementHook extends MessagesModelHook {
    
    @Override
    public String getName() {
        return "context_enhancement";
    }

    @Override
    public AgentCommand beforeModel(List<Message> previousMessages, RunnableConfig config) {
        // 示例 1: 使用 REPLACE 策略替换所有消息
        List<Message> newMessages = new ArrayList<>();
        newMessages.add(new SystemMessage("你是一个专业的助手"));
        newMessages.addAll(previousMessages);
        return new AgentCommand(newMessages, UpdatePolicy.REPLACE);
        
        // 示例 2: 使用 APPEND 策略追加消息
        // List<Message> additionalMessages = List.of(
        //     new UserMessage("请记住：保持友好和专业")
        // );
        // return new AgentCommand(additionalMessages, UpdatePolicy.APPEND);
    }
}`}
</Code>

**支持跳转控制**：

`MessagesModelHook` 也支持通过 `JumpTo` 实现提前退出：

<Code
  language="java"
  title="MessagesModelHook 跳转控制示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/HooksExample.java"
>
{`import com.alibaba.cloud.ai.graph.agent.hook.JumpTo;
import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.messages.AgentCommand;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import org.springframework.ai.chat.messages.Message;
import java.util.List;

@HookPositions({HookPosition.BEFORE_MODEL})
public class EarlyExitHook extends MessagesModelHook {
    
    @Override
    public String getName() {
        return "early_exit";
    }

    @Override
    public List<JumpTo> canJumpTo() {
        return List.of(JumpTo.end);
    }

    @Override
    public AgentCommand beforeModel(List<Message> previousMessages, RunnableConfig config) {
        // 检查某些条件，如果满足则提前退出
        if (shouldExit(previousMessages)) {
            return new AgentCommand(JumpTo.end, previousMessages);
        }
        return new AgentCommand(previousMessages);
    }
    
    private boolean shouldExit(List<Message> messages) {
        // 实现你的退出逻辑
        return false;
    }
}`}
</Code>

### ModelHook

在模型调用前后执行自定义逻辑：

<Code
  language="java"
  title="CustomModelHook 自定义 ModelHook 示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/HooksExample.java"
>
{`import com.alibaba.cloud.ai.graph.agent.hook.ModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import java.util.concurrent.CompletableFuture;

@HookPositions({HookPosition.BEFORE_MODEL, HookPosition.AFTER_MODEL})
public class CustomModelHook extends ModelHook {

    @Override
    public String getName() {
        return "custom_model_hook";
    }

    @Override
    public CompletableFuture<Map<String, Object>> beforeModel(OverAllState state, RunnableConfig config) {
        // 在模型调用前执行
        System.out.println("准备调用模型...");

        // 可以修改状态
        // 例如：添加额外的上下文
        return CompletableFuture.completedFuture(Map.of("extra_context", "某些额外信息"));
    }

    @Override
    public CompletableFuture<Map<String, Object>> afterModel(OverAllState state, RunnableConfig config) {
        // 在模型调用后执行
        System.out.println("模型调用完成");

        // 可以记录响应信息
        return CompletableFuture.completedFuture(Map.of());
    }
}`}
</Code>

### MessagesModelHook vs ModelHook：如何选择？

`MessagesModelHook` 和 `ModelHook` 都可以在模型调用前后执行自定义逻辑，但它们有不同的设计目标和适用场景。

#### 核心区别

| 特性 | MessagesModelHook | ModelHook |
|------|------------------|-----------|
| **易用性** | ⭐⭐⭐⭐⭐ 更简单，直接操作消息列表 | ⭐⭐⭐ 需要理解 `OverAllState` |
| **灵活性** | ⭐⭐⭐ 专注于消息操作 | ⭐⭐⭐⭐⭐ 可访问和修改完整状态 |
| **推荐场景** | 消息修剪、过滤、添加系统提示 | 需要访问全局状态、自定义状态管理 |
| **API 复杂度** | 简单：`AgentCommand` 返回消息列表 | 复杂：返回 `Map<String, Object>` 更新状态 |

#### 使用建议

**选择 MessagesModelHook，如果你需要：**
- ✅ 简单的消息操作（修剪、过滤、转换）
- ✅ 添加或修改系统提示
- ✅ 消息压缩和摘要
- ✅ 快速实现消息相关的 Hook

**选择 ModelHook，如果你需要：**
- ✅ 访问和修改 `OverAllState` 中的其他数据
- ✅ 在状态中存储自定义信息（如计数器、缓存等）
- ✅ 基于全局状态做复杂决策
- ✅ 需要查看 Agent 执行过程中的完整上下文

#### 对比示例

**场景：消息修剪**

使用 `MessagesModelHook`（推荐）：

<Code
  language="java"
  title="使用 MessagesModelHook 实现消息修剪" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/HooksExample.java"
>
{`import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.messages.AgentCommand;
import com.alibaba.cloud.ai.graph.agent.hook.messages.UpdatePolicy;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import org.springframework.ai.chat.messages.Message;
import java.util.List;

@HookPositions({HookPosition.BEFORE_MODEL})
public class SimpleMessageTrimmingHook extends MessagesModelHook {
    private static final int MAX_MESSAGES = 10;

    @Override
    public String getName() {
        return "simple_message_trimming";
    }

    @Override
    public AgentCommand beforeModel(List<Message> previousMessages, RunnableConfig config) {
        if (previousMessages.size() > MAX_MESSAGES) {
            List<Message> trimmed = previousMessages.subList(
                previousMessages.size() - MAX_MESSAGES,
                previousMessages.size()
            );
            return new AgentCommand(trimmed, UpdatePolicy.REPLACE);
        }
        return new AgentCommand(previousMessages);
    }
}`}
</Code>

使用 `ModelHook`（更复杂但更灵活）：

<Code
  language="java"
  title="使用 ModelHook 实现消息修剪（可访问状态）" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/HooksExample.java"
>
{`import com.alibaba.cloud.ai.graph.agent.hook.ModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.state.ReplaceAllWith;
import org.springframework.ai.chat.messages.Message;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@HookPositions({HookPosition.BEFORE_MODEL})
public class AdvancedMessageTrimmingHook extends ModelHook {
    private static final int MAX_MESSAGES = 10;
    private static final String TRIM_COUNT_KEY = "trim_count";

    @Override
    public String getName() {
        return "advanced_message_trimming";
    }

    @Override
    public CompletableFuture<Map<String, Object>> beforeModel(OverAllState state, RunnableConfig config) {
        // 可以访问完整状态
        Optional<Object> messagesOpt = state.value("messages");
        if (messagesOpt.isEmpty()) {
            return CompletableFuture.completedFuture(Map.of());
        }

        List<Message> messages = (List<Message>) messagesOpt.get();
        
        // 可以访问和更新自定义状态
        int trimCount = (Integer) state.value(TRIM_COUNT_KEY).orElse(0);
        
        if (messages.size() > MAX_MESSAGES) {
            List<Message> trimmed = messages.subList(
                messages.size() - MAX_MESSAGES,
                messages.size()
            );
            
            // 可以同时更新消息和自定义状态
            return CompletableFuture.completedFuture(Map.of(
                "messages", ReplaceAllWith.of(trimmed),
                TRIM_COUNT_KEY, trimCount + 1  // 记录修剪次数
            ));
        }
        
        return CompletableFuture.completedFuture(Map.of());
    }
}`}
</Code>

**总结**：

- **优先使用 `MessagesModelHook`**：对于大多数消息操作场景，它提供了更简洁的 API，代码更易读易维护。
- **使用 `ModelHook`**：当你需要访问全局状态、存储自定义数据或进行复杂的状态管理时。

### AgentHook

在 Agent 整体执行的开始和结束时执行：

<Code
  language="java"
  title="CustomAgentHook 自定义 AgentHook 示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/HooksExample.java"
>
{`import com.alibaba.cloud.ai.graph.agent.hook.AgentHook;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import java.util.concurrent.CompletableFuture;

@HookPositions({HookPosition.BEFORE_AGENT, HookPosition.AFTER_AGENT})
public class CustomAgentHook extends AgentHook {

    @Override
    public String getName() {
        return "custom_agent_hook";
    }

    @Override
    public CompletableFuture<Map<String, Object>> beforeAgent(OverAllState state, RunnableConfig config) {
        System.out.println("Agent 开始执行");
        // 可以初始化资源、记录开始时间等
        return CompletableFuture.completedFuture(Map.of("start_time", System.currentTimeMillis()));
    }

    @Override
    public CompletableFuture<Map<String, Object>> afterAgent(OverAllState state, RunnableConfig config) {
        System.out.println("Agent 执行完成");
        // 可以清理资源、计算执行时间等
        Optional<Object> startTime = state.value("start_time");
        if (startTime.isPresent()) {
            long duration = System.currentTimeMillis() - (Long) startTime.get();
            System.out.println("执行耗时: " + duration + "ms");
        }
        return CompletableFuture.completedFuture(Map.of());
    }
}`}
</Code>

### ModelInterceptor

拦截和修改模型请求和响应：

<Code
  language="java"
  title="LoggingInterceptor 自定义 ModelInterceptor 示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/HooksExample.java"
>
{`import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;

public class LoggingInterceptor extends ModelInterceptor {

    @Override
    public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
        // 请求前记录
        System.out.println("发送请求到模型: " + request.getMessages().size() + " 条消息");

        long startTime = System.currentTimeMillis();

        // 执行实际调用
        ModelResponse response = handler.call(request);

        // 响应后记录
        long duration = System.currentTimeMillis() - startTime;
        System.out.println("模型响应耗时: " + duration + "ms");

        return response;
    }

    @Override
    public String getName() {
        return "LoggingInterceptor";
    }
}`}
</Code>

### ToolInterceptor

拦截和修改工具调用：

<Code
  language="java"
  title="ToolMonitoringInterceptor 自定义 ToolInterceptor 示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/HooksExample.java"
>
{`import com.alibaba.cloud.ai.graph.agent.interceptor.ToolInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallResponse;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallHandler;

public class ToolMonitoringInterceptor extends ToolInterceptor {

    @Override
    public ToolCallResponse interceptToolCall(ToolCallRequest request, ToolCallHandler handler) {
        String toolName = request.getToolName();
        long startTime = System.currentTimeMillis();

        System.out.println("执行工具: " + toolName);

        try {
            ToolCallResponse response = handler.call(request);

            long duration = System.currentTimeMillis() - startTime;
            System.out.println("工具 " + toolName + " 执行成功 (耗时: " + duration + "ms)");

            return response;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            System.err.println("工具 " + toolName + " 执行失败 (耗时: " + duration + "ms): " + e.getMessage());

            return ToolCallResponse.of(
                request.getToolCallId(),
                request.getToolName(),
                "工具执行失败: " + e.getMessage()
            );
        }
    }

    @Override
    public String getName() {
        return "ToolMonitoringInterceptor";
    }
}`}
</Code>

### 使用 RunnableConfig 跨调用共享数据

`RunnableConfig` 提供了一个 `context()` 方法，允许你在同一个执行流程中的多个 Hook 调用、多轮模型或工具调用之间共享数据。这对于实现计数器、累积统计信息或跨多次调用维护状态非常有用。

**适用场景**：
* 跟踪模型或工具调用次数
* 累积性能指标（总耗时、平均响应时间等）
* 在 before/after Hook 之间传递临时数据
* 实现基于计数的限流或断路器

**示例：使用 RunnableConfig.context() 实现调用计数器**

<Code
  language="java"
  title="ModelCallCounterHook 调用计数器示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/HooksExample.java"
>
{`import com.alibaba.cloud.ai.graph.agent.hook.ModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.OverAllState;
import java.util.concurrent.CompletableFuture;
import java.util.Map;

@HookPositions({HookPosition.BEFORE_MODEL, HookPosition.AFTER_MODEL})
public class ModelCallCounterHook extends ModelHook {

    private static final String CALL_COUNT_KEY = "__model_call_count__";
    private static final String TOTAL_TIME_KEY = "__total_model_time__";
    private static final String START_TIME_KEY = "__call_start_time__";

    @Override
    public String getName() {
        return "model_call_counter";
    }

    @Override
    public CompletableFuture<Map<String, Object>> beforeModel(OverAllState state, RunnableConfig config) {
        // 从 context 读取当前计数（如果不存在则默认为 0）
        int currentCount = config.context().containsKey(CALL_COUNT_KEY)
                ? (int) config.context().get(CALL_COUNT_KEY) : 0;

        System.out.println("模型调用 #" + (currentCount + 1));

        // 记录开始时间
        config.context().put(START_TIME_KEY, System.currentTimeMillis());

        return CompletableFuture.completedFuture(Map.of());
    }

    @Override
    public CompletableFuture<Map<String, Object>> afterModel(OverAllState state, RunnableConfig config) {
        // 读取当前计数并递增
        int currentCount = config.context().containsKey(CALL_COUNT_KEY)
                ? (int) config.context().get(CALL_COUNT_KEY) : 0;
        config.context().put(CALL_COUNT_KEY, currentCount + 1);

        // 计算本次调用耗时并累加到总耗时
        if (config.context().containsKey(START_TIME_KEY)) {
            long startTime = (long) config.context().get(START_TIME_KEY);
            long duration = System.currentTimeMillis() - startTime;

            long totalTime = config.context().containsKey(TOTAL_TIME_KEY)
                    ? (long) config.context().get(TOTAL_TIME_KEY) : 0L;
            config.context().put(TOTAL_TIME_KEY, totalTime + duration);

            // 输出统计信息
            int newCount = currentCount + 1;
            long newTotalTime = totalTime + duration;
            System.out.println("模型调用完成: " + duration + "ms");
            System.out.println("累计统计 - 调用次数: " + newCount + ", 总耗时: " + newTotalTime + "ms, 平均: " + (newTotalTime / newCount) + "ms");
        }

        return CompletableFuture.completedFuture(Map.of());
    }
}`}
</Code>

**示例：基于 context 实现调用次数限制**

<Code
  language="java"
  title="ModelCallLimiterHook 调用次数限制示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/HooksExample.java"
>
{`import com.alibaba.cloud.ai.graph.agent.hook.ModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.agent.hook.JumpTo;
import org.springframework.ai.chat.messages.AssistantMessage;
import java.util.List;
import java.util.ArrayList;

@HookPositions({HookPosition.BEFORE_MODEL, HookPosition.AFTER_MODEL})
public class ModelCallLimiterHook extends ModelHook {

    private static final String CALL_COUNT_KEY = "__model_call_count__";
    private final int maxCalls;

    public ModelCallLimiterHook(int maxCalls) {
        this.maxCalls = maxCalls;
    }

    @Override
    public String getName() {
        return "model_call_limiter";
    }

    @Override
    public CompletableFuture<Map<String, Object>> beforeModel(OverAllState state, RunnableConfig config) {
        // 读取当前调用次数
        int callCount = config.context().containsKey(CALL_COUNT_KEY)
                ? (int) config.context().get(CALL_COUNT_KEY) : 0;

        // 检查是否超过限制
        if (callCount >= maxCalls) {
            System.out.println("达到模型调用次数限制: " + maxCalls);

            // 添加终止消息
            List<Message> messages = new ArrayList<>(
                (List<Message>) state.value("messages").orElse(new ArrayList<>())
            );
            messages.add(new AssistantMessage(
                "已达到模型调用次数限制 (" + callCount + "/" + maxCalls + ")，Agent 执行终止。"
            ));

            // 返回更新并跳转到结束
            return CompletableFuture.completedFuture(Map.of("messages", messages));
        }

        return CompletableFuture.completedFuture(Map.of());
    }

    @Override
    public CompletableFuture<Map<String, Object>> afterModel(OverAllState state, RunnableConfig config) {
        // 递增计数器
        int callCount = config.context().containsKey(CALL_COUNT_KEY)
                ? (int) config.context().get(CALL_COUNT_KEY) : 0;
        config.context().put(CALL_COUNT_KEY, callCount + 1);

        return CompletableFuture.completedFuture(Map.of());
    }

    @Override
    public List<JumpTo> canJumpTo() {
        return List.of(JumpTo.end);
    }
}`}
</Code>

**使用示例**：

<Code
  language="java"
  title="使用 ModelCallCounterHook 和 ModelCallLimiterHook" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/HooksExample.java"
>
{`ReactAgent agent = ReactAgent.builder()
    .name("limited_agent")
    .model(chatModel)
    .tools(tools)
    .hooks(new ModelCallCounterHook())  // 监控调用统计
    .hooks(new ModelCallLimiterHook(5)) // 限制最多调用 5 次
    .build();`}
</Code>

**关键要点**：

* **context() 是共享的**: 同一个执行流程中的所有 Hook 共享同一个 context
* **数据持久性**: context 中的数据在整个 Agent 执行期间保持有效
* **类型安全**: 需要自己管理 context 中数据的类型转换
* **命名约定**: 建议使用双下划线前缀命名 context key（如 `__model_call_count__`）以避免与用户数据冲突

## 执行顺序

使用多个 Hooks 和 Interceptors 时，理解执行顺序很重要：

<Code
  language="java"
  title="多个 Hooks 和 Interceptors 配置示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/HooksExample.java"
>
{`ReactAgent agent = ReactAgent.builder()
    .name("my_agent")
    .model(chatModel)
    .hooks(hook1, hook2, hook3)
    .interceptors(interceptor1, interceptor2)
    .interceptors(toolInterceptor1, toolInterceptor2)
    .build();`}
</Code>

**执行流程**：

1. **Before Agent Hooks**（按顺序）:
   - `hook1.beforeAgent()`
   - `hook2.beforeAgent()`
   - `hook3.beforeAgent()`

2. **Agent 循环开始**

3. **Before Model Hooks**（按顺序）:
   - `hook1.beforeModel()`
   - `hook2.beforeModel()`
   - `hook3.beforeModel()`

4. **Model Interceptors**（嵌套调用）:
   - `interceptor1` → `interceptor2` → 模型调用

5. **After Model Hooks**（逆序）:
   - `hook3.afterModel()`
   - `hook2.afterModel()`
   - `hook1.afterModel()`

6. **Tool Interceptors**（如果有工具调用，嵌套调用）:
   - `toolInterceptor1` → `toolInterceptor2` → 工具执行

7. **Agent 循环结束**

8. **After Agent Hooks**（逆序）:
   - `hook3.afterAgent()`
   - `hook2.afterAgent()`
   - `hook1.afterAgent()`

**关键规则**：

* `before_*` hooks: 从第一个到最后一个
* `after_*` hooks: 从最后一个到第一个（逆序）
* Interceptors: 嵌套调用（第一个拦截器包装所有其他的）

<!-- ### Agent 跳转 -->

<!-- 要从 Hook 中提前退出，返回包含 `jump_to` 的字典： -->

<!-- ```java -->
<!-- import com.alibaba.cloud.ai.graph.agent.hook.JumpTo; -->

<!-- public class EarlyExitHook implements ModelHook { -->

<!--     @Override -->
<!--     public String getName() { -->
<!--         return "early_exit"; -->
<!--     } -->

<!--     @Override -->
<!--     public HookPosition[] getHookPositions() { -->
<!--         return new HookPosition[]{HookPosition.BEFORE_MODEL}; -->
<!--     } -->

<!--     @Override -->
<!--     public List<JumpTo> canJumpTo() { -->
<!--         return List.of(JumpTo.end, JumpTo.tool); -->
<!--     } -->

<!--     @Override -->
<!--     public Map<String, Object> beforeModel(OverAllState state, RunnableConfig config) { -->
<!--         // 检查某些条件 -->
<!--         if (shouldExit(state)) { -->
<!--             // 跳转到结束 -->
<!--             return Map.of( -->
<!--                 "jump_to", JumpTo.end, -->
<!--                 "messages", List.of(new AssistantMessage("由于条件满足而提前退出")) -->
<!--             ); -->
<!--         } -->
<!--         return Map.of(); -->
<!--     } -->
<!-- } -->
<!-- ``` -->

<!-- 可用的跳转目标： -->

<!-- * `JumpTo.end`: 跳转到 Agent 执行的结束 -->
<!-- * `JumpTo.tool`: 跳转到工具节点 -->
<!-- * `JumpTo.model`: 跳转到模型节点 -->

<!-- **重要提示**：从 `beforeModel` 或 `afterModel` 跳转到 `model` 时，所有 `beforeModel` Hook 将再次运行。 -->

## 实际示例

### 示例 1：内容审核 Interceptor

<Code
  language="java"
  title="ContentModerationInterceptor 内容审核示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/HooksExample.java"
>
{`public class ContentModerationInterceptor extends ModelInterceptor {

    private static final List<String> BLOCKED_WORDS =
        List.of("敏感词1", "敏感词2", "敏感词3");

    @Override
    public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
        // 检查输入
        for (Message msg : request.getMessages()) {
            String content = msg.getText().toLowerCase();
            for (String blocked : BLOCKED_WORDS) {
                if (content.contains(blocked)) {
                    return ModelResponse.blocked(
                        "检测到不适当的内容，请修改您的输入"
                    );
                }
            }
        }

        // 执行模型调用
        ModelResponse response = handler.call(request);

        // 检查输出
        String output = response.getContent();
        for (String blocked : BLOCKED_WORDS) {
            if (output.contains(blocked)) {
                // 清理输出
                output = output.replaceAll(blocked, "[已过滤]");
                return response.withContent(output);
            }
        }

        return response;
    }

    @Override
    public String getName() {
        return "ContentModerationInterceptor";
    }
}`}
</Code>

### 示例 2：性能监控 - 使用 Interceptor

使用 `ModelInterceptor` 和 `ToolInterceptor` 监控模型和工具调用的性能：

<Code
  language="java"
  title="性能监控 Interceptor 示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/HooksExample.java"
>
{`// 模型调用性能监控
public class ModelPerformanceInterceptor extends ModelInterceptor {

    @Override
    public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
        // 请求前记录
        System.out.println("发送请求到模型: " + request.getMessages().size() + " 条消息");

        long startTime = System.currentTimeMillis();

        // 执行实际调用
        ModelResponse response = handler.call(request);

        // 响应后记录
        long duration = System.currentTimeMillis() - startTime;
        System.out.println("模型响应耗时: " + duration + "ms");

        return response;
    }

    @Override
    public String getName() {
        return "ModelPerformanceInterceptor";
    }
}

// 工具调用性能监控
public class ToolPerformanceInterceptor extends ToolInterceptor {

    @Override
    public ToolCallResponse interceptToolCall(ToolCallRequest request, ToolCallHandler handler) {
        String toolName = request.getToolName();
        long startTime = System.currentTimeMillis();

        System.out.println("执行工具: " + toolName);

        try {
            ToolCallResponse response = handler.call(request);

            long duration = System.currentTimeMillis() - startTime;
            System.out.println("工具 " + toolName + " 执行成功 (耗时: " + duration + "ms)");

            return response;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            System.err.println("工具 " + toolName + " 执行失败 (耗时: " + duration + "ms): " + e.getMessage());

            return ToolCallResponse.of(
                request.getToolCallId(),
                request.getToolName(),
                "工具执行失败: " + e.getMessage()
            );
        }
    }

    @Override
    public String getName() {
        return "ToolPerformanceInterceptor";
    }
}

// 使用示例
ReactAgent agent = ReactAgent.builder()
    .name("monitored_agent")
    .model(chatModel)
    .tools(tools)
    .interceptors(new ModelPerformanceInterceptor())
    .interceptors(new ToolPerformanceInterceptor())
    .build();`}
</Code>

### 示例 3：工具缓存 Interceptor

<Code
  language="java"
  title="ToolCacheInterceptor 工具缓存示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/HooksExample.java"
>
{`public class ToolCacheInterceptor extends ToolInterceptor {

    private Map<String, ToolCallResponse> cache = new ConcurrentHashMap<>();
    private final long ttlMs;

    public ToolCacheInterceptor(long ttlMs) {
        this.ttlMs = ttlMs;
    }

    @Override
    public ToolCallResponse interceptToolCall(ToolCallRequest request, ToolCallHandler handler) {
        String cacheKey = generateCacheKey(request);

        // 检查缓存
        ToolCallResponse cached = cache.get(cacheKey);
        if (cached != null && !isExpired(cached)) {
            System.out.println("缓存命中: " + request.getToolName());
            return cached;
        }

        // 执行工具
        ToolCallResponse response = handler.call(request);

        // 缓存结果
        cache.put(cacheKey, response);

        return response;
    }

    @Override
    public String getName() {
        return "ToolCacheInterceptor";
    }

    private String generateCacheKey(ToolCallRequest request) {
        return request.getToolName() + ":" +
               request.getArguments();
    }

    private boolean isExpired(ToolCallResponse response) {
        // 实现 TTL 检查逻辑
        return false;
    }
}`}
</Code>

## 总结

Hooks 和 Interceptors 提供了强大的机制来控制和自定义 Agent 的执行流程：

- **Hooks**: 在 Agent 执行的关键点插入自定义逻辑（before/after）
- **Interceptors**: 拦截和修改模型调用和工具执行
- **灵活组合**: 可以组合多个 Hooks 和 Interceptors
- **执行顺序**: 理解执行顺序对于构建正确的功能至关重要
- **跳转控制**: 支持提前退出和条件跳转

通过合理使用这些机制，你可以构建具有监控、安全、性能优化等高级功能的生产级 Agent 应用。

## 相关资源

* [Agents 文档](./agents.md) - 了解 ReactAgent 的核心概念
* [Messages 文档](./messages.md) - 了解消息类型和使用
* [Models 文档](./models.md) - 了解模型配置和使用
