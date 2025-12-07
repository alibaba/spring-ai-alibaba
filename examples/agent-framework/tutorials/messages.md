---
title: Messages 消息
description: 了解Messages作为模型交互的基本单元，学习消息角色、内容和元数据的使用方法
keywords: [Messages, 消息, Role, Content, Metadata, UserMessage, SystemMessage, AssistantMessage, LLM交互]
---

Messages 是 Spring AI Alibaba 中模型交互的基本单元。它们代表模型的输入和输出，携带在与 LLM 交互时表示对话状态所需的内容和元数据。

Messages 是包含以下内容的对象：

* **Role（角色）** - 标识消息类型（如 `system`、`user`、`assistant`）
* **Content（内容）** - 表示消息的实际内容（如文本、图像、音频、文档等）
* **Metadata（元数据）** - 可选字段，如响应信息、消息 ID 和 token 使用情况

Spring AI Alibaba 提供了一个标准的消息类型系统，可在所有模型提供商之间工作，确保无论调用哪个模型都具有一致的行为。

## 基础使用

使用 messages 最简单的方式是创建消息对象并在调用模型时传递它们。

<Code
  language="java"
  title="基础消息使用示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/MessagesExample.java"
>
{`import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;
import java.util.List;

// 使用 DashScope ChatModel
ChatModel chatModel = // ... 初始化 ChatModel

SystemMessage systemMsg = new SystemMessage("你是一个有帮助的助手。");
UserMessage userMsg = new UserMessage("你好，你好吗？");

// 与聊天模型一起使用
List<org.springframework.ai.chat.messages.Message> messages = List.of(systemMsg, userMsg);
Prompt prompt = new Prompt(messages);
ChatResponse response = chatModel.call(prompt);  // 返回 ChatResponse，包含 AssistantMessage`}
</Code>

### 文本提示

文本提示是字符串 - 适用于简单的生成任务，不需要保留对话历史。

<Code
  language="java"
  title="文本提示示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/MessagesExample.java"
>
{`// 使用字符串直接调用
String response = chatModel.call("写一首关于春天的俳句");`}
</Code>

**使用文本提示的场景**：

* 有单个独立的请求
* 不需要对话历史
* 想要最小的代码复杂性

### 消息提示

或者，你可以通过提供消息对象列表向模型传递消息列表。

<Code
  language="java"
  title="消息提示示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/MessagesExample.java"
>
{`import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;
import java.util.List;

List<org.springframework.ai.chat.messages.Message> messages = List.of(
    new SystemMessage("你是一个诗歌专家"),
    new UserMessage("写一首关于春天的俳句"),
    new AssistantMessage("樱花盛开时...")
);
Prompt prompt = new Prompt(messages);
ChatResponse response = chatModel.call(prompt);`}
</Code>

**使用消息提示的场景**：

* 管理多轮对话
* 处理多模态内容（图像、音频、文件）
* 包含系统指令

## 消息类型

* **System Message（系统消息）** - 告诉模型如何行为并为交互提供上下文
* **User Message（用户消息）** - 表示用户输入和与模型的交互
* **Assistant Message（助手消息）** - 模型生成的响应，包括文本内容、工具调用和元数据
* **Tool Response Message（工具响应消息）** - 表示工具调用的输出

### System Message

`SystemMessage` 表示一组初始指令，用于引导模型的行为。你可以使用系统消息来设置语气、定义模型的角色并建立响应指南。

<Code
  language="java"
  title="SystemMessage 基础指令示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/MessagesExample.java"
>
{`// 基础指令
SystemMessage systemMsg = new SystemMessage("你是一个有帮助的编程助手。");

List<org.springframework.ai.chat.messages.Message> messages = List.of(
    systemMsg,
    new UserMessage("如何创建 REST API？")
);
ChatResponse response = chatModel.call(new Prompt(messages));`}
</Code>

<Code
  language="java"
  title="SystemMessage 详细角色设定示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/MessagesExample.java"
>
{`// 详细的角色设定
SystemMessage systemMsg = new SystemMessage("""
    你是一位资深的 Java 开发者，擅长 Web 框架。
    始终提供代码示例并解释你的推理。
    在解释中要简洁但透彻。
    """);

List<org.springframework.ai.chat.messages.Message> messages = List.of(
    systemMsg,
    new UserMessage("如何创建 REST API？")
);
ChatResponse response = chatModel.call(new Prompt(messages));`}
</Code>

