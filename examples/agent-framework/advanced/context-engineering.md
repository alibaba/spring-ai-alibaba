---
title: 上下文工程（Context Engineering）
description: 学习如何通过上下文工程提高Agent的可靠性，包括模型上下文、工具上下文和生命周期上下文的管理
keywords:
  [
    上下文工程,
    Context Engineering,
    Agent可靠性,
    模型上下文,
    工具上下文,
    生命周期上下文,
    LLM优化,
  ]
---

## 概述

构建 Agent 的难点在于使其足够可靠、效果足够好。虽然我们可以很容易写一个 Agent 示例，但要做一个能在生产环境中稳定使用、能解决实际问题的 Agent 并不容易。

### 为什么 Agent 会失败？

当 Agent 失败时，通常是因为 Agent 内部的 LLM 调用采取了错误的操作或者没有按我们预期的执行。LLM 失败的原因有两个：

1. 底层 LLM 能力不足
2. 没有向 LLM 传递"正确"的上下文

大多数情况下 —— 实际上是第二个原因导致 Agent 不可靠。

**上下文工程**是以正确的格式提供正确的信息和工具，使 LLM 能够完成任务。这是 AI 工程师的首要工作。缺乏"正确"的上下文是更可靠 Agent 的头号障碍，Spring AI Alibaba 的 Agent 抽象专门设计用于优化上下文工程。

### Agent 循环

典型的 Agent 循环由两个主要步骤组成：

1. **模型调用** - 使用提示和可用工具调用 LLM，返回响应或执行工具的请求
2. **工具执行** - 执行 LLM 请求的工具，返回工具结果

![reactagent](/img/agent/agents/reactagent.png)

此循环持续进行，直到 LLM 决定任务完成并退出。

### 你可以控制什么

要构建可靠的 Agent，你需要控制 Agent 循环每个步骤发生的事情，以及步骤之间发生的事情。

