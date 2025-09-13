# Spring AI Alibaba JDBC Memory Module

[中文版本](./README-zh.md)

## Introduction

The Spring AI Alibaba JDBC Memory module is a component of the Spring AI Alibaba project, specifically designed to provide a JDBC-based in-memory storage solution. This module enables AI applications to persist conversational history, prompts, and contextual data to relational databases through the JDBC interface, thereby implementing long-term memory capabilities.

## Core Features

- **JDBC Persistent Storage**: Leverages relational databases for persistent storage of AI conversation history and contextual data.
- **Spring Ecosystem Integration**: Seamlessly integrates with the Spring Framework and Spring Boot applications.
- **Flexible Database Support**: Compatible with various JDBC-based databases such as MySQL, PostgreSQL, H2, and others.
- **Automatic Schema Generation**: Supports automatic creation of required database table structures.

## Get Started

### Maven Dependency

Add the following dependency to your project:

```xml
<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>spring-ai-alibaba-autoconfigure-memory</artifactId>
    <version>${latest.version}</version>
</dependency>

<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>spring-ai-alibaba-starter-memory-jdbc</artifactId>
    <version>${latest.version}</version>
</dependency>
```

### Basic Configuration

Add the following database configuration to your `application.properties` or `application.yml`:

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

### Sample Code

```java
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import com.alibaba.cloud.ai.memory.jdbc.MysqlChatMemoryRepository;

@RestController
public class ChatController {

    @Autowired
    private MysqlChatMemoryRepository mysqlChatMemoryRepository;

    @Autowired
    private ChatClient chatClient;

    /**
     * Redis Stream Chat Interface (Actual implementation uses MySQL Chat Memory)
     *
     * @param prompt User-submitted prompt
     * @param chatId Conversation ID
     * @param response HTTP response object for encoding configuration
     * @return String streaming response
     */
    @GetMapping("/redis")
    public Flux<String> redis(
            @RequestParam("prompt") String prompt,
            @RequestParam("chatId") String chatId,
            HttpServletResponse response) {

        // Sets the response character encoding to UTF-8
        response.setCharacterEncoding("UTF-8");

        // Constructs a message window-based memory component with a limit of 10 retained messages
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(mysqlChatMemoryRepository)
                .maxMessages(10)
                .build();

        // Initiates a chat request and returns the result in streaming mode
        return chatClient.prompt(prompt)
                .advisors(new MessageChatMemoryAdvisor(chatMemory))
                .advisors(a -> a
                        .param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 100)
                )
                .stream()
                .content(); // Retrieves the content stream
    }
}
```

## Database Schema

The module automatically creates the following table structures:

> Note: Some variations may exist across different database implementations.

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

## Frequently Asked Questions

**Q: Which databases are supported?**  
A: Theoretically supports all relational databases that provide JDBC drivers. Tested databases include MySQL, PostgreSQL, H2, and Oracle.
