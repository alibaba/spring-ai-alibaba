# Spring AI Alibaba JDBC Memory 模块

## 简介

Spring AI Alibaba JDBC Memory 模块是Spring AI Alibaba项目的一个组件，专门用于提供基于JDBC的内存存储解决方案。该模块允许AI应用程序通过JDBC接口将对话历史、提示信息和上下文数据持久化到关系型数据库中，从而实现长期记忆功能。

## 主要特性

- **JDBC持久化存储**：利用关系型数据库进行AI对话历史和上下文数据的持久化存储
- **与Spring生态集成**：无缝集成Spring框架和Spring Boot应用
- **灵活的数据库支持**：支持各种基于JDBC的数据库，如MySQL、PostgreSQL、H2等
- **自动建表**：支持自动创建所需的数据库表结构

## 快速开始

### Maven依赖

将以下依赖添加到你的项目中：

```xml
<dependency>
    <groupId>com.alibaba.spring.ai</groupId>
    <artifactId>spring-ai-alibaba-starter-memory-jdbc</artifactId>
    <version>${latest.version}</version>
</dependency>
```

### 基本配置

在`application.properties`或`application.yml`中添加数据库配置：

```yml
spring:
  ai:
    memory:
      mysql:
        enabled: true
  datasource:
    url: jdbc:mysql://localhost:3306/test?useUnicode=true&characterEncoding=utf-8&useSSL=false&allowPublicKeyRetrieval=true&zeroDateTimeBehavior=convertToNull&transformedBitIsBoolean=true&allowMultiQueries=true&tinyInt1isBit=false&allowLoadLocalInfile=true&allowLocalInfile=true&allowUrl
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
```

### 示例代码

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import com.alibaba.cloud.ai.memory.jdbc.MysqlChatMemoryRepository;

@RestController
public class ChatController {

    @Autowired
    private MysqlChatMemoryRepository mysqlChatMemoryRepository;

    /**
     * Redis 流式聊天接口（实际使用的是 MySQL Chat Memory）
     *
     * @param prompt 用户输入的提示词
     * @param chatId 对话 ID
     * @param response HTTP 响应对象，用于设置编码
     * @return 字符串流式响应
     */
    @GetMapping("/redis")
    public Flux<String> redis(
            @RequestParam("prompt") String prompt,
            @RequestParam("chatId") String chatId,
            HttpServletResponse response) {

        // 设置响应字符编码为 UTF-8
        response.setCharacterEncoding("UTF-8");

        // 构建带有消息窗口的记忆组件，限制最多保存 10 条消息
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(mysqlChatMemoryRepository)
                .maxMessages(10)
                .build();

        // 发起对话请求并流式返回结果
        return chatClient.prompt(prompt)
                .advisors(new MessageChatMemoryAdvisor(chatMemory)) 
                .advisors(a -> a
                        .param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId) 
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 100)  
                )
                .stream()
                .content(); // 获取内容流
    }
}
```

## 数据库表结构

该模块会自动创建以下表结构：

> 不同数据库可能会有些许差异

```sql
CREATE TABLE ai_chat_memory (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id VARCHAR(256) NOT NULL,
    content LONGTEXT NOT NULL,
    type VARCHAR(100) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    CONSTRAINT chk_message_type CHECK (type IN ('USER', 'ASSISTANT', 'SYSTEM', 'TOOL'))
)
```

## 常见问题解答

**Q: 支持哪些数据库？**  
A: 理论上支持所有提供JDBC驱动的关系型数据库，已测试的包括MySQL、PostgreSQL、H2和Oracle。
