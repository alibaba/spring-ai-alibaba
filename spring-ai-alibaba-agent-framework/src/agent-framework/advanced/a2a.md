---
title: 分布式智能体（A2A Agent）
description: 了解如何在Spring AI Alibaba中实现Multi-agent协作，包括工具调用和交接模式
keywords: [Multi-agent, Multi-Agent, 工具调用, Tool Calling, Handoffs, Agent协作, 子Agent]
---

## 分布式 Agent
### A2A 协议简介
随着智能体应用的广泛落地，智能体应用间的分布式部署与远程通信成为要解决的关键问题，Google 推出的 [Agent2Agent（A2A）协议](https://a2a-protocol.org/latest/)即面向这一落地场景：A2A 解决智能体与其他使用不同框架、部署在不同机器、不同公司的智能体进行有效通信和协作的问题。

![](https://intranetproxy.alipay.com/skylark/lark/0/2025/png/54037/1756226684101-03a0e1a3-78cc-49f3-870d-5e17f373ec12.png)

### Spring AI Alibaba 中如何调用 A2A 远程智能体
A2aRemoteAgent 用来声明一个远程通信智能体，通过 AgentCard 指定远程智能体信息，包括连接的 url 地址、capabilities 等。

![](https://intranetproxy.alipay.com/skylark/lark/0/2025/png/54037/1756258006246-75d69627-9602-464c-a9c7-598791387b70.png)

以上示例中分别实现了两个远程智能体，然后使用 SequentialAgent 将两个智能体组成串行流程，就像使用同进程内的智能体一样。可在此查看以上代码片段[完整示例](https://github.com/alibaba/spring-ai-alibaba/blob/main/spring-ai-alibaba-graph-core/src/test/java/com/alibaba/cloud/ai/graph/agent/RemoteAgentTest.java)。

![](https://intranetproxy.alipay.com/skylark/lark/0/2025/png/54037/1756258024121-662ad249-3107-49cc-bf4c-1e0c1f316452.png)

### Spring AI Alibaba 中如何发布 A2A 智能体
如果要将一个智能体发布为 A2A 服务，需要增加 Spring AI Alibaba Runtime 组件依赖， Spring AI Alibaba Runtime 负责将智能体发布出去。具体请参见后续 Spring AI Alibaba Runtime 文章解读。

### 基于 Nacos A2A Registry 的远程智能体自动发现
Nacos3 最新版本提供了 A2A AgentCard 模型的存储与推送支持，因此可以作为 A2A Registry 实现。Spring AI Alibaba 通过与 Nacos A2A Registry 集成，可以实现：

1. A2A Server AgentCard 自动注册到 Nacos A2A Registry
2. A2a Client 自动订阅发现可用 AgentCard，实现 AgentCard 调用的负载均衡

![](https://intranetproxy.alipay.com/skylark/lark/0/2025/png/54037/1756226678279-775bf8da-b273-4fc9-b302-bc3618b0bd99.png)