| 上下文类型                               | 你控制的内容                                                | 瞬态或持久 |
| ---------------------------------------- | ----------------------------------------------------------- | ---------- |
| **[模型上下文](#model-context)**         | 模型调用中包含什么（指令、消息历史、工具、响应格式）        | 瞬态       |
| **[工具上下文](#tool-context)**          | 工具可以访问和产生什么（对状态、存储、运行时上下文的读/写） | 持久       |
| **[生命周期上下文](#lifecycle-context)** | 模型和工具调用之间发生什么（摘要、防护栏、日志等）          | 持久       |

> - 瞬态上下文。LLM 在单次调用中看到的内容。你可以修改消息、工具或提示，而不改变状态中保存的内容。
> - 持久上下文。跨轮次保存在状态中的内容。生命周期钩子和工具写入会永久修改它。

### 数据源

在整个过程中，你的 Agent 访问（读/写）不同的数据源：

| 数据源            | 别名     | 范围     | 示例                                          |
| ----------------- | -------- | -------- | --------------------------------------------- |
| **运行时上下文**  | 静态配置 | 会话范围 | 用户 ID、API 密钥、数据库连接、权限、环境设置 |
| **状态（State）** | 短期记忆 | 会话范围 | 当前消息、上传的文件、认证状态、工具结果      |
| **存储（Store）** | 长期记忆 | 跨会话   | 用户偏好、提取的见解、记忆、历史数据          |

### 工作原理

在 Spring AI Alibaba 中，**Hook**和**Interceptor**是实现上下文工程的机制。

它们允许你挂接到 Agent 生命周期的任何步骤并：

- 更新上下文
- 跳转到 Agent 生命周期的不同步骤

在本指南中，你将看到频繁使用 Hook 和 Interceptor API 作为上下文工程的手段。

## 模型上下文（Model Context）

控制每次模型调用中包含的内容——指令、可用工具、使用哪个模型以及输出格式。这些决策直接影响可靠性和成本。

<div class="card-group" cols="2">
  <div class="card">
    <div class="card__header">
      <div class="card__icon">💬</div>
      <div class="card__title">系统提示</div>
    </div>
    <div class="card__body">开发者对LLM的基础指令。</div>
  </div>

  <div class="card">
    <div class="card__header">
      <div class="card__icon">💭</div>
      <div class="card__title">消息</div>
    </div>
    <div class="card__body">发送给LLM的完整消息列表（对话历史）。</div>
  </div>

  <div class="card">
    <div class="card__header">
      <div class="card__icon">🔧</div>
      <div class="card__title">工具</div>
    </div>
    <div class="card__body">Agent可以访问以采取行动的工具。</div>
  </div>

  <div class="card">
    <div class="card__header">
      <div class="card__icon">🧠</div>
      <div class="card__title">模型</div>
    </div>
    <div class="card__body">要调用的实际模型（包括配置）。</div>
  </div>

  <div class="card">
    <div class="card__header">
      <div class="card__icon">📋</div>
      <div class="card__title">响应格式</div>
    </div>
    <div class="card__body">模型最终响应的架构规范。</div>
  </div>
</div>

所有这些类型的模型上下文都可以从**状态**（短期记忆）、**存储**（长期记忆）或**运行时上下文**（静态配置）中获取。

### 系统提示（System Prompt）

系统提示设置 LLM 的行为和能力。不同的用户、上下文或对话阶段需要不同的指令。成功的 Agent 利用记忆、偏好和配置为对话的当前状态提供正确的指令。

#### 基于状态的动态提示

<Code
  language="java"
  title="基于状态的动态提示示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/advanced/ContextEngineeringExample.java"
>
{`import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import org.springframework.ai.chat.messages.Message;

// 创建一个模型拦截器，根据对话长度调整系统提示
class StateAwarePromptInterceptor extends ModelInterceptor {
    @Override
    public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
        List<Message> messages = request.getMessages();
        int messageCount = messages.size();

        // 基础提示
        String basePrompt = "你是一个有用的助手。";

        // 根据消息数量调整提示
        if (messageCount > 10) {
            basePrompt += "\n这是一个长对话 - 请尽量保持精准简捷。";
        }

        // 更新系统消息（参考 TodoListInterceptor 的实现方式）
        SystemMessage enhancedSystemMessage;
        if (request.getSystemMessage() == null) {
            enhancedSystemMessage = new SystemMessage(basePrompt);
        } else {
            enhancedSystemMessage = new SystemMessage(
                request.getSystemMessage().getText() + "\n\n" + basePrompt
            );
        }

        // 创建增强的请求
        ModelRequest enhancedRequest = ModelRequest.builder(request)
            .systemMessage(enhancedSystemMessage)
            .build();

        // 调用处理器
        return handler.call(enhancedRequest);
    }

    @Override
    public String getName() {
        return "StateAwarePromptInterceptor";
    }
}

// 使用拦截器创建Agent
ReactAgent agent = ReactAgent.builder()
    .name("context_aware_agent")
    .model(chatModel)
    .interceptors(new StateAwarePromptInterceptor())
    .build();`}
</Code>

#### 基于存储的个性化提示

<Code
  language="java"
  title="基于存储的个性化提示示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/advanced/ContextEngineeringExample.java"
>
{`// 从长期记忆加载用户偏好
class PersonalizedPromptInterceptor extends ModelInterceptor {
    private final UserPreferenceStore store;

    public PersonalizedPromptInterceptor(UserPreferenceStore store) {
        this.store = store;
    }

    @Override
    public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
        // 从运行时上下文获取用户ID
        String userId = getUserIdFromContext(request);

        // 从存储加载用户偏好
        UserPreferences prefs = store.getPreferences(userId);

        // 构建个性化提示
        String personalizedPrompt = buildPersonalizedPrompt(prefs);

        // 更新系统消息（参考 TodoListInterceptor 的实现方式）
        SystemMessage enhancedSystemMessage;
        if (request.getSystemMessage() == null) {
            enhancedSystemMessage = new SystemMessage(personalizedPrompt);
        } else {
            enhancedSystemMessage = new SystemMessage(
                request.getSystemMessage().getText() + "\n\n" + personalizedPrompt
            );
        }

        // 创建增强的请求
        ModelRequest enhancedRequest = ModelRequest.builder(request)
            .systemMessage(enhancedSystemMessage)
            .build();

        // 调用处理器
        return handler.call(enhancedRequest);
    }

    private String getUserIdFromContext(ModelRequest request) {
        // 从请求上下文提取用户ID
        return "user_001"; // 简化示例
    }

    private String buildPersonalizedPrompt(UserPreferences prefs) {
        StringBuilder prompt = new StringBuilder("你是一个有用的助手。");

        if (prefs.getCommunicationStyle() != null) {
            prompt.append("\n沟通风格：").append(prefs.getCommunicationStyle());
        }

        if (prefs.getLanguage() != null) {
            prompt.append("\n使用语言：").append(prefs.getLanguage());
        }

        if (!prefs.getInterests().isEmpty()) {
            prompt.append("\n用户兴趣：").append(String.join(", ", prefs.getInterests()));
        }

        return prompt.toString();
    }

    @Override
    public String getName() {
        return "PersonalizedPromptInterceptor";
    }
}`}
</Code>

### 消息历史（Messages）

控制发送给 LLM 的消息列表。你可以：

- 过滤或修改消息
- 添加上下文或摘要
- 压缩长对话

#### 消息过滤

<Code
  language="java"
  title="MessageFilterInterceptor 消息过滤示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/advanced/ContextEngineeringExample.java"
>
{`class MessageFilterInterceptor extends ModelInterceptor {
    private final int maxMessages;

    public MessageFilterInterceptor(int maxMessages) {
        this.maxMessages = maxMessages;
    }

    @Override
    public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
        List<Message> messages = request.getMessages();

        // 只保留最近的N条消息
        if (messages.size() > maxMessages) {
            List<Message> filtered = new ArrayList<>();

            // 添加系统消息
            messages.stream()
                .filter(m -> m instanceof SystemMessage)
                .findFirst()
                .ifPresent(filtered::add);

            // 添加最近的消息
            int startIndex = Math.max(0, messages.size() - maxMessages + 1);
            filtered.addAll(messages.subList(startIndex, messages.size()));

            messages = filtered;
        }

        ModelRequest enhancedRequest = ModelRequest.builder(request)
            .messages(messages)
            .build();

        return handler.call(enhancedRequest);
    }

    @Override
    public String getName() {
        return "MessageFilterInterceptor";
    }
}`}
</Code>

> **瞬时消息更新 VS 持久消息更新**
>
> 1. 上述示例使用 `ModelInterceptor` 来实现临时更新 —— 修改单次调用时发送给模型的消息内容，而不会改变状态中保存的数据。
> 2. 对于需要持久更新状态的情况（例如生命周期上下文中的摘要示例），请使用如 ModelHook 等生命周期钩子来永久更新对话历史。更多详情请参阅 Hook & Interceptor 文档。

### 工具（Tools）

动态控制 Agent 可以访问哪些工具。

#### 基于上下文的工具选择

<Code
  language="java"
  title="ContextualToolInterceptor 基于上下文的工具选择示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/advanced/ContextEngineeringExample.java"
>
{`class ContextualToolInterceptor extends ModelInterceptor {
    private final Map<String, List<ToolCallback>> roleBasedTools;

    public ContextualToolInterceptor(Map<String, List<ToolCallback>> roleBasedTools) {
        this.roleBasedTools = roleBasedTools;
    }

    @Override
    public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
        // 从上下文获取用户角色
        String userRole = getUserRole(request);

        // 根据角色选择工具
        List<ToolCallback> allowedTools = roleBasedTools.getOrDefault(
            userRole,
            Collections.emptyList()
        );

        // 更新工具选项（注：实际实现需要根据框架API调整）
        // 这里展示概念性代码
        System.out.println("为角色 " + userRole + " 选择了 " + allowedTools.size() + " 个工具");

        return handler.call(request);
    }

    private String getUserRole(ModelRequest request) {
        // 从请求上下文提取用户角色
        return "user"; // 简化示例
    }

    @Override
    public String getName() {
        return "ContextualToolInterceptor";
    }
}

// 配置基于角色的工具
Map<String, List<ToolCallback>> roleTools = Map.of(
    "admin", List.of(readTool, writeTool, deleteTool),
    "user", List.of(readTool),
    "guest", List.of()
);

ReactAgent agent = ReactAgent.builder()
    .name("role_based_agent")
    .model(chatModel)
    .interceptors(new ContextualToolInterceptor(roleTools))
    .build();`}
</Code>

### 模型选择（Model）

根据任务复杂度或用户偏好动态选择模型。

<Code
  language="java"
  title="DynamicModelInterceptor 动态模型选择示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/advanced/ContextEngineeringExample.java"
>
{`class DynamicModelInterceptor extends ModelInterceptor {
    private final ChatModel simpleModel;
    private final ChatModel complexModel;

    public DynamicModelInterceptor(ChatModel simpleModel, ChatModel complexModel) {
        this.simpleModel = simpleModel;
        this.complexModel = complexModel;
    }

    @Override
    public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
        // 分析任务复杂度
        boolean isComplexTask = analyzeComplexity(request.getMessages());

        // 选择合适的模型
        ChatModel selectedModel = isComplexTask ? complexModel : simpleModel;

        // 注意：在实际实现中，你可能需要在Agent级别切换模型
        // 这里展示的是概念性示例

        return handler.call(request);
    }

    private boolean analyzeComplexity(List<Message> messages) {
        // 实现复杂度分析逻辑
        // 例如：检查消息长度、关键词等
        return messages.size() > 5;
    }

    @Override
    public String getName() {
        return "DynamicModelInterceptor";
    }
}`}
</Code>

### 响应格式（Response Format）

使用结构化输出控制模型响应格式。

<Code
  language="java"
  title="响应格式控制示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/advanced/ContextEngineeringExample.java"
>
{`// 在Agent级别设置输出格式
ReactAgent agent = ReactAgent.builder()
    .name("structured_agent")
    .model(chatModel)
    .outputType(MyResponseClass.class) // 或 .outputSchema(jsonSchema)
    .build();

// 也可以在Interceptor中动态调整
class DynamicFormatInterceptor extends ModelInterceptor {
    @Override
    public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
        // 根据请求内容决定输出格式
        String outputSchema = determineOutputSchema(request);

        // 在消息中添加格式说明
        List<Message> updatedMessages = addFormatInstructions(
            request.getMessages(),
            outputSchema
        );

        ModelRequest enhancedRequest = ModelRequest.builder(request)
            .messages(updatedMessages)
            .build();

        return handler.call(enhancedRequest);
    }

    private String determineOutputSchema(ModelRequest request) {
        // 实现输出格式决定逻辑
        return "";
    }

    private List<Message> addFormatInstructions(List<Message> messages, String schema) {
        // 实现格式说明添加逻辑
        return messages;
    }

    @Override
    public String getName() {
        return "DynamicFormatInterceptor";
    }
}`}
</Code>

## 工具上下文（Tool Context）

控制工具可以访问和修改的内容。

### 工具中访问状态

<Code
  language="java"
  title="StatefulTool 工具中访问状态示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/advanced/ContextEngineeringExample.java"
>
{`class StatefulTool implements Function<StatefulTool.Request, StatefulTool.Response> {
    public record Request(String query) {}
    public record Response(String result) {}

    @Override
    public Response apply(Request request, ToolContext toolContext) {
        // 从 Agent 持久状态读取信息
        OverAllState currentState = (OverAllState) toolContext.getContext().get("state");
        // 'messages' can be any key persisted in short memory
        Optional<Object> messages = currentState.value("messages");

        // 从 Agent 运行上下文读取信息
        RunnableConfig config = (RunnableConfig) toolContext.getContext().get("config");
        Optional<Object> userContext = config.metadata("user_context_key");

        // 使用状态信息处理请求
        String result = processWithContext(request.query(), messages, userContext);

        return new Response(result);
    }

    private String processWithContext(String query, Optional<Object> messages, Optional<Object> userContext) {
        // 实现处理逻辑
        return "处理结果";
    }
}`}
</Code>

### 工具修改状态

<Code
  language="java"
  title="StateModifyingTool 工具修改状态示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/advanced/ContextEngineeringExample.java"
>
{`class StateModifyingTool implements Function<StateModifyingTool.Request, StateModifyingTool.Response> {
    public record Request(String data) {}
    public record Response(String status) {}

    @Override
    public Response apply(Request request, ToolContext toolContext) {
        // 从 Agent 持久状态读取信息
        Map<String, Object> extraState = (Map<String, Object>) toolContext.getContext().get("extraState");

        // 处理数据
        String processed = process(request.data());

        // extraState 是一个特殊设计，更新到 extraState 中的值会被持久化到 State 状态中，并被后续的 Loop 节点看到。
        extraState.put("processed_data", processed);

        return new Response("数据已处理并保存到状态");
    }

    private String process(String data) {
        // 实现处理逻辑
        return "处理后的数据";
    }
}`}
</Code>

## 生命周期上下文（Lifecycle Context）

使用 Hook 在 Agent 生命周期的不同阶段执行操作。

### Hook 位置

Spring AI Alibaba 支持以下 Hook 位置：

- `BEFORE_AGENT` - Agent 开始之前
- `AFTER_AGENT` - Agent 完成之后
- `BEFORE_MODEL` - 模型调用之前
- `AFTER_MODEL` - 模型调用之后

### 自定义 Hook 示例

<Code
  language="java"
  title="LoggingHook 自定义Hook示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/advanced/ContextEngineeringExample.java"
>
{`import com.alibaba.cloud.ai.graph.agent.hook.Hook;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.ModelHook;

class LoggingHook extends ModelHook {
    @Override
    public String getName() {
        return "logging_hook";
    }

    @Override
    public HookPosition[] getHookPositions() {
        return new HookPosition[]{
            HookPosition.BEFORE_MODEL,
            HookPosition.AFTER_MODEL
        };
    }

    @Override
    public CompletableFuture<Map<String, Object>> beforeModel(OverAllState state, RunnableConfig config) {
        // 在模型调用前记录
        List<?> messages = (List<?>) state.value("messages").orElse(List.of());
        System.out.println("模型调用前 - 消息数: " + messages.size());
        return CompletableFuture.completedFuture(Map.of());
    }

    @Override
    public CompletableFuture<Map<String, Object>> afterModel(OverAllState state, RunnableConfig config) {
        // 在模型调用后记录
        System.out.println("模型调用后 - 响应已生成");
        return CompletableFuture.completedFuture(Map.of());
    }
}

// 使用Hook
ReactAgent agent = ReactAgent.builder()
    .name("logged_agent")
    .model(chatModel)
    .hooks(new LoggingHook())
    .build();`}
</Code>

### 消息摘要 Hook

<Code
  language="java"
  title="SummarizationHook 消息摘要Hook示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/advanced/ContextEngineeringExample.java"
>
{`import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.agent.hook.messages.AgentCommand;
import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.messages.UpdatePolicy;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@HookPositions({HookPosition.BEFORE_MODEL})
class SummarizationHook extends MessagesModelHook {
    private final ChatModel summarizationModel;
    private final int triggerLength;

    public SummarizationHook(ChatModel model, int triggerLength) {
        this.summarizationModel = model;
        this.triggerLength = triggerLength;
    }

    @Override
    public String getName() {
        return "summarization_hook";
    }

    @Override
    public AgentCommand beforeModel(List<Message> previousMessages, RunnableConfig config) {
        if (previousMessages.size() > triggerLength) {
            // 生成对话摘要
            String summary = generateSummary(previousMessages);

            // 查找是否已存在 SystemMessage（保留它，不修改）
            SystemMessage existingSystemMessage = null;
            for (Message msg : previousMessages) {
                if (msg instanceof SystemMessage) {
                    existingSystemMessage = (SystemMessage) msg;
                    break;
                }
            }

            // 创建包含摘要的上下文消息（使用 UserMessage 而不是 SystemMessage）
            // 这样可以将摘要作为对话上下文的一部分，而不修改系统提示
            UserMessage summaryContextMessage = new UserMessage(
                "【上下文摘要】之前的对话摘要：" + summary
            );

            // 保留最近的几条消息
            int recentCount = Math.min(5, previousMessages.size());
            List<Message> recentMessages = previousMessages.subList(
                previousMessages.size() - recentCount,
                previousMessages.size()
            );

            // 构建新的消息列表
            List<Message> newMessages = new ArrayList<>();
            
            // 保留原有的 SystemMessage（如果存在）
            if (existingSystemMessage != null) {
                newMessages.add(existingSystemMessage);
            }
            
            // 添加摘要上下文消息
            newMessages.add(summaryContextMessage);
            
            // 添加最近的消息，排除旧的 SystemMessage（如果存在）
            for (Message msg : recentMessages) {
                if (msg != existingSystemMessage) {
                    newMessages.add(msg);
                }
            }

            // 使用 REPLACE 策略替换消息列表
            return new AgentCommand(newMessages, UpdatePolicy.REPLACE);
        }

        // 如果消息数量未超过阈值，返回原始消息（不进行修改）
        return new AgentCommand(previousMessages);
    }

    private String generateSummary(List<Message> messages) {
        // 使用另一个模型生成摘要
        String conversation = messages.stream()
            .map(Message::getText)
            .collect(Collectors.joining("\n"));

        // 简化示例：返回固定摘要
        return "之前讨论了多个主题...";
    }
}`}
</Code>

## 相关文档

- [Hooks](../tutorials/hooks.md) - Hook 机制详解
- [Interceptors](../tutorials/hooks.md) - 拦截器详解
- [Agents](../tutorials/agents.md) - Agent 基础概念
- [Memory](./memory.md) - 状态和记忆管理
