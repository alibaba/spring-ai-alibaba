# Spring AI Alibaba Redis Memory 模块

[English](./README.md)

## 简介

Spring AI Alibaba Redis Memory 模块是Spring AI Alibaba项目的核心组件之一，专门提供基于Redis的高性能内存存储解决方案。该模块利用Redis的高速缓存和持久化特性，为AI应用提供快速、可靠的对话历史和上下文数据存储服务，使AI系统能够"记住"之前的交互，从而提供更连贯、更个性化的用户体验。

## 主要特性

- **高性能Redis存储**：利用Redis的高速读写能力，实现对话历史和上下文数据的快速存取
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
    <artifactId>spring-ai-alibaba-starter-memory-redis</artifactId>
    <version>${latest.version}</version>
</dependency>
```

### 基本配置-单机

在`application.properties`或`application.yml`中添加Redis配置：

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

### 基本配置-集群

在`application.properties`或`application.yml`中添加Redis配置：

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

### 切换redis client客户端

在`application.properties`或`application.yml`中添加Redis配置：

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

### 配置SSL
```yaml
# 基础spring.ssl配置(可选pem或jks配置)，参考spring.ssl原始配置
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

### 完全配置-单机
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

### 完全配置-集群
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

### (可选)使用JedisPoolConfig覆盖默认的JedisRedisChatMemoryRepository

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

### (可选)使用GenericObjectPoolConfig覆盖默认的LettuceRedisChatMemoryRepository

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

### (可选)使用Config覆盖默认的RedissonRedisChatMemoryRepository

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

### 示例代码

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
     * 流式聊天接口（基于 Redis 存储对话历史）
     *
     * @param prompt 用户输入的问题或提示
     * @param chatId 对话 ID，用于标识当前会话
     * @param response HttpServletResponse 对象，用于设置响应编码
     * @return 返回流式响应内容（Flux<String>），逐步输出 AI 回答
     */
    @GetMapping("/redis")
    public Flux<String> redisChat(
            @RequestParam("prompt") String prompt,
            @RequestParam("chatId") String chatId,
            HttpServletResponse response) {

        // 设置响应字符编码为 UTF-8，确保中文等字符正确显示
        response.setCharacterEncoding("UTF-8");

        // 构建带消息窗口的记忆组件，最多保留最近 10 条消息
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(baseRedisChatMemoryRepository)
                .maxMessages(10)
                .build();

        // 发起 AI 模型调用，并启用记忆功能
        return chatClient.prompt(prompt)
                .advisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
                .stream()     // 使用流式响应
                .content();   // 获取内容流
    }
}
```
