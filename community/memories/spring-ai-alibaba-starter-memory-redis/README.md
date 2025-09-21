# Spring AI Alibaba Redis Memory Module

[中文版本](./README-zh.md)

## Introduction

The Spring AI Alibaba Redis Memory module is a core component of the Spring AI Alibaba project, specifically designed to provide a Redis-based high-performance in-memory storage solution. Leveraging Redis' high-speed caching and persistence capabilities, this module delivers fast and reliable storage services for conversational history and contextual data in AI applications. It enables AI systems to remember previous interactions, thereby facilitating more coherent and personalized user experiences.

## Core Features

- **High-Performance Redis Storage**：Leverages Redis's high-speed read/write capabilities to enable rapid storage and retrieval of conversational history and contextual data.
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
    <artifactId>spring-ai-alibaba-starter-memory-redis</artifactId>
    <version>${latest.version}</version>
</dependency>
```

### Basic Configuration - Stand-alone

Add the following Redis configuration to your `application.properties` or `application.yml`:

```yaml
spring:
  ai:
    memory:
      redis:
        # Supports standalone and cluster
        mode: standalone
        # Supports jedis, lettuce, and redisson
        client-type: lettuce
        host: localhost
        port: 6379
```

### Basic Configuration - Cluster

Add the following Redis configuration to your `application.properties` or `application.yml`:

```yaml
spring:
  ai:
    memory:
      redis:
        # Supports standalone and cluster
        mode: cluster
        # Supports jedis, lettuce, and redisson
        client-type: lettuce
        cluster:
          nodes: localhost:6379,localhost:6380,localhost:6381
```

### Switching Redis Client Implementation

Add the following Redis configuration to your `application.properties`or `application.yml`:

```yaml
spring:
  ai:
    memory:
      redis:
        # Supports standalone and cluster
        mode: cluster
        # Supports jedis, lettuce, and redisson
        client-type: jedis
        cluster:
          nodes: localhost:6379,localhost:6380,localhost:6381
```

### SSL Configuration
```yaml
# Basic spring.ssl configuration (supports either PEM or JKS). Refer to standard Spring SSL configuration for details.
spring:
  ssl:
    bundle:
      pem:
        myPemBundle:
          keystore:
            certificate: "classpath:cert.pem"
            private-key: "classpath:key.pem"
          truststore:
            certificate: "classpath:cert.pem"
  ai:
    memory:
      redis:
        # Supports standalone and cluster
        mode: standalone
        # Supports jedis, lettuce, and redisson
        client-type: lettuce
        host: localhost
        port: 6379
        ssl:
          enabled: true
          bundle: myPemBundle
```

### Complete Configuration - Stand-alone
```yaml
spring:
  ai:
    memory:
      redis:
        # Supports standalone and cluster
        mode: standalone
        # Supports jedis, lettuce, and redisson
        client-type: jedis
        host: localhost
        port: 6379
        username: yourUsername
        password: yourPassword
        timeout: 2000
```

### Complete Configuration - Cluster
```yaml
spring:
  ai:
    memory:
      redis:
        # Supports standalone and cluster
        mode: cluster
        username: yourUsername
        password: yourPassword
        timeout: 2000
        # Supports jedis, lettuce, and redisson
        client-type: jedis
        cluster:
          nodes: localhost:6379,localhost:6380,localhost:6381
```

### (Optional) Overriding Default JedisRedisChatMemoryRepository with JedisPoolConfig

```java
@Configuration
public class CustomJedisRedisChatMemoryAutoConfiguration extends RedisChatMemoryConnectionAutoConfiguration<JedisRedisChatMemoryRepository> {

    private static final Logger logger = LoggerFactory.getLogger(CustomJedisRedisChatMemoryAutoConfiguration.class);

    public CustomJedisRedisChatMemoryAutoConfiguration(RedisChatMemoryProperties properties, RedisChatMemoryConnectionDetails connectionDetails, ObjectProvider<SslBundles> sslBundles) {
        super(properties, connectionDetails, sslBundles);
    }

