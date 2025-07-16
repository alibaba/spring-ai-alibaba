# Spring AI Alibaba Elasticsearch Memory 模块

## 简介

Spring AI Alibaba Elasticsearch Memory 模块是 Spring AI Alibaba 项目的核心组件之一，
专门提供基于 Elasticsearch 的存储解决方案。该模块利用 Elasticsearch 的全文检索和分布式特性，为 AI 应用提供快速、可靠的对话历史和上下文数据存储服务，使 AI 系统能够"记住"之前的交互，从而提供更连贯、更个性化的用户体验。

## 主要特性

- **Elasticsearch 存储**：利用 Elasticsearch 的高性能和分布式特性，实现对话历史和上下文数据的快速存取
- **与 Spring 生态无缝集成**：完美兼容 Spring 框架和 Spring Boot 应用
- **灵活的记忆管理**：支持设置对话记忆大小限制，自动清理过期对话

## 快速开始

### Maven 依赖

将以下依赖添加到你的项目中：

```xml
<dependency>
    <groupId>com.alibaba.spring.ai</groupId>
    <artifactId>spring-ai-alibaba-starter-memory-elasticsearch</artifactId>
    <version>${latest.version}</version>
</dependency>
```

### 基本配置

在`application.properties`或`application.yml`中添加 Elasticsearch 配置：

```yaml
elasticsearch:
  host: localhost
  port: 9200
  scheme: http
  # 如果有认证需求
  # username: elastic
  # password: password
```

### 示例代码

```java
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

    /**
     * 流式聊天接口（基于 Elasticsearch 存储对话历史）
     *
     * @param prompt 用户输入的问题或提示
     * @param chatId 对话 ID，用于标识当前会话
     * @param response HttpServletResponse 对象，用于设置响应编码
     * @return 返回流式响应内容（Flux<String>），逐步输出 AI 回答
     */
    @GetMapping("/chat")
    public Flux<String> elasticsearchChat(
            @RequestParam("prompt") String prompt,
            @RequestParam("chatId") String chatId,
            HttpServletResponse response) {

        // 设置响应字符编码为 UTF-8，确保中文等字符正确显示
        response.setCharacterEncoding("UTF-8");

        // 构建带消息窗口的记忆组件，最多保留最近 10 条消息
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(elasticsearchChatMemoryRepository)
                .maxMessages(10)
                .build();

        // 发起 AI 模型调用，并启用记忆功能
        return chatClient.prompt(prompt)
                .advisors(new MessageChatMemoryAdvisor(chatMemory))
                .advisors(a -> a
                        .param("chatMemoryConversationId", chatId)
                        .param("chatMemoryRetrieveSize", 100)
                )
                .stream()     // 使用流式响应
                .content();   // 获取内容流
    }
}
```

## 高级特性

### 自定义 Elasticsearch 配置

可以通过`ElasticsearchConfig`类来自定义更复杂的 Elasticsearch 配置：

```java
ElasticsearchConfig config = new ElasticsearchConfig();
config.setHost("localhost");
config.setPort(9200);
config.setScheme("https"); // 使用HTTPS
config.setUsername("elastic");
config.setPassword("password");

// 使用集群多节点配置
List<String> nodes = new ArrayList<>();
nodes.add("node1:9200");
nodes.add("node2:9200");
config.setNodes(nodes);

ElasticsearchChatMemoryRepository repository = new ElasticsearchChatMemoryRepository(config);
```

### 管理记忆大小

```java
// 为特定对话清理过期消息，保留最新的消息
// 参数：对话ID，最大消息数量，要删除的消息数量
repository.clearOverLimit("conversation-123", 10, 5);
```
