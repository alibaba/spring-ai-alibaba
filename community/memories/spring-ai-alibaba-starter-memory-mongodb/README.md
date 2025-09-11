# Spring AI Alibaba MongoDB Memory Module

[中文版本](./README-zh.md)

## Introduction

The Spring AI Alibaba MongoDB Memory module is a core component of the Spring AI Alibaba project, specifically designed to provide a MongoDB-based storage solution. It delivers fast and reliable storage services for conversational history and contextual data in AI applications, enabling AI systems to remember previous interactions and thereby deliver more coherent and personalized user experiences.

## Core Features

- **MongoDB Storage**：Leverages MongoDB's high-speed read/write capabilities to enable rapid storage of conversational history and contextual data.
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
    <artifactId>spring-ai-alibaba-starter-memory-mongodb</artifactId>
    <version>${latest.version}</version>
</dependency>
```

### Basic Configuration

Add the following MongoDB configuration to your `application.properties` or `application.yml`:

```yaml
spring:
  ai:
    memory:
      mongodb:
        host: localhost
        port: 27017
```

### Sample Code

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
     * Stream-based chat interface (with conversation history stored in MongoDB).
     *
     * @param prompt User's input question or prompt.
     * @param chatId Conversation ID used to identify the current session.
     * @param response HttpServletResponse object for setting response encoding.
     * @return Streamed response content (Flux<String>), gradually output AI responses
     */
    @GetMapping("/mongodb")
    public Flux<String> mongodbChat(
            @RequestParam("prompt") String prompt,
            @RequestParam("chatId") String chatId,
            HttpServletResponse response) {

        // Sets the response character encoding to UTF-8 to ensure proper display of Chinese and other Unicode characters
        response.setCharacterEncoding("UTF-8");

        // Constructs a message window-based chat memory component retaining up to 10 recent messages
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(mongoDBChatMemoryRepository)
                .maxMessages(10)
                .build();

        // Initiates AI model invocation with memory capabilities enabled
        return chatClient.prompt(prompt)
                .advisors(new MessageChatMemoryAdvisor(chatMemory))
                .advisors(a -> a
                        .param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 100)
                )
                .stream()     // Enables streaming response
                .content();   // Retrieves the content stream
    }
}
```
