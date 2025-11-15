---
title: 记忆管理（Memory）
description: 了解如何在Agent中使用记忆管理功能，实现对话历史存储、检查点机制和状态持久化
keywords: [记忆管理, Memory, 检查点, Checkpoint, 状态持久化, 对话历史, MemorySaver, 会话管理]
---

## 概述

Spring AI Alibaba 的 Agent 使用持久化机制来实现长期记忆功能。这是一个高级主题，需要对 Spring AI Alibaba 的 Agent 框架有一定了解。

```
┌─────────────────────────────────────────────────────────┐
│                     ReactAgent                          │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  ┌──────────────────┐        ┌────────────────────┐   │
│  │  短期记忆         │        │   长期记忆          │   │
│  │  Short-term      │        │   Long-term        │   │
│  │  (MemorySaver)   │        │   (MemoryStore)    │   │
│  └──────────────────┘        └────────────────────┘   │
│         │                             │                │
│         │ threadId                    │ namespace/key  │
│         ↓                             ↓                │
│  ┌──────────────────┐        ┌────────────────────┐   │
│  │  对话历史         │        │  用户画像          │   │
│  │  Conversation    │        │  User Profiles     │   │
│  │  History         │        │  Preferences       │   │
│  │  Message State   │        │  Persistent Data   │   │
│  └──────────────────┘        └────────────────────┘   │
│                                                         │
│  ┌─────────────────────────────────────────────────┐  │
│  │            ModelHook / 拦截器                    │  │
│  │  - beforeModel: 加载并注入记忆                   │  │
│  │  - afterModel: 保存并学习交互内容                │  │
│  └─────────────────────────────────────────────────┘  │
│                                                         │
│  ┌─────────────────────────────────────────────────┐  │
│  │            记忆工具 Tools                        │  │
│  │  - saveMemory: 显式保存到长期记忆                │  │
│  │  - getMemory: 显式从长期记忆检索                 │  │
│  └─────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

## 记忆存储

Spring AI Alibaba 将长期记忆以 JSON 文档的形式存储在 Store 中。

每个记忆都在自定义的 `namespace`（类似于文件夹）下组织，并使用唯一的 `key`（类似于文件名）。命名空间通常包含用户或组织 ID 或其他标签，以便更容易地组织信息。

这种结构支持记忆的层次化组织。通过内容过滤器支持跨命名空间搜索。

```java
import com.alibaba.cloud.ai.graph.store.stores.MemoryStore;
import com.alibaba.cloud.ai.graph.store.StoreItem;

import java.util.*;

// MemoryStore 将数据保存到内存字典中。在生产环境中请使用基于数据库的存储实现
MemoryStore store = new MemoryStore();

String userId = "my-user";
String applicationContext = "chitchat";
List<String> namespace = List.of(userId, applicationContext);

// 保存记忆
Map<String, Object> memoryData = new HashMap<>();
memoryData.put("rules", List.of(
    "用户喜欢简短直接的语言",
    "用户只说中文和Java"
));
memoryData.put("my-key", "my-value");

StoreItem item = StoreItem.of(namespace, "a-memory", memoryData);
store.putItem(item);

// 通过ID获取记忆
Optional<StoreItem> retrievedItem = store.getItem(namespace, "a-memory");

// 在此命名空间内搜索记忆，通过内容等价性过滤，按向量相似度排序
List<StoreItem> items = store.searchItems(
    namespace,
    Map.of("my-key", "my-value")
);
```

## 在工具中读取长期记忆

下面的示例展示了如何创建一个工具，让 Agent 能够查询用户信息。

```java
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.store.stores.MemoryStore;
import com.alibaba.cloud.ai.graph.store.StoreItem;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.*;
import java.util.function.BiFunction;

// 定义请求和响应记录
public record GetMemoryRequest(List<String> namespace, String key) {}
public record MemoryResponse(String message, Map<String, Object> value) {}

// 创建内存存储
MemoryStore store = new MemoryStore();

// 向存储中写入示例数据
Map<String, Object> userData = new HashMap<>();
userData.put("name", "张三");
userData.put("language", "中文");

StoreItem userItem = StoreItem.of(List.of("users"), "user_123", userData);
store.putItem(userItem);

// 创建获取用户信息的工具
BiFunction<GetMemoryRequest, ToolContext, MemoryResponse> getUserInfoFunction =
    (request, context) -> {
        Optional<StoreItem> itemOpt = store.getItem(request.namespace(), request.key());
        if (itemOpt.isPresent()) {
            Map<String, Object> value = itemOpt.get().getValue();
            return new MemoryResponse("找到用户信息", value);
        }
        return new MemoryResponse("未找到用户", Map.of());
    };

