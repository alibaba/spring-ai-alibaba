---
title: Redis 检查点持久化
description: 使用 Redis 数据库持久化和管理 Spring AI Alibaba Graph 工作流状态，实现跨执行的状态保持
keywords: [Spring AI Alibaba, Redis, Checkpoint, 检查点, 持久化, 状态管理, 工作流状态]
---

# Redis 检查点持久化

> 在 Redis 数据库中持久化和管理您的 Spring AI Alibaba Graph 工作流状态，确保持久性

## 概述

Redis 检查点持久化是 Spring AI Alibaba Graph 生态系统的一个模块，它使得工作流状态能够可靠地存储在 Redis 数据库中。这使您基于 LLM 的应用程序在执行之间保持状态——确保工作流进度不会丢失，并且可以在任何时候恢复或分析。

主要特性包括：
- **基于 Redis 的持久化**：所有工作流状态都存储在 Redis 数据库中，可以在进程重启或系统故障后保存。
- **高性能存储**：利用 Redis 的内存存储特性，提供极快的读写性能。
- **自动过期管理**：支持 TTL（Time To Live）配置，自动清理过期的检查点数据。

## 功能特性

- **持久化状态**：持久化 Spring AI Alibaba Graph 工作流的整个状态，允许随时继续或恢复。
- **高性能访问**：利用 Redis 的内存存储和数据结构，提供毫秒级的访问速度。
- **灵活的配置**：支持单机、哨兵、集群等多种 Redis 部署模式。
- **无缝集成**：开箱即用地与 Spring AI Alibaba Graph 的状态管理和工作流 API 配合使用。

## 要求

- **Redis 数据库**：推荐版本 6.0 或更高。
- **Java 17+**
- **Spring AI Alibaba Graph 核心库**
- **Redisson 客户端**：用于与 Redis 交互

## 快速开始

### 添加依赖

在您的项目构建配置中添加以下内容：

**Maven**
<Code language="xml">
{`
<!-- Redisson 客户端依赖 -->
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson</artifactId>
    <version>3.22.0</version>
</dependency>`}
</Code>

**Gradle**
<Code language="gradle">
{`implementation 'com.alibaba.cloud.ai:spring-ai-alibaba-graph-checkpoint-redis:1.0.0.3-SNAPSHOT'
implementation 'org.redisson:redisson:3.24.3'`}
</Code>



### 初始化 RedisSaver

RedisSaver 使用 Redisson 客户端进行配置。您需要先配置 Redisson 客户端，然后创建 RedisSaver。

<Code
  language="java"
  title="初始化 RedisSaver" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/examples/CheckpointRedisExample.java"
>
{`import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.RedisSaver;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

// 配置 Redisson 客户端
Config config = new Config();
config.useSingleServer()
        .setAddress("redis://localhost:6379");  // Redis 地址

RedissonClient redisson = Redisson.create(config);

// 初始化 Redis Saver
RedisSaver saver = new RedisSaver(redisson);

// 配置 Saver
SaverConfig saverConfig = SaverConfig.builder()
        .register(saver)
        .build();`}
</Code>

### 完整示例

以下是如何使用 Redis 检查点持久化来保存、重新加载和验证工作流状态的完整示例：

<Code
  language="java"
  title="完整示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/examples/CheckpointRedisExample.java"
