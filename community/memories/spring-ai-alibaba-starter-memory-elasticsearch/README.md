# Spring AI Alibaba Elasticsearch Memory Module

[中文版本](./README-zh.md)

## Introduction

The Spring AI Alibaba Elasticsearch Memory Module is a core component of the Spring AI Alibaba project,
specifically designed to provide an Elasticsearch-based storage solution. Leveraging Elasticsearch's full-text search and distributed capabilities, this module delivers fast and reliable storage services for conversational history and contextual data in AI applications. It enables AI systems to remember past interactions, thereby facilitating more coherent and personalized user experiences.

## Core Features

- **Elasticsearch Storage**: Leverages Elasticsearch's high performance and distributed architecture to enable rapid storage and retrieval of conversational history and contextual data.
- **Seamless Integration with Spring Ecosystem**: Provides full compatibility with the Spring Framework and Spring Boot applications for effortless adoption.
- **Flexible Memory Management**: Supports configurable conversation memory size limits with automatic cleanup of expired dialogues.

## Get Started

### Maven Dependency

Add the following dependency to your project:

```xml
<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>spring-ai-alibaba-starter-memory-elasticsearch</artifactId>
    <version>${latest.version}</version>
</dependency>
```

### Basic Configuration

Add the following Elasticsearch configuration to your `application.properties` or `application.yml`:

```yaml
elasticsearch:
  host: localhost
  port: 9200
  scheme: http
  # If authentication is required
  # username: elastic
  # password: password
```

### Sample Code

```java
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import com.alibaba.cloud.ai.memory.elasticsearch.ElasticsearchChatMemoryRepository;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;

import javax.servlet.http.HttpServletResponse;

@RestController
public class ChatController {

    @Autowired
    private ElasticsearchChatMemoryRepository elasticsearchChatMemoryRepository;

    @Autowired
    private ChatClient chatClient;

    /**
     * Stream-based chat interface (with conversation history stored in Elasticsearch).
     *
     * @param prompt User's input question or prompt.
     * @param chatId Conversation ID used to identify the current session.
     * @param response HttpServletResponse object for setting response encoding.
     * @return Streamed response content (Flux<String>), gradually output AI responses
     */
    @GetMapping("/chat")
    public Flux<String> elasticsearchChat(
            @RequestParam("prompt") String prompt,
            @RequestParam("chatId") String chatId,
            HttpServletResponse response) {

        // Sets the response character encoding to UTF-8 to ensure proper display of Chinese and other Unicode characters
        response.setCharacterEncoding("UTF-8");

        // Constructs a message window-based chat memory component retaining up to 10 recent messages
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(elasticsearchChatMemoryRepository)
                .maxMessages(10)
                .build();

        // Initiates AI model invocation with memory capabilities enabled
        return chatClient.prompt(prompt)
                .advisors(new MessageChatMemoryAdvisor(chatMemory))
                .advisors(a -> a
                        .param("chatMemoryConversationId", chatId)
                        .param("chatMemoryRetrieveSize", 100)
                )
                .stream()     // Enables streaming response
                .content();   // Retrieves the content stream
    }
}
```

## Advanced features

### Custom Elasticsearch Configuration

Custom, more complex Elasticsearch configurations can be defined through the `ElasticsearchConfig` class.

```java
ElasticsearchConfig config = new ElasticsearchConfig();
config.setHost("localhost");
config.setPort(9200);
config.setScheme("https"); // Use HTTPS
config.setUsername("elastic");
config.setPassword("password");

// Uses multi-node cluster configuration
List<String> nodes = new ArrayList<>();
nodes.add("node1:9200");
nodes.add("node2:9200");
config.setNodes(nodes);

ElasticsearchChatMemoryRepository repository = new ElasticsearchChatMemoryRepository(config);
```

### Managing Memory Size

```java
// Cleans up expired messages for a specific conversation while retaining the most recent ones
// Parameters: conversation ID, maximum number of messages to keep, number of messages to delete
repository.clearOverLimit("conversation-123", 10, 5);
```