ToolCallback getUserInfoTool = FunctionToolCallback.builder("getUserInfo", getUserInfoFunction)
    .description("查询用户信息")
    .inputType(GetMemoryRequest.class)
    .build();

// 创建Agent
ReactAgent agent = ReactAgent.builder()
    .name("memory_agent")
    .model(chatModel)
    .tools(getUserInfoTool)
    .saver(new MemorySaver())
    .build();

// 运行Agent
RunnableConfig config = RunnableConfig.builder()
    .threadId("session_001")
    .addMetadata("user_id", "user_123")
    .build();

agent.invoke("查询用户信息，namespace=['users'], key='user_123'", config);
```

## 在工具中写入长期记忆

下面的示例展示了如何创建一个更新用户信息的工具。

```java
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.store.stores.MemoryStore;
import com.alibaba.cloud.ai.graph.store.StoreItem;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.*;
import java.util.function.BiFunction;

// 定义请求记录
public record SaveMemoryRequest(List<String> namespace, String key, Map<String, Object> value) {}
public record MemoryResponse(String message, Map<String, Object> value) {}

// 创建内存存储
MemoryStore store = new MemoryStore();

// 创建保存用户信息的工具
BiFunction<SaveMemoryRequest, ToolContext, MemoryResponse> saveUserInfoFunction =
    (request, context) -> {
        StoreItem item = StoreItem.of(request.namespace(), request.key(), request.value());
        store.putItem(item);
        return new MemoryResponse("成功保存用户信息", request.value());
    };

ToolCallback saveUserInfoTool = FunctionToolCallback.builder("saveUserInfo", saveUserInfoFunction)
    .description("保存用户信息")
    .inputType(SaveMemoryRequest.class)
    .build();

// 创建Agent
ReactAgent agent = ReactAgent.builder()
    .name("save_memory_agent")
    .model(chatModel)
    .tools(saveUserInfoTool)
    .saver(new MemorySaver())
    .build();

// 运行Agent
RunnableConfig config = RunnableConfig.builder()
    .threadId("session_001")
    .addMetadata("user_id", "user_123")
    .build();

agent.invoke(
    "我叫张三，请保存我的信息。使用 saveUserInfo 工具，namespace=['users'], key='user_123', value={'name': '张三'}",
    config
);

// 可以直接访问存储获取值
Optional<StoreItem> savedItem = store.getItem(List.of("users"), "user_123");
Map<String, Object> savedValue = savedItem.get().getValue();
```

## 使用 ModelHook 管理长期记忆

下面的示例展示了如何使用 ModelHook 在模型调用前后自动加载和保存长期记忆。

```java
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.ModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.JumpTo;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.store.stores.MemoryStore;
import com.alibaba.cloud.ai.graph.store.StoreItem;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;

import java.util.*;
import java.util.concurrent.CompletableFuture;

// 创建内存存储
MemoryStore memoryStore = new MemoryStore();

// 预先填充用户画像
Map<String, Object> profileData = new HashMap<>();
profileData.put("name", "王小明");
profileData.put("age", 28);
profileData.put("email", "wang@example.com");
profileData.put("preferences", List.of("喜欢咖啡", "喜欢阅读"));

StoreItem profileItem = StoreItem.of(List.of("user_profiles"), "user_001", profileData);
memoryStore.putItem(profileItem);

// 创建记忆拦截器
ModelHook memoryInterceptor = new ModelHook() {
    @Override
    public String getName() {
        return "memory_interceptor";
    }

    @Override
    public List<JumpTo> canJumpTo() {
        return List.of();
    }

    @Override
    public HookPosition[] getHookPositions() {
        return new HookPosition[]{HookPosition.BEFORE_MODEL, HookPosition.AFTER_MODEL};
    }

    @Override
    public CompletableFuture<Map<String, Object>> beforeModel(OverAllState state, RunnableConfig config) {
        // 从配置中获取用户ID
        String userId = (String) config.metadata("user_id").orElse(null);
        if (userId == null) {
            return CompletableFuture.completedFuture(Map.of());
        }

        // 从记忆存储中加载用户画像
        Optional<StoreItem> itemOpt = memoryStore.getItem(List.of("user_profiles"), userId);
        if (itemOpt.isPresent()) {
            Map<String, Object> profileData = itemOpt.get().getValue();

            // 将用户上下文注入系统消息
            String userContext = String.format(
                "用户信息：姓名=%s, 年龄=%s, 邮箱=%s, 偏好=%s",
                profileData.get("name"),
                profileData.get("age"),
                profileData.get("email"),
                profileData.get("preferences")
            );

            // 添加包含用户上下文的系统消息
            List<Message> messages = (List<Message>) state.value("messages").orElse(new ArrayList<>());
            List<Message> newMessages = new ArrayList<>();
            newMessages.add(new SystemMessage(userContext));
            newMessages.addAll(messages);

            return CompletableFuture.completedFuture(Map.of("messages", newMessages));
        }

        return CompletableFuture.completedFuture(Map.of());
    }

    @Override
    public CompletableFuture<Map<String, Object>> afterModel(OverAllState state, RunnableConfig config) {
        // 可以在这里实现对话后的记忆保存逻辑
        return CompletableFuture.completedFuture(Map.of());
    }
};

