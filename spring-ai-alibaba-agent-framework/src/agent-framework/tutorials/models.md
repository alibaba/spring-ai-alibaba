---
title: Models 模型
description: 学习使用Chat Model API与各种AI模型交互，实现聊天补全功能和自然语言处理
keywords: [Chat Model, ChatModel API, GPT, 语言模型, Prompt, ChatResponse, AI模型集成]
---

## ChatModel API

ChatModel API 为开发者提供了将 AI 驱动的聊天补全功能集成到应用程序中的能力。它利用预训练的语言模型（如 GPT），根据用户的自然语言输入生成类似人类的响应。

该 API 通常通过向 AI 模型发送提示或部分对话来工作，然后模型根据其训练数据和对自然语言模式的理解生成对话的完成或延续。完成的响应随后返回给应用程序，应用程序可以将其呈现给用户或用于进一步处理。

`Spring AI ChatModel API` 被设计为一个简单且可移植的接口，用于与各种 AI 模型交互，允许开发者在不同模型之间切换时只需最少的代码更改。

借助 `Prompt`（用于输入封装）和 `ChatResponse`（用于输出处理）等配套类，ChatModel API 统一了与 AI 模型的通信。它管理请求准备和响应解析的复杂性，提供直接且简化的 API 交互。

## API 概述

本节提供 Spring AI ChatModel API 接口和相关类的指南。

### ChatModel

