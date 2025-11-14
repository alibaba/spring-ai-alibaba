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

```java
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.*;
import com.alibaba.cloud.ai.graph.agent.interceptor.*;

ReactAgent agent = ReactAgent.builder()
    .name("my_agent")
    .model(chatModel)
    .tools(tools)
    .hooks(loggingHook, messageTrimmingHook)
    .interceptors(guardrailInterceptor, retryInterceptor)
    .build();
```

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

```java
import com.alibaba.cloud.ai.graph.agent.hook.summarization.SummarizationHook;

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
    .build();
```

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

```java
import com.alibaba.cloud.ai.graph.agent.hook.hip.HumanInTheLoopHook;
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
    .build();
```

**重要提示**：Human-in-the-loop Hook 需要 checkpointer 来维护跨中断的状态。示例中我们演示用了 `RedisSaver`。

### 模型调用限制（Model Call Limit）

限制模型调用次数以防止无限循环或过度成本。

**适用场景**：
* 防止失控的 Agent 进行太多 API 调用
* 在生产部署中强制执行成本控制
* 在特定调用预算内测试 Agent 行为

```java
ReactAgent agent = ReactAgent.builder()
    .name("my_agent")
    .model(chatModel)
    .maxIterations(10)  // 最多 10 次迭代（默认为 10）
    .saver(new MemorySaver())
    .build();
```

### 工具调用限制（Tool Call Limit）

使用自定义停止条件限制工具调用：

```java
import com.alibaba.cloud.ai.graph.OverAllState;
import java.util.function.Function;

Function<OverAllState, Boolean> customStopCondition = state -> {
    // 如果找到答案或错误过多则停止
    Optional<Object> foundAnswer = state.value("answer_found");
    if (foundAnswer.isPresent() && (Boolean) foundAnswer.get()) {
        return false;  // 停止执行
    }

    Optional<Object> errorCount = state.value("error_count");
    if (errorCount.isPresent() && (Integer) errorCount.get() > 3) {
        return false;  // 停止执行
    }

    return true;  // 继续执行
};

ReactAgent agent = ReactAgent.builder()
    .name("controlled_agent")
    .model(chatModel)
    .shouldContinueFunction(customStopCondition)
    .saver(new MemorySaver())
    .build();
```

### PII 检测（Personally Identifiable Information）

检测和处理对话中的个人身份信息。

**适用场景**：
* 具有合规要求的医疗保健和金融应用
* 需要清理日志的客户服务 Agent
* 任何处理敏感用户数据的应用程序

```java
import com.alibaba.cloud.ai.graph.agent.interceptor.PIIDetectionInterceptor;

// 使用
ReactAgent agent = ReactAgent.builder()
    .name("secure_agent")
    .model(chatModel)
    .modelInterceptors(new PIIDetectionInterceptor())
    .build();
```

### 工具重试（Tool Retry）

自动重试失败的工具调用，具有可配置的指数退避。

**适用场景**：
* 处理外部 API 调用中的瞬态故障
* 提高依赖网络的工具的可靠性
* 构建优雅处理临时错误的弹性 Agent

```java
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolRetryInterceptor;

// 使用
ReactAgent agent = ReactAgent.builder()
    .name("resilient_agent")
    .model(chatModel)
    .tools(searchTool, databaseTool)
    .toolInterceptors(new ToolRetryInterceptor(3, 1000, 2.0))
    .build();
```

**配置选项**：
- `maxRetries`: 最大重试次数（默认 3）
- `initialDelayMs`: 初始延迟毫秒数（默认 1000）
- `backoffMultiplier`: 退避倍数（默认 2.0）

### Planning（规划）

在执行工具之前强制执行一个规划步骤，以概述 Agent 将要采取的步骤。

**适用场景**：
*   需要执行复杂、多步骤任务的 Agent
*   通过在执行前显示 Agent 的计划来提高透明度
*   通过检查建议的计划来调试错误

```java
import com.alibaba.cloud.ai.graph.agent.hook.PlanningHook;

// 使用
ReactAgent agent = ReactAgent.builder()
    .name("planning_agent")
    .model(chatModel)
    .tools(myTool)
    .hooks(new PlanningHook())
    .build();
```

### LLM Tool Selector（LLM 工具选择器）

使用一个 LLM 来决定在多个可用工具之间选择哪个工具。

**适用场景**：
*   当多个工具可以实现相似目标时
*   需要根据细微的上下文差异进行工具选择
*   动态选择最适合特定输入的工具

