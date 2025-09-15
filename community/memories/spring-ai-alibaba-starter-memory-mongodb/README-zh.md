# Spring AI Alibaba MongoDB Memory 模块

[English](./README.md)

## 简介

Spring AI Alibaba MongoDB Memory 模块是Spring AI Alibaba项目的核心组件之一，专门提供基于MongoDB的存储解决方案。为AI应用提供快速、可靠的对话历史和上下文数据存储服务，使AI系统能够"记住"之前的交互，从而提供更连贯、更个性化的用户体验。

## 主要特性

- **MongoDB存储**：利用MongoDB的高速读写能力，实现对话历史和上下文数据的快速存取
- **与Spring生态无缝集成**：完美兼容Spring框架和Spring Boot应用

## 快速开始

### Maven依赖

将以下依赖添加到你的项目中：

```xml
<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>spring-ai-alibaba-autoconfigure-memory</artifactId>
    <version>${latest.version}</version>
</dependency>

<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>spring-ai-alibaba-starter-memory-mongodb</artifactId>
    <version>${latest.version}</version>
</dependency>
```

### 基本配置

在`application.properties`或`application.yml`中添加MongoDB配置：

```yaml
spring:
  ai:
    memory:
      mongodb:
        host: localhost
        port: 27017
```

### 示例代码

```java
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import com.alibaba.cloud.ai.memory.mongodb.MongoDBChatMemoryRepository;

import javax.servlet.http.HttpServletResponse;

@RestController
public class ChatController {

    @Autowired
    private MongoDBChatMemoryRepository mongoDBChatMemoryRepository;

    @Autowired
    private ChatClient chatClient;

    /**
     * 流式聊天接口（基于 MongoDB 存储对话历史）
     *
     * @param prompt 用户输入的问题或提示
     * @param chatId 对话 ID，用于标识当前会话
     * @param response HttpServletResponse 对象，用于设置响应编码
     * @return 返回流式响应内容（Flux<String>），逐步输出 AI 回答
     */
    @GetMapping("/mongodb")
    public Flux<String> mongodbChat(
            @RequestParam("prompt") String prompt,
            @RequestParam("chatId") String chatId,
            HttpServletResponse response) {

        // 设置响应字符编码为 UTF-8，确保中文等字符正确显示
        response.setCharacterEncoding("UTF-8");

        // 构建带消息窗口的记忆组件，最多保留最近 10 条消息
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(mongoDBChatMemoryRepository)
                .maxMessages(10)
                .build();

        // 发起 AI 模型调用，并启用记忆功能
        return chatClient.prompt(prompt)
                .advisors(new MessageChatMemoryAdvisor(chatMemory))
                .advisors(a -> a
                        .param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 100)
                )
                .stream()     // 使用流式响应
                .content();   // 获取内容流
    }
}
```
