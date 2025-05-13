---
title: Spring AI Alibaba MCP 市场正式上线！
keywords: [Spring AI, Spring AI Alibaba, MCP, MCP市场]
description: Spring AI Alibaba 正式上线 MCP 市场，开发者可以在这里搜索市面上可用的 MCP Server 服务，了解每个服务的实现与接入方法。
author: 刘军
date: "2025-04-07"
category: article
---

Spring AI Alibaba 正式上线 MCP 市场：[**https://java2ai.com/mcp/**](https://java2ai.com/mcp/)。

开发者可以在这里搜索市面上可用的 MCP Server 服务，了解每个服务的实现与接入方法。

![](/img/blog/mcp-marketplace/marketplace-screenshot.png)

## MCP 市场是做什么的？
Spring AI Alibaba MCP 当前主要提供 MCP Server 的查看与浏览能力，帮您省去了到处搜集 MCP 实现的负担，后续可通过接入 Higress 等 AI 网关体验在线接入。

由于 MCP 正处于快速发展阶段，我们将尽量保持 MCP 市场展示最新的数据。欢迎社区贡献者一起参与贡献，我们共同维护 MCP 市场更新，欢迎在这里 [修改源码](https://github.com/springaialibaba/spring-ai-alibaba-website/blob/main/src/components/plugin/McpHub/PluginEnum.js) 并提交 PR。

## 什么是 MCP？
模型上下文协议（MCP）是一种开放协议，旨在实现 LLM 应用程序与外部数据源和工具之间的无缝集成。它作为一种标准化的方式，将 LLM 与其所需的上下文联系起来。

## MCP 解决什么问题？
MCP 解决了人工智能系统和数据源之间零散集成的问题。它解决了人工智能模型与数据隔离并被困在信息孤岛后面的挑战，用一个通用协议取代了多个自定义实现。

## MCP 的有哪些应用案例？
<font style="color:rgb(2, 8, 23);">MCP 可用于各种场景，包括：构建 Claude、Clinet 等智能体 IDE、增强聊天界面、创建自定义 AI 工作流、将 AI 应用与外部数据源连接等。</font>

## 如何使用 Java 开发应用并发布为 MCP Server？
Spring AI Alibaba 支持将 Java 应用发布为标准的 MCP Server，开发者只需要按照约定的步骤配置应用即可发布自己的 MCP 服务。可查看官方发布的 [博客](https://java2ai.com/blog/spring-ai-alibaba-mcp/#22-%E4%BD%BF%E7%94%A8-spring-ai-mcp-%E5%BF%AB%E9%80%9F%E6%90%AD%E5%BB%BA-mcp-server)。

## 如何在 Java 智能体中调用 MCP 服务？
使用 Spring AI Alibaba 开发的智能体应用可以接入 MCP 市场上所有 MCP Server 实现，具体使用方式可查看官方发布的 [博客](https://java2ai.com/blog/spring-ai-alibaba-mcp/#22-%E4%BD%BF%E7%94%A8-spring-ai-mcp-%E5%BF%AB%E9%80%9F%E6%90%AD%E5%BB%BA-mcp-server)。

## 如何将已有的 Java 应用转换成 MCP 服务？
站在大模型视角，应用发布的服务要通过 MCP 接入智能体，必须满足以下要求:

+ 服务包含明确的语义描述、方法入参格式与语义描述、方法返回值格式描述
+ 服务支持并能识别 MCP 协议请求

基于以上两点，存量的大部分 Java 应用应该都是不支持的。因此，我们需要做一些额外的工作来完成 MCP 接入：

1. 使用 Spring AI Alibaba 对应用进行改造，直接将应用发布为 MCP Server。这需要对应用进行代码改造，如升级 JDK17、增加注解、重新发布应用等。
2. 使用 Nacos + Higress 网关代理，通过配置化的形式在 Nacos 中增加服务描述等元数据，同时使用 Higress 实现协议代理转换。该方案的优势是可以实现零代码修改 MCP 升级，具体可参考 [Nacos 社区发布的博客](https://nacos.io/blog/nacos-gvr7dx_awbbpb_vksfvdh9258pgddl/)。

稍后，我们将会在 Spring AI Alibaba 社区发布通过集成 Higress、Nacos 提供的 MCP 集成方案，敬请期待！