### User Message

`UserMessage` 表示用户输入和交互。它们可以包含文本、图像、音频、文件和任何其他数量的多模态内容。

#### 文本内容

<Code
  language="java"
  title="UserMessage 文本内容示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/MessagesExample.java"
>
{`// 使用消息对象
ChatResponse response = chatModel.call(
    new Prompt(List.of(new UserMessage("什么是机器学习？")))
);

// 使用字符串快捷方式
// 使用字符串是单个 UserMessage 的快捷方式
String response = chatModel.call("什么是机器学习？");`}
</Code>

#### 消息元数据

<Code
  language="java"
  title="UserMessage 消息元数据示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/MessagesExample.java"
>
{`import java.util.Map;

UserMessage userMsg = UserMessage.builder()
    .text("你好！")
    .metadata(Map.of(
        "user_id", "alice",  // 可选：识别不同用户
        "session_id", "sess_123"  // 可选：会话标识符
    ))
    .build();`}
</Code>

**注意**：元数据字段的行为因提供商而异 - 有些用于用户识别，有些则忽略它。要检查，请参考模型提供商的文档。

#### 多模态内容

`UserMessage` 可以包含多模态内容，如图像：

<Code
  language="java"
  title="UserMessage 多模态内容示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/MessagesExample.java"
>
{`import org.springframework.ai.content.Media;
import org.springframework.util.MimeTypeUtils;
import java.net.URL;

// 从 URL 创建图像
UserMessage userMsg = UserMessage.builder()
    .text("描述这张图片的内容。")
    .media(Media.builder()
        .mimeType(MimeTypeUtils.IMAGE_JPEG)
        .data(new URL("https://example.com/image.jpg"))
        .build())
    .build();`}
</Code>

### Assistant Message

`AssistantMessage` 表示模型调用的输出。它们可以包括多模态数据、工具调用以及你稍后可以访问的提供商特定元数据。

<Code
  language="java"
  title="AssistantMessage 基础使用示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/MessagesExample.java"
>
{`ChatResponse response = chatModel.call(new Prompt("解释 AI"));
AssistantMessage aiMessage = response.getResult().getOutput();
System.out.println(aiMessage.getText());`}
</Code>

`AssistantMessage` 对象由模型调用返回，其中包含响应中的所有相关元数据。

提供商对消息类型的权重/上下文化方式不同，这意味着有时手动创建新的 `AssistantMessage` 对象并将其插入消息历史中（就像它来自模型一样）会很有帮助。

<Code
  language="java"
  title="手动创建 AssistantMessage 示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/MessagesExample.java"
>
{`import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

// 手动创建 AI 消息（例如，用于对话历史）
AssistantMessage aiMsg = new AssistantMessage("我很乐意帮助你回答这个问题！");

// 添加到对话历史
List<org.springframework.ai.chat.messages.Message> messages = List.of(
    new SystemMessage("你是一个有帮助的助手"),
    new UserMessage("你能帮我吗？"),
    aiMsg,  // 插入，就像它来自模型一样
    new UserMessage("太好了！2+2 等于多少？")
);

ChatResponse response = chatModel.call(new Prompt(messages));`}
</Code>

**AssistantMessage 属性**：

- **text**: 消息的文本内容
- **metadata**: 消息的元数据映射
- **toolCalls**: 模型进行的工具调用列表
- **media**: 媒体内容列表（如果有）

#### 工具调用

当模型进行工具调用时，它们包含在 `AssistantMessage` 中：

<Code
  language="java"
  title="AssistantMessage 工具调用示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/MessagesExample.java"
>
{`import org.springframework.ai.chat.messages.AssistantMessage.ToolCall;

ChatResponse response = chatModel.call(prompt);
AssistantMessage aiMessage = response.getResult().getOutput();

if (aiMessage.hasToolCalls()) {
    for (ToolCall toolCall : aiMessage.getToolCalls()) {
        System.out.println("Tool: " + toolCall.name());
        System.out.println("Args: " + toolCall.arguments());
        System.out.println("ID: " + toolCall.id());
    }
}`}
</Code>