以下是 [ChatModel](https://github.com/spring-projects/spring-ai/blob/main/spring-ai-model/src/main/java/org/springframework/ai/chat/model/ChatModel.java) 接口定义：

```java
public interface ChatModel extends Model<Prompt, ChatResponse>, StreamingChatModel {

    default String call(String message) {...}

    @Override
    ChatResponse call(Prompt prompt);
}
```

带有 `String` 参数的 `call()` 方法简化了初始使用，避免了更复杂的 `Prompt` 和 `ChatResponse` 类的复杂性。在实际应用中，更常见的是使用接受 `Prompt` 实例并返回 `ChatResponse` 的 `call()` 方法。

### StreamingChatModel

以下是 [StreamingChatModel](https://github.com/spring-projects/spring-ai/blob/main/spring-ai-model/src/main/java/org/springframework/ai/chat/model/StreamingChatModel.java) 接口定义：

```java
public interface StreamingChatModel extends StreamingModel<Prompt, ChatResponse> {

    default Flux<String> stream(String message) {...}

    @Override
    Flux<ChatResponse> stream(Prompt prompt);
}
```

`stream()` 方法接受 `String` 或 `Prompt` 参数，类似于 `ChatModel`，但使用响应式 Flux API 流式传输响应。

### Prompt

[Prompt](https://github.com/spring-projects/spring-ai/blob/main/spring-ai-client-chat/src/main/java/org/springframework/ai/chat/prompt/Prompt.java) 是一个封装了 [Message](https://github.com/spring-projects/spring-ai/blob/main/spring-ai-model/src/main/java/org/springframework/ai/chat/messages/Message.java) 对象列表和可选模型请求选项的 `ModelRequest`。以下是 `Prompt` 类的简化版本，排除了构造函数和其他实用方法：

```java
public class Prompt implements ModelRequest<List<Message>> {

    private final List<Message> messages;

    private ChatOptions modelOptions;

    @Override
    public ChatOptions getOptions() {...}

    @Override
    public List<Message> getInstructions() {...}

    // 构造函数和实用方法省略
}
```

#### Message

`Message` 接口封装了 `Prompt` 文本内容、元数据属性集合以及称为 `MessageType` 的分类。

接口定义如下：

```java
public interface Content {

    String getText();

    Map<String, Object> getMetadata();
}

public interface Message extends Content {

    MessageType getMessageType();
}
```

多模态消息类型还实现了 `MediaContent` 接口，提供 `Media` 内容对象列表。

```java
public interface MediaContent extends Content {

    Collection<Media> getMedia();
}
```

`Message` 接口有多种实现，对应于 AI 模型可以处理的消息类别：

- **UserMessage**: 用户消息
- **SystemMessage**: 系统消息
- **AssistantMessage**: 助手消息
- **FunctionMessage**: 函数消息
- **ToolResponseMessage**: 工具响应消息

聊天完成端点根据对话角色区分消息类别，由 `MessageType` 有效映射。

例如，OpenAI 识别不同对话角色的消息类别，如 `system`、`user`、`function` 或 `assistant`。

虽然术语 `MessageType` 可能暗示特定的消息格式，但在此上下文中，它有效地指定了消息在对话中扮演的角色。

对于不使用特定角色的 AI 模型，`UserMessage` 实现充当标准类别，通常表示用户生成的查询或指令。

#### ChatOptions

表示可以传递给 AI 模型的选项。`ChatOptions` 类是 `ModelOptions` 的子类，用于定义可以传递给 AI 模型的少数可移植选项。`ChatOptions` 类定义如下：

```java
public interface ChatOptions extends ModelOptions {

    String getModel();
    Float getFrequencyPenalty();
    Integer getMaxTokens();
    Float getPresencePenalty();
    List<String> getStopSequences();
    Float getTemperature();
    Integer getTopK();
    Float getTopP();
    ChatOptions copy();
}
```

**常用选项说明**：

- **model**: 要使用的模型 ID
- **frequencyPenalty**: 频率惩罚（-2.0 到 2.0），降低重复令牌的可能性
- **maxTokens**: 生成响应的最大令牌数
- **presencePenalty**: 存在惩罚（-2.0 到 2.0），鼓励谈论新主题
- **stopSequences**: 停止序列列表，遇到时停止生成
- **temperature**: 采样温度（0.0 到 2.0），控制随机性
- **topK**: Top-K 采样参数
- **topP**: Top-P（核采样）参数

此外，每个特定模型的 ChatModel/StreamingChatModel 实现都可以有自己的选项。例如，OpenAI Chat Completion 模型有自己的选项，如 `logitBias`、`seed` 和 `user`。

这是一个强大的功能，允许开发者在启动应用程序时使用特定于模型的选项，然后在运行时使用 `Prompt` 请求覆盖它们。

Spring AI 提供了一个复杂的系统来配置和使用 ChatModels。它允许在启动时设置默认配置，同时还提供了在每个请求基础上覆盖这些设置的灵活性。

**选项合并流程**：

1. **启动配置** - ChatModel/StreamingChatModel 使用"启动"ChatOptions 初始化。这些选项在 ChatModel 初始化期间设置，旨在提供默认配置。

2. **运行时配置** - 对于每个请求，Prompt 可以包含运行时 ChatOptions，这些可以覆盖启动选项。

3. **选项合并过程** - "合并选项"步骤结合启动和运行时选项。如果提供了运行时选项，它们优先于启动选项。

4. **输入处理** - "转换输入"步骤将输入指令转换为本地的、特定于模型的格式。

5. **输出处理** - "转换输出"步骤将模型的响应转换为标准化的 `ChatResponse` 格式。

启动和运行时选项的分离允许全局配置和特定于请求的调整。

### ChatResponse

`ChatResponse` 类的结构如下：

```java
public class ChatResponse implements ModelResponse<Generation> {

    private final ChatResponseMetadata chatResponseMetadata;
    private final List<Generation> generations;

    @Override
    public ChatResponseMetadata getMetadata() {...}

    @Override
    public List<Generation> getResults() {...}

    // 其他方法省略
}
```

[ChatResponse](https://github.com/spring-projects/spring-ai/blob/main/spring-ai-model/src/main/java/org/springframework/ai/chat/model/ChatResponse.java) 类保存 AI 模型的输出，每个 `Generation` 实例包含单个提示可能产生的多个输出之一。

`ChatResponse` 类还携带关于 AI 模型响应的 `ChatResponseMetadata` 元数据。

### Generation

最后，[Generation](https://github.com/spring-projects/spring-ai/blob/main/spring-ai-model/src/main/java/org/springframework/ai/chat/model/Generation.java) 类从 `ModelResult` 扩展，表示模型输出（助手消息）和相关元数据：

```java
public class Generation implements ModelResult<AssistantMessage> {

    private final AssistantMessage assistantMessage;
    private ChatGenerationMetadata chatGenerationMetadata;

    @Override
    public AssistantMessage getOutput() {...}

    @Override
    public ChatGenerationMetadata getMetadata() {...}

    // 其他方法省略
}
```

## 可用实现

Spring AI 提供了与多个 AI 服务提供商的集成，所有这些都通过统一的 `ChatModel` 和 `StreamingChatModel` 接口进行交互：

- **OpenAI Chat Completion** (支持流式、多模态和函数调用)
- **Microsoft Azure OpenAI Chat Completion** (支持流式和函数调用)
- **Alibaba DashScope Chat Completion** (支持流式和函数调用)
- **Ollama Chat Completion** (支持流式、多模态和函数调用)
- **Hugging Face Chat Completion** (不支持流式)
- **Google Vertex AI Gemini Chat Completion** (支持流式、多模态和函数调用)
- **Amazon Bedrock**
- **Mistral AI Chat Completion** (支持流式和函数调用)
- **Anthropic Chat Completion** (支持流式和函数调用)

关于每个模型的具体用法与特性，请查看 Spring AI Alibaba 模型适配文档。

## DashScopeChatModel

DashScope 是阿里云提供的大模型服务平台，提供通义千问等多个大语言模型。Spring AI Alibaba 提供了 DashScopeChatModel
的集成。

### 前置条件

在使用 DashScopeChatModel 之前，你需要：

1. 获取 DashScope API Key：访问 [阿里云百炼](https://www.aliyun.com/product/bailian)
2. 设置环境变量：`export AI_DASHSCOPE_API_KEY=your_api_key`

### 添加依赖

```xml
<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>spring-ai-alibaba-starter-dashscope</artifactId>
    <version>1.1.0.0-M4</version>
</dependency>
```

### 基础使用

#### 创建 ChatModel

```java
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.springframework.ai.chat.model.ChatModel;

// 创建 DashScope API 实例
DashScopeApi dashScopeApi = DashScopeApi.builder()
    .apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
    .build();

// 创建 ChatModel
ChatModel chatModel = DashScopeChatModel.builder()
    .dashScopeApi(dashScopeApi)
    .build();
```

#### 简单调用

```java
// 使用字符串直接调用
String response = chatModel.call("介绍一下Spring框架");
System.out.println(response);
```

#### 使用 Prompt

```java
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;

// 创建 Prompt
Prompt prompt = new Prompt(new UserMessage("解释什么是微服务架构"));

// 调用并获取响应
ChatResponse response = chatModel.call(prompt);
String answer = response.getResult().getOutput().getContent();
System.out.println(answer);
```

### 配置选项

#### 使用 ChatOptions

```java
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;

DashScopeChatOptions options = DashScopeChatOptions.builder()
    .model("qwen-plus")           // 模型名称
    .temperature(0.7)              // 温度参数
    .maxTokens(2000)               // 最大令牌数
    .topP(0.9)                     // Top-P 采样
    .build();

ChatModel chatModel = DashScopeChatModel.builder()
    .dashScopeApi(dashScopeApi)
    .defaultOptions(options)
    .build();
```

#### 运行时覆盖选项

```java
// 创建带有特定选项的 Prompt
DashScopeChatOptions runtimeOptions = DashScopeChatOptions.builder()
    .temperature(0.3)  // 更低的温度，更确定的输出
    .maxTokens(500)
    .build();

Prompt prompt = new Prompt(
    new UserMessage("用一句话总结Java的特点"),
    runtimeOptions
);

ChatResponse response = chatModel.call(prompt);
```

### 流式响应

```java
import reactor.core.publisher.Flux;

// 使用流式 API
Flux<ChatResponse> responseStream = chatModel.stream(
    new Prompt("详细解释Spring Boot的自动配置原理")
);

// 订阅并处理流式响应
responseStream.subscribe(
    chatResponse -> {
        String content = chatResponse.getResult()
            .getOutput()
            .getContent();
        System.out.print(content);
    },
    error -> System.err.println("错误: " + error.getMessage()),
    () -> System.out.println("\n流式响应完成")
);
```

### 多轮对话

```java
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import java.util.List;

// 创建对话历史
List<Message> messages = List.of(
    new SystemMessage("你是一个Java专家"),
    new UserMessage("什么是Spring Boot?"),
    new AssistantMessage("Spring Boot是..."),
    new UserMessage("它有什么优势?")
);

Prompt prompt = new Prompt(messages);
ChatResponse response = chatModel.call(prompt);
```

### 支持的模型

DashScope 支持多个模型，包括：

- **qwen-turbo**: 通义千问超大规模语言模型，支持中文、英文等
- **qwen-plus**: 通义千问增强版
- **qwen-max**: 通义千问旗舰版
- **qwen-max-longcontext**: 支持长文本的通义千问

### 函数调用

DashScopeChatModel 支持函数调用（Function Calling），允许模型调用外部函数：

```java
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.function.FunctionCallback;

// 定义函数
FunctionCallback weatherFunction = FunctionCallback.builder()
    .function("getWeather", (city) -> {
        // 实际的天气查询逻辑
        return "晴朗，25°C";
    })
    .description("获取指定城市的天气")
    .inputType(String.class)
    .build();

// 使用函数
DashScopeChatOptions options = DashScopeChatOptions.builder()
    .functions(List.of(weatherFunction))
    .build();

Prompt prompt = new Prompt("北京的天气怎么样?", options);
ChatResponse response = chatModel.call(prompt);
```

## 与 ReactAgent 集成

在 Spring AI Alibaba Agent Framework 中使用 DashScopeChatModel：

```java
import com.alibaba.cloud.ai.graph.agent.ReactAgent;

ReactAgent agent = ReactAgent.builder()
    .name("my_agent")
    .model(chatModel)
    .systemPrompt("你是一个有帮助的AI助手")
    .build();

// 调用 Agent
AssistantMessage response = agent.call("帮我分析这个问题");
```

详细的 Agent 使用方法请参考 [Agents 文档](./agents.md)。

## 总结

通过这个统一的 API，开发者可以轻松地在不同的 AI 服务提供商之间切换，同时保持代码的一致性和可维护性。

