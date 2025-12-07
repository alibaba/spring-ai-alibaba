---
title: 分布式智能体（A2A Agent）
description: 了解如何在Spring AI Alibaba中使用A2A协议实现分布式Agent通信，包括Agent注册、发现和远程调用
keywords: [A2A, Agent-to-Agent, 分布式Agent, 远程Agent, Nacos, Agent注册, Agent发现]
---

## A2A 协议简介

随着智能体应用的广泛落地，智能体应用间的分布式部署与远程通信成为要解决的关键问题，Google 推出的 [Agent2Agent（A2A）协议](https://a2a-protocol.org/latest/)即面向这一落地场景：A2A 解决智能体与其他使用不同框架、部署在不同机器、不同公司的智能体进行有效通信和协作的问题。

A2A 协议定义了智能体之间通信的标准方式，使得不同框架、不同部署环境的智能体能够无缝协作。

## A2A 架构

Spring AI Alibaba 的 A2A 实现包含三个核心组件：

1. **A2A Server**：将本地 ReactAgent 暴露为 A2A 服务
2. **A2A Registry**：Agent 注册中心（支持 Nacos）
3. **A2A Discovery**：Agent 发现机制（支持 Nacos）

### 注册与发现流程

```
┌─────────────────┐         ┌──────────────┐         ┌─────────────────┐
│  Agent Provider │         │  Nacos       │         │  Agent Consumer │
│  (本地Agent)    │────────▶│  Registry    │◀────────│  (远程调用)     │
└─────────────────┘         └──────────────┘         └─────────────────┘
       │                            │                          │
       │ 1. 注册 AgentCard          │                          │
       │───────────────────────────▶│                          │
       │                            │                          │
       │                            │ 2. 查询 AgentCard        │
       │                            │◀──────────────────────────│
       │                            │                          │
       │ 3. 远程调用                │                          │
       │◀───────────────────────────│                          │
       │                            │                          │
```

## 发布 A2A 智能体

要将一个智能体发布为 A2A 服务，需要：

1. 创建 ReactAgent Bean
2. 配置 A2A Server
3. （可选）配置 Nacos Registry 进行自动注册

### 定义本地 Agent

<Code
  language="java"
  title="定义本地 ReactAgent 示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/advanced/A2AExample.java"
>
{`import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class A2AAgentConfig {

    @Bean(name = "dataAnalysisAgent")
    public ReactAgent dataAnalysisAgent(@Qualifier("dashscopeChatModel") ChatModel chatModel) {
        return ReactAgent.builder()
                .name("data_analysis_agent")
                .model(chatModel)
                .description("专门用于数据分析和统计计算的本地智能体")
                .instruction("你是一个专业的数据分析专家，擅长处理各类数据统计和分析任务。" +
                        "你能够理解用户的数据分析需求，提供准确的统计计算结果和专业的分析建议。")
                .outputKey("messages")
                .build();
    }
}`}
</Code>

### 配置 A2A Server

在 `application.yml` 中配置 A2A Server：

<Code
  language="yaml"
  title="A2A Server 配置示例"
>
{`spring:
  ai:
    alibaba:
      a2a:
        server:
          version: 1.0.0
          card:
            name: data_analysis_agent
            description: 专门用于数据分析和统计计算的本地智能体
            provider:
              name: Spring AI Alibaba Documentation
              organization: Spring AI Alibaba`}
</Code>

启动应用后，A2A Server 会自动：
- 根据 ReactAgent Bean 生成 AgentCard
- 暴露 REST API 端点：
  - `/.well-known/agent.json` - AgentCard 元数据
  - `/a2a/message` - Agent 调用端点

## 调用 A2A 远程智能体

### 使用 AgentCardProvider 发现 Agent

Spring AI Alibaba 支持通过 `AgentCardProvider` 从注册中心（如 Nacos）发现远程 Agent。

<Code
  language="java"
  title="使用 AgentCardProvider 发现并调用远程 Agent" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/advanced/A2AExample.java"
>
{`import com.alibaba.cloud.ai.graph.agent.a2a.A2aRemoteAgent;
import com.alibaba.cloud.ai.graph.agent.a2a.AgentCardProvider;
import com.alibaba.cloud.ai.graph.OverAllState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class A2AExample {

    private final AgentCardProvider agentCardProvider;

    @Autowired
    public A2AExample(AgentCardProvider agentCardProvider) {
        this.agentCardProvider = agentCardProvider;
    }

    public void callRemoteAgent() {
        // 通过 AgentCardProvider 从注册中心发现 Agent
        A2aRemoteAgent remote = A2aRemoteAgent.builder()
                .name("data_analysis_agent")
                .agentCardProvider(agentCardProvider)  // 从 Nacos 自动获取 AgentCard
                .description("数据分析远程代理")
                .build();

        // 远程调用
        Optional<OverAllState> result = remote.invoke("请根据季度数据给出同比与环比分析概要。");
        
        result.ifPresent(state -> {
            System.out.println("调用成功: " + state.value("output"));
        });
    }
}`}
</Code>

## 基于 Nacos 的 A2A Registry

Nacos3 最新版本提供了 A2A AgentCard 模型的存储与推送支持，因此可以作为 A2A Registry 实现。Spring AI Alibaba 通过与 Nacos A2A Registry 集成，可以实现：

1. **A2A Server AgentCard 自动注册到 Nacos A2A Registry**
2. **A2a Client 自动订阅发现可用 AgentCard，实现 AgentCard 调用的负载均衡**

### 配置 Nacos Registry 和 Discovery

<Code
  language="yaml"
  title="Nacos A2A 配置示例"
>
{`spring:
  ai:
    alibaba:
      a2a:
        nacos:
          server-addr: 127.0.0.1:8848
          username: nacos
          password: nacos
          discovery:
            enabled: true   # 启用服务发现（查询其他 Agent）
          registry:
            enabled: true   # 启用服务注册（注册本地 Agent）
        server:
          version: 1.0.0
          card:
            name: data_analysis_agent
            description: 专门用于数据分析和统计计算的本地智能体
            provider:
              name: Spring AI Alibaba Documentation
              organization: Spring AI Alibaba`}
</Code>

**重要配置说明**：
- `registry.enabled: true` - 必须启用才能将 Agent 注册到 Nacos（服务提供者）
- `discovery.enabled: true` - 启用后才能通过 AgentCardProvider 发现其他 Agent（服务消费者）
- `server.card.name` - 必须与 ReactAgent Bean 的 `name` 一致

### 完整示例

<Code
  language="java"
  title="A2A 一体化示例：注册 -> 发现 -> 调用"
  sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/advanced/A2AExample.java"
>
{`import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.a2a.A2aRemoteAgent;
import com.alibaba.cloud.ai.graph.agent.a2a.AgentCardProvider;
import com.alibaba.cloud.ai.graph.OverAllState;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class A2AExample {

    private final ChatModel chatModel;
    private final AgentCardProvider agentCardProvider;
    private final ReactAgent localDataAnalysisAgent;

    @Autowired
    public A2AExample(@Qualifier("dashscopeChatModel") ChatModel chatModel,
                      AgentCardProvider agentCardProvider,
                      @Qualifier("dataAnalysisAgent") ReactAgent localDataAnalysisAgent) {
        this.chatModel = chatModel;
        this.agentCardProvider = agentCardProvider;
        this.localDataAnalysisAgent = localDataAnalysisAgent;
    }

    public void runDemo() {
        // 1. 本地直连：验证本地注册的 ReactAgent 可用
        Optional<OverAllState> localResult = localDataAnalysisAgent.invoke(
            "请对上月销售数据进行趋势分析，并给出关键结论。"
        );
        localResult.ifPresent(state -> {
            System.out.println("本地调用成功");
        });

        // 2. 发现：通过 AgentCardProvider 从注册中心获取该 Agent 的 AgentCard
        A2aRemoteAgent remote = A2aRemoteAgent.builder()
                .name("data_analysis_agent")
                .agentCardProvider(agentCardProvider)  // 从 Nacos 自动获取 AgentCard
                .description("数据分析远程代理")
                .build();

        // 3. 远程调用：通过 A2aRemoteAgent 调用
        Optional<OverAllState> remoteResult = remote.invoke(
            "请根据季度数据给出同比与环比分析概要。"
        );
        remoteResult.ifPresent(state -> {
            System.out.println("远程调用成功");
        });
    }
}`}
</Code>

## 验证和测试

### 验证本地 AgentCard

```bash
curl http://localhost:8080/.well-known/agent.json
```

### 验证 Nacos 注册

1. 打开 Nacos 控制台：http://localhost:8848/nacos
2. 登录（nacos/nacos）
3. 查看 A2A 服务注册维度，应该能看到注册的 Agent

### 注册与发现的区别

| 功能 | 配置项 | 作用 | 角色 |
|------|--------|------|------|
| **Registry（注册）** | `registry.enabled: true` | 将本地 Agent 注册到 Nacos | 服务提供者 |
| **Discovery（发现）** | `discovery.enabled: true` | 从 Nacos 查询其他 Agent | 服务消费者 |

两者可独立配置，也可同时启用。当同时启用时：
- 作为**提供者**：注册本地 Agent 到 Nacos
- 作为**消费者**：可发现并调用其他已注册的 Agent（包括自己）

## 注意事项

1. **依赖要求**：
   - 需要添加 `spring-ai-alibaba-starter-a2a-nacos` 依赖
   - 确保 Nacos 服务正常运行

2. **AgentCard 元数据**：
   - `server.card.name` 必须与 ReactAgent Bean 的 `name` 一致
   - `server.card.provider` 可选，用于标识 Agent 提供者信息

3. **多 Agent 注册**：
   - 默认情况下，只有一个 Agent Bean 会被注册
   - 如需注册多个 Agent，需运行多个应用实例，每个实例配置不同的 Agent

## 故障排查

### Agent 没有注册到 Nacos
- 检查 `registry.enabled: true` 是否配置
- 查看应用日志，确认 Nacos Registry AutoConfiguration 是否生效
- 验证 Nacos 连接配置（server-addr、username、password）

### AgentCardProvider 无法发现 Agent
- 检查 `discovery.enabled: true` 是否配置
- 确认 Agent 已成功注册到 Nacos
- 验证 agent name 是否匹配

### 远程调用失败
- 确认目标 Agent 的 REST API 端点可访问
- 检查网络连接和防火墙配置
- 查看 A2A 消息传输日志