// 创建带有记忆拦截器的Agent
ReactAgent agent = ReactAgent.builder()
    .name("memory_agent")
    .model(chatModel)
    .hooks(memoryInterceptor)
    .saver(new MemorySaver())
    .build();

RunnableConfig config = RunnableConfig.builder()
    .threadId("session_001")
    .addMetadata("user_id", "user_001")
    .build();

// Agent会自动加载用户画像信息
agent.invoke("请介绍一下我的信息。", config);
```

## 结合短期和长期记忆

短期记忆用于存储对话上下文，长期记忆用于存储持久化数据。下面的示例展示了如何同时使用两种记忆。

```java
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.ModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.store.stores.MemoryStore;
import com.alibaba.cloud.ai.graph.store.StoreItem;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;

import java.util.*;
import java.util.concurrent.CompletableFuture;

// 创建记忆存储
MemoryStore memoryStore = new MemoryStore();

// 设置长期记忆
Map<String, Object> userProfile = new HashMap<>();
userProfile.put("name", "李工程师");
userProfile.put("occupation", "软件工程师");

StoreItem profileItem = StoreItem.of(List.of("profiles"), "user_002", userProfile);
memoryStore.putItem(profileItem);

// 创建组合记忆Hook
ModelHook combinedMemoryHook = new ModelHook() {
    @Override
    public String getName() {
        return "combined_memory";
    }

    @Override
    public List<JumpTo> canJumpTo() {
        return List.of();
    }

    @Override
    public HookPosition[] getHookPositions() {
        return new HookPosition[]{HookPosition.BEFORE_MODEL};
    }

    @Override
    public CompletableFuture<Map<String, Object>> beforeModel(OverAllState state, RunnableConfig config) {
        Optional<Object> userIdOpt = config.metadata("user_id");
        if (userIdOpt.isEmpty()) {
            return CompletableFuture.completedFuture(Map.of());
        }
        String userId = (String) userIdOpt.get();

        // 从长期记忆加载
        Optional<StoreItem> profileOpt = memoryStore.getItem(List.of("profiles"), userId);
        if (profileOpt.isEmpty()) {
            return CompletableFuture.completedFuture(Map.of());
        }

        Map<String, Object> profile = profileOpt.get().getValue();
        String contextInfo = String.format("长期记忆：用户 %s, 职业: %s",
            profile.get("name"), profile.get("occupation"));

        // 注入到消息中
        List<Message> messages = (List<Message>) state.value("messages").orElse(new ArrayList<>());
        List<Message> newMessages = new ArrayList<>();
        newMessages.add(new SystemMessage(contextInfo));
        newMessages.addAll(messages);

        return CompletableFuture.completedFuture(Map.of("messages", newMessages));
    }
};

// 创建Agent
ReactAgent agent = ReactAgent.builder()
    .name("combined_memory_agent")
    .model(chatModel)
    .hooks(combinedMemoryHook)
    .saver(new MemorySaver()) // 短期记忆
    .build();

RunnableConfig config = RunnableConfig.builder()
    .threadId("combined_thread")
    .addMetadata("user_id", "user_002")
    .build();

// 短期记忆：在对话中记住
agent.invoke("我今天在做一个 Spring 项目。", config);

// 提出需要同时使用两种记忆的问题
agent.invoke("根据我的职业和今天的工作，给我一些建议。", config);
// 响应会同时使用长期记忆（职业）和短期记忆（Spring项目）
```

## 跨会话记忆

同一用户在不同会话中应该能够访问相同的长期记忆。

```java
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.store.stores.MemoryStore;
import com.alibaba.cloud.ai.graph.store.StoreItem;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.*;

// 创建记忆存储和工具
MemoryStore memoryStore = new MemoryStore();

