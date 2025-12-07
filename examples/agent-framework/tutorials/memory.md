---
title: Memory 短期记忆
description: 了解如何使用短期记忆让Agent记住先前交互，管理对话历史和线程级持久化
keywords: [短期记忆, Short-term Memory, 对话历史, 线程, Thread, Checkpointer, 会话管理, 上下文窗口]
---

## 概述

记忆可以让 Agent 记住之前的会话内容。对于 AI Agent，记忆至关重要，因为它让它们能够记住先前的交互、从反馈中学习并适应用户偏好。随着 Agent 处理更复杂的任务和大量用户交互，这种能力对于效率和用户满意度都变得至关重要。

短期记忆让你的应用程序能够在单个线程或会话中记住先前的交互。

> **注意**：会话可以隔离同一个 Agent 实例中的多个不同交互，类似于电子邮件在单个对话中分组消息的方式。

## 理解 ReactAgent 中的短期记忆
Spring AI Alibaba 将短期记忆作为 Agent 状态的一部分进行管理。


通过将这些存储在 Graph 的状态中，Agent 可以访问给定对话的完整上下文，同时保持不同对话之间的分离。状态使用 checkpointer 持久化到数据库（或内存），以便可以随时恢复线程。短期记忆在调用 Agent 或完成步骤（如工具调用）时更新，并在每个步骤开始时读取状态。

## 记忆带来的上下文过长问题
保留所有对话历史是实现短期记忆最常见的形式。但较长的对话对历史可能会导致大模型 LLM 上下文窗口超限，导致上下文丢失或报错。

即使你在使用的大模型上下文长度足够大，大多数模型在处理较长上下文时的表现仍然很差。因为很多模型会被过时或偏离主题的内容"分散注意力"。同时，过长的上下文，还会带来响应时间变长、Token 成本增加等问题。

在 Spring AI ALibaba 中，ReactAgent 使用 [messages](./messages.md) 记录和传递上下文，其中包括指令（SystemMessage）和输入（UserMessage）。在 ReactAgent 中，消息（Message）在用户输入和模型响应之间交替，导致消息列表随着时间的推移变得越来越长。由于上下文窗口有限，许多应用程序可以从使用技术来移除或"忘记"过时信息中受益，即 “上下文工程”。


## 使用方法

在 Spring AI Alibaba 中，要向 Agent 添加短期记忆（会话级持久化），你需要在创建 Agent 时指定 `checkpointer`。

<Code
  language="java"
  title="配置短期记忆示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/MemoryExample.java"
>
{`import com.alibaba.cloud.ai.graph.agent.ReactAgent;

import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.RunnableConfig;

// 配置 checkpointer
ReactAgent agent = ReactAgent.builder()
    .name("my_agent")
    .model(chatModel)
    .tools(getUserInfoTool)
    .saver(new MemorySaver())
    .build();

// 使用 thread_id 维护对话上下文
RunnableConfig config = RunnableConfig.builder()
    .threadId("1") // threadId 指定会话 ID
    .build();

agent.call("你好！我叫 Bob。", config);`}
</Code>

### 在生产环境中

在生产环境中，使用数据库支持的 checkpointer：

**示例：使用 Redis Checkpointer**：

<Code
  language="java"
  title="使用 Redis Checkpointer 示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/MemoryExample.java"
>
{`import com.alibaba.cloud.ai.graph.checkpoint.savers.RedisSaver;
import org.redisson.api.RedissonClient;

// 配置 Redis checkpointer
RedisSaver redisSaver = new RedisSaver(redissonClient);

ReactAgent agent = ReactAgent.builder()
    .name("my_agent")
    .model(chatModel)
    .tools(getUserInfoTool)
    .saver(redisSaver)
    .build();`}
</Code>

## 自定义 Agent 记忆

默认情况下，Agent 使用状态通过 `messages` 键管理短期记忆，特别是对话历史。

你可以通过在工具或 Hook 中访问和修改状态来扩展记忆功能。

<Code
  language="java"
  title="自定义记忆 Hook 示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/MemoryExample.java"