```java
import com.alibaba.cloud.ai.graph.agent.interceptor.LLMToolSelectorInterceptor;
import org.springframework.ai.chat.model.ChatModel;

// 使用
ChatModel selectorModel = ...; // 用于选择的另一个ChatModel
ReactAgent agent = ReactAgent.builder()
    .name("smart_selector_agent")
    .model(chatModel)
    .tools(tool1, tool2)
    .toolInterceptors(new LLMToolSelectorInterceptor(selectorModel))
    .build();
```

### LLM Tool Emulator（LLM 工具模拟器）

在没有实际执行工具的情况下，使用 LLM 模拟工具的输出。

**适用场景**：
*   在演示或测试期间模拟 API
*   在开发过程中为工具提供占位符行为
*   在不产生实际成本或副作用的情况下测试 Agent 逻辑

```java
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolEmulatorInterceptor;

// 使用
ReactAgent agent = ReactAgent.builder()
    .name("emulator_agent")
    .model(chatModel)
    .tools(simulatedTool)
    .toolInterceptors(new ToolEmulatorInterceptor(chatModel))
    .build();
```

### Context Editing（上下文编辑）

在将上下文发送给 LLM 之前对其进行修改，以注入、删除或修改信息。

**适用场景**：
*   向 LLM 提供额外的上下文或指令
*   从对话历史中删除不相关或冗余的信息
*   动态修改上下文以引导 Agent 的行为

```java
import com.alibaba.cloud.ai.graph.agent.hook.ContextEditingHook;

// 使用
ReactAgent agent = ReactAgent.builder()
    .name("context_aware_agent")
    .model(chatModel)
    .hooks(new ContextEditingHook("Remember to be polite and helpful."))
    .build();
```

## 自定义 Hooks 和 Interceptors

通过实现在 Agent 执行流程中特定点运行的钩子来构建自定义功能。

你可以通过以下方式创建自定义功能：

1. **ModelHook** - 在模型调用前后执行
2. **AgentHook** - 在 Agent 开始和结束时执行
3. **ModelInterceptor** - 拦截和修改模型请求/响应
4. **ToolInterceptor** - 拦截和修改工具调用

### ModelHook

在模型调用前后执行自定义逻辑：

```java
import com.alibaba.cloud.ai.graph.agent.hook.ModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;

public class CustomModelHook implements ModelHook {

    @Override
    public String getName() {
        return "custom_model_hook";
    }

    @Override
    public HookPosition[] getHookPositions() {
        return new HookPosition[]{
            HookPosition.BEFORE_MODEL,
            HookPosition.AFTER_MODEL
        };
    }

    @Override
    public Map<String, Object> beforeModel(OverAllState state, RunnableConfig config) {
        // 在模型调用前执行
        System.out.println("准备调用模型...");

        // 可以修改状态
        // 例如：添加额外的上下文
        return Map.of("extra_context", "某些额外信息");
    }

    @Override
    public Map<String, Object> afterModel(OverAllState state, RunnableConfig config) {
        // 在模型调用后执行
        System.out.println("模型调用完成");

        // 可以记录响应信息
        return Map.of();
    }
}
```

### AgentHook

在 Agent 整体执行的开始和结束时执行：

```java
import com.alibaba.cloud.ai.graph.agent.hook.AgentHook;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;

public class CustomAgentHook implements AgentHook {

    @Override
    public String getName() {
        return "custom_agent_hook";
    }

    @Override
    public HookPosition[] getHookPositions() {
        return new HookPosition[]{
            HookPosition.BEFORE_AGENT,
            HookPosition.AFTER_AGENT
        };
    }

    @Override
    public Map<String, Object> beforeAgent(OverAllState state, RunnableConfig config) {
        System.out.println("Agent 开始执行");
        // 可以初始化资源、记录开始时间等
        return Map.of("start_time", System.currentTimeMillis());
    }

    @Override
    public Map<String, Object> afterAgent(OverAllState state, RunnableConfig config) {
        System.out.println("Agent 执行完成");
        // 可以清理资源、计算执行时间等
        Optional<Object> startTime = state.value("start_time");
        if (startTime.isPresent()) {
            long duration = System.currentTimeMillis() - (Long) startTime.get();
            System.out.println("执行耗时: " + duration + "ms");
        }
        return Map.of();
    }
}
```

### ModelInterceptor

拦截和修改模型请求和响应：

```java
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;

public class LoggingInterceptor implements ModelInterceptor {

    @Override
    public ModelResponse intercept(ModelRequest request, ModelCallHandler handler) {
        // 请求前记录
        System.out.println("发送请求到模型: " + request.getMessages().size() + " 条消息");

        long startTime = System.currentTimeMillis();

        // 执行实际调用
        ModelResponse response = handler.handle(request);

        // 响应后记录
        long duration = System.currentTimeMillis() - startTime;
        System.out.println("模型响应耗时: " + duration + "ms");

        return response;
    }
}
```