>
{`import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.RedisSaver;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.StateSnapshot;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

public class RedisCheckpointExample {

    public void testCheckpointWithRedis() throws Exception {
        // 初始化 Redis Saver
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://localhost:6379");

        RedissonClient redisson = Redisson.create(config);
        RedisSaver saver = new RedisSaver(redisson);

        SaverConfig saverConfig = SaverConfig.builder()
                .register(saver)
                .build();

        // 定义状态策略
        KeyStrategyFactory keyStrategyFactory = () -> {
            Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
            keyStrategyMap.put("input", new ReplaceStrategy());
            keyStrategyMap.put("agent_1:prop1", new ReplaceStrategy());
            return keyStrategyMap;
        };

        // 定义节点
        var agent1 = node_async(state -> {
            System.out.println("agent_1 执行中");
            return Map.of("agent_1:prop1", "agent_1:test");
        });

        // 构建图
        StateGraph stateGraph = new StateGraph(keyStrategyFactory)
                .addNode("agent_1", agent1)
                .addEdge(START, "agent_1")
                .addEdge("agent_1", END);

        // 使用检查点编译图
        CompiledGraph workflow = stateGraph.compile(
                CompileConfig.builder()
                        .saverConfig(saverConfig)
                        .build()
        );

        // 执行工作流
        RunnableConfig runnableConfig = RunnableConfig.builder()
                .threadId("test-thread-1")
                .build();

        Map<String, Object> inputs = Map.of("input", "test1");
        OverAllState result = workflow.invoke(inputs, runnableConfig).orElseThrow();

        // 获取检查点历史
        List<StateSnapshot> history = (List<StateSnapshot>) workflow.getStateHistory(runnableConfig);

        System.out.println("检查点历史数量: " + history.size());

        // 获取最后保存的检查点
        StateSnapshot lastSnapshot = workflow.getState(runnableConfig);

        System.out.println("最后检查点节点: " + lastSnapshot.node());

        // 测试从 Redis 重新加载检查点
        // 创建新的 saver（重置缓存）
        Config newConfig = new Config();
        newConfig.useSingleServer()
                .setAddress("redis://localhost:6379");

        RedissonClient newRedisson = Redisson.create(newConfig);
        RedisSaver newSaver = new RedisSaver(newRedisson);

        SaverConfig newSaverConfig = SaverConfig.builder()
                .register(newSaver)
                .build();

        // 重新编译图
        CompiledGraph reloadedWorkflow = stateGraph.compile(
                CompileConfig.builder()
                        .saverConfig(newSaverConfig)
                        .build()
        );

        // 使用相同的 threadId 获取历史
        RunnableConfig reloadConfig = RunnableConfig.builder()
                .threadId("test-thread-1")
                .build();

        Collection<StateSnapshot> reloadedHistory = reloadedWorkflow.getStateHistory(reloadConfig);

        System.out.println("重新加载的检查点历史数量: " + reloadedHistory.size());
    }
}`}
</Code>

### Spring Boot 配置

如果您使用 Spring Boot，可以通过配置文件配置 Redis 连接：

<Code
  language="yaml"
  title="application.yml"
>
{`spring:
  redis:
    host: localhost
    port: 6379
    password: # 可选，如果 Redis 设置了密码
    database: 0`}
</Code>

然后在配置类中创建 RedisSaver Bean：

<Code
  language="java"
  title="Spring Boot 配置类" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/examples/CheckpointRedisExample.java"
>
{`import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.RedisSaver;

@Configuration
public class GraphCheckpointConfig {

    @Value("&#36;{spring.redis.host}")
    private String redisHost;

    @Value("&#36;{spring.redis.port}")
    private int redisPort;

    @Value("&#36;{spring.redis.password:}")
    private String redisPassword;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress(String.format("redis://%s:%d", redisHost, redisPort));
        
        if (redisPassword != null && !redisPassword.isEmpty()) {
            config.useSingleServer().setPassword(redisPassword);
        }
        
        return Redisson.create(config);
    }

    @Bean
    public RedisSaver redisSaver(RedissonClient redissonClient) {
        return new RedisSaver(redissonClient);
    }

    @Bean
    public SaverConfig saverConfig(RedisSaver redisSaver) {
        return SaverConfig.builder()
                .register(redisSaver)
                .build();
    }
}`}
</Code>

## 数据存储

Redis 检查点器使用以下数据结构来存储工作流状态：

- **Hash 结构**：存储每个检查点的完整状态数据
- **Sorted Set**：用于存储检查点的元数据和排序信息
- **String**：存储检查点的序列化状态

Redis 会自动管理这些数据结构，无需手动创建表或 schema。

## 高级用法

### 恢复工作流