>
{`import com.alibaba.cloud.ai.graph.agent.hook.ModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import org.springframework.ai.chat.messages.Message;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

// 在 Hook 中访问和修改状态
public class CustomMemoryHook extends ModelHook {

    @Override
    public String getName() {
        return "custom_memory";
    }

    @Override
    public HookPosition[] getHookPositions() {
        return new HookPosition[]{HookPosition.BEFORE_MODEL};
    }

    @Override
    public CompletableFuture<Map<String, Object>> beforeModel(OverAllState state, RunnableConfig config) {
        // 访问消息历史
        Optional<Object> messagesOpt = state.value("messages");
        if (messagesOpt.isPresent()) {
            List<Message> messages = (List<Message>) messagesOpt.get();
            // 处理消息...
        }

        // 添加自定义状态
        return CompletableFuture.completedFuture(Map.of(
            "user_id", "user_123",
            "preferences", Map.of("theme", "dark")
        ));
    }

    @Override
    public CompletableFuture<Map<String, Object>> afterModel(OverAllState state, RunnableConfig config) {
        return CompletableFuture.completedFuture(Map.of());
    }
}`}
</Code>

## 常见模式

启用[短期记忆](#使用方法)后，长对话可能超过 LLM 的上下文窗口。常见的解决方案包括：

* 修剪消息。在调用 LLM 之前移除前 N 条或后 N 条消息
* 删除消息。从 Graph 状态中永久删除消息
* 总结消息。总结历史中较早的消息并用摘要替换它们
* 自定义策略。自定义策略（例如消息过滤等）

这允许 Agent 在 reasoning-acting 循环中持续跟踪对话而不超过 LLM 的上下文窗口。

### 修剪消息

大多数 LLM 都有最大支持的上下文窗口（以 token 计）。

决定何时截断消息的一种方法是计算消息历史中的 token 数量，并在接近该限制时进行截断。

要在 Agent 中修剪消息历史，请使用 `ModelHook`：

<Code
  language="java"
  title="MessageTrimmingHook 修剪消息示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/MemoryExample.java"
>
{`import com.alibaba.cloud.ai.graph.agent.hook.ModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import org.springframework.ai.chat.messages.Message;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class MessageTrimmingHook extends ModelHook {

    private static final int MAX_MESSAGES = 3;

    @Override
    public String getName() {
        return "message_trimming";
    }

    @Override
    public HookPosition[] getHookPositions() {
        return new HookPosition[]{HookPosition.BEFORE_MODEL};
    }

    @Override
    public CompletableFuture<Map<String, Object>> beforeModel(OverAllState state, RunnableConfig config) {
        Optional<Object> messagesOpt = state.value("messages");
        if (!messagesOpt.isPresent()) {
            return CompletableFuture.completedFuture(Map.of());
        }

        List<Message> messages = (List<Message>) messagesOpt.get();

        if (messages.size() <= MAX_MESSAGES) {
            return CompletableFuture.completedFuture(Map.of()); // 无需更改
        }

        // 保留第一条消息和最后几条消息，并将中间消息标记为删除
        Message firstMsg = messages.get(0);
        int keepCount = messages.size() % 2 == 0 ? 3 : 4;
        List<Message> recentMessages = messages.subList(
            messages.size() - keepCount,
            messages.size()
        );

        List<Object> newMessages = new ArrayList<>();
        // 标记中间消息为删除（使用 RemoveByHash）
        if (messages.size() - keepCount > 1) {
            for (Message msg : messages.subList(1, messages.size() - keepCount)) {
                newMessages.add(com.alibaba.cloud.ai.graph.state.RemoveByHash.of(msg));
            }
        }

        return CompletableFuture.completedFuture(Map.of("messages", newMessages));
    }

    @Override
    public CompletableFuture<Map<String, Object>> afterModel(OverAllState state, RunnableConfig config) {
        return CompletableFuture.completedFuture(Map.of());
    }
}

// 使用
ReactAgent agent = ReactAgent.builder()
    .name("my_agent")
    .model(chatModel)
    .tools(tools)
    .hooks(new MessageTrimmingHook())
    .saver(new MemorySaver())
    .build();

RunnableConfig config = RunnableConfig.builder()
    .threadId("1")
    .build();

agent.call("你好，我叫 bob", config);
agent.call("写一首关于猫的短诗", config);
agent.call("现在对狗做同样的事情", config);
AssistantMessage finalResponse = agent.call("我叫什么名字？", config);

System.out.println(finalResponse.getText());
// 输出：你的名字是 Bob。你之前告诉我的。`}
</Code>

### 删除消息

你可以从 Graph 状态中删除消息以管理消息历史。

这在你想要删除特定消息或清除整个消息历史时很有用。

要从 Graph 状态中删除消息，你可以在 Hook 中返回新的消息列表：

<Code
  language="java"
  title="MessageDeletionHook 删除消息示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/MemoryExample.java"
>
{`import com.alibaba.cloud.ai.graph.agent.hook.ModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.state.RemoveByHash;
import org.springframework.ai.chat.messages.Message;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class MessageDeletionHook extends ModelHook {

    @Override
    public String getName() {
        return "message_deletion";
    }

    @Override
    public HookPosition[] getHookPositions() {
        return new HookPosition[]{HookPosition.AFTER_MODEL};
    }

    @Override
    public CompletableFuture<Map<String, Object>> beforeModel(OverAllState state, RunnableConfig config) {
        return CompletableFuture.completedFuture(Map.of());
    }

    @Override
    public CompletableFuture<Map<String, Object>> afterModel(OverAllState state, RunnableConfig config) {
        Optional<Object> messagesOpt = state.value("messages");
        if (!messagesOpt.isPresent()) {
            return CompletableFuture.completedFuture(Map.of());
        }

        List<Message> messages = (List<Message>) messagesOpt.get();

        if (messages.size() > 2) {
            // 将最早的两条消息转为 RemoveByHash 对象以便从状态中删除
            List<Object> removeOldMessages = new ArrayList<>();
            removeOldMessages.add(RemoveByHash.of(messages.get(0)));
            removeOldMessages.add(RemoveByHash.of(messages.get(1)));
            return CompletableFuture.completedFuture(Map.of("messages", removeOldMessages));
        }

        return CompletableFuture.completedFuture(Map.of());
    }
}`}
</Code>

**删除所有消息**：

<Code
  language="java"
  title="ClearAllMessagesHook 删除所有消息示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/MemoryExample.java"
>
{`import com.alibaba.cloud.ai.graph.state.RemoveByHash;

public class ClearAllMessagesHook extends ModelHook {

    @Override
    public String getName() {
        return "clear_all_messages";
    }

    @Override
    public HookPosition[] getHookPositions() {
        return new HookPosition[]{HookPosition.AFTER_MODEL};
    }

    @Override
    public CompletableFuture<Map<String, Object>> afterModel(OverAllState state, RunnableConfig config) {
        Optional<Object> messagesOpt = state.value("messages");
        if (!messagesOpt.isPresent()) {
            return CompletableFuture.completedFuture(Map.of());
        }

        List<Message> messages = (List<Message>) messagesOpt.get();

        // 将所有消息转为 RemoveByHash 对象以便从状态中删除
        List<Object> removeAllMessages = new ArrayList<>();
        for (Message msg : messages) {
            removeAllMessages.add(RemoveByHash.of(msg));
        }

        return CompletableFuture.completedFuture(Map.of("messages", removeAllMessages));
    }
}`}
</Code>

**警告**：删除消息时，**确保**生成的消息历史有效。检查你使用的 LLM 提供商的限制。例如：

* 某些提供商期望消息历史以 `user` 消息开始
* 大多数提供商要求带有工具调用的 `assistant` 消息后跟相应的 `tool` 结果消息

<Code
  language="java"
  title="DeleteOldMessagesHook 删除旧消息示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/MemoryExample.java"
>
{`import com.alibaba.cloud.ai.graph.agent.hook.ModelHook;
import com.alibaba.cloud.ai.graph.state.RemoveByHash;

public class DeleteOldMessagesHook extends ModelHook {

    @Override
    public String getName() {
        return "delete_old_messages";
    }

    @Override
    public HookPosition[] getHookPositions() {
        return new HookPosition[]{HookPosition.AFTER_MODEL};
    }

    @Override
    public CompletableFuture<Map<String, Object>> afterModel(OverAllState state, RunnableConfig config) {
        Optional<Object> messagesOpt = state.value("messages");
        if (!messagesOpt.isPresent()) {
            return CompletableFuture.completedFuture(Map.of());
        }

        List<Message> messages = (List<Message>) messagesOpt.get();
        if (messages.size() > 2) {
            // 将最早的两条消息转为 RemoveByHash 对象以便从状态中删除
            List<Object> removeOldMessages = new ArrayList<>();
            removeOldMessages.add(RemoveByHash.of(messages.get(0)));
            removeOldMessages.add(RemoveByHash.of(messages.get(1)));
            return CompletableFuture.completedFuture(Map.of("messages", removeOldMessages));
        }

        return CompletableFuture.completedFuture(Map.of());
    }
}

ReactAgent agent = ReactAgent.builder()
    .name("my_agent")
    .model(chatModel)
    .systemPrompt("请简洁明了。")
    .hooks(new DeleteOldMessagesHook())
    .saver(new MemorySaver())
    .build();

RunnableConfig config = RunnableConfig.builder()
    .threadId("1")
    .build();

// 第一次调用
agent.call("你好！我是 bob", config);
// 输出：[('human', "你好！我是 bob"), ('assistant', '你好 Bob！很高兴见到你...')]

// 第二次调用
agent.call("我叫什么名字？", config);
// 输出：[('human', "我叫什么名字？"), ('assistant', '你的名字是 Bob...')]`}
</Code>

### 总结消息

如上所示，修剪或删除消息的问题在于你可能会丢失消息队列淘汰的信息。因此，一些应用程序受益于使用聊天模型总结消息历史的更复杂方法。

要在 Agent 中总结消息历史，可以使用自定义 Hook：

<Code
  language="java"
  title="MessageSummarizationHook 总结消息示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/MemoryExample.java"
>
{`import com.alibaba.cloud.ai.graph.agent.hook.ModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.state.RemoveByHash;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class MessageSummarizationHook extends ModelHook {

    private final ChatModel summaryModel;
    private final int maxTokensBeforeSummary;
    private final int messagesToKeep;

    public MessageSummarizationHook(
        ChatModel summaryModel,
        int maxTokensBeforeSummary,
        int messagesToKeep
    ) {
        this.summaryModel = summaryModel;
        this.maxTokensBeforeSummary = maxTokensBeforeSummary;
        this.messagesToKeep = messagesToKeep;
    }

    @Override
    public String getName() {
        return "message_summarization";
    }

    @Override
    public HookPosition[] getHookPositions() {
        return new HookPosition[]{HookPosition.BEFORE_MODEL};
    }

    @Override
    public CompletableFuture<Map<String, Object>> beforeModel(OverAllState state, RunnableConfig config) {
        Optional<Object> messagesOpt = state.value("messages");
        if (!messagesOpt.isPresent()) {
            return CompletableFuture.completedFuture(Map.of());
        }

        List<Message> messages = (List<Message>) messagesOpt.get();

        // 估算 token 数量（简化版）
        int estimatedTokens = messages.stream()
            .mapToInt(m -> m.getText().length() / 4)
            .sum();

        if (estimatedTokens < maxTokensBeforeSummary) {
            return CompletableFuture.completedFuture(Map.of());
        }

        // 需要总结
        int messagesToSummarize = messages.size() - messagesToKeep;
        if (messagesToSummarize <= 0) {
            return CompletableFuture.completedFuture(Map.of());
        }

        List<Message> oldMessages = messages.subList(0, messagesToSummarize);

        // 生成摘要
        String summary = generateSummary(oldMessages);

        // 创建摘要消息
        SystemMessage summaryMessage = new SystemMessage(
            "## 之前对话摘要:\n" + summary
        );

        // 只需要把摘要消息和需要删除的消息保留在状态中，其余未包含的消息将会自动保留
        List<Object> newMessages = new ArrayList<>();
        newMessages.add(summaryMessage);
        // IMPORTANT! Convert summarized messages to RemoveByHash objects so we can remove them from state
        for (Message msg : oldMessages) {
            newMessages.add(RemoveByHash.of(msg));
        }

        return CompletableFuture.completedFuture(Map.of("messages", newMessages));
    }

    private String generateSummary(List<Message> messages) {
        StringBuilder conversation = new StringBuilder();
        for (Message msg : messages) {
            conversation.append(msg.getMessageType())
                      .append(": ")
                      .append(msg.getText())
                      .append("\n");
        }

        String summaryPrompt = "请简要总结以下对话:\n\n" + conversation;

        ChatResponse response = summaryModel.call(
            new Prompt(new UserMessage(summaryPrompt))
        );

        return response.getResult().getOutput().getText();
    }

    @Override
    public CompletableFuture<Map<String, Object>> afterModel(OverAllState state, RunnableConfig config) {
        return CompletableFuture.completedFuture(Map.of());
    }
}

// 使用
ChatModel summaryModel = // ... 用于总结的模型（可以是更便宜的模型）

MessageSummarizationHook summarizationHook = new MessageSummarizationHook(
    summaryModel,
    4000,  // 在 4000 tokens 时触发总结
    20     // 总结后保留最后 20 条消息
);

ReactAgent agent = ReactAgent.builder()
    .name("my_agent")
    .model(chatModel)
    .hooks(summarizationHook)
    .saver(new MemorySaver())
    .build();

RunnableConfig config = RunnableConfig.builder()
    .threadId("1")
    .build();

agent.call("你好，我叫 bob", config);
agent.call("写一首关于猫的短诗", config);
agent.call("现在对狗做同样的事情", config);
AssistantMessage finalResponse = agent.call("我叫什么名字？", config);

System.out.println(finalResponse.getText());
// 输出：你的名字是 Bob！`}
</Code>

## 访问记忆

你可以通过多种方式访问和修改 Agent 的短期记忆（状态）：

### 工具

#### 在工具中读取短期记忆

使用 `ToolContext` 参数在工具中访问短期记忆（状态）。

`toolContext` 参数从工具签名中隐藏（因此模型看不到它），但工具可以通过它访问状态。

<Code
  language="java"
  title="在工具中读取短期记忆示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/MemoryExample.java"
>
{`import com.alibaba.cloud.ai.graph.RunnableConfig;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.messages.AssistantMessage;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import java.util.function.BiFunction;

public class UserInfoTool implements BiFunction<String, ToolContext, String> {

    @Override
    public String apply(String query, ToolContext toolContext) {
        // 从上下文中获取用户信息
        RunnableConfig config = (RunnableConfig) toolContext.getContext().get("config");
        String userId = (String) config.metadata("user_id").orElse("");

        if ("user_123".equals(userId)) {
            return "用户是 John Smith";
        } else {
            return "未知用户";
        }
    }
}

// 创建工具
ToolCallback getUserInfoTool = FunctionToolCallback
    .builder("get_user_info", new UserInfoTool())
    .description("查找用户信息")
    .inputType(String.class)
    .build();

// 使用
ReactAgent agent = ReactAgent.builder()
    .name("my_agent")
    .model(chatModel)
    .tools(getUserInfoTool)
    .saver(new MemorySaver())
    .build();

RunnableConfig config = RunnableConfig.builder()
    .threadId("1")
    .addMetadata("user_id", "user_123")
    .build();

AssistantMessage response = agent.call("获取用户信息", config);
System.out.println(response.getText());`}
</Code>

#### 从工具写入短期记忆

要在执行期间修改 Agent 的短期记忆（状态），你可以在 Hook 中更新状态，或者使用工具返回的信息更新状态。

这对于持久化中间结果或使信息对后续工具或提示可访问很有用。

### 提示

在 Hook 中访问短期记忆（状态）以基于对话历史或自定义状态字段创建动态提示。

<Code
  language="java"
  title="DynamicPromptInterceptor 动态提示示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/MemoryExample.java"
>
{`import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;

public class DynamicPromptInterceptor extends ModelInterceptor {

    @Override
    public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
        // 从上下文中获取用户名
        Map<String, Object> context = request.getContext();
        String userName = (String) context.get("user_name");

        // 创建动态系统提示
        String systemPrompt = "你是一个有帮助的助手。称呼用户为 " + userName + "。";

        // 创建修改后的请求（示例），实际使用中需要根据具体 API 进行调整
		SystemMessage enhancedSystemMessage;
		if (request.getSystemMessage() == null) {
			enhancedSystemMessage = new SystemMessage(systemPrompt);
		} else {
			enhancedSystemMessage = new SystemMessage(request.getSystemMessage().getText() + "\n\n" + systemPrompt);
		}

		// Create enhanced request
		ModelRequest enhancedRequest = ModelRequest.builder(request)
				.systemMessage(enhancedSystemMessage)
				.build();

        return handler.call(enhancedRequest);
    }

    @Override
    public String getName() {
        return "DynamicPromptInterceptor";
    }
}

ReactAgent agent = ReactAgent.builder()
    .name("my_agent")
    .model(chatModel)
    .tools(getWeatherTool)
    .interceptors(new DynamicPromptInterceptor())
    .build();

// 使用时传递上下文
Map<String, Object> context = Map.of("user_name", "John Smith");`}
</Code>

### Before Model

在 `beforeModel` Hook 中访问短期记忆（状态）以在模型调用之前处理消息。

<Code
  language="java"
  title="TrimMessagesHook Before Model 示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/MemoryExample.java"
>
{`import com.alibaba.cloud.ai.graph.agent.hook.ModelHook;

public class TrimMessagesHook extends ModelHook {

    @Override
    public String getName() {
        return "trim_messages";
    }

    @Override
    public HookPosition[] getHookPositions() {
        return new HookPosition[]{HookPosition.BEFORE_MODEL};
    }

    @Override
    public CompletableFuture<Map<String, Object>> beforeModel(OverAllState state, RunnableConfig config) {
        // 访问和修改消息
        Optional<Object> messagesOpt = state.value("messages");
        if (messagesOpt.isPresent()) {
            List<Message> messages = (List<Message>) messagesOpt.get();

            if (messages.size() <= 3) {
                return CompletableFuture.completedFuture(Map.of()); // 无需更改
            }

            // 保留第一条和最后几条消息，并将中间消息标记为删除
            Message firstMsg = messages.get(0);
            List<Message> recentMessages = messages.subList(
                messages.size() - 3,
                messages.size()
            );

            List<Object> newMessages = new ArrayList<>();
            newMessages.add(firstMsg);
            newMessages.addAll(recentMessages);
            // 标记中间消息为删除（使用 RemoveByHash）
            if (messages.size() - 3 > 1) {
                for (Message msg : messages.subList(1, messages.size() - 3)) {
                    newMessages.add(com.alibaba.cloud.ai.graph.state.RemoveByHash.of(msg));
                }
            }

            return CompletableFuture.completedFuture(Map.of("messages", newMessages));
        }

        return CompletableFuture.completedFuture(Map.of());
    }

    @Override
    public CompletableFuture<Map<String, Object>> afterModel(OverAllState state, RunnableConfig config) {
        return CompletableFuture.completedFuture(Map.of());
    }
}

ReactAgent agent = ReactAgent.builder()
    .name("my_agent")
    .model(chatModel)
    .tools(tools)
    .hooks(new TrimMessagesHook())
    .saver(new MemorySaver())
    .build();`}
</Code>

### After Model

在 `afterModel` Hook 中访问短期记忆（状态）以在模型调用之后处理消息。

<Code
  language="java"
  title="ValidateResponseHook After Model 示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/MemoryExample.java"
>
{`import com.alibaba.cloud.ai.graph.agent.hook.ModelHook;

public class ValidateResponseHook extends ModelHook {

    private static final List<String> STOP_WORDS =
        List.of("password", "secret", "api_key");

    @Override
    public String getName() {
        return "validate_response";
    }

    @Override
    public HookPosition[] getHookPositions() {
        return new HookPosition[]{HookPosition.AFTER_MODEL};
    }

    @Override
    public CompletableFuture<Map<String, Object>> beforeModel(OverAllState state, RunnableConfig config) {
        return CompletableFuture.completedFuture(Map.of());
    }

    @Override
    public CompletableFuture<Map<String, Object>> afterModel(OverAllState state, RunnableConfig config) {
        Optional<Object> messagesOpt = state.value("messages");
        if (!messagesOpt.isPresent()) {
            return CompletableFuture.completedFuture(Map.of());
        }

        List<Message> messages = (List<Message>) messagesOpt.get();
        if (messages.isEmpty()) {
            return CompletableFuture.completedFuture(Map.of());
        }

        Message lastMessage = messages.get(messages.size() - 1);
        String content = lastMessage.getText();

        // 检查是否包含敏感词
        for (String stopWord : STOP_WORDS) {
            if (content.toLowerCase().contains(stopWord)) {
                // 移除包含敏感词的消息
                List<Message> filtered = messages.subList(0, messages.size() - 1);
                filtered.add(new AssistantMessage(
                    "抱歉，我无法提供该信息。"
                ));
                return CompletableFuture.completedFuture(Map.of("messages", filtered));
            }
        }

        return CompletableFuture.completedFuture(Map.of());
    }
}

ReactAgent agent = ReactAgent.builder()
    .name("secure_agent")
    .model(chatModel)
    .hooks(new ValidateResponseHook())
    .saver(new MemorySaver())
    .build();`}
</Code>

## 相关资源

* [Agents 文档](./agents.md) - 了解 ReactAgent 的核心概念
* [Hooks 和 Interceptors](./hooks.md) - 了解如何扩展 Agent 功能
* [Messages 文档](./messages.md) - 了解消息类型和使用
