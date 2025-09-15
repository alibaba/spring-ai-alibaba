# Spring AI Alibaba Memcached Memory Module

[中文版本](./README-zh.md)

## Introduction

The Spring AI Alibaba Memcached Memory module is a core component of the Spring AI Alibaba project, specifically designed to provide a Memcached-based storage solution. It delivers fast and reliable storage services for conversational history and contextual data in AI applications, enabling AI systems to remember previous interactions and thereby deliver more coherent and personalized user experiences.

## Core Features

- **Memcached Storage**：Leverages Memcached's high-speed read/write capabilities to enable rapid storage of conversational history and contextual data.
- **Seamless Integration with Spring Ecosystem**: Provides full compatibility with the Spring Framework and Spring Boot applications for effortless adoption.

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
    <artifactId>spring-ai-alibaba-starter-memory-memcached</artifactId>
    <version>${latest.version}</version>
</dependency>
```

### Basic Configuration

Add the following memcached configuration to your `application.properties` or `application.yml`:

```yaml
spring:
  ai:
    memory:
      memcached:
        host: localhost
        port: 11211
```

### Sample Code

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

        // Sets response character encoding to UTF-8 to ensure proper display of Chinese and other Unicode characters
        response.setCharacterEncoding("UTF-8");

        // Constructs a message window-based chat memory component retaining up to 10 recent messages
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(memcachedChatMemoryRepository)
                .maxMessages(1)
                .build();
        ChatClient chatClient = ChatClient.builder(chatModel).defaultAdvisors(
                        new SimpleLoggerAdvisor(), MessageChatMemoryAdvisor.builder(chatMemory).conversationId("memcachedId").build())
                .build();
        // Initiates AI model invocation with memory capabilities enabled
        return chatClient.prompt().user("你好")
                .call().chatResponse().getResult().getOutput().getText();
    }
}
```