#### Token 使用

Spring AI Alibaba 的 `ChatResponse` 可以在其元数据中保存 token 计数和其他使用元数据：

<Code
  language="java"
  title="Token 使用信息访问示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/MessagesExample.java"
>
{`ChatResponse response = chatModel.call(new Prompt("你好！"));
ChatResponseMetadata metadata = response.getMetadata();

// 访问使用信息
if (metadata != null && metadata.getUsage() != null) {
    System.out.println("Input tokens: " + metadata.getUsage().getPromptTokens());
    System.out.println("Output tokens: " + metadata.getUsage().getCompletionTokens());
    System.out.println("Total tokens: " + metadata.getUsage().getTotalTokens());
}`}
</Code>

#### 流式和块

在流式传输期间，你将收到可以组合成完整消息对象的块：

<Code
  language="java"
  title="流式输出示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/MessagesExample.java"
>
{`import reactor.core.publisher.Flux;

Flux<ChatResponse> responseStream = chatModel.stream(new Prompt("你好"));

StringBuilder fullResponse = new StringBuilder();
responseStream.subscribe(
    chunk -> {
        String content = chunk.getResult().getOutput().getText();
        fullResponse.append(content);
        System.out.print(content);
    }
);`}
</Code>

**了解更多**：
- 从聊天模型流式传输 tokens
- 从 agents 流式传输 tokens 和/或步骤

### Tool Response Message

对于支持工具调用的模型，AI 消息可以包含工具调用。工具消息用于将单个工具执行的结果传回模型。

<Code
  language="java"
  title="ToolResponseMessage 工具响应消息示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/MessagesExample.java"
>
{`import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage.ToolResponse;

// 在模型进行工具调用后
AssistantMessage aiMessage = AssistantMessage.builder()
    .content("")
    .toolCalls(List.of(
        new AssistantMessage.ToolCall(
            "call_123",
            "tool",
            "get_weather",
            "{\"location\": \"San Francisco\"}"
        )
    ))
    .build();

// 执行工具并创建结果消息
String weatherResult = "晴朗，22°C";
ToolResponseMessage toolMessage = ToolResponseMessage.builder()
    .responses(List.of(
        new ToolResponse("call_123", "get_weather", weatherResult)
    ))
    .build();

// 继续对话
List<org.springframework.ai.chat.messages.Message> messages = List.of(
    new UserMessage("旧金山的天气怎么样？"),
    aiMessage,      // 模型的工具调用
    toolMessage     // 工具执行结果
);
ChatResponse response = chatModel.call(new Prompt(messages));`}
</Code>

**ToolResponseMessage 属性**：

- **responses**: ToolResponse 对象列表，每个包含：
  - **id**: 工具调用 ID（必须与 AIMessage 中的工具调用 ID 匹配）
  - **name**: 调用的工具名称
  - **responseData**: 工具调用的字符串化输出

## 多模态内容

**多模态性**指的是处理不同形式数据的能力，如文本、音频、图像和视频。Spring AI Alibaba 包含这些数据的标准类型，可以跨提供商使用。

聊天模型可以接受多模态数据作为输入并生成它作为输出。下面我们展示包含多模态数据的输入消息的简短示例。

### 图像输入

<Code
  language="java"
  title="图像输入示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/MessagesExample.java"
>
{`import org.springframework.ai.content.Media;
import org.springframework.util.MimeTypeUtils;
import java.net.URL;

// 从 URL
UserMessage message = UserMessage.builder()
    .text("描述这张图片的内容。")
    .media(Media.builder()
        .mimeType(MimeTypeUtils.IMAGE_JPEG)
        .data(new URL("https://example.com/image.jpg"))
        .build())
    .build();

// 从本地文件
import org.springframework.core.io.ClassPathResource;

UserMessage message = UserMessage.builder()
    .text("描述这张图片的内容。")
    .media(new Media(
        MimeTypeUtils.IMAGE_JPEG,
        new ClassPathResource("images/photo.jpg")
    ))
    .build();`}
</Code>

### 音频输入

<Code
  language="java"
  title="音频输入示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/MessagesExample.java"