    @Bean
    public JedisRedisChatMemoryRepository jedisRedisChatMemoryRepository() {
        return super.buildRedisChatMemoryRepository();
    }

    @Override
    protected JedisRedisChatMemoryRepository createStandaloneChatMemoryRepository(RedisChatMemoryStandaloneConfiguration standaloneConfiguration) {
        logger.info("Configuring Redis Standalone chat memory repository using Jedis");
        return JedisRedisChatMemoryRepository.builder()
                .host(standaloneConfiguration.hostName())
                .port(standaloneConfiguration.port())
                .username(standaloneConfiguration.username())
                .password(standaloneConfiguration.password())
                .timeout(standaloneConfiguration.timeout())
                .sslBundles(standaloneConfiguration.sslBundles())
                .useSsl(standaloneConfiguration.ssl().isEnabled())
                .bundle(standaloneConfiguration.ssl().getBundle())
                // using your JedisPoolConfig here
                .poolConfig(new JedisPoolConfig())
                .build();
    }

    @Override
    protected JedisRedisChatMemoryRepository createClusterChatMemoryRepository(RedisChatMemoryClusterConfiguration clusterConfiguration) {
        logger.info("Configuring Redis Cluster chat memory repository using Jedis");
        return JedisRedisChatMemoryRepository.builder()
                .nodes(clusterConfiguration.nodeAddresses())
                .username(clusterConfiguration.username())
                .password(clusterConfiguration.password())
                .timeout(clusterConfiguration.timeout())
                .sslBundles(clusterConfiguration.sslBundles())
                .useSsl(clusterConfiguration.ssl().isEnabled())
                .bundle(clusterConfiguration.ssl().getBundle())
                // using your JedisPoolConfig here
                .poolConfig(new JedisPoolConfig())
                .build();
    }
}
```

### (Optional) Overriding Default LettuceRedisChatMemoryRepository with GenericObjectPoolConfig

```java
import com.alibaba.cloud.ai.autoconfigure.memory.redis.*;
import com.alibaba.cloud.ai.memory.redis.LettuceRedisChatMemoryRepository;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CustomLettuceRedisChatMemoryAutoConfiguration extends RedisChatMemoryConnectionAutoConfiguration<LettuceRedisChatMemoryRepository> {

    private static final Logger logger = LoggerFactory.getLogger(CustomLettuceRedisChatMemoryAutoConfiguration.class);

    public CustomLettuceRedisChatMemoryAutoConfiguration(RedisChatMemoryProperties properties, RedisChatMemoryConnectionDetails connectionDetails, ObjectProvider<SslBundles> sslBundles) {
        super(properties, connectionDetails, sslBundles);
    }

    @Override
    @Bean
    protected LettuceRedisChatMemoryRepository buildRedisChatMemoryRepository() {
        return super.buildRedisChatMemoryRepository();
    }

    @Override
    protected LettuceRedisChatMemoryRepository createStandaloneChatMemoryRepository(RedisChatMemoryStandaloneConfiguration standaloneConfiguration) {
        logger.info("Configuring Redis Standalone chat memory repository using Lettuce");
        return LettuceRedisChatMemoryRepository.builder()
                .host(standaloneConfiguration.hostName())
                .port(standaloneConfiguration.port())
                .username(standaloneConfiguration.username())
                .password(standaloneConfiguration.password())
                .timeout(standaloneConfiguration.timeout())
                .sslBundles(standaloneConfiguration.sslBundles())
                .useSsl(standaloneConfiguration.ssl().isEnabled())
                .bundle(standaloneConfiguration.ssl().getBundle())
                // using your GenericObjectPoolConfig here
                .poolConfig(new GenericObjectPoolConfig<>())
                .build();
    }

    @Override
    protected LettuceRedisChatMemoryRepository createClusterChatMemoryRepository(RedisChatMemoryClusterConfiguration clusterConfiguration) {
        logger.info("Configuring Redis Cluster chat memory repository using Lettuce");
        return LettuceRedisChatMemoryRepository.builder()
                .nodes(clusterConfiguration.nodeAddresses())
                .username(clusterConfiguration.username())
                .password(clusterConfiguration.password())
                .timeout(clusterConfiguration.timeout())
                .sslBundles(clusterConfiguration.sslBundles())
                .useSsl(clusterConfiguration.ssl().isEnabled())
                .bundle(clusterConfiguration.ssl().getBundle())
                // using your GenericObjectPoolConfig here
                .poolConfig(new GenericObjectPoolConfig<>())
                .build();
    }
}
```

### (Optional) Overriding Default RedissonRedisChatMemoryRepository with Config

```java
import com.alibaba.cloud.ai.autoconfigure.memory.redis.*;
import com.alibaba.cloud.ai.memory.redis.RedissonRedisChatMemoryRepository;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CustomRedissonRedisChatMemoryAutoConfiguration extends RedisChatMemoryConnectionAutoConfiguration<RedissonRedisChatMemoryRepository> {

    private static final Logger logger = LoggerFactory.getLogger(CustomRedissonRedisChatMemoryAutoConfiguration.class);

    public CustomRedissonRedisChatMemoryAutoConfiguration(RedisChatMemoryProperties properties, RedisChatMemoryConnectionDetails connectionDetails, ObjectProvider<SslBundles> sslBundles) {
        super(properties, connectionDetails, sslBundles);
    }

    @Override
    @Bean
    protected RedissonRedisChatMemoryRepository buildRedisChatMemoryRepository() {
        return super.buildRedisChatMemoryRepository();
    }

    @Override
    protected RedissonRedisChatMemoryRepository createStandaloneRepository(RedisChatMemoryStandaloneConfiguration configuration) {
        logger.info("Configuring Redis Standalone chat memory repository using Redisson");
        return RedissonRedisChatMemoryRepository.builder()
                .host(configuration.hostName())
                .port(configuration.port())
                .username(configuration.username())
                .password(configuration.password())
                .timeout(configuration.timeout())
                .sslBundles(configuration.sslBundles())
                .useSsl(configuration.ssl().isEnabled())
                .bundle(configuration.ssl().getBundle())
                // using your Config here
                .redissonConfig(new Config())
                .build();
    }

    @Override
    protected RedissonRedisChatMemoryRepository createClusterRepository(RedisChatMemoryClusterConfiguration configuration) {
        logger.info("Configuring Redis Cluster chat memory repository using Redisson");
        return RedissonRedisChatMemoryRepository.builder()
                .nodes(configuration.nodeAddresses())
                .username(configuration.username())
                .password(configuration.password())
                .timeout(configuration.timeout())
                .sslBundles(configuration.sslBundles())
                .useSsl(configuration.ssl().isEnabled())
                .bundle(configuration.ssl().getBundle())
                // using your Config here
                .redissonConfig(new Config())
                .build();
    }
}
```

### Sample Code

```java
import com.alibaba.cloud.ai.memory.redis.BaseRedisChatMemoryRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
public class ChatController {

    @Autowired
    private BaseRedisChatMemoryRepository baseRedisChatMemoryRepository;

    @Autowired
    private ChatClient chatClient;

    /**
     * Stream-based chat interface (with conversation history stored in Redis).
     *
     * @param prompt User's input question or prompt.
     * @param chatId Conversation ID used to identify the current session.
     * @param response HttpServletResponse object for setting response encoding.
     * @return Streamed response content (Flux<String>), gradually output AI responses
     */
    @GetMapping("/redis")
    public Flux<String> redisChat(
            @RequestParam("prompt") String prompt,
            @RequestParam("chatId") String chatId,
            HttpServletResponse response) {

        // Sets the response character encoding to UTF-8 to ensure proper display of Chinese and other Unicode characters
        response.setCharacterEncoding("UTF-8");

        // Constructs a message window-based chat memory component retaining up to 10 recent messages
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(baseRedisChatMemoryRepository)
                .maxMessages(10)
                .build();

        // Initiates AI model invocation with memory capabilities enabled
        return chatClient.prompt(prompt)
                .advisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
                .stream()     // Enables streaming response
                .content();   // Retrieves the content stream
    }
}
```