### ToolInterceptor

拦截和修改工具调用：

```java
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallResponse;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallHandler;

public class ToolMonitoringInterceptor implements ToolInterceptor {

    @Override
    public ToolCallResponse intercept(ToolCallRequest request, ToolCallHandler handler) {
        String toolName = request.getToolCall().name();
        long startTime = System.currentTimeMillis();

        System.out.println("执行工具: " + toolName);

        try {
            ToolCallResponse response = handler.handle(request);

            long duration = System.currentTimeMillis() - startTime;
            System.out.println("工具 " + toolName + " 执行成功 (耗时: " + duration + "ms)");

            return response;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            System.err.println("工具 " + toolName + " 执行失败 (耗时: " + duration + "ms): " + e.getMessage());

            return ToolCallResponse.error(
                request.getToolCall(),
                "工具执行失败: " + e.getMessage()
            );
        }
    }
}
```

## 执行顺序

使用多个 Hooks 和 Interceptors 时，理解执行顺序很重要：

```java
ReactAgent agent = ReactAgent.builder()
    .name("my_agent")
    .model(chatModel)
    .hooks(hook1, hook2, hook3)
    .modelInterceptors(interceptor1, interceptor2)
    .toolInterceptors(toolInterceptor1, toolInterceptor2)
    .build();
```

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

```java
public class ContentModerationInterceptor implements ModelInterceptor {

    private static final List<String> BLOCKED_WORDS =
        List.of("敏感词1", "敏感词2", "敏感词3");

    @Override
    public ModelResponse intercept(ModelRequest request, ModelCallHandler handler) {
        // 检查输入
        for (Message msg : request.getMessages()) {
            String content = msg.getContent().toLowerCase();
            for (String blocked : BLOCKED_WORDS) {
                if (content.contains(blocked)) {
                    return ModelResponse.blocked(
                        "检测到不适当的内容，请修改您的输入"
                    );
                }
            }
        }

        // 执行模型调用
        ModelResponse response = handler.handle(request);

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
}
```

### 示例 2：性能监控 Hook

```java
public class PerformanceMonitoringHook implements AgentHook {

    private Map<String, Long> metrics = new HashMap<>();

    @Override
    public String getName() {
        return "performance_monitoring";
    }

    @Override
    public HookPosition[] getHookPositions() {
        return new HookPosition[]{
            HookPosition.BEFORE_AGENT,
            HookPosition.AFTER_AGENT
        };
    }

    @Override
    public Map<String, Object> beforeAgent(OverAllState state, RunnableConfig config) {
        metrics.put("start_time", System.currentTimeMillis());
        metrics.put("model_calls", 0L);
        metrics.put("tool_calls", 0L);
        return Map.of();
    }

    @Override
    public Map<String, Object> afterAgent(OverAllState state, RunnableConfig config) {
        long duration = System.currentTimeMillis() - metrics.get("start_time");

        System.out.println("===== 性能报告 =====");
        System.out.println("总耗时: " + duration + "ms");
        System.out.println("模型调用次数: " + metrics.get("model_calls"));
        System.out.println("工具调用次数: " + metrics.get("tool_calls"));
        System.out.println("==================");

        return Map.of();
    }
}
```

### 示例 3：工具缓存 Interceptor

```java
public class ToolCacheInterceptor implements ToolInterceptor {

    private Map<String, ToolCallResponse> cache = new ConcurrentHashMap<>();
    private final long ttlMs;

    public ToolCacheInterceptor(long ttlMs) {
        this.ttlMs = ttlMs;
    }

    @Override
    public ToolCallResponse intercept(ToolCallRequest request, ToolCallHandler handler) {
        String cacheKey = generateCacheKey(request);

        // 检查缓存
        ToolCallResponse cached = cache.get(cacheKey);
        if (cached != null && !isExpired(cached)) {
            System.out.println("缓存命中: " + request.getToolCall().name());
            return cached;
        }

        // 执行工具
        ToolCallResponse response = handler.handle(request);

        // 缓存结果
        cache.put(cacheKey, response);

        return response;
    }

    private String generateCacheKey(ToolCallRequest request) {
        return request.getToolCall().name() + ":" +
               request.getToolCall().arguments();
    }

    private boolean isExpired(ToolCallResponse response) {
        // 实现 TTL 检查逻辑
        return false;
    }
}
```

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