ToolCallback saveMemoryTool = FunctionToolCallback.builder("saveMemory",
    (SaveMemoryRequest request, context) -> {
        StoreItem item = StoreItem.of(request.namespace(), request.key(), request.value());
        memoryStore.putItem(item);
        return new MemoryResponse("已保存", request.value());
    })
    .description("保存到长期记忆")
    .inputType(SaveMemoryRequest.class)
    .build();

ToolCallback getMemoryTool = FunctionToolCallback.builder("getMemory",
    (GetMemoryRequest request, context) -> {
        Optional<StoreItem> itemOpt = memoryStore.getItem(request.namespace(), request.key());
        return new MemoryResponse(
            itemOpt.isPresent() ? "找到" : "未找到",
            itemOpt.map(StoreItem::getValue).orElse(Map.of())
        );
    })
    .description("从长期记忆获取")
    .inputType(GetMemoryRequest.class)
    .build();

ReactAgent agent = ReactAgent.builder()
    .name("session_agent")
    .model(chatModel)
    .tools(saveMemoryTool, getMemoryTool)
    .saver(new MemorySaver())
    .build();

// 会话1：保存信息
RunnableConfig session1 = RunnableConfig.builder()
    .threadId("session_morning")
    .addMetadata("user_id", "user_003")
    .build();

agent.invoke(
    "记住我的密码是 secret123。用 saveMemory 保存，namespace=['credentials'], key='user_003_password', value={'password': 'secret123'}。",
    session1
);

// 会话2：检索信息（不同的线程，同一用户）
RunnableConfig session2 = RunnableConfig.builder()
    .threadId("session_afternoon")
    .addMetadata("user_id", "user_003")
    .build();

agent.invoke(
    "我的密码是什么？用 getMemory 获取，namespace=['credentials'], key='user_003_password'。",
    session2
);
// 长期记忆在不同会话间持久化
```

## 用户偏好学习

Agent 可以随着时间的推移学习并存储用户偏好。

```java
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.ModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.store.stores.MemoryStore;
import com.alibaba.cloud.ai.graph.store.StoreItem;
import org.springframework.ai.chat.messages.Message;

import java.util.*;
import java.util.concurrent.CompletableFuture;

MemoryStore memoryStore = new MemoryStore();

ModelHook preferenceLearningHook = new ModelHook() {
    @Override
    public String getName() {
        return "preference_learning";
    }

    @Override
    public List<JumpTo> canJumpTo() {
        return List.of();
    }

    @Override
    public HookPosition[] getHookPositions() {
        return new HookPosition[]{HookPosition.AFTER_MODEL};
    }

    @Override
    public CompletableFuture<Map<String, Object>> afterModel(OverAllState state, RunnableConfig config) {
        String userId = (String) config.metadata("user_id").orElse(null);
        if (userId == null) {
            return CompletableFuture.completedFuture(Map.of());
        }

        // 提取用户输入
        List<Message> messages = (List<Message>) state.value("messages").orElse(new ArrayList<>());
        if (messages.isEmpty()) {
            return CompletableFuture.completedFuture(Map.of());
        }

        // 加载现有偏好
        Optional<StoreItem> prefsOpt = memoryStore.getItem(List.of("user_data"), userId + "_preferences");
        List<String> prefs = new ArrayList<>();
        if (prefsOpt.isPresent()) {
            Map<String, Object> prefsData = prefsOpt.get().getValue();
            prefs = (List<String>) prefsData.getOrDefault("items", new ArrayList<>());
        }

        // 简单的偏好提取（实际应用中使用NLP）
        for (Message msg : messages) {
            String content = msg.getText().toLowerCase();
            if (content.contains("喜欢") || content.contains("偏好")) {
                prefs.add(msg.getText());

                Map<String, Object> prefsData = new HashMap<>();
                prefsData.put("items", prefs);
                StoreItem item = StoreItem.of(List.of("user_data"), userId + "_preferences", prefsData);
                memoryStore.putItem(item);

                System.out.println("学习到用户偏好 " + userId + ": " + msg.getText());
            }
        }

        return CompletableFuture.completedFuture(Map.of());
    }
};

ReactAgent agent = ReactAgent.builder()
    .name("learning_agent")
    .model(chatModel)
    .hooks(preferenceLearningHook)
    .saver(new MemorySaver())
    .build();

RunnableConfig config = RunnableConfig.builder()
    .threadId("learning_thread")
    .addMetadata("user_id", "user_004")
    .build();

// 用户表达偏好
agent.invoke("我喜欢喝绿茶。", config);
agent.invoke("我偏好早上运动。", config);

// 验证偏好已被存储
Optional<StoreItem> savedPrefs = memoryStore.getItem(List.of("user_data"), "user_004_preferences");
// 偏好应该被保存到长期记忆中
```

