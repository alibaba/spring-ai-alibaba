# Spring AI Alibaba Memcached Memory 模块

## 简介

Spring AI Alibaba Memcached Memory 模块是Spring AI Alibaba项目的核心组件之一，专门提供基于Memcached的存储解决方案。为AI应用提供快速、可靠的对话历史和上下文数据存储服务，使AI系统能够"记住"之前的交互，从而提供更连贯、更个性化的用户体验。

## 主要特性

- **Memcached存储**：利用Memcached的高速读写能力，实现对话历史和上下文数据的快速存取
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
    <artifactId>spring-ai-alibaba-starter-memory-memcached</artifactId>
    <version>${latest.version}</version>
</dependency>
```

### 基本配置

在`application.properties`或`application.yml`中添加MongoDB配置：

```yaml
spring:
  ai:
    memory:
      memcached:
        host: localhost
        port: 11211
```

### 示例代码

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import com.alibaba.cloud.ai.memory.mongodb.MongoDBChatMemoryRepository;
import javax.servlet.http.HttpServletResponse;

@RestController
public class ChatController {

    @Autowired
    private MemcachedChatMemoryRepository memcachedChatMemoryRepository;

    @GetMapping("/memcached")
    public String memcachedChat(
            HttpServletResponse response) {

        // 设置响应字符编码为 UTF-8，确保中文等字符正确显示
        response.setCharacterEncoding("UTF-8");

        // 构建带消息窗口的记忆组件，最多保留最近 10 条消息
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(memcachedChatMemoryRepository)
                .maxMessages(1)
                .build();
        ChatClient chatClient = ChatClient.builder(chatModel).defaultAdvisors(
                        new SimpleLoggerAdvisor(), MessageChatMemoryAdvisor.builder(chatMemory).conversationId("memcachedId").build())
                .build();
        // 发起 AI 模型调用，并启用记忆功能
        return chatClient.prompt().user("你好")
                .call().chatResponse().getResult().getOutput().getText();
    }
}
```