从 Redis 恢复工作流非常简单：

<Code
  language="java"
  title="恢复工作流" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/examples/CheckpointRedisExample.java"
>
{`import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.RunnableConfig;

import java.util.Map;

// 使用相同的 threadId
RunnableConfig config = RunnableConfig.builder()
        .threadId("original-thread-id")
        .build();

// 从上次检查点继续执行
workflow.invoke(Map.of(), config);`}
</Code>

### 查询历史状态

<Code
  language="java"
  title="查询历史状态" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/examples/CheckpointRedisExample.java"
>
{`import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.state.StateSnapshot;

import java.util.List;

// 获取所有历史状态
List<StateSnapshot> history = (List<StateSnapshot>) workflow.getStateHistory(config);

// 遍历历史
for (StateSnapshot snapshot : history) {
    System.out.println("节点: " + snapshot.node());
    System.out.println("状态: " + snapshot.state());
    System.out.println("Checkpoint ID: " + snapshot.config().checkPointId());
}`}
</Code>

### 从特定检查点恢复

<Code
  language="java"
  title="从特定检查点恢复" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/examples/CheckpointRedisExample.java"
>
{`import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.RunnableConfig;

import java.util.Map;

// 获取特定检查点
RunnableConfig checkpointConfig = RunnableConfig.builder()
        .threadId("thread-id")
        .checkPointId("specific-checkpoint-id")
        .build();

// 从该检查点继续
workflow.invoke(Map.of(), checkpointConfig);
System.out.println("从检查点恢复执行完成");`}
</Code>

## 性能优化

1. **连接池**：Redisson 自动管理连接池，优化 Redis 连接性能。
2. **内存优化**：合理配置 Redis 内存限制，避免内存溢出。
3. **TTL 设置**：为检查点数据设置合理的过期时间，自动清理旧数据。
4. **批量操作**：利用 Redis Pipeline 减少网络往返次数。
5. **持久化配置**：根据需求配置 RDB 或 AOF 持久化策略。

## 故障排除

### 连接问题

确保 Redis 服务器可访问且连接参数正确：

<Code
  language="java"
  title="测试连接" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/examples/CheckpointRedisExample.java"
>
{`// 测试 Redis 连接
try {
    Config config = new Config();
    config.useSingleServer()
            .setAddress("redis://localhost:6379");
    
    RedissonClient redisson = Redisson.create(config);
    
    // 执行简单的 ping 操作测试连接
    redisson.getKeys().count();
    System.out.println("Redis 连接成功！");
    
    redisson.shutdown();
} catch (Exception e) {
    System.err.println("Redis 连接失败: " + e.getMessage());
}`}
</Code>

### 内存问题

如果遇到内存不足的错误：
- 检查 Redis 内存使用情况：`redis-cli info memory`
- 配置 Redis 最大内存限制
- 为检查点数据设置合理的 TTL
- 考虑使用 Redis 集群模式分散内存压力

## 最佳实践

1. **唯一的线程 ID**：为每个独立的工作流实例使用唯一的线程 ID。
2. **定期备份**：配置 Redis 持久化（RDB 或 AOF）以防止数据丢失。
3. **监控**：监控 Redis 内存使用、连接数和性能指标。
4. **TTL 策略**：为检查点数据设置合理的过期时间，自动清理旧数据。
5. **安全性**：使用 Redis 密码认证，限制网络访问，启用 TLS 加密。
6. **高可用**：生产环境建议使用 Redis Sentinel 或 Cluster 模式。
7. **内存管理**：合理配置 Redis 最大内存和淘汰策略。

## 总结

Redis 检查点持久化为 Spring AI Alibaba Graph 提供了高性能的状态管理解决方案，使您的 AI 应用程序能够在故障后恢复，实现长时间运行的工作流，并支持人在回路中的交互。通过利用 Redis 的内存存储特性和高性能，您可以构建健壮的、生产级的 AI 应用程序。Redis 的快速读写能力和灵活的数据结构使其成为检查点持久化的理想选择。