>
{`import org.springframework.ai.content.Media;
import org.springframework.util.MimeTypeUtils;

UserMessage message = UserMessage.builder()
    .text("描述这段音频的内容。")
    .media(new Media(
        MimeTypeUtils.parseMimeType("audio/wav"),
        new ClassPathResource("audio/recording.wav")
    ))
    .build();`}
</Code>

### 视频输入

<Code
  language="java"
  title="视频输入示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/MessagesExample.java"
>
{`import org.springframework.ai.content.Media;
import org.springframework.util.MimeTypeUtils;

UserMessage message = UserMessage.builder()
    .text("描述这段视频的内容。")
    .media(Media.builder()
        .mimeType(MimeTypeUtils.parseMimeType("video/mp4"))
        .data(new URL("https://example.com/path/to/video.mp4"))
        .build())
    .build();`}
</Code>

**警告**：并非所有模型都支持所有文件类型。请查看模型提供商的文档以了解支持的格式和大小限制。

## 与 Chat Models 一起使用

Chat models 接受消息对象序列作为输入并返回 `ChatResponse`（包含 `AssistantMessage`）作为输出。交互通常是无状态的，因此简单的对话循环涉及使用不断增长的消息列表调用模型。

### 基础对话示例

<Code
  language="java"
  title="基础对话示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/MessagesExample.java"
>
{`import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;
import java.util.ArrayList;
import java.util.List;

ChatModel chatModel = // ... 初始化

List<Message> conversationHistory = new ArrayList<>();

// 第一轮对话
conversationHistory.add(new UserMessage("你好！"));
ChatResponse response1 = chatModel.call(new Prompt(conversationHistory));
conversationHistory.add(response1.getResult().getOutput());

// 第二轮对话
conversationHistory.add(new UserMessage("你能帮我学习 Java 吗？"));
ChatResponse response2 = chatModel.call(new Prompt(conversationHistory));
conversationHistory.add(response2.getResult().getOutput());

// 第三轮对话
conversationHistory.add(new UserMessage("从哪里开始？"));
ChatResponse response3 = chatModel.call(new Prompt(conversationHistory));`}
</Code>

### 使用 Builder 模式

Spring AI Alibaba 的消息类提供了 builder 模式以便于构建：

<Code
  language="java"
  title="Builder 模式示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/MessagesExample.java"
>
{`// UserMessage with builder
UserMessage userMsg = UserMessage.builder()
    .text("你好，我想学习 Spring AI Alibaba")
    .metadata(Map.of("user_id", "user_123"))
    .build();

// SystemMessage with builder
SystemMessage systemMsg = SystemMessage.builder()
    .text("你是一个 Spring 框架专家")
    .metadata(Map.of("version", "1.0"))
    .build();

// AssistantMessage with builder
AssistantMessage assistantMsg = AssistantMessage.builder()
    .content("我很乐意帮助你学习 Spring AI Alibaba！")
    .build();`}
</Code>

### 消息复制和修改

<Code
  language="java"
  title="消息复制和修改示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/MessagesExample.java"
>
{`// 复制消息
UserMessage original = new UserMessage("原始消息");
UserMessage copy = original.copy();

// 使用 mutate 创建修改的副本
UserMessage modified = original.mutate()
    .text("修改后的消息")
    .metadata(Map.of("modified", true))
    .build();`}
</Code>

## 在 ReactAgent 中使用

ReactAgent 自动管理消息历史，但你也可以直接使用消息：

<Code
  language="java"
  title="在 ReactAgent 中使用消息" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/MessagesExample.java"
>
{`import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;

ReactAgent agent = ReactAgent.builder()
    .name("my_agent")
    .model(chatModel)
    .systemPrompt("你是一个有帮助的助手")
    .build();

// 使用字符串
AssistantMessage response1 = agent.call("你好");

// 使用 UserMessage
UserMessage userMsg = new UserMessage("帮我写一首诗");
AssistantMessage response2 = agent.call(userMsg);

// 使用消息列表
List<Message> messages = List.of(
    new UserMessage("我喜欢春天"),
    new UserMessage("写一首关于春天的诗")
);
AssistantMessage response3 = agent.call(messages);`}
</Code>
